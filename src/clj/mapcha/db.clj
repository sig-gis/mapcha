(ns mapcha.db
  (:require [cemerick.friend.credentials :refer [hash-bcrypt]]
            [shoreleave.middleware.rpc   :refer [defremote]]
            [clojure.java.jdbc           :refer [with-db-transaction]]
            [yesql.core                  :refer [defqueries]]
            [postal.core                 :refer [send-message]]
            [clojure.string              :as str]
            [clojure.java.io             :as io]
            [clojure.data.csv            :as csv]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;
;;; The mighty db-spec
;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defonce db-spec
  {:classname   "org.postgresql.Driver"
   :subprotocol "postgresql"
   :subname     "//localhost:5432/mapcha"
   :user        "mapcha"})

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

(defn square-distance
  [x1 y1 x2 y2]
  (+ (Math/pow (- x2 x1) 2.0)
     (Math/pow (- y2 y1) 2.0)))

(defn create-gridded-sample-set
  [plot-x plot-y buffer-radius sample-resolution]
  (let [left           (- plot-x buffer-radius)
        right          (+ plot-x buffer-radius)
        top            (- plot-y buffer-radius)
        bottom         (+ plot-y buffer-radius)
        radius-squared (* buffer-radius buffer-radius)]
    (for [x (range left (+ right sample-resolution) sample-resolution)
          y (range top (+ bottom sample-resolution) sample-resolution)
          :when (< (square-distance x y plot-x plot-y) radius-squared)]
      [x y])))

(defn create-random-sample-set
  [plot-x plot-y buffer-radius samples-per-plot]
  (for [offset-angle (repeatedly samples-per-plot #(rand (* 2.0 Math/PI)))]
    (let [offset-magnitude (rand buffer-radius)
          x-offset         (* offset-magnitude (Math/cos offset-angle))
          y-offset         (* offset-magnitude (Math/sin offset-angle))]
      [(+ plot-x x-offset)
       (+ plot-y y-offset)])))

(defn get-imagery-id-by-title
  [imagery-title]
  (:id (first (get-imagery-info-sql {:title imagery-title}))))

(defn create-random-points-in-bounds
  [lat-min lat-max lon-min lon-max num-points]
  (let [lon-range (- lon-max lon-min)
        lat-range (- lat-max lat-min)]
    (repeatedly num-points
                #(vector
                  (+ lat-min (rand lat-range))
                  (+ lon-min (rand lon-range))))))

(defn create-new-project
  [{:keys [project-name project-description boundary-lon-min boundary-lon-max
           boundary-lat-min boundary-lat-max plots buffer-radius sample-type
           samples-per-plot sample-resolution sample-values imagery-selector]}]
  (try
    (with-db-transaction [conn db-spec]
      ;; 1. Insert project-name, project-description, and boundary (as a
      ;;    polygon) into mapcha.projects.

      (let [boundary-lon-min  (Double/parseDouble boundary-lon-min)
            boundary-lon-max  (Double/parseDouble boundary-lon-max)
            boundary-lat-min  (Double/parseDouble boundary-lat-min)
            boundary-lat-max  (Double/parseDouble boundary-lat-max)
            buffer-radius     (Double/parseDouble buffer-radius)
            samples-per-plot  (if (= sample-type "random")
                                (Integer/parseInt samples-per-plot))
            sample-resolution (if (= sample-type "gridded")
                                (Double/parseDouble sample-resolution)
                                -1.0)
            imagery-id        (get-imagery-id-by-title imagery-selector)
            project-info (first
                          (add-project-sql {:name              project-name
                                            :description       project-description
                                            :lon_min           boundary-lon-min
                                            :lon_max           boundary-lon-max
                                            :lat_min           boundary-lat-min
                                            :lat_max           boundary-lat-max
                                            :sample_resolution sample-resolution
                                            :imagery_id        imagery-id}
                                           {:connection conn}))
            project-id   (project-info :id)
            plots        (if (string? plots)
                           (create-random-points-in-bounds
                            boundary-lat-min
                            boundary-lat-max
                            boundary-lon-min
                            boundary-lon-max
                            (Integer/parseInt plots))
                           plots)]

        ;; 2. Insert plot rows into mapcha.plots with random centers
        ;;    within boundary and radius=buffer-radius.

        (doseq [[plot-lat plot-lon] plots]
          (let [plot-info (first
                           (add-plot-sql {:project_id project-id
                                          :lon        plot-lon
                                          :lat        plot-lat
                                          :radius     buffer-radius}
                                         {:connection conn}))
                plot-id   (plot-info :id)
                plot-x    (plot-info :web_mercator_x)
                plot-y    (plot-info :web_mercator_y)]

            ;; 3. Insert sample rows into mapcha.samples for each plot
            ;;    with the spatial distribution specified by sample-type.

            (doseq [[x y] (if (= sample-type "gridded")
                            (create-gridded-sample-set plot-x
                                                       plot-y
                                                       buffer-radius
                                                       sample-resolution)
                            (create-random-sample-set plot-x
                                                      plot-y
                                                      buffer-radius
                                                      samples-per-plot))]
              (add-sample-sql {:plot_id  plot-id
                               :sample_x x
                               :sample_y y}
                              {:connection conn}))))

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

(defn load-csv [csv-filename]
  (with-open [in-file (io/reader (io/resource (str "csv/" csv-filename)))]
    (mapv (fn [[lat lon]]
            [(Double/parseDouble lat)
             (Double/parseDouble lon)])
          (rest (csv/read-csv in-file)))))

(defn wrap-lat [x]
  (- (mod (+ x 90) 180) 90))

(defn wrap-lon [x]
  (- (mod (+ x 180) 360) 180))

(defn create-new-project-from-csv
  [{:keys [project-name project-description plot-csv buffer-radius sample-type
           samples-per-plot sample-resolution sample-values imagery-selector]
    :as params}]
  (let [plot-points   (load-csv plot-csv)
        latitudes     (map first plot-points)
        longitudes    (map second plot-points)
        min-lat       (wrap-lat (- (reduce min latitudes)  0.1))
        min-lon       (wrap-lon (- (reduce min longitudes) 0.1))
        max-lat       (wrap-lat (+ (reduce max latitudes)  0.1))
        max-lon       (wrap-lon (+ (reduce max longitudes) 0.1))]
    (create-new-project
     (assoc params
            :boundary-lon-min (str min-lon)
            :boundary-lon-max (str max-lon)
            :boundary-lat-min (str min-lat)
            :boundary-lat-max (str max-lat)
            :plots            plot-points))))

;; Here is an example of project creation from a CSV with 2 columns
;; ("latitude", "longitude"), which must be stored in the
;; resources/csv/ directory.
;;
;; (create-new-project-from-csv
;;  {:project-name "NASA SERVIR Chipset Test"
;;   :project-description ""
;;   :plot-csv "chip_2002_centerPoints.csv"
;;   :buffer-radius "75.0"
;;   :sample-type "gridded"
;;   :sample-resolution "30.0"
;;   :sample-values "[[\"Chipset Point\" \"#0dee0b\" \"\"] [\"No Imagery\" \"#ec1717\" \"\"]]"
;;   :imagery-selector "NASASERVIRChipset2002"})

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
  (first (get-random-plot-by-min-analyses-no-flag-check-sql
          {:project_id project-id})))

(defremote get-sample-points
  [plot-id]
  (get-sample-points-sql {:plot_id plot-id}))

(defremote add-user-samples
  [user-id plot-id imagery-title sample-value-map]
  (increment-plot-analyses-sql {:plot_id plot-id})
  (doseq [[sample-id value-id] sample-value-map]
    (add-user-sample-sql {:user_id    user-id
                          :sample_id  sample-id
                          :value_id   value-id
                          :imagery_id (get-imagery-id-by-title imagery-title)})))

(defremote archive-project
  [project-id]
  (first (archive-project-sql {:project_id project-id})))

(defremote revive-project
  [project-id]
  (first (revive-project-sql {:project_id project-id})))

(defremote flag-plot
  [plot-id]
  (first (flag-plot-sql {:plot_id plot-id})))

;; Filename: mapcha_<project>_<date>.csv
;; Fields: plot_id, center_lon, center_lat, radius_m, sample_points,
;;         user_assignments, value1_%, value2_%, ..., valueN_%
(defremote dump-project-aggregate-data
  [project-id]
  (let [sample-values   (->> (get-sample-values project-id)
                             (sort-by :id)
                             (mapv :value))
        plot-data-raw   (dump-project-aggregate-data-sql {:project_id project-id})
        plot-data-clean (for [[plot-id records] (group-by :plot_id plot-data-raw)]
                          (if (= 1 (count records))
                            (let [record (first records)]
                              (-> record
                                  (assoc :values {(:value record)
                                                  (:percent record)})
                                  (dissoc :value :percent)))
                            (-> (first records)
                                (dissoc :value :percent)
                                (assoc :values
                                       (into {}
                                             (for [{:keys [value percent]} records]
                                               [value percent]))))))
        plot-data-table (cons
                         (map str/upper-case
                              (concat ["plot_id" "center_lon" "center_lat"
                                       "radius_m" "flagged" "analyses"
                                       "sample_points" "user_assignments"]
                                      sample-values))
                         (mapv
                          (fn [{:keys [plot_id center_lon center_lat radius_m
                                       flagged analyses sample_points
                                       user_assignments values]}]
                            (concat [plot_id center_lon center_lat radius_m
                                     flagged analyses sample_points
                                     user_assignments]
                                    (mapv #(values % 0.0) sample-values)))
                          plot-data-clean))
        project-name    (-> (get-project-info project-id)
                            (:name)
                            (str/replace #" " "_")
                            (str/replace #"," "")
                            (str/lower-case))
        current-date    (str (java.time.LocalDate/now))
        output-url      (str "/downloads/mapcha_" project-name "_"
                             current-date ".csv")
        output-filename (str "resources/public" output-url)]
    (with-open [out-file (io/writer output-filename)]
      (csv/write-csv out-file plot-data-table))
    output-url))
