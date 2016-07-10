(ns mapcha.dashboard
  (:require [goog.dom :as dom]
            [goog.style :as style]
            [reagent.core :as r]
            [mapcha.map-utils :as map]
            [mapcha.utils :as u]
            [shoreleave.remotes.http-rpc :refer [remote-callback]])
  (:require-macros [shoreleave.remotes.macros :as macros]))

(defonce project-list (r/atom ()))

(defonce sample-values-list (r/atom ()))

(defonce current-project (atom {}))

(defonce current-plot (atom {}))

(defonce current-samples (atom ()))

(defonce current-value-button (atom nil))

(defonce current-value (atom {}))

(defn set-current-value! [evt new-value]
  (let [button (.-currentTarget evt)]
    (when @current-value-button
      (u/lowlight-border @current-value-button))
    (u/highlight-border button)
    (reset! current-value-button button)
    (reset! current-value new-value)))

;;=================== IN PROGRESS ================================

(defonce user-samples (atom {}))

(defn save-values! []
  true)

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

;; FIXME: Deprecated. Fold this logic into set-current-value! above.
(defn select-value []
  (if-let [sample (first (map/get-selected-samples))]
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

;;=================== IN PROGRESS ================================

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
    [:table
     [:tbody
      [:tr
       [:td
        [:input#new-plot-button.button {:type "button" :name "new-plot"
                                        :value "Analyze New Plot"
                                        :on-click (fn [evt]
                                                    (load-random-plot!)
                                                    (u/disable-element!
                                                     (.-currentTarget evt)))}]]
       [:td "or"]
       [:td
        [:input#quit-button.button {:type "button" :name "dashboard-quit"
                                    :value "Quit"
                                    :on-click #(set! (.-location js/window)
                                                     "/")}]]]]]]
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
