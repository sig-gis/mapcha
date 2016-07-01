(ns mapcha.utils
  (:require [goog.Uri.QueryData :as gqd]
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
