(ns hot-dice.prod
  (:require [hot-dice.core :as core]))

;;ignore println statements in prod
(set! *print-fn* (fn [& _]))

(core/init!)
