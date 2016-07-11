(ns mapcha.dashboard
  (:require [goog.dom :as dom]
            [goog.Timer :as timer]
            [reagent.core :as r]
            [mapcha.map-utils :as map]
            [mapcha.utils :as u]
            [shoreleave.remotes.http-rpc :refer [remote-callback]])
  (:require-macros [shoreleave.remotes.macros :as macros]))

(defonce project-list (r/atom ()))

(defonce sample-values-list (r/atom ()))

(defonce current-project (atom nil))

(defonce current-plot (atom nil))

(defonce current-samples (atom ()))

(defonce user-samples (atom {}))

;; FIXME: stub
(defn save-values! [evt]
  (js/alert "Saving your values to the database...")
  (u/disable-element! (.-currentTarget evt))
  (u/enable-element! (dom/getElement "new-plot-button"))
  (map/disable-selection @map/map-ref))

(defn set-current-value! [evt {:keys [id value color]}]
  (if-let [samples (seq (map/get-selected-samples))]
    (let [button (.-currentTarget evt)]
      (u/highlight-border button)
      (timer/callOnce #(u/lowlight-border button) 500)
      (doseq [sample samples]
        (let [sample-id (.get sample "sample_id")]
          (swap! user-samples assoc sample-id id)
          (map/highlight-sample sample color)))
      (when (= (set (keys @user-samples))
               (into #{} (map :id) @current-samples))
        (u/enable-element! (dom/getElement "save-values-button"))))
    (js/alert "No sample points selected. Please click some first.")))

(defn load-sample-points! [plot-id]
  (remote-callback :get-sample-points
                   [plot-id]
                   #(let [new-samples %]
                      (reset! current-samples new-samples)
                      (reset! user-samples {})
                      (map/draw-points new-samples))))

(defn load-random-plot! []
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
      (reset! current-plot nil)
      (reset! current-samples ())
      (reset! user-samples {})
      (map/disable-selection @map/map-ref)
      (load-sample-values! new-project-id)
      (u/enable-element! (dom/getElement "new-plot-button"))
      (u/disable-element! (dom/getElement "save-values-button"))
      (map/draw-polygon (:boundary new-project)))))

(defn sidebar-contents []
  [:div#sidebar-contents
   [:fieldset
    [:legend "Select Project"]
    [:select {:name "project-id" :size "1"
              :default-value (:id (first @project-list))
              :on-change switch-project!}
     (for [{:keys [id name]} @project-list]
       [:option {:key id :value id} name])]
    [:input#new-plot-button.button {:type "button" :name "new-plot"
                                    :value "Analyze New Plot"
                                    :on-click (fn [evt]
                                                (load-random-plot!)
                                                (u/disable-element!
                                                 (.-currentTarget evt)))}]]
   [:fieldset
    [:legend "Sample Values"]
    [:ul
     (for [{:keys [id value color] :as sample-value} @sample-values-list]
       [:li {:key id}
        [:input {:type "button" :name (str value "_" id) :value value
                 :style (if color
                          {:background-color color
                           :color "white"
                           :font-weight "bold"
                           :text-shadow (str "1px 1px 2px black,"
                                             "0px 0px 25px blue,"
                                             "0px 0px 5px darkblue")}
                          {})
                 :on-click #(set-current-value! % sample-value)}]])]
    [:input#save-values-button.button {:type "button" :name "save-values"
                                       :value "Save Assignments"
                                       :on-click save-values!
                                       :disabled true
                                       :style {:opacity "0.5"}}]]])

(defn ^:export main []
  (load-projects-and-sample-values!)
  (r/render [sidebar-contents] (dom/getElement "sidebar"))
  (map/digitalglobe-base-map {:div-name      "image-analysis-pane"
                              :center-coords [102.0 17.0]
                              :zoom-level    5}))
