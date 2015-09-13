(ns hot-dice.handler
  (:require [compojure.core :refer [GET defroutes]]
            [compojure.route :refer [not-found resources]]
            [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
            [hiccup.core :refer [html]]
            [hiccup.page :refer [include-js include-css]]
            [prone.middleware :refer [wrap-exceptions]]
            [ring.middleware.reload :refer [wrap-reload]]
            [environ.core :refer [env]]))

(def home-page
  (html
   [:html
    [:head
     [:title "hot dice"]
     [:meta {:charset "utf-8"}]
     [:meta {:name "viewport"
             :content "width=device-width, initial-scale=1"}]
     (include-css "//yui.yahooapis.com/pure/0.6.0/pure-min.css")
     (include-css "https://maxcdn.bootstrapcdn.com/font-awesome/4.4.0/css/font-awesome.min.css")
     (include-css (if (env :dev) "css/site.css" "css/site.min.css"))]
    [:body
     [:div#app
      [:p "Loading ..."]]
     (include-js "js/app.js")]]))

(defroutes routes
  (GET "/" [] home-page)
  (resources "/")
  (not-found "Not Found"))

(def app
  (let [handler (wrap-defaults #'routes site-defaults)]
    (if (env :dev) (-> handler wrap-exceptions wrap-reload) handler)))
