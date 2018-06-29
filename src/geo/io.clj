(ns geo.io
  "Helper functions for converting JTS geometries to and from various
   geospatial IO formats (geojson, wkt, wkb)."
  (:require [geo.jts :refer [gf-wgs84] :as jts]
            [geo.spatial :refer [Shapelike to-jts]]
            [clojure.data]
            [clojure.walk :refer [keywordize-keys stringify-keys]])
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

(defn properties [^Feature feature]
  (keywordize-keys (into {} (.getProperties feature))))

(defprotocol GeoJSONGeometry
  (read-geometry [this]))

(extend-protocol GeoJSONGeometry
  org.wololo.geojson.Geometry
  (read-geometry [this] (.read geojson-reader this))
  Feature
  (read-geometry [this] (read-geometry (.getGeometry this))))

(defprotocol GeoJSONFeatures
  (to-features [this]))

(extend-protocol GeoJSONFeatures
  org.wololo.geojson.Geometry
  (to-features [this] [{:properties {} :geometry (read-geometry this)}])
  Feature
  (to-features [this] [{:properties (properties this) :geometry (read-geometry this)}])
  FeatureCollection
  (to-features [this] (mapcat to-features (.getFeatures this))))

(defn read-geojson
  "Parse a GeoJSON string into a sequence of maps representing GeoJSON \"Features\".

  These will contain a :geometry key containing a JTS geometry and a :features key
  containing a (possibly empty) map of features.

  (read-geojson \"{\\\"type\\\":\\\"Polygon\\\",\\\"coordinates\\\":[[[-70.0024,30.0019],[-70.0024,30.0016],[-70.0017,30.0016],[-70.0017,30.0019],[-70.0024,30.0019]]]}\")
  => [{:properties {}, :geometry #object[org.locationtech.jts.geom.Polygon(...)]}]

  (read-geojson \"{\\\"type\\\":\\\"Feature\\\",\\\"geometry\\\":{\\\"type\\\":\\\"Point\\\",\\\"coordinates\\\":[0.0,0.0]},\\\"properties\\\":{\\\"name\\\":\\\"null island\\\"}}\")
  => [{:properties {:name \"null island\"}
       :geometry #object[org.locationtech.jts.geom.Point(...)]}]


  (read-geojson \"{\\\"type\\\":\\\"FeatureCollection\\\",\\\"features\\\":[{\\\"type\\\":\\\"Feature\\\",\\\"geometry\\\":{\\\"type\\\":\\\"Point\\\",\\\"coordinates\\\":[0.0,0.0]},\\\"properties\\\":{\\\"name\\\":\\\"null island\\\"}}]}\")
  => [{:properties {:name \"null island\"},
       :geometry #object[org.locationtech.jts.geom.Point(...)]}]
  "
  ([^String geojson]
   (read-geojson geojson jts/default-srid))
  ([^String geojson srid]
   (->> geojson
        parse-geojson
        to-features
        (map (fn [f] (update f :geometry (fn [g] (jts/set-srid g srid))))))))

(defn read-geojson-geometry
  "Parse a GeoJSON string representing a single Geometry into a JTS Geometry."
  ([^String geojson]
   (read-geojson-geometry geojson jts/default-srid))
  ([^String geojson srid]
   (-> geojson
       parse-geojson
       read-geometry
       (jts/set-srid srid))))

(defn to-geojson [shapelike] (.toString (.write geojson-writer (to-jts shapelike jts/default-srid))))

(defn- gj-feature
  [{shapelike :geometry properties :properties}]
  (let [gj-geom (.write geojson-writer (to-jts shapelike jts/default-srid))]
    (Feature. gj-geom (stringify-keys properties))))

(defn to-geojson-feature
  [feature-map]
  (.toString (gj-feature feature-map)))

(defn to-geojson-feature-collection
  [feature-maps]
  (let [features (map gj-feature feature-maps)]
    (.toString (.write geojson-writer features))))
