(ns geo.jts
  "Wrapper for the locationtech JTS spatial library. Constructors for points,
  coordinate sequences, rings, polygons, multipolygons, and so on."
  (:require [geo.crs :as crs :refer [Transformable]])
  (:import (org.locationtech.jts.geom Coordinate
                                      CoordinateSequence
                                      CoordinateSequenceFilter
                                      CoordinateXYZM
                                      Envelope
                                      Geometry
                                      GeometryCollection
                                      GeometryFactory
                                      Point
                                      LinearRing
                                      LineSegment
                                      LineString
                                      MultiPoint
                                      MultiLineString
                                      MultiPolygon
                                      Polygon
                                      PrecisionModel)
           (org.locationtech.proj4j CoordinateTransform ProjCoordinate)))

(def ^PrecisionModel pm crs/pm) ; Deprecated as of 3.0.2
(def ^GeometryFactory gf crs/get-geometry-factory) ; Deprecated as of 3.0.2
(def default-srid crs/default-srid) ; Deprecated as of 3.0.2
(def ^GeometryFactory gf-wgs84 crs/gf-wgs84); Deprecated as of 3.0.2
(def ^GeometryFactory get-factory crs/get-geometry-factory) ; Deprecated as of 3.0.2
(def get-srid crs/get-srid) ; Deprecated as of 3.0.2

(defn set-srid
  "Sets a geometry's SRID to a new value, and returns that geometry."
  [^Geometry geom srid]
  (.createGeometry (crs/get-geometry-factory srid) geom))

(defn coordinate
  "Creates a Coordinate."
  ([^double x ^double y]
   (Coordinate. x y))
  ([^double x ^double y ^double z]
   (Coordinate. x y z))
  ([^double x ^double y ^double z ^double m]
   (CoordinateXYZM. x y z m)))

(defn ^Point point
  "Creates a Point from a Coordinate, a lat/long, or an x,y pair with an SRID."
  ([^Coordinate coordinate]
   (.createPoint crs/gf-wgs84 coordinate))
  ([lat long]
   (point long lat crs/default-srid))
  ([x y srid]
   (.createPoint (crs/get-geometry-factory srid) ^Coordinate (coordinate x y))))

(defn ^"[Lorg.locationtech.jts.geom.Coordinate;" coord-array
  [coordinates]
  (into-array Coordinate coordinates))

(defn ^"[Lorg.locationtech.jts.geom.Point;" point-array
  [points]
  (into-array Point points))

(defn ^"[Lorg.locationtech.jts.geom.Geometry;" geom-array
  [geoms]
  (into-array Geometry geoms))

(defn ^"[Lorg.locationtech.jts.geom.LinearRing;" linear-ring-array
  [rings]
  (into-array LinearRing rings))

(defn ^"[Lorg.locationtech.jts.geom.LineString;" linestring-array
  [linestrings]
  (into-array LineString linestrings))

(defn ^"[Lorg.locationtech.jts.geom.Polygon;" polygon-array
  [polygons]
  (into-array Polygon polygons))

(defn ^MultiPoint multi-point
  "Given a list of points, generates a MultiPoint."
  [points]
  (.createMultiPoint (crs/get-geometry-factory (first points))
                     (point-array points)))

(defn ^CoordinateSequence coordinate-sequence
  "Given a list of Coordinates, generates a CoordinateSequence."
  [coordinates]
  (-> (.getCoordinateSequenceFactory crs/gf-wgs84)
      (.create (coord-array coordinates))))

(defn ^GeometryCollection geometry-collection
  "Given a list of Geometries, generates a GeometryCollection."
  [geometries]
  (.createGeometryCollection (crs/get-geometry-factory (first geometries))
                             (geom-array geometries)))

(defn geometries
  "Given a GeometryCollection, generate a sequence of Geometries"
  [^GeometryCollection c]
  (mapv (fn [n] (.getGeometryN c n)) (range (.getNumGeometries c))))

(defn wkt->coords-array
  [flat-coord-list]
  (->> flat-coord-list
       (partition 2)
       (map (partial apply coordinate))))

(defn ^LineString linestring
  "Given a list of Coordinates, creates a LineString. Allows an optional SRID argument at end."
  ([coordinates]
   (.createLineString crs/gf-wgs84 (coord-array coordinates)))
  ([coordinates srid]
   (.createLineString (crs/get-geometry-factory srid) (coord-array coordinates))))

(defn ^MultiLineString multi-linestring
  "Given a list of LineStrings, generates a MultiLineString."
  [linestrings]
  (.createMultiLineString (crs/get-geometry-factory (first linestrings))
                          (linestring-array linestrings)))

(defn linestring-wkt
  "Makes a LineString from a WKT-style data structure: a flat sequence of
  coordinate pairs, e.g. [0 0, 1 0, 0 2, 0 0]. Allows an optional SRID argument at end."
  ([coordinates]
   (-> coordinates wkt->coords-array linestring))
  ([coordinates srid]
   (-> coordinates wkt->coords-array (linestring srid))))

(defn multi-linestring-wkt
  "Creates a MultiLineString from a WKT-style data structure, e.g. [[0 0, 1 0, 0 2, 0 0] [0 -1 1 2]].
  Allows an optional SRID argument at end."
  ([wkt]
   (multi-linestring (map linestring-wkt wkt)))
  ([wkt srid]
   (multi-linestring (map #(linestring-wkt % srid) wkt))))

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

(defn line-segment
  "Given two Coordinates, creates a LineSegment."
  [^Coordinate c1 ^Coordinate c2]
  (LineSegment. c1 c2))

(defn segment-at-idx
  "LineSegment from a LineString's point at index to index + 1."
  [^LineString linestring idx]
  (line-segment (coord (point-n linestring idx))
                (coord (point-n linestring (inc idx)))))

(defn ^LinearRing linear-ring
  "Given a list of Coordinates, creates a LinearRing. Allows an optional SRID argument at end."
  ([coordinates]
   (.createLinearRing crs/gf-wgs84 (coord-array coordinates)))
  ([coordinates srid]
   (.createLinearRing (crs/get-geometry-factory srid) (coord-array coordinates))))

(defn linear-ring-wkt
  "Makes a LinearRing from a WKT-style data structure: a flat sequence of
  coordinate pairs, e.g. [0 0, 1 0, 0 2, 0 0]. Allows an optional SRID argument at end."
  ([coordinates]
   (-> coordinates wkt->coords-array linear-ring))
  ([coordinates srid]
   (-> coordinates wkt->coords-array (linear-ring srid))))

(defn ^Polygon polygon
  "Given a LinearRing shell, and a list of LinearRing holes, generates a
  polygon."
  ([shell]
   (polygon shell nil))
  ([^LinearRing shell holes]
   (.createPolygon (crs/get-geometry-factory shell) shell
                   (linear-ring-array holes))))

(defn polygon-wkt
  "Generates a polygon from a WKT-style data structure: a sequence of
  [outer-ring hole1 hole2 ...], where outer-ring and each hole is a flat list
  of coordinate pairs, e.g.

  [[0 0 10 0 10 10 0 0]
   [1 1  9 1  9  9 1 1]].

   Allows an optional SRID argument at end."
  ([rings]
   (polygon-wkt rings crs/default-srid))
  ([rings srid]
   (let [rings (map #(linear-ring-wkt % srid) rings)]
     (polygon (first rings) (into-array LinearRing (rest rings))))))

(defn ^MultiPolygon multi-polygon
  "Given a list of polygons, generates a MultiPolygon."
  [polygons]
  (.createMultiPolygon (crs/get-geometry-factory (first polygons))
                       (polygon-array polygons)))

; Deprecated since 3.0.2.
(defn polygons
  "Given a MultiPolygon, generate a sequence of Polygons.
  Deprecated in favor of more general geometries function."
  [^MultiPolygon m]
  (geometries m))

(defn multi-polygon-wkt
  "Creates a MultiPolygon from a WKT-style data structure, e.g. [[[0 0 1 0 2 2
  0 0]] [5 5 10 10 6 2]]. Allows an optional SRID argument at end."
  ([wkt]
   (multi-polygon (map polygon-wkt wkt)))
  ([wkt srid]
   (multi-polygon (map #(polygon-wkt % srid) wkt))))

(defn coordinates
  "Get a sequence of Coordinates from a Geometry"
  [^Geometry geom]
  (into [] (.getCoordinates geom)))

(defn same-srid?
  "Check if two Geometries have the same SRID. If both geometries have SRIDs of 0, will also return true."
  [^Geometry g1 ^Geometry g2]
  (= (crs/get-srid g1) (crs/get-srid g2)))

(defn same-coords?
  "Check if two Coordinates have the same number of dimensions and equal ordinates."
  [^Coordinate c1 ^Coordinate c2]
  (.equals2D c1 c2))

(defn same-geom?
  "Check if two geometries are topologically equal, with the same SRID.
  Two SRIDs of 0 are considered equal to each other."
  [^Geometry g1 ^Geometry g2]
  (and (same-srid? g1 g2)
       (.equalsTopo g1 g2)))

(defn- ^Coordinate transform-coord
  "Transforms a coordinate using a proj4j transform.
  Can either be specified with a transform argument or two projection arguments."
  ([^Coordinate coord ^CoordinateTransform transform]
   (-> (.transform transform
                   (ProjCoordinate. (.x coord) (.y coord) (.z coord))
                   (ProjCoordinate.))
       (#(coordinate (.x ^ProjCoordinate %) (.y ^ProjCoordinate %) (.z ^ProjCoordinate %)))))
  ([coord c1 c2]
   (if (= c1 c2) coord
       (transform-coord coord (crs/create-transform c1 c2)))))

(defn- transform-coord-seq-item
  "Transforms one item in a CoordinateSequence using a proj4j transform."
  [^CoordinateSequence cseq ^Integer i ^CoordinateTransform transform]
  (let [coordinate (.getCoordinate cseq i)
        transformed (transform-coord coordinate transform)]
    (.setOrdinate cseq i 0 (.x transformed))
    (.setOrdinate cseq i 1 (.y transformed))))

(defn- ^CoordinateSequenceFilter transform-coord-seq-filter
  "Implement JTS's CoordinateSequenceFilter, to be applied to a Geometry using tf and transform-geom."
  [transform]
  (reify CoordinateSequenceFilter
    (filter [_ seq i]
      (transform-coord-seq-item seq i transform))
    (isDone [_]
      false)
    (isGeometryChanged [_]
      true)))

(defn- tf
  "Transform a Geometry by applying CoordinateTransform to the Geometry.
  When the target CRS has an SRID, set the geometry's SRID to that."
  ([^Geometry g ^CoordinateTransform transform]
   (tf g transform (crs/get-target-crs transform)))
  ([^Geometry g ^CoordinateTransform transform ^GeometryFactory gf]
   (let [g (.copy g)]
     (.apply g (transform-coord-seq-filter transform))
     (set-srid g gf))))

(defn transform-geom
  "Transform a Geometry.
  When a single CoordinateTransform is passed, apply that transform to the Geometry. When the target CRS
  has an SRID, set the geometry's SRID to that. When a single Transformable target is passed, attempt to
  find the geometry's CRS to generate and apply a CoordinateTransform. When two CRSs are passed as
  arguments, generate a CoordinateTransform and apply accordingly. If the final argument is a
  GeometryFactory, use that in the transformation."
  ([g t]
   (if (instance? CoordinateTransform t)
         (tf g t)
         (transform-geom g t (crs/get-geometry-factory t))))
  ([g c1 c2]
   (cond (instance? CoordinateTransform c1)
         (tf g c1 (crs/get-geometry-factory c2))
         (and (satisfies? Transformable c1)
              (instance? GeometryFactory c2))
         (let [geom-srid (crs/get-srid g)]
           (assert (not= 0 geom-srid)
                   "Geometry must have a valid SRID to be transformed")
           (if (= (crs/get-srid c1) geom-srid)
             g
             (transform-geom g geom-srid c1 c2)))
         :else
         (transform-geom g (crs/create-transform c1 c2))))
  ([g c1 c2 geometry-factory]
   (transform-geom g (crs/create-transform c1 c2) geometry-factory)))

(defn ^Point centroid
  "Get the centroid of a JTS object."
  [^Geometry g]
  (.getCentroid g))

(defn intersection
  "Get the intersection of two geometries."
  [^Geometry g1 ^Geometry g2]
  (.intersection g1 g2))

(defn ^Envelope get-envelope-internal
  "Get a JTS envelope from a geometry."
  [^Geometry g]
  (.getEnvelopeInternal g))

(defn envelope
  "Create a JTS envelope from two coordinates."
  [c1 c2]
  (Envelope. c1 c2))

(defn subdivide
  "Subdivide a Geometry into quadrants around its centroid."
  [^Geometry g]
  (let [e (get-envelope-internal g)
        c (centroid g)
        c-x (.getX c)
        c-y (.getY c)
        min-x (.getMinX e)
        min-y (.getMinY e)
        max-x (.getMaxX e)
        max-y (.getMaxY e)
        gf (crs/get-geometry-factory g)
        make-quadrant (fn [c1 c2] (.toGeometry gf (envelope c1 c2)))
        q1 (make-quadrant (coord c) (coordinate max-x max-y))
        q2 (make-quadrant (coordinate min-x c-y) (coordinate c-x max-y))
        q3 (make-quadrant (coordinate min-x min-y) (coord c))
        q4 (make-quadrant (coordinate c-x min-y) (coordinate max-x c-y))]
    (map #(intersection g %) [q1 q2 q3 q4])))
