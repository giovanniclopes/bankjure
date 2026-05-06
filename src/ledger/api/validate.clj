(ns ledger.api.validate
  (:require [malli.core :as m]
            [malli.error :as me]))

(def create-account-body
  [:map [:owner [:string {:min 1}]]])

(def post-tx-body
  [:map
   [:kind [:enum "deposit" "withdraw"]]
   [:amount [:and number? [:fn {:error/fn (constantly "must be > 0")} #(> % 0)]]]])

(defn explain-human [schema value]
  (some-> (m/explain schema value) me/humanize))
