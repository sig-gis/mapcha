(ns mapcha.admin
  (:require [goog.dom :as dom]
            [mapcha.map-utils :as map]))

(defn ^:export main []
  (map/digitalglobe-base-map {:div-name      "new-project-map"
                              :center-coords [102.0 17.0]
                              :zoom-level    5}))
