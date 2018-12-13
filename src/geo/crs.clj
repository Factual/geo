(ns geo.crs
  "Helper functions for identifying and manipulating Coordinate Reference Systems."
  (:import (org.locationtech.proj4j CoordinateReferenceSystem
                                    CoordinateTransformFactory
                                    CRSFactory)))

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

(defprotocol Transformable
  (create-crs [this] "Create a CRS system. If given an integer or long, assume it is an EPSG code.
                      If given a valid CRS name or proj4 string, use that as the CRS identifier.
                      If given a proj4j CoordinateReferenceSystem, return that.")
  (get-srid [this]   "Attempt to get the SRID for a CRS identifier. If unable, return 0."))

(extend-protocol Transformable
  Long
  (create-crs [this] (create-crs-number this))
  (get-srid [this] this)

  Integer
  (create-crs [this] (create-crs-number this))
  (get-srid [this] this)

  String
  (create-crs [this] (cond (crs-name? this)
                           (create-crs-name this)
                           (proj4-str? this)
                           (create-crs-parameters this)))
  (get-srid [this] (let [epsg? (epsg-str? this)]
                     (if epsg?
                       (read-string (last epsg?))
                       0)))

  CoordinateReferenceSystem
  (create-crs [this] this)
  (get-srid [this] (get-srid (.getName this))))

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

