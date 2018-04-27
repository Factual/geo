(ns geo.t-io
  (:require [geo.io :as sut]
            [midje.sweet :as midje :refer [fact facts]]))

(def wkt "POLYGON ((-70.0024 30.0019, -70.0024 30.0016, -70.0017 30.0016, -70.0017 30.0019, -70.0024 30.0019))")
(def wkt-2 "POLYGON ((-118.41632050954 34.0569111034308,-118.416338488265 34.0568986639481,-118.416492289093 34.0567922421742,-118.416530323474 34.0567659511355,-118.418142315196 34.0556504823845,-118.418150052351 34.0556594411781,-118.418689703423 34.0562853532086,-118.418415962306 34.0564736252562,-118.418418218819 34.0564759256614,-118.418378572273 34.0565031846417,-118.418376282883 34.0565009118285,-118.418233752614 34.0565989279524,-118.418236937922 34.0566021044676,-118.418196205493 34.0566301090773,-118.4181930532 34.0566269324467,-118.418073292223 34.0567096187712,-118.417930201783 34.0568079936995,-118.417773815715 34.0566504764553,-118.41769386392 34.0567054359181,-118.417612270018 34.0566232540736,-118.416840191548 34.0571544513339,-118.416817318266 34.0571701863547,-118.41678665332 34.0571912877453,-118.416674690686 34.0572683160349,-118.41632050954 34.0569111034308))")
(def geojson "{\"type\":\"Polygon\",\"coordinates\":[[[-70.0024,30.0019],[-70.0024,30.0016],[-70.0017,30.0016],[-70.0017,30.0019],[-70.0024,30.0019]]]}")
(def ls-wkt "LINESTRING (-70.0024 30.0019, -70.0024 30.0016, -70.0017 30.0016, -70.0017 30.0019, -70.0024 30.0019)")
;; TODO: Handle geojson features and feature collections.
;; Could use https://github.com/bjornharrtell/jts2geojson for inspiration
(def geojson-feature "{\"type\": \"Feature\",\"geometry\": {\"type\": \"Point\",\"coordinates\": [102.0, 0.5]},\"properties\": {\"prop0\": \"value0\"}}")
(def geojson-feature-coll "{\"type\":\"FeatureCollection\",\"features\":[{\"type\":\"Feature\",\"geometry\":{\"type\":\"Point\",\"coordinates\":[102.0,0.5]},\"properties\":{\"prop0\":\"value0\"}},{\"type\":\"Feature\",\"geometry\":{\"type\":\"LineString\",\"coordinates\":[[102.0,0.0],[103.0,1.0],[104.0,0.0],[105.0,1.0]]},\"properties\":{\"prop0\":\"value0\",\"prop1\":0.0}},{\"type\":\"Feature\",\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[[[100.0,0.0],[101.0,0.0],[101.0,1.0],[100.0,1.0],[100.0,0.0]]]},\"properties\":{\"prop0\":\"value0\",\"prop1\":{\"this\":\"that\"}}}]}")

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

(fact "reads and writes geojson"
      (type (sut/read-geojson geojson)) => org.locationtech.jts.geom.Polygon
      (.getNumPoints (sut/read-geojson geojson)) => 5
      (map (fn [c] [(.x c) (.y c)]) (.getCoordinates (sut/read-geojson geojson))) => coords
      (-> geojson sut/read-geojson sut/to-geojson) => geojson)
