(ns ledger.server
  (:require [ledger.api :as api]
            [ring.adapter.jetty :as jetty]
            [ring.middleware.cors :refer [wrap-cors]]))

(defn app []
  (-> api/router
      (wrap-cors
       :access-control-allow-origin [#".*"]
       :access-control-allow-methods [:get :post :put :delete :options :head]
       :access-control-allow-headers ["Content-Type"])))

(defn -main [& _args]
  (jetty/run-jetty (app) {:port 8080 :join? true}))
