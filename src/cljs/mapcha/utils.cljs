(ns mapcha.utils
  (:require [goog.Uri.QueryData :as gqd]
            [goog.style         :as style]
            [clojure.string     :as s]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;
;;; Utility functions
;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn log [& args]
  (.log js/console (apply str args)))

(defn log-state-changes [key atom old-val new-val]
  (log (s/capitalize (name key)) " changed from " old-val " to " new-val "."))

(defn no-newlines [string]
  (s/replace string "\n" " "))

(defn ajax-format [obj]
  (.toString (gqd/createFromMap obj)))

(defn disable-element! [elem]
  (set! (.-disabled elem) true)
  (style/setStyle elem "opacity" "0.5"))

(defn enable-element! [elem]
  (set! (.-disabled elem) false)
  (style/setStyle elem "opacity" "1.0"))
