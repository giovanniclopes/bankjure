(ns ledger.middleware.auth
  (:require [buddy.sign.jwt :as jwt]
            [cheshire.core :as json]
            [clojure.string :as str]))

(defn jwt-secret []
  (some-> (System/getenv "BANKJURE_JWT_SECRET") not-empty))

(defn- bearer-token [req]
  (some-> (get-in req [:headers "authorization"])
          (str/replace #"^Bearer\s+" "")))

(defn wrap-require-jwt-for-mutations [handler]
  (fn [req]
    (if (#{:options :get :head} (:request-method req))
      (handler req)
      (if-not (jwt-secret)
        (handler req)
        (let [tok (bearer-token req)]
          (if (and tok (try (jwt/unsign tok (jwt-secret) {:alg :hs256})
                            (catch Exception _ nil)))
            (handler req)
            {:status 401
             :headers {"Content-Type" "application/json; charset=utf-8"}
             :body (json/generate-string
                    {:ok false :error "unauthorized — use Authorization Bearer (JWT HS256, BANKJURE_JWT_SECRET)"})}))))))
