(ns ledger.db-integration-test
  (:require [clojure.test :refer [deftest is]]
            [datomic.client.api :as d]
            [ledger.db :as db]
            [ledger.schema :as schema]))

(defn- connect-isolated []
  (let [dir (str (System/getProperty "java.io.tmpdir")
                 "/bankjure-integ-" (random-uuid))
        sys (str "integ-sys-" (random-uuid))
        client (d/client {:server-type :datomic-local
                          :system      sys
                          :storage-dir dir})]
    (d/create-database client {:db-name "ledger"})
    (let [conn (d/connect client {:db-name "ledger"})]
      (schema/install! conn)
      {:conn conn})))

(deftest balance-sequence-deposit-withdraw-deposit
  (let [{:keys [conn]} (connect-isolated)
        acct (db/create-account-returning! conn "P")
        account-id (:account/id acct)]
    (db/process-transaction! conn account-id :tx.type/deposit 50)
    (is (= (java.math.BigDecimal. "50") (db/get-balance (d/db conn) account-id)))
    (db/process-transaction! conn account-id :tx.type/withdraw 50)
    (is (= (java.math.BigDecimal. "0") (db/get-balance (d/db conn) account-id)))
    (db/process-transaction! conn account-id :tx.type/deposit 50)
    (is (= (java.math.BigDecimal. "50") (db/get-balance (d/db conn) account-id)))))

(deftest balance-as-of-matches-current-after-tx
  (let [{:keys [conn]} (connect-isolated)
        acct (db/create-account-returning! conn "T")
        account-id (:account/id acct)
        dbv (d/db conn)]
    (is (= 0M (db/get-balance-as-of dbv account-id (java.util.Date. 0))))
    (db/process-transaction! conn account-id :tx.type/deposit 33)
    (let [db2     (d/db conn)
          last-tx (ffirst
                   (d/q '[:find (max ?i)
                          :where [?e :db/txInstant ?i]]
                        db2))]
      (is (some? last-tx))
      (is (= (db/get-balance db2 account-id)
             (db/get-balance-as-of db2 account-id last-tx))))))
