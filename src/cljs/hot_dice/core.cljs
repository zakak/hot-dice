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
  (go 
    (dotimes [n (:spin-count config)]
      (swap! dice assoc-in [k :face] (game/rand-between min max))
      (<! (timeout (:spin-timeout config)))))
  (swap! dice assoc-in [k :n] (game/rand-between min max)))

(defn roll-all! []
  (doseq [[k die] (map-indexed vector @dice)]
    (when-not (:hold die)
      (roll-die! k die))))

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
   [:div {:class "pure-g"}
    [:div {:class "pure-u-3-3 center"}
     [dice-view]]]

   [:div {:class "pure-g"}
    [:div {:class "pure-1-3"}
     [:button {:class "pure-button"
               :on-click #(reset! dice (game/init-dice))}
      [:i {:class "fa fa-refresh"}]]]
    [:div {:class "pure-1-3"}
     [:span " "]]
    [:div {:class "pure-1-3"}
     [:button {:class "pure-button pure-button-primary"
               :on-click #(roll-all!)}
      [:i {:class "fa fa-random"}]]]]
   [:div {:class "footer"}
    [:a {:href "#/about"} "?"]]])

(defn about-page []
  [:div [:h2 "help"]])

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
