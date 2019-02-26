(ns geo.io
  "Helper functions for converting JTS geometries to and from various
   geospatial IO formats (geojson, wkt, wkb)."
  (:require [geo.jts :refer [gf-wgs84] :as jts]
            [geo.spatial :as spatial :refer [Shapelike to-jts]]
            [clojure.data]
            [clojure.walk :refer [keywordize-keys stringify-keys]])
  (:import (java.util Arrays Arrays$ArrayList)
           (org.locationtech.jts.io WKTReader WKTWriter WKBReader WKBWriter)
           (org.locationtech.jts.geom Geometry GeometryCollection)
           (org.wololo.geojson FeatureCollection GeoJSONFactory)
           (org.wololo.jts2geojson GeoJSONReader GeoJSONWriter)))

(defn ^Arrays$ArrayList feature-list
  [features]
  (Arrays/asList (into-array org.wololo.geojson.Feature features)))

(defn read-wkt
  "Read a WKT string and convert to a Geometry.
   Can optionally pass in SRID. Defaults to WGS84"
  ([^String wkt] (.read (WKTReader. gf-wgs84) wkt))
  ([^String wkt srid] (.read (WKTReader. (jts/gf srid)) wkt)))

(defn to-wkt [shapelike] (.write (WKTWriter.) (to-jts shapelike)))

(defn read-wkb
  "Read a WKB byte array and convert to a Geometry.
   Can optionally pass in SRID. Defaults to WGS84"
  ([^bytes bytes] (.read (WKBReader. gf-wgs84) bytes))
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
  [shapelike]
  (.write (WKBWriter.) (to-jts shapelike)))

(defn to-ewkb [shapelike]
  "Write an EWKB, including the SRID"
  (.write (WKBWriter. 2 true) (to-jts shapelike)))

(defn to-wkb-hex
  "Write a WKB as a hex string, excluding any SRID"
  [shapelike]
  (WKBWriter/toHex (to-wkb (to-jts shapelike))))

(defn to-ewkb-hex
  "Write an EWKB as a hex string, excluding any SRID"
  [shapelike]
  (WKBWriter/toHex (to-ewkb (to-jts shapelike))))

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
  (read-geometry [this] (.read (GeoJSONReader.) this))
  org.wololo.geojson.Feature
  (read-geometry [this] (read-geometry (.getGeometry this))))

(defprotocol GeoJSONFeatures
  (to-features [this]))

(extend-protocol GeoJSONFeatures
  org.wololo.geojson.Geometry
  (to-features [this] [(spatial/->Feature (read-geometry this) {})])
  org.wololo.geojson.GeometryCollection
  (to-features [this] (mapcat to-features (jts/geometries (read-geometry this))))
  Geometry
  (to-features [this] [(spatial/->Feature this {})])
  GeometryCollection
  (to-features [this] (mapcat to-features (jts/geometries this)))
  org.wololo.geojson.Feature
  (to-features [this] [(spatial/->Feature (read-geometry this) (properties this))])
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

(defn to-geojson [shapelike] (.toString (.write (GeoJSONWriter.) (to-jts shapelike jts/default-srid))))

(defn- ^org.wololo.geojson.Feature gj-feature
  [^geo.spatial.Feature feature]
  (let [gj-geom (.write (GeoJSONWriter.) (to-jts (:geometry feature) jts/default-srid))]
    (org.wololo.geojson.Feature. gj-geom (stringify-keys (:properties feature)))))

(defn to-geojson-feature
  [^geo.spatial.Feature feature]
  (.toString (gj-feature feature)))

(defn to-geojson-feature-collection
  [feature-maps]
  (let [features (feature-list (map gj-feature feature-maps))]
    (.toString (.write (GeoJSONWriter.) features))))
