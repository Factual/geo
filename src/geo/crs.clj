(ns geo.crs
  "Helper functions for identifying and manipulating
  Coordinate Reference Systems."
  (:import (org.locationtech.proj4j CoordinateReferenceSystem
                                    CoordinateTransform
                                    CoordinateTransformFactory
                                    CRSFactory
                                    ProjCoordinate)
           (org.locationtech.jts.geom Coordinate
                                      CoordinateSequence
                                      CoordinateSequenceFilter
                                      Geometry
                                      GeometryFactory
                                      PrecisionModel)))

(defn starts-with? [^String string prefix]
  (.startsWith string prefix))
(defn includes? [^String string substring]
  (.contains string substring))

(def epsg-str? (partial re-matches #"EPSG:(\d+)"))
(def srid->epsg-str
  "Convert an SRID integer to EPSG string."
  (partial str "EPSG:"))

(defn epsg-str->srid
  "Converts EPSG string to SRID, if possible."
  [epsg]
  (let [match (epsg-str? epsg)]
    (assert match "Must be a valid EPSG string")
    (Integer/parseInt (last match))))

(def ^PrecisionModel pm (PrecisionModel. PrecisionModel/FLOATING))

(def ^CoordinateTransformFactory ctf-factory
  (CoordinateTransformFactory.))

(def ^CRSFactory crs-factory
  (CRSFactory.))

(def valid-crs-prefixes ["EPSG" "ESRI" "NAD83" "NAD27" "WORLD"])

(defn crs-name?
  "Check if input is a valid CRS name accepted by proj4j.

  Accepted CRS names are in the forms:
  EPSG:xxxx, ESRI:xxxx, NAD83:xxxx, NAD27:xxx, or WORLD:xxxx."
  [crs-str]
  (some (fn [prefix] (starts-with? crs-str (str prefix ":")))
        valid-crs-prefixes))

(defn proj4-str?
  "Check if input appears to be a proj4 string"
  [crs-str]
  (includes? crs-str "+proj="))

(defn- create-crs-number
  [c]
  (let [valid? (not= c 0)]
    (assert valid? "Cannot create CRS with EPSG 0")
    (.createFromName crs-factory (srid->epsg-str c))))

(defn- create-crs-name
  [c]
  (.createFromName crs-factory c))

(defn- create-crs-parameters
  [^String c]
  (.createFromParameters crs-factory "" c))

(defn get-name
  "Get the name of a coordinate reference system."
  [^CoordinateReferenceSystem c]
  (.getName c))

(defn get-parameters
  "Get the proj parameters from an existing coordinate reference system."
  [^CoordinateReferenceSystem c]
  (.getParameters c))

(defn get-parameter-string
  "Get the proj string from an existing coordinate reference system."
  [^CoordinateReferenceSystem c]
  (.getParameterString c))

(defn ^CoordinateReferenceSystem get-source-crs
  "Get the source coordinate reference system of a transform."
  [^CoordinateTransform t]
  (.getSourceCRS t))

(defn ^CoordinateReferenceSystem get-target-crs
  "Get the source coordinate reference system of a transform."
  [^CoordinateTransform t]
  (.getTargetCRS t))

(defprotocol Transformable
  (^CoordinateReferenceSystem create-crs [this]
   "Create a CRS system. If given an integer or long, assume it is an EPSG code.
    If given a valid CRS name or proj4 string, use that as the CRS identifier.
    If given a proj4j CoordinateReferenceSystem, return that.")
  (^GeometryFactory get-geometry-factory [this]
   "Creates a JTS GeometryFactory from a given CRS identifier.")
  (^Integer get-srid [this]
   "Attempt to get the SRID for a CRS identifier. If unable, return 0.")
  (^CoordinateTransform create-transform [this] [this tgt])
  (^Geometry ^:private transform-helper [this g] [this g c] [this g c gf]
   "An internal helper to coordinate logic of jts/transform-geom."))

(defn set-srid
  "Sets a geometry's SRID to a new value, and returns that geometry."
  [^Geometry geom srid]
  (if (= (get-srid geom) (get-srid srid))
      geom
      (.createGeometry (get-geometry-factory srid) geom)))

(defn- ^Coordinate transform-coord
  "Transforms a coordinate using a proj4j transform.
  Can either be specified with a transform argument
  or two projection arguments."
  ([^Coordinate coord ^CoordinateTransform transform]
   (-> (.transform transform
                   (ProjCoordinate. (.x coord) (.y coord) (.z coord))
                   (ProjCoordinate.))
       (#(Coordinate. (.x ^ProjCoordinate %)
                      (.y ^ProjCoordinate %)
                      (.z ^ProjCoordinate %)))))
  ([coord c1 c2]
   (if (= c1 c2) coord
       (transform-coord coord (create-transform c1 c2)))))

(defn- transform-coord-seq-item
  "Transforms one item in a CoordinateSequence using a proj4j transform."
  [^CoordinateSequence cseq ^Integer i ^CoordinateTransform transform]
  (let [coordinate (.getCoordinate cseq i)
        transformed (transform-coord coordinate transform)]
    (.setOrdinate cseq i 0 (.x transformed))
    (.setOrdinate cseq i 1 (.y transformed))))

(defn- ^CoordinateSequenceFilter transform-coord-seq-filter
  "Implement JTS's CoordinateSequenceFilter, to be applied to a
  Geometry using tf and transform-geom."
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
  [^Geometry g]
  (let [geom-srid (get-srid g)]
    (and (not= 0 geom-srid)
         (not (nil? geom-srid)))))

(defn- tf
  "Transform a Geometry by applying CoordinateTransform to the Geometry.
  When the target CRS has an SRID, set the geometry's SRID to that."
  [^Geometry g ^CoordinateTransform transform ^GeometryFactory gf]
  (let [g (.copy g)]
    (.apply g (transform-coord-seq-filter transform))
    (set-srid g gf)))


(extend-protocol Transformable
  Long
  (create-crs [this] (create-crs-number this))
  (get-geometry-factory [this] (get-geometry-factory (get-srid this)))
  (get-srid [this] (int this))
  (create-transform [this tgt]
    (create-transform (create-crs this) (create-crs tgt)))
  (transform-helper
    ([this g] (transform-helper (get-srid this) g))
    ([this g c] (transform-helper (get-srid this) g c))
    ([this g c1 f] (transform-helper (get-srid this) g c1 f)))

  Integer
  (create-crs [this] (create-crs-number this))
  (get-geometry-factory [this] (GeometryFactory. pm this))
  (get-srid [this] this)
  (create-transform [this tgt]
    (create-transform (create-crs this) (create-crs tgt)))
  (transform-helper
    ([this g]
     (transform-helper (get-geometry-factory this) g (create-transform g this)))
    ([this g c]
     (if (geom-srid? g)
       (transform-helper (get-geometry-factory c) g (create-transform g c))
       (transform-helper (get-geometry-factory c) g (create-transform this c))))
    ([this g c2 f]
     (transform-helper (get-geometry-factory f) g (create-transform this c2))))

  String
  (create-crs [this] (cond (crs-name? this)
                           (create-crs-name this)
                           (proj4-str? this)
                           (create-crs-parameters this)))
  (get-geometry-factory [this] (get-geometry-factory (get-srid this)))
  (get-srid [this] (let [epsg? (epsg-str? this)]
                     (if epsg?
                       (read-string (last epsg?))
                       0)))
  (create-transform [this tgt]
    (create-transform (create-crs this) (create-crs tgt)))
  (transform-helper
    ([this g] (transform-helper (create-crs this) g))
    ([this g c] (transform-helper (create-crs this) g c))
    ([this g c1 f] (transform-helper (create-crs this) g c1 f)))

  CoordinateReferenceSystem
  (create-crs [this] this)
  (get-geometry-factory [this] (get-geometry-factory (get-srid this)))
  (get-srid [this] (get-srid (get-name this)))
  (create-transform [this tgt]
    (.createTransform ctf-factory this (create-crs tgt)))
  (transform-helper
    ([this g]
     (transform-helper
      (get-geometry-factory this) g (create-transform (create-crs g) this)))
    ([this g c2]
     (transform-helper
      (get-geometry-factory c2) g (create-transform this (create-crs c2))))
    ([this g c2 f]
     (transform-helper (get-geometry-factory f) g (create-transform this c2))))

  Geometry
  (create-crs [this] (create-crs (get-srid this)))
  (get-geometry-factory [this] (.getFactory this))
  (get-srid [this] (.getSRID this))
  (create-transform [this tgt]
    (create-transform (create-crs this) (create-crs tgt)))
  (transform-helper
    ([this t]
     (do (assert (geom-srid? this)
                 "Geometry must have a valid SRID to generate a transform.")
         (transform-helper (get-geometry-factory t) this t)))
    ([this c1 c2 gf]
     (transform-helper
      (get-geometry-factory gf) this (create-transform c1 c2))))

  CoordinateTransform
  (create-transform [this] this)
  (get-geometry-factory [this] (get-geometry-factory (get-target-crs this)))
  (transform-helper
    ([this g]
     (transform-helper (get-geometry-factory this) g this))
    ([this g factory]
     (transform-helper (get-geometry-factory factory) g this)))

  GeometryFactory
  (create-crs [this] (create-crs (get-srid this)))
  (get-geometry-factory [this] this)
  (get-srid [this] (.getSRID this))
  (create-transform [this tgt]
    (create-transform (create-crs this) (create-crs tgt)))
  (transform-helper
    ([this g]
     (transform-helper this g (create-transform g this)))
    ([this g t]
     (if (instance? CoordinateTransform t)
       ;; Base case: GeometryFactory, Geometry, CoordinateTransform
       (if (.equals (get-source-crs t)
                    (get-target-crs t))
         (set-srid g this)
         (tf g t this))
       (transform-helper t g this)))
    ([this g c1 c2]
     (transform-helper this g (create-transform c1 c2)))))

(def default-srid (int 4326))
(def ^GeometryFactory gf-wgs84 (get-geometry-factory default-srid))

(defn transform-geom
  "Transforms a Geometry to a different Coordinate Reference System.
   Takes a Geometry as a first argument, and either one, two, or three
   Transformables as additional arguments. When the second argument is
   a GeometryFactory, use that factory to construct the new Geometry.
   When only a target CRS is provided, use the Geometry's internal
   SRID as the source CRS.

   The most efficient way to call this is with its base case of
   (transform-geom Geometry GeometryFactory CoordinateTransform).

   All other argument calls to this function ultimately reduce
   down to that base case."
  ([g t]
   (transform-helper t g))
  ([g c1 c2]
   (transform-helper c1 g c2))
  ([g c1 c2 geometry-factory]
   (transform-helper c1 g c2 geometry-factory)))
