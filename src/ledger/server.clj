(ns ledger.server
  (:require [ledger.api :as api]
            [ledger.log-line :as log]
            [ledger.middleware.auth :as auth]
            [ring.adapter.jetty :as jetty]
            [ring.middleware.cors :refer [wrap-cors]]
            [ring.middleware.params :refer [wrap-params]]))

(defn- wrap-api-log [handler]
  (fn [req]
    (let [res (handler req)]
      (when (>= (:status res) 400)
        (log/emit {:level "warn" :evt :http :method (name (:request-method req))
                   :uri (:uri req) :status (:status res)}))
      res)))

(defn app []
  (-> api/router
      wrap-params
      auth/wrap-require-jwt-for-mutations
      wrap-api-log
      (wrap-cors
       :access-control-allow-origin [#".*"]
       :access-control-allow-methods [:get :post :put :delete :options :head]
       :access-control-allow-headers ["Content-Type" "Authorization"])))

(defn -main [& _args]
  (jetty/run-jetty (app) {:port 8080 :join? true}))
