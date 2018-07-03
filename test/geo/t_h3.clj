(ns geo.t-h3
  (:require [geo.h3 :as sut]
            [geo.geohash :as geohash]
            [geo.jts :as jts]
            [geo.spatial :as spatial]
            [midje.sweet :refer [fact facts]])
  (:import (org.locationtech.jts.geom Geometry LinearRing)
           (com.uber.h3core.util GeoCoord)))

(def geohash-with-hole (jts/set-srid (.difference (spatial/to-jts (geohash/geohash "u4pruy"))
                                                  (spatial/to-jts (geohash/geohash "u4pruyk")))
                                     jts/default-srid))

(facts "h3 core functions"
       (fact "from coordinates"
             (sut/pt->h3 57.64911063015461 10.407439693808556 7) => "871f24ac5ffffff")
       (fact "from point"
             (sut/pt->h3 (jts/point 57.64911063015461 10.407439693808556) 7) => "871f24ac5ffffff")
       (fact "strings and longs"
             (sut/string->long "871f24ac5ffffff") => 608533827635118079
             (sut/long->string 608533827635118079) => "871f24ac5ffffff")
       (fact "resolution"
             (sut/get-resolution "871f24ac5ffffff") => 7
             (sut/get-resolution (sut/string->long "871f24ac5ffffff")) => 7)
       (fact "boundary"
             (type (sut/jts-boundary "871f24ac5ffffff")) => LinearRing)
       (fact "geo coord"
             (type (first (sut/geo-coords (geohash/geohash "u4pruy")))) => GeoCoord)
       (fact "edges"
             (sut/edge "871f24ac4ffffff" "871f24ac0ffffff") => "1371f24ac4ffffff"
             (sut/edge-origin "1371f24ac4ffffff") => "871f24ac4ffffff"
             (sut/edge-destination "1371f24ac4ffffff") => "871f24ac0ffffff"
             (sut/edges "871f24ac4ffffff") => ["1171f24ac4ffffff" "1271f24ac4ffffff" "1371f24ac4ffffff"
                                               "1471f24ac4ffffff" "1571f24ac4ffffff" "1671f24ac4ffffff"]
             (type (first (sut/edge-boundary "1371f24ac4ffffff"))) => GeoCoord))


(facts "h3 algorithms"
       (fact "ring"
             (sut/ring "871f24ac5ffffff" 1) => [["871f24ac5ffffff"]
                                                ["871f24ac4ffffff" "871f24ac0ffffff" "871f24ac1ffffff"
                                                 "871f24aeaffffff" "871f24aeeffffff" "871f24ae3ffffff"]]
             (sut/ring-cells "871f24ac5ffffff" 1) => ["871f24ac5ffffff" "871f24ac4ffffff" "871f24ac0ffffff"
                                                      "871f24ac1ffffff" "871f24aeaffffff" "871f24aeeffffff"
                                                      "871f24ae3ffffff"])
       (fact "polyfill"
             (sut/polyfill (geohash/geohash "u4pruy") 9) => ["891f24ac54bffff"
                                                             "891f24ac097ffff"
                                                             "891f24ac0b3ffff"
                                                             "891f24ac543ffff"
                                                             "891f24ac55bffff"]
             (count (sut/polyfill geohash-with-hole 12)) => 1648)
       (fact "compact/uncompact"
             (count (sut/compact (sut/polyfill geohash-with-hole 12))) => 310
             (count (sut/uncompact (sut/polyfill geohash-with-hole 12) 13)) => 11536)
       (fact "multi-polygon"
             (count (jts/coordinates
                      (sut/multi-polygon (sut/polyfill geohash-with-hole 12)))) => 402))
