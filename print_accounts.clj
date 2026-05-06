(require '[ledger.core :as core]
         '[ledger.db :as db]
         '[datomic.client.api :as d])

(let [conn (core/system)
      db  (d/db conn)]
  (println "contas (titular · id · saldo)")
  (doseq [a (db/list-all-accounts db)]
    (println
     (:account/owner a)
     (:account/id a)
     (.toPlainString (.stripTrailingZeros (db/get-balance db (:account/id a))))))
  (shutdown-agents))
