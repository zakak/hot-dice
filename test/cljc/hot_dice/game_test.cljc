(ns hot-dice.game-test
  (:require [hot-dice.game :refer [straight of-a-kind] :as game]
            #?(:clj [clojure.test :refer [deftest is]]
               :cljs [cemerick.cljs.test :refer-macros [is are deftest testing use-fixtures done]])))

(defn dice [& args]
  (map game/new-die args))

(defn nums [coll]
  (map :n coll))

(deftest test-straight
  (is (= #{1 2 3 4 5} (-> (dice 1 2 3 4 5) straight nums set)))
  (is (= #{2 3 4 5 6} (-> (dice 6 5 4 3 2) straight nums set)))
  
  (is (= #{} (-> (dice 1 2 3 4 6) straight nums set)))
  (is (= #{} (-> (dice 1 2 3 4) straight nums set))))

(deftest test-of-a-kind
  (is (= [1 1 1] (-> (dice 1 1 1 5 5) of-a-kind nums)))
  (is (= [2 2 2 2] (-> (dice 2 2 2 2 5) of-a-kind nums)))
  (is (= [3 3 3 3 3] (-> (dice 3 3 3 3 3) of-a-kind nums)))
  
  (is (= [] (-> (dice 1 2 3 4 5) of-a-kind nums)))
  (is (= [] (-> (dice 1 1 4 5 5) of-a-kind nums))))
