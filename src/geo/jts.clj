(ns geo.jts
  "Wrapper for the locationtech JTS spatial library. Constructors for points,
  coordinate sequences, rings, polygons, multipolygons, and so on."
  (:require [geo.crs :as crs])
  (:import (org.locationtech.jts.geom Coordinate
                                      CoordinateSequenceFilter
                                      Geometry
                                      Point
                                      LinearRing
                                      LineSegment
                                      LineString
                                      PrecisionModel
                                      Polygon
                                      PrecisionModel
                                      GeometryFactory)
           (org.osgeo.proj4j CoordinateTransformFactory
                             CRSFactory
                             ProjCoordinate)))

(def ^PrecisionModel pm (PrecisionModel. PrecisionModel/FLOATING))

(defn gf
  "Creates a GeometryFactory for a given SRID."
  [srid]
  (GeometryFactory. pm srid))

(def ^GeometryFactory gf-wgs84
  (gf 4326))

(defn get-srid
  "Gets an integer SRID for a given geometry."
  [geom]
  (.getSRID geom))

(defn set-srid
  "Sets a geometry's SRID to a new value, and returns that geometry."
  [geom srid]
  (doto geom (.setSRID srid)))

(defn get-factory
  "Gets a GeometryFactory for a given geometry."
  [geom]
  (.getFactory geom))

(defn coordinate
  "Creates a Coordinate."
  ([x y]
   (Coordinate. x y))
  ([x y z]
   (Coordinate. x y z)))

(defn point
  "Creates a Point from a Coordinate, or an x,y pair. Allows an optional SRID argument at end."
  ([^Coordinate coordinate]
   (.createPoint gf coordinate))
  ([x y]
   (point (coordinate x y)))
  ([x y srid]
   (point (coordinate x y) srid)))

(defn coordinate-sequence
  "Given a list of Coordinates, generates a CoordinateSequence."
  [coordinates]
  (.. gf-wgs84 getCoordinateSequenceFactory create
      (into-array Coordinate coordinates)))

(defn wkt->coords-array
  [flat-coord-list]
  (->> flat-coord-list
       (partition 2)
       (map (partial apply coordinate))))

(defn linestring
  "Given a list of Coordinates, creates a LineString. Allows an optional SRID argument at end."
  ([coordinates]
   (.createLineString gf-wgs84 (into-array Coordinate coordinates)))
  ([coordinates srid]
   (.createLineString (gf srid) (into-array Coordinate coordinates))))

(defn linestring-wkt
  "Makes a LineString from a WKT-style data structure: a flat sequence of
  coordinate pairs, e.g. [0 0, 1 0, 0 2, 0 0]. Allows an optional SRID argument at end."
  ([coordinates]
   (-> coordinates wkt->coords-array linestring))
  ([coordinates srid]
   (-> coordinates wkt->coords-array (linestring srid))))

(defn coords
  [^LineString linestring]
  (-> linestring .getCoordinateSequence .toCoordinateArray))

(defn coord
  [^Point point]
  (.getCoordinate point))

(defn point-n
  "Get the point for a linestring at the specified index."
  [^LineString linestring idx]
  (.getPointN linestring idx))

(defn segment-at-idx
  "LineSegment from a LineString's point at index to index + 1."
  [^LineString linestring idx]
  (LineSegment. (coord (point-n linestring idx))
                (coord (point-n linestring (inc idx)))))

(defn linear-ring
  "Given a list of Coordinates, creates a LinearRing. Allows an optional SRID argument at end."
  ([coordinates]
   (.createLinearRing gf-wgs84 (into-array Coordinate coordinates)))
  ([coordinates srid]
   (.createLinearRing (gf srid) (into-array Coordinate coordinates))))

(defn linear-ring-wkt
  "Makes a LinearRing from a WKT-style data structure: a flat sequence of
  coordinate pairs, e.g. [0 0, 1 0, 0 2, 0 0]. Allows an optional SRID argument at end."
  ([coordinates]
   (-> coordinates wkt->coords-array linear-ring))
  ([coordinates srid]
   (-> coordinates wkt->coords-array (linear-ring srid))))

(defn polygon
  "Given a LinearRing shell, and a list of LinearRing holes, generates a
  polygon."
  ([shell]
   (polygon shell nil))
  ([shell holes]
   (.createPolygon (get-factory shell) shell (into-array LinearRing holes))))

(defn polygon-wkt
  "Generates a polygon from a WKT-style data structure: a sequence of
  [outer-ring hole1 hole2 ...], where outer-ring and each hole is a flat list
  of coordinate pairs, e.g.

  [[0 0 10 0 10 10 0 0]
   [1 1  9 1  9  9 1 1]].

   Allows an optional SRID argument at end."
  ([rings]
   (let [rings (map linear-ring-wkt rings)]
     (polygon (first rings) (into-array LinearRing (rest rings)))))
  ([rings srid]
   (let [rings (map #(linear-ring-wkt % srid) rings)]
     (polygon (first rings) (into-array LinearRing (rest rings))))))

(defn multi-polygon
  "Given a list of polygons, generates a MultiPolygon."
  [polygons]
  (.createMultiPolygon (get-factory (first polygons)) (into-array Polygon polygons)))

(defn multi-polygon-wkt
  "Creates a MultiPolygon from a WKT-style data structure, e.g. [[[0 0 1 0 2 2
  0 0]] [5 5 10 10 6 2]]. Allows an optional SRID argument at end."
  ([wkt]
   (multi-polygon (map polygon-wkt wkt)))
  ([wkt srid]
   (multi-polygon (map #(polygon-wkt % srid) wkt))))

(defn- coord-dimension-check
  "Return 3 if x, y, and z dimensions are valid. Return 2 if only x and y are valid."
  [coord]
  (let [x (not (Double/isNaN (.x coord)))
        y (not (Double/isNaN (.y coord)))
        z (not (Double/isNaN (.z coord)))]
    (cond (and x y z)
          3
          (and x y)
          2)))

(defn coordinates
  "Get a sequence of Coordinates from a Geometry"
  [^Geometry geom]
  (into [] (.getCoordinates geom)))

(defn geom-dimension-check
  "In a Geometry, return 3 if any Coordinate has a Z.
  Otherwise return 2 if any Coordinate has a Y."
  [geom]
  (let [coords (coordinates geom)
        dimensions (into [] (map coord-dimension-check coords))]
    (cond (some #{3} dimensions)
          3
          (some #{2} dimensions)
          2)))

(defn same-srid?
  "Check if two Geometries have the same SRID."
  [^Geometry g1 ^Geometry g2]
  (and (= (get-srid g1) (get-srid g2))
       (not= (get-srid g1) 0)))

(defn same-coords?
  "Check if two Coordinates have the same number of dimensions and equal ordinates."
  [^Coordinate c1 ^Coordinate c2]
  (let [d1 (coord-dimension-check c1)
        d2 (coord-dimension-check c2)]
    (and (= d1 d2)
         (cond (= d1 3)
               (.equals3D c1 c2)
               (= d1 2)
               (.equals2D c1 c2)))))

(defn same-geom?
  "Check if two geometries are topologically equal, with the same SRID."
  [^Geometry g1 ^Geometry g2]
  (and (same-srid? g1 g2)
       (.equalsTopo g1 g2)))

(defn- transform-coord
  "Transforms a coordinate using a proj4j transform.
  Can either be specified with a transform argument or two projection arguments."
  ([coord transform]
   (-> (.transform transform
                   (ProjCoordinate. (.x coord) (.y coord) (.z coord))
                   (ProjCoordinate.))
       (#(coordinate (.x %) (.y %) (.z %)))))
  ([coord c1 c2]
   (if (= c1 c2) coord
       (transform-coord coord (crs/create-transform c1 c2)))))

(defn- transform-coord-seq-item
  "Transforms one item in a CoordinateSequence using a proj4j transform."
  [cseq i transform]
  (let [coordinate (.getCoordinate cseq i)
        transformed (transform-coord coordinate transform)]
    (.setOrdinate cseq i 0 (.x transformed))
    (.setOrdinate cseq i 1 (.y transformed))))

(defn- transform-coord-seq-filter
  "Implement JTS's CoordinateSequenceFilter, to be applied to a Geometry using transform-geom."
  [transform]
  (reify CoordinateSequenceFilter
    (filter [_ seq i]
      (transform-coord-seq-item seq i transform))
    (isDone [_]
      false)
    (isGeometryChanged [_]
      true)))

(defn- tf-set-srid
  "When the final projection for a tf is an SRID or EPSG, set the Geometry's SRID."
  [g c]
  (cond (int? c)
        (set-srid g c)
        (crs/epsg? c)
        (set-srid g (crs/epsg->srid c))
        :else
        g))

(defn- tf
  "Transform a Geometry from one CRS to another.
  When the target transformation is an EPSG code, set the Geometry's SRID to that integer."
  [g c1 c2]
  (let [tcsf (transform-coord-seq-filter
               (crs/create-transform c1 c2))]
    (.apply g tcsf)
    (tf-set-srid g c2)))

(defn transform-geom
  "Transform a Geometry using a proj4j transform, if needed. Returns a new Geometry if a transform occurs.
  When only one CRS is given, get the CRS of the existing geometry.
  When two are given, force the transformation to occur between those two systems."
  ([g crs]
   (let [geom-srid (get-srid g)]
     (cond (= geom-srid 0)
           (Exception. "Geometry does not have an SRID")
           (or (= geom-srid crs)
               (= (crs/srid->epsg geom-srid) crs))
           g
           :else
           (transform-geom g geom-srid crs))))
  ([g crs1 crs2]
   (-> (if (= crs1 crs2)
         (tf-set-srid g crs2)
         (tf (.clone g) crs1 crs2)))))
