(ns hot-dice.game-test
  (:require [clojure.test :refer [deftest is]]   
            [hot-dice.game :refer [of-a-kind hot-dice-scores? scores score-n-of-a-kind singles straight] :as game]))

(defn ->dice [& args]
  (map #(game/new-die :n %) args))

(defn nums [coll]
  (map :n coll))

(defn only-scores [scores]
  (set (map #(select-keys % [:type :score]) scores)))

(deftest test-straight
  (is (= [1 2 3 4 5] (-> (->dice 1 2 3 4 5) straight nums)))
  (is (= [6 5 4 3 2] (-> (->dice 6 5 4 3 2) straight nums)))
  
  (is (= [] (-> (->dice 1 1 1 1 1) straight nums)))
  (is (= [] (-> (->dice 1 2 3 4 6) straight nums))))

(deftest test-of-a-kind
  (is (= [1 1 1] (-> (->dice 1 1 1 5 5) of-a-kind nums)))
  (is (= [2 2 2 2] (-> (->dice 2 2 2 2 5) of-a-kind nums)))
  (is (= [3 3 3 3 3] (-> (->dice 3 3 3 3 3) of-a-kind nums)))
  
  (is (= [] (-> (->dice 1 2 3 4 5) of-a-kind nums)))
  (is (= [] (-> (->dice 1 1 4 5 5) of-a-kind nums))))

(deftest test-singles
  (is (= [1 1 5] (-> (->dice 1 1 4 5 6) singles nums)))
  (is (= [1 5 5] (-> (->dice 1 5 4 5 6) singles nums)))

  (is (= [] (-> (->dice 2 2 3 4 6) singles nums))))

(deftest test-core-n-of-a-kind
  (is (= 1000 (score-n-of-a-kind 1 3)))
  (is (= 200  (score-n-of-a-kind 2 3)))
  (is (= 300  (score-n-of-a-kind 3 3)))
  (is (= 400  (score-n-of-a-kind 4 3)))
  (is (= 500  (score-n-of-a-kind 5 3)))
  (is (= 600  (score-n-of-a-kind 6 3)))

  (is (= 2000 (score-n-of-a-kind 1 4)))
  (is (= 400  (score-n-of-a-kind 2 4)))
  (is (= 600  (score-n-of-a-kind 3 4)))
  (is (= 800  (score-n-of-a-kind 4 4)))
  (is (= 1000 (score-n-of-a-kind 5 4)))
  (is (= 1200 (score-n-of-a-kind 6 4)))

  (is (= 4000  (score-n-of-a-kind 1 5)))
  (is (= 800   (score-n-of-a-kind 2 5)))
  (is (= 1200  (score-n-of-a-kind 3 5)))
  (is (= 1600  (score-n-of-a-kind 4 5)))
  (is (= 2000  (score-n-of-a-kind 5 5)))
  (is (= 2400  (score-n-of-a-kind 6 5))))

(deftest test-score
  (is (= #{{:type :straight
            :score 1500}} (-> (->dice 1 2 3 4 5) scores only-scores)))

  (is (= #{{:type :of-a-kind
            :score 1000}} (-> (->dice 1 1 1 4 6) scores only-scores)))
  (is (= #{{:type :of-a-kind
            :score 300}} (-> (->dice 3 3 3 4 6) scores only-scores)))

  (is (= #{{:type :of-a-kind
            :score 2000}
           {:type :single
            :score 50}} (-> (->dice 1 1 1 1 5) scores only-scores)))

  (is (= #{{:type :single
            :score 200}
           {:type :single
            :score 50}} (-> (->dice 1 1 3 4 5) scores only-scores))))

(deftest test-hot-dice-score?
  (is (-> (->dice 1 2 3 4 5) scores hot-dice-scores?))
  (is (-> (->dice 1 1 1 1 5) scores hot-dice-scores?))

  (is (not (-> (->dice 1 1 1 4 6) scores hot-dice-scores?)))
  (is (not (-> (->dice 3 3 3 4 6) scores hot-dice-scores?)))
  (is (not (-> (->dice 1 1 3 4 5) scores hot-dice-scores?))))
