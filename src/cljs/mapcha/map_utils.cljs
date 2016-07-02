(ns mapcha.map-utils
  (:require [goog.dom :as dom]
            [ol.proj]
            [ol.Map]
            [ol.layer.Tile]
            [ol.layer.Vector]
            [ol.source.MapQuest]
            [ol.source.Vector]
            [ol.source.XYZ]
            [ol.control]
            [ol.control.ScaleLine]
            [ol.View]
            [ol.Feature]
            [ol.geom.Point]
            [ol.geom.Circle]
            [ol.style.Style]
            [ol.style.Icon]
            [ol.style.Fill]
            [ol.style.Stroke]
            [ol.Overlay]
            [ol.interaction.Select]
            [ol.interaction.Translate]
            [ol.extent]
            [ol.format.GeoJSON]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;
;;; Create the default OpenLayers Map Object used on all pages
;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defonce map-ref (atom nil))

(defn digitalglobe-base-map [{:keys [div-name center-coords zoom-level]}]
  (let [digital-globe-access-token   (str "pk.eyJ1IjoiZGlnaXRhbGdsb2JlIiwiYS"
                                          "I6ImNpcTJ3ZTlyZTAwOWNuam00ZWU3aTk"
                                          "xdWIifQ.9OFrmevVe0YB2dJokKhhdA")
        recent-imagery-url           "digitalglobe.nal0g75k"
        recent-imagery-+-streets-url "digitalglobe.nal0mpda"]
    (->>
     (js/ol.Map.
      #js {:target div-name
           :layers #js [(js/ol.layer.Tile.
                         #js {:title "DigitalGlobe Maps API: Recent Imagery"
                              :source (js/ol.source.XYZ.
                                       #js {:url (str
                                                  "http://api.tiles.mapbox.com/v4/"
                                                  recent-imagery-url
                                                  "/{z}/{x}/{y}.png?access_token="
                                                  digital-globe-access-token)
                                            :attribution "© DigitalGlobe, Inc"})})
                        (js/ol.layer.Tile.
                         #js {:title "DigitalGlobe Maps API: Recent Imagery+Streets"
                              :source (js/ol.source.XYZ.
                                       #js {:url (str
                                                  "http://api.tiles.mapbox.com/v4/"
                                                  recent-imagery-+-streets-url
                                                  "/{z}/{x}/{y}.png?access_token="
                                                  digital-globe-access-token)
                                            :attribution "© DigitalGlobe, Inc"})})]
           :controls (.extend (js/ol.control.defaults)
                              #js [(js/ol.control.ScaleLine.)])
           :view (js/ol.View.
                  #js {:projection "EPSG:3857"
                       :center (js/ol.proj.fromLonLat (clj->js center-coords))
                       :zoom zoom-level})})
     (reset! map-ref))))

(defn zoom-map-to-layer [map layer]
  (let [view   (.getView map)
        size   (.getSize map)
        extent (.getExtent (.getSource layer))]
    (.fit view extent size)))

(def styles
  {:point   (js/ol.style.Style.
             #js {:image (js/ol.style.Icon. #js {:src "/favicon.ico"})})
   :polygon (js/ol.style.Style.
             #js {:fill   (js/ol.style.Fill.
                           #js {:color "rgba(200,200,200,0.2)"})
                  :stroke (js/ol.style.Stroke.
                           #js {:color "#8b2323"
                                :width 3})})})

(defonce current-boundary (atom nil))

(defn draw-polygon [polygon]
  (let [geometry (-> (js/ol.format.GeoJSON.)
                     (.readGeometry polygon)
                     (.transform "EPSG:4326" "EPSG:3857"))
        polygon  (js/ol.layer.Vector.
                  #js {:source (js/ol.source.Vector.
                                #js {:features #js [(js/ol.Feature.
                                                     #js {:geometry geometry})]})
                       :style  (styles :polygon)})]
    (when @current-boundary
      (.removeLayer @map-ref @current-boundary))
    (reset! current-boundary polygon)
    (doto @map-ref
      (.addLayer polygon)
      (zoom-map-to-layer polygon))))

(defonce current-buffer (atom nil))

(defn draw-buffer [center radius]
  (let [coordinates (-> (js/ol.format.GeoJSON.)
                        (.readGeometry center)
                        (.transform "EPSG:4326" "EPSG:3857")
                        (.getCoordinates))
        buffer      (js/ol.layer.Vector.
                     #js {:source (js/ol.source.Vector.
                                   #js {:features #js [(js/ol.Feature.
                                                        #js {:geometry
                                                             (js/ol.geom.Circle.
                                                              coordinates
                                                              radius)})]})
                          :style  (styles :polygon)})]
    (when @current-buffer
      (.removeLayer @map-ref @current-buffer))
    (reset! current-buffer buffer)
    (doto @map-ref
      (.addLayer buffer)
      (zoom-map-to-layer buffer))))

;;;;;;;;;;;;;;;;;;;;;;; IWAP CODE BELOW HERE ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;
;;; Lon/Lat Reprojection
;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; Default map projection for OpenLayers is "Web Mercator"
;; (EPSG:3857), which is also the default for OpenStreetMap, Google
;; Maps, MapQuest, and Bing Maps.

(defn reproject-to-map [longitude latitude]
  (js/ol.proj.transform #js [(js/Number longitude) (js/Number latitude)]
                        "EPSG:4326"
                        "EPSG:3857"))

(defn reproject-from-map [x y]
  (js/ol.proj.transform #js [(js/Number x) (js/Number y)]
                        "EPSG:3857"
                        "EPSG:4326"))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;
;;; Create the default OpenLayers Map Object used on all pages
;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn mapquest-base-map [{:keys [div-name center-coords zoom-level]}]
  (js/ol.Map.
   #js {:target div-name
        :layers #js [(js/ol.layer.Tile.
                      #js {:source (js/ol.source.MapQuest. #js {:layer "sat"})})
                     (js/ol.layer.Tile.
                      #js {:source (js/ol.source.MapQuest. #js {:layer "hyb"})})]
        :controls (.extend (js/ol.control.defaults)
                           #js [(js/ol.control.ScaleLine.)])
        :view (js/ol.View.
               #js {:projection "EPSG:3857"
                    :center (js/ol.proj.fromLonLat (clj->js center-coords))
                    :zoom zoom-level})}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;
;;; Create the OpenLayers Map Object for the New Report Map
;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defonce map-ref (atom nil))

(defn init-map [div-name]
  (reset! map-ref (mapquest-base-map {:div-name      div-name
                                      :center-coords [-97.3426776 37.6906938]
                                      :zoom-level    4})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;
;;; Create the OpenLayers Map Object for the Overview Map
;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defonce overview-map-ref (atom nil))

(defn create-address-overview-layer [report-addresses]
  (let [address-points (for [[address longitude latitude combined-score]
                             report-addresses]
                         (js/ol.Feature.
                          #js {:geometry (js/ol.geom.Point.
                                          (reproject-to-map longitude latitude))
                               :address  address
                               :score    combined-score}))]
    (js/ol.layer.Vector.
     #js {:source (js/ol.source.Vector.
                   #js {:features (clj->js address-points)})
          :style  (js/ol.style.Style.
                   #js {:image (js/ol.style.Icon. #js {:src "/favicon.ico"})})})))

(defn show-address-popup [overlay feature evt]
  (set! (.-innerHTML (dom/getElement "popup-content"))
        (str "<ul><li><span class=\"popup-label\">Address: </span>"
             (.get feature "address")
             "</li><li><span class=\"popup-label\">Score: </span>"
             (.get feature "score")
             "</li></ul>"))
  (.setPosition overlay (.-coordinate evt)))

(defn hide-address-popup [overlay]
  (.setPosition overlay js/undefined))

(defn init-overview-map [div-name report-addresses]
  (let [overview-map    (mapquest-base-map {:div-name      div-name
                                            :center-coords [-97.3426776 37.6906938]
                                            :zoom-level    4})
        address-layer   (create-address-overview-layer report-addresses)
        address-overlay (js/ol.Overlay. #js {:element (dom/getElement "popup")})]
    (reset! overview-map-ref
            (doto overview-map
              (.addLayer address-layer)
              (.addOverlay address-overlay)
              (zoom-map-to-layer address-layer)
              (.on "click"
                   (fn [evt]
                     (if-let [feature (.forEachFeatureAtPixel
                                       overview-map
                                       (.-pixel evt)
                                       (fn [feature] feature))]
                       (show-address-popup address-overlay feature evt)
                       (hide-address-popup address-overlay))))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;
;;; Show Address Marker
;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defonce interactions
  (let [select (js/ol.interaction.Select.)]
    {:select    select
     :translate (js/ol.interaction.Translate.
                 #js {:features (.getFeatures select)})}))

(defn deactivate-marker-drag []
  (doto @map-ref
    (.removeInteraction (interactions :select))
    (.removeInteraction (interactions :translate))))

(defn activate-marker-drag []
  (deactivate-marker-drag)
  (doto @map-ref
    (.addInteraction (interactions :select))
    (.addInteraction (interactions :translate))))

(defonce address-marker-layer (atom nil))

(defn show-address-marker [longitude latitude address]
  (let [icon-feature (js/ol.Feature.
                      #js {:geometry  (js/ol.geom.Point.
                                       (reproject-to-map longitude latitude))
                           :longitude longitude
                           :latitude  latitude
                           :address   address})
        icon-style   (js/ol.style.Style.
                      #js {:image (js/ol.style.Icon.
                                   #js {:src "/favicon.ico"})})
        icon-layer   (js/ol.layer.Vector.
                      #js {:source (js/ol.source.Vector.
                                    #js {:features
                                         #js [(doto icon-feature
                                                (.setStyle icon-style))]})})]
    (when @address-marker-layer
      (.pop (.getFeatures (interactions :select)))
      (.removeLayer @map-ref @address-marker-layer))
    (.addLayer @map-ref icon-layer)
    (reset! address-marker-layer icon-layer)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;
;;; Show/Hide Fire Score Buffers
;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn get-marker-point []
  (-> @address-marker-layer
      (.getSource)
      (.getFeatures)
      (first)
      (.getGeometry)
      (.getCoordinates)))

(defn miles-to-meters [miles]
  (* miles 1600))

(defonce buffer-colors
  {:fire-risk-radius    "#8b2323"
   :fire-hazard-radius  "#cd3333"
   :fire-weather-radius "#ee7621"})

(defn get-buffer [buffer-name center radius]
  (js/ol.layer.Vector.
   #js {:visible false
        :source  (js/ol.source.Vector.
                  #js {:features #js [(js/ol.Feature.
                                       #js {:geometry (js/ol.geom.Circle.
                                                       center
                                                       radius)})]})
        :style   (js/ol.style.Style.
                  #js {:fill   (js/ol.style.Fill.
                                #js {:color "rgba(200,200,200,0.2)"})
                       :stroke (js/ol.style.Stroke.
                                #js {:color (buffer-colors buffer-name)
                                     :width 3})})}))

(defonce buffer-layers (atom {}))

(defn create-fire-score-buffers [buffer-sizes-in-miles]
  (if (< (.getLength (.getLayers @map-ref)) 6)
    (let [marker-coords (get-marker-point)
          buffers       (into {}
                              (map (fn [[buffer-name radius]]
                                     [buffer-name
                                      (get-buffer buffer-name
                                                  marker-coords
                                                  (miles-to-meters radius))]))
                              buffer-sizes-in-miles)]
      (reset! buffer-layers buffers)
      (doseq [[buffer-name layer] buffers]
        (.addLayer @map-ref layer)))))

(defn show-fire-score-buffer [layer-name]
  (let [layer (@buffer-layers layer-name)]
    (.setVisible layer true)))

(defn hide-fire-score-buffer [layer-name]
  (let [layer (@buffer-layers layer-name)]
    (.setVisible layer false)))

(defn remove-fire-score-buffers []
  (doseq [layer (vals @buffer-layers)]
    (.removeLayer @map-ref layer))
  (reset! buffer-layers {}))

(defn resize-buffer [layer-name new-radius]
  (-> (@buffer-layers layer-name)
      (.getSource)
      (.getFeatures)
      (first)
      (.getGeometry)
      (.setRadius (miles-to-meters new-radius))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;
;;; Set Center and Zoom Level
;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn zoom-and-recenter-map [longitude latitude zoom-level]
  (doto (.getView @map-ref)
    (.setCenter (reproject-to-map longitude latitude))
    (.setZoom zoom-level)))

(defn zoom-to-visible-buffers []
  (when-let [visible-layers (seq (filter #(.getVisible %) (vals @buffer-layers)))]
    (let [max-extent (reduce #(js/ol.extent.extend %1 %2)
                             (map #(.getExtent (.getSource %)) visible-layers))
          view       (.getView @map-ref)
          size       (.getSize @map-ref)]
      (.fit view max-extent size #js {:padding #js [50 50 50 50]
                                      :constrainResolution false}))))

(defn ensure-valid-extent [[ulx uly lrx lry]]
  (let [pad 100]
    (if (= ulx lrx)
      (if (= uly lry)
        #js [(- ulx pad) (- uly pad) (+ lrx pad) (+ lry pad)]
        #js [(- ulx pad) uly (+ lrx pad) lry])
      (if (= uly lry)
        #js [ulx (- uly pad) lrx (+ lry pad)]
        #js [ulx uly lrx lry]))))

(defn zoom-map-to-layer-safe [map layer]
  (let [view   (.getView map)
        size   (.getSize map)
        extent (ensure-valid-extent (.getExtent (.getSource layer)))]
    (.fit view extent size #js {:padding #js [50 50 50 50] :minResolution 1})))

