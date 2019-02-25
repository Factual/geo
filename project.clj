(defproject
  factual/geo "3.0.0-rc-1"
  :url     "https://github.com/factual/geo"
  :license {:name "Eclipse Public License - v 1.0"
            :url  "http://www.eclipse.org/legal/epl-v10.html"
            :distribution :repo
            :comments "same as Clojure"}
  :description "Geospatial operations over points, lines, polys, geohashes, etc."
  :dependencies
  [[org.clojure/math.numeric-tower "0.0.4"]
   [ch.hsr/geohash "1.3.0"]
   [com.uber/h3 "3.4.0"]
   [org.locationtech.proj4j/proj4j "1.0.0"]
   [org.locationtech.spatial4j/spatial4j "0.7"]
   [org.locationtech.jts/jts-core "1.16.1"]
   [org.locationtech.jts.io/jts-io-common "1.16.1"]
   [org.noggit/noggit "0.8"]
   [org.wololo/jts2geojson "0.13.0"]]
  :codox {:themes [:rdash]}
  :profiles {:dev {:global-vars {*warn-on-reflection* true}
                   :plugins [[lein-midje "3.2.1"]
                             [lein-codox "0.10.6"]
                             [lein-project-version "0.1.0"]]
                   :dependencies [[org.clojure/clojure "1.10.0"]
                                  [codox-theme-rdash "0.1.2"]
                                  [criterium "0.4.4"]
                                  [cheshire "5.8.1"]
                                  [midje "1.9.6"]]}
             :1.9 {:dependencies [[org.clojure/clojure "1.9.0"]]}
             :1.8 {:dependencies [[org.clojure/clojure "1.8.0"]]}
             :1.7 {:dependencies [[org.clojure/clojure "1.7.0"]]}}
  :release-tasks [["vcs" "assert-committed"]
                  ["deploy"]
                  ["vcs" "tag" "v" "--no-sign"]
                  ["vcs" "push"]]
  :repositories [["snapshots" {:url "https://clojars.org"
                               :username :env/clojars_username
                               :password :env/clojars_password}]
                 ["releases"  {:url "https://clojars.org"
                               :username :env/clojars_username
                               :password :env/clojars_password}]]
  :deploy-repositories [["snapshots" {:url "https://clojars.org"
                                      :username :env/clojars_username
                                      :password :env/clojars_password}]
                        ["releases"  {:url "https://clojars.org"
                                      :username :env/clojars_username
                                      :password :env/clojars_password}]])
