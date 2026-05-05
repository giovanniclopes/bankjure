(ns ledger.db
  "Database operations -- Datalog queries and transactional writes.
   Depends on ledger.logic for pure validation."
  (:require [datomic.client.api :as d]
            [ledger.logic :as logic]))


;; -- RF01: Create Account ----------------------------------------------

(defn create-account!
  "Transacts a new account entity and returns the Datomic tx result."
  [conn owner]
  (let [tx-data (logic/build-account-tx owner)]
    (d/transact conn {:tx-data [tx-data]})))


;; -- RF03: Balance Calculation (the Reduce) ----------------------------

(defn get-balance
  "Calculates the current balance for an account by summing all deposits
   and subtracting all withdrawals via Datalog aggregation.

   Uses two sub-queries; the client API only supports find-rel elements,
   so we use plain aggregates (not find-tuple or find-scalar)."
  [db account-id]
  (let [lookup    [:account/id account-id]
        deposits  (ffirst
                    (d/q '[:find (sum ?amount)
                           :in $ ?a
                           :where [?tx :tx/account ?a]
                                  [?tx :tx/type :tx.type/deposit]
                                  [?tx :tx/amount ?amount]]
                         db lookup))
        withdraws (ffirst
                    (d/q '[:find (sum ?amount)
                           :in $ ?a
                           :where [?tx :tx/account ?a]
                                  [?tx :tx/type :tx.type/withdraw]
                                  [?tx :tx/amount ?amount]]
                         db lookup))]
    (- (or deposits 0M) (or withdraws 0M))))


;; -- RF04: Time-Travel Audit -------------------------------------------

(defn get-balance-as-of
  "Returns the balance of an account at a specific point in time.

   Uses Datomic's d/as-of to project the database to a past state,
   then delegates to get-balance. The instant should be a java.util.Date.

   This enables full historical auditing -- 'viagem no tempo'."
  [db account-id instant]
  (let [historical-db (d/as-of db instant)]
    (get-balance historical-db account-id)))


;; -- RF02: Process Transaction (validated) -----------------------------

(defn process-transaction!
  "Processes a financial transaction after validating business rules.

   For deposits:  always accepted.
   For withdrawals: checks sufficient funds via ledger.logic/validate-withdraw.

   Returns {:success true,  :tx-result <datomic-result>}
        or {:success false, :reason \"...\", :balance <current>}"
  [conn account-id type amount]
  (let [db (d/db conn)]
    (if (= type :tx.type/withdraw)
      (let [balance    (get-balance db account-id)
            validation (logic/validate-withdraw balance amount)]
        (if (:valid validation)
          (let [tx-data (logic/build-withdraw-tx account-id amount)
                result  (d/transact conn {:tx-data [tx-data]})]
            {:success true :tx-result result})
          {:success false
           :reason  (:reason validation)
           :balance balance}))
      ;; deposit -- always accepted
      (let [tx-data (logic/build-deposit-tx account-id amount)
            result  (d/transact conn {:tx-data [tx-data]})]
        {:success true :tx-result result}))))


;; -- Convenience Queries -----------------------------------------------

(defn find-account
  "Looks up an account entity by its :account/id.
   Returns the pulled entity map or nil.

   Two-step: first resolve the entity ID via query,
   then pull attributes via d/pull (client API compatible)."
  [db account-id]
  (let [eid (ffirst
              (d/q '[:find ?e
                     :in $ ?id
                     :where [?e :account/id ?id]]
                   db account-id))]
    (when eid
      (d/pull db '[:account/id :account/owner] eid))))

(defn list-transactions
  "Returns all transaction facts for an account, unordered.

   Resolves :db/txInstant from the assertion datom's transaction entity."
  [db account-id]
  (let [rows (d/q '[:find ?tx ?inst
                    :in $ ?a
                    :where [?tx :tx/account ?a]
                           [?tx :tx/amount _ ?t true]
                           [?t :db/txInstant ?inst]]
                  db [:account/id account-id])]
    (mapv (fn [[eid inst]]
            (assoc (d/pull db '[:tx/type :tx/amount] eid)
                   :db/txInstant inst))
          rows)))
