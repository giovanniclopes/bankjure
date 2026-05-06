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
