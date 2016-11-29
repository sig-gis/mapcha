(ns mapcha.css
  (:require [clojure.string :as str]
            [garden.core :refer [css]]))

;;==================
;; Global Constants
;;==================

(def ms-gradient-filter (str "progid:DXImageTransform.Microsoft.gradient("
                             "startColorstr=#CCFFFFFF,endColorstr=#CCFFFFFF)"))

;;===================
;; Utility Functions
;;===================

(defn vendor-map [attr val]
  (into {(keyword attr) val}
        (map (fn [prefix]
               [(keyword (str prefix "-" (name attr))) val]))
        ["-webkit" "-moz" "-o"]))

;;=============
;; Page Styles
;;=============

(def universal-styles
  [[:body
    {:font {:family "Open Sans" :size "16px"}
     :height "100vh"
     :background-image "url(../img/mountain_field_scenery_small.jpg)"}
    (vendor-map :background-size "cover")]

   [:header :nav :section
    {:display "block"}]

   [:h1 :h2 :h3 :h4 :h5 :h6 :h7 :p :label
    {:font {:family "Open Sans" :weight "400"}}]

   [:.button
    {:line-height "100%"
     :cursor "pointer"
     :background-color "#3FABC6"
     :color "white"
     :font-family "Oswald"
     :text {:overflow "ellipsis" :transform "uppercase"}}
    (vendor-map :border-radius "0px")]

   [:header
    {:position "fixed"
     :top "0rem"
     :left "0rem"
     :height "6rem"
     :width "100%"
     :color "#eee"
     :background-color "#3a3a3a"}

    [:a
     {:color "#eee"}

     [:&:link :&:visited :&:active
      {:text-decoration "none"}]

     [:&:hover :&.active-link
      {:text-shadow "0px 0px 4px #fff"}]]]

   [:#logos
    {:position "relative"
     :margin "0px"
     :width "100%"
     :height "55px"
     :background-color "#fff"}

    [:img#usaid
     {:position "absolute"
      :top "5px"
      :left "30px"
      :width "150px"}]

    [:img#nasa
     {:position "absolute"
      :top "0px"
      :left "210px"
      :width "60px"}]

    [:img#adpc
     {:position "absolute"
      :top "9px"
      :left "300px"
      :width "80px"}]

    [:img#servir
     {:position "absolute"
      :top "12px"
      :right "30px"
      :width "250px"}]]

   [:nav
    {:position "absolute"
     :top "64px"
     :left "30px"}

    [:ul
     [:li
      {:display "inline"
       :list-style-type "none"
       :padding "0.4rem"}]]]

   [:#login-info
    {:position "absolute"
     :top "66px"
     :right "30px"}

    [:p
     {:color "#eee"
      :font-size "0.8rem"}

     [:a
      {:margin-left "0.2rem"
       :padding "0.4rem"
       :border "1px solid #999"}]]]

   [:#content
    {:position "fixed"
     :top "6rem"
     :left "0rem"
     :height "calc(100vh - 6rem)"
     :width "100%"
     :background-color "rgba(255,255,255,0.2)"}

    [:a
     {:color "#444"}

     [:&:link :&:visited :&:active
      {:text-decoration "none"}]

     [:&:hover :&.active-link
      {:text-shadow "0px 0px 4px #333"}]]]])

(def home-styles
  [:#home
   {:position "absolute"
    :right "5rem"
    :height "100%"
    :width "350px"
    :padding "2rem"
    :background "rgba(255,255,255,0.8)"
    :filter ms-gradient-filter
    :-ms-filter ms-gradient-filter}

   [:h1
    {:margin-top "20px"
     :padding "0.5rem 0.5rem 0.5rem 1rem"
     :font {:size "3.5rem" :weight "bold"}
     :color "gray"
     :line-height "80%"}]

   [:h2
    {:margin-bottom "10px"
     :padding "0.5rem 0.5rem 0.5rem 1rem"
     :font {:size "2.5rem" :weight "400"}
     :color "gray"
     :line-height "110%"}]

   [:h3
    {:padding "0.5rem 0.5rem 0.75rem 1rem"
     :color "#3C948B"
     :font {:family "Open Sans" :weight "400" :size "1.1rem" :style "normal"}}]

   [:p
    {:padding "0.5rem"}]])

(def about-styles
  [:#about
   {:position "absolute"
    :right "5rem"
    :height "100%"
    :width "350px"
    :padding "2rem"
    :background "rgba(255,255,255,0.8)"
    :filter ms-gradient-filter
    :-ms-filter ms-gradient-filter}

   [:h1
    {:font {:size "1.9rem" :weight "100"}
     :padding "0.5rem"
     :color "gray"}]

   [:img
    {:padding "0rem 0.5rem"}]

   [:p
    {:padding "0.5rem"}]])

(def login-register-password-account-styles
  [:#login-form :#register-form :#password-form :#account-form
   {:position "absolute"
    :right "5rem"
    :height "100%"
    :width "350px"
    :padding "2rem"
    :background "rgba(255,255,255,0.8)"
    :filter ms-gradient-filter
    :-ms-filter ms-gradient-filter}

   [:h1
    {:font {:size "1.9rem" :weight "100"}
     :padding "0.5rem"
     :color "gray"}]

   [:h2
    {:font {:size "1rem" :weight "400"}
     :padding "0.1rem"
     :color "darkgray"}]

   [:input#email :input#password :input#password-confirmation
    :input#password-reset-key :input#current-password
    {:height "2rem"
     :width "20rem"
     :border "1px solid #ececec"
     :margin "0.3rem"
     :padding "0px 5px"}

    [:&:focus
     {:border "1px solid darkgray"
      :cursor "grab"}]

    [:&:hover
     {:border "1px solid gray"
      :cursor "pointer"}]]

   [:p#forgot-password
    [:a
     {:display "block"
      :font {:size "0.9rem" :weight "700"}
      :margin "0.5rem 0.5rem 0rem 1rem"}]]

   [:input.button
    {:height "2rem"
     :width "21rem"
     :margin "0.6rem 0.3rem"
     :padding "0.5rem"
     :border "1px solid #ececec"}

    [:&:focus
     {:border "1px solid #999"}]]

   [:hr
    {:height "12px"
     :border "0"
     :box-shadow "inset 0 12px 12px -12px rgba(0, 0, 0, 0.5)"}]])

(def select-project-styles
  [:#select-project-form
   {:position "absolute"
    :right "5rem"
    :height "100%"
    :width "350px"
    :padding "2rem"
    :background "rgba(255,255,255,0.8)"
    :filter ms-gradient-filter
    :-ms-filter ms-gradient-filter}

   [:h1
    {:font {:size "1.9rem" :weight "100"}
     :padding "0.5rem"
     :color "gray"}]

   [:ul
    {:list-style-type "none"
     :padding "0.5rem"
     :height "80%"
     :overflow "scroll"}

    [:li
     [:a
      {:display "block"
       :width "95%"
       :border "1px solid blue"
       :margin-bottom "0.5rem"
       :padding "0.25rem"
       :text-align "center"
       :font-size "1rem"
       :color "#3C948B"
       :text-decoration "none"}]]]])

(def dashboard-styles
  [:#dashboard
   {:height "100%"
    :padding "0rem"
    :background "rgba(255,255,255,0.8)"}

   [:#quit-button
    {:display "inline-block"
     :margin-top "0rem"
     :width "5rem"
     :position "absolute"
     :left "calc(70vw - 5rem)"
     :z-index "500"
     :background-color "#3FABC6"
     :height "40px"
     :font-size "17"
     :border "none"
     :text-align "center"}]

   [:#image-analysis-pane
    {:float "left"
     :height "100%"
     :width "70%"
     :background-color "darkgray"}]

   [:.ol-dragbox
    {:background-color "rgba(255,255,255,0.4)"
     :border-color "rgba(100,150,0,1)"}]

   [:.ol-rotate
    {:top "4rem"}]

   [:#sidebar
    {:float "right"
     :height "100%"
     :width "30%"
     :padding "0rem"
     :overflow "scroll"}

    [:#sidebar-contents
     {:height "100%"}

     [:fieldset
      {:margin-top "0px"}

      [:legend
       {:margin-top "-5px"
        :padding "10px 0px"
        :color "#3FABC6"
        :font {:size "1.2rem" :family "Oswald" :weight "400"}
        :text {:transform "uppercase" :indent "10px"}}]

      [:select#project-id
       {:font {:family "Verdana,Geneva,sans-serif" :weight "200" :size "0.9rem"}
        :line-height "90%"
        :padding "0rem"
        :height "1.8rem"
        :width "100%"
        :border "none"
        :outline "none"
        :color "#3D3B39"
        :text-index "5px"}]

      [:option
       {:color "#3D3B39"
        :text-indent "5px"}]

      [:input#new-plot-button
       {:margin "10px 5px 5px 5px"
        :padding "8px 0px"
        :font-size "1.2rem"
        :text-align "left"}]

      [:ul
       [:li
        {:float "left"
         :width "50%"}

        [:input
         {:height "40px"
          :width "100%"
          :font {:family "Verdana,Geneva,sans-serif" :size "0.8rem"}
          :text {:transform "uppercase" :align "left" :indent "10px"}
          :cursor "pointer"}]

        [:#final-plot-options
         {:clear "left"
          :float "left"
          :margin-left "5px"}

         [:td
          {:padding "5px"
           :color "#3FABC6"
           :font {:size "1.2rem" :family "Oswald" :weight "400"}
           :text {:transform "uppercase" :align "center"}}

          [:input#save-values-button :input#flag-plot-button
           {:padding "8px 0px"
            :font-size "1.2rem"}]]]]]]]]
   [:#imagery-info
    {:clear "both"
     :position "relative"
     :top "-1.5rem"
     :left "18%"
     :width "40%"
     :text-align "center"
     :font-size "0.8rem"
     :color "white"
     :padding "0.25rem"
     :background-color "rgba(75,75,150,0.6)"}]])

(def admin-styles
  [:#admin
   {:height "100%"
    :width "100%"
    :padding "10px 20px"
    :background "rgba(255,255,255,0.9)"
    :overflow "scroll"}

   [:h1
    {:font {:size "1.8rem" :weight "bold"}
     :margin-bottom "10px"}]

   [:#project-selection
    {:margin-bottom "10px"}

    [:label
     {:font {:size "1rem" :family "Oswald" :weight "400"}
      :color "#3fabc6"
      :text-transform "uppercase"
      :display "block"
      :margin-bottom "5px"}]

    [:select
     {:border "1px solid darkgray"
      :height "2rem"
      :font-size "0.9rem"}]]

   [:input#download-plot-data
    {:position "absolute"
     :top "10px"
     :right "15rem"
     :font-size "1rem"
     :padding "0.5rem"
     :color "white"
     :box-shadow "0.25rem 0.25rem 0.5rem rgba(21,21,153,0.6)"}]

   [:input#create-project
    {:position "absolute"
     :top "10px"
     :right "20px"
     :font-size "1rem"
     :padding "0.5rem"
     :color "white"
     :box-shadow "0.25rem 0.25rem 0.5rem rgba(21,21,153,0.6)"}]

   [:fieldset
    {:margin-right "1.5rem"}]

   [:legend
    {:margin-top "1rem"
     :font {:size "1rem" :family "Oswald" :weight "400"}
     :color "#3fabc6"
     :text-transform "uppercase"}]

   [:#project-info :#plot-info :#sample-info :#bounding-box
    [:label
     {:display "block"
      :font-size "0.8rem"
      :padding "0.25rem 0rem"}]]

   [:input#project-name :input#plots :input#radius
    :input#samples-per-plot :input#sample-resolution
    :input#lat-max :input#lon-min :input#lon-max :input#lat-min
    {:height "1rem"
     :padding "0.3rem"
     :margin-bottom "5px"}]

   [:fieldset#project-info
    {:float "left"
     :width "18%"}

    [:input#project-name
     {:width "90%"}]

    [:textarea#project-description
     {:height "6rem"
      :width "90%"
      :padding "0rem 0.7rem"
      :border-radius "0.25rem"
      :border "1px solid darkgray"}]]

   [:fieldset#plot-info
    {:float "left"
     :width "10%"}

    [:input#plots :input#radius
     {:width "80%"}]]

   [:fieldset#sample-info
    {:float "left"
     :width "12%"}

    [:input#random-sample-type :input#gridded-sample-type
     {:vertical-align "middle"}]

    [:input#samples-per-plot :input#sample-resolution
     {:width "80%"}]

    [:table
     {:margin-left "1rem"
      :background-color "white"
      :border "1px solid #aaa"}

     [:td
      {:padding "0px 5px"}]]]

   [:fieldset#bounding-box
    {:float "left"
     :width "15rem"}

    [:label
     {:text-align "center"
      :margin-bottom "5px"}]

    [:input
     {:display "block"
      :width "5rem"}

     [:&#lat-max
      {:margin "0rem auto 0.25rem auto"}]

     [:&#lon-min
      {:margin "0.25rem 0rem"
       :float "left"}]

     [:&#lon-max
      {:margin "0.25rem 0rem"
       :float "right"}]

     [:&#lat-min
      {:clear "both"
       :margin "0.25rem auto"}]]]

   [:#map-and-imagery
    {:position "absolute"
     :top "5rem"
     :right "20px"
     :width "calc(100% - 18% - 10% - 12% - 15rem - 4.5rem - 50px)"
     :height "calc(60vh + 2rem + 5px)"}

    [:#new-project-map
     {:height "60vh"
      :margin-bottom "5px"
      :border "1px solid black"}]

    [:label
     {:font {:size "1rem" :family "Oswald" :weight "400"}
      :color "#3fabc6"
      :text-transform "uppercase"
      :display "inline"}]

    [:select
     {:border "1px solid darkgray"
      :height "2rem"
      :font-size "0.9rem"}]]

   [:fieldset#sample-value-info
    {:float "left"
     :clear "both"
     :margin-bottom "20px"}

    [:th :td
     {:text-align "center"
      :font-size "0.8rem"
      :padding "0.25rem"}]

    [:input.button
     {:padding "0.25rem"
      :min-width "2rem"
      :color "white"}]

    [:.circle
     {:width "1rem"
      :height "1rem"
      :border-radius "0.5rem"
      :margin "0rem auto"}]

    [:input#value-name :input#value-color :input#value-image
     {:font-size "0.9rem"
      :height "2rem"
      :padding "0rem"
      :margin-bottom "5px"}]

    [:input#add-sample-value
     {:margin "0.5rem 3rem"
      :box-shadow "0.25rem 0.25rem 0.5rem rgba(21,21,153,0.6)"}]]

   [:#spinner
    {:position "absolute"
     :top "1rem"
     :left "45%"
     :width "4rem"
     :height "4rem"
     :border {:radius "2rem" :bottom "0.5rem solid #3fabc6"}
     :visibility "hidden"}
    (vendor-map :animation "sweep 1s infinite linear")]])

(def error-page-styles
  [[:.alert
     {:position "absolute"
      :top "1rem"
      :left "1rem"
      :font {:size "1rem" :style "italic"}
      :width "35rem"
      :padding "1rem"
      :border "1px solid #009"
      :background "white"
      :z-index "1000"
      :opacity "0"}
    (vendor-map :animation "fade-out 5s")]

   [:p.error-message
    {:margin-top "1rem"
     :font {:size "1rem" :style "normal"}
     :color "#555"}]

   [:#access-denied :#page-not-found :#error-page
    {:position "absolute"
     :right "5rem"
     :height "100%"
     :width "350px"
     :padding "2rem"
     :background "rgba(255,255,255,0.8)"
     :filter ms-gradient-filter
     :-ms-filter ms-gradient-filter}

    [:h3
     {:font-size "1.5rem"
      :font-weight "100"
      :color "gray"}]]])

(def animation-styles
  "@-webkit-keyframes sweep { to { -webkit-transform:rotate(360deg); } }

@-moz-keyframes    sweep { to { -moz-transform:rotate(360deg); } }

@-o-keyframes      sweep { to { -o-transform:rotate(360deg); } }

@keyframes         sweep { to { transform:rotate(360deg); } }

@-webkit-keyframes fade-out {
    0%   { opacity: 0; }
    50%  { opacity: 1; }
    100% { opacity: 0; }
}

@-moz-keyframes fade-out {
    0%   { opacity: 0; }
    50%  { opacity: 1; }
    100% { opacity: 0; }
}

@-o-keyframes fade-out {
    0%   { opacity: 0; }
    50%  { opacity: 1; }
    100% { opacity: 0; }
}

@keyframes fade-out {
    0%   { opacity: 0; }
    50%  { opacity: 1; }
    100% { opacity: 0; }
}")

;;========================
;; HTTP Response Creation
;;========================

(defn wrap-http-response [& body]
  {:status  200
   :headers {"Content-Type" "text/css"}
   :body    (str/join "\n\n" body)})

(defn make-stylesheet [request]
  (wrap-http-response
   (css universal-styles
        home-styles
        about-styles
        login-register-password-account-styles
        select-project-styles
        dashboard-styles
        admin-styles
        error-page-styles)
   animation-styles))
