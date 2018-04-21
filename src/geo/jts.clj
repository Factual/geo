(ns geo.jts
  "Wrapper for the locationtech JTS spatial library. Constructors for points,
  coordinate sequences, rings, polygons, multipolygons, and so on."
  (:import (org.locationtech.jts.geom Coordinate
                                      Point
                                      LinearRing
                                      PrecisionModel
                                      Polygon
                                      MultiPolygon
                                      PrecisionModel
                                      GeometryFactory)))

(def ^PrecisionModel pm (PrecisionModel. PrecisionModel/FLOATING))

(def ^GeometryFactory gf
  (GeometryFactory. pm 4326))

(defn coordinate
  "Creates a Cooordinate."
  ([x y]
   (Coordinate. x y)))

(defn point
  "Creates a Point from a Coordinate, or an x,y pair."
  ([x y]
   (point (coordinate x y)))
  ([^Coordinate coordinate]
   (.createPoint gf coordinate)))

(defn coordinate-sequence
  "Given a list of Coordinates, generates a CoordinateSequence."
  [coordinates]
  (.. gf getCoordinateSequenceFactory create
    (into-array Coordinate coordinates)))

(defn wkt->coords-array
  [flat-coord-list]
  (->> flat-coord-list
       (partition 2)
       (map (partial apply coordinate))))

(defn linestring
  "Given a list of Coordinates, creates a LineString"
  [coordinates]
  (.createLineString gf (into-array Coordinate coordinates)))

(defn linestring-wkt
  "Makes a LineString from a WKT-style data structure: a flat sequence of
  coordinate pairs, e.g. [0 0, 1 0, 0 2, 0 0]"
  [coordinates]
  (-> coordinates wkt->coords-array linestring))

(defn coords
  [^org.locationtech.jts.geom.LineString linestring]
  (-> linestring .getCoordinateSequence .toCoordinateArray))

(defn coord
  [^org.locationtech.jts.geom.Point point]
  (.getCoordinate point))

(defn point-n
  "Get the point for a linestring at the specified index."
  [^org.locationtech.jts.geom.LineString linestring idx]
  (.getPointN linestring idx))

(defn segment-at-idx
  "LineSegment from a LineString's point at index to index + 1."
  [^org.locationtech.jts.geom.LineString linestring idx]
  (org.locationtech.jts.geom.LineSegment. (coord (point-n linestring idx))
                                          (coord (point-n linestring (inc idx)))))
(defn linear-ring
  "Given a list of Coordinates, creates a LinearRing."
  [coordinates]
  (.createLinearRing gf (into-array Coordinate coordinates)))

(defn linear-ring-wkt
  "Makes a LinearRing from a WKT-style data structure: a flat sequence of
  coordinate pairs, e.g. [0 0, 1 0, 0 2, 0 0]"
  [coordinates]
  (-> coordinates wkt->coords-array linear-ring))

(defn polygon
  "Given a LinearRing shell, and a list of LinearRing holes, generates a
  polygon."
  ([shell]
   (polygon shell nil))
  ([shell holes]
   (.createPolygon gf shell (into-array LinearRing holes))))

(defn polygon-wkt
  "Generates a polygon from a WKT-style data structure: a sequence of
  [outer-ring hole1 hole2 ...], where outer-ring and each hole is a flat list
  of coordinate pairs, e.g.

  [[0 0 10 0 10 10 0 0]
   [1 1  9 1  9  9 1 1]]"
  [rings]
  (let [rings (map linear-ring-wkt rings)]
    (polygon (first rings) (rest rings))))

(defn multi-polygon
  "Given a list of polygons, generates a MultiPolygon."
  [polygons]
  (.createMultiPolygon gf (into-array Polygon polygons)))

(defn multi-polygon-wkt
  "Creates a MultiPolygon from a WKT-style data structure, e.g. [[[0 0 1 0 2 2
  0 0]] [5 5 10 10 6 2]]"
  [wkt]
  (multi-polygon (map polygon-wkt wkt)))
