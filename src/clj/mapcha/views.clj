(ns mapcha.views
  (:require [ring.util.response          :refer [status]]
            [cemerick.friend             :as    friend]
            [cemerick.friend.credentials :refer [bcrypt-credential-fn]]
            [hiccup.page                 :refer [html5 include-css include-js]]
            [hiccup.element              :refer [javascript-tag link-to image]]
            [mapcha.validation           :refer [is-email? validate-params]]
            [mapcha.db                   :refer [find-user-info
                                                 set-user-email
                                                 set-user-password
                                                 add-password-reset-key
                                                 remove-password-reset-key
                                                 send-password-reset-key
                                                 create-new-project
                                                 get-all-projects
                                                 get-sample-values]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;
;;; Friend helper functions
;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn logged-in?
  [request]
  (boolean (friend/identity request)))

(defn current-user-id
  [request]
  (when-let [{:keys [current authentications]} (friend/identity request)]
    (:id (authentications current))))

(defn current-email
  [request]
  (:current (friend/identity request)))

(defn current-role
  [request]
  (when-let [{:keys [current authentications]} (friend/identity request)]
    (first (:roles (authentications current)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;
;;; Site-wide header
;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn wrap-http-response
  [body]
  {:status  200
   :headers {"Content-Type" "text/html"}
   :body    body})

(defn wrap-header
  [request & content]
  (let [description (str "Mapcha is an Image Analysis Crowdsourcing Platform "
                         "by Spatial Informatics Group")
        keywords    (str "mapcha image analysis crowdsourcing platform asia "
                         "mekong cambodia thailand laos vietnam myanmar SIG "
                         "spatial informatics group")
        html5shiv   (str "<!--[if lt IE 9]><script type=\"text/javascript\" "
                         "src=\"/js/html5shiv.js\"></script><![endif]-->")
        link-active (fn [url label]
                      (if (= url (:uri request))
                        (link-to {:class "active-link"} url label)
                        (link-to url label)))]
    (wrap-http-response
     (html5
      {:lang "en"}
      [:head
       [:title "Mapcha"]
       [:meta {:charset "utf-8"}]
       [:meta {:name "description" :content description}]
       [:meta {:name "keyword" :content keywords}]
       [:meta {:name "viewport" :content "width=device-width, initial-scale=1.0"}]
       [:link {:rel "shortcut icon" :href "/favicon.ico"}]
       (include-css "/css/cssreset-min.css"
                    "/css/openlayers_3.13.0.css"
                    "/css/stylesheet.css")
       html5shiv]
      [:body
       [:header
        [:div#logos
         (image {:id "usaid"} "/img/usaid.png")
         (image {:id "nasa"} "/img/nasa.png")
         (image {:id "adpc"} "/img/ADPC.jpg")
         (image {:id "servir"} "/img/servir.png")]
        [:nav
         [:ul
          (case (current-role request)
            :admin (list
                    [:li (link-active "/" "Home")]
                    [:li (link-active "/about" "About")]
                    [:li (link-active "/account" "Account")]
                    [:li (link-active "/dashboard" "Dashboard")]
                    [:li (link-active "/admin" "Admin")])
            :user  (list
                    [:li (link-active "/" "Home")]
                    [:li (link-active "/about" "About")]
                    [:li (link-active "/account" "Account")]
                    [:li (link-active "/dashboard" "Dashboard")])
            (list
             [:li (link-active "/" "Home")]
             [:li (link-active "/about" "About")]))]]
        [:div#login-info
         [:p
          (if (logged-in? request)
            (list
             (str "Logged in as " (current-email request) " ")
             (link-active "/logout" "Logout"))
            (list
             (link-active "/login" "Login")))]]]
       [:section#content
        (when-let [message (:flash request)]
          (if (string? message)
            [:p {:class "alert"} message]
            (for [sub-message message]
              [:p {:class "alert"} sub-message])))
        content]]))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;
;;; Home page
;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn home-page
  [request]
  (wrap-header
   request
   [:div#home
    [:h1 "Mapcha"]
    [:h2 "Earth Image Identification"]
    [:h3 "Collaborate. Play. Map the world."]
    [:hr]
    [:p
     "Mapcha is a collaborative effort between its developers and its "
     "community of users. We welcome suggestions for improvements on our "
     (link-to "https://github.com/sig-gis/mapcha/issues" "Github")
     " issues page."]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;
;;; About page
;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn about-page
  [request]
  (wrap-header
   request
   [:div#about
    [:h1 "About Mapcha"]
    [:p (str "Mapcha is a custom built, open-source, high resolution satellite"
             " image viewing and interpretation system that is being developed"
             " by SERVIR-Mekong as a tool for use in projects that require land"
             " cover and/or land use reference data. Mapcha promotes consistency"
             " in locating, interpreting, and labeling reference data plots for"
             " use in classifying and monitoring land cover / land use change."
             " The full functionality of Mapcha including collaborative"
             " compilation of reference point databases is implemented online"
             " so there is no need for desktop installation.")]
    (link-to "http://www.sig-gis.com" (image "/img/sig-logo.png"))
    [:p "Copyright &copy; " (link-to "http://www.sig-gis.com" "SIG-GIS") " 2016"]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;
;;; Login page
;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn login-page
  [request]
  (wrap-header
   request
   [:div#login-form
    [:h1 "Sign into your account"]
    [:form {:method "post" :action "/login"}
     [:input {:type "email" :name "email" :value "" :placeholder "Email"}]
     [:input {:type "password" :name "password" :value "" :placeholder "Password"}]
     [:p#forgot-password (link-to "/password" "Forgot your password?")]
     [:input.button {:type "submit" :name "login" :value "Login"}]]
    [:hr]
    [:h1 "New to Mapcha?"]
    [:input.button {:type "button" :name "register"
                    :value "Register a new account"
                    :onclick "window.location='/register'"}]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;
;;; Register page
;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn register-page
  [request]
  (wrap-header
   request
   [:div#register-form
    [:h1 "Register a new account"]
    [:form {:method "post" :action "/register"}
     [:input {:type "email" :name "email" :value ""
              :placeholder "Email" :autocomplete "off"}]
     [:input {:type "password" :name "password" :value ""
              :placeholder "Password" :autocomplete "off"}]
     [:input {:type "password" :name "password-confirmation" :value ""
              :placeholder "Password confirmation" :autocomplete "off"}]
     [:input.button {:type "submit" :name "register" :value "Register"}]]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;
;;; Password Reset page
;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn try-reset-password
  [{:keys [email password password-reset-key] :as params}]
  (if-let [errors (validate-params params)]
    errors
    (if-let [user-info (find-user-info email)]
      (if (= password-reset-key (:reset_key user-info))
        (do (set-user-password email password)
            (remove-password-reset-key email)
            (str "Password updated successfully for " email "."))
        (str "Incorrect password reset key for " email "."))
      (str "No account found for " email "."))))

(defn password-reset-page
  [request]
  (let [email              (-> request :params :email)
        password-reset-key (-> request :params :password-reset-key)]
    (wrap-header
     (if (= (:request-method request) :post)
       (assoc request :flash (try-reset-password (:params request)))
       request)
     [:div#password-form
      [:h1 "Enter your reset info"]
      [:form {:method "post" :action "/password-reset"}
       [:input#email {:type "email" :name "email" :value email
                      :placeholder "Email" :autocomplete "off"}]
       [:input {:type "text" :name "password-reset-key" :value password-reset-key
                :placeholder "Password reset key" :autocomplete "off"}]
       [:input {:type "password" :name "password" :value ""
                :placeholder "New password" :autocomplete "off"}]
       [:input {:type "password" :name "password-confirmation" :value ""
                :placeholder "New password confirmation" :autocomplete "off"}]
       [:input.button {:type "submit" :value "Reset Password"}]]])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;
;;; Password page
;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn send-password-reset-instructions
  [email]
  (if-not (.isEmpty email)
    (if-not (is-email? email)
      (str email " is not a valid email address.")
      (if-not (:password (find-user-info email))
        (str "No password found for " email ".")
        (let [reset-key (add-password-reset-key email)]
          (if (send-password-reset-key email reset-key)
            (str "Password reset instructions successfully sent to " email ".")
            (do (remove-password-reset-key email)
                (str "Sending password reset info to " email " failed!"))))))))

(defn password-page
  [request]
  (let [email         (-> request :params :email)
        flash-message (when (= (:request-method request) :post)
                        (send-password-reset-instructions email))]
    (if (and flash-message (.startsWith flash-message "Password reset"))
      (password-reset-page
       (-> request
           (assoc :request-method :get)
           (assoc :flash flash-message)
           (assoc-in [:params :email] email)))
      (wrap-header
       (assoc request :flash flash-message)
       [:div#password-form
        [:h1 "Enter your login email"]
        [:form {:method "post" :action "/password"}
         [:input#email {:type "email" :name "email" :value ""
                        :placeholder "Email" :autocomplete "off"}]
         [:input.button {:type "submit" :value "Request Password Reset Key"}]]]))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;
;;; Select Project page
;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn select-project
  [request]
  (wrap-header
   request
   [:div#select-project-form
    [:h1 "Select a Project"]
    [:ul
     (for [{:keys [id name]} (get-all-projects)]
       [:li (link-to (str "/dashboard?project=" id) name)])]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;
;;; Account page
;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn account-page
  [request]
  (wrap-header
   request
   [:div#account-form
    [:h1 "Account Settings"]
    [:form {:method "post" :action "/account"}
     [:h2 "Reset email"]
     [:input {:type "email" :name "email" :value ""
              :placeholder "New email" :autocomplete "off"}]
     [:h2 "Reset password"]
     [:input {:type "password" :name "password" :value ""
              :placeholder "New password" :autocomplete "off"}]
     [:input {:type "password" :name "password-confirmation" :value ""
              :placeholder "New password confirmation" :autocomplete "off"}]
     [:h2 "Verify your identity"]
     [:input {:type "password" :name "current-password" :value ""
              :placeholder "Current password" :autocomplete "off"}]
     [:input.button {:type "submit" :name "update-account"
                     :value "Update account settings"}]]]))

(defn authenticate-user
  [email password]
  (bcrypt-credential-fn find-user-info {:username email :password password}))

(defn update-email
  [current-email new-email]
  (if-not (.isEmpty new-email)
    (if-not (is-email? new-email)
      (str new-email " is not a valid email address.")
      (if (set-user-email current-email new-email)
        (str "Email updated to " new-email ".")
        (str "An account named " new-email " already exists.")))))

(defn update-password
  [current-email password password-confirmation]
  (if-not (.isEmpty password)
    (if (< (count password) 8)
      "Password must be at least 8 characters."
      (if-not (= password password-confirmation)
        "Password and Password confirmation do not match."
        (do (set-user-password current-email password)
            "Password updated successfully.")))))

(defn switch-user
  [new-user request]
  (let [new-auth (dissoc (find-user-info new-user) :password)]
    (-> request
        (assoc-in [:session :cemerick.friend/identity :current]
                  new-user)
        (assoc-in [:session :cemerick.friend/identity :authentications]
                  {new-user new-auth}))))

(defn update-account-page
  [{{:keys [email password password-confirmation current-password]} :params
    :as request}]
  (let [current-email (current-email request)]
    (if (authenticate-user current-email current-password)
      (let [request (assoc request :flash
                           (remove nil?
                                   [(update-password current-email
                                                     password
                                                     password-confirmation)
                                    (update-email current-email email)]))]
        (if (some #(.startsWith % "Email updated") (:flash request))
          (switch-user email (account-page (switch-user email request)))
          (account-page request)))
      (account-page
       (assoc request :flash (str "User verification failed! Incorrect "
                                  "password for user " current-email "."))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;
;;; Dashboard page
;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn dashboard-page
  [request]
  (wrap-header
   request
   [:div#dashboard
    [:input#quit-button.button {:type "button" :name "dashboard-quit" :value "Quit"
                                :onclick "window.location='/select-project'"}]
    [:div#image-analysis-pane]
    [:div#sidebar]
    [:p#imagery-info (str "DigitalGlobe Maps API: Recent Imagery+Streets | "
                          "June 2015 | "
                          "© DigitalGlobe, Inc")]
    [:input#user-id {:type "hidden" :name "user-id"
                     :value (str (current-user-id request))}]
    [:input#initial-project-id {:type "hidden" :name "initial-project-id"
                                :value (str (-> request :params :project))}]]
   (include-js "/mapcha.js")
   (javascript-tag "mapcha.dashboard.main()")))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;
;;; Admin page
;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn admin-page
  [request]
  (wrap-header
   request
   [:div#admin
    [:h1 "Project Management"]
    [:div#create-project-form]]
   (include-js "/mapcha.js")
   (javascript-tag "mapcha.admin.main()")))

(defn create-new-project-page
  [request]
  (let [flash-message (if (create-new-project (:params request))
                        (str "New project " (-> request :params :project-name)
                             " created and launched!")
                        "Error with project creation!")]
    (admin-page (assoc request :flash flash-message))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;
;;; Error pages
;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn access-denied-page
  [request]
  (-> (wrap-header
       request
       [:div#access-denied
        [:h3 "Access Denied"]
        [:p.error-message (str "Sorry, you are not authorized"
                               " to access this resource.")]])
      (status 401)))

(defn page-not-found-page
  [request]
  (-> (wrap-header
       request
       [:div#page-not-found
        [:h3 "Page Not Found"]
        [:p.error-message (str "There's no page at the address you requested. "
                               "If you entered it by hand, check for typos. "
                               "If you followed a link or a bookmark, it may "
                               "need to be updated.")]])
      (status 404)))

(defn error-page
  [request]
  (-> (wrap-header
       request
       [:div#error-page
        [:h3 "Error"]
        [:p.error-message (str "Something went wrong. Try again, and if the "
                               "problem persists, please contact support.")]])
      (status 500)))
