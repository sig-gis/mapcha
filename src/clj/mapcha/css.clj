(ns mapcha.css
  (:require [garden.core :refer [css]]))

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

;;========================
;; HTTP Response Creation
;;========================

(defn wrap-http-response [body]
  {:status  200
   :headers {"Content-Type" "text/css"}
   :body    body})

(defn make-stylesheet [request]
  (wrap-http-response
   (css universal-styles
        home-styles
        about-styles)))
