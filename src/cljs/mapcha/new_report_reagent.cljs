(ns mapcha.new-report-reagent
  (:require [goog.dom :as dom]
            [goog.style :as style]
            [goog.Timer :as timer]
            [reagent.core :as r]
            [mapcha.map-utils :as map]))

(def click-count (r/atom 0))

;; FIXME: stub
(defn find-property-form-foo []
  [:div
   "The atom " [:code "click-count"] " has value: "
   @click-count ". "
   [:input {:type "button"
            :value "Click me!"
            :on-click #(swap! click-count inc)}]])

;; FIXME: Add (ef/set-style :visibility "hidden")
;;        and (ev/listen :even #(action)) features
;;        to find-property-form.
(defn find-property-form []
  [:div#address-lookup
   [:p#address-prompt
    "First enter the report address: "
    [:span#address-empty-msg "Cannot be empty!"]]
   [:textarea#address-input {:rows "4"}]
   [:p.light "e.g., 2122 S Grand Ave Santa Ana, CA 92705"]
   [:input.lookup-address-button.button {:type "button" :value "Lookup address"}]
   [:img#address-spinner {:src "/img/spinner.gif"}]
   [:p#address-result-prompt "If this address looks correct, proceed:"]
   [:p#address-result.light]
   [:p#drag-address-marker (str "Note: To move the marker, single-click "
                                "to select and then click-and-drag.")]
   [:input.forward-button.button {:type "button" :value "Select products"}]])

(declare close-content-pane)

(defn new-report-page []
  [:div#new-report
   [:input#close-button.button {:type "button" :value "X"
                                :on-click close-content-pane}]
   [:h1 "New Report"]
   [:ol
    [:li.active-step "1. Find property"]
    [:li "2. Select products"]
    [:li "3. View report"]]
   [:div#user-form
    [find-property-form]]
   [:div#map-view]])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;
;;; Content Pane Initialization
;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn init-content-pane []
  (let [content-pane (dom/getElement "content-pane")
        size         (dom/getViewportSize)
        width        (- (.-width size) 40)
        height       (- (.-height size) 70)]
    (style/setStyle content-pane "border-width" "1px")
    (style/setHeight content-pane height)
    (style/setWidth content-pane width)
    (timer/callOnce #(r/render [new-report-page] content-pane) 500)
    (timer/callOnce #(map/init-map "map-view") 500)))

(defn close-content-pane []
  (let [content-pane (dom/getElement "content-pane")]
    (r/unmount-component-at-node content-pane)
    (style/setWidth content-pane 5)
    (style/setHeight content-pane 0)
    (timer/callOnce #(style/setStyle content-pane "border-width" "0px") 500)))

(defn new-report-button []
  [:input#new-report-button.button {:type "button" :name "new-report"
                                    :value "Get New Report"
                                    :on-click init-content-pane}])

(defn lookup-report-addresses []
  (apply map vector
         (for [field ["address" "longitude" "latitude" "combined-score"]]
           (->> (dom/getElementsByTagNameAndClass "td" field)
                (map dom/getTextContent)
                (butlast)))))

(defn ^:export main []
  (r/render [new-report-button] (dom/getElement "new-report-button-container"))
  (map/init-overview-map "overview-map" (lookup-report-addresses))
  (dom/removeNode (dom/getElement "overview-map-wait-message")))
