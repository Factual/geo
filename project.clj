(defproject
  factual/geo "3.0.1"
  :url     "https://github.com/factual/geo"
  :license {:name "Eclipse Public License - v 1.0"
            :url  "http://www.eclipse.org/legal/epl-v10.html"
            :distribution :repo
            :comments "same as Clojure"}
  :description "Geospatial operations over points, lines, polys, geohashes, etc."
  :dependencies
  [[org.clojure/math.numeric-tower "0.0.5"]
   [ch.hsr/geohash "1.4.0"]
   [com.uber/h3 "3.7.1"]
   [org.locationtech.proj4j/proj4j "1.1.4"]
   [org.locationtech.spatial4j/spatial4j "0.8"]
   [org.locationtech.jts/jts-core "1.18.2"]
   [org.locationtech.jts.io/jts-io-common "1.18.2"]
   [org.wololo/jts2geojson "0.16.1"]]
  :codox {:themes [:rdash]}
  :profiles {:dev {:global-vars {*warn-on-reflection* true}
                   :plugins [[lein-midje "3.2.2"]
                             [lein-codox "0.10.7"]
                             [lein-project-version "0.1.0"]]
                   :dependencies [[org.clojure/clojure "1.10.3"]
                                  [codox-theme-rdash "0.1.2"]
                                  [criterium "0.4.6"]
                                  [cheshire "5.10.1"]
                                  [midje "1.10.5"]]}
             :1.9 {:dependencies [[org.clojure/clojure "1.9.0"]]}
             :1.8 {:dependencies [[org.clojure/clojure "1.8.0"]]}
             :1.7 {:dependencies [[org.clojure/clojure "1.7.0"]]}}
  :repositories [["snapshots" {:url "https://clojars.org/repo"
                               :username :env/clojars_username
                               :password :env/clojars_password
                               :sign-releases false}]
                 ["releases"  {:url "https://clojars.org/repo"
                               :username :env/clojars_username
                               :password :env/clojars_password
                               :sign-releases false}]]
  :deploy-repositories [["clojars" {:url "https://clojars.org/repo"
                                    :username :env/clojars_username
                                    :password :env/clojars_password
                                    :sign-releases false}]
                        ["snapshots" {:url "https://clojars.org/repo"
                                      :username :env/clojars_username
                                      :password :env/clojars_password
                                      :sign-releases false}]
                        ["releases"  {:url "https://clojars.org/repo"
                                      :username :env/clojars_username
                                      :password :env/clojars_password
                                      :sign-releases false}]])
