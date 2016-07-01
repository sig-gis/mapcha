(ns mapcha.dashboard
  (:require [goog.dom :as dom]
            [reagent.core :as r]
            [mapcha.map-utils :as map]
            [shoreleave.remotes.http-rpc :refer [remote-callback]])
  (:require-macros [shoreleave.remotes.macros :as macros]))

(defn zoom-to-plot []
  ;; 1. Record plot-id in an atom
  ;; 2. Zoom to randomly selected plot (chosen by least number of samples)
  ;; 3. Show the buffer boundary
  ;; 4. Show 15 sample points in red
  ;; 5. Disable this button
  (js/alert "Called zoom-to-plot"))

(defn select-value []
  ;; 1. Change the point's color to green
  ;; 2. Assoc a user-samples atom (holding a map) to set
  ;;    {@sample-id @sample-value-id}
   (js/alert "Called select-value"))

(def project-list (r/atom ()))

(def sample-values-list (r/atom ()))

(defn load-sample-values! [project-id]
  (remote-callback :get-sample-values
                   [project-id]
                   #(reset! sample-values-list %)))

(defn load-projects-and-sample-values! []
  (remote-callback :get-all-projects
                   []
                   #(do (reset! project-list %)
                        (load-sample-values! (:id (first %))))))

(defn sidebar-contents []
  (let [projects      @project-list
        project1      (first projects)
        sample-values @sample-values-list]
    [:div#sidebar-contents
     [:fieldset
      [:legend "Select Project"]
      [:select {:name "project-id" :size "1" :default-value (:id project1)}
       (for [{:keys [id name]} projects]
         [:option {:key id :value id} name])]
      [:input#new-plot-button.button {:type "button" :name "new-plot"
                                      :value "Analyze New Plot"
                                      :on-click zoom-to-plot}]]
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
  (map/digitalglobe-base-map {:div-name      "image-analysis-pane"
                              :center-coords [102.0 17.0]
                              :zoom-level    5})
  (load-projects-and-sample-values!)
  (r/render [sidebar-contents] (dom/getElement "sidebar")))
