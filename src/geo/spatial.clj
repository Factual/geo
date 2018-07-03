(ns geo.spatial
  "Provides common interfaces for working with spatial objects and geodesics.
  All units are in meters/radians/steradians unless otherwise specified.
  Provides static spatial contexts for the earth, and some constants like the
  earth's radii and circumferences, along with points like the poles.

  Defines protocols for unified access to Points and Shapes, to allow different
  geometry libraries to interoperate.

  Basic utility functions for unit conversion; e.g. degrees<->radians.

  Functions for computing earth radii, surface distances, and surface areas.

  Constructors for shapes like bounding-boxes and circles, and utility
  functions over shapes for their heights, centers, areas, etc. Can also
  compute relationships between shapes: their intersections, contains, disjoint
  statuses, etc."
  (:use [clojure.math.numeric-tower :only [abs]])
  (:require [geo.jts :as jts])
  (:import (ch.hsr.geohash WGS84Point GeoHash)
           (ch.hsr.geohash.util VincentyGeodesy)
           (com.uber.h3core.util GeoCoord)
           (org.locationtech.spatial4j.shape SpatialRelation
                                             Shape
                                             Rectangle)
           (org.locationtech.jts.geom Coordinate
                                      Geometry
                                      LinearRing
                                      Polygon)
           (org.locationtech.spatial4j.shape.impl GeoCircle PointImpl RectangleImpl)
           (org.locationtech.spatial4j.shape.jts JtsGeometry
                                                 JtsPoint
                                                 JtsShapeFactory)
           (org.locationtech.spatial4j.distance DistanceCalculator DistanceUtils)
           (org.locationtech.spatial4j.context SpatialContextFactory
                                               SpatialContext)
           (org.locationtech.spatial4j.context.jts JtsSpatialContext)))

(declare spatial4j-point)
(declare geohash-point)
(declare jts-point)
(declare h3-point)

(defn square [x]
  (Math/pow x 2))

(def ^SpatialContext earth
  "The SpatialContext of the earth, as according to spatial4j."
  (SpatialContextFactory/makeSpatialContext
   {"geo" "true"
    "datelineRule" "width180"
    "spatialContextFactory" "org.locationtech.spatial4j.context.jts.JtsSpatialContextFactory"
    "distCalculator" "vincentySphere"}
   (.getClassLoader JtsSpatialContext)))

(def ^JtsShapeFactory jts-earth
  "ShapeFactory for producing spatial4j Shapes from JTSGeometries based"
  (->> (.getClassLoader JtsSpatialContext)
       (SpatialContextFactory/makeSpatialContext
        {"geo" "true"
         "datelineRule" "width180"
         "spatialContextFactory" "org.locationtech.spatial4j.context.jts.JtsSpatialContextFactory"
         "distCalculator" "vincentySphere"})
       (.getShapeFactory)))

(def earth-mean-radius
  "Earth's mean radius, in meters."
  (* 1000 DistanceUtils/EARTH_MEAN_RADIUS_KM))

(def earth-equatorial-radius
  "Earth's radius at the equator, in meters."
  6378137.0)

(def earth-polar-radius
  "Earth's radius at the poles, in meters."
  6356752.3)

(def earth-equatorial-radius-squared
  (square earth-equatorial-radius))

(def earth-polar-radius-squared
  (square earth-polar-radius))

(def earth-mean-circumference
  "Earth's mean circumference, in meters."
  (* 1000 40041))

(def earth-equatorial-circumference
  "Earth's equatorial circumference, in meters"
  (* 1000 40075))

(def earth-meridional-circumference
  "Earth's circumference around a meridian, in meters."
  (* 1000 40008))

(defn crosses-dateline? [^Geometry jts-geom]
  (>= (.getWidth (.getEnvelopeInternal jts-geom))
      180))

(defprotocol Shapelike
  (^Shape to-shape [this] "Convert anything to a Shape.")
  (^Geometry to-jts [this] [this srid] "Convert anything to a projected JTS Geometry."))

(extend-protocol Shapelike
  GeoCircle
  (to-shape [this] this)
  (to-jts ([_] (throw (Exception. "Cannot cast GeoCircle to JTS.")))
          ([_ _] (throw (Exception. "Cannot cast GeoCircle to JTS."))))

  RectangleImpl
  (to-shape [this] this)
  (to-jts ([this] (jts/set-srid (.getGeom ^JtsGeometry this) jts/default-srid))
          ([this srid] (to-jts (to-jts this) srid)))

  PointImpl
  (to-shape [this] this)
  (to-jts ([this] (jts/set-srid (jts-point (.getY this) (.getX this)) jts/default-srid))
          ([this srid] (to-jts (to-jts this) srid)))

  JtsGeometry
  (to-shape [this] this)
  (to-jts ([this] (jts/set-srid (.getGeom this) jts/default-srid))
          ([this srid] (to-jts (to-jts this) srid)))

  JtsPoint
  (to-shape [this] this)
  (to-jts ([this] (jts/set-srid (jts-point (.getY this) (.getX this)) jts/default-srid))
          ([this srid] (to-jts (to-jts this) srid)))

  Geometry
  (to-shape [this] (.makeShape jts-earth (jts/transform-geom this jts/default-srid) true true))
  (to-jts ([this] this)
          ([this srid] (jts/transform-geom this srid)))

  GeoCoord
  (to-shape [this] (spatial4j-point this))
  (to-jts ([this] (jts-point this))
          ([this srid] (jts/transform-geom (to-jts this) srid))))

(defprotocol Polygonal
  (to-polygon [this] [this srid] "Ensure that an object is 2D, with lineal boundaries."))

(extend-protocol Polygonal
  GeoHash
  (to-polygon ([this] (to-jts this))
              ([this srid] (to-jts this srid)))

  RectangleImpl
  (to-polygon ([this] (to-jts this))
              ([this srid] (to-jts this srid)))

  Polygon
  (to-polygon ([this] this)
              ([this srid] (to-jts this srid)))

  LinearRing
  (to-polygon ([this] (jts/polygon this))
              ([this srid] (jts/polygon (jts/transform-geom this srid)))))

(defprotocol Point
  (latitude [this])
  (longitude [this])
  (to-spatial4j-point [this])
  (to-geohash-point [this])
  (to-h3-point [this]))

(extend-protocol Point
  WGS84Point
  (latitude [this] (.getLatitude this))
  (longitude [this] (.getLongitude this))
  (to-spatial4j-point [this] (spatial4j-point this))
  (to-geohash-point [this] this)
  (to-h3-point [this] (h3-point this))

  org.locationtech.jts.geom.Point
  (latitude [this] (.getY (jts/transform-geom this jts/default-srid)))
  (longitude [this] (.getX (jts/transform-geom this jts/default-srid)))
  (to-spatial4j-point [this] (spatial4j-point this))
  (to-geohash-point [this] (geohash-point this))
  (to-h3-point [this] (h3-point this))

  org.locationtech.jts.geom.Coordinate
  (latitude [this] (.y this))
  (longitude [this] (.x this))
  (to-spatial4j-point [this] (spatial4j-point this))
  (to-geohash-point [this] (geohash-point this))
  (to-h3-point [this] (h3-point this))

  org.locationtech.spatial4j.shape.Point
  (latitude [this] (.getY this))
  (longitude [this] (.getX this))
  (to-spatial4j-point [this] this)
  (to-geohash-point [this] (geohash-point this))
  (to-h3-point [this] (h3-point this))

  com.uber.h3core.util.GeoCoord
  (latitude [this] (.lat this))
  (longitude [this] (.lng this))
  (to-spatial4j-point [this] (spatial4j-point this))
  (to-geohash-point [this] (geohash-point this))
  (to-h3-point [this] this))

(defn degrees->radians
  [degrees]
  (DistanceUtils/toRadians degrees))

(defn radians->degrees
  [radians]
  (DistanceUtils/toDegrees radians))

(defn earth-radius
  "Returns an approximate radius for the earth, at some point. Based on the
  geodetic model for an oblate spheroid."
  [point]
  (let [l   (degrees->radians (latitude point))
        a   earth-equatorial-radius
        a2  earth-equatorial-radius-squared
        b   earth-polar-radius
        b2  earth-polar-radius-squared
        cos (Math/cos l)
        sin (Math/sin l)]
    (Math/sqrt
      (/ (+ (square (* a2 cos))
            (square (* b2 sin)))
         (+ (square (* a cos))
            (square (* b sin)))))))

(defn distance->radians
  "Converts distance, in meters on the surface of the earth, to radians.
  Assumes earth mean radius."
  ([meters]
   (distance->radians meters earth-mean-radius))
  ([meters radius]
   (DistanceUtils/dist2Radians meters radius)))

(defn distance-at-point->radians
  "Converts a distance near a point on the earth into radians; using a more
  accurate model of the radius of the earth near that point."
  [meters point]
  (distance->radians meters (earth-radius point)))

(defn radians->distance
  "Converts radians to meter distance on the surface of the earth. Assumes
  earth mean radius."
  ([radians]
   (radians->distance radians earth-mean-radius))
  ([radians radius]
   (DistanceUtils/radians2Dist radians radius)))

(def square-degree-in-steradians
  (/ (* 180 180) (* Math/PI Math/PI)))

(defn square-degrees->steradians
  [steradians]
  (/ steradians square-degree-in-steradians))

(defn jts-point
  "Returns a Point used by JTS."
  ([point]
   (jts/point (latitude point) (longitude point)))
  ([lat long]
   (jts/point lat long)))

(defn steradians->area
  "Converts steradians to square meters on the surface of the earth. Assumes
  earth mean radius."
  ([steradians]
   (steradians->area steradians earth-mean-radius))
  ([steradians radius]
   (* steradians (square radius))))

(defn spatial4j-point
  "A spatial4j point on the earth."
  ([point]
   (.makePoint earth (longitude point) (latitude point)))
  ([lat long]
   (.makePoint earth long lat)))

(defn geohash-point
  "Returns a WGS84Point used by the geohash library."
  ([point]
   (WGS84Point. (latitude point) (longitude point)))
  ([lat long]
   (WGS84Point. lat long)))

(defn h3-point
  "Returns a GeoCoord used by the H3 library."
  ([point]
   (GeoCoord. (latitude point) (longitude point)))
  ([lat long]
   (GeoCoord. lat long)))

(def point spatial4j-point)

(def north-pole (spatial4j-point 90 0))
(def south-pole (spatial4j-point -90 0))

(defn circle
  "A spatial4j circle around the given point or lat,long, with radius in
  meters."
  ([point meters]
   ; GeoCircle takes its radius in degrees, so we need to figure out how many
   ; degrees to use. For anything under 100 kilometers we use a local
   ; approximation; for bigger stuff we use the mean radius.
   (let [point   (to-spatial4j-point point)
         radians (if (< 1e6 meters)
                   (distance->radians meters)
                   (distance-at-point->radians meters point))
         degrees (radians->degrees radians)]
     (.makeCircle earth point degrees))))

(defn distance
  "Distance between two points, in meters"
  [a b]
  ; There's a singularity in the geohash library's distance calculation
  ; algorithm, used here, which causes distances near the poles to return NaN.
  (assert (not= 90.0 (abs (latitude a))))
  (assert (not= 90.0 (abs (latitude b))))
  (VincentyGeodesy/distanceInMeters
   (to-geohash-point a)
   (to-geohash-point b)))

(defn distance-in-degrees
  "Distance between two points, in degrees."
  [a b]
  (-> earth
    .getDistCalc
    (.distance (to-spatial4j-point a)
               (to-spatial4j-point b))))

(defn bounding-box
  "Returns the bounding box of any shape."
  ^org.locationtech.spatial4j.shape.Rectangle [shape]
  (.getBoundingBox (to-shape shape)))

(defn center
  "Returns the centroid of a spatial4j shape. Note that .getCenter does bad
  things for JTS shapes that cross the international dateline, so we use use
  (center (bounding-box x)) for JTS stuff."
  [shape]
  (let [shape (to-shape shape)]
    (if (instance? JtsGeometry shape)
      (.getCenter (bounding-box shape))
      (.getCenter (to-shape shape)))))

(defn height
  "Returns the height of a shape, in degrees."
  [shape]
  (-> shape bounding-box .getHeight))

(defn width
  "Returns the width of a shape, in degrees."
  [shape]
  (-> shape bounding-box .getWidth))

(defn area-in-square-degrees
  "The area of a rectangle in square degrees."
  [rect]
  (.area (.getDistCalc earth)
         ^Rectangle (to-shape rect)))

(defn area
  "The area of a rectangle in square meters. Note that spatial4j's term 'area'
  actually refers to solid angle, not area; we convert by multiplying by the
  earth's radius at the midpoint of the rectangle."
  [rect]
  (-> rect
      area-in-square-degrees
      square-degrees->steradians
      steradians->area))

(defn relate
  "The relationship between two shapes. Returns a keyword:

  :contains    a contains b
  :within      a falls within b
  :intersects  a and b have at least one point in common
  :disjoint    a and b have no points in common"
  [a b]
  (condp = (.relate (to-shape a) (to-shape b))
    SpatialRelation/DISJOINT    :disjoint
    SpatialRelation/INTERSECTS  :intersects
    SpatialRelation/WITHIN      :within
    SpatialRelation/CONTAINS    :contains))

(defn intersects?
  "Do two shapes intersect in any way? Note that spatial4j's relate() considers
  intersection *different* from containment, e.g. if A completely surrounds B,
  their relation is not INTERSECTS. Spatial4j has a intersects() function on
  relations (the one used here) which considers two shapes intersecting if
  their intersection is non-empty; i.e. they are not disjoint."
  [a b]
  (.intersects (.relate (to-shape a) (to-shape b))))

(defn dist-at-idx
  "Distance between the linestring's point at the given index and the subsequent point."
  [linestring idx]
  (distance (jts/point-n linestring idx)
            (jts/point-n linestring (inc idx))))

(defn length
  "Get geodesic length of a (jts) linestring by summing lengths of successive points"
  [^org.locationtech.jts.geom.LineString linestring]
  (let [num-points (.getNumPoints linestring)]
    (if (= 0 num-points)
      0
      (loop [length 0.0
             idx 0]
        (if (= idx (dec num-points))
          length
          (recur (+ length (dist-at-idx linestring idx))
                 (inc idx)))))))

(defn within-dist? [p1 p2 dist]
  (<= (distance p1 p2) dist))

(defn point-between [^Coordinate c1 ^Coordinate c2 dist]
  (let [ratio (/ dist (distance c1 c2))
        segment (org.locationtech.jts.geom.LineSegment. c1 c2)]
    (.pointAlongOffset segment ratio 0)))

(defn- coord-list-length [coords]
  (if (< (count coords) 2)
    0
    (length (jts/linestring coords))))

(defn- cut-point [current-segment next-coord segment-length]
  (let [current-length (coord-list-length current-segment)
        shortfall (- segment-length current-length)]
    (point-between (last current-segment) next-coord shortfall)))

(defn- under-cap-with-next-point? [coords next-coord dist]
  (< (length (jts/linestring (conj coords next-coord))) dist))

(defn- resegment-wgs84
  "Performs the resegment operation used in (resegment),
   with the assumption that a linestring is in WGS84 projection"
  [linestring segment-length]
  (loop [coords (jts/coords linestring)
         segments []
         current []]
    (let [[next & remaining] coords]
      (cond
        (empty? coords) (map jts/linestring (conj segments current))
        (empty? current) (recur remaining segments (conj current next))
        (under-cap-with-next-point? current next segment-length) (recur remaining segments (conj current next))
        :else (let [cut-point (cut-point current next segment-length)]
                (recur coords
                       (conj segments (conj current cut-point))
                       [cut-point]))))))

(defn resegment
  "Repartitions a JTS LineString into multiple contiguous linestrings, each up to the
   provided length (in meters). Final segment may be less than the requested length.
   Length of individual segments may vary a bit but total length should remain the same."
  [linestring segment-length]
  (let [srid (jts/get-srid linestring)]
    (map #(jts/transform-geom % srid) (resegment-wgs84 (jts/transform-geom linestring 4326) segment-length))))

(def ^DistanceCalculator vincenty-distance-calculator (org.locationtech.spatial4j.distance.GeodesicSphereDistCalc$Vincenty.))

(defn rand-point-in-radius
  "Get a random point around the given latitude and longitude within the given radius.

  (rand-point-in-radius 34.05656 -118.41881 100)
  (rand-point-in-radius 34.05656 -118.41881 100 :clustered)
  (rand-point-in-radius 34.05656 -118.41881 100 (fn [] 1))

  Returns org.locationtech.spatial4j.shape.jts.JtsPoint; Use geo.spatial/latitude and geo.spatial/longitude
  to retrieve raw coords.

  Accepts an optional 4th argument for customizing the distribution. Can be either :uniform or :clustered
  for built-in distributions, or a custom fn.

  Distribution fn should return a float between 0.0 and 1.0 when invoked.

  The built-in :clustered distribution uses a linear distribution of radius, which results in points
  clustered more heavily toward the center of the radius.

  :uniform uses an exponential distribution of radius which results in points being spread evenly across
  the circle.

  Default distribution is :uniform."
  ([center-lat center-lon radius-meters]
   (rand-point-in-radius center-lat center-lon radius-meters :uniform))
  ([center-lat center-lon radius-meters distribution]
   (assert (or (= :uniform distribution)
               (= :clustered distribution)
               (fn? distribution))
           "distribution must be :uniform, :clustered, or fn")
   (let [center (point center-lat center-lon)
         offset-fn (case distribution
                     :uniform (fn [] (Math/sqrt (rand)))
                     :clustered rand
                     distribution)
         radius (* (offset-fn) radius-meters)
         offset-degrees (-> radius
                            (distance-at-point->radians center)
                            (radians->degrees))
         angle-degrees (rand 360)]
     (.pointOnBearing vincenty-distance-calculator
                      center
                      offset-degrees
                      angle-degrees
                      earth
                      nil))))
