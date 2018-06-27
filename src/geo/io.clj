(ns geo.io
  "Helper functions for converting JTS geometries to and from various
   geospatial IO formats (geojson, wkt, wkb)."
  (:require [geo.jts :refer [gf-wgs84] :as jts]
            [clojure.data]
            [clojure.walk :refer [keywordize-keys]])
  (:import (org.locationtech.jts.io WKTReader WKTWriter WKBReader WKBWriter)
           (org.locationtech.jts.geom Geometry)
           (org.wololo.geojson Feature FeatureCollection GeoJSONFactory)
           (org.wololo.jts2geojson GeoJSONReader GeoJSONWriter)))

(def ^WKTReader wkt-reader (WKTReader. gf-wgs84))
(def ^WKTWriter wkt-writer (WKTWriter.))

(def ^WKBReader wkb-reader (WKBReader. gf-wgs84))
(def ^WKBWriter wkb-writer (WKBWriter.))
(def ^WKBWriter wkb-writer-2d-srid (WKBWriter. 2 true))

(def ^GeoJSONReader geojson-reader (GeoJSONReader.))
(def ^GeoJSONWriter geojson-writer (GeoJSONWriter.))

(defn read-wkt
  "Read a WKT string and convert to a Geometry.
   Can optionally pass in SRID. Defaults to WGS84"
  ([^String wkt] (.read wkt-reader wkt))
  ([^String wkt srid] (.read (WKTReader. (jts/gf srid)) wkt)))

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

(defn parse-geojson
  "Parse a geojson using GeoJSONFactory's create"
  [^String geojson]
  (GeoJSONFactory/create geojson))

(defn properties [^org.wololo.geojson.Feature feature]
  (keywordize-keys (into {} (.getProperties feature))))

(defprotocol GeoJSONGeometry
  (read-geometry [this]))

(extend-protocol GeoJSONGeometry
  org.wololo.geojson.Geometry
  (read-geometry [this] (.read geojson-reader this))
  org.wololo.geojson.Feature
  (read-geometry [this] (read-geometry (.getGeometry this))))

(defprotocol GeoJSONFeatures
  (to-features [this]))

(extend-protocol GeoJSONFeatures
  org.wololo.geojson.Geometry
  (to-features [this] [{:properties {} :geometry (read-geometry this)}])
  org.wololo.geojson.Feature
  (to-features [this] [{:properties (properties this) :geometry (read-geometry this)}])
  org.wololo.geojson.FeatureCollection
  (to-features [this] (mapcat to-features (.getFeatures this))))

(defn read-geojson
  "Parse a GeoJSON and convert based on a set of options. By default, and if no options are specified,
  their values default to false."
  ([^String geojson]
   (read-geojson geojson jts/default-srid))
  ([^String geojson srid]
   (->> geojson
        parse-geojson
        to-features
        (map (fn [f] (update f :geometry (fn [g] (jts/set-srid g srid))))))))

(defn to-geojson [^Geometry geom] (.toString (.write geojson-writer geom)))
;; TODO
;; (defn to-geojson-feature [^Geometry geom properties] (.toString (.write geojson-writer geom)))
;; (defn to-geojson-feature-collection [geoms] (.toString (.write geojson-writer geom)))
