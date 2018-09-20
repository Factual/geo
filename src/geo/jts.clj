(ns geo.jts
  "Wrapper for the locationtech JTS spatial library. Constructors for points,
  coordinate sequences, rings, polygons, multipolygons, and so on."
  (:require [geo.crs :as crs])
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
                                      MultiPolygon
                                      Polygon
                                      PrecisionModel)
           (org.osgeo.proj4j CoordinateTransform ProjCoordinate)))

(def ^PrecisionModel pm (PrecisionModel. PrecisionModel/FLOATING))

(defn ^GeometryFactory gf
  "Creates a GeometryFactory for a given SRID."
  [^Integer srid]
  (GeometryFactory. pm srid))

(def default-srid 4326)

(def ^GeometryFactory gf-wgs84 (gf default-srid))

(defn get-srid
  "Gets an integer SRID for a given geometry."
  [^Geometry geom]
  (.getSRID geom))

(defn set-srid
  "Sets a geometry's SRID to a new value, and returns that geometry."
  [^Geometry geom ^Integer srid]
  (doto geom (.setSRID srid)))

(defn ^GeometryFactory get-factory
  "Gets a GeometryFactory for a given geometry."
  [^Geometry geom]
  (.getFactory geom))

(defn coordinate
  "Creates a Coordinate."
  ([^double x ^double y]
   (Coordinate. x y))
  ([^double x ^double y ^double z]
   (Coordinate. x y z))
  ([^double x ^double y ^double z ^double m]
   (CoordinateXYZM. x y z m)))

(defn point
  "Creates a Point from a Coordinate, a lat/long, or an x,y pair with an SRID."
  ([^Coordinate coordinate]
   (.createPoint gf-wgs84 coordinate))
  ([lat long]
   (point long lat default-srid))
  ([x y srid]
   (.createPoint (gf srid) ^Coordinate (coordinate x y))))

(defn ^"[Lorg.locationtech.jts.geom.Coordinate;" coord-array
  [coordinates]
  (into-array Coordinate coordinates))

(defn ^"[Lorg.locationtech.jts.geom.Geometry;" geom-array
  [geoms]
  (into-array Geometry geoms))

(defn ^"[Lorg.locationtech.jts.geom.LinearRing;" linear-ring-array
  [rings]
  (into-array LinearRing rings))

(defn ^"[Lorg.locationtech.jts.geom.Polygon;" polygon-array
  [polygons]
  (into-array Polygon polygons))

(defn ^CoordinateSequence coordinate-sequence
  "Given a list of Coordinates, generates a CoordinateSequence."
  [coordinates]
  (-> (.getCoordinateSequenceFactory gf-wgs84)
      (.create (coord-array coordinates))))

(defn ^GeometryCollection geometry-collection
  "Given a list of Geometries, generates a GeometryCollection."
  [geometries]
  (-> (get-factory (first geometries))
      (.createGeometryCollection (geom-array geometries))))

(defn geometries
  "Given a GeometryCollection, generate a sequence of Geometries"
  [^GeometryCollection c]
  (let [n (.getNumGeometries c)
        srid (get-srid c)
        geom-n (fn [^GeometryCollection c ^Integer n]
                 (-> c
                     (.getGeometryN n)
                     (set-srid srid)))]
    (mapv #(geom-n c %) (range n))))

(defn wkt->coords-array
  [flat-coord-list]
  (->> flat-coord-list
       (partition 2)
       (map (partial apply coordinate))))

(defn linestring
  "Given a list of Coordinates, creates a LineString. Allows an optional SRID argument at end."
  ([coordinates]
   (.createLineString gf-wgs84 (coord-array coordinates)))
  ([coordinates srid]
   (.createLineString (gf srid) (coord-array coordinates))))

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

(defn ^LinearRing linear-ring
  "Given a list of Coordinates, creates a LinearRing. Allows an optional SRID argument at end."
  ([coordinates]
   (.createLinearRing gf-wgs84 (coord-array coordinates)))
  ([coordinates srid]
   (.createLinearRing (gf srid) (coord-array coordinates))))

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
   (.createPolygon (get-factory shell) shell
                   (linear-ring-array holes))))

(defn polygon-wkt
  "Generates a polygon from a WKT-style data structure: a sequence of
  [outer-ring hole1 hole2 ...], where outer-ring and each hole is a flat list
  of coordinate pairs, e.g.

  [[0 0 10 0 10 10 0 0]
   [1 1  9 1  9  9 1 1]].

   Allows an optional SRID argument at end."
  ([rings]
   (polygon-wkt rings default-srid))
  ([rings srid]
   (let [rings (map #(linear-ring-wkt % srid) rings)]
     (polygon (first rings) (into-array LinearRing (rest rings))))))

(defn multi-polygon
  "Given a list of polygons, generates a MultiPolygon."
  [polygons]
  (let [f (first polygons)
        srid (get-srid f)]
       (-> (.createMultiPolygon (get-factory f)
                                (polygon-array polygons))
           (set-srid srid))))

(defn polygons
  "Given a MultiPolygon, generate a sequence of Polygons"
  [^MultiPolygon m]
  (let [n (.getNumGeometries m)
        srid (get-srid m)
        geom-n (fn [^MultiPolygon m ^Integer n]
                 (-> m
                     (.getGeometryN n)
                     (set-srid srid)))]
    (mapv #(geom-n m %) (range n))))

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
  (= (get-srid g1) (get-srid g2)))

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
  (cond (integer? c) (set-srid g c)
        (crs/epsg-str? c) (set-srid g (crs/epsg-str->srid c))
        :else g))

(defn- tf
  "Transform a Geometry from one CRS to another.
  When the target transformation is an EPSG code, set the Geometry's SRID to that integer."
  [^Geometry g c1 c2]
  (let [tcsf (transform-coord-seq-filter (crs/create-transform c1 c2))]
    (.apply g tcsf)
    (tf-set-srid g c2)))

(defn ^Geometry transform-geom
  "Transform a Geometry using a proj4j transform, if needed. Returns a new Geometry if a transform occurs.
  When only one CRS is given, get the CRS of the existing geometry.
  When two are given, force the transformation to occur between those two systems."
  ([g crs]
   (let [geom-srid (get-srid g)]
     (assert (not= 0 geom-srid) "Geometry must have a valid SRID to be transformed")
     (if (or (= geom-srid crs) (= (crs/srid->epsg-str geom-srid) crs))
       g
       (transform-geom g geom-srid crs))))
  ([^Geometry g crs1 crs2]
   (if (= crs1 crs2)
     (tf-set-srid g crs2)
     (tf (.copy g) crs1 crs2))))

(defn ^Point centroid
  "Get the centroid of a JTS object."
  [^Geometry g]
  (let [srid (get-srid g)]
       (set-srid (.getCentroid g) srid)))

(defn intersection
  "Get the intersection of two geometries."
  [^Geometry g1 ^Geometry g2]
  (let [srid (get-srid g1)]
    (set-srid (.intersection g1 g2) srid)))

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
        gf (get-factory g)
        make-quadrant (fn [c1 c2] (.toGeometry gf (envelope c1 c2)))
        q1 (make-quadrant (coord c) (coordinate max-x max-y))
        q2 (make-quadrant (coordinate min-x c-y) (coordinate c-x max-y))
        q3 (make-quadrant (coordinate min-x min-y) (coord c))
        q4 (make-quadrant (coordinate c-x min-y) (coordinate max-x c-y))]
    (map #(intersection g %) [q1 q2 q3 q4])))
