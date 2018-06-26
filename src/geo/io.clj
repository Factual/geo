(ns geo.io
  "Helper functions for converting JTS geometries to and from various
   geospatial IO formats (geojson, wkt, wkb)."
  (:require [geo.jts :refer [gf-wgs84] :as jts]
            [clojure.data])
  (:import (org.locationtech.jts.io WKTReader WKTWriter WKBReader WKBWriter)
           (org.locationtech.jts.geom Geometry)
           (org.locationtech.jts.io.geojson GeoJsonReader GeoJsonWriter)))

(def ^WKTReader wkt-reader (WKTReader. gf-wgs84))
(def ^WKTWriter wkt-writer (WKTWriter.))

(def ^WKBReader wkb-reader (WKBReader. gf-wgs84))
(def ^WKBWriter wkb-writer (WKBWriter.))
(def ^WKBWriter wkb-writer-2d-srid (WKBWriter. 2 true))

(def ^GeoJsonReader geojson-reader (GeoJsonReader. gf-wgs84))
(def ^GeoJsonWriter geojson-writer (GeoJsonWriter.))

(.setEncodeCRS geojson-writer false)

(defn read-wkt
  "Read a WKT string and convert to a Geometry.
   Can optionally pass in SRID. Defaults to WGS84"
  ([^String wkt] (.read wkt-reader wkt))
  ([^String wkt srid]
     (.read (WKTReader. (jts/gf srid)) wkt)))

(defn to-wkt [^Geometry geom] (.write wkt-writer geom))

(defn read-wkb
  "Read a WKB byte array and convert to a Geometry.
   Can optionally pass in SRID. Defaults to WGS84"
  ([^bytes bytes] (.read wkb-reader bytes))
  ([^bytes bytes srid]
     (.read (WKBReader. (jts/gf srid)) bytes)))

(defn read-wkb-hex
  "Read a WKB hex string and convert to a Geometry"
  ([^String s]
     (read-wkb (WKBReader/hexToBytes s)))
  ([^String s srid]
     (read-wkb (WKBReader/hexToBytes s) srid)))

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
