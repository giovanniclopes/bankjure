(ns ledger.logic-properties-test
  (:require [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [ledger.logic :as logic]))

(defspec sufficient-funds-equiv-100 100
  (prop/for-all [balance (gen/double* {:infinite? false :NaN? false})
                 amount (gen/double* {:infinite? false :NaN? false})]
    (= (logic/sufficient-funds? balance amount) (>= balance amount))))

(defspec validate-withdraw-sufficient-100 100
  (prop/for-all [balance (gen/double* {:min 0 :infinite? false :NaN? false})
                 raw-amt (gen/double* {:min 0 :infinite? false :NaN? false})]
    (let [withdraw (min raw-amt balance)]
      (= {:valid true} (logic/validate-withdraw balance withdraw)))))

(defspec validate-withdraw-insufficient-100 100
  (prop/for-all [balance (gen/double* {:min 0 :max 1e6 :infinite? false :NaN? false})
                 extra (gen/double* {:min 0.01 :max 1e6 :infinite? false :NaN? false})]
    (let [amount (+ balance extra)
          r (logic/validate-withdraw balance amount)]
      (and (false? (:valid r))
           (= "Insufficient funds" (:reason r))
           (= balance (:balance r))
           (= amount (:requested r))))))

(defspec build-deposit-tx-shape-100 100
  (prop/for-all [id gen/uuid
                 amt (gen/double* {:min 0.01 :max 1e12 :infinite? false :NaN? false})]
    (let [tx (logic/build-deposit-tx id amt)]
      (and (= [:account/id id] (:tx/account tx))
           (= :tx.type/deposit (:tx/type tx))
           (pos? (compare (:tx/amount tx) 0M))))))

(defspec build-withdraw-tx-shape-100 100
  (prop/for-all [id gen/uuid
                 amt (gen/double* {:min 0.01 :max 1e12 :infinite? false :NaN? false})]
    (let [tx (logic/build-withdraw-tx id amt)]
      (and (= [:account/id id] (:tx/account tx))
           (= :tx.type/withdraw (:tx/type tx))
           (pos? (compare (:tx/amount tx) 0M))))))
