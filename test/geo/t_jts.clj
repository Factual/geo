(ns geo.t-jts
  (:use midje.sweet
        geo.jts)
  (:import (com.vividsolutions.jts.geom Coordinate)))

(facts "coordinate"
       (fact (coordinate 1 2) => (Coordinate. 1 2)))

(facts "polygon"
       (fact (->> [0 0 10 0 10 10 0 0]
               (partition 2)
               (map (partial apply coordinate))
               linear-ring
               polygon
               str)
             => "POLYGON ((0 0, 10 0, 10 10, 0 0))"))

(facts "multipolygon-wkt"
       (fact (str (multi-polygon-wkt [[[10 10, 110 10, 110 110, 10 110, 10 10],
                                       [20 20, 20 30, 30 30, 30 20, 20 20],
                                       [40 20, 40 30, 50 30, 50 20, 40 20]]]))
             => "MULTIPOLYGON (((10 10, 110 10, 110 110, 10 110, 10 10), (20 20, 20 30, 30 30, 30 20, 20 20), (40 20, 40 30, 50 30, 50 20, 40 20)))"))

(facts "making line-strings"
       (.getNumPoints (line-string-wkt [0 0 0 1 0 2])) => 3)
