(ns geo.io
  (:require [geo.spatial :as s]
            [clojure.data])
  (:import (com.vividsolutions.jts.io WKTReader WKTWriter WKBReader WKBWriter)
           (com.vividsolutions.jts.geom Geometry)
           (org.wololo.jts2geojson GeoJSONReader GeoJSONWriter)))

(def ^WKTReader wkt-reader (WKTReader.))
(def ^WKTWriter wkt-writer (WKTWriter.))

(def ^WKBReader wkb-reader (WKBReader.))
(def ^WKBWriter wkb-writer (WKBWriter.))

(def ^GeoJSONReader geojson-reader (GeoJSONReader.))
(def ^GeoJSONWriter geojson-writer (GeoJSONWriter.))

(defn read-wkt [^String wkt] (.read wkt-reader wkt))
(defn to-wkt [^Geometry geom] (.write wkt-writer geom))

(defn read-wkb [^bytes bytes] (.read wkb-reader bytes))
(defn to-wkb [^Geometry geom] (.write wkb-writer geom))

(defn read-geojson [^String geojson] (.read geojson-reader geojson))
(defn to-geojson [^Geometry geom] (.toString (.write geojson-writer geom)))
