(ns geo.t-io
  (:require [clojure.string :refer [trim]]
            [geo.io :as sut]
            [geo.jts :as jts]
            [midje.sweet :refer [fact facts truthy]])
  (:import (org.locationtech.jts.geom Geometry GeometryCollection)))

(def wkt (trim (slurp "test/resources/wkt")))
(def wkt-2 (trim (slurp "test/resources/wkt-2")))
(def wkb-hex (trim (slurp "test/resources/wkb-hex")))
(def ewkb-hex-wgs84 (trim (slurp "test/resources/ewkb-hex-wgs84")))
(def wkb-2-hex (trim (slurp "test/resources/wkb-2-hex")))
(def ewkb-2-hex-wgs84 (trim (slurp "test/resources/ewkb-2-hex-wgs84")))
(def geometry (trim (slurp "test/resources/geometry")))
(def feature (trim (slurp "test/resources/feature")))
(def feature-collection-1 (trim (slurp "test/resources/feature-collection-1")))
(def feature-collection-2 (trim (slurp "test/resources/feature-collection-2")))
(def null-island-geometry (trim (slurp "test/resources/null-island-geometry")))
(def one-island-geometry (trim (slurp "test/resources/one-island-geometry")))
(def null-island-properties (read-string (trim (slurp "test/resources/null-island-properties"))))
(def null-island-properties-kw (read-string (trim (slurp "test/resources/null-island-properties-kw"))))
(def one-island-properties (read-string (trim (slurp "test/resources/one-island-properties"))))
(def one-island-properties-kw (read-string (trim (slurp "test/resources/one-island-properties-kw"))))

(def coords [[-70.0024 30.0019]
             [-70.0024 30.0016]
             [-70.0017 30.0016]
             [-70.0017 30.0019]
             [-70.0024 30.0019]])

(fact "reads and writes wkt"
      (type (sut/read-wkt wkt)) => org.locationtech.jts.geom.Polygon
      (.getNumPoints (sut/read-wkt wkt)) => 5
      (map (fn [c] [(.x c) (.y c)]) (.getCoordinates (sut/read-wkt wkt))) => coords
      (-> wkt sut/read-wkt sut/to-wkt) => wkt)

(fact "reads and writes wkb"
      (let [geom (sut/read-wkt wkt)
            wkb (sut/to-wkb geom)]
        (count wkb) => 93
        (.getNumPoints (sut/read-wkb wkb)) => 5
        (map (fn [c] [(.x c) (.y c)]) (.getCoordinates (sut/read-wkt wkt))) => coords
        (-> wkt sut/read-wkt sut/to-wkb sut/read-wkb sut/to-wkt) => wkt))

(fact "reads and writes ewkb"
      (let [geom (jts/set-srid (sut/read-wkt wkt) 3857)
            ewkb (sut/to-ewkb geom)]
        (count ewkb) => 97
        (.getNumPoints (sut/read-wkb ewkb)) => 5
        (map (fn [c] [(.x c) (.y c)]) (.getCoordinates (sut/read-wkb ewkb))) => coords
        (-> wkt sut/read-wkt sut/to-ewkb sut/read-wkb sut/to-wkt) => wkt))

(facts "reads and writes wkb in hex string"
       (fact "wkb-hex identity"
             (-> wkb-hex sut/read-wkb-hex sut/to-wkb-hex) => wkb-hex
             (-> wkb-2-hex sut/read-wkb-hex sut/to-wkb-hex) => wkb-2-hex)
       (fact "ewkb-hex -> wkb-hex"
             (-> ewkb-hex-wgs84 sut/read-wkb-hex sut/to-wkb-hex) => wkb-hex
             (-> ewkb-2-hex-wgs84 sut/read-wkb-hex sut/to-wkb-hex) => wkb-2-hex)
       (fact "wkt -> wkb-hex"
             (-> wkt sut/read-wkt sut/to-wkb-hex) => wkb-hex
             (-> wkt-2 sut/read-wkt (jts/set-srid 3857) sut/to-wkb-hex) => wkb-2-hex)
       (fact "wkb-hex -> wkt"
             (-> wkb-hex sut/read-wkb-hex sut/to-wkt) => wkt
             (-> wkb-2-hex sut/read-wkb-hex sut/to-wkt) => wkt-2)
       (fact "ewkb-hex -> wkt"
             (-> ewkb-hex-wgs84 sut/read-wkb-hex sut/to-wkt) => wkt
             (-> ewkb-2-hex-wgs84 sut/read-wkb-hex sut/to-wkt) => wkt-2)
       (fact "wkt with projection -> wkt"
             (-> wkt sut/read-wkt (jts/set-srid 3857) sut/to-wkb-hex) => wkb-hex
             (-> wkt-2 sut/read-wkt (jts/set-srid 3857) sut/to-wkb-hex) => wkb-2-hex))

(fact "understands projection in hex string"
      (-> wkb-hex sut/read-wkb-hex jts/get-srid) => 4326
      (-> ewkb-hex-wgs84 sut/read-wkb-hex jts/get-srid) => 4326
      (-> wkb-2-hex sut/read-wkb-hex jts/get-srid) => 4326
      (-> ewkb-2-hex-wgs84 sut/read-wkb-hex jts/get-srid) => 4326)

(fact "reads WKTs and WKBs with custom SRID"
      (-> wkt sut/read-wkt jts/get-srid) => 4326
      (-> wkt (sut/read-wkt 2229) jts/get-srid) => 2229
      (-> wkb-hex sut/read-wkb-hex jts/get-srid) => 4326
      (-> wkb-hex (sut/read-wkb-hex 2229) jts/get-srid) => 2229)

(facts "reads and writes ewkb in hex string"
       (fact "ewkb-hex identity"
             (-> ewkb-hex-wgs84 sut/read-wkb-hex sut/to-ewkb-hex) => ewkb-hex-wgs84
             (-> ewkb-2-hex-wgs84 sut/read-wkb-hex sut/to-ewkb-hex) => ewkb-2-hex-wgs84)
       (fact "wkb-hex with projection set -> ewkb-hex"
            (-> wkb-hex sut/read-wkb-hex (jts/set-srid 4326) sut/to-ewkb-hex) => ewkb-hex-wgs84
            (-> wkb-2-hex sut/read-wkb-hex (jts/set-srid 4326) sut/to-ewkb-hex) => ewkb-2-hex-wgs84)
       (fact "wkt with projection set -> ewkb-hex"
             (-> wkt sut/read-wkt (jts/set-srid 4326) sut/to-ewkb-hex) => ewkb-hex-wgs84
             (-> wkt-2 sut/read-wkt (jts/set-srid 4326) sut/to-ewkb-hex) => ewkb-2-hex-wgs84)
       (fact "ewkb-hex with same projection set"
             (-> ewkb-hex-wgs84 sut/read-wkb-hex (jts/set-srid 4326) sut/to-ewkb-hex) => ewkb-hex-wgs84
             (-> ewkb-2-hex-wgs84 sut/read-wkb-hex (jts/set-srid 4326) sut/to-ewkb-hex) => ewkb-2-hex-wgs84))

(fact "Reading GeoJSON Geometry"
      (count (sut/read-geojson geometry)) => 1
      (let [parsed (first (sut/read-geojson geometry))]
        parsed => map?
        (keys parsed) => [:properties :geometry]
        (-> parsed :geometry .getNumPoints) => 5
        (->> parsed :geometry .getCoordinates (map (fn [c] [(.x c) (.y c)]))) => coords
        (-> parsed :geometry sut/to-geojson sut/read-geojson parsed)))

(fact "Reading GeoJSON Feature"
      (count (sut/read-geojson feature)) => 1
      (-> feature
          sut/read-geojson
          first
          :geometry
          type) => org.locationtech.jts.geom.Point)

(fact "Reading GeoJSON FeatureCollection"
      (count (sut/read-geojson feature-collection-1)) => 1
      (->> feature-collection-2
           sut/read-geojson
           (map :geometry)
           (map type)) => [org.locationtech.jts.geom.Point org.locationtech.jts.geom.Point])

#_(facts "by default, geojson features with properties lose all info except geometry when read in"
      (fact "dropping all properties for a single feature"
            (-> feature sut/read-geojson sut/to-geojson)
            => null-island-geometry)
      (fact "dropping all properties for a feature collection with one feature"
            (->> feature-collection-1 sut/read-geojson (map sut/to-geojson))
            => (list null-island-geometry))
      (fact "dropping all properties for a feature collection with two features"
            (->> feature-collection-2 sut/read-geojson (map sut/to-geojson))
            => (list null-island-geometry one-island-geometry)))

#_(facts "options map for read-geojson handles properties differently"
       (fact "feature: keep properties, no collections"
             (-> feature (sut/read-geojson {:properties? true}) :properties)
             => null-island-properties)
       (fact "feature: keep properties, no collections, properties as keywords"
             (-> feature (sut/read-geojson {:properties? true
                                            :keywords? true}) :properties)
             => null-island-properties-kw)
       (fact "feature collection: keep properties, no collections"
             (map :properties (-> feature-collection-1 (sut/read-geojson {:properties? true})))
             => (list null-island-properties)
             (instance? Geometry
                        (first (map :geometry (-> feature-collection-1 (sut/read-geojson {:properties? true})))))
             => truthy
             (instance? Geometry
                        (first (map :geometry (-> feature-collection-2 (sut/read-geojson {:properties? true})))))
             => truthy
             (map :properties (-> feature-collection-1 (sut/read-geojson {:properties? true})))
             => (list null-island-properties)
             (map :properties (-> feature-collection-2 (sut/read-geojson {:properties? true})))
             => (list null-island-properties one-island-properties))
       (fact "feature collection: keep properties, collections"
             (-> feature-collection-1 (sut/read-geojson {:properties? true
                                                         :collection? true})
                 :properties)
             => (list null-island-properties)
             (-> feature-collection-2 (sut/read-geojson {:properties? true
                                                         :collection? true})
                 :properties)
             => (list null-island-properties one-island-properties)
             (instance? GeometryCollection
                        (-> feature-collection-1 (sut/read-geojson {:properties? true :collection? true})
                            :geometry))
             => truthy
             (instance? GeometryCollection
                        (-> feature-collection-2 (sut/read-geojson {:properties? true :collection? true})
                            :geometry))
             => truthy)
       (fact "feature collection: keep properties, collections, properties as keywords"
             (-> feature-collection-1 (sut/read-geojson {:properties? true :collection? true :keywords? true})
                 :properties)
             => (list null-island-properties-kw)
             (-> feature-collection-2 (sut/read-geojson {:properties? true :collection? true :keywords? true})
                 :properties)
             => (list null-island-properties-kw one-island-properties-kw)
             (instance? GeometryCollection
                        (-> feature-collection-1
                            (sut/read-geojson {:properties? true :collection? true :keywords? true})
                            :geometry))
             => truthy
             (instance? GeometryCollection
                        (-> feature-collection-2
                            (sut/read-geojson {:properties? true :collection? true :keywords true})
                            :geometry))
             => truthy))

(fact "parsing geojson defaults to EPSG:4326 but SRID can be overridden in option map"
      (map (comp jts/get-srid :geometry) (sut/read-geojson geometry)) => [4326]
      (map (comp jts/get-srid :geometry) (sut/read-geojson feature)) => [4326]
      (map (comp jts/get-srid :geometry) (sut/read-geojson feature-collection-1)) => [4326]
      (map (comp jts/get-srid :geometry) (sut/read-geojson geometry 2229)) => [2229]
      (map (comp jts/get-srid :geometry) (sut/read-geojson feature 2229)) => [2229]
      (map (comp jts/get-srid :geometry) (sut/read-geojson feature-collection-1 2229)) => [2229])
