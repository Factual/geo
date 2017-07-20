(ns geo.t-spatial
  (:use midje.sweet
        geo.spatial
        [geo.jts :only [multi-polygon-wkt]])
  (:require [geo.jts :as jts])
  (:import (org.locationtech.spatial4j.context SpatialContext)))

(facts "earth"
       (fact earth => (partial instance? SpatialContext)))

(facts "earth-radius"
       (fact "accurate at equator"
             (earth-radius (geohash-point 0 0))
             => (roughly earth-equatorial-radius 1/10000))
       (fact "accurate at pole"
             (earth-radius (geohash-point 90 0))
             => (roughly earth-polar-radius 1/10000))
       ; A particularly special point in France which we know the radius for
       (fact "accurate in France"
             (earth-radius (geohash-point 48.46791 0))
             => (roughly 6366197 10)))

(facts "degrees->radians"
       (fact (degrees->radians 0) => 0.0)
       (fact (degrees->radians 180) => Math/PI)
       (fact (degrees->radians 360) => (* 2 Math/PI)))

(facts "radians->degrees"
       (fact (radians->degrees 0) => 0.0)
       (fact (radians->degrees Math/PI) => 180.0)
       (fact (radians->degrees (* 2 Math/PI)) => 360.0))

(facts "square-degrees->steradians"
       (fact (square-degrees->steradians (square (/ 180 Math/PI)))
             => (roughly 1 0.00001)))

(facts "distance->radians"
       (fact (distance->radians 0) => 0.0)
       (fact (distance->radians earth-mean-circumference)
             => (roughly (* 2 Math/PI))))

(facts "radians->distance"
       (fact (radians->distance 0 0.0))
       (fact (radians->distance (* 2 Math/PI))
             => (roughly earth-mean-circumference)))

(facts "interchangeable points"
       (let [flip-spatial (comp to-spatial4j-point to-geohash-point)
             flip-geohash (comp to-geohash-point to-spatial4j-point)
             g1 (geohash-point 0 0)
             g2 (geohash-point 12.456 -98.765)
             s1 (spatial4j-point 0 0)   
             s2 (spatial4j-point 12.456 -98.765)]
         ; Identity conversions
         (fact (to-geohash-point g2) g2)
         (fact (to-spatial4j-point g2) s2)
         
         ; Direct conversions
         (fact (to-spatial4j-point s2) s2)
         (fact (to-geohash-point s2) g2)

         ; Two-way conversions
         (fact (flip-geohash g1) => g1) 
         (fact (flip-geohash g2) => g2)
         (fact (flip-spatial s1) => s1)
         (fact (flip-spatial s2) => s2)))

; Have some airports
(let [lhr (spatial4j-point 51.477500 -0.461388)
      syd (spatial4j-point -33.946110 151.177222)
      lax (spatial4j-point 33.942495 -118.408067)
      sfo (spatial4j-point 37.619105 -122.375236)
      oak (spatial4j-point 37.721306 -122.220721)
      ; and some distances between them, in meters
      lhr-syd 17015628
      syd-lax 12050690
      lax-lhr 8780169
      sfo-oak 17734]
  ; See http://www.gcmap.com/dist?P=LHR-SYD%2CSYD-LAX%2CLHR-LAX%0D%0A&DU=m&DM=&SG=&SU=mph
  (facts "distance in meters"
         ; This breaks VincentyGeodesy from the geohash library; there's a
         ; singularity at the poles.
         (future-fact (distance (geohash-point 89.999, 0)
                         (geohash-point 90.0, 0))
               => 1.234567)
         (fact (distance lhr syd) => (roughly lhr-syd))
         (fact (distance syd lax) => (roughly syd-lax))
         (fact (distance lax lhr) => (roughly lax-lhr))
         (fact (distance (geohash-point sfo)
                         (geohash-point oak)) => (roughly sfo-oak)))

  (facts "intersections"
         (fact "A circle around a point intersects that point."
               (intersects? (circle lhr 10) lhr) => truthy)
         (fact "A circle around Sydney to London has an error of ~ 5 KM"
               ; This is a pretty darn big circle; covers more than half the
               ; globe.
               (intersects? syd (circle lhr (+ 4500 (distance lhr syd))))
               => falsey
               (intersects? syd (circle lhr (+ 5000 (distance lhr syd))))
               => truthy)
         (fact "A circle around SFO to OAK has an error of roughly 13 meters."
               (intersects? sfo (circle oak (- (distance sfo oak) 12)))
               => truthy
               (intersects? sfo (circle oak (- (distance sfo oak) 14)))
               => falsey))

  (facts "relationships"
         (fact "relate returns keywords"
               (relate (circle oak 10) (circle oak 10))
               => :contains)))

(facts "JTS multi-polygons"
       (let [a (multi-polygon-wkt [[[0 0, 2 0, 2 2, 0 0]]])
             b (multi-polygon-wkt [[[0 0, 1 0, 0 1, 0 0]]])
             c (multi-polygon-wkt [[[-1 -1, -2 -2, -1 -2, -1 -1]]])]
         (fact (intersects? a b) => true)
         (fact (intersects? a c) => false)
         (fact (intersects? b c) => false)))

(facts "Alaska"
       (let [evil (multi-polygon-wkt [[[179 0, 179 1, -179 1, -179 0, 179 0]]])]
         (fact (height evil) => 1.0)
         (fact (width evil) => 2.0)
         (fact (area (bounding-box evil)) => (roughly 2.4727e10))
         (fact (center (bounding-box evil)) => (point 0.5 180.0))
         (fact (center evil) => (point 0.5 180.0))))

(facts "centroid"
       (fact (-> [[0 0, 10 0, 10 10, 0 10, 0 0]]
                  ; A little weird: centroids ignore holes in polygons
                  ; Maybe someday, try holes?
                  ; [1 1, 5 1,  5 9,   1 9,  1 1]]
                 jts/polygon-wkt
                 center)
             => (spatial4j-point 5 5)))
