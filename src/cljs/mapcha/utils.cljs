(ns mapcha.utils
  (:require [goog.Uri.QueryData :as gqd]
            [clojure.string     :as s]
            [enfocus.core       :as ef])
  (:require-macros [enfocus.macros :as em]))

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

(defn get-attribute [selector attribute]
  (ef/from selector (ef/get-attr attribute)))

(defn get-value [selector]
  (ef/from selector (ef/get-prop :value)))

(defn get-text [selector]
  (ef/from selector (ef/get-text)))

(em/defaction show-element [selector]
  selector (ef/set-style :visibility "visible"))

(em/defaction hide-element [selector]
  selector (ef/set-style :visibility "hidden"))
