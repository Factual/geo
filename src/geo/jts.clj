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

(def ^PrecisionModel pm ; Deprecated as of 3.1.0
  "Deprecated as of 3.1.0, in favor of geo.crs/pm." crs/pm)
(def ^GeometryFactory gf ; Deprecated as of 3.1.0
  "Deprecated as of 3.1.0, in favor of geo.crs/get-geometry-factory."
  crs/get-geometry-factory)
(def default-srid ; Deprecated as of 3.1.0
  "Deprecated as of 3.1.0, in favor of geo.crs/default-srid."
  crs/default-srid)
(def ^GeometryFactory gf-wgs84 ; Deprecated as of 3.1.0
  "Deprecated as of 3.1.0, in favor of geo.crs/gf-wgs84."
  crs/gf-wgs84)
(def ^GeometryFactory get-factory ; Deprecated as of 3.1.0
  "Deprecated as of 3.1.0, in favor of geo.crs/get-geometry-factory."
  crs/get-geometry-factory)
(def get-srid ; Deprecated as of 3.1.0
  "Deprecated as of 3.1.0, in favor of geo.crs/get-srid."
  crs/get-srid)

(defn set-srid
  "Sets a geometry's SRID to a new value, and returns that geometry."
  [^Geometry geom srid]
  (if (= (crs/get-srid geom) (crs/get-srid srid))
      geom
      (.createGeometry (crs/get-geometry-factory srid) geom)))

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

; Deprecated since 3.1.0.
(defn polygons
  "Given a MultiPolygon, generate a sequence of Polygons.
  Deprecated since 3.1.0, in favor of geo.jts/geometries."
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

(defn- geom-srid?
  "Check if a Geometry has a valid SRID."
  [g]
  (let [geom-srid (crs/get-srid g)]
    (and (not= 0 geom-srid)
         (not (nil? geom-srid)))))

(defn- tf
  "Transform a Geometry by applying CoordinateTransform to the Geometry.
  When the target CRS has an SRID, set the geometry's SRID to that."
  [^Geometry g ^CoordinateTransform transform ^GeometryFactory gf]
  (let [g (.copy g)]
    (.apply g (transform-coord-seq-filter transform))
    (set-srid g gf)))

(defn transform-geom
  "Transform a Geometry.
  When a Geometry and CoordinateTransform is passed, apply that transform to the Geometry.

  When a Geometry and Transformable is passed, attempt to transform the Geometry to that target.

  When a Geometry, CoordinateTransform, and Transformable are passed, apply the CoordinateTransform
  and get a GeometryFactory from the transformable.

  When a Geometry, Transformable, and GeometryFactory are passed, and the Geometry has an SRID,
  take the Transformable as a target CRS, create a CoordinateTransform,
  and use the GeometryFactory for a target SRID.

  When a Geometry, Transformable, and GeometryFactory are passed, and the Geometry has no SRID,
  take the Transformable as a source CRS and the GeometryFactory as a target CRS,
  create a corresponding Transform, and use the GeometryFactory for a target SRID.

  When a Geometry, Transformable, and Transformable are passed, create a CoordinateTransform and
  get a GeometryFactory from the target Transformable.

  When a Geometry, Transformable, Transformable, and GeometryFactory are all passed, create a
  CoordinateTransform from the two Transformables and use the supplied GeometryFactory for the
  target SRID."
  ([g t]
   ;; t is either a Transform or a Transformable
   (if (instance? CoordinateTransform t)
         ; If t is CoordinateTransform
         (tf g t (crs/get-target-crs t))
         ; If t is Transformable, make sure that g has a source CRS
         (do
           (assert (geom-srid? g)
                   "Geometry must have a valid source SRID to generate a transform to a target.")
           (transform-geom g t (crs/get-geometry-factory t)))))
  ([g c1 c2]
   ;; c1 is either a CoordinateTransform or Transformable
   ;; c2 is either a Transformable or a GeometryFactory
   (cond
     ;; If c1 is a CoordinateTransform, assume c2 acts as a GeometryFactory
     (instance? CoordinateTransform c1)
     (tf g c1 (crs/get-geometry-factory c2))
     ;; If c1 is Transformable, c2 is GeometryFactory, and g has SRID,
     ;; take c1 as a target CRS, create the relevant Transform, and apply the Factory
     ;; as the target SRID.
     ;; If c1 is Transformable, c2 is GeometryFactory, and g does not have SRID,
     ;; then take c1 as the source CRS and get the target CRS from the factory.
     (and (satisfies? Transformable c1)
          (instance? GeometryFactory c2))
       (if (geom-srid? g)
         (transform-geom g (crs/get-srid g) c1 c2)
         (transform-geom g c1 (crs/get-srid c2) c2))
   ;; Otherwise, c1 and c2 are both Transformable, and create transform
   :else
   (transform-geom g (crs/create-transform c1 c2) (crs/get-geometry-factory c2))))
  ([g c1 c2 geometry-factory]
   ;; When c1, c2, and geometry-factory are all provided,
   ;; create a transform and use the supplied factory
   (if (= (crs/get-srid c1) (crs/get-srid c2))
     (set-srid g geometry-factory)
     (transform-geom g (crs/create-transform c1 c2) geometry-factory))))

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
