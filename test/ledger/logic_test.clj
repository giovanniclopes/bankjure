(ns ledger.logic-test
  "Unit tests for ledger.logic -- pure functions only, no database."
  (:require [clojure.test :refer :all]
            [ledger.logic :as logic]))


;; -- RF01: Account Creation --------------------------------------------

(deftest test-build-account-tx
  (testing "Generated transaction map structure"
    (let [tx (logic/build-account-tx "Alice")]
      (is (string? (:account/owner tx)))
      (is (= "Alice" (:account/owner tx)))
      (is (uuid? (:account/id tx)))
      (is (string? (:db/id tx)))
      (is (clojure.string/starts-with? (:db/id tx) "new-account-")))))

;; -- RF02: Withdraw Validation -----------------------------------------

(deftest test-sufficient-funds?
  (testing "Balance >= amount => true"
    (is (true? (logic/sufficient-funds? 100.0 50.0)))
    (is (true? (logic/sufficient-funds? 100.0 100.0)))
    (is (true? (logic/sufficient-funds? 0.0 0.0))))
  (testing "Balance < amount => false"
    (is (false? (logic/sufficient-funds? 0.0 50.0)))
    (is (false? (logic/sufficient-funds? 100.0 150.0)))))

(deftest test-validate-withdraw
  (testing "Sufficient balance returns :valid true"
    (is (= {:valid true} (logic/validate-withdraw 100.0 50.0)))
    (is (= {:valid true} (logic/validate-withdraw 100.0 100.0))))
  (testing "Insufficient balance returns :valid false with reason"
    (let [result (logic/validate-withdraw 100.0 200.0)]
      (is (false? (:valid result)))
      (is (= "Insufficient funds" (:reason result)))
      (is (= 100.0 (:balance result)))
      (is (= 200.0 (:requested result))))))

;; -- Transaction Fact Builders -----------------------------------------

(deftest test-build-deposit-tx
  (let [id (java.util.UUID/randomUUID)
        tx (logic/build-deposit-tx id 150.0)]
    (is (= [:account/id id] (:tx/account tx)))
    (is (= :tx.type/deposit (:tx/type tx)))
    (is (= 150M (:tx/amount tx)))))

(deftest test-build-withdraw-tx
  (let [id (java.util.UUID/randomUUID)
        tx (logic/build-withdraw-tx id 75.0)]
    (is (= [:account/id id] (:tx/account tx)))
    (is (= :tx.type/withdraw (:tx/type tx)))
    (is (= 75M (:tx/amount tx)))))
