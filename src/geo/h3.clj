(ns geo.h3
  "Working with H3."
  (:require [geo.spatial :as spatial]
            [geo.jts :as jts]
            [clojure.walk :as walk])
  (:import (ch.hsr.geohash GeoHash)
           (com.uber.h3core H3Core)
           (com.uber.h3core.util GeoCoord)
           (geo.spatial Point Shapelike)
           (org.locationtech.jts.geom LinearRing MultiPolygon Polygon)
           (org.locationtech.spatial4j.shape.impl RectangleImpl)))

(def ^H3Core h3-inst (H3Core/newInstance))

(defn- long->string
  "Convert a long representation of an H3 cell to a string."
  [^Long h]
  (.h3ToString h3-inst h))

(defn- string->long
  "Convert a string representation of an H3 cell to a long."
  [^String h]
  (.stringToH3 h3-inst h))

(defn- h3->pt-long
  "Long helper to return a GeoCoord of the center point of a cell."
  [^Long h]
  (.h3ToGeo h3-inst h))

(defn- h3->pt-string
  "String helper to return a GeoCoord of the center point of a cell."
  [^String h]
  (.h3ToGeo h3-inst h))

(defn- get-resolution-string
  "String helper to return the resolution of a cell."
  [^String h]
  (.h3GetResolution h3-inst h))

(defn get-resolution-long
  "Long helper to return the resolution of a cell."
  [^Long h]
  (.h3GetResolution h3-inst h))

(defn- k-ring-string
  "String helper to return a list of neighboring indices in all directions for 'k' rings."
  [^String h ^Integer k]
  (.kRing h3-inst h k))

(defn- k-ring-long
  "Long helper to return a list of neighboring indices in all directions for 'k' rings."
  [^Long h ^Integer k]
  (.kRing h3-inst h k))

(defn- k-ring-distances-string
  "String helper to return a list of neighboring indices in all directions for 'k' rings,
  ordered by distance from the origin index."
  [^String h ^Integer k]
  (.kRingDistances h3-inst h k))

(defn- k-ring-distances-long
  "String helper to return a list of neighboring indices in all directions for 'k' rings,
  ordered by distance from the origin index."
  [^Long h ^Integer k]
  (.kRingDistances h3-inst h k))

(defn- to-jts-common
  "Convert a geo boundary to JTS Polygon."
  [g]
  (as-> g v
        (into [] v)
        (conj v (first v))
        (map #(jts/coordinate (spatial/longitude %) (spatial/latitude %)) v)
        (jts/linear-ring v)
        (jts/polygon v)))

(defn- to-jts-string
  "String helper for: given an H3 identifier, return a Polygon of that cell."
  [^String h]
  (to-jts-common (.h3ToGeoBoundary h3-inst h)))

(defn- to-jts-long
  "Long helper for: given an H3 identifier, return a Polygon of that cell."
  [^Long h]
  (to-jts-common (.h3ToGeoBoundary h3-inst h)))

(defn- edge-string
  "String helper for: given both 'from' and 'to' cells, get a unidirectional edge index."
  [^String from ^String to]
  (.getH3UnidirectionalEdge h3-inst from to))

(defn- edge-long
  "Long helper for: given both 'from' and 'to' cells, get a unidirectional edge index."
  [^Long from ^Long to]
  (.getH3UnidirectionalEdge h3-inst from to))

(defn- edge-origin-string
  "String helper for: given a unidirectional edge, get its origin."
  [^String edge]
  (.getOriginH3IndexFromUnidirectionalEdge h3-inst edge))

(defn- edge-origin-long
  "Long helper for: given a unidirectional edge, get its origin."
  [^Long edge]
  (.getOriginH3IndexFromUnidirectionalEdge h3-inst edge))

(defn- edge-destination-string
  "String helper for: given a unidirectional edge, get its destination."
  [^String edge]
  (.getDestinationH3IndexFromUnidirectionalEdge h3-inst edge))

(defn- edge-destination-long
  "Long helper for: given a unidirectional edge, get its destination."
  [^Long edge]
  (.getDestinationH3IndexFromUnidirectionalEdge h3-inst edge))

(defn- edges-string
  "String helper to get all edges originating from an index."
  [^String cell]
  (into [] (.getH3UnidirectionalEdgesFromHexagon h3-inst cell)))

(defn- edges-long
  "Long helper to get all edges originating from an index."
  [^Long cell]
  (into [] (.getH3UnidirectionalEdgesFromHexagon h3-inst cell)))

(defn- edge-boundary-string
  "String helper to get coordinates representing the edge."
  [^String edge]
  (into [] (.getH3UnidirectionalEdgeBoundary h3-inst edge)))

(defn- edge-boundary-long
  "Long helper to get coordinates representing the edge."
  [^Long edge]
  (into [] (.getH3UnidirectionalEdgeBoundary h3-inst edge)))

(defn- pentagon?-string
  "String helper to check if an index is a pentagon"
  [^String cell]
  (.h3IsPentagon h3-inst cell))

(defn- pentagon?-long
  "Long helper to check if an index is a pentagon"
  [^Long cell]
  (.h3IsPentagon h3-inst cell))

(defn- is-valid?-string
  "String helper to check if an index is valid"
  [^String cell]
  (.h3IsValid h3-inst cell))

(defn- is-valid?-long
  "Long helper to check if an index is valid"
  [^Long cell]
  (.h3IsValid h3-inst cell))

(defn- neighbors?-string
  "String helper to check if cells are neighbors"
  [^String c1 ^String c2]
  (.h3IndexesAreNeighbors h3-inst c1 c2))

(defn- neighbors?-long
  "String helper to check if cells are neighbors"
  [^Long c1 ^Long c2]
  (.h3IndexesAreNeighbors h3-inst c1 c2))

(defprotocol H3Index
  (to-string [this] "Return index as a string.")
  (to-long [this] "Return index as a long.")
  (h3->pt [this] "Return a GeoCoord of the center point of a cell.")
  (get-resolution [this] "Return the resolution of a cell.")
  (k-ring [this k] "Return a list of neighboring indices in all directions for 'k' rings.")
  (k-ring-distances [this k] "Return a list of neighboring indices in all directions for 'k' rings, ordered by distance from the origin index.")
  (to-jts [this] "Given an H3 identifier, return a Polygon of that cell.")
  (edge [from to] "Given both 'from' and 'to' cells, get a unidirectional edge index.")
  (edge-origin [this] "Given a unidirectional edge, get its origin.")
  (edge-destination [this] "Given a unidirectional edge, get its destination.")
  (edges [this] "Get all edges originating from an index.")
  (edge-boundary [this] "Get coordinates representing the edge.")
  (pentagon? [this] "Check if an index is a pentagon.")
  (is-valid? [this] "Check if an index is valid.")
  (neighbors? [this cell] "Check if two indexes are neighbors."))

(extend-protocol H3Index
  String
  (to-string [this] this)
  (to-long [this] (string->long this))
  (h3->pt [this] (h3->pt-string this))
  (get-resolution [this] (get-resolution-string this))
  (k-ring [this k] (k-ring-string this k))
  (k-ring-distances [this k] (k-ring-distances-string this k))
  (to-jts [this] (to-jts-string this))
  (edge [from to] (edge-string from to))
  (edge-origin [this] (edge-origin-string this))
  (edge-destination [this] (edge-destination-string this))
  (edges [this] (edges-string this))
  (edge-boundary [this] (edge-boundary-string this))
  (pentagon? [this] (pentagon?-string this))
  (is-valid? [this] (is-valid?-string this))
  (neighbors? [this cell] (neighbors?-string this cell))

  Long
  (to-string [this] (long->string this))
  (to-long [this] this)
  (h3->pt [this] (h3->pt-long this))
  (get-resolution [this] (get-resolution-long this))
  (k-ring [this k] (k-ring-long this k))
  (k-ring-distances [this k] (k-ring-distances-long this k))
  (to-jts [this] (to-jts-long this))
  (edge [from to] (edge-long from to))
  (edge-origin [this] (edge-origin-long this))
  (edge-destination [this] (edge-destination-long this))
  (edges [this] (edges-long this))
  (edge-boundary [this] (edge-boundary-long this))
  (pentagon? [this] (pentagon?-long this))
  (is-valid? [this] (is-valid?-long this))
  (neighbors? [this cell] (neighbors?-long this cell)))

(defprotocol Polygonal
  (to-polygon [this] [this srid] "Ensure that an object is 2D, with lineal boundaries.")
  (polyfill [this res] "Return all resolution 'res' cells that cover a given Shapelike, excluding internal holes."))

(declare polyfill-p)
(declare polyfill-mp)

(extend-protocol Polygonal
  GeoHash
  (to-polygon ([this] (spatial/to-jts this))
    ([this srid] (spatial/to-jts this srid)))
  (polyfill [this res] (polyfill-p this res))

  RectangleImpl
  (to-polygon ([this] (spatial/to-jts this))
    ([this srid] (spatial/to-jts this srid)))
  (polyfill [this res] (polyfill-p this res))

  Polygon
  (to-polygon ([this] this)
    ([this srid] (spatial/to-jts this srid)))
  (polyfill [this res] (polyfill-p this res))

  LinearRing
  (to-polygon ([this] (jts/polygon this))
    ([this srid] (jts/polygon (jts/transform-geom this srid))))
  (polyfill [this res] (polyfill-p this res))

  MultiPolygon
  (to-polygon ([this] this)
    ([this srid] (spatial/to-jts this srid)))
  (polyfill [this res] (polyfill-mp this res)))

(defn pt->h3
  "Return the index of the resolution 'res' cell that a point or lat/lng pair is contained within."
  ([^Point pt ^Integer res]
   (pt->h3 (spatial/latitude pt) (spatial/longitude pt) res))
  ([^Double lat ^Double lng ^Integer res]
   (.geoToH3Address h3-inst lat lng res)))

(defn geo-coords
  "Return all coordinates for a given Shapelike as GeoCoords"
  [^Shapelike s]
  (map spatial/h3-point (jts/coordinates (spatial/to-jts s))))

(defn- polyfill-p
  "Polygon helper to return all resolution 'res' cells that cover a given shape,
  excluding internal holes."
  [s ^Integer res]
  (let [s (to-polygon s jts/default-srid)
        num-interior-rings (.getNumInteriorRing ^Polygon s)
        ext-ring (.getExteriorRing ^Polygon s)
        int-rings (map #(.getInteriorRingN ^Polygon s %) (range num-interior-rings))]
    (.polyfillAddress h3-inst (geo-coords ext-ring) (map geo-coords int-rings) res)))

(defn- polyfill-mp
  "Multipolygon helper to return all resolution 'res' cells that cover a given shape,
   excluding internal holes."
  [mp ^Integer res]
  (let [pf-polys (fn [p] (mapcat #(polyfill-p % res) p))]
    (into [] (-> mp
                 jts/polygons
                 pf-polys
                 flatten))))

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

(defn- geocoord-array-wkt
  "Create a wkt-style data structure from a collection of GeoCoords."
  [coords]
  (->> coords
       (map (fn [coord] [(spatial/longitude coord) (spatial/latitude coord)]))
       flatten
       vec))

(defn- geocoord-multi-helper
  "Helper function to pass to postwalk for multi-polygon generators."
  [v]
  (if (instance? GeoCoord (first v))
    (geocoord-array-wkt v)
    v))


(defn- multi-polygon-n
  "Multi-polygon generator for numbers"
  [cells]
  (as-> cells v
        (.h3SetToMultiPolygon h3-inst v true)
        (mapv #(into [] %) v)
        (walk/postwalk geocoord-multi-helper v)
        (jts/multi-polygon-wkt v)))

(defn- multi-polygon-s
  "Multi-polygon generator for strings"
  [cells]
  (as-> cells v
        (.h3AddressSetToMultiPolygon h3-inst v true)
        (mapv #(into [] %) v)
        (walk/postwalk geocoord-multi-helper v)
        (jts/multi-polygon-wkt v)))

(defn multi-polygon
  "Given a contiguous set of H3 cells, return a JTS MultiPolygon."
  [cells]
  (cond (number? (first cells))
        (multi-polygon-n cells)
        (string? (first cells))
        (multi-polygon-s cells)))
