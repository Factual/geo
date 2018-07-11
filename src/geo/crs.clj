(ns geo.crs
  "Helper functions for identifying and manipulating Coordinate Reference Systems."
  (:require [clojure.string :refer [starts-with?]])
  (:import (org.osgeo.proj4j CoordinateTransform
                             CoordinateTransformFactory
                             CRSFactory)))

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

(defn proj4-string?
  "Check if input appears to be a proj4 string"
  [crs-str]
  (clojure.string/includes? crs-str "+proj="))

(defn- create-crs-int
  [^Integer c]
  (.createFromName crs-factory (srid->epsg-str c)))

(defn- create-crs-name
  [^String c]
  (.createFromName crs-factory c))

(defn- create-crs-parameters
  [^String c]
  (.createFromParameters crs-factory "" c))

(defn- create-crs
  "Create a CRS system. If given an integer, assume it is an EPSG code.
  If given a valid CRS name or proj4 string, use that as the CRS identifier."
  [c]
  (cond (int? c)
        (create-crs-int c)
        (crs-name? c)
        (create-crs-name c)
        (proj4-string? c)
        (create-crs-parameters c)))

(defn ^CoordinateTransform create-transform
  "Creates a proj4j transform between two projection systems.
  c1 or c2 can be:
   - integers (which will be interpreted as that EPSG)
   - a string identifier for types EPSG:XXXX, ESRI:XXXX, WORLD:XXXX, NAD83:XXXX, or NAD27:XXXX
   - proj4 string."
  [c1 c2]
  (.createTransform ctf-factory
                    (create-crs c1)
                    (create-crs c2)))
