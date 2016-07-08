(ns mapcha.dashboard
  (:require [goog.dom :as dom]
            [reagent.core :as r]
            [mapcha.map-utils :as map]
            [shoreleave.remotes.http-rpc :refer [remote-callback]])
  (:require-macros [shoreleave.remotes.macros :as macros]))

(defonce project-list (r/atom ()))

(defonce sample-values-list (r/atom ()))

(defonce analyze-plot? (r/atom true))

(defonce current-project (atom {}))

(defonce current-plot (atom {}))

(defonce current-samples (atom ()))

(defonce user-samples (atom {}))

(defn select-value []
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
  (remote-callback :get-random-plot
                   [(:id @current-project)]
                   #(let [new-plot %]
                      (reset! current-plot new-plot)
                      (map/draw-buffer (:center new-plot)
                                       (:radius new-plot))
                      (load-sample-points! (:id new-plot))))
  (reset! analyze-plot? false))

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

(defn save-values! []
  (reset! analyze-plot? true))

(defn set-current-value! [evt {:keys [id value color]}]
  (js/alert (str "You called set-current-value! with inputs: "
                 id " " value " " color)))

(defn convert-base [x from-base to-base]
  (-> x
      (js/parseInt from-base)
      (.toString to-base)))

(defn complementary-color [color]
  (let [max-color  (convert-base "FFFFFF" 16 10)
        this-color (convert-base (subs color 1) 16 10)
        new-color  (convert-base (- max-color this-color) 10 16)]
    (if (< (count new-color) 6)
      (apply str (concat "#" (repeat (- 6 (count new-color)) "0") new-color))
      (str "#" new-color))))

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
      (if @analyze-plot?
        [:input#new-plot-button.button {:type "button" :name "new-plot"
                                        :value "Analyze New Plot"
                                        :on-click load-random-plot!}]
        [:input#save-values-button.button {:type "button" :name "save-values"
                                           :value "Save Assignments"
                                           :on-click save-values!}])]
     [:fieldset
      [:legend "Sample Values"]
      [:ul
       (for [{:keys [id value color] :as sample-value} sample-values]
         [:li {:key id}
          [:input {:type "button" :name (str value "_" id) :value value
                   :style (if color
                            {:background-color color
                             :color (complementary-color color)}
                            {})
                   :on-click #(set-current-value! % sample-value)}]])]]
     [:input.button {:type "button" :name "dashboard-quit" :value "Quit"
                     :on-click #(set! (.-location js/window) "/")}]]))

(defn ^:export main []
  (load-projects-and-sample-values!)
  (r/render [sidebar-contents] (dom/getElement "sidebar"))
  (map/digitalglobe-base-map {:div-name      "image-analysis-pane"
                              :center-coords [102.0 17.0]
                              :zoom-level    5}))
