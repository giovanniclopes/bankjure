(ns ledger.api
  (:require [cheshire.core :as json]
            [clojure.string :as str]
            [datomic.client.api :as d]
            [ledger.core :as core]
            [ledger.db :as db]))

(defonce ^:private !conn (atom nil))

(defn conn! []
  (or @!conn
      (let [c (core/connect)]
        (reset! !conn c)
        c)))

(def ^:private no-cache-headers
  {"Cache-Control" "no-store, no-cache, must-revalidate"
   "Pragma"        "no-cache"})

(defn json-response [status body]
  {:status status
   :headers (merge no-cache-headers
                   {"Content-Type" "application/json; charset=utf-8"})
   :body (json/generate-string body)})

(defn read-json-body [request]
  (when-let [raw (some-> request :body slurp not-empty)]
    (json/parse-string raw true)))

(defn parse-account-uuid [s]
  (try (java.util.UUID/fromString s) (catch Exception _ nil)))

(defn account-segment [uri]
  (or (when-let [[_ id] (re-matches #"/api/accounts/([^/]+)/transactions" uri)]
        (when-let [u (parse-account-uuid id)]
          {:id u :tail :transactions}))
      (when-let [[_ id] (re-matches #"/api/accounts/([^/]+)" uri)]
        (when-let [u (parse-account-uuid id)]
          {:id u :tail nil}))))

(defn balance->wire [b]
  (if (instance? java.math.BigDecimal b)
    (.toPlainString (.stripTrailingZeros b))
    (str b)))

(defn tx->wire [t]
  {:kind  (name (:tx/type t))
   :amount (balance->wire (:tx/amount t))
   :at    (some-> ^java.util.Date (:db/txInstant t) .toInstant .toString)})

(defn handle-health [_req]
  (json-response 200 {:ok true}))

(defn handle-create-account [req]
  (let [body (read-json-body req)
        owner (some-> body :owner str str/trim)]
    (cond
      (str/blank? owner)
      (json-response 400 {:ok false :error "owner is required"})

      :else
      (let [m (db/create-account-returning! (conn!) owner)]
        (json-response 201 {:ok        true
                            :accountId (str (:account/id m))
                            :owner     (:account/owner m)})))))

(defn handle-get-account [account-id]
  (let [dbv (d/db (conn!))
        acc (db/find-account dbv account-id)]
    (if acc
      (json-response 200 {:ok           true
                          :accountId    (str (:account/id acc))
                          :owner        (:account/owner acc)
                          :balance      (balance->wire (db/get-balance dbv account-id))
                          :transactions (mapv tx->wire (db/list-transactions dbv account-id))})
      (json-response 404 {:ok false :error "account not found"}))))

(defn handle-list-transactions [account-id]
  (let [dbv (d/db (conn!))]
    (if (db/find-account dbv account-id)
      (json-response 200 {:ok           true
                          :transactions (mapv tx->wire (db/list-transactions dbv account-id))})
      (json-response 404 {:ok false :error "account not found"}))))

(defn kind->keyword [k]
  (let [s (some-> k str str/lower-case)]
    (case s
      "deposit" :tx.type/deposit
      "withdraw" :tx.type/withdraw
      nil)))

(defn handle-post-transaction [account-id req]
  (let [dbv (d/db (conn!))]
    (if-not (db/find-account dbv account-id)
      (json-response 404 {:ok false :error "account not found"})
      (let [body (read-json-body req)
            kind (kind->keyword (:kind body))
            amt  (:amount body)]
        (cond
          (nil? kind)
          (json-response 400 {:ok false :error "kind must be deposit or withdraw"})

          (or (nil? amt) (not (number? amt)))
          (json-response 400 {:ok false :error "amount must be a number"})

          :else
          (let [res (db/process-transaction! (conn!) account-id kind amt)]
            (if (:success res)
              (json-response 200 {:ok      true
                                  :balance (balance->wire (db/get-balance (d/db (conn!)) account-id))})
              (json-response 422 {:ok     false
                                  :error  (:reason res)
                                  :balance (some-> res :balance balance->wire)}))))))))

(defn router [req]
  (let [method (:request-method req)
        uri    (:uri req)]
    (cond
      (and (= method :get) (= uri "/api/health"))
      (handle-health req)

      (and (= method :post) (= uri "/api/accounts"))
      (handle-create-account req)

      (and (= method :get) (some? (account-segment uri)))
      (let [{:keys [id tail]} (account-segment uri)]
        (case tail
          :transactions (handle-list-transactions id)
          nil (handle-get-account id)))

      (and (= method :post) (str/ends-with? uri "/transactions"))
      (when-let [[_ uuid-str] (re-matches #"/api/accounts/([^/]+)/transactions" uri)]
        (when-let [id (parse-account-uuid uuid-str)]
          (handle-post-transaction id req)))

      :else
      (json-response 404 {:ok false :error "not found"}))))
