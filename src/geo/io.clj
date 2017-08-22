(ns geo.io
  (:require [geo.spatial :as s]
            [clojure.data])
  (:import (com.vividsolutions.jts.io WKTReader WKTWriter WKBReader WKBWriter)
           (org.wololo.jts2geojson GeoJSONReader GeoJSONWriter)))

(def wkt-reader (WKTReader.))
(def wkt-writer (WKTWriter.))

(def wkb-reader (WKBReader.))
(def wkb-writer (WKBWriter.))

(def geojson-reader (GeoJSONReader.))
(def geojson-writer (GeoJSONWriter.))

(defn read-wkt [wkt] (.read wkt-reader wkt))
(defn to-wkt [geom] (.write wkt-writer geom))

(defn read-wkb [bytes] (.read wkb-reader bytes))
(defn to-wkb [geom] (.write wkb-writer geom))

(defn read-geojson [geojson] (.read geojson-reader geojson))
(defn to-geojson [geom] (.toString (.write geojson-writer geom)))
