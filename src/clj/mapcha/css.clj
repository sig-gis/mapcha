(ns mapcha.css
  (:require [garden.core  :refer [css]]
            [garden.units :refer [px]]))

(defn wrap-http-response
  [body]
  {:status  200
   :headers {"Content-Type" "text/css"}
   :body    body})

(defn make-stylesheet [request]
  (wrap-http-response
   (css [:h1 {:font-size (px 16)}])))
