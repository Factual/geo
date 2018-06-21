(ns geo.io
  "Helper functions for converting JTS geometries to and from various
   geospatial IO formats (geojson, wkt, wkb)."
  (:require [geo.jts]
            [clojure.data])
  (:import (org.locationtech.jts.io WKTReader WKTWriter WKBReader WKBWriter)
           (org.locationtech.jts.geom Geometry)
           (org.locationtech.jts.io.geojson GeoJsonReader GeoJsonWriter)))

(def ^WKTReader wkt-reader (WKTReader.))
(def ^WKTWriter wkt-writer (WKTWriter.))

(def ^WKBReader wkb-reader (WKBReader.))
(def ^WKBWriter wkb-writer (WKBWriter.))
(def ^WKBWriter wkb-writer-2d-srid (WKBWriter. 2 true))

(def ^GeoJsonReader geojson-reader (GeoJsonReader.))
(def ^GeoJsonWriter geojson-writer (GeoJsonWriter.))

(.setEncodeCRS geojson-writer false)

(defn read-wkt [^String wkt] (.read wkt-reader wkt))
(defn to-wkt [^Geometry geom] (.write wkt-writer geom))

(defn read-wkb
  "Read a WKB byte array and convert to a Geometry"
  [^bytes bytes]
  (.read wkb-reader bytes))

(defn read-wkb-hex
  "Read a WKB hex string and convert to a Geometry"
  [^String s]
  (read-wkb (WKBReader/hexToBytes s)))

(defn to-wkb
  "Write a WKB, excluding any SRID"
  [^Geometry geom]
  (.write wkb-writer geom))

(defn to-ewkb [^Geometry geom]
  "Write an EWKB, including the SRID"
  (.write wkb-writer-2d-srid geom))

(defn to-wkb-hex
  "Write a WKB as a hex string, excluding any SRID"
  [^Geometry geom]
  (WKBWriter/toHex (to-wkb geom)))

(defn to-ewkb-hex
  "Write an EWKB as a hex string, excluding any SRID"
  [^Geometry geom]
  (WKBWriter/toHex (to-ewkb geom)))

(defn read-geojson [^String geojson] (.read geojson-reader geojson))
(defn to-geojson [^Geometry geom] (.toString (.write geojson-writer geom)))
