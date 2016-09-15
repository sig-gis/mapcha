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

(defonce current-plot (r/atom nil))

(defonce current-samples (atom ()))

(defonce user-samples (atom {}))

(defn save-values! [evt]
  (let [user-id    (js/parseInt (.-value (dom/getElement "user-id")))
        plot-id    (:id @current-plot)
        imagery-id 2] ;; DigitalGlobe Maps API: Recent Imagery+Streets
    (remote-callback :add-user-samples
                     [user-id plot-id imagery-id @user-samples]
                     #(js/alert
                       "Your assignments have been saved to the database."))
    (u/disable-element! (.-currentTarget evt))
    (reset! current-plot nil)
    (map/disable-selection @map/map-ref)))

(defn set-current-value! [evt {:keys [id value color]}]
  (if-let [selected-features (map/get-selected-samples)]
    (let [samples (seq (.getArray selected-features))
          button  (.-currentTarget evt)]
      (u/highlight-border button)
      (timer/callOnce #(u/lowlight-border button) 500)
      (doseq [sample samples]
        (let [sample-id (.get sample "sample_id")]
          (swap! user-samples assoc sample-id id)
          (map/highlight-sample sample color)))
      (.clear selected-features)
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
  [new-project-id]
  (when-let [new-project (->> @project-list
                              (filter #(= new-project-id (:id %)))
                              (first))]
    (reset! current-project new-project)
    (reset! current-plot nil)
    (reset! current-samples ())
    (reset! user-samples {})
    (doto @map/map-ref
      (map/remove-plot-layer)
      (map/remove-sample-layer)
      (map/disable-selection))
    (load-sample-values! new-project-id)
    (u/enable-element! (dom/getElement "new-plot-button"))
    (u/disable-element! (dom/getElement "save-values-button"))
    (map/draw-polygon (:boundary new-project))))

(defn flag-plot!
  [plot-id]
  (remote-callback :flag-plot
                   [plot-id]
                   #(js/alert (str "Plot " plot-id " has been flagged."))))

(defn sidebar-contents []
  [:div#sidebar-contents
   [:fieldset
    [:legend "Select Project"]
    [:select {:name "project-id" :size "1"
              :default-value (:id @current-project)
              :on-change #(switch-project!
                           (js/parseInt (.-value (.-currentTarget %))))}
     (for [{:keys [id name]} @project-list]
       [:option {:key id :value id} name])]
    [:input#new-plot-button.button {:type "button" :name "new-plot"
                                    :value (if @current-plot
                                             "Flag Plot as Bad"
                                             "Analyze New Plot")
                                    :on-click (fn [_]
                                                (when @current-plot
                                                  (flag-plot! (:id @current-plot)))
                                                (load-random-plot!))}]]
   [:fieldset
    [:legend "Sample Values"]
    [:ul
     (for [{:keys [id value color] :as sample-value} @sample-values-list]
       [:li {:key id}
        [:input {:type "button" :name (str value "_" id) :value value
                 :style (if color
                          {:border-left-style "solid"
                           :border-left-width "1.5rem"
                           :border-left-color color}
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
                              :zoom-level    5})
  (when-let [project-id (js/parseInt (.-value
                                      (dom/getElement "initial-project-id")))]
    (js/alert (str "Switching to Project ") project-id)
    (switch-project! project-id)))
