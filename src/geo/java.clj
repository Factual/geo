(ns geo.java
  (:import (org.locationtech.spatial4j.shape Shape))
  (:gen-class
   :name com.factual.geo.Java
   :methods[#^{:static true} [toShape [geo.spatial.Shapelike] org.locationtech.spatial4j.shape.Shape]
            #^{:static true} [geohashToShape [ch.hsr.geohash.GeoHash] org.locationtech.spatial4j.shape.Shape]
            #^{:static true} [geohashToShape [ch.hsr.geohash.GeoHash] org.locationtech.spatial4j.shape.Shape]])

  (:require [geo.spatial :as spatial]
            [geo.geohash :as gh]
            [geo.h3 :as h3]
            [geo.io :as gio])
  )

;; Seems not to work:
(defn -toShape ^org.locationtech.spatial4j.shape.Shape [^geo.spatial.Shapelike shapelike]
  (spatial/to-shape shapelike))
;; Does work:
(defn -geohashToShape ^org.locationtech.spatial4j.shape.Shape [^ch.hsr.geohash.GeoHash gh]
  (spatial/to-shape gh))
