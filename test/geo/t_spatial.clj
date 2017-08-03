(ns geo.t-spatial
  (:use midje.sweet)
  (:require [geo.jts :as jts]
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

(facts "BUG: Multiple to-shape calls breaks dateline handling without cloning"
       (let [polygon (jts/polygon-wkt [[179 0 179 1 -179 1 -179 0 179 0]])]
         (fact (s/width polygon) => 2.0)
         (fact (s/width polygon) => 2.0)
         (fact (s/width bbox-1) => (s/width bbox-2))))

(facts "centroid"
       (fact (-> [[0 0, 10 0, 10 10, 0 10, 0 0]]
                  ; A little weird: centroids ignore holes in polygons
                  ; Maybe someday, try holes?
                  ; [1 1, 5 1,  5 9,   1 9,  1 1]]
                 jts/polygon-wkt
                 s/center)
             => (s/spatial4j-point 5 5)))
