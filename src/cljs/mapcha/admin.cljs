(ns mapcha.admin
  (:require [goog.dom :as dom]
            [goog.style :as style]
            [reagent.core :as r]
            [mapcha.map-utils :as map]
            [shoreleave.remotes.http-rpc :refer [remote-callback]])
  (:require-macros [shoreleave.remotes.macros :as macros]))

(defonce project-list (r/atom ()))

(defonce current-project (r/atom nil))

(defonce sample-values (r/atom []))

(defonce current-imagery (atom "DigitalGlobeRecentImagery+Streets"))

(defn load-projects! []
  (remote-callback :get-all-projects
                   []
                   #(reset! project-list %)))

(defn load-project-info! [project-id]
  (remote-callback :get-project-info
                   [project-id]
                   #(let [{:keys [name description num_plots radius
                                  num_samples sample_resolution
                                  lon_min lon_max lat_min lat_max]} %]
                      (set! (.-value (dom/getElement "project-selector")) project-id)
                      (set! (.-value (dom/getElement "project-name")) (or name ""))
                      (set! (.-value (dom/getElement "project-description"))
                            (or description ""))
                      (set! (.-value (dom/getElement "plots")) (or num_plots ""))
                      (set! (.-value (dom/getElement "radius")) (or radius ""))
                      (set! (.-value (dom/getElement "num-samples"))
                            (if-not num_samples
                              ""
                              (if (neg? sample_resolution)
                                num_samples
                                (str num_samples " @ " sample_resolution "m"))))
                      (reset! current-project %))))

(defn load-sample-values! [project-id]
  (remote-callback :get-sample-values
                   [project-id]
                   #(reset! sample-values
                            (mapv (juxt :value :color :image) %))))

(defn add-sample-value-row! []
  (let [name  (.-value (dom/getElement "value-name"))
        color (.-value (dom/getElement "value-color"))
        image (.-value (dom/getElement "value-image"))]
    (if (not= name "")
      (do (swap! sample-values conj [name color image])
          (set! (.-value (dom/getElement "value-name")) "")
          (set! (.-value (dom/getElement "value-color")) "#000000")
          (set! (.-value (dom/getElement "value-image")) ""))
      (js/alert "A sample value must possess both a name and a color."))))

(defn delete-element [vc pos]
  (into
   (subvec vc 0 pos)
   (subvec vc (inc pos))))

(defn remove-sample-value-row! [idx]
  (when (> (count @sample-values) idx)
    (swap! sample-values delete-element idx)))

(defn set-current-project! [project-id]
  (load-project-info! project-id)
  (load-sample-values! project-id))

(defn set-current-imagery! [new-imagery]
  (doseq [layer (.getArray (.getLayers @map/map-ref))]
    (let [title (.get layer "title")]
      (if (= title @current-imagery)
        (.setVisible layer false))
      (if (= title new-imagery)
        (.setVisible layer true))))
  (reset! current-imagery new-imagery))

(defn delete-current-project! []
  (let [project-id (js/parseInt (.-value (dom/getElement "project-selector")))]
    (remote-callback :archive-project
                     [project-id]
                     #(do (js/alert (str "Project " project-id
                                         " has been deleted."))
                          (set-current-project! 0)
                          (load-projects!)))))

(defn submit-form [evt]
  (if @current-project
    (when (js/confirm "Do you REALLY want to delete this project?!")
      (delete-current-project!))
    (do
      (set! (.-value (.-currentTarget evt)) "Processing...please wait...")
      (style/setStyle (dom/getElement "spinner") "visibility" "visible")
      (.submit (dom/getElement "project-management-form")))))

(defn export-current-plot-data []
  (let [project-id (js/parseInt (.-value (dom/getElement "project-selector")))]
    (when-not (zero? project-id)
      (remote-callback :dump-project-aggregate-data
                       [project-id]
                       #(.open js/window %)))))

(defn create-project-form-contents []
  (let [{:keys [num_plots lon_min lon_max lat_min lat_max]} @current-project
        {:keys [minlon minlat maxlon maxlat]} @map/current-bbox]
    [:form#project-management-form {:method "post" :action "/admin"}
     [:div#project-selection
      [:label "Currently Viewing:"]
      [:select#project-selector {:name "project-selector" :size "1"
                                 :default-value "0"
                                 :on-change #(-> (.-currentTarget %)
                                                 (.-value)
                                                 (js/parseInt)
                                                 (set-current-project!))}
       [:option {:key 0 :value 0} "New Project"]
       (for [{:keys [id name]} @project-list]
         [:option {:key id :value id} name])]]
     [:input.button {:type "button" :name "download-plot-data"
                     :value "Download Data" :on-click export-current-plot-data
                     :style {:visibility (if num_plots "visible" "hidden")}}]
     [:input.button {:type "button" :name "create-project"
                     :value (if num_plots
                              "Delete this project"
                              "Create and launch this project")
                     :on-click submit-form}]
     [:fieldset#project-info
      [:legend "Project Info"]
      [:label "Name"]
      [:input#project-name {:type "text" :name "project-name" :auto-complete "off"}]
      [:label "Description"]
      [:textarea#project-description {:name "project-description"}]]
     [:fieldset#plot-info
      [:legend "Plot Info"]
      [:label "Number of plots"]
      [:input#plots {:type "number" :name "plots" :auto-complete "off"
                     :min "0" :step "1"}]
      [:label "Plot radius (m)"]
      [:input#radius {:type "number" :name "buffer-radius" :auto-complete "off"
               :min "0.0" :step "any"}]
      [:label (if num_plots
                "Samples per plot"
                "Sample resolution (m)")]
      (if num_plots
        [:input#num-samples {:type "text" :name "sample-resolution"}]
        [:input#num-samples {:type "number" :name "sample-resolution"
                             :auto-complete "off" :min "0.0" :step "any"}])]
     [:fieldset#bounding-box
      [:legend "Define Bounding Box"]
      [:label "Hold CTRL and click-and-drag a bounding box on the map"]
      [:input#lat-max {:type "number" :name "boundary-lat-max"
                       :value (or lat_max maxlat "")
                       :placeholder "North" :auto-complete "off"
                       :min "-90.0" :max "90.0" :step "any"}]
      [:input#lon-min {:type "number" :name "boundary-lon-min"
                       :value (or lon_min minlon "")
                       :placeholder "West" :auto-complete "off"
                       :min "-180.0" :max "180.0" :step "any"}]
      [:input#lon-max {:type "number" :name "boundary-lon-max"
                       :value (or lon_max maxlon "")
                       :placeholder "East" :auto-complete "off"
                       :min "-180.0" :max "180.0" :step "any"}]
      [:input#lat-min {:type "number" :name "boundary-lat-min"
                       :value (or lat_min minlat "")
                       :placeholder "South" :auto-complete "off"
                       :min "-90.0" :max "90.0" :step "any"}]]
     [:div#map-and-imagery
      [:div#new-project-map]
      [:label "Basemap imagery: "]
      [:select {:name "imagery-selector" :size "1"
                :default-value "DigitalGlobeRecentImagery+Streets"
                :on-change #(-> (.-currentTarget %)
                                (.-value)
                                (set-current-imagery!))}
       [:option {:value "DigitalGlobeRecentImagery"}
        "DigitalGlobe: Recent Imagery"]
       [:option {:value "DigitalGlobeRecentImagery+Streets"}
        "DigitalGlobe: Recent Imagery+Streets"]
       [:option {:value "BingAerial"} "Bing Maps: Aerial"]
       [:option {:value "BingAerialWithLabels"} "Bing Maps: Aerial with Labels"]]]
     [:fieldset#sample-info
      [:legend "Sample Values"]
      [:table
       [:thead
        [:tr
         [:th ""]
         [:th "Name"]
         [:th "Color"]
         [:th "Reference Image"]]]
       [:tbody
        (map-indexed
         (fn [idx [name color image]]
           [:tr {:key idx}
            [:td [:input.button {:type "button" :value "-"
                                 :on-click #(remove-sample-value-row! idx)
                                 :disabled (if num_plots true false)}]]
            [:td name]
            [:td [:div.circle {:style {:background-color color}}]]
            [:td image]])
         @sample-values)
        [:tr
         [:td ""]
         [:td [:input#value-name {:type "text" :name "value-name"
                                  :auto-complete "off"
                                  :disabled (if num_plots true false)}]]
         [:td [:input#value-color {:type "color" :name "value-color"
                                   :disabled (if num_plots true false)}]]
         [:td [:input#value-image {:type "file" :name "value-image"
                                   :accept "image/*"
                                   :disabled (if num_plots true false)}]]]]]
      [:input.button {:type "button" :name "add-sample-value"
                      :value "Add sample value"
                      :on-click add-sample-value-row!
                      :disabled (if num_plots true false)}]
      [:input {:type "hidden" :name "sample-values"
               :value (pr-str @sample-values)}]]
     [:div#spinner]]))

(defn ^:export main []
  (load-projects!)
  (r/render [create-project-form-contents] (dom/getElement "create-project-form"))
  (map/digitalglobe-base-map {:div-name      "new-project-map"
                              :center-coords [102.0 17.0]
                              :zoom-level    5})
  (map/enable-dragbox-draw @map/map-ref))
