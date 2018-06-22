(ns geo.t-io
  (:require [clojure.string :refer [trim]]
            [geo.io :as sut]
            [geo.jts :as jts]
            [midje.sweet :refer [fact facts truthy]]))

(def wkt (trim (slurp "test/resources/wkt")))
(def wkt-2 (trim (slurp "test/resources/wkt-2")))
(def wkb-hex (trim (slurp "test/resources/wkb-hex")))
(def ewkb-hex-wgs84 (trim (slurp "test/resources/ewkb-hex-wgs84")))
(def wkb-2-hex (trim (slurp "test/resources/wkb-2-hex")))
(def ewkb-2-hex-wgs84 (trim (slurp "test/resources/ewkb-2-hex-wgs84")))
(def geojson (trim (slurp "test/resources/geojson")))

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

(fact "reads and writes geojson"
      (type (sut/read-geojson geojson)) => org.locationtech.jts.geom.Polygon
      (.getNumPoints (sut/read-geojson geojson)) => 5
      (map (fn [c] [(.x c) (.y c)]) (.getCoordinates (sut/read-geojson geojson))) => coords
      (-> geojson sut/read-geojson sut/to-geojson) => geojson)
