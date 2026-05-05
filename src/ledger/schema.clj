(ns ledger.schema
  "Datomic schema definitions for the immutable ledger.
   Defines atomic facts for Account and Transaction entities."
  (:require [datomic.client.api :as d]))

(def attributes
  "Schema attributes as data. Transacted once at database creation."
  [{:db/ident       :account/id
    :db/valueType   :db.type/uuid
    :db/cardinality :db.cardinality/one
    :db/unique      :db.unique/identity
    :db/doc         "Unique account identifier"}

   {:db/ident       :account/owner
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc         "Account owner name"}

   {:db/ident       :tx/account
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/one
    :db/doc         "Reference to the account entity"}

   {:db/ident       :tx/type
    :db/valueType   :db.type/keyword
    :db/cardinality :db.cardinality/one
    :db/doc         "Transaction type — :tx.type/deposit or :tx.type/withdraw"}

   {:db/ident       :tx/amount
    :db/valueType   :db.type/bigdec
    :db/cardinality :db.cardinality/one
    :db/doc         "Transaction amount"}])

(defn install!
  "Transacts the schema attributes into Datomic.
   Idempotent — safe to call on an existing database."
  [conn]
  (d/transact conn {:tx-data attributes}))
