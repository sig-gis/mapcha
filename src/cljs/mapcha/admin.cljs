(ns mapcha.admin
  (:require [goog.dom :as dom]
            [reagent.core :as r]
            [mapcha.map-utils :as map]
            [shoreleave.remotes.http-rpc :refer [remote-callback]])
  (:require-macros [shoreleave.remotes.macros :as macros]))

(defonce sample-values (r/atom []))

(defn add-sample-value-row! []
  (let [name  (.-value (dom/getElement "value-name"))
        color (.-value (dom/getElement "value-color"))
        image (.-value (dom/getElement "value-image"))]
    (if (not= name "")
      (do (swap! sample-values conj [name color image])
          (set! (.-value (dom/getElement "value-name")) "")
          (set! (.-value (dom/getElement "value-color")) "#000000"))
      (js/alert "A sample value must possess both a name and a color."))))

(defn delete-element [vc pos]
  (into
   (subvec vc 0 pos)
   (subvec vc (inc pos))))

(defn remove-sample-value-row! [idx]
  (when (> (count @sample-values) idx)
    (swap! sample-values delete-element idx)))

(defn disable-element! [evt]
  (set! (.-disabled (.-currentTarget evt)) true))

(defn create-project-form-contents []
  [:form {:method "post" :action "/admin"}
   [:fieldset#project-info
    [:legend "Project Info"]
    [:table
     [:tbody
      [:tr
       [:td [:label "Name"]]
       [:td [:input {:type "text" :name "project-name"
                     :size "21" :auto-complete "off"}]]]
      [:tr
       [:td [:label "Description"]]
       [:td [:textarea {:name "project-description" :rows "3" :cols "31"}]]]]]]
   [:fieldset#plot-info
    [:legend "Plot Info"]
    [:table
     [:tbody
      [:tr
       [:td [:label "Number of plots"]]
       [:td [:input {:type "number" :name "plots"
                     :auto-complete "off" :min "0" :step "1"}]]]
      [:tr
       [:td [:label "Plot radius in meters"]]
       [:td [:input {:type "number" :name "buffer-radius"
                     :auto-complete "off" :min "0.0" :step "any"}]]]
      [:tr
       [:td [:label "Samples per plot"]]
       [:td [:input {:type "number" :name "samples-per-plot"
                     :auto-complete "off" :min "0" :step "1"}]]]]]]
   [:fieldset#bounding-box
    [:legend "Define Bounding Box"]
    [:input#lat-max {:type "number" :name "boundary-lat-max"
                     :placeholder "Lat Max" :auto-complete "off"
                     :min "-90.0" :max "90.0" :step "any"}]
    [:input#lon-min {:type "number" :name "boundary-lon-min"
                     :placeholder "Lon Min" :auto-complete "off"
                     :min "-180.0" :max "180.0" :step "any"}]
    [:input#lon-max {:type "number" :name "boundary-lon-max"
                     :placeholder "Lon Max" :auto-complete "off"
                     :min "-180.0" :max "180.0" :step "any"}]
    [:input#lat-min {:type "number" :name "boundary-lat-min"
                     :placeholder "Lat Min" :auto-complete "off"
                     :min "-90.0" :max "90.0" :step "any"}]]
   [:fieldset#sample-info
    [:legend "Sample Values"]
    [:table
     [:thead
      [:tr
       [:th.empty ""] [:th "Name"] [:th "Color"] [:th "Image"] [:th.empty ""]]]
     [:tbody
      (map-indexed
       (fn [idx [name color image]]
         [:tr {:key idx}
          [:td [:input.button {:type "button" :value "-"
                               :on-click #(remove-sample-value-row! idx)}]]
          [:td name]
          [:td [:div.circle {:style {:background-color color}}]]
          [:td image]
          [:td ""]])
       @sample-values)
      [:tr
       [:td ""]
       [:td [:input#value-name {:type "text" :name "value-name"
                                :size "12" :auto-complete "off"}]]
       [:td [:input#value-color {:type "color" :name "value-color"}]]
       [:td [:input#value-image {:type "file" :name "value-image"
                                 :accept "image/*"}]]
       [:td [:input.button {:type "button" :value "Add sample value"
                            :on-click add-sample-value-row!}]]]]]
    [:input {:type "hidden" :name "sample-values" :value (pr-str @sample-values)}]]
   [:input.button {:type "submit" :name "create-project"
                   :value "Create and launch this project"
                   :on-click disable-element!}]
   [:img#compass-rose {:src "img/compass_rose.png"}]])

(defn ^:export main []
  (r/render [create-project-form-contents] (dom/getElement "create-project-form"))
  (map/digitalglobe-base-map {:div-name      "new-project-map"
                              :center-coords [102.0 17.0]
                              :zoom-level    5}))
