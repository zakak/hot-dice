(ns hot-dice.game)

(def config {:dice-count 5
             :default-die-num 6
             :straight-min 5
             :of-a-kind-min 3})

(defn rand-between [x y]
  (+ (rand-int (- (inc y) x)) x))

(defn new-die
  ([] (new-die (:default-die-num config)))
  ([n]
   {:min 1
    :max 6
    :n n
    :face n
    :hold false}))

(defn init-dice 
  ([] (init-dice (:dice-count config)))
  ([n]
   (into [] (repeatedly n new-die))))

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
