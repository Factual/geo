(defproject
  factual/geo "2.1.0-rc-0"
  :url     "https://github.com/factual/geo"
  :license {:name "Eclipse Public License - v 1.0"
            :url  "http://www.eclipse.org/legal/epl-v10.html"
            :distribution :repo
            :comments "same as Clojure"}
  :description "Geospatial operations over points, lines, polys, geohashes, etc."
  :dependencies
  [[org.clojure/math.numeric-tower "0.0.4"]
   [ch.hsr/geohash "1.3.0"]
   [com.uber/h3 "3.0.3"]
   [org.locationtech.geotrellis/geotrellis-proj4_2.11 "1.2.1"]
   [org.locationtech.spatial4j/spatial4j "0.7"]
   [org.locationtech.jts/jts-core "1.15.1"]
   [org.locationtech.jts.io/jts-io-common "1.15.1"]
   [org.noggit/noggit "0.8"]
   [org.wololo/jts2geojson "0.12.0"]]
  :global-vars {*warn-on-reflection* true}
  :codox {:themes [:rdash]}
  :profiles {:dev {:plugins [[lein-midje "3.2.1"]
                             [lein-codox "0.10.4"]]
                   :dependencies [[org.clojure/clojure "1.9.0"]
                                  [codox-theme-rdash "0.1.2"]
                                  [criterium "0.4.4"]
                                  [cheshire "5.8.0"]
                                  [midje "1.9.2"]]}
             :1.8 {:dependencies [[org.clojure/clojure "1.8.0"]]}
             :1.7 {:dependencies [[org.clojure/clojure "1.7.0"]]}})
