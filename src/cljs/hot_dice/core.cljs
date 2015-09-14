(ns hot-dice.core
    (:require [reagent.core :as reagent :refer [atom]]
              [reagent.session :as session]
              [secretary.core :as secretary :include-macros true]
              [goog.events :as events]
              [goog.history.EventType :as EventType]
              [hot-dice.game :as game]
              [cljs.core.async :as async :refer [<! >! chan put! close! timeout]])
    (:import goog.History)
    (:require-macros [cljs.core.async.macros :refer [go]]))

(enable-console-print!)

;; -------------------------
;; Data

(defonce config {:spin-count 5
                 :spin-timeout 100})

(defonce state
  (atom {:round 1
         :mode :start}))

;; -------------------------
;; Fn

(defonce dice (atom (game/init-dice)))

(defn toggle-hold-die! [k die]
  (swap! dice update-in [k :hold] not))

(defn roll-die! [k {:keys [min max] :as die}]
  (swap! dice assoc-in [k :rolling] true)
  (swap! dice assoc-in [k :n] (game/rand-between min max))
  (go
    (dotimes [n (:spin-count config)]
      (swap! dice assoc-in [k :face] (game/rand-between min max))
      (<! (timeout (:spin-timeout config))))
    (swap! dice update-in [k] #(assoc % :face (:n %)))
    (swap! dice assoc-in [k :rolling] false)))

(defn roll-all! []
  (doseq [[k die] (map-indexed vector @dice)]
    (when-not (:hold die)
      (roll-die! k die)))
  (when-let [s (seq (game/score @dice))]
    (prn (sort (map :n @dice)))
    (prn s)))

(defmulti score-line :type)
(defmethod score-line :default [score]
  (str 
    (name (:type score))
    " "
    (apply str (interpose ", " (map :n (:dice score))))
    " = "
    (:score score)))

;; -------------------------
;; Views

(defn dice-view []
  [:ul {:class "dice"}
   (for [[k die] (map-indexed vector @dice)]
     [:li {:key k
           :class (when (:hold die) "hold")}
      [:img {:class "pure-image"
             :src (str "image/die" (:face die) ".svg")
             :on-click #(toggle-hold-die! k die)}]])])

(defn home-page []
  [:div
   [:div {:class "row"}
    [:div {:class "col-xs-12 text-center"}
     [dice-view]]]

   [:div {:class "row"}
    [:div {:class "col-xs-12 text-center"}
     [:button {:class "btn btn-default fa fa-2x fa-refresh"
               :on-click #(reset! dice (game/init-dice))}]
     " "
     [:button {:class "btn btn-primary fa fa-2x fa-random"
               :on-click #(roll-all!)}]]]

   [:div {:class "row"}
    [:div {:class "col-xs-12 text-center"}
     (when (every? #(= false %) (map :rolling @dice))
       [:ul {:class "list"}
        (for [[i score] (map-indexed vector (game/score @dice))]
          [:li {:key i} (score-line score)])])]]

   [:div {:class "footer"}
    [:a {:href "#/about"} "?"]]])

(defn about-page []
  [:div [:h2 "about"]
   [:p
    [:a {:href "#/"} "back to the game"]]
   [:p [:h3 "scoring"]
    [:ul {:class "list"}
     [:li (str "straight " (-> game/config :straight-score))]
     [:li (str "1s x " (get-in game/config [:singles 1]))]
     [:li (str "5s x " (get-in game/config [:singles 5]))]
     [:li (str "3 or more 1s of a kind " (get-in game/config [:3-of-a-kind 1]))]
     [:li (str "3 or more 2s of a kind " (get-in game/config [:3-of-a-kind 2]))]
     [:li (str "3 or more 3s of a kind " (get-in game/config [:3-of-a-kind 3]))]
     [:li (str "3 or more 4s of a kind " (get-in game/config [:3-of-a-kind 4]))]
     [:li (str "3 or more 5s of a kind " (get-in game/config [:3-of-a-kind 5]))]
     [:li (str "3 or more 6s of a kind " (get-in game/config [:3-of-a-kind 6]))]]]])

(defn current-page []
  [:div [(session/get :current-page)]])

;; -------------------------
;; Routes
(secretary/set-config! :prefix "#")

(secretary/defroute "/" []
  (session/put! :current-page #'home-page))

(secretary/defroute "/about" []
  (session/put! :current-page #'about-page))

;; -------------------------
;; History
;; must be called after routes have been defined
(defn hook-browser-navigation! []
  (doto (History.)
    (events/listen
     EventType/NAVIGATE
     (fn [event]
       (secretary/dispatch! (.-token event))))
    (.setEnabled true)))

;; -------------------------
;; Initialize app
(defn mount-root []
  (reagent/render [current-page] (.getElementById js/document "app")))

(defn init! []
  (hook-browser-navigation!)
  (mount-root))
