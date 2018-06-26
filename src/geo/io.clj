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

(defn- feat->geom
  "Convert a Feature into a Geometry, applying the options specified by read-geojson."
  ([^Feature feature]
   (feat->geom feature {}))
  ([^Feature feature options]
   (let [options (merge {:properties? false
                         :keywords? false
                         :srid 4326} options)
         properties? (:properties? options)
         keywords? (:keywords? options)
         g (jts/set-srid (.read geojson-reader (.getGeometry feature)) (:srid options))]
     (cond (false? properties?)
           g
           (true? properties?)
           (hash-map :geometry g
                     :properties (cond (false? keywords?)
                                       (into {} (.getProperties feature))
                                       (true? keywords?)
                                       (keywordize-keys (into {} (.getProperties feature)))))))))

(defn- into-collection
  "Convert a vector of features or geometries into a single collection,
   applying the options specified by read-geojson."
  [v options]
  (let [options (merge {} options)
        properties? (:properties? options)
        geoms (cond (instance? Geometry (first v))
                    (into-array v)
                    (map? (first v))
                    (into-array (map :geometry v)))
        gc (.createGeometryCollection gf-wgs84 geoms)
        props (when (true? properties?)
                (vec (map :properties v)))]
    (cond
      (true? properties?)
      (hash-map :geometry gc
                :properties props)
      (false? properties?)
      gc)))

(defn- fc->geoms
  "Convert a FeatureCollection into geometries, applying the options specified by read-geojson"
  ([^FeatureCollection fc]
   (fc->geoms fc {}))

  ([^FeatureCollection fc options]
   (let [options (merge {:properties? false
                         :collection? false
                         :keywords? false} options)
         collection? (:collection? options)
         v (into [] (map #(feat->geom % options) (vec (.getFeatures fc))))]
     (cond (false? collection?)
           v
           (true? collection?)
           (into-collection v options)))))

(defn read-geojson
  "Parse a GeoJSON and convert based on a set of options. By default, and if no options are specified,
  their values default to false.

  Options map:
  {:properties?
   If false, return either a single Geometry or a vector of Geometries.
   If true, if the GeoJSON contains Features, return either a single hash-map or a vector of hash-maps.
   Each hash-map consists of a :geometry key containing a JTS Geometry and a :properties key containing
   all the properties associated with that Feature.

   :keywords?
   Only applies when properties are being returned. If false, return properties where keys are strings.
   If true, return properties where keys are keywords.

   :collection?
   If false, split GeometryCollections and FeatureCollections into individual Geometries or hash-maps
   representing Features.
   If true, combine all Geometries into one GeometryCollection and combine all properties into one
   vector with individual hash-maps for each Feature.

   :srid
   If set, set the Geometry's SRID. Defaults to 4326."

  ([^String geojson]
   (read-geojson geojson {:srid 4326}))
  ([^String geojson options]
   (let [parsed (parse-geojson geojson)]
     (cond (instance? org.wololo.geojson.Geometry parsed)
           (jts/set-srid (.read geojson-reader parsed) (:srid options))
           (instance? FeatureCollection parsed)
           (fc->geoms parsed options)
           (instance? Feature parsed)
           (feat->geom parsed options)))))

(defn to-geojson [^Geometry geom] (.toString (.write geojson-writer geom)))
