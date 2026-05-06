(ns ledger.log-line
  (:require [cheshire.core :as json]))

(defn emit [m]
  (println (json/generate-string (assoc m :ts (str (java.time.Instant/now))))))
