(ns ledger.core
  "Immutable Ledger -- client management, database bootstrap, and system glue.
   Entry-point namespace for the application."
  (:require [datomic.client.api :as d]
            [ledger.schema :as schema]))

(def db-name "ledger")


;; -- Client Management -------------------------------------------------

(defonce ^:private client-instance (atom nil))

(defn- client
  "Returns or creates the Datomic Local client. Lazy init — the dev-local
   transactor starts implicitly on first use."
  []
  (when (nil? @client-instance)
    (reset! client-instance (d/client {:server-type :datomic-local
                                       :system      db-name
                                       :storage-dir (str (System/getProperty "user.dir") "/data")})))
  @client-instance)


;; -- Database Lifecycle ------------------------------------------------

(defn create-db!
  "Creates the ledger database. Idempotent — safe to call on an existing database."
  []
  (d/create-database (client) {:db-name db-name}))

(defn delete-db!
  "Deletes the ledger database and all its data."
  []
  (d/delete-database (client) {:db-name db-name}))

(defn connect
  "Connects to the ledger database and installs the schema.
   Returns a Datomic connection ready for queries and transactions."
  []
  (create-db!)
  (let [conn (d/connect (client) {:db-name db-name})]
    (schema/install! conn)
    conn))


;; -- System (convenience) ----------------------------------------------

(defn system
  "One-shot bootstrap: creates the database, connects, and installs the schema.
   Returns the connection."
  []
  (connect))
