(defproject
  geo "1.0.0"
  :repositories
  {"factual" "http://maven.corp.factual.com/nexus/content/groups/public"
   "releases" {:url "http://maven.corp.factual.com/nexus/content/repositories/releases"
               :sign-releases false}
   "snapshots" {:url "http://maven.corp.factual.com/nexus/content/repositories/snapshots"
                :snapshots {:update :always}}}
  :description "Geohashing places, addresses, and geopulse data with cascalog."
  :dependencies
  [[org.clojure/math.numeric-tower "0.0.2"]
   [la.tomoj/geohash-java "1.0.6"]
   [com.spatial4j/spatial4j "0.3"]
   ; JTS depends on an ancient version of xerces which breaks hadoop
   [com.vividsolutions/jts "1.11" :exclusions [xerces/xercesImpl]]]
  :profiles {:dev {:plugins [[lein-midje "3.1.1"]]
                   :dependencies [[org.clojure/clojure "1.6.0"]
                                  [midje "1.6.3"]
                                  [midje-cascalog "0.4.0"]]}})
