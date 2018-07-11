(ns geo.t-jts
  (:require [geo.jts :refer :all]
            [geo.geohash :as geohash]
            [geo.spatial :as spatial]
            [midje.sweet :refer [fact facts throws roughly truthy]])
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

(facts "polygons <-> multipolygon"
       (fact "multipolygon to polygons"
             (str (first (polygons (multi-polygon-wkt [[[-1 -1 11 -1 11 11 -1 -1]],
                                                       [[0 0 10 0 10 10 0 0]]]))))
             => "POLYGON ((-1 -1, 11 -1, 11 11, -1 -1))")
       (fact "polygons to multipolygon"
             (str (multi-polygon [(polygon-wkt [[-1 -1 11 -1 11 11 -1 -1]])
                                  (polygon-wkt [[0 0 10 0 10 10 0 0]])]))
             => "MULTIPOLYGON (((-1 -1, 11 -1, 11 11, -1 -1)), ((0 0, 10 0, 10 10, 0 0)))")
       (fact "multipolygon SRIDs"
             (-> (multi-polygon [(spatial/to-jts (geohash/geohash "u4pruy"))
                                 (spatial/to-jts (geohash/geohash "u4pruu"))])
                 get-srid) => 4326
             (-> (multi-polygon [(spatial/to-jts (geohash/geohash "u4pruy"))
                                 (spatial/to-jts (geohash/geohash "u4pruu"))])
                 polygons
                 first
                 get-srid) => 4326))

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

(facts "Comparing geometry SRIDs"
       (let [g1 (linestring-wkt [0 0 0 1 0 2])
             g2 (linestring-wkt [1 1 2 2 3 3])
             g3 (linestring-wkt [1 1 2 2 3 3] 1234)]
         (get-srid g1) => 4326
         (same-srid? g1 g2) => true
         (same-srid? (set-srid g1 0) (set-srid g2 0)) => true
         (same-srid? g1 g3) => false))

(fact "setting SRID for a geom"
      (get-srid (point 0 0)) => 4326
      (get-srid (set-srid (point 0 0) 23031)) => 23031)

(facts "proj4j"
       (fact "point: 3 param transform"
         (same-geom? (transform-geom (point 3.8142776 51.285914 4326) 23031)
                     (point 556878.9016076007 5682145.166264554 23031))
         => truthy
         (same-geom? (set-srid (transform-geom (point 3.8142776 51.285914 4326)
                                               "+proj=utm +zone=31 +ellps=intl +towgs84=-87,-98,-121,0,0,0,0 +units=m +no_defs")
                               23031)
                     (point 556878.9016076007 5682145.166264554 23031))
         => truthy
         (spatial/latitude (point 556878.9016076007 5682145.166264554 23031))
         => (roughly 51.285914 0.000001)
         (spatial/longitude (point 556878.9016076007 5682145.166264554 23031))
         => (roughly 3.8142776 0.000001))
       (fact "geometry: stereographic azimuthal, using a linestring"
         (same-geom?
           (transform-geom (linestring-wkt [0 -75 -57.65625 -79.21875]) 3031)
           (linestring-wkt [0 1638783.2384072358 -992481.6337864351 628482.0632797639] 3031))
         => truthy)
       (fact "geometry: stereographic azimuthal, using a linestring, projected with proj4 string"
         (same-geom?
           (set-srid (transform-geom (linestring-wkt [0 -75 -57.65625 -79.21875])
                                     "+proj=stere +lat_0=-90 +lat_ts=-71 +lon_0=0 +k=1 +x_0=0 +y_0=0 +datum=WGS84 +units=m +no_defs")
                     3031)
           (linestring-wkt [0 1638783.2384072358 -992481.6337864351 628482.0632797639] 3031))
         => truthy)
       (fact "geometry: projecting from a geometry with 0 SRID using only target CRS throws exception"
             (transform-geom (point 10 10 0) 4326)
             => (throws java.lang.AssertionError))
       (fact "geometry: projecting from a geometry with a 0 SRID using both a source and target CRS"
             (same-geom?
               (transform-geom (point 10 10 0) 4326 4326)
               (point 10 10 4326))
             => truthy)
       (fact "An EPSG can be specified as an int or as an 'EPSG:XXXX' string. as an equivalent proj4 string"
             (let [p1 (point 3.8142776 51.285914 4326)
                   p2 (point 556878.9016076007 5682145.166264554 23031)]
               (same-geom? (transform-geom p1 23031) p2)
               => truthy
               (same-geom? (transform-geom p1 "EPSG:23031") p2)
               => truthy))
       (fact "If using a different CRS name or proj4 string, SRID is not automatically set"
             (let [p1 (point 3.8142776 51.285914 4326)
                   p2 (point 556878.9016076007 5682145.166264554 23031)]
               (same-geom? (-> (transform-geom p1 "+proj=utm +zone=31 +ellps=intl +towgs84=-87,-98,-121,0,0,0,0 +units=m +no_defs")
                               (set-srid 23031))
                           p2)
               => truthy))
       (fact "CRS systems with different names"
             (let [p1 (point 42.3601 -71.0589)
                   p2 (transform-geom p1 26986)]
               (.getX (transform-geom p1 "EPSG:26986"))
               => (.getX p2)
               (.getX (transform-geom p1 "ESRI:26986"))
               => (roughly (.getX p2) 0.001)
               (.getX (transform-geom p1 "NAD83:2001"))
               => (.getX p2)
               (.getX (transform-geom p1 "+proj=lcc +lat_1=42.68333333333333 +lat_2=41.71666666666667 +lat_0=41 +lon_0=-71.5 +x_0=200000 +y_0=750000 +datum=NAD83 +units=m +no_defs"))
               => (.getX p2))))
