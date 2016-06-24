(ns mapcha.db
  (:require [cemerick.friend.credentials :refer [hash-bcrypt]]
            [clojure.java.jdbc           :refer [with-db-transaction]]
            [yesql.core                  :refer [defquery defqueries]]
            [postal.core                 :refer [send-message]])
  (:import org.postgresql.jdbc.PgArray))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;
;;; The mighty db-spec
;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defonce db-spec
  {:classname   "org.postgresql.Driver"
   :subprotocol "postgresql"
   :subname     "//localhost:5432/mapcha"
   :user        "gjohnson"})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;
;;; User account functions
;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defqueries "sql/user_management.sql" {:connection db-spec})

(defn find-user-info [email]
  (with-db-transaction [conn db-spec]
    (if-let [user-info (first (find-user-info-sql {:email email}
                                                  {:connection conn}))]
      (update-in user-info [:roles] #(set (map keyword (.getArray ^PgArray %)))))))

(defn add-user [email password role]
  (when-not (find-user-info email)
    (with-db-transaction [conn db-spec]
      (let [user-info (first (add-user-sql {:email    email
                                            :password (hash-bcrypt password)}
                                           {:connection conn}))
            user-role (first (add-user-role-sql {:email email
                                                 :role  (name role)}
                                                {:connection conn}))]
        {:identity (:identity user-info)
         :password (:password user-info)
         :roles    (hash-set (keyword (:role user-role)))}))))

(defn set-user-email [old-email new-email]
  (when-not (find-user-info new-email)
    (first (set-user-email-sql {:new_email new-email :old_email old-email}))))

(defn set-user-password [email password]
  (first (set-user-password-sql {:password (hash-bcrypt password) :email email})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;
;;; User report functions
;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn find-user-reports
  [email]
  (find-user-reports-sql {:email email}))

(defn add-user-report
  [email address longitude latitude fire_risk_mean fire_risk_stddev fire_hazard_mean
   fire_hazard_stddev fire_weather_mean fire_weather_stddev combined_score cost]
  (first (add-user-report-sql {:email email
                               :address address
                               :longitude longitude
                               :latitude latitude
                               :fire_risk_mean fire_risk_mean
                               :fire_risk_stddev fire_risk_stddev
                               :fire_hazard_mean fire_hazard_mean
                               :fire_hazard_stddev fire_hazard_stddev
                               :fire_weather_mean fire_weather_mean
                               :fire_weather_stddev fire_weather_stddev
                               :combined_score combined_score
                               :cost cost})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;
;;; Password reset functions
;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn random-hex-string [length]
  (->> #(rand-nth [1 2 3 4 5 6 7 8 9 0 \a \b \c \d \e \f])
       (repeatedly length)
       (apply str)))

(defn add-password-reset-key [email]
  (-> {:email email :reset_key (random-hex-string 40)}
      (set-password-reset-key-sql)
      (first)
      (:reset_key)))

(defn remove-password-reset-key [email]
  (-> {:email email :reset_key nil}
      (set-password-reset-key-sql)
      (first)))

(defn send-password-reset-key
  [email reset-key]
  (let [instructions (str "Hi " email ",\n\n"
                          "  To reset your password, simply type this passphrase "
                          "into the 'Password reset key' textfield on the password "
                          "page.\n\n"
                          reset-key
                          "\n\n"
                          "If you have already closed the Password Reset page, "
                          "you can navigate back to it here:\n\n"
                          "  https://mapcha.sig-gis.com/password-reset?email=" email
                          "&password-reset-key=" reset-key)
        send-results (send-message ^{:host "smtp.gmail.com"
                                     :user "gjohnson@sig-gis.com"
                                     :pass "51451897f9"
                                     :port 587
                                     :tls  true}
                                   {:from    "gjohnson@sig-gis.com"
                                    :to      email
                                    :subject "Mapcha Reset Password Instructions"
                                    :body    instructions})]
    (= (send-results :error) :SUCCESS)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;
;;; Fire score queries
;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defquery query-fire-risk-sql    "sql/query_fire_risk.sql"    {:connection db-spec})
(defquery query-fire-hazard-sql  "sql/query_fire_hazard.sql"  {:connection db-spec})
(defquery query-fire-weather-sql "sql/query_fire_weather.sql" {:connection db-spec})

(defonce query-fns
  {:fire-risk    query-fire-risk-sql
   :fire-hazard  query-fire-hazard-sql
   :fire-weather query-fire-weather-sql})

(defn run-fire-score-query
  [fire-score-type address-lon address-lat radius power-factor]
  (let [query-fn (query-fns fire-score-type)
        results  (query-fn {:lon          address-lon
                            :lat          address-lat
                            :radius       radius
                            :power_factor power-factor})
        {:keys [global_mean global_stddev local_mean local_stddev]} (first results)]
    {:global-mean   global_mean
     :global-stddev global_stddev
     :local-mean    local_mean
     :local-stddev  local_stddev
     :histogram     (map #(select-keys % [:midpoint :width :percent]) results)}))
