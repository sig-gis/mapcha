(ns mapcha.dashboard
  (:require [goog.dom :as dom]
            [reagent.core :as r]
            [mapcha.map-utils :as map]
            [shoreleave.remotes.http-rpc :refer [remote-callback]])
  (:require-macros [shoreleave.remotes.macros :as macros]))

(defonce project-list (r/atom ()))

(defonce sample-values-list (r/atom ()))

(defonce current-project (atom {}))

(defonce current-plot (atom {}))

(defonce current-samples (atom ()))

(defonce user-samples (atom {}))

(defn select-value []
  ;; 1. Change the point's color to green
  (if-let [sample (map/get-selected-sample)]
    (let [sample-id (.get sample "sample_id")]
      (if-let [sample-value-id (some->> "input[name=\"sample-values\"]:checked"
                                        (.querySelector js/document)
                                        (.-value)
                                        (js/parseInt))]
        (let [sample-value (->> @sample-values-list
                                (filter #(= sample-value-id (:id %)))
                                (first)
                                (:value))]
          (swap! user-samples assoc sample-id sample-value-id)
          (map/highlight-sample sample)
          (js/alert (str "Selected " sample-value " for sample #" sample-id ".")))
        (js/alert "No sample value selected. Please choose one from the list.")))
    (js/alert "No sample point selected. Please click one first.")))

(defn load-sample-points! [plot-id]
  (remote-callback :get-sample-points
                   [plot-id]
                   #(let [new-samples %]
                      (reset! current-samples new-samples)
                      (map/draw-points new-samples))))

(defn load-random-plot! []
  ;; 5. Disable this button
  (remote-callback :get-random-plot
                   [(:id @current-project)]
                   #(let [new-plot %]
                      (reset! current-plot new-plot)
                      (map/draw-buffer (:center new-plot)
                                       (:radius new-plot))
                      (load-sample-points! (:id new-plot)))))

(defn load-sample-values! [project-id]
  (remote-callback :get-sample-values
                   [project-id]
                   #(reset! sample-values-list %)))

(defn load-projects-and-sample-values! []
  (remote-callback :get-all-projects
                   []
                   #(let [project1 (first %)]
                      (reset! project-list %)
                      (reset! current-project project1)
                      (load-sample-values! (:id project1))
                      (map/draw-polygon (:boundary project1)))))

(defn switch-project!
  [evt]
  (let [new-project-id (js/parseInt (.-value (.-currentTarget evt)))]
    (when-let [new-project (->> @project-list
                                (filter #(= new-project-id (:id %)))
                                (first))]
      (reset! current-project new-project)
      (load-sample-values! new-project-id)
      (map/draw-polygon (:boundary new-project)))))

(defn sidebar-contents []
  (let [projects      @project-list
        project1      (first projects)
        sample-values @sample-values-list]
    [:div#sidebar-contents
     [:fieldset
      [:legend "Select Project"]
      [:select {:name "project-id" :size "1" :default-value (:id project1)
                :on-change switch-project!}
       (for [{:keys [id name]} projects]
         [:option {:key id :value id} name])]
      [:input#new-plot-button.button {:type "button" :name "new-plot"
                                      :value "Analyze New Plot"
                                      :on-click load-random-plot!}]]
     [:fieldset
      [:legend "Sample Values"]
      [:ul
       (for [{:keys [id value]} sample-values]
         [:li {:key id}
          [:input.sample-values {:type "radio" :name "sample-values"
                                 :id (str value "_" id) :value id}]
          [:label.sample-values {:for (str value "_" id)} value]])]
      [:input#select-value-button.button {:type "button" :name "select-value"
                                          :value "Select Value"
                                          :on-click select-value}]]]))

(defn ^:export main []
  (load-projects-and-sample-values!)
  (r/render [sidebar-contents] (dom/getElement "sidebar"))
  (map/digitalglobe-base-map {:div-name      "image-analysis-pane"
                              :center-coords [102.0 17.0]
                              :zoom-level    5}))
