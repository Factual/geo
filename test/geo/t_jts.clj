(ns geo.t-jts
  (:require [geo.jts :refer :all]
            [geo.crs :as crs]
            [geo.geohash :as geohash]
            [geo.spatial :as spatial]
            [midje.sweet :refer [fact facts throws roughly truthy]])
  (:import (org.locationtech.jts.geom Coordinate CoordinateXYZM)))

(facts "coordinate"
       (fact "XY coordinate"
             (coordinate 1 2) => (Coordinate. 1 2)
             (.getX (coordinate 1 2)) => 1.0
             (.getY (coordinate 1 2)) => 2.0
             (Double/isNaN (.getZ (coordinate 1 2))) => true)
       (fact "XYZ coordinate"
             (coordinate 1 2 3) => (Coordinate. 1 2 3)
             (.getX (coordinate 1 2 3)) => 1.0
             (.getY (coordinate 1 2 3)) => 2.0
             (.getZ (coordinate 1 2 3)) => 3.0)
       (fact "XYZ coordinates can still be pulled out from geometries that don't support higher dimensions"
             (.getCoordinate (point (coordinate 1 2 3))) => (Coordinate. 1 2 3)
             (.getCoordinateN (linestring [(coordinate 1 2 3) (coordinate 4 5 6)]) 0) => (Coordinate. 1 2 3))
       (fact "XYZM coordinate"
             (coordinate 1 2 3 4) => (CoordinateXYZM. 1 2 3 4)
             (.getX (coordinate 1 2 3 4)) => 1.0
             (.getY (coordinate 1 2 3 4)) => 2.0
             (.getZ (coordinate 1 2 3 4)) => 3.0
             (.getM (coordinate 1 2 3 4)) => 4.0)
       (fact "XYZM coordinates can still be pulled out from geometries that don't support higher dimensions"
             (.getCoordinate (point (coordinate 1 2 3 4))) => (CoordinateXYZM. 1 2 3 4)
             (.getCoordinateN (linestring [(coordinate 1 2 3 4) (coordinate 4 5 6 7)]) 0) => (CoordinateXYZM. 1 2 3 4)))

(facts "multi-point"
       (fact (str (multi-point [(point 0 0) (point 1 1)]))
             => "MULTIPOINT ((0 0), (1 1))"))

(facts "coordinate sequence"
       (fact "XY/XYZ coordinate sequence"
             (.getDimension (coordinate-sequence [(coordinate 1 1) (coordinate 2 2)])) => 3
             (.getDimension (coordinate-sequence [(coordinate 1 1 1) (coordinate 2 2 2)])) => 3)
       (fact "XYZM coordinate sequence"
             (.getDimension (coordinate-sequence [(coordinate 1 1 1 1) (coordinate 2 2 2 2)])) => 4))

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
             (str (first (geometries (multi-polygon-wkt [[[-1 -1 11 -1 11 11 -1 -1]],
                                                       [[0 0 10 0 10 10 0 0]]]))))
             => "POLYGON ((-1 -1, 11 -1, 11 11, -1 -1))")
       (fact "polygons to multipolygon"
             (str (multi-polygon [(polygon-wkt [[-1 -1 11 -1 11 11 -1 -1]])
                                  (polygon-wkt [[0 0 10 0 10 10 0 0]])]))
             => "MULTIPOLYGON (((-1 -1, 11 -1, 11 11, -1 -1)), ((0 0, 10 0, 10 10, 0 0)))")
       (fact "multipolygon SRIDs"
             (-> (multi-polygon [(spatial/to-jts (geohash/geohash "u4pruy"))
                                 (spatial/to-jts (geohash/geohash "u4pruu"))])
                 crs/get-srid) => 4326
             (-> (multi-polygon [(spatial/to-jts (geohash/geohash "u4pruy"))
                                 (spatial/to-jts (geohash/geohash "u4pruu"))])
                 geometries
                 first
                 crs/get-srid) => 4326))

(facts "geometries <-> geometrycollection"
       (fact "points"
             (str (first (geometries (geometry-collection [(point 0 0)
                                                           (point 1 1)]))))
             => "POINT (0 0)")
       (fact "srids"
             (crs/get-srid (first (geometries (geometry-collection [(point 0 0 2229)
                                                                (point 0 0 2229)]))))
             => 2229))

(facts "line segments"
       (let [c1 (coordinate 0 0)
             c2 (coordinate 1 1)
             ls (line-segment (coordinate 0 0) (coordinate 1 1))]
         (fact (type ls) => org.locationtech.jts.geom.LineSegment)
         (fact (.getCoordinate ls 0) => c1)
         (fact (.getCoordinate ls 1) => c2)))

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

(facts "multi-linestrings"
       (fact (str (multi-linestring
                    [(linestring [(coordinate 0 0) (coordinate 1 1)])
                     (linestring [(coordinate 2 2) (coordinate 3 3)])]))
             => "MULTILINESTRING ((0 0, 1 1), (2 2, 3 3))"))

(facts "multi-linestring-wkt"
       (fact (str (multi-linestring-wkt [[0 0, 1 0, 0 2, 0 0] [0 -1 1 2]]))
             => "MULTILINESTRING ((0 0, 1 0, 0 2, 0 0), (0 -1, 1 2))"))

(facts "Comparing geometry SRIDs"
       (let [g1 (linestring-wkt [0 0 0 1 0 2])
             g2 (linestring-wkt [1 1 2 2 3 3])
             g3 (linestring-wkt [1 1 2 2 3 3] 1234)]
         (crs/get-srid g1) => 4326
         (same-srid? g1 g2) => true
         (same-srid? (set-srid g1 0) (set-srid g2 0)) => true
         (same-srid? g1 g3) => false))

(fact "setting SRID for a geom"
      (crs/get-srid (point 0 0)) => 4326
      (crs/get-srid (set-srid (point 0 0) 23031)) => 23031)

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
       (fact "geometry: projection can happen using an external transform object, though SRID may be set to 0 if it cannot be determined."
             (same-geom?
               (transform-geom (point 3.8142776 51.285914 4326) (crs/create-transform 4326 23031))
               (point 556878.9016076007 5682145.166264554 23031))
             => truthy)
       (fact "An EPSG can be specified as a number, an 'EPSG:XXXX' string, as an equivalent proj4 string,
              or a proj4j CRS object."
             (let [p1 (point 3.8142776 51.285914 4326)
                   p2 (point 556878.9016076007 5682145.166264554 23031)]
               (same-geom? (transform-geom p1 23031) p2)
               => truthy
               (same-geom? (transform-geom p1 "EPSG:23031") p2)
               => truthy
               (crs/get-srid (transform-geom p1 23031))
               => 23031
               (crs/get-srid (transform-geom p1 "EPSG:23031"))
               => 23031
               (crs/get-srid (transform-geom p1 (crs/create-crs 23031)))
               => 23031))
       (fact "If using a different CRS name or proj4 string, SRID is not automatically set"
             (let [p1 (point 3.8142776 51.285914 4326)
                   p2 (point 556878.9016076007 5682145.166264554 23031)]
               (same-geom? (-> (transform-geom p1 "+proj=utm +zone=31 +ellps=intl +towgs84=-87,-98,-121,0,0,0,0 +units=m +no_defs")
                               (set-srid 23031))
                           p2)
               => truthy))
       (fact "crs/set-srid can take any Transformable"
             (let [p1 (point 10 10 0)]
               (crs/get-srid (set-srid p1 (crs/create-crs 23031)))
               => 23031
               (crs/get-srid (set-srid p1 (crs/create-crs "EPSG:23031")))
               => 23031))
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
               => (.getX p2)))
       (fact "An external GeometryFactory can be passed"
             (let [p1 (point 42.3601 -71.0589)
                   p2 (transform-geom p1 3586)
                   p3 (transform-geom p1 3586 (crs/get-geometry-factory 3586))
                   p4 (transform-geom p1 3586 (crs/get-geometry-factory p2))]
               (crs/get-srid p2) => 3586
               (crs/get-srid p3) => 3586
               (crs/get-srid p4) => 3586
               (same-geom? p2 p3) => true
               (same-geom? p2 p4) => true
               (same-geom? p3 p4) => true))
       (fact "When passing two CRSs and a GeometryFactory, the GeometryFactory's SRID will be used."
             (let [c1 4326
                   c2 3586
                   s1 (crs/create-crs c1)
                   s2 (crs/create-crs c2)
                   f1 (crs/get-geometry-factory 3586)
                   f_false (crs/get-geometry-factory 9999)
                   t1 (crs/create-transform 4326 3586)
                   p1 (point 42.3601 -71.0589)
                   p2 (transform-geom p1 c1 c2 f1)
                   p3 (transform-geom p1 s1 s2 f1)
                   p4 (transform-geom p1 t1 f1)
                   p2_false (transform-geom p1 c1 c2 f_false)
                   p3_false (transform-geom p1 s1 s2 f_false)
                   p4_false (transform-geom p1 t1 f_false)]
               (same-geom? p2 p3) => true
               (same-geom? p2 p4) => true
               (same-geom? p3 p4) => true
               (crs/get-srid p2_false) => 9999
               (crs/get-srid p3_false) => 9999
               (crs/get-srid p4_false) => 9999
               (same-geom? p2 p2_false) => false
               (same-geom? p3 p3_false) => false
               (same-geom? p4 p4_false) => false
               (same-geom? p2_false p3_false) => true
               (same-geom? p2_false p4_false) => true
               (same-geom? p3_false p4_false) => true))
       (fact "When passing one Transformable and one GeometryFactory, determine whether
              Transformable is target or source depending on if Geometry has SRID."
             (let [g1 (point 42.3601 -71.0589)
                   g2 (point -71.0589 42.3601 0)
                   g3 (transform-geom g1 3586)
                   f1 (crs/get-geometry-factory 4326)
                   f2 (crs/get-geometry-factory 3586)]
               (same-geom? (transform-geom g1 3586 f2) g3) => true
               (same-geom? (transform-geom g2 3586 f2) g3) => false
               (same-geom? (transform-geom (set-srid g2 4326) 3586 f2) g3) => true
               (same-geom? (transform-geom g2 3586 f2) g1) => false
               (same-geom? (set-srid (transform-geom g2 3586 f2) 4326) g1) => true))
       (fact "When passing a CoordinateTransform and a GeometryFactory, the GeometryFactory's SRID will be used."
             (let [c1 4326
                   c2 3586
                   f1 (crs/get-geometry-factory 3586)
                   f2 (crs/get-geometry-factory 4326)
                   t1 (crs/create-transform 4326 3586)
                   p1 (point 42.3601 -71.0589)
                   p2 (transform-geom p1 t1)
                   p3 (transform-geom p1 t1 f1)
                   p4 (transform-geom p1 t1 f2)]
               (crs/get-srid p1) => 4326
               (crs/get-srid p2) => 3586
               (crs/get-srid p3) => 3586
               (crs/get-srid p4) => 4326)))
