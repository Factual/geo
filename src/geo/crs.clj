(ns geo.crs
  "Helper functions for identifying and manipulating Coordinate Reference Systems."
  (:require [clojure.string :refer [join]])
  (:import (org.osgeo.proj4j CoordinateTransformFactory
                             CRSFactory
                             ProjCoordinate)))

(defn epsg?
  [s]
  (try (and (= (subs s 0 4) "EPSG")
            (int? (read-string (subs s 5))))
       (catch Exception _
         false)))

(defn srid->epsg
  "Converts SRID integer to EPSG string."
  [srid]
  (join ["EPSG:" srid]))

(defn epsg->srid
  "Converts EPSG string to SRID, if possible."
  [epsg]
  (cond (int? epsg)
        epsg
        (epsg? epsg)
        (read-string (subs epsg 5))))

(def ^CoordinateTransformFactory ctf-factory
  (CoordinateTransformFactory.))

(def ^CRSFactory crs-factory
  (CRSFactory.))

(defn crs-name?
  "Check if input is a valid CRS name"
  [c]
  (try (or (= (subs c 0 5) "EPSG:")
           (= (subs c 0 5) "ESRI:")
           (= (subs c 0 5) "NA83:")
           (= (subs c 0 6) "WORLD:")
           (= (subs c 0 6) "NAD27:"))
       (catch Exception _
         false)))

(defn proj4-string?
  "Check if input appears to be a proj4 string"
  [c]
  (try (.contains c "+proj=")
       (catch Exception _
         false)))

(defn- create-crs
  "Create a CRS system. If given an integer, assume it is an EPSG code.
  If given a valid CRS name or proj4 string, use that as the CRS identifier."
  [c]
  (cond (int? c)
        (.createFromName crs-factory (srid->epsg c))
        (crs-name? c)
        (.createFromName crs-factory c)
        (proj4-string? c)
        (.createFromParameters crs-factory "" c)))

(defn create-transform
  "Creates a proj4j transform between two projection systems.
  c1 or c2 can be:
   integers (which will be interpreted as that EPSG);
   a string identifier for types EPSG:XXXX, ESRI:XXXX, WORLD:XXXX, NA83:XXXX, or NAD27:XXXX;
   or a proj4 string."
  [c1 c2]
  (.createTransform ctf-factory
                    (create-crs c1)
                    (create-crs c2)))
