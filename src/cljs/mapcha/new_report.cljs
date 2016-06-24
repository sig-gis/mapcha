(ns mapcha.new-report
  (:require [goog.net.XhrIo           :as xhr]
            [goog.dom                 :as dom]
            [clojure.string           :as s]
            [cljs.reader              :as reader]
            [enfocus.core             :as ef]
            [enfocus.effects          :as eff]
            [enfocus.events           :as ev]
            [mapcha.utils             :as u]
            [mapcha.map-utils         :as map]
            [mapcha.enfocus-svg       :as svg])
  (:require-macros [enfocus.macros :as em]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;
;;; Event Handlers - Find Property Form
;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defonce address-ref (atom "" :validator identity))
(add-watch address-ref :address u/log-state-changes)

(defn record-address [address]
  (try (reset! address-ref (u/no-newlines address))
       (catch js/Error _ (u/show-element "#address-empty-msg"))))

(defonce coord-ref (atom {:lon nil :lat nil}
                         :validator (fn [{:keys [lon lat]}]
                                      (and (number? lon)
                                           (number? lat)))))
(add-watch coord-ref :coord u/log-state-changes)

(defn record-coord [lon lat]
  (reset! coord-ref {:lon lon :lat lat}))

(em/defaction show-current-address []
  "#address-input" (ef/content @address-ref))

(em/defaction hide-address-info []
  "#address-result-prompt" (ef/set-style :visibility "hidden")
  "#address-result"        (ef/set-style :visibility "hidden")
  "#drag-address-marker"   (ef/set-style :visibility "hidden")
  ".forward-button"        (ef/set-style :visibility "hidden"))

(em/defaction show-address-info [address]
  "#address-result-prompt"  (ef/do->
                             (ef/content "If this address looks correct, proceed:")
                             (ef/set-style :visibility "visible"))
  "#address-result"         (ef/do->
                             (ef/html-content address)
                             (ef/set-style :visibility "visible"))
  "#drag-address-marker"    (ef/set-style :visibility "visible")
  ".forward-button"         (ef/set-style :visibility "visible"))

(em/defaction show-address-lookup-error [error-msg]
  "#address-result-prompt" (ef/do->
                            (ef/html-content error-msg)
                            (ef/set-style :visibility "visible")))

(defn handle-success [{:strs [results status error_message]}]
  (cond (= status "ZERO_RESULTS")
        (show-address-lookup-error
         (str "No matching addresses were found for your input."))

        (not= status "OK")
        (show-address-lookup-error
         (str "There was an error finding your address:<br />Status: "
              status "<br />Error Message: " error_message))

        :else
        (let [first-result (first results)
              address      (get first-result "formatted_address")
              latitude     (get-in first-result ["geometry" "location" "lat"])
              longitude    (get-in first-result ["geometry" "location" "lng"])]
          (record-address address)
          (record-coord longitude latitude)
          (show-address-info address)
          (map/zoom-and-recenter-map longitude latitude 15)
          (map/show-address-marker longitude latitude address)
          (map/activate-marker-drag))))

(defn handle-failure [response-cljs]
  (show-address-lookup-error
   (str "An error occurred while communicating with the remote address lookup "
        "service.<br />Please try again.<br />[" response-cljs "]")))

(defn lookup-address-callback [event]
  (let [xhr-io        (.-target event)
        response-cljs (js->clj (.getResponseJson xhr-io))]
    (u/log "xhrIo.getResponseJson: " response-cljs)
    (u/hide-element "#address-spinner")
    (if (.isSuccess xhr-io)
      (handle-success response-cljs)
      (handle-failure response-cljs))))

;; FIXME: replace xhr/send with shoreleave function or direct google call
(defn lookup-address []
  (let [address (u/get-value "#address-input")]
    (if (s/blank? address)
      (u/show-element "#address-empty-msg")
      (do (hide-address-info)
          (u/show-element "#address-spinner")
          (xhr/send "/geocode" lookup-address-callback "POST"
                    (u/ajax-format #js {:address address}))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;
;;; Event Handlers - Select Products Form
;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn get-selected-products []
  (if-let [selected-products (u/get-attribute "#product-form input:checked" :name)]
    (if (string? selected-products)
      (list selected-products)
      selected-products)))

(defn get-buffer-sizes []
  (ef/from js/document
    :fire-risk-radius    "#fire-risk-radius"    (ef/get-prop :value)
    :fire-hazard-radius  "#fire-hazard-radius"  (ef/get-prop :value)
    :fire-weather-radius "#fire-weather-radius" (ef/get-prop :value)))

(defonce product-ref (atom () :validator identity))
(add-watch product-ref :product u/log-state-changes)

(defn record-products []
  (try (reset! product-ref (get-selected-products))
       (catch js/Error _ (u/show-element "#product-empty-msg"))))

(defonce slider-ref (atom {} :validator identity))
(add-watch slider-ref :slider u/log-state-changes)

(defn record-sliders []
  (reset! slider-ref
          {:fire-risk-radius          (u/get-value "#fire-risk-radius")
           :fire-risk-power-factor    (u/get-value "#fire-risk-power-factor")
           :fire-hazard-radius        (u/get-value "#fire-hazard-radius")
           :fire-hazard-power-factor  (u/get-value "#fire-hazard-power-factor")
           :fire-weather-radius       (u/get-value "#fire-weather-radius")
           :fire-weather-power-factor (u/get-value "#fire-weather-power-factor")}))

(defn calculate-total-cost []
  (let [checked-vals (u/get-value "#product-form input:checked")]
    (cond
      (nil? checked-vals)    "0"
      (string? checked-vals) checked-vals
      (seq? checked-vals)    (str (reduce + (map #(js/parseInt %) checked-vals))))))

(defonce cost-ref (atom "0" :validator identity))
(add-watch cost-ref :cost u/log-state-changes)

(defn record-cost []
  (reset! cost-ref (calculate-total-cost)))

(em/defaction update-total-cost []
  "#total-cost" (ef/content (calculate-total-cost)))

(em/defaction show-product-tooltip [selector]
  (str "#" (name selector) "-tooltip") (ef/set-style :visibility "visible"))

(em/defaction hide-product-tooltip [selector]
  (str "#" (name selector) "-tooltip") (ef/set-style :visibility "hidden"))

(defn toggle-buffer-visibility [node]
  (let [buffer-name (keyword (str (.-name node) "-radius"))]
    (if (.-checked node)
      (map/show-fire-score-buffer buffer-name)
      (map/hide-fire-score-buffer buffer-name))))

(defn update-buffer-size [node]
  (let [layer-name (keyword (.-id node))
        new-radius (.-valueAsNumber node)]
    (map/resize-buffer layer-name new-radius)))

(em/defaction set-slider-values [selector radius power-factor]
  (str "#" selector "-radius")              #(do (set! (.-value %) radius) %)
  (str "#" selector "-power-factor")        #(do (set! (.-value %) power-factor) %)
  (str "#" selector "-radius-slider")       (ef/content (str radius))
  (str "#" selector "-power-factor-slider") (ef/content (str power-factor)))

(em/defaction disable-sliders [selector]
  (str selector " input[type=range]") (ef/set-attr :disabled "true")
  (str selector " label")             (ef/set-style :color "grey")
  selector                            (ef/delay 500
                                                (eff/chain
                                                 (eff/fade-out 500)
                                                 (ef/set-style
                                                  :visibility "hidden"))))

(em/defaction enable-sliders [selector]
  selector                            (ef/do-> (ef/set-style :visibility "visible")
                                               (eff/fade-in 500))
  (str selector " label")             (ef/delay 500 (ef/set-style :color "black"))
  (str selector " input[type=range]") (ef/delay 500 (ef/remove-attr :disabled)))

(em/defaction update-slider-value [node]
  (str "#" (.-id node) "-slider") (ef/content (.-value node)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;
;;; Event Handlers - View Report Form
;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(em/defaction load-query-results [selector results]
  (str selector " .mean")
  (ef/content (str (Math/round (results :global-mean))))

  ;; (str selector " .stddev")
  ;; (ef/content (str " +- " (Math/round (results :global-stddev))))

  ;; (str selector " .scaled-mean")
  ;; (ef/filter #(results :local-mean)
  ;;            (ef/content (str (Math/round (results :local-mean)))))

  ;; (str selector " .scaled-stddev")
  ;; (ef/filter #(results :local-stddev)
  ;;            (ef/content (str " +- " (Math/round (results :local-stddev)))))

  (str selector " .stddev")
  (ef/remove-node)

  (str selector " li:last-child")
  (ef/remove-node))

(em/defaction load-no-results-found [selector]
  (str selector " ul")
  (ef/set-style :width "500px")

  (str selector " .mean")
  (ef/substitute
   (ef/html [:span.no-results-found "No results are available for this location."]))

  (str selector " .stddev")
  (ef/remove-node)

  (str selector " li:last-child")
  (ef/remove-node)

  (str selector " .result-explanation")
  (ef/remove-node))

(em/defaction make-results-visible [selector]
  selector (ef/set-style :display "block"))

(em/defaction update-wait-message []
  "#wait-message" (ef/content "Assessment complete. Your results are shown below."))

(defn send-fire-score-query-callback [event]
  (let [xhr-io  (.-target event)
        results (reader/read-string (.getResponseText xhr-io))]
    (u/log "xhrIo.getResponseText: " results)
    (when-let [fire-risk-results (results :fire-risk)]
      (if (fire-risk-results :global-mean)
        (do
          (load-query-results "#fire-risk-results" fire-risk-results)
          (svg/append-histogram (dom/getElement "fire-risk-results")
                                (fire-risk-results :histogram)
                                (fire-risk-results :global-mean)))
        (load-no-results-found "#fire-risk-results"))
      (make-results-visible "#fire-risk-results"))
    (when-let [fire-hazard-results (results :fire-hazard)]
      (if (fire-hazard-results :global-mean)
        (do
          (load-query-results "#fire-hazard-results" fire-hazard-results)
          (svg/append-histogram (dom/getElement "fire-hazard-results")
                                (fire-hazard-results :histogram)
                                (fire-hazard-results :global-mean)))
        (load-no-results-found "#fire-hazard-results"))
      (make-results-visible "#fire-hazard-results"))
    (when-let [fire-weather-results (results :fire-weather)]
      (if (fire-weather-results :global-mean)
        (do
          (load-query-results "#fire-weather-results" fire-weather-results)
          (svg/append-histogram (dom/getElement "fire-weather-results")
                                (fire-weather-results :histogram)
                                (fire-weather-results :global-mean)))
        (load-no-results-found "#fire-weather-results"))
      (make-results-visible "#fire-weather-results"))
    (update-wait-message)))

;; FIXME: replace xhr/send with shoreleave function
(defn send-fire-score-query []
  (let [products-chosen           (set @product-ref)
        address-lon               (@coord-ref :lon)
        address-lat               (@coord-ref :lat )
        fire-risk-radius          (@slider-ref :fire-risk-radius)
        fire-risk-power-factor    (@slider-ref :fire-risk-power-factor)
        fire-hazard-radius        (@slider-ref :fire-hazard-radius)
        fire-hazard-power-factor  (@slider-ref :fire-hazard-power-factor)
        fire-weather-radius       (@slider-ref :fire-weather-radius)
        fire-weather-power-factor (@slider-ref :fire-weather-power-factor)]
    (xhr/send "/fire-score" send-fire-score-query-callback "POST"
              (u/ajax-format
               #js {:address-lon               address-lon
                    :address-lat               address-lat
                    :fire-risk-radius          fire-risk-radius
                    :fire-risk-power-factor    fire-risk-power-factor
                    :fire-hazard-radius        fire-hazard-radius
                    :fire-hazard-power-factor  fire-hazard-power-factor
                    :fire-weather-radius       fire-weather-radius
                    :fire-weather-power-factor fire-weather-power-factor
                    :fire-risk?                (products-chosen "fire-risk")
                    :fire-hazard?              (products-chosen "fire-hazard")
                    :fire-weather?             (products-chosen "fire-weather")}))))

(defn save-report-to-db-callback [event]
  (let [xhr-io  (.-target event)
        results (reader/read-string (.getResponseText xhr-io))]
    (u/log "xhrIo.getResponseText: " results)))

(defn numeric-string? [s]
  (not (js/isNaN (js/parseFloat s))))

(defn calculate-combined-score [& scores]
  (if-let [real-scores (->> scores
                            (filter numeric-string?)
                            (map js/parseFloat)
                            (seq))]
    (/ (reduce + real-scores)
       (count real-scores))))

(defn save-report-to-db []
  (let [fire-risk-mean      (u/get-text "#fire-risk-results .mean")
        fire-hazard-mean    (u/get-text "#fire-hazard-results .mean")
        fire-weather-mean   (u/get-text "#fire-weather-results .mean")
        fire-risk-stddev    (u/get-text "#fire-risk-results .stddev")
        fire-hazard-stddev  (u/get-text "#fire-hazard-results .stddev")
        fire-weather-stddev (u/get-text "#fire-weather-results .stddev")]
    (xhr/send "/save-report" save-report-to-db-callback "POST"
              (u/ajax-format
               #js {:address             @address-ref
                    :longitude           (@coord-ref :lon)
                    :latitude            (@coord-ref :lat)
                    :cost                @cost-ref
                    :fire-risk-mean      (if (numeric-string? fire-risk-mean)
                                           fire-risk-mean)
                    :fire-hazard-mean    (if (numeric-string? fire-hazard-mean)
                                           fire-hazard-mean)
                    :fire-weather-mean   (if (numeric-string? fire-weather-mean)
                                           fire-weather-mean)
                    :fire-risk-stddev    (if fire-risk-stddev
                                           (subs fire-risk-stddev 3))
                    :fire-hazard-stddev  (if fire-hazard-stddev
                                           (subs fire-hazard-stddev 3))
                    :fire-weather-stddev (if fire-weather-stddev
                                           (subs fire-weather-stddev 3))
                    :combined-score      (calculate-combined-score
                                          fire-risk-mean
                                          fire-hazard-mean
                                          fire-weather-mean)}))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;
;;; Switching Between User Forms
;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(em/defaction switch-user-form [snippet form-number]
  "#new-report ol li.active-step"
  (ef/remove-class "active-step")

  (str "#new-report ol li:nth-child(" form-number ")")
  (ef/add-class "active-step")

  "#user-form"
  (eff/chain
   (eff/fade-out 500)
   (ef/content (snippet))
   (ef/filter #(= form-number 1) #(do (show-current-address)
                                      (show-address-info @address-ref)
                                      (map/activate-marker-drag)
                                      (map/remove-fire-score-buffers)
                                      %))
   (ef/filter #(= form-number 2) #(do (record-sliders)
                                      (disable-sliders "#fire-risk-controls")
                                      (disable-sliders "#fire-hazard-controls")
                                      (disable-sliders "#fire-weather-controls")
                                      (map/deactivate-marker-drag)
                                      (map/create-fire-score-buffers
                                       (get-buffer-sizes))
                                      %))
   (ef/filter #(= form-number 3) #(do (send-fire-score-query)
                                      %))
   (eff/fade-in 500)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;
;;; HTML Snippets
;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(declare find-property-form select-products-form view-report-form close-pane)

(em/defsnippet new-report-page
  :compiled "html/new-report.html" "#new-report" []
  "#close-button" (ev/listen :click close-pane)
  "#user-form"    (ef/content (find-property-form)))

(em/defsnippet find-property-form
  :compiled "html/new-report.html" "#address-lookup" []
  "#address-empty-msg"     (ef/set-style :visibility "hidden")
  "#address-input"         (ev/listen :input #(u/hide-element "#address-empty-msg"))
  ".lookup-address-button" (ev/listen :click lookup-address)
  "#address-spinner"       (ef/set-style :visibility "hidden")
  "#address-result-prompt" (ef/set-style :visibility "hidden")
  "#address-result"        (ef/set-style :visibility "hidden")
  "#drag-address-marker"   (ef/set-style :visibility "hidden")
  ".forward-button"        (ef/do->
                            (ef/set-style :visibility "hidden")
                            (ev/listen :click #(do
                                                 (->> (map/get-marker-point)
                                                      (apply map/reproject-from-map)
                                                      (apply record-coord))
                                                 (switch-user-form
                                                  select-products-form
                                                  2)))))

(em/defsnippet select-products-form
  :compiled "html/new-report.html" "#product-form" []
  "#fire-risk-controls"
  (ef/set-style :visibility "hidden")

  "#fire-hazard-controls"
  (ef/set-style :visibility "hidden")

  "#fire-weather-controls"
  (ef/set-style :visibility "hidden")

  "#fire-risk-analysis"
  (ef/do->
   (ev/listen :mouseover #(show-product-tooltip :fire-risk))
   (ev/listen :mouseout  #(hide-product-tooltip :fire-risk)))

  "#fire-hazard-analysis"
  (ef/do->
   (ev/listen :mouseover #(show-product-tooltip :fire-hazard))
   (ev/listen :mouseout  #(hide-product-tooltip :fire-hazard)))

  "#fire-weather-analysis"
  (ef/do->
   (ev/listen :mouseover #(show-product-tooltip :fire-weather))
   (ev/listen :mouseout  #(hide-product-tooltip :fire-weather)))

  "#fire-risk-controller"
  (ev/listen :click #(if (= (.-value (.-currentTarget %)) "Adjust Defaults")
                       (do (enable-sliders "#fire-risk-controls")
                           (set! (.-value (.-currentTarget %)) "Use Defaults"))
                       (do (set-slider-values "fire-risk" 2 1)
                           (disable-sliders "#fire-risk-controls")
                           (update-buffer-size (dom/getElement "fire-risk-radius"))
                           (map/zoom-to-visible-buffers)
                           (record-sliders)
                           (set! (.-value (.-currentTarget %)) "Adjust Defaults"))))

  "#fire-hazard-controller"
  (ev/listen :click #(if (= (.-value (.-currentTarget %)) "Adjust Defaults")
                       (do (enable-sliders "#fire-hazard-controls")
                           (set! (.-value (.-currentTarget %)) "Use Defaults"))
                       (do (set-slider-values "fire-hazard" 0.5 1)
                           (disable-sliders "#fire-hazard-controls")
                           (update-buffer-size
                            (dom/getElement "fire-hazard-radius"))
                           (map/zoom-to-visible-buffers)
                           (record-sliders)
                           (set! (.-value (.-currentTarget %)) "Adjust Defaults"))))

  "#fire-weather-controller"
  (ev/listen :click #(if (= (.-value (.-currentTarget %)) "Adjust Defaults")
                       (do (enable-sliders "#fire-weather-controls")
                           (set! (.-value (.-currentTarget %)) "Use Defaults"))
                       (do (set-slider-values "fire-weather" 4 1)
                           (disable-sliders "#fire-weather-controls")
                           (update-buffer-size
                            (dom/getElement "fire-weather-radius"))
                           (map/zoom-to-visible-buffers)
                           (record-sliders)
                           (set! (.-value (.-currentTarget %)) "Adjust Defaults"))))

  "input[type=checkbox]"
  (ev/listen :change #(do (u/hide-element "#product-empty-msg")
                          (update-total-cost)
                          (toggle-buffer-visibility (.-currentTarget %))
                          (map/zoom-to-visible-buffers)))

  "input[type=range]"
  (ev/listen :change #(do (update-slider-value (.-currentTarget %))
                          (when (re-matches #".*radius$" (.-id (.-currentTarget %)))
                            (update-buffer-size (.-currentTarget %))
                            (map/zoom-to-visible-buffers))
                          (record-sliders)))

  "#product-empty-msg"
  (ef/set-style :visibility "hidden")

  ".back-button"
  (ev/listen :click #(switch-user-form find-property-form 1))

  ".forward-button"
  (ev/listen :click #(when (and (record-products) (record-cost))
                       (switch-user-form view-report-form 3))))

(em/defsnippet view-report-form
  :compiled "html/new-report.html" "#view-report" []
  "#fire-risk-results"    (ef/set-style :display "none")
  "#fire-hazard-results"  (ef/set-style :display "none")
  "#fire-weather-results" (ef/set-style :display "none")
  ".back-button"          (ev/listen :click #(do
                                               (map/remove-fire-score-buffers)
                                               (switch-user-form
                                                select-products-form
                                                2)))
  "#save-button"          (ev/listen :click #(do
                                               (save-report-to-db)
                                               (ef/at (.-currentTarget %)
                                                 (ef/do->
                                                  (ef/set-attr :value "Report saved"
                                                               :disabled "true")
                                                  (ef/set-style :opacity 0.5))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;
;;; Content Pane Initialization
;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(em/defaction setup-pane [width height]
  "#content-pane" (eff/chain
                   (ef/set-style :border-width "1px")
                   (eff/resize :curwidth height 500)
                   (eff/resize width :curheight 500)
                   (ef/content (new-report-page))
                   #(do (map/init-map "map-view") %)))

(em/defaction close-pane []
  "#content-pane" (eff/chain
                   (ef/content "")
                   (eff/resize 5 :curheight 500)
                   (eff/resize :curwidth 0 500)
                   (ef/set-style :border-width "0px")))

(defn init-content-pane []
  (let [size   (dom/getViewportSize)
        width  (- (.-width size) 40)
        height (- (.-height size) 70)]
    (setup-pane width height)))

(defn lookup-report-addresses []
  (butlast
   (map vector
        (ef/from "td.address"        (ef/get-text))
        (ef/from "td.longitude"      (ef/get-text))
        (ef/from "td.latitude"       (ef/get-text))
        (ef/from "td.combined-score" (ef/get-text)))))

(em/defaction ^:export main []
  "#new-report-button"         (ef/do->
                                (ev/listen :click init-content-pane)
                                (ef/set-attr :value "Get New Report"))

  "#overview-map-wait-message" (ef/do->
                                #(do (map/init-overview-map
                                      "overview-map"
                                      (lookup-report-addresses)) %)
                                (ef/remove-node)))
