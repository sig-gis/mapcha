(ns mapcha.homepage
  (:require [mapcha.map-utils :as map]))

(def location-records
  [["Cambodia" "104.916667" "11.55"    ]
   ["Thailand" "100.483333" "13.75"    ]
   ["Vietnam"  "105.85"     "21.033333"]
   ["Laos"     "102.6"      "17.966667"]
   ["Myanmar"   "96.1"      "19.75"    ]])

(defn ^:export main []
  (map/init-overview-map "overview-map" location-records))
