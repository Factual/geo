(ns geo.io
  "Helper functions for converting JTS geometries to and from various
   geospatial IO formats (geojson, wkt, wkb)."
  (:require [geo.spatial :as s]
            [clojure.data])
  (:import (org.locationtech.jts.io WKTReader WKTWriter WKBReader WKBWriter)
           (org.locationtech.jts.geom Geometry)
           (org.locationtech.jts.io.geojson GeoJsonReader GeoJsonWriter)))

(def ^WKTReader wkt-reader (WKTReader.))
(def ^WKTWriter wkt-writer (WKTWriter.))

(def ^WKBReader wkb-reader (WKBReader.))
(def ^WKBWriter wkb-writer (WKBWriter.))

(def ^GeoJsonReader geojson-reader (GeoJsonReader.))
(def ^GeoJsonWriter geojson-writer (GeoJsonWriter.))

(.setEncodeCRS geojson-writer false)

(defn read-wkt [^String wkt] (.read wkt-reader wkt))
(defn to-wkt [^Geometry geom] (.write wkt-writer geom))

(defn read-wkb [^bytes bytes] (.read wkb-reader bytes))
(defn to-wkb [^Geometry geom] (.write wkb-writer geom))

(defn read-geojson [^String geojson] (.read geojson-reader geojson))
(defn to-geojson [^Geometry geom] (.toString (.write geojson-writer geom)))
