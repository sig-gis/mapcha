(ns mapcha.ajax
  (:require [ring.util.response :refer [response content-type]]
            [cemerick.friend    :refer [identity] :rename {identity auth-map}]
            [mapcha.db          :refer [run-fire-score-query add-user-report]])
  (:import java.net.URLEncoder))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;
;;; AJAX response pages
;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; FIXME: Find an open source geocoding alternative to the Google Maps API
(defn geocode
  [request]
  (let [address       (-> request :params :address (URLEncoder/encode "UTF-8"))
        request-url   (str "https://maps.googleapis.com/maps/api/geocode/json?"
                           "address=" address "&sensor=false")
        response-json (slurp request-url)]
    (println "Request URL:" request-url)
    (println "Response JSON:" response-json)
    (-> (response response-json)
        (content-type "text/json"))))

(defn miles->meters [miles] (* miles 1600))

(defn parse-double [s] (Double. s))

(defn fire-score [request]
  (let [as-double                 #(-> request :params % parse-double)
        address-lon               (-> :address-lon as-double)
        address-lat               (-> :address-lat as-double)
        fire-risk-radius          (-> :fire-risk-radius as-double miles->meters)
        fire-risk-power-factor    (-> :fire-risk-power-factor as-double)
        fire-hazard-radius        (-> :fire-hazard-radius as-double miles->meters)
        fire-hazard-power-factor  (-> :fire-hazard-power-factor as-double)
        fire-weather-radius       (-> :fire-weather-radius as-double miles->meters)
        fire-weather-power-factor (-> :fire-weather-power-factor as-double)
        fire-risk?                (-> request :params :fire-risk?)
        fire-hazard?              (-> request :params :fire-hazard?)
        fire-weather?             (-> request :params :fire-weather?)
        query-result              {:fire-risk    (if (not= fire-risk? "null")
                                                   (run-fire-score-query
                                                    :fire-risk
                                                    address-lon
                                                    address-lat
                                                    fire-risk-radius
                                                    fire-risk-power-factor))
                                   :fire-hazard  (if (not= fire-hazard? "null")
                                                   (run-fire-score-query
                                                    :fire-hazard
                                                    address-lon
                                                    address-lat
                                                    fire-hazard-radius
                                                    fire-hazard-power-factor))
                                   :fire-weather (if (not= fire-weather? "null")
                                                   (run-fire-score-query
                                                    :fire-weather
                                                    address-lon
                                                    address-lat
                                                    fire-weather-radius
                                                    fire-weather-power-factor))}]
    (println query-result)
    (response (pr-str query-result))))

(defn save-report
  [request]
  (let [cleaned-params (into {} (remove #(= "null" (val %)) (request :params)))
        query-result   (add-user-report
                        (:current (auth-map request))
                        (->     cleaned-params :address)
                        (->     cleaned-params :longitude           parse-double)
                        (->     cleaned-params :latitude            parse-double)
                        (some-> cleaned-params :fire-risk-mean      parse-double)
                        (some-> cleaned-params :fire-risk-stddev    parse-double)
                        (some-> cleaned-params :fire-hazard-mean    parse-double)
                        (some-> cleaned-params :fire-hazard-stddev  parse-double)
                        (some-> cleaned-params :fire-weather-mean   parse-double)
                        (some-> cleaned-params :fire-weather-stddev parse-double)
                        (some-> cleaned-params :combined-score      parse-double)
                        (->     cleaned-params :cost                parse-double))]
    (println query-result)
    (response (pr-str query-result))))
