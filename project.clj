(defproject
  factual/geo "1.1.0"
  :url     "https://github.com/factual/geo"
  :license {:name "Eclipse Public License - v 1.0"
            :url  "http://www.eclipse.org/legal/epl-v10.html"
            :distribution :repo
            :comments "same as Clojure"}
  :description "Geospatial operations over points, lines, polys, geohashes, etc.o"
  :dependencies
  [[org.clojure/math.numeric-tower "0.0.4"]
   [ch.hsr/geohash "1.3.0"]
   [org.locationtech.spatial4j/spatial4j "0.6"]
   [com.vividsolutions/jts "1.13"]
   [org.wololo/jts2geojson "0.10.0"]
   [org.noggit/noggit "0.8"]]
  :profiles {:dev {:plugins [[lein-midje "3.1.1"]]
                   :dependencies [[org.clojure/clojure "1.8.0"]
                                  [criterium "0.4.4"]
                                  [midje "1.6.3"]]}})
