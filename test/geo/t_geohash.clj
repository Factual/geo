(ns geo.t-geohash
  (:require [geo.spatial :as spatial]
            [geo.jts :as jts]
            [geo.io :as gio]
            [geo.geohash :refer :all]
            [midje.sweet :refer [fact facts just roughly throws truthy]]
            [criterium.core :as crit])
  (:import (ch.hsr.geohash GeoHash)
           (org.locationtech.jts.geom PrecisionModel
                                      Envelope
                                      GeometryFactory)))

(facts "geohash"
       (fact (geohash 50 20 64) => (partial instance? GeoHash))
       (fact "from string"
             (geohash-center (geohash "u4pruydqqvj"))
             => (spatial/geohash-point 57.64911063015461 10.407439693808556))
       (fact "interchangeable center"
             (jts/same-geom? (spatial/jts-point (geohash-center (geohash "u4p4uydqqvj")))
                             (spatial/jts-point (spatial/center (geohash "u4p4uydqqvj"))))
             => truthy))

(facts "subdivide"
       (fact (map #(.longValue ^GeoHash %) (subdivide (geohash "")))
             => [0 (dec (- (long (Math/pow 2 63))))])

       (fact (map string (subdivide (geohash "scu2") 25))
             => ["scu20" "scu21" "scu22" "scu23" "scu24" "scu25" "scu26"
                 "scu27" "scu28" "scu29" "scu2b" "scu2c" "scu2d" "scu2e"
                 "scu2f" "scu2g" "scu2h" "scu2j" "scu2k" "scu2m" "scu2n"
                 "scu2p" "scu2q" "scu2r" "scu2s" "scu2t" "scu2u" "scu2v"
                 "scu2w" "scu2x" "scu2y" "scu2z"]))

(with-redefs
  ; For debugging purposes, we'll treat hashes as [lat long] pairs on an
  ; infinite grid.
  [northern-neighbor (fn [[lat long]] [(inc lat) long])
   southern-neighbor (fn [[lat long]] [(dec lat) long])
   eastern-neighbor  (fn [[lat long]] [lat (inc long)])
   western-neighbor  (fn [[lat long]] [lat (dec long)])]

  ; Check mock
  (fact (northern-neighbor [1 2]) => [2 2])

  (facts "square-ring"
         (fact (square-ring [0 0] -1) => (throws AssertionError))
         (fact (square-ring [0 0] 0)  => (throws AssertionError))
         (fact (square-ring [0 0] 1)  => [[0 0]])
         (fact (square-ring :foo  2)  => (throws AssertionError))
         (fact (square-ring [0 0] 3)  => [[ 1  1] [ 0  1]   ; south
                                          [-1  1] [-1  0]   ; west
                                          [-1 -1] [0  -1]   ; north
                                          [ 1 -1] [ 1  0]]) ; east
         (fact (square-ring [1 1] 4)  => (throws AssertionError))
         (fact (square-ring [1 1] 5)  =>
               [[ 2  2] [ 1  2] [ 0  2] [-1  2]    ; south
                [ -2 2] [-2  1] [-2  0] [-2 -1]    ; west
                [-2 -2] [-1 -2] [ 0 -2] [ 1 -2]    ; north
                [ 2 -2] [ 2 -1] [ 2  0] [ 2  1]])) ; east

  (facts "concentric-square-rings"
         (fact (take 1 (concentric-square-rings [3 -4])) => [[[3 -4]]])
         (fact (take 2 (concentric-square-rings [0 0])) =>
               [[[0 0]]
                (square-ring [0 0] 3)])
         (fact (take 4 (concentric-square-rings [-2 5])) =>
               (cons [[-2 5]]
                (map (partial apply square-ring) [[[-2 5] 3]
                                                  [[-1 6] 5]
                                                  [[ 0 7] 7]])))))

(facts "geohash-area"
       (let [a 5.101e14] ; Earth's surface
         (fact "whole earth"
               (spatial/area (geohash 45 0 0)) => (roughly a))
         (fact "half earth"
               (spatial/area (geohash 45 0 1)) => (roughly (/ a 2)))
         (fact "eighth earth"
               (spatial/area (geohash 45 0 3)) => (roughly (/ a 8)))
         ; As we start digging down, the skew becomes more pronounced.
         (fact "1/256ths" (spatial/area (geohash 45 0 8))
               => (roughly (/ a (Math/pow 2 8)) (/ a (Math/pow 2 15))))
         (fact "1/65536ths" (spatial/area (geohash 45 0 16))
               => (roughly (/ a (Math/pow 2 16)) (/ a (Math/pow 2 19))))
         (fact "2^-30ths" (spatial/area (geohash 45 0 30))
               => (roughly (/ a (Math/pow 2 30)) (/ a (Math/pow 2 33))))
         (fact "2^-50ths" (spatial/area (geohash 45 0 30))
               => (roughly (/ a (Math/pow 2 50)) (/ a (Math/pow 2 29))))

         ; 40 bits of precision gets you a 700 square meter block at the equator
         (fact "40 bits at the equator"
               (spatial/area (geohash 0 0 40)) => (roughly 728.6959))
         (fact "40 bits at 45 degrees"
               (spatial/area (geohash 45 0 40)) => (roughly 515.265))
         (fact "40 bits at 60 degrees"
               (spatial/area (geohash 60 0 40)) => (roughly 364.34))
         ; But only a narrow slice at the pole
         (fact "40 bits at the pole"
               (spatial/area (geohash 90 0 40)) => (roughly 0.001091)))

       (facts "geohash-precision-max-error"
              (fact "20 bits" (geohash-max-error 20) => (roughly 43696.))
              (fact "25 bits" (geohash-max-error 25) => (roughly 6895.2))
              (fact "30 bits" (geohash-max-error 30) => (roughly 1365.2))
              (fact "35 bits" (geohash-max-error 35) => (roughly 215.47))
              (fact "40 bits" (geohash-max-error 40) => (roughly 42.672))
              (fact "45 bits" (geohash-max-error 45) => (roughly 6.7335))
              (fact "50 bits" (geohash-max-error 50) => (roughly 1.3335))
              (fact "55 bits" (geohash-max-error 55) => (roughly 0.2104))
              (fact "60 bits" (geohash-max-error 60) => (roughly 0.04167))))

(facts "geohashes-near"
       (fact "10 meter radius around 1,1 with 35 bits precision"
             (map string (geohashes-near (spatial/geohash-point 1 1) 10 35))
             => ["s00twy0"])
       (fact "100 meter radius with 35 bits precision"
             (map string
                  (geohashes-near (spatial/geohash-point 37.613834 -119.088062) 100 35))
             => (just ["9qemfp6" "9qemfpe" "9qemfp7" "9qemfp5" "9qemfp4" "9qemfp3"
                       "9qemfpd"] :in-any-order))
       (fact "10 meter radius around 1,1 with 40 bits precision"
             (map string (geohashes-near (spatial/geohash-point 1 1) 10 40))
             => (just ["s00twy01" "s00twy00"] :in-any-order))
       (fact "100 meter radius with 40 bits precision"
             (map string
                  (geohashes-near (spatial/geohash-point -50.675351 166.191226) 100 40))
             => (just ["pwyptybf" "pwyptyc5" "pwyptyc4" "pwyptyc1" "pwyptybc"
                       "pwyptyb9" "pwyptybd" "pwyptybe" "pwyptybg" "pwyptyck"
                       "pwyptyc7" "pwyptyc6" "pwyptyc3" "pwyptyc2" "pwyptyc0"
                       "pwyptybb" "pwyptyb8" "pwyptyb2" "pwyptyb3" "pwyptyb6"
                       "pwyptyb7" "pwyptybk" "pwyptybs" "pwyptybu" "pwyptych"
                       "pwyptyct" "pwyptycs" "pwyptyce" "pwyptycd" "pwyptyc9"
                       "pwyptyc8" "pwypty9x" "pwypty9r" "pwypty9p" "pwypty8z"
                       "pwypty8x" "pwypty8r" "pwypty8p" "pwyptyb0" "pwyptyb1"
                       "pwyptyb4" "pwyptyb5" "pwyptybh" "pwyptybj" "pwyptybm"
                       "pwyptybt" "pwyptybv" "pwyptycj" "pwyptycm" "pwyptycu"
                       "pwyptycg" "pwyptycf" "pwyptycc" "pwypty9q" "pwypty9n"
                       "pwypty8y" "pwypty8w" "pwypty8q" "pwypty8n" "pwyptwxz"
                       "pwyptwzb" "pwyptwzc" "pwyptwzf" "pwyptwzg" "pwyptwzu"
                       "pwyptwzv" "pwyptwzy" "pwyptybn" "pwyptybq" "pwyptybw"
                       "pwyptyby" "pwyptycn" "pwyptycq" "pwyptycw" "pwypty9j"
                       "pwypty8v" "pwypty8t" "pwypty8m" "pwyptwz9" "pwyptwzd"
                       "pwyptwze" "pwyptybp" "pwyptybr" "pwyptybx" "pwyptybz"
                       "pwyptycp" "pwyptycr"] :in-any-order))
       (fact "10 meter radius with 45 bits precision"
             (map string
                  (geohashes-near (spatial/geohash-point 35.971411 -121.453086) 10 45))
             => (just ["9q3ssk2s9" "9q3ssk2sf" "9q3ssk2sd" "9q3ssk2s6"
                       "9q3ssk2s3" "9q3ssk2s2" "9q3ssk2s8" "9q3ssk2sb"
                       "9q3ssk2sc" "9q3ssk2t5" "9q3ssk2sg" "9q3ssk2se"
                       "9q3ssk2s7" "9q3ssk2s5" "9q3ssk2s4" "9q3ssk2s1"
                       "9q3ssk2s0" "9q3ssk2kr" "9q3ssk2kx" "9q3ssk2kz"
                       "9q3ssk2t0" "9q3ssk2t1" "9q3ssk2t4" "9q3ssk2su"
                       "9q3ssk2ss" "9q3ssk2sk"] :in-any-order)))

(facts "least-upper-bound-index"
       (fact (least-upper-bound-index [10 9 8 5 2 1] 3) => 3))

(facts "shape->precision"
       (fact (shape->precision (geohash 45 45 2)) => 1)
       (fact (shape->precision (geohash 45 45 64)) => 63))

(facts "Geohashes covering a shape"
       (let [wkt [[70 30, 70 31, 71, 31, 71 30, 70 30]]
             geom (jts/polygon-wkt wkt)]
         (fact (map string (geohashes-intersecting geom 15)) => (just ["tt6" "tt9" "tt3" "ttd"]
                                                                      :in-any-order))))

(facts "Geohashes for dateline-crossing polygons"
       (let [polygon (jts/polygon-wkt [[179 0, 179 1, -179 1, -179 0, 179 0]])]
         (fact (map string (geohashes-intersecting polygon 15))
               => (just ["xbp" "800" "2pb" "rzz"] :in-any-order))))

(facts "Intersecting Geohashes for geometry crossing origin (equator and prime meridian)"
       (let [factory (GeometryFactory. (PrecisionModel.) 4326)
             envelope (Envelope. -2.0 2.0 -2.0 2.0)
             geom (.toGeometry factory envelope)]
         (fact (map string (geohashes-intersecting geom 10))
               => (just ["s0" "kp" "7z" "eb"] :in-any-order))))

(facts "Intersecting geohashes for multipolygon"
       (let [geom (jts/multi-polygon-wkt [[[70 30, 70 31, 71, 31, 71 30, 70 30]]
                                          [[-50 20, -50 21, -51, 21, -51 20, -50 20]]])]
         (fact (map string (geohashes-intersecting geom 15))
               => (just ["dgs" "tt6" "dge" "ttd" "tt3" "tt9"] :in-any-order))))

(facts "Neighbors"
       (map string (neighbors (geohash "u4pruyd"))) => ["u4pruyf" "u4pruyg" "u4pruye"
                                                        "u4pruy7" "u4pruy6" "u4pruy3"
                                                        "u4pruy9" "u4pruyc"])

(facts "Converting geohashes to shapes"
       (let [gh (geohash "9q5")]
         (spatial/area gh) => 2.0161507786744812E10))

(facts "Getting bounding geometries for geohashes"
       (let [gh (geohash "9q5")
             points [[-119.53125 33.75, -119.53125 35.15625, -118.125 35.15625, -118.125 33.75, -119.53125 33.75]]]
         (bbox-geom gh) => (jts/polygon-wkt points)
         (jts/get-srid (bbox-geom gh)) => 4326
         (jts/get-srid (spatial/to-jts gh)) => 4326
         (jts/get-srid (spatial/to-jts gh 1234)) => 1234))

(facts "Getting bounding Shapes for geohashes"
       (let [gh (geohash "9q5")]
         (bbox gh) => (.rect spatial/jts-earth -119.53125 -118.125 33.75 35.15625)
         (bbox gh) => (spatial/to-shape gh)))

(comment
  "intersecting-geohashes benchmarking"
  (let [sample-wkt "POLYGON((-107.814331054688 33.9746840624585,-107.63786315918 34.2560813847164,-107.405776977539 33.9991657910092,-107.814331054688 33.9746840624585))"
        sample-wkt-2 "POLYGON((-117.904586791992 34.9164815742019,-117.887420654297 34.9242230169058,-117.89201259613 34.9413219789948,-117.885317802429 34.9604925010402,-117.870254516602 34.9942850112354,-117.788200378418 34.9906286382738,-117.746658325195 34.9624972324491,-117.837295532227 34.9264749358464,-117.861843109131 34.9218302853154,-117.852573394775 34.918170678527,-117.869567871094 34.9083170799602,-117.850341796875 34.9004333495724,-117.862014770508 34.8802982472034,-117.86598443985 34.8796645450209,-117.87469625473 34.886265369791,-117.884159088135 34.8933936650348,-117.885360717773 34.9005741371092,-117.907333374023 34.8908592309128,-117.915058135986 34.8995886192834,-117.904586791992 34.9069093264736,-117.912397384644 34.9043049188956,-117.916774749756 34.9074724307645,-117.904586791992 34.9164815742019))"
        sample-geom (gio/read-wkt sample-wkt)
        sample-geom-2 (gio/read-wkt sample-wkt-2)
        big-geom (jts/polygon-wkt [[70 30, 70 31, 71, 31, 71 30, 70 30]])])
  (crit/with-progress-reporting (crit/bench (geohashes-intersecting sample-geom 35)))
  (crit/with-progress-reporting (crit/bench (geohashes-intersecting sample-geom-2 35)))
  (crit/with-progress-reporting (crit/bench (geohashes-intersecting big-geom 35))))
