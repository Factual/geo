(ns geo.h3
  "Working with H3."
  (:require [geo.spatial :as spatial]
            [geo.jts :as jts])
  (:import (org.locationtech.jts.geom Geometry)
           (com.uber.h3core H3Core)
           (geo.spatial Shapelike)))

(def h3-inst (H3Core/newInstance))

(defn long->string
  "Convert a long representation of an H3 cell to a string."
  [^Long h]
  (.h3ToString h3-inst h))

(defn string->long
  "Convert a string representation of an H3 cell to a long."
  [^String h]
  (.stringToH3 h3-inst h))

(defn pt->h3
  "Return the index of the resolution 'res' cell that a point or lat/lng pair is contained within."
  ([pt res]
   (pt->h3 (spatial/latitude pt) (spatial/longitude pt) res))
  ([lat lng res]
   (.geoToH3Address h3-inst lat lng res)))

(defn h3->pt
  "Return a GeoCoord of the center point of a cell."
  [h]
  (.h3ToGeo h3-inst h))

(defn get-resolution
  "Return the resolution of a cell."
  [h]
  (.h3GetResolution h3-inst h))

(defn- geo-boundary
  "Given an H3 identifier, return a List of GeoCoords for that cell's boundary"
  [h]
  (.h3ToGeoBoundary h3-inst h))

(defn jts-boundary
  "Given an H3 identifier, return a LinearRing of that cell's boundary."
  [h]
  (as-> h v
        (geo-boundary v)
        (into [] v)
        (conj v (first v))
        (map #(jts/coordinate (spatial/longitude %) (spatial/latitude %)) v)
        (jts/linear-ring v)))

(defn geo-coords
  "Return all coordinates for a given Shapelike as GeoCoords"
  [^Shapelike s]
  (map spatial/h3-point (jts/coordinates (spatial/to-jts s))))

(defn polyfill
  "Return all resolution 'res' cells that cover a given Shapelike, excluding internal holes."
  [^Shapelike s ^Integer res]
  (let [s (spatial/to-jts s jts/default-srid)
        num-interior-rings (.getNumInteriorRing s)
        ext-ring (.getExteriorRing s)
        int-rings (map #(.getInteriorRingN s %) (range num-interior-rings))]
    (.polyfillAddress h3-inst (geo-coords ext-ring) (map geo-coords int-rings) res)))

(defn compact
  "Given a set of H3 cells, return a compacted set of cells, at possibly coarser resolutions."
  [cells]
  (cond (number? (first cells))
        (.compact h3-inst cells)
        (string? (first cells))
        (.compactAddress h3-inst cells)))

(defn uncompact
  "Given a set of H3 cells, return an uncompacted set of cells to a certain resolution."
  [cells res]
  (cond (number? (first cells))
        (.uncompact h3-inst cells res)
        (string? (first cells))
        (.uncompactAddress h3-inst cells res)))
