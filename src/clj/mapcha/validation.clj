(ns mapcha.validation)

(defn is-email?
  "Returns true if s is an email address"
  [email-string]
  (let [pattern (re-pattern
                 (str "(?i)[a-z0-9!#$%&'*+/=?^_`{|}~-]+"
                      "(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*"
                      "@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+"
                      "[a-z0-9](?:[a-z0-9-]*[a-z0-9])?"))]
    (re-matches pattern email-string)))

(defn validate-params
  [{:keys [email password password-confirmation]}]
  (seq
   (remove nil?
           [(if-not (is-email? email)
              (str email " is not a valid email address."))
            (if (< (count password) 8)
              "Password must be at least 8 characters.")
            (if-not (= password password-confirmation)
              "Password and Password confirmation do not match.")])))
