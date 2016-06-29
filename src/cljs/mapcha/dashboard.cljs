(ns mapcha.dashboard
  (:require [goog.dom :as dom]
            [mapcha.map-utils :as map]))

(defn ^:export main []
  (map/mapquest-base-map {:div-name      "image-analysis-pane"
                          :center-coords [102.0 17.0]
                          :zoom-level    5}))
