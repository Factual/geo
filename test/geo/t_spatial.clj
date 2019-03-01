(ns geo.t-spatial
  (:require [geo.jts :as jts]
            [geo.spatial :as s]
            [geo.geohash :refer :all]
            [midje.sweet :refer [fact facts falsey future-fact n-of roughly throws truthy]])
  (:import (org.locationtech.spatial4j.context SpatialContext)
           (org.locationtech.spatial4j.shape.jts JtsGeometry)))

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
       (let [g->s (comp s/to-spatial4j-point s/to-geohash-point)
             s->g (comp s/to-geohash-point s/to-spatial4j-point)
             j->s (comp s/to-spatial4j-point s/to-jts)
             s->j (comp s/to-jts s/to-spatial4j-point)
             j->g (comp s/to-geohash-point s/to-jts)
             g->j (comp s/to-jts s/to-geohash-point)
             g1 (s/geohash-point 0 0)
             g2 (s/geohash-point 12.456 -98.765)
             s1 (s/spatial4j-point 0 0)
             s2 (s/spatial4j-point 12.456 -98.765)
             j1 (s/jts-point 0 0)
             j2 (s/jts-point 12.456 -98.765)]
         ; Identity conversions
         (fact (s/to-geohash-point g2) => g2)
         (fact (s/to-spatial4j-point g2) => s2)
         (fact (s/to-jts j2) => j2)

         ; Direct conversions
         (fact (s/to-spatial4j-point g2) => s2)
         (fact (s/to-spatial4j-point j2) => s2)
         (fact (s/to-geohash-point s2) => g2)
         (fact (s/to-geohash-point j2) => g2)
         (fact (s/to-jts s2) => j2)
         (fact (s/to-jts g2) => j2)

         ; Two-way conversions
         (fact (s->g g1) => g1)
         (fact (s->g g2) => g2)
         (fact (j->g g1) => g1)
         (fact (j->g g2) => g2)
         (fact (g->s s1) => s1)
         (fact (g->s s2) => s2)
         (fact (j->s s1) => s1)
         (fact (j->s s2) => s2)
         (fact (s->j j1) => j1)
         (fact (s->j j2) => j2)
         (fact (g->j j1) => j1)
         (fact (g->j j2) => j2)))

(facts "interchangeable polygons"
       (let [s->j (comp s/to-jts s/to-shape)
             j1 (->> [0 0 10 0 10 10 0 0]
                     (partition 2)
                     (map (partial apply jts/coordinate))
                     jts/linear-ring
                     jts/polygon)]

         ; Identity conversion
         (fact (s/to-jts j1) => j1)

         ; Direct conversion
         (fact (type (s/to-shape j1)) => JtsGeometry)

         ; Two-way conversion
         (fact (s->j j1) => j1)))

(facts "spatial4j circles cannot be converted to JTS"
       (let [cir1 (s/circle (s/point 0 0) 100)]

         ; Attempt to convert GeoCircle to JTS.
         (fact (s/to-jts cir1) => throws)

         ; Attempt to convert GeoCircle to projected JTS.
         (fact (s/to-jts cir1 jts/default-srid) => throws)))

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
       (s/length (jts/linestring-wkt [0 0 0 1])) => (roughly 110574.38)
       (s/length (jts/linestring-wkt [0 0 0 2])) => (roughly 221149.45)
       (s/length (jts/linestring-wkt [0 0 0 1 0 2])) => (roughly 221149.45))

(def long-sample [-54.4482421875 23.946096014998382 -53.9208984375 24.467150664739002 -52.27294921875 24.926294766395593 -50.60302734375 24.487148563173424 -50.42724609375 23.704894502324912 -50.20751953125 22.63429269379353 -51.17431640625 22.51255695405145 -51.943359375 22.755920681486405 -51.85546874999999 23.443088931121785 -52.55859375 23.865745352647956 -53.23974609375 23.301901124188877 -53.3935546875 22.51255695405145 -54.07470703125 22.471954507739227 -54.29443359375 23.160563309048314])

(facts "Resegmenting linestrings at max length"
       (facts "Splitting simple linestring over max length"
              (let [ls (jts/linestring-wkt [0 0 0 2])
                    resegmented (s/resegment ls 10000)]
                (s/length ls) => 221149.4533708848
                (count resegmented) => 23
                (reduce + (map s/length resegmented)) => (roughly 221149)))
       (fact "Splitting linestring under max gives single segment"
             (let [ls (jts/linestring-wkt [0 0 0.0001 0.0001 0.0002 0.0002])]
               (s/length ls) => (roughly 31.38)
               (count (s/resegment ls 1000)) => 1
               (s/length (first (s/resegment ls 1000))) => (roughly 31.38)))
       (fact "Splitting complex linestring over max length"
             (let [ls (jts/linestring-wkt long-sample)
                   rs (s/resegment ls 100000)]
               (s/length ls) => 1316265.356651721
               (->> rs (map s/length) (reduce +)) => (roughly 1316265)
               (->> rs
                    (drop-last 1) ;; last segment just has remaining distance
                    (map s/length)
                    (map (roughly 100000))) => (n-of true 13)
               (-> rs last s/length) => (roughly 16265 50)
               (-> rs count) => 14)))

;; overriding core.rand with a custom generator to specify seed
(let [gen (java.util.Random. 1)]
  (with-redefs [clojure.core/rand (fn [& args] (if-let [i (first args)]
                                                 (* i (.nextDouble gen))
                                                 (.nextDouble gen)))]
    (facts (str "Getting a random point in a radius")
           (let [points (take 20 (repeatedly (partial s/rand-point-in-radius 0 0 500)))]
             (->> points
                  (map (partial s/distance (s/point 0 0)))
                  (map #(>= 500 % 0))) => (n-of true 20))

           (fact "Accepts custom distribution function"
                 (let [distrib (fn [] 1)
                       points (take 20)]
                   (->> (partial s/rand-point-in-radius 0 0 100 distrib)
                        (repeatedly)
                        (take 20)
                        (map (partial s/distance (s/point 0 0)))
                        (map (roughly 100.0 1.0))) => (n-of true 20)))

           (fact "Uniform vs Clustered distributions"
                 (let [c-points (take 1000 (repeatedly (partial s/rand-point-in-radius 0 0 100 :clustered)))
                       u-points (take 1000 (repeatedly (partial s/rand-point-in-radius 0 0 100 :uniform)))
                       dist (partial s/distance (s/point 0 0))]
                   ;; Clustered distribution places ~50% of points within 50m, i.e. radius midpoint
                   (->> c-points
                        (map dist)
                        (filter (partial > 50))
                        count) => (roughly 500 20)

                   ;; Uniform distribution places ~25% of points within 50m, i.e. radius midpoint
                   (->> u-points
                        (map dist)
                        (filter (partial > 50))
                        count) => (roughly 250 10))))))

(facts "features"
       (let [g1 (s/jts-point 0 0)
             p1 {:name "null"}
             f1 (s/->Feature g1 p1)
             f2 (s/map->Feature {:geometry g1
                                 :properties p1
                                 :additional "information"})]
         (fact "feature record is recognized"
               (type f1) => geo.spatial.Feature
               (type f2) => geo.spatial.Feature)
         (fact "feature record has a geometry field"
               (:geometry f1) => g1
               (:geometry f2) => g1)
         (fact "feature record has a properties field"
               (:properties f1) => p1
               (:properties f2) => p1)
         (fact "features generated from maps retain additional fields"
               (:additional f2) => "information"
               (:additional f1) => nil)))
