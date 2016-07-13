(ns mapcha.db
  (:require [cemerick.friend.credentials :refer [hash-bcrypt]]
            [shoreleave.middleware.rpc   :refer [defremote]]
            [clojure.java.jdbc           :refer [with-db-transaction]]
            [yesql.core                  :refer [defqueries]]
            [postal.core                 :refer [send-message]]
            [clojure.string              :as str]))

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
  (when-let [user-info (first (find-user-info-sql {:email email}))]
    (-> user-info
        (assoc :roles (hash-set (keyword (:role user-info))))
        (dissoc :role))))

(defn add-user [email password role]
  (when-not (find-user-info email)
    (let [user-info (first (add-user-sql {:email    email
                                          :password (hash-bcrypt password)
                                          :role     (name role)}))]
      {:identity (:email user-info)
       :password (:password user-info)
       :roles    (hash-set (keyword (:role user-info)))})))

(defn set-user-email [old-email new-email]
  (when-not (find-user-info new-email)
    (first (set-user-email-sql {:new_email new-email :old_email old-email}))))

(defn set-user-password [email password]
  (first (set-user-password-sql {:password (hash-bcrypt password) :email email})))

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
;;; Project management functions
;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defqueries "sql/project_management.sql" {:connection db-spec})

(defn create-new-project
  [{:keys [project-name project-description boundary-lon-min
           boundary-lon-max boundary-lat-min boundary-lat-max
           plots buffer-radius samples-per-plot sample-values]}]
  (try
    (with-db-transaction [conn db-spec]
      ;; 1. Insert project-name, project-description, and boundary (as a
      ;;    polygon) into mapcha.projects.

      (let [boundary-lon-min (Double/parseDouble boundary-lon-min)
            boundary-lon-max (Double/parseDouble boundary-lon-max)
            boundary-lat-min (Double/parseDouble boundary-lat-min)
            boundary-lat-max (Double/parseDouble boundary-lat-max)
            plots            (Integer/parseInt plots)
            buffer-radius    (Double/parseDouble buffer-radius)
            samples-per-plot (Integer/parseInt samples-per-plot)
            project-info (first
                          (add-project-sql {:name        project-name
                                            :description project-description
                                            :lon_min     boundary-lon-min
                                            :lon_max     boundary-lon-max
                                            :lat_min     boundary-lat-min
                                            :lat_max     boundary-lat-max}
                                           {:connection conn}))
            project-id   (project-info :id)
            lon-range    (- boundary-lon-max boundary-lon-min)
            lat-range    (- boundary-lat-max boundary-lat-min)]

        ;; 2. Insert plots rows into mapcha.plots with random centers
        ;;    within boundary and radius=buffer-radius.

        (dotimes [_ plots]
          (let [plot-lon  (+ boundary-lon-min (rand lon-range))
                plot-lat  (+ boundary-lat-min (rand lat-range))
                plot-info (first
                           (add-plot-sql {:project_id project-id
                                          :lon        plot-lon
                                          :lat        plot-lat
                                          :radius     buffer-radius}
                                         {:connection conn}))
                plot-id   (plot-info :id)
                plot-x    (plot-info :web_mercator_x)
                plot-y    (plot-info :web_mercator_y)]

            ;; 3. Insert samples-per-plot rows into mapcha.samples for each
            ;;    plot with a randomly selected point within the plot's
            ;;    buffer

            (dotimes [_ samples-per-plot]
              (let [offset-angle     (rand (* 2.0 Math/PI))
                    offset-magnitude (rand buffer-radius)
                    x-offset         (* offset-magnitude (Math/cos offset-angle))
                    y-offset         (* offset-magnitude (Math/sin offset-angle))]
                (add-sample-sql {:plot_id  plot-id
                                 :sample_x (+ plot-x x-offset)
                                 :sample_y (+ plot-y y-offset)}
                                {:connection conn})))))

        ;; 4. Read sample-values (EDN string -> [[name color image]*])
        ;;    and insert them into mapcha.sample_values.

        (doseq [[name color image] (read-string sample-values)]
          (add-sample-value-sql {:project_id project-id
                                 :value      name
                                 :color      color
                                 :image      image}
                                {:connection conn})))
      true)
    (catch Exception e false)))

(defremote get-all-projects
  []
  (get-all-projects-sql))

(defremote get-project-info
  [project-id]
  (first (get-project-info-sql {:project_id project-id})))

(defremote get-sample-values
  [project-id]
  (get-sample-values-sql {:project_id project-id}))

(defremote get-random-plot
  [project-id]
  (first (get-random-plot-sql {:project_id project-id})))

(defremote get-sample-points
  [plot-id]
  (get-sample-points-sql {:plot_id plot-id}))

(defremote add-user-samples
  [user-id imagery-id sample-value-map]
  (doseq [[sample-id value-id] sample-value-map]
    (add-user-sample-sql {:user_id   user-id
                          :sample_id sample-id
                          :value_id  value-id
                          :imagery_id imagery-id})))

(defremote archive-project
  [project-id]
  (first (archive-project-sql {:project_id project-id})))

(defremote revive-project
  [project-id]
  (first (revive-project-sql {:project_id project-id})))
