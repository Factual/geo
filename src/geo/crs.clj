(ns geo.crs
  "Helper functions for identifying and manipulating Coordinate Reference Systems."
  (:import (org.locationtech.proj4j CoordinateReferenceSystem
                                    CoordinateTransform
                                    CoordinateTransformFactory
                                    CRSFactory)
           (org.locationtech.jts.geom Geometry
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
  (.createFromName crs-factory (srid->epsg-str c)))

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

(defn get-source-crs
  "Get the source coordinate reference system of a transform."
  [^CoordinateTransform t]
  (.getSourceCRS t))

(defn get-target-crs
  "Get the source coordinate reference system of a transform."
  [^CoordinateTransform t]
  (.getTargetCRS t))

(defprotocol Transformable
  (^CoordinateReferenceSystem create-crs [this]
   "Create a CRS system. If given an integer or long, assume it is an EPSG code.
    If given a valid CRS name or proj4 string, use that as the CRS identifier.
    If given a proj4j CoordinateReferenceSystem, return that.")
  (^GeometryFactory get-geometry-factory [this]
   "Creates a CoordinateFactory or a GeometryFactory.")
  (^Integer get-srid [this]
   "Attempt to get the SRID for a CRS identifier. If unable, return 0."))

(extend-protocol Transformable
  Long
  (create-crs [this] (create-crs-number this))
  (get-geometry-factory [this] (get-geometry-factory (get-srid this)))
  (get-srid [this] (int this))

  Integer
  (create-crs [this] (create-crs-number this))
  (get-geometry-factory [this] (GeometryFactory. pm this))
  (get-srid [this] this)

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

  CoordinateReferenceSystem
  (create-crs [this] this)
  (get-geometry-factory [this] (get-geometry-factory (get-srid this)))
  (get-srid [this] (get-srid (get-name this)))

  Geometry
  (create-crs [this] (create-crs (get-srid this)))
  (get-geometry-factory [this] (.getFactory this))
  (get-srid [this] (.getSRID this))

  GeometryFactory
  (create-crs [this] (create-crs (get-srid this)))
  (get-geometry-factory [this] this)
  (get-srid [this] (.getSRID this)))

(def default-srid (int 4326))
(def ^GeometryFactory gf-wgs84 (get-geometry-factory default-srid))

(defn create-transform
  [c1 c2]
  "Creates a proj4j transform between two projection systems.
  c1 or c2 can be:
   - a long (which will be interpreted as that EPSG)
   - an integer (which will be interpreted as that EPSG)
   - a string identifier for types EPSG:XXXX, ESRI:XXXX, WORLD:XXXX, NAD83:XXXX, or NAD27:XXXX
   - a proj4 string, or
   - a proj4j CoordinateReferenceSystem"
  (.createTransform ctf-factory (create-crs c1) (create-crs c2)))

