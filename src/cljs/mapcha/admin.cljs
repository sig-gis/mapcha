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

(defn load-projects! []
  (remote-callback :get-all-projects
                   []
                   #(reset! project-list %)))

(defn load-project-info! [project-id]
  (remote-callback :get-project-info
                   [project-id]
                   #(do (set! (.-value (dom/getElement "project-description")) "")
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
  (set! (.-value (dom/getElement "project-selector")) project-id)
  (load-project-info! project-id)
  (load-sample-values! project-id))

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

(defn create-project-form-contents []
  (let [{:keys [name description num_plots radius num_samples
                lon_min lon_max lat_min lat_max]} @current-project
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
     [:fieldset#project-info
      [:legend "Project Info"]
      [:table
       [:tbody
        [:tr
         [:td [:label "Name"]]
         [:td [:input#project-name {:type "text" :name "project-name"
                       :auto-complete "off" :value name}]]]
        [:tr
         [:td [:label "Description"]]
         [:td [:textarea#project-description
               {:name "project-description" :value description}]]]]]]
     [:fieldset#plot-info
      [:legend "Plot Info"]
      [:table
       [:tbody
        [:tr
         [:td [:label "Number of plots"]]
         [:td [:input {:type "number" :name "plots" :value num_plots
                       :auto-complete "off" :min "0" :step "1"}]]]
        [:tr
         [:td [:label "Plot radius in meters"]]
         [:td [:input {:type "number" :name "buffer-radius" :value radius
                       :auto-complete "off" :min "0.0" :step "any"}]]]
        [:tr
         [:td [:label "Samples per plot"]]
         [:td [:input {:type "number" :name "samples-per-plot" :value num_samples
                       :auto-complete "off" :min "0" :step "1"}]]]]]]
     [:fieldset#bounding-box
      [:legend "Define Bounding Box"]
      [:input#lat-max {:type "number" :name "boundary-lat-max"
                       :value (or lat_max maxlat)
                       :placeholder "Lat Max" :auto-complete "off"
                       :min "-90.0" :max "90.0" :step "any"}]
      [:input#lon-min {:type "number" :name "boundary-lon-min"
                       :value (or lon_min minlon)
                       :placeholder "Lon Min" :auto-complete "off"
                       :min "-180.0" :max "180.0" :step "any"}]
      [:input#lon-max {:type "number" :name "boundary-lon-max"
                       :value (or lon_max maxlon)
                       :placeholder "Lon Max" :auto-complete "off"
                       :min "-180.0" :max "180.0" :step "any"}]
      [:input#lat-min {:type "number" :name "boundary-lat-min"
                       :value (or lat_min minlat)
                       :placeholder "Lat Min" :auto-complete "off"
                       :min "-90.0" :max "90.0" :step "any"}]]
     [:fieldset#sample-info
      [:legend "Sample Values"]
      [:table
       [:thead
        [:tr
         [:th.empty ""]
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
     [:input.button {:type "button" :name "create-project"
                     :value (if num_plots
                              "Delete this project"
                              "Create and launch this project")
                     :on-click submit-form}]
     [:div#spinner]]))

(defn ^:export main []
  (load-projects!)
  (r/render [create-project-form-contents] (dom/getElement "create-project-form"))
  (map/digitalglobe-base-map {:div-name      "new-project-map"
                              :center-coords [102.0 17.0]
                              :zoom-level    5})
  (map/enable-dragbox-draw @map/map-ref))
