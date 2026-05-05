(require '[ledger.core :as core])
(require '[ledger.db :as db])
(require '[datomic.client.api :as d])

(println "=== Bootstrap ===")
(def conn (core/system))
(println "Connected:" conn)

(println "\n=== Create Account ===")
(def tx (db/create-account! conn "Alice"))
(println "TempIDs:" (:tempids tx))

;; O tempid resolve para o entity ID. Precisamos consultar
;; o :account/id (UUID) a partir do entity ID.
(def eid (first (vals (:tempids tx))))
(println "Entity ID:" eid)

;; Buscar o account-id (UUID) via pull
(def dbval (d/db conn))
(def account (d/pull dbval '[:account/id :account/owner] eid))
(println "Account pulled:" account)
(def account-id (:account/id account))
(println "Account UUID:" account-id)

(println "\n=== Find Account ===")
(def found (db/find-account dbval account-id))
(println "Found:" found)

(println "\n=== Deposit 100 ===")
(def deposit-tx (db/process-transaction! conn account-id :tx.type/deposit 100.0))
(println "Deposit result:" (:success deposit-tx))

(println "\n=== Balance ===")
(def balance (db/get-balance (d/db conn) account-id))
(println "Balance:" balance)

(println "\n=== Withdraw 30 ===")
(def withdraw-tx (db/process-transaction! conn account-id :tx.type/withdraw 30.0))
(println "Withdraw success:" (:success withdraw-tx))
(def balance2 (db/get-balance (d/db conn) account-id))
(println "Balance after withdraw:" balance2)

(println "\n=== Withdraw 999 (should fail) ===")
(def bad-withdraw (db/process-transaction! conn account-id :tx.type/withdraw 999.0))
(println "Bad withdraw:" (select-keys bad-withdraw [:success :reason :balance]))

(println "\n=== Time Travel ===")
(def db-latest (d/db conn))
(def txs-for-as-of (db/list-transactions db-latest account-id))
(def as-of-instant ^java.util.Date
  (reduce (fn [^java.util.Date a t]
            (let [^java.util.Date b (:db/txInstant t)]
              (cond (nil? b) a
                    (nil? a) b
                    (.after b a) b
                    :else a)))
          nil
          txs-for-as-of))
(when as-of-instant
  (println "Balance as-of last tx instant:" (db/get-balance-as-of db-latest account-id as-of-instant)))

(println "\n=== List Transactions ===")
(def txs (db/list-transactions (d/db conn) account-id))
(println "Transaction count:" (count txs))
(doseq [t txs] (println "  " t))

(println "\n=== ALL OK ===")
