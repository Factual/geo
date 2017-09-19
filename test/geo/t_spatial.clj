(ns geo.t-spatial
  (:use midje.sweet)
  (:require [geo.jts :as jts]
            [geo.io :as gio]
            [geo.spatial :as s])
  (:import (org.locationtech.spatial4j.context SpatialContext)))

(facts "earth"
       (fact s/earth => (partial instance? SpatialContext)))

(facts "earth-radius"
       (fact "accurate at equator"
             (s/earth-radius (s/geohash-point 0 0))
             => (roughly s/earth-equatorial-radius 1/10000))
       (fact "accurate at pole"
             (s/earth-radius (s/geohash-point 90 0))
             => (roughly s/earth-polar-radius 1/10000))
       ; A particularly special point in France which we know the radius for
       (fact "accurate in France"
             (s/earth-radius (s/geohash-point 48.46791 0))
             => (roughly 6366197 10)))

(facts "degrees->radians"
       (fact (s/degrees->radians 0) => 0.0)
       (fact (s/degrees->radians 180) => Math/PI)
       (fact (s/degrees->radians 360) => (* 2 Math/PI)))

(facts "radians->degrees"
       (fact (s/radians->degrees 0) => 0.0)
       (fact (s/radians->degrees Math/PI) => 180.0)
       (fact (s/radians->degrees (* 2 Math/PI)) => 360.0))

(facts "square-degrees->steradians"
       (fact (s/square-degrees->steradians (s/square (/ 180 Math/PI)))
             => (roughly 1 0.00001)))

(facts "distance->radians"
       (fact (s/distance->radians 0) => 0.0)
       (fact (s/distance->radians s/earth-mean-circumference)
             => (roughly (* 2 Math/PI))))

(facts "radians->distance"
       (fact (s/radians->distance 0 0.0))
       (fact (s/radians->distance (* 2 Math/PI))
             => (roughly s/earth-mean-circumference)))

(facts "interchangeable points"
       (let [flip-spatial (comp s/to-spatial4j-point s/to-geohash-point)
             flip-geohash (comp s/to-geohash-point s/to-spatial4j-point)
             g1 (s/geohash-point 0 0)
             g2 (s/geohash-point 12.456 -98.765)
             s1 (s/spatial4j-point 0 0)
             s2 (s/spatial4j-point 12.456 -98.765)]
         ; Identity conversions
         (fact (s/to-geohash-point g2) g2)
         (fact (s/to-spatial4j-point g2) s2)

         ; Direct conversions
         (fact (s/to-spatial4j-point s2) s2)
         (fact (s/to-geohash-point s2) g2)

         ; Two-way conversions
         (fact (flip-geohash g1) => g1)
         (fact (flip-geohash g2) => g2)
         (fact (flip-spatial s1) => s1)
         (fact (flip-spatial s2) => s2)))

; Have some airports
(let [lhr (s/spatial4j-point 51.477500 -0.461388)
      syd (s/spatial4j-point -33.946110 151.177222)
      lax (s/spatial4j-point 33.942495 -118.408067)
      sfo (s/spatial4j-point 37.619105 -122.375236)
      oak (s/spatial4j-point 37.721306 -122.220721)
      ; and some distances between them, in meters
      lhr-syd 17015628
      syd-lax 12050690
      lax-lhr 8780169
      sfo-oak 17734]
  ; See http://www.gcmap.com/dist?P=LHR-SYD%2CSYD-LAX%2CLHR-LAX%0D%0A&DU=m&DM=&SG=&SU=mph
  (facts "distance in meters"
         ; This breaks VincentyGeodesy from the geohash library; there's a
         ; singularity at the poles.
         (future-fact (s/distance (geohash-point 89.999, 0)
                         (geohash-point 90.0, 0))
               => 1.234567)
         (fact (s/distance lhr syd) => (roughly lhr-syd))
         (fact (s/distance syd lax) => (roughly syd-lax))
         (fact (s/distance lax lhr) => (roughly lax-lhr))
         (fact (s/distance (s/geohash-point sfo)
                         (s/geohash-point oak)) => (roughly sfo-oak)))

  (facts "intersections"
         (fact "A circle around a point intersects that point."
               (s/intersects? (s/circle lhr 10) lhr) => truthy)
         (fact "A circle around Sydney to London has an error of ~ 5 KM"
               ; This is a pretty darn big circle; covers more than half the
               ; globe.
               (s/intersects? syd (s/circle lhr (+ 4500 (s/distance lhr syd))))
               => falsey
               (s/intersects? syd (s/circle lhr (+ 5000 (s/distance lhr syd))))
               => truthy)
         (fact "A circle around SFO to OAK has an error of roughly 13 meters."
               (s/intersects? sfo (s/circle oak (- (s/distance sfo oak) 12)))
               => truthy
               (s/intersects? sfo (s/circle oak (- (s/distance sfo oak) 14)))
               => falsey))

  (facts "relationships"
         (fact "relate returns keywords"
               (s/relate (s/circle oak 10) (s/circle oak 10))
               => :contains)))

(facts "JTS multi-polygons"
       (let [a (jts/multi-polygon-wkt [[[0 0, 2 0, 2 2, 0 0]]])
             b (jts/multi-polygon-wkt [[[0 0, 1 0, 0 1, 0 0]]])
             c (jts/multi-polygon-wkt [[[-1 -1, -2 -2, -1 -2, -1 -1]]])]
         (fact (s/intersects? a b) => true)
         (fact (s/intersects? a c) => false)
         (fact (s/intersects? b c) => false)))

(facts "Dateline Polygons"
       (let [poly-wkt [[179 0 179 1 -179 1 -179 0 179 0]]]
         (fact (-> poly-wkt jts/polygon-wkt s/height) => 1.0)
         (fact (-> poly-wkt jts/polygon-wkt s/width) => 2.0)
         (fact (-> poly-wkt jts/polygon-wkt s/bounding-box s/width) => 2.0)
         (fact (-> [poly-wkt] jts/multi-polygon-wkt s/width) => 2.0)
         (fact (-> poly-wkt jts/polygon-wkt s/bounding-box s/area) => (roughly 2.4727e10))))

(facts "Dateline-crossing geom handled properly with multiple to-shape calls"
       ;; Protecting against a previous bug
       ;; https://github.com/locationtech/spatial4j/issues/150
       (let [polygon (jts/polygon-wkt [[179 0 179 1 -179 1 -179 0 179 0]])]
         (fact (s/width polygon) => 2.0)
         (fact (s/width polygon) => 2.0)))

(facts "centroid"
       (fact (-> [[0 0, 10 0, 10 10, 0 10, 0 0]]
                  ; A little weird: centroids ignore holes in polygons
                  ; Maybe someday, try holes?
                  ; [1 1, 5 1,  5 9,   1 9,  1 1]]
                 jts/polygon-wkt
                 s/center)
             => (s/spatial4j-point 5 5)))

(facts "linestring length"
       (s/length (jts/line-string-wkt [0 0 0 1])) => 110574.3885571743
       (s/length (jts/line-string-wkt [0 0 0 2])) => 221149.4533708848
       (s/length (jts/line-string-wkt [0 0 0 1 0 2])) => 221149.4533708848)

(facts "linestring splitting basic case"
       (let [ls (jts/line-string-wkt [0 0 0 2])
             resegmented (s/resegment ls 10000)]
         (s/length ls) => 221149.4533708848
         (count resegmented) => 23
         (reduce + (map s/length resegmented)) => (roughly 221149)
         (reduce + (map s/length (s/resegment sample-ls 100))) => (roughly 221149.4533708848)))

(let [ls (jts/line-string-wkt [0 0 0 1 0 2])
      d1 (s/dist-at-idx ls 0)
      segment-max 10000.0
      segment-over (> d1 segment-max)
      ratio (/ segment-max d1)
      segment-1 (s/segment-at-idx ls 0)
      next-point (.pointAlongOffset (s/segment-at-idx sample-ls 0) ratio 0)
      adjusted-dist (s/distance (s/point-n ls 0) next-point)]
  (println "Distance at first segment: " d1)
  (println "First segment over max length: " segment-over)
  (println "Ratio: " ratio)
  (println "Next point with distance would be: " next-point)
  (println "Chopped distance: " adjusted-dist)
  )

(def long-sample [-54.4482421875 23.946096014998382 -53.9208984375 24.467150664739002 -52.27294921875 24.926294766395593 -50.60302734375 24.487148563173424 -50.42724609375 23.704894502324912 -50.20751953125 22.63429269379353 -51.17431640625 22.51255695405145 -51.943359375 22.755920681486405 -51.85546874999999 23.443088931121785 -52.55859375 23.865745352647956 -53.23974609375 23.301901124188877 -53.3935546875 22.51255695405145 -54.07470703125 22.471954507739227 -54.29443359375 23.160563309048314])

(facts "splitting more complex linestring"
       (let [ls (jts/line-string-wkt long-sample)
             rs (s/resegment ls 100000)]
         (s/length ls) => 1316265.356651721
         (->> rs (map s/length) (reduce +)) => (roughly 1316265)
         (->> rs
              (drop-last 1) ;; last segment just has remaining distance
              (map s/length)
              (map (roughly 100000))) => (n-of true 13)
         (-> rs last s/length) => (roughly 16265 50)
         (-> rs count) => 14)
       (fact "Splitting linestring under max gives single segment"
             (let [ls (jts/line-string-wkt [0 0 0.0001 0.0001 0.0002 0.0002])]
               (s/length ls) => (roughly 31.38)
               (count (s/resegment ls 1000)) => 1
               (s/length (first (s/resegment ls 1000))) => (roughly 31.38))))
