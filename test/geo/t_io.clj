(ns geo.t-io
  (:require [geo.io :as sut]
            [geo.jts :as jts]
            [midje.sweet :refer [fact facts truthy]]))

(def wkt "POLYGON ((-70.0024 30.0019, -70.0024 30.0016, -70.0017 30.0016, -70.0017 30.0019, -70.0024 30.0019))")
(def wkt-2 "POLYGON ((-118.41632050954 34.0569111034308, -118.416338488265 34.0568986639481, -118.416492289093 34.0567922421742, -118.416530323474 34.0567659511355, -118.418142315196 34.0556504823845, -118.418150052351 34.0556594411781, -118.418689703423 34.0562853532086, -118.418415962306 34.0564736252562, -118.418418218819 34.0564759256614, -118.418378572273 34.0565031846417, -118.418376282883 34.0565009118285, -118.418233752614 34.0565989279524, -118.418236937922 34.0566021044676, -118.418196205493 34.0566301090773, -118.4181930532 34.0566269324467, -118.418073292223 34.0567096187712, -118.417930201783 34.0568079936995, -118.417773815715 34.0566504764553, -118.41769386392 34.0567054359181, -118.417612270018 34.0566232540736, -118.416840191548 34.0571544513339, -118.416817318266 34.0571701863547, -118.41678665332 34.0571912877453, -118.416674690686 34.0572683160349, -118.41632050954 34.0569111034308))")
(def wkb-hex "00000000030000000100000005C0518027525460AA403E007C84B5DCC6C0518027525460AA403E0068DB8BAC71C051801BDA5119CE403E0068DB8BAC71C051801BDA5119CE403E007C84B5DCC6C0518027525460AA403E007C84B5DCC6")
(def ewkb-hex-wgs84 "0020000003000010E60000000100000005C0518027525460AA403E007C84B5DCC6C0518027525460AA403E0068DB8BAC71C051801BDA5119CE403E0068DB8BAC71C051801BDA5119CE403E007C84B5DCC6C0518027525460AA403E007C84B5DCC6")
(def wkb-2-hex "00000000030000000100000019C05D9AA4FEC7483740410748DCF001DDC05D9AA54A2FCA834041074874966BEDC05D9AA7CF462C3340410744F7DB6700C05D9AA86ECD473E404107441B4FD3A0C05D9AC2D7FC35CB4041071F8E14EC8DC05D9AC2F86FEAC44041071FD93BC96BC05D9ACBCFE5D7EB404107345BC3A32BC05D9AC753BE8EA24041073A871AC7EEC05D9AC75D3578344041073A9A66DD10C05D9AC6B6EB509F4041073B7F1113F6C05D9AC6AD5119E44041073B6C003FB1C05D9AC4578063DF4041073EA23845A9C05D9AC464DC96AB4041073EBCDDC95CC05D9AC3BA0479D74041073FA7C93A5BC05D9AC3ACCBBA234041073F8D237737C05D9AC1B67B5B724041074242C2FED7C05D9ABF5E512ACB404107457BFD8B9DC05D9ABCCE62E7BF4041074052A3D6D4C05D9ABB7F0B51AE404107421FAC6307C05D9ABA28D088AE4041073F6E483838C05D9AAD827B96DA40410750D649A664C05D9AAD228B96F8404107515A48598FC05D9AACA1ED5ACC404107520B4B3DB8C05D9AAACC52644E4041075491743BC3C05D9AA4FEC7483740410748DCF001DD")
(def ewkb-2-hex-wgs84 "0020000003000010E60000000100000019C05D9AA4FEC7483740410748DCF001DDC05D9AA54A2FCA834041074874966BEDC05D9AA7CF462C3340410744F7DB6700C05D9AA86ECD473E404107441B4FD3A0C05D9AC2D7FC35CB4041071F8E14EC8DC05D9AC2F86FEAC44041071FD93BC96BC05D9ACBCFE5D7EB404107345BC3A32BC05D9AC753BE8EA24041073A871AC7EEC05D9AC75D3578344041073A9A66DD10C05D9AC6B6EB509F4041073B7F1113F6C05D9AC6AD5119E44041073B6C003FB1C05D9AC4578063DF4041073EA23845A9C05D9AC464DC96AB4041073EBCDDC95CC05D9AC3BA0479D74041073FA7C93A5BC05D9AC3ACCBBA234041073F8D237737C05D9AC1B67B5B724041074242C2FED7C05D9ABF5E512ACB404107457BFD8B9DC05D9ABCCE62E7BF4041074052A3D6D4C05D9ABB7F0B51AE404107421FAC6307C05D9ABA28D088AE4041073F6E483838C05D9AAD827B96DA40410750D649A664C05D9AAD228B96F8404107515A48598FC05D9AACA1ED5ACC404107520B4B3DB8C05D9AAACC52644E4041075491743BC3C05D9AA4FEC7483740410748DCF001DD")
(def geojson "{\"type\":\"Polygon\",\"coordinates\":[[[-70.0024,30.0019],[-70.0024,30.0016],[-70.0017,30.0016],[-70.0017,30.0019],[-70.0024,30.0019]]]}")

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
      (-> wkb-hex sut/read-wkb-hex jts/get-srid) => 0
      (-> ewkb-hex-wgs84 sut/read-wkb-hex jts/get-srid) => 4326
      (-> wkb-2-hex sut/read-wkb-hex jts/get-srid) => 0
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
