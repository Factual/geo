(ns geo.t-jts
  (:require [geo.jts :refer :all]
            [midje.sweet :refer [fact facts truthy]])
  (:import (org.locationtech.jts.geom Coordinate)))

(facts "coordinate"
       (fact (coordinate 1 2) => (Coordinate. 1 2)))

(facts "polygon"
       (fact (->> [0 0 10 0 10 10 0 0]
               (partition 2)
               (map (partial apply coordinate))
               linear-ring
               polygon
               str)
             => "POLYGON ((0 0, 10 0, 10 10, 0 0))"))

(facts "multipolygon-wkt"
       (fact (str (multi-polygon-wkt [[[10 10, 110 10, 110 110, 10 110, 10 10],
                                       [20 20, 20 30, 30 30, 30 20, 20 20],
                                       [40 20, 40 30, 50 30, 50 20, 40 20]]]))
             => "MULTIPOLYGON (((10 10, 110 10, 110 110, 10 110, 10 10), (20 20, 20 30, 30 30, 30 20, 20 20), (40 20, 40 30, 50 30, 50 20, 40 20)))"))

(facts "linestrings"
       (.getNumPoints (linestring-wkt [0 0 0 1 0 2])) => 3
       (type (first (coords (linestring-wkt [0 0 0 1 0 2])))) => org.locationtech.jts.geom.Coordinate
       (count (coords (linestring-wkt [0 0 0 1 0 2]))) => 3
       (.getNumPoints (linestring (coords (linestring-wkt [0 0 0 1 0 2])))) => 3
       (.getX (point-n (linestring-wkt [0 0 0 1 0 2]) 1)) => 0.0
       (.getY (point-n (linestring-wkt [0 0 0 1 0 2]) 1)) => 1.0
       (let [segment (segment-at-idx (linestring-wkt [0 -1 1 2]) 0)]
         (type segment) => org.locationtech.jts.geom.LineSegment
         (-> segment .p0 .x) => 0.0
         (-> segment .p0 .y) => -1.0
         (-> segment .p1 .x) => 1.0
         (-> segment .p1 .y) => 2.0))

(facts "proj4j"
       (fact "point: 3 param transform"
         (same-geom? (transform-geom (point 3.8142776 51.285914 4326) 23031)
                     (point 556878.9016076007 5682145.166264554 23031))
         => truthy
         (same-geom? (set-srid (transform-geom (point 3.8142776 51.285914 4326)
                                               "+proj=utm +zone=31 +ellps=intl +towgs84=-87,-98,-121,0,0,0,0 +units=m +no_defs")
                               23031)
                     (point 556878.9016076007 5682145.166264554 23031))
         => truthy)
       (fact "geometry: stereographic azimuthal, using a linestring"
         (same-geom?
           (transform-geom (linestring-wkt [0 -75 -57.65625 -79.21875]) 3031)
           (linestring-wkt [0 1638783.2384072358 -992481.6337864351 628482.0632797639] 3031))
         => truthy)
       (same-geom?
         (set-srid (transform-geom (linestring-wkt [0 -75 -57.65625 -79.21875])
                                   "+proj=stere +lat_0=-90 +lat_ts=-71 +lon_0=0 +k=1 +x_0=0 +y_0=0 +datum=WGS84 +units=m +no_defs")
                   3031)
         (linestring-wkt [0 1638783.2384072358 -992481.6337864351 628482.0632797639] 3031))
       => truthy)