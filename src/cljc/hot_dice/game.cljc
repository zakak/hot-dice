(ns hot-dice.game)

(def config {:dice-count 5
             :default-die-num 6
             :straight-min 5
             :straight-score 1500
             :of-a-kind-min 3
             :3-of-a-kind {1 1000
                         2 200
                         3 300
                         4 400
                         5 500
                         6 600}
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
       :face n
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

(defn- score-of-a-kind [dice]
  (when-let [coll (seq (of-a-kind dice))]
    (let [n (-> coll first :n)
          total (count coll)]
      [{:type :of-a-kind
        :score (get (:3-of-a-kind config) n)
        :dice coll}])))

(defn- other-score [dice]
  (set (filter seq (concat (score-singles dice)
                           (score-of-a-kind dice)))))

(defn score [dice]
  (if-let [coll (seq (straight dice))]
    #{{:type :straight
       :score (:straight-score config)
       :dice coll}}
    (other-score dice)))
