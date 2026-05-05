(ns ledger.logic
  "Pure business logic -- no side effects, no database access.
   Every function here takes values and returns values.")

(defn- ->amount
  [x]
  (cond (instance? java.math.BigDecimal x) x
        (number? x) (bigdec x)
        :else (throw (ex-info "Amount must be numeric" {:value x}))))

;; -- RF01: Account Creation --------------------------------------------

(defn build-account-tx
  "Pure function: given an owner name, returns a Datomic transaction map
   for creating a new Account entity.

   Input:  owner (string)
   Output: Datomic transaction map with generated :account/id"
  [owner]
  {:db/id         (str "new-account-" (java.util.UUID/randomUUID))
   :account/id    (java.util.UUID/randomUUID)
   :account/owner owner})


;; -- RF02: Transaction Validation --------------------------------------

(defn sufficient-funds?
  "Predicate: does the current balance cover the requested amount?
   balance and amount should be numeric."
  [balance amount]
  (>= balance amount))

(defn validate-withdraw [current-balance amount]
  (if (sufficient-funds? current-balance amount)
    {:valid true}
    {:valid false
     :reason "Insufficient funds"
     :balance current-balance
     :requested amount}))

;; -- Transaction Fact Builders -----------------------------------------

(defn build-deposit-tx
  "Builds a deposit transaction fact map.
   Uses lookup-ref [:account/id ...] to target the account."
  [account-id amount]
  {:tx/account [:account/id account-id]
   :tx/type    :tx.type/deposit
   :tx/amount  (->amount amount)})

(defn build-withdraw-tx
  "Builds a withdrawal transaction fact map."
  [account-id amount]
  {:tx/account [:account/id account-id]
   :tx/type    :tx.type/withdraw
   :tx/amount  (->amount amount)})
