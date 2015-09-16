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
                 :spin-timeout 150
                 :type-format {:single ""
                               :straight "Straight "
                               :of-a-kind "Set of "}})

(defonce state (atom {:scoring-enabled true
                      :round 1
                      :mode :start}))

(defonce dice (atom (game/init-dice)))

;; -------------------------
;; Fn

(defn toggle-hold-die! [k die]
  (swap! dice update-in [k :hold] not))

(defn rand-timeout []
  (let [t-min (+ (:spin-timeout config) 50)
        t-max (- (:spin-timeout config) 50)]
    (timeout (game/rand-between t-min t-max))))

(defn roll-die! [k {:keys [min max] :as die}]
    (swap! dice assoc-in [k :rolling?] true)
    (go
      (dotimes [n (:spin-count config)]
        (swap! dice assoc-in [k :n] (game/rand-between min max))
        (<! (rand-timeout)))
      (swap! dice assoc-in [k :rolling?] false)))

(defn roll-all! []
  (doseq [[k die] (map-indexed vector @dice)]
    (when-not (:hold die)
      (roll-die! k die))))

(defmulti score-line :type)
(defmethod score-line :default [{:keys [type dice score]}]
  (str 
    (-> config :type-format type)
    " "
    (apply str (interpose ", " (map :n dice)))
    " = "
    score))

;; -------------------------
;; Views

(defn dice-view []
  [:ul {:class "dice"}
   (for [[k die] (map-indexed vector @dice)]
     [:li {:key k
           :class (when (:hold die) "hold")}
      [:img {:class "pure-image"
             :src (str "image/die" (:n die) ".svg")
             :on-click #(toggle-hold-die! k die)}]])])

(defn not-rolling? [dice]
  (every? #(= false %) (map :rolling? dice)))

(defn scoring-view []
  (let [scores (game/scores @dice)]
    [:div {:class "row scoring"}
     [:div {:class "col-xs-12 text-center"}
      [:ul {:class "list"}
       (for [[i score] (map-indexed vector scores)]
         [:li {:key i} (score-line score)])]
      (when (game/hot-dice-scores? scores)
        [:button {:class "btn btn-danger fa fa-2x fa-fire"}])]]))

(defmulti controls-view #(:scoring-enabled @state))

(defmethod controls-view true []
  [:div
   [:div {:class "row"}
    [:div {:class "col-xs-12 text-center"}
     [:button {:class "btn btn-default fa fa-2x fa-refresh"
               :on-click #(reset! dice (game/init-dice))}]
     " "
     [:button {:class "btn btn-default fa fa-2x fa-random"
               :on-click #(roll-all!)}]]]
   (when (not-rolling? @dice)
     [scoring-view])])

(defmethod controls-view :default []
  [:div {:class "row"}
   [:div {:class "col-xs-12 text-center"}
    [:button {:class "btn btn-default fa fa-2x fa-refresh"
              :on-click #(reset! dice (game/init-dice))}]
    " "
    [:button {:class "btn btn-default fa fa-2x fa-random"
              :on-click #(roll-all!)}]]])

(defn home-page []
  [:div
   [:div {:class "row"}
    [:div {:class "col-xs-12 text-center"}
     [dice-view]]]

   [:div {:class "controls"}
    [controls-view]]

   [:div {:class "footer"}
    [:a {:href "#/about"} "?"]]])

(defn about-page []
  [:div [:h2 "about"]
   [:p
    [:a {:href "#/"} "back to the game"]]
   [:p [:h3 "scoring"]

    [:ul {:class "list"}
     [:li (str "straight " (-> game/config :straight-score))]]

    [:ul {:class "list"}
     [:li (str "1s x " (get-in game/config [:singles 1]))]
     [:li (str "5s x " (get-in game/config [:singles 5]))]]

    [:ul {:class "list"}
     [:li (str "1, 1, 1 = " (game/score-n-of-a-kind 1 3))]
     [:li (str "2, 2, 2 = " (game/score-n-of-a-kind 2 3))]
     [:li (str "3, 3, 3 = " (game/score-n-of-a-kind 3 3))]
     [:li "..."]]

    [:ul {:class "list"}
     [:li (str "1, 1, 1, 1 = " (game/score-n-of-a-kind 1 4))]
     [:li (str "2, 2, 2, 2 = " (game/score-n-of-a-kind 2 4))]
     [:li (str "3, 3, 3, 3 = " (game/score-n-of-a-kind 3 4))]
     [:li "..."]]

    [:ul {:class "list"}
     [:li (str "1, 1, 1, 1, 1 = " (game/score-n-of-a-kind 1 5))]
     [:li (str "2, 2, 2, 2, 2 = " (game/score-n-of-a-kind 2 5))]
     [:li (str "3, 3, 3, 3, 3 = " (game/score-n-of-a-kind 3 5))]
     [:li "..."]]
     
    [:label "Scoring enabled? "
     [:input {:type "checkbox"
              :checked (:scoring-enabled @state)
              :on-change #(swap! state update :scoring-enabled not)}]]]])

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
