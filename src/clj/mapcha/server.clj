(ns mapcha.server
  (:require [ring.adapter.jetty             :refer [run-jetty]]
            [ring.middleware.resource       :refer [wrap-resource]]
            [ring.middleware.file-info      :refer [wrap-file-info]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.nested-params  :refer [wrap-nested-params]]
            [ring.middleware.params         :refer [wrap-params]]
            [ring.middleware.flash          :refer [wrap-flash]]
            [ring.middleware.session        :refer [wrap-session]]
            [ring.middleware.session.cookie :refer [cookie-store]]
            [ring.middleware.lint           :refer [wrap-lint]]
            [ring.middleware.stacktrace     :refer [wrap-stacktrace]]
            [ring.util.response             :refer [redirect]]
            [shoreleave.middleware.rpc      :refer [wrap-rpc]]
            [cemerick.friend                :refer [authenticate
                                                    logout
                                                    wrap-authorize]]
            [cemerick.friend.credentials    :refer [bcrypt-credential-fn]]
            [net.cgrand.moustache           :refer [app]]
            [mapcha.validation              :refer [validate-params]]
            [mapcha.db                      :refer [find-user-info add-user]]
            [mapcha.views                   :as    views]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;
;;; URI routing
;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; Moustache examples
;;
;; (def multiple-hi
;;   (app [& names] {:get ["bonjour " (apply str (interpose ", " names)) "!"]}))
;;
;; (defn is-integer? [string]
;;   (try (Integer/parseInt string) (catch Exception e)))
;;
;; (def route-handler-example
;;   (app
;;    ["hi" [name #"fred|lucy|ethel"]] {:get ["hello " name "!"]}
;;    ["hi" _] {:get "I don't talk to strangers"}
;;    ["hi" &] multiple-hi
;;    ["countdown" [n is-integer?]] {:get ["counting down: "
;;                                         (->> (range n 0 -1)
;;                                              (interpose " ")
;;                                              (apply str))

(def route-handler
  (app
   ;; Public pages (no authentication required)
   []                 {:get views/home-page}
   ["about"]          {:get views/about-page}
   ;; Login/Registration/Password-Reset/Logout pages
   ["login"]          {:any views/login-page}
   ["register"]       {:any views/register-page}
   ["password"]       {:any views/password-page}
   ["password-reset"] {:any views/password-reset-page}
   ["logout"]         (app logout :any (fn [_] (redirect "/")))
   ;; Private pages (must be logged in to access)
   ["account"]        (app (wrap-authorize #{:user :admin})
                           :get views/account-page
                           :post views/update-account-page)
   ["dashboard"]      (app (wrap-authorize #{:user :admin})
                           :get views/dashboard-page)
   ["admin"]          (app (wrap-authorize #{:admin})
                           :get views/admin-page
                           :post views/create-new-project-page)
   ;; Page not found (matches everything else)
   [&]                views/page-not-found-page))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;
;;; Friend authentication functions
;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; Pseudo-code description of Friend's `authenticate' function:
;;
;; Add :cemerick.friend/auth-config -> config-map to request-map
;; Returns the first non-nil workflow result
;; If all workflow results are nil,
;;   If (not allow-anon?)
;;     Redirect to login-uri and preserve session info
;;       (also add :cemerick.friend/unauthorized-uri request-uri to :session map)
;;   Otherwise
;;     (handler request)
;;   If an error occurs,
;;     Redirect to login-uri and preserve session info
;;       (also add :cemerick.friend/unauthorized-uri request-uri to :session map)
;; If (= (type result) :cemerick.friend/auth),
;;   Add authentication map to request map below
;;     [:session :cemerick.friend/identity :authentications]
;;     (retrievable with (identity request))
;;   Either redirect to one of these uris (if they exist)
;;     1. :cemerick.friend/unauthorized-uri field on the :session map
;;     2. :cemerick.friend/redirect-on-auth? field from workflow-result metadata
;;     3. :default-landing-uri on the :cemerick.friend/auth-config map
;;     And preserve session map (except :cemerick.friend/unauthorized-uri field)
;;   Or run the original handler
;;   If both return nil, return nil.
;;   If either returns a real value, make sure the session identity information
;;     is available on the response map
;;   If an error occurs,
;;     (->> error-map
;;          (assoc request :cemerick.friend/authorization-failure)
;;          (unauthorized-handler))
;; If (not= (type result) :cemerick.friend/auth),
;;   return result map (should be a ring response)
;;
;; Input parameters to `authenticate':
;; 
;; - :allow-anon? (default = true)
;;     if true, continue with ring-app even if all workflows return
;;     nil. if false (or an error occurs with the ring-app), redirect
;;     to login-uri and preserve session info (also add
;;     :cemerick.friend/unauthorized-uri request-uri to :session map)
;;
;; - :default-landing-uri (default = "/")
;;     where you are redirected in case of a successful authentication
;;     but no unauthorized-uri field on the request map
;;
;; - :login-uri (default = "/login")
;;     where to redirect in case of authentication failure
;;
;; - :credential-fn (default = (constantly nil))
;;     should return a map of type :cemerick.friend/auth or nil
;;
;; - :workflows (default = [])
;;     should return a map of type :cemerick.friend/auth, a ring
;;     response map, or nil
;;
;; - :unathorized-handler (default = #'default-unauthorized-handler)
;;     ring handler that returns a response map if an authentication
;;     error occurs in the wrapped route handler

(defn login-workflow
  [{params :params}]
  (if (:login params)
    (let [auth-inputs {:username (:email params)
                       :password (:password params)}]
      (if-let [auth-outputs (bcrypt-credential-fn find-user-info auth-inputs)]
        (with-meta auth-outputs {:type :cemerick.friend/auth})
        (assoc (redirect "/login")
               :flash "Invalid email/password combination.")))))

(defn register-workflow
  [{params :params}]
  (if (:register params)
    (if-let [error-message (validate-params params)]
      (assoc (redirect "/register") :flash error-message)
      (let [email    (:email params)
            password (:password params)]
        (if-let [user-info (add-user email password :user)]
          (with-meta (dissoc user-info :password) {:type :cemerick.friend/auth})
          (assoc (redirect "/register")
                 :flash (str "Account " email " already exists.")))))))

(defn wrap-authenticate
  [handler]
  (authenticate handler
                {:workflows           [login-workflow register-workflow]
                 :credential-fn       (partial bcrypt-credential-fn
                                               find-user-info) ;; not used
                 :allow-anon?         true
                 :login-uri           "/login"
                 :default-landing-uri "/"
                 :unauthorized-handler views/access-denied-page}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;
;;; Ring middlewares
;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn safe-log [params]
  (dissoc params :password :password-confirmation :current-password))

(defn log [msg & vals]
  (let [line (apply format msg vals)]
    (locking System/out (println line))))

(defn wrap-request-logging [handler]
  (fn [{:keys [request-method uri params flash] :as req}]
    (let [start  (System/currentTimeMillis)
          resp   (handler req)
          finish (System/currentTimeMillis)
          total  (- finish start)]
      (log "request %s %s (%dms) %s %s"
           request-method uri total (safe-log params) flash)
      resp)))

(defn wrap-error-page
  [handler]
  (fn [req]
    (try (handler req)
         (catch Exception e
           (views/error-page req)))))

(defn wrap-if [handler test middleware]
  (if test
    (middleware handler)
    handler))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;
;;; Ring handler
;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defonce run-mode (atom :development))
(defn set-development-mode! [] (reset! run-mode :development))
(defn set-production-mode!  [] (reset! run-mode :production))

(def secure-app
  (-> route-handler
      (wrap-rpc)
      (wrap-resource "public")
      (wrap-file-info)
      (wrap-authenticate)
      (wrap-request-logging)
      (wrap-keyword-params)
      (wrap-nested-params)
      (wrap-params)
      (wrap-flash)
      (wrap-session {:store (cookie-store {:key "1291427194355772"})
                     :cookie-attrs {:max-age 10800}})
      (wrap-if (= @run-mode :production)  wrap-error-page)
      (wrap-if (= @run-mode :development) wrap-lint)
      (wrap-if (= @run-mode :development) wrap-stacktrace)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;
;;; The -main function starts an embedded Jetty webserver and waits
;;; for connections on port 8080. The webserver object is stored in
;;; the var `server'.
;;;
;;; For interactive development:
;;; (.stop server)
;;; (.start server)
;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn -main
  []
  (def server
    (run-jetty #'secure-app
               {:port  8080
                :join? false})))
