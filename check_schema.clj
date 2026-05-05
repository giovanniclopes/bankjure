(require '[ledger.core :as core])
(require '[datomic.client.api :as d])

(def conn (core/system))
(def db (d/db conn))

;; Use d/pull with the ident keyword directly
(println ":tx/amount ->" (d/pull db '[:db/ident :db/valueType :db/cardinality] :tx/amount))
(println ":account/id ->" (d/pull db '[:db/ident :db/valueType] :account/id))
(println ":tx/type ->" (d/pull db '[:db/ident :db/valueType] :tx/type))
