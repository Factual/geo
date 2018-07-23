(ns geo.t-h3
  (:require [geo.h3 :as sut]
            [geo.geohash :as geohash]
            [geo.io :as io]
            [geo.jts :as jts]
            [geo.spatial :as spatial]
            [midje.sweet :refer [fact facts falsey roughly truthy]])
  (:import (org.locationtech.jts.geom Geometry Polygon)
           (com.uber.h3core.util GeoCoord)))

(def geohash-with-hole (jts/set-srid (.difference (spatial/to-jts (geohash/geohash "u4pruy"))
                                                  (spatial/to-jts (geohash/geohash "u4pruyk")))
                                     jts/default-srid))
(def h3-example-str "871f24ac5ffffff")
(def h3-example-long 608533827635118079)
(def h3-example-latitude 57.64911063015461)
(def h3-example-longitude 10.407439693808556)

(facts "h3 core functions"
       (fact "from coordinates"
             (sut/pt->h3 h3-example-latitude h3-example-longitude 7) => h3-example-long
             (sut/pt->h3-address h3-example-latitude h3-example-longitude 7) => h3-example-str)
       (fact "from point"
             (sut/pt->h3 (jts/point h3-example-latitude h3-example-longitude) 7) => h3-example-long
             (sut/pt->h3-address (jts/point h3-example-latitude h3-example-longitude) 7) => h3-example-str)
       (fact "strings and longs"
             (sut/to-long h3-example-str) => h3-example-long
             (sut/to-string h3-example-long) => h3-example-str)
       (fact "resolution"
             (sut/get-resolution h3-example-str) => 7
             (sut/get-resolution (sut/to-long h3-example-str)) => 7)
       (fact "jts boundary"
             (type (sut/to-jts h3-example-str)) => Polygon)
       (fact "geo coord"
             (type (first (sut/geo-coords (geohash/geohash "u4pruy")))) => GeoCoord)
       (fact "edges"
             (sut/edge "871f24ac4ffffff" "871f24ac0ffffff") => "1371f24ac4ffffff"
             (sut/edge-origin "1371f24ac4ffffff") => "871f24ac4ffffff"
             (sut/edge-destination "1371f24ac4ffffff") => "871f24ac0ffffff"
             (sut/edges "871f24ac4ffffff") => ["1171f24ac4ffffff" "1271f24ac4ffffff" "1371f24ac4ffffff"
                                               "1471f24ac4ffffff" "1571f24ac4ffffff" "1671f24ac4ffffff"]
             (type (first (sut/edge-boundary "1371f24ac4ffffff"))) => GeoCoord)
       (fact "h3->pt"
             (str (spatial/to-jts (sut/h3->pt h3-example-str)))
             => "POINT (10.423520614389421 57.65506363212537)")
       (fact "pentagon"
             (sut/pentagon? "8f28308280f18f2") => falsey
             (sut/pentagon? "821c07fffffffff") => truthy)
       (fact "validity checks"
             (sut/is-valid? h3-example-long) => truthy
             (sut/is-valid? -1) => falsey
             (sut/is-valid? h3-example-str) => truthy)
       (fact "neighbor checks"
             (sut/neighbors? "871f24ac4ffffff" "871f24ac0ffffff") => truthy
             (sut/neighbors? 608533827618340863 608533827551231999) => truthy
             (sut/neighbors? "871f24ac0ffffff" "871f24aeeffffff") => falsey))


(facts "h3 algorithms"
       (fact "rings"
             (sut/k-ring h3-example-str 0) => ["871f24ac5ffffff"]
             (sut/k-ring h3-example-str 1) => ["871f24ac5ffffff" "871f24ac4ffffff" "871f24ac0ffffff"
                                               "871f24ac1ffffff" "871f24aeaffffff" "871f24aeeffffff"
                                               "871f24ae3ffffff"]
             (sut/k-ring h3-example-str 2) => ["871f24ac5ffffff" "871f24ac4ffffff" "871f24ac0ffffff"
                                               "871f24ac1ffffff" "871f24aeaffffff" "871f24aeeffffff"
                                               "871f24ae3ffffff" "871f24ae2ffffff" "871f24af1ffffff"
                                               "871f24ac6ffffff" "871f24ac2ffffff" "871f24ac3ffffff"
                                               "871f24aceffffff" "871f24accffffff" "871f24aebffffff"
                                               "871f24ae8ffffff" "871f24aecffffff" "871f24ae1ffffff"
                                               "871f24ae0ffffff"]
             (sut/k-ring-distances h3-example-str 2) => [["871f24ac5ffffff"]
                                                         ["871f24ac4ffffff" "871f24ac0ffffff" "871f24ac1ffffff"
                                                          "871f24aeaffffff" "871f24aeeffffff" "871f24ae3ffffff"]
                                                         ["871f24ae2ffffff" "871f24af1ffffff" "871f24ac6ffffff"
                                                          "871f24ac2ffffff" "871f24ac3ffffff" "871f24aceffffff"
                                                          "871f24accffffff" "871f24aebffffff" "871f24ae8ffffff"
                                                          "871f24aecffffff" "871f24ae1ffffff" "871f24ae0ffffff"]])
       (fact "polyfill"
             (sut/polyfill (geohash/geohash "u4pruy") 9) => [617541026878062591
                                                             617541026799157247
                                                             617541026800992255
                                                             617541026877538303
                                                             617541026879111167]
             (sut/polyfill-address (geohash/geohash "u4pruy") 9) => ["891f24ac54bffff"
                                                                     "891f24ac097ffff"
                                                                     "891f24ac0b3ffff"
                                                                     "891f24ac543ffff"
                                                                     "891f24ac55bffff"]
             (-> (jts/multi-polygon [(spatial/to-jts (geohash/geohash "u4pruy"))
                                     (spatial/to-jts (geohash/geohash "u4pruu"))])
                 (sut/polyfill 9))
             => [617541026878062591 617541026799157247 617541026800992255 617541026877538303
                 617541026879111167 617541026790244351 617541026789982207 617541026789720063
                 617541026789457919]
             (-> (jts/multi-polygon [(spatial/to-jts (geohash/geohash "u4pruy"))
                                     (spatial/to-jts (geohash/geohash "u4pruu"))])
                 (sut/polyfill-address 9))
             => ["891f24ac54bffff" "891f24ac097ffff" "891f24ac0b3ffff" "891f24ac543ffff"
                 "891f24ac55bffff" "891f24ac00fffff" "891f24ac00bffff" "891f24ac007ffff"
                 "891f24ac003ffff"]
             (count (sut/polyfill geohash-with-hole 12)) => 1648)
       (fact "polyfill works recursively on large shapes"
             (count (sut/polyfill (jts/polygon-wkt [[-70 42 -70 44 -68 44 -68 42 -70 42]]) 7)) => 6769
             (count (sut/polyfill (jts/polygon-wkt [[-70 42 -70 44 -68 44 -68 42 -70 42]]) 9)) => 331662)
       (fact "compact/uncompact"
             (count (sut/compact (sut/polyfill geohash-with-hole 12))) => 310
             (count (sut/uncompact (sut/polyfill geohash-with-hole 12) 13)) => 11536)
       (fact "polyfills returning smaller results are uncompacted"
             (float (/ (count (sut/polyfill (geo.geohash/geohash "dr") 6))
                       (count (sut/polyfill (geo.geohash/geohash "dr") 5))))
             => (roughly 7 0.01))
       (fact "polyfills returning larger arrays are not uncompacted"
             (< (float (/ (count (sut/polyfill (geo.geohash/geohash "dr") 9))
                          (count (sut/polyfill (geo.geohash/geohash "dr") 8)))) 7)
             => truthy)
       (fact "polyfill is graceful on bad shapes"
             (sut/polyfill (geo.io/read-wkt "POLYGON EMPTY") 10)
             => []
             (sut/polyfill-address (geo.io/read-wkt "POLYGON EMPTY") 10)
             => [])
       (fact "multi-polygon"
             (count (jts/coordinates
                      (sut/multi-polygon (sut/polyfill geohash-with-hole 12)))) => 402)
       (fact "interoperability with geometry collections"
             (->> (sut/k-ring-distances h3-example-str 3)
                  (map sut/multi-polygon)
                  jts/geometry-collection
                  io/to-features
                  (#(nth % 1))
                  :geometry
                  jts/coordinates
                  count)
             => 26))
