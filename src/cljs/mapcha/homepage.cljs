(ns mapcha.homepage
  (:require [goog.dom :as dom]
            [mapcha.map-utils :as map]))

(def plot-list
  [["Cambodia" "104.916667" "11.55"     "10.0"]
   ["Thailand" "100.483333" "13.75"     "10.0"]
   ["Vietnam"  "105.85"     "21.033333" "10.0"]
   ["Laos"     "102.6"      "17.966667" "10.0"]
   ["Myanmar"  "96.1"       "19.75"     "10.0"]])

(defn ^:export main []
  (map/init-overview-map "overview-map" plot-list)
  (dom/removeNode (dom/getElement "overview-map-wait-message")))
