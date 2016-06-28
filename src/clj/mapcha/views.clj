(ns mapcha.views
  (:require [ring.util.response          :refer [status]]
            [cemerick.friend             :as    friend]
            [cemerick.friend.credentials :refer [bcrypt-credential-fn]]
            [hiccup.page                 :refer [html5 include-css include-js]]
            [hiccup.element              :refer [javascript-tag link-to]]
            [mapcha.validation           :refer [is-email? validate-params]]
            [mapcha.db                   :refer [find-user-info
                                                 set-user-email
                                                 set-user-password
                                                 add-password-reset-key
                                                 remove-password-reset-key
                                                 send-password-reset-key
                                                 create-new-project]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;
;;; Friend helper functions
;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn logged-in?
  [request]
  (boolean (friend/identity request)))

(defn current-email
  [request]
  (:current (friend/identity request)))

(defn current-role
  [request]
  (when-let [{:keys [current authentications]} (friend/identity request)]
    (first (:roles (authentications current)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;
;;; Site-wide header and footer
;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn wrap-http-response
  [body]
  {:status  200
   :headers {"Content-Type" "text/html"}
   :body    body})

(defn wrap-header-footer
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
       (include-css "/css/stylesheet.css" "/css/openlayers_3.13.0.css")
       html5shiv]
      [:body
       [:header
        [:h1 (link-active "/" "Mapcha")]
        [:nav
         [:ul
          (case (current-role request)
            :admin (list
                    [:li (link-active "/account" "Account")]
                    [:li (link-active "/dashboard" "Dashboard")]
                    [:li (link-active "/admin" "Admin")]
                    [:li (link-active "/about" "About")]
                    [:li (link-active "/contact" "Contact")])
            :user  (list
                    [:li (link-active "/account" "Account")]
                    [:li (link-active "/dashboard" "Dashboard")]
                    [:li (link-active "/about" "About")]
                    [:li (link-active "/contact" "Contact")])
            (list
             [:li (link-active "/about" "About")]
             [:li (link-active "/contact" "Contact")]))]]
        [:div#login-info
         [:p
          (if (logged-in? request)
            (list
             (str "Logged in as " (current-email request) " ")
             (link-active "/logout" "Logout"))
            (list
             (link-active "/login" "Login")
             (link-active "/register" "Register")))]]]
       [:section#content
        (when-let [message (:flash request)]
          (if (string? message)
            [:p {:class "alert"} message]
            (for [sub-message message]
              [:p {:class "alert"} sub-message])))
        content]
       [:footer
        [:p (link-to "http://www.sig-gis.com" "&copy; SIG-GIS 2016")]]]))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;
;;; Home page
;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn home-page
  [request]
  (wrap-header-footer
   request
   [:section#home-top
    [:div#left-block
     [:h1 "Mapcha: " [:em "Earth Image Identification"]]
     [:h2 "Collaborate. Play. Map the world."]
     [:input.button {:type "button" :value "Learn more »"
                     :onclick "window.location='/learn'"}]]
    [:div#right-block
     [:div#login
      [:form {:method "post" :action "/login"}
       [:input {:type "email" :name "email" :value "" :placeholder "Email"}]
       [:input {:type "password" :name "password" :value ""
                :placeholder "Password"}]
       [:p#forgot-password (link-to "/password" "Forgot your password?")]
       [:input.button {:type "submit" :name "login" :value "Login"}]]]
     [:div#register
      [:form {:method "post" :action "/register"}
       [:h3 "New to Mapcha?"]
       [:input {:type "email" :name "email" :value ""
                :placeholder "Email" :autocomplete "off"}]
       [:input {:type "password" :name "password" :value ""
                :placeholder "Password" :autocomplete "off"}]
       [:input {:type "password" :name "password-confirmation" :value ""
                :placeholder "Password confirmation" :autocomplete "off"}]
       [:input.button {:type "submit" :name "register" :value "Register"}]]]]]
   [:section#home-content
    [:p#overview-map-wait-message "Overview map is loading. Please wait..."]
    [:div#overview-map
     [:div#popup.ol-popup
      [:div#popup-content]]]
    [:p "Enter a description of " [:span.trademark "Mapcha"] " here..."]]
   (include-js "/mapcha.js")
   (javascript-tag "mapcha.overview_plot.main()")))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;
;;; About page
;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn about-page
  [request]
  (wrap-header-footer
   request
   [:h3 "About page...in progress"]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;
;;; Contact page
;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn contact-page
  [request]
  (wrap-header-footer
   request
   [:h3 "Contact page...in progress"]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;
;;; Learn page
;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn learn-page
  [request]
  (wrap-header-footer
   request
   [:h3 "Learn page...in progress"]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;
;;; Login page
;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn login-page
  [request]
  (wrap-header-footer
   request
   [:div#login-form
    [:h1 "Sign into your account"]
    [:form {:method "post" :action "/login"}
     [:input {:type "email" :name "email" :value "" :placeholder "Email"}]
     [:input {:type "password" :name "password" :value "" :placeholder "Password"}]
     [:p#forgot-password (link-to "/password" "Forgot your password?")]
     [:input.button {:type "submit" :name "login" :value "Login"}]]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;
;;; Register page
;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn register-page
  [request]
  (wrap-header-footer
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
    (wrap-header-footer
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
      (wrap-header-footer
       (assoc request :flash flash-message)
       [:div#password-form
        [:h1 "Enter your email address"]
        [:form {:method "post" :action "/password"}
         [:input#email {:type "email" :name "email" :value ""
                        :placeholder "Email" :autocomplete "off"}]
         [:input.button {:type "submit" :value "Request Password Reset Key"}]]]))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;
;;; Account page
;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn account-page
  [request]
  (wrap-header-footer
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
  (wrap-header-footer
   request
   [:h3 "Dashboard page...in progress"]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;
;;; Admin page
;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn admin-page
  [request]
  (wrap-header-footer
   request
   [:div#admin
    [:h1 "Create New Project"]
    [:form {:method "post" :action "/admin"}
     [:h2 "Name"]
     [:input {:type "text" :name "project-name" :autocomplete "off"}]
     [:h2 "Description"]
     [:input {:type "text" :name "project-description" :autocomplete "off"}]
     [:h2 "Define bounding box"]
     [:input {:type "text" :name "boundary-lon-min"
              :placeholder "Longitude Min" :autocomplete "off"}]
     [:input {:type "text" :name "boundary-lon-max"
              :placeholder "Longitude Max" :autocomplete "off"}]
     [:input {:type "text" :name "boundary-lat-min"
              :placeholder "Latitude Min" :autocomplete "off"}]
     [:input {:type "text" :name "boundary-lat-max"
              :placeholder "Latitude Max" :autocomplete "off"}]
     [:h2 "Number of plots"]
     [:input {:type "text" :name "plots" :autocomplete "off"}]
     [:h2 "Plot radius in meters"]
     [:input {:type "text" :name "buffer-radius" :autocomplete "off"}]
     [:h2 "Number of samples per plot"]
     [:input {:type "text" :name "samples-per-plot" :autocomplete "off"}]
     [:h2 "Categories per sample"]
     [:input {:type "text" :name "sample-values"
              :placeholder "Forest, Field, Agriculture, Water"
              :autocomplete "off"}]
     [:br]
     [:input.button {:type "submit" :name "create-project"
                     :value "Create and launch this project"}]]]))

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
  (-> (wrap-header-footer
       request
       [:h3 "Access Denied"]
       [:p.error-message "Sorry, you are not authorized to access this resource."])
      (status 401)))

(defn page-not-found-page
  [request]
  (-> (wrap-header-footer
       request
       [:h3 "Page Not Found"]
       [:p.error-message (str "There's no page at the address you requested. "
                              "If you entered it by hand, check for typos. "
                              "If you followed a link or a bookmark, it may "
                              "need to be updated.")])
      (status 404)))

(defn error-page
  [request]
  (-> (wrap-header-footer
       request
       [:h3 "Error"]
       [:p.error-message (str "Something went wrong. Try again, and if the "
                              "problem persists, please contact support.")])
      (status 500)))
