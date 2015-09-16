(ns hot-dice.game
  (:require #?(:clj [clojure.core.match :refer [match]]
               :cljs [cljs.core.match :refer-macros [match]])))

(def config {:dice-count 5
             :default-die-num 6
             :straight-min 5
             :straight-score 1500
             :of-a-kind-min 3
             :singles {1 100
                       5 50}})

(defn rand-between [x y]
  (+ (rand-int (- (inc y) x)) x))

(defn new-die [& {:as opts}]
  (let [n (:default-die-num config)]
    (merge
      {:min 1
       :max 6
       :n n
       :hold false}
      opts)))

(defn init-dice 
  ([] (init-dice (:dice-count config)))
  ([n]
   (into [] (for [n (range n)]
              (new-die :key n)))))

(defn straight
  ([dice] (straight dice (:straight-min config)))
  ([dice min-count]
   (let [nums (set (map :n dice))
         min (apply min nums)
         max (apply max nums)
         range (set (range min (inc max)))]
     (if (and (>= (count range) min-count)
                (= range nums))
       dice
       []))))

(defn of-a-kind
  ([dice] (of-a-kind dice (:of-a-kind-min config)))
  ([dice min-count]
   (let [found (->> dice
                    (group-by :n)
                    (filter #(>= (count (second %)) min-count)))]
     (if (seq found)
       (-> found last last)
       []))))

(defn singles
  ([dice] (singles dice (:singles config)))
  ([dice specials]
   (filter :score
           (map #(assoc % :score
                        (get specials (:n %))) dice))))

(defn- score-singles [dice]
  (when-let [coll (seq (singles dice))]
    (for [[_ nums] (->> coll (group-by :n))]
      {:type :single
       :score (reduce + 0 (map :score nums))
       :dice nums})))

(defn score-n-of-a-kind [n total]
  (match [n total]
         [1 3] 1000
         [_ 3] (* 100 n)
         [1 4] 2000
         [_ 4] (* 100 2 n)
         [1 5] 4000
         [_ 5] (* 100 4 n)
         :else nil))

(defn- score-of-a-kind [dice]
  (when-let [coll (seq (of-a-kind dice))]
    (let [n (-> coll first :n)
          total (count coll)]
      [{:type :of-a-kind
        :score (score-n-of-a-kind n total)
        :dice coll}])))

(defn- other-score [dice]
  (let [kind (score-of-a-kind dice)
        sing (score-singles dice)]
    (-> (cond
          (and (seq kind)
               (seq sing)) (concat kind
                                   (remove #(== (-> % :dice first :n)
                                                (-> kind first :dice first :n)) sing))
          (seq kind) kind
          (seq sing) sing
          :else [])
        (set))))

(defn hot-dice-scores? [scores]
  (>= (-> (mapcat :dice scores) count) (:dice-count config)))

(defn scores [dice]
  (if-let [coll (seq (straight dice))]
    #{{:type :straight
       :score (:straight-score config)
       :dice coll}}
    (other-score dice)))
