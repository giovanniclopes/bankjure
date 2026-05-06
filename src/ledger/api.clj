(ns ledger.api
  (:require [cheshire.core :as json]
            [clojure.string :as str]
            [datomic.client.api :as d]
            [ledger.api.validate :as v]
            [ledger.core :as core]
            [ledger.db :as db]
            [ledger.log-line :as log]
            [malli.core :as m]))

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
  (or (when-let [[_ raw] (re-matches #"/api/accounts/([^/]+)/balance-as-of" uri)]
        (when-let [u (parse-account-uuid raw)]
          {:id u :tail :balance-as-of}))
      (when-let [[_ raw] (re-matches #"/api/accounts/([^/]+)/transactions" uri)]
        (when-let [u (parse-account-uuid raw)]
          {:id u :tail :transactions}))
      (when-let [[_ raw] (re-matches #"/api/accounts/([^/]+)" uri)]
        (when-let [u (parse-account-uuid raw)]
          {:id u :tail nil}))))

(defn balance->wire [b]
  (if (instance? java.math.BigDecimal b)
    (.toPlainString (.stripTrailingZeros b))
    (str b)))

(defn tx->wire [t]
  {:kind  (name (:tx/type t))
   :amount (balance->wire (:tx/amount t))
   :at    (some-> ^java.util.Date (:db/txInstant t) .toInstant .toString)})

(defn- parse-instant->date [^String s]
  (try (java.util.Date/from (java.time.Instant/parse s)) (catch Exception _ nil)))

(defn handle-health [_req]
  (json-response 200 {:ok true}))

(defn handle-create-account [req]
  (let [raw (read-json-body req)
        body (when raw (update raw :owner #(some-> % str str/trim)))]
    (if (m/validate v/create-account-body body)
      (let [created (db/create-account-returning! (conn!) (:owner body))]
        (json-response 201 {:ok        true
                            :accountId (str (:account/id created))
                            :owner     (:account/owner created)}))
      (let [det (v/explain-human v/create-account-body body)]
        (log/emit {:level "warn" :evt :validation :route :post-accounts :details det})
        (json-response 400 {:ok false :error "invalid body" :details det})))))

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

(defn handle-balance-as-of [account-id req]
  (let [at-str (get (:query-params req) "at")
        dbv    (d/db (conn!))]
    (if-not (db/find-account dbv account-id)
      (json-response 404 {:ok false :error "account not found"})
      (if-let [^java.util.Date inst (some-> at-str not-empty parse-instant->date)]
        (json-response 200 {:ok      true
                            :at      at-str
                            :balance (balance->wire (db/get-balance-as-of dbv account-id inst))})
        (json-response 400 {:ok false :error "query at must be ISO-8601 instant (ex: 2026-05-06T15:00:00Z)"})))))

(defn kind->keyword [k]
  (let [s (some-> k str str/lower-case)]
    (case s
      "deposit" :tx.type/deposit
      "withdraw" :tx.type/withdraw
      nil)))

(defn handle-post-transaction [account-id req]
  (let [dbv  (d/db (conn!))
        body (read-json-body req)]
    (if-not (db/find-account dbv account-id)
      (json-response 404 {:ok false :error "account not found"})
      (if-not (m/validate v/post-tx-body body)
        (let [det (v/explain-human v/post-tx-body body)]
          (log/emit {:level "warn" :evt :validation :route :post-tx :details det})
          (json-response 400 {:ok false :error "invalid body" :details det}))
        (let [kind (kind->keyword (:kind body))
              amt  (:amount body)]
          (if (nil? kind)
            (json-response 400 {:ok false :error "kind must be deposit or withdraw"})
            (let [res (db/process-transaction! (conn!) account-id kind amt)]
              (if (:success res)
                (json-response 200 {:ok      true
                                    :balance (balance->wire (db/get-balance (d/db (conn!)) account-id))})
                (json-response 422
                  {:ok false :error (:reason res)
                   :balance (some-> res :balance balance->wire)})))))))))

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
          :balance-as-of (handle-balance-as-of id req)
          nil (handle-get-account id)))

      (and (= method :post) (str/ends-with? uri "/transactions"))
      (when-let [[_ uuid-str] (re-matches #"/api/accounts/([^/]+)/transactions" uri)]
        (when-let [id (parse-account-uuid uuid-str)]
          (handle-post-transaction id req)))

      :else
      (json-response 404 {:ok false :error "not found"}))))
