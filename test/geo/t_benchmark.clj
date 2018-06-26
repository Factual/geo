(ns geo.t-benchmark
  (:require [geo.jts :refer :all]
            [midje.sweet :refer [fact facts throws roughly truthy]])
  (:import (org.locationtech.jts.geom Coordinate)))


(facts "benchmark linestring"
       (time
        (dotimes [_ 100000]
          (linestring [(coordinate 30 70) (coordinate 31 71)] 4326))))
; "Elapsed time: 272.940221 msecs"
; "Elapsed time: 35.859359 msecs"
