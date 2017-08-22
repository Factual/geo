(ns geo.t-geohash
  (:require [geo.spatial :as spatial]
            [geo.jts :as jts])
  (:use midje.sweet geo.geohash)
  (:import (ch.hsr.geohash GeoHash)
           (org.locationtech.spatial4j.context SpatialContext)
           (com.vividsolutions.jts.geom PrecisionModel
                                        Envelope
                                        GeometryFactory)))

(facts "geohash"
       (fact (geohash 50 20 64) => (partial instance? GeoHash))
       (fact "from string"
             (geohash-center (geohash "u4pruydqqvj"))
             => (spatial/geohash-point 57.64911063015461 10.407439693808556)))

(facts "subdivide"
       (fact (map #(.longValue %) (subdivide (geohash "")))
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
       (let [wkt [[70 30, 70 31, 71, 31, 71 30, 70 30]]]
         (time (-> wkt jts/polygon-wkt (covering-geohashes 25)))
         (time (map string (-> wkt jts/polygon-wkt (geohashes-intersecting 25))))
         (fact (-> wkt jts/polygon-wkt (covering-geohashes 15)) => #{"tt6" "tt9" "tt3" "ttd"})))

(facts "Geohashes for dateline-crossing polygons"
       (let [polygon (jts/polygon-wkt [[179 0, 179 1, -179 1, -179 0, 179 0]])]
         (fact (covering-geohashes polygon 15)
               => (just ["xbp" "800" "2pb" "rzz"] :in-any-order))
         (fact (map string (geohashes-intersecting polygon 15))
               => (just ["xbp" "800" "2pb" "rzz"] :in-any-order))))

(facts "Intersecting Geohashes for geometry crossing origin (equator and prime meridian)"
       (let [factory (GeometryFactory. (PrecisionModel.) 4326)
             envelope (Envelope. -2.0 2.0 -2.0 2.0)
             geom (.toGeometry factory envelope)]
         (fact (map string (geohashes-intersecting geom 10))
               => (just ["s0" "kp" "7z" "eb"] :in-any-order))))

(facts "Neighbors"
       (map string (neighbors (geohash "u4pruyd"))) => ["u4pruyf" "u4pruyg" "u4pruye"
                                                        "u4pruy7" "u4pruy6" "u4pruy3"
                                                        "u4pruy9" "u4pruyc"])
