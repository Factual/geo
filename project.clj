(defproject
  factual/geo "1.0.0"
  :url     "https://github.com/factual/geo"
  :license {:name "Eclipse Public License - v 1.0"
            :url  "http://www.eclipse.org/legal/epl-v10.html"
            :distribution :repo
            :comments "same as Clojure"}
  :description "Geospatial operations over points, lines, polys, geohashes, etc.o"
  :dependencies
  [[org.clojure/math.numeric-tower "0.0.4"]
   [la.tomoj/geohash-java "1.0.6"]
   [org.locationtech.spatial4j/spatial4j "0.6"]
   [com.vividsolutions/jts "1.13"]]
  :profiles {:dev {:plugins [[lein-midje "3.1.1"]]
                   :dependencies [[org.clojure/clojure "1.8.0"]
                                  [midje "1.6.3"]
                                  [midje-cascalog "0.4.0" :exclusions [org.clojure/clojure]]]}})
