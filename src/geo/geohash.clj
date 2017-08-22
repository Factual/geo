(ns geo.geohash
  "Working with geohashes.


  "
  (:use geo.spatial)
  (:import (ch.hsr.geohash WGS84Point
                           GeoHash)
           (ch.hsr.geohash.util VincentyGeodesy)
           (org.locationtech.spatial4j.shape.impl RectangleImpl)
           (org.locationtech.spatial4j.context.jts JtsSpatialContext)
           (org.locationtech.spatial4j.io GeohashUtils)
           (org.locationtech.spatial4j.shape SpatialRelation)
           (org.locationtech.spatial4j.distance DistanceUtils)
           (org.locationtech.spatial4j.context SpatialContextFactory)))

(defn geohash
  "Creates a geohash from a string, or at the given point with the given bit
  precision."
  ([string]
   (GeoHash/fromGeohashString string))
  ([point precision]
   (geohash (latitude point)
            (longitude point)
            precision))
  ([lat long precision]
   (GeoHash/withBitPrecision lat long precision)))

(extend-protocol Shapelike
  GeoHash
  (to-shape [geohash]
            (let [box (.getBoundingBox geohash)]
              (.makeRectangle earth
                              (.getMinLon box)
                              (.getMaxLon box)
                              (.getMinLat box)
                              (.getMaxLat box)))))

(defn northern-neighbor [^GeoHash h] (.getNorthernNeighbour h))
(defn eastern-neighbor [^GeoHash h] (.getEasternNeighbour h))
(defn western-neighbor [^GeoHash h] (.getWesternNeighbour h))
(defn southern-neighbor [^GeoHash h] (.getSouthernNeighbour h))
(defn neighbors [^GeoHash h] (vec (.getAdjacent h)))

(defn subdivide
  "Given a geohash, returns all geohashes inside it, of a given precision."
  ([^GeoHash geohash]
   (subdivide geohash (inc (.significantBits geohash))))
  ([^GeoHash geohash precision]
   (let [number (Math/pow 2 (- precision
                               (.significantBits geohash)))]
     (->> (GeoHash/fromLongValue (.longValue geohash) precision)
       (iterate #(.next ^GeoHash %))
       (take number)))))

(defn square-ring
  "Given a geohash at the northeast corner of a square (n-2) geohashes on a
  side, returns a list of geohashes in a path around the square, such that the
  first entry in the list is the northeast corner of a square (n) geohashes on
  a side.

  O is the origin argument
  F is the first hash in the returned sequence
  L is the last hash in the returned sequence
  E represents the last *and* first in the sequence

  n=1   3      5        7
                     +----LF
             +--LF   |    O|
       +LF   |  O|   |     |
    E  |O|   |   |   |     |
       +-+   |   |   |     |
             +---+   |     |
                     +-----+

  If n is one, returns [origin].

  This algorithm is undefined at the poles."
  [origin n]
  (assert (odd? n))
  (assert (pos? n))
  (if (= n 1)
    ; Special case: if we're asked to return a 1x1 square, we'll return the
    ; origin argument.
    [origin]
    ; We build the list backwards by recurring from last to first,
    ; counterclockwise.
    (let [len   (* 4 (dec n))      ; Total sequence length
          west  0                  ; We start (at L), with i = 0, going west
          south (- n 2)            ; Turn south at i = n - 2
          east  (+ south (- n 1))  ; Turn east
          north (+ east  (- n 1))  ; Turn north
          end   (+ north n)]       ; Then stop (at F)
      (loop [ring (list)                       ; Accrued list
             i    0                            ; List length
             cell (northern-neighbor origin)]  ; Current hash
        (let [next-cell (condp <= i
                          end   nil
                          north (northern-neighbor cell)
                          east  (eastern-neighbor  cell)
                          south (southern-neighbor cell)
                          (western-neighbor cell))]
          (if (nil? next-cell)
            ring
            (recur (conj ring cell)
                   (inc i)
                   next-cell)))))))

(defn concentric-square-rings
  "Given a single geohash, returns a lazy sequence of concentric square rings
  of geohashes around it.  The first element is [hash], the second element is
  [northern-neighbor, northwest-neighbor, west-neighbor, ...], the third
  element is the list of all geohashes around *those*, and so on."
  ([origin]
   (concentric-square-rings origin 1))
  ([origin n]
   (let [ring (square-ring origin n)
         next-origin  (first ring)]
     (cons ring
           (lazy-seq (concentric-square-rings next-origin (+ n 2)))))))

(defn geohash-center
  "Returns the center point of a geohash."
  [^GeoHash geohash]
  (.getBoundingBoxCenterPoint geohash))

(defn geohash-midline-dimensions
  "Returns a vector of [lat-extent long-extent], where lat-extent is the length
  of the geohash through the midpoint of top and bottom, and long-extent is the
  length of the geohash through the midpoint of left and right sides. All
  figures in meters."
  [^GeoHash geohash]
  (let [box     (.getBoundingBox geohash)
        min-lat (.getMinLat box)
        max-lat (.getMaxLat box)
        min-long (.getMinLon box)
        max-long (.getMaxLon box)
        mean-lat (/ (+ min-lat max-lat) 2)
        mean-long (/ (+ min-long max-long) 2)]
    [(distance (geohash-point min-lat mean-long)
               (geohash-point max-lat mean-long))
     (distance (geohash-point mean-lat min-long)
               (geohash-point mean-lat max-long))]))

(defn geohash-midline-area
  "An estimate of a geohash's area, in square meters, based on its midline
  dimensions."
  [geohash]
  (apply * (geohash-midline-dimensions geohash)))

(defn geohash-error
  "Returns the error (i.e. the distance in meters between opposite corners) of
  the given geohash."
  [^GeoHash geohash]
  (let [box (.getBoundingBox geohash)]
    (distance (.getLowerRight box) (.getUpperLeft box))))

(defn geohash-max-error
  "Returns the maximum error (i.e. the distance between opposite corners of the
  geohash bounding box) for a given number of geohash bits. Geohashes are least
  precise at the equator."
  [bits]
  (geohash-error (geohash 0 0 bits)))

(defn string
  "Returns the base32 encoded string value of a geohash."
  [^GeoHash geohash]
  (.toBase32 geohash))

(defn significant-bits [^GeoHash geohash] (.significantBits geohash))
(defn character-precision [^GeoHash geohash] (.getCharacterPrecision geohash))

(def degrees-precision-long-cache
  (map (comp width (partial geohash 45 45)) (range 0 64)))
(def degrees-precision-lat-cache
  (map (comp height (partial geohash 45 45)) (range 0 64)))

(defn least-upper-bound-index
  "Given a sequence of numbers in descending order, finds the index of the
  largest number which is just greater than the target."
  [numbers target]
  (dec (count (take-while #(< target %) numbers))))

(defn shape->precision
  "Estimates the precision which generates geohash regions on the scale of the
  given shape."
  [shape]
  (min (least-upper-bound-index degrees-precision-lat-cache (height shape))
       (least-upper-bound-index degrees-precision-long-cache (height shape))))

(defn children [gh]
  (->> gh string GeohashUtils/getSubGeohashes (map geohash)))

(defn bbox-geom [geohash]
  (let [bbox (.getBoundingBox geohash)
        rect (RectangleImpl. (.getMinLon bbox)
                             (.getMaxLon bbox)
                             (.getMinLat bbox)
                             (.getMaxLat bbox)
                             JtsSpatialContext/GEO)]
    (.getGeometryFrom JtsSpatialContext/GEO rect)))

(defn geohashes-intersecting
  ([shape desired-level] (geohashes-intersecting shape desired-level desired-level))
  ([shape min-level max-level] (geohashes-intersecting shape min-level max-level (geohash "")))
  ([shape min-level max-level _]
   (loop [matches #{}
         queue (list (geohash ""))]
     (if (empty? queue) matches
         (let [current (first queue)
               level (significant-bits current)]

           (if (and (<= level max-level) (intersects? shape current))
             (if (= level max-level) (recur (conj matches current)
                                            (rest queue))
                 (if (>= level min-level)
                     (recur (conj matches current)
                            (into (rest queue)
                                  (children current)))
                     (recur matches
                            (into (rest queue)
                                  (children current)))))
             (recur matches (rest queue))))))))

(defn geohashes-near
  "Returns a list of geohashes of the given precision within radius meters of
  the given point."
  [point radius precision]
  (geohashes-intersecting (circle point radius) precision))
