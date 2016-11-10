(task-options!
 pom  {:project     'sig-gis/mapcha
       :version     "1.0.0-SNAPSHOT"
       :description "SIG's Image Analysis Crowdsourcing Platform"
       :url         "http://mapcha.sig-gis.com"}
 repl {:eval        '(set! *warn-on-reflection* true)})

(set-env!
 :source-paths   #{"src/clj" "src/cljs" "test/clj" "test/cljs"}
 :resource-paths #{"resources"}
 :dependencies   '[[org.clojure/clojure         "1.8.0"]
                   [org.clojure/clojurescript   "1.8.51"]
                   [ring                        "1.4.0"]
                   [com.cemerick/friend         "0.2.1"
                    :exclusions [commons-codec]]
                   [net.cgrand/moustache        "1.2.0-alpha2"
                    :exclusions [org.clojure/clojure ring/ring-core]]
                   [hiccup                      "1.0.5"]
                   [org.clojure/java.jdbc       "0.5.0"]
                   [org.postgresql/postgresql   "9.4.1208.jre7"]
                   [yesql                       "0.5.2"]
                   [com.draines/postal          "1.11.4"]
                   [org.clojure/data.csv        "0.1.3"]
                   [org.clojars.magomimmo/shoreleave-remote-ring "0.3.2"]
                   [org.clojars.magomimmo/shoreleave-remote      "0.3.1"]
                   [javax.servlet/servlet-api   "2.5"]
                   [reagent                     "0.6.0"]
                   [cljsjs/openlayers           "3.15.1"]
                   [adzerk/boot-reload          "0.4.13"    :scope "test"]
                   [adzerk/boot-cljs            "1.7.228-1" :scope "test"]
                   [adzerk/boot-cljs-repl       "0.3.0"     :scope "test"]
                   [pandeiro/boot-http          "0.7.3"     :scope "test"]
                   [crisptrutski/boot-cljs-test "0.2.1"     :scope "test"]
                   [com.cemerick/piggieback     "0.2.1"     :scope "test"]
                   [weasel                      "0.7.0"     :scope "test"]
                   [org.clojure/tools.nrepl     "0.2.12"    :scope "test"]])

(require
 '[adzerk.boot-reload          :refer [reload]]
 '[adzerk.boot-cljs            :refer [cljs]]
 '[adzerk.boot-cljs-repl       :refer [cljs-repl start-repl]]
 '[pandeiro.boot-http          :refer [serve]]
 '[crisptrutski.boot-cljs-test :refer [test-cljs]])

(deftask auto-test []
  (comp (watch)
        (speak)
        (test-cljs)))

(deftask dev []
  (comp (serve :port    8080
               :init    'mapcha.server/set-development-mode!
               :handler 'mapcha.server/secure-app
               :reload  true)
        (watch)
        (cljs-repl)
        (reload)
        (cljs :optimizations :none
              :source-map    true)
        (target :dir #{"target/"})))

(deftask site []
  (comp (serve :httpkit true
               :port    8080
               :init    'mapcha.server/set-production-mode!
               :handler 'mapcha.server/secure-app)
        (cljs :optimizations :advanced
              :source-map    true)
        (target :dir #{"target/"})
        (wait)))
