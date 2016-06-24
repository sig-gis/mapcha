(ns mapcha.enfocus-svg
  (:require [enfocus.core :as ef]
            [domina       :as domina])
  (:require-macros [enfocus.macros :as em]))

;;=================================================================
;; SVG Examples
;;
;; <circle cx="250" cy="25" r="25"/>
;; <ellipse cx="250" cy="25" rx="100" ry="25"/>
;; <line x1="0" y1="0" x2="500" y2="50" stroke="black"/>
;; <text x="250" y="25" font-family="serif" font-size="25"
;;       text-anchor="middle" fill="gray">
;;   Easy-peasy
;; </text>
;;=================================================================

;; adapted from enfocus.core
(defn svg
  [node-spec]
  (cond
    (string? node-spec) (.createTextNode js/document node-spec)
    (vector? node-spec)
    (let [[tag & [m & ms :as more]] node-spec
          [tag-name & segments] (.split (name tag) #"(?=[#.])")
          id (some (fn [seg]
                     (when (= \# (.charAt seg 0)) (subs seg 1))) segments)
          classes (keep (fn [seg]
                          (when (= \. (.charAt seg 0)) (subs seg 1)))
                        segments)
          attrs (if (map? m) m {})
          attrs (if id (assoc attrs :id id) attrs)
          attrs (if-not (empty? classes)
                  (assoc attrs :class (apply str (interpose " " classes)))
                  attrs)
          content (flatten (map svg (if (map? m) ms more)))
          node (.createElementNS js/document "http://www.w3.org/2000/svg" tag-name)]
      (doseq [[key val] attrs]
        (.setAttribute node (name key) val))
      (when content (domina/append! node content)))
    (sequential? node-spec) (flatten (map svg node-spec))
    :else (.createTextNode js/document (str node-spec))))

(defmulti make-scale (fn [& {:keys [type domain range]}] type))

(defmethod make-scale :linear [& {:keys [type domain range]}]
  (let [[domain-min domain-max] domain
        [ range-min  range-max] range]
    (fn [x]
      (case x
        :domain-min domain-min
        :domain-max domain-max
        :range-min  range-min
        :range-max  range-max
        (+ (* (/ (- x domain-min)
                 (- domain-max domain-min))
              (- range-max range-min))
           range-min)))))

(defn get-tick-steps [ticks min max]
  (let [step (/ (- max min) (dec ticks))]
    (map #(Math/round (+ min (* % step)))
         (range ticks))))

(defn make-ticks [orient mark label]
  (let [tick-coords  (case orient
                       :left   {:x1 0 :y1 mark :x2 -5 :y2 mark}
                       :right  {:x1 0 :y1 mark :x2  5 :y2 mark}
                       :top    {:x1 mark :y1 0 :x2 mark :y2 -5}
                       :bottom {:x1 mark :y1 0 :x2 mark :y2  5})
        label-coords (case orient
                       :left   {:x -8 :y (+ mark 4) :text-anchor "end"}
                       :right  {:x  8 :y (+ mark 4) :text-anchor "start"}
                       :top    {:x mark :y -15 :text-anchor "middle"}
                       :bottom {:x mark :y  15 :text-anchor "middle"})
        text-color   (str "rgb("
                          (Math/round (* label 2.55))
                          ","
                          (Math/round (* (- 100 label) 2.55))
                          ",0)")]
    (svg (list [:line (assoc tick-coords :stroke "black")]
               [:text (assoc label-coords :fill text-color) label]))))

(defn make-axis [& {:keys [scale orient ticks]}]
  (let [domain-min  (scale :domain-min)
        domain-max  (scale :domain-max)
        range-min   (scale :range-min)
        range-max   (scale :range-max)
        axis-coords (case orient
                      :left   {:x1 0 :y1 range-min :x2 0 :y2 range-max}
                      :right  {:x1 0 :y1 range-min :x2 0 :y2 range-max}
                      :top    {:x1 range-min :y1 0 :x2 range-max :y2 0}
                      :bottom {:x1 range-min :y1 0 :x2 range-max :y2 0})
        axis        (svg [:line (assoc axis-coords :stroke "black")])
        ticks       (mapcat (partial make-ticks orient)
                            (get-tick-steps ticks range-min range-max)
                            (get-tick-steps ticks domain-min domain-max))]
    (ef/do-> (ef/append axis)
             (ef/filter #(seq ticks) (ef/append ticks)))))

(defn append-histogram [top-node histogram-data fire-score]
  (let [svg-width          200
        svg-height         100
        svg-padding        {:left 23 :right 10 :top 15 :bottom 20}
        bar-padding        1
        x-min              0
        x-max              100
        y-min              0
        y-max              (apply max (map :percent histogram-data))
        x-scale            (make-scale :type   :linear
                                       :domain [x-min x-max]
                                       :range  [(:left svg-padding)
                                                (- svg-width
                                                   (:right svg-padding))])
        y-scale            (make-scale :type   :linear
                                       :domain [y-min y-max]
                                       :range  [(- svg-height
                                                   (:bottom svg-padding))
                                                (:top svg-padding)])
        x-axis             (make-axis :scale  x-scale
                                      :orient :bottom
                                      :ticks  11)
        y-axis             (make-axis :scale  y-scale
                                      :orient :left
                                      :ticks  0)
        label-coords       {:x (int (x-scale fire-score))
                            :y (int (y-scale y-max))}
        histogram-midpoint (x-scale (/ (+ x-min x-max) 2))
        base-svg           (svg [:svg.histogram {:width  svg-width
                                                 :height svg-height}
                                 [:rect]
                                 [:text]
                                 [:g {:class     "x-axis"
                                      :transform (str "translate(0,"
                                                      (- svg-height
                                                         (:bottom svg-padding))
                                                      ")")}]
                                 [:g {:class     "y-axis"
                                      :transform (str "translate("
                                                      (:left svg-padding)
                                                      ",0)")}]])]
    (ef/at top-node
      "ul"   (ef/after base-svg)
      "rect" (em/clone-for [{:keys [midpoint width percent]} histogram-data]
               (ef/set-attr :x      (int (x-scale (- midpoint (/ width 2))))
                            :y      (int (y-scale percent))
                            :width  (max 1 (- (int (x-scale width))
                                              (:left svg-padding)
                                              bar-padding))
                            :height (- svg-height
                                       (:bottom svg-padding)
                                       (int (y-scale percent)))
                            :fill   (str "rgb("
                                         (Math/round (* midpoint 2.55))
                                         ","
                                         (Math/round (* (- 100 midpoint) 2.55))
                                         ",0)")))
      "text" (ef/do-> (ef/content (if (< (:x label-coords) histogram-midpoint)
                                    (str "\u2193 Your score: "
                                         (Math/round fire-score))
                                    (str "Your score: "
                                         (Math/round fire-score)
                                         " \u2193")))
                      (ef/set-attr :x           (if (< (:x label-coords)
                                                       histogram-midpoint)
                                                  (- (:x label-coords) 3)
                                                  (+ (:x label-coords) 3))
                                   :y           (- (:y label-coords) 3)
                                   :text-anchor (if (< (:x label-coords)
                                                       histogram-midpoint)
                                                  "start"
                                                  "end")
                                   :font-family "sans-serif"
                                   :font-weight "bold"
                                   :font-size   "9px"
                                   :fill        "red"))
      ".x-axis" x-axis
      ".y-axis" y-axis)))
