(ns geo.poly
  "Polygon operations: primarily intersection testing, point-contains,
  polygon->edges, bounding boxes, normal vectors, linear coordinates,
  bisections, box intersections, bounded-space partitioning, and so on.")

(def EPSILON 1.0e-10)

(defn region-contains?
  "Returns true if the bounded region contains the point. The region need not
  be convex, and its edges may intersect. It must contain at least three
  points. Algorithm from
  http://stackoverflow.com/questions/217578/point-in-polygon-aka-hit-test.

  This would normally be written as a reduction, but the first/last points wrap
  around."

  [lat lng points]
  (loop [[plat plng & others] points
         [qlat qlng]          (drop (- (count points) 2) points)
         inside?              false]
    (let [crosses?    (not= (> plat lat) (> qlat lat))
          intersects? (and crosses?
                           (< lng (+ plng (* (- qlng plng) (/ (- lat plat) (- qlat plat))))))
          inside?'    (if intersects? (not inside?) inside?)]
      (if (empty? others)
        inside?'
        (recur others [plat plng] inside?')))))

(defn single-poly-contains?
  "Uses edge intersection testing to determine whether a single bounded shape
  contains the given point."
  [lat lng [include & exclude]] (and (region-contains? lat lng include)
                                     (not (some (partial region-contains? lat lng) exclude))))

(defn poly-contains?
  "Used for implicitly-closed polys. Count the number of side-intersections for a ray extending from
  [lat, ∞]; if even, the point is outside.

  poly should be of this form:

  [[[lat1 lng1 lat2 lng2 ... latk lngk] [latk+1 lngk+1 ... ]]   ; one poly with a hole
   [[latj lngj latj+1 lngj+1 ...] ...]                          ; another poly with a hole
   ...]

  Specifically:

  [[[& include] [& exclude] [& exclude] ...]
   [[& include] [& exclude] ...]
   ...]

  The toplevel array is considered to be the union of its elements."

  [lat lng poly] (some (partial single-poly-contains? lat lng) poly))

(defn region-bounding-box
  "Returns a bounding box for the given region. The bounding box is returned as [[minlat minlng]
  [maxlat maxlng]]."
  [region] (let [ps (partition 2 region)]
             [[(apply min (map first ps)) (apply min (map second ps))]
              [(apply max (map first ps)) (apply max (map second ps))]]))

(defn region-edges
  "Returns a list of [[lat1 lng1] [lat2 lng2]], each of which describes an edge of the region. The
  region is implicitly closed."
  [region] (let [region-points (partition 2 region)]
             (cons [(last region-points) (first region-points)]
                   (partition 2 1 region-points))))

(defn dot   [[x1 y1] [x2 y2]]        (+ (* x1 x2) (* y1 y2)))
(defn dist² [v]                      (dot v v))
(defn minus [[x1 y1] [x2 y2]]        [(- x1 x2) (- y1 y2)])
(defn plus* [[x y]   [dx dy] factor] [(+ x (* factor dx)) (+ y (* factor dy))])

(defn normal
  "Returns a normal vector for the given line. You can then take the dot product of points against
  this vector to see which side of the line they are on."
  [[x1 y1] [x2 y2]] [(- y2 y1) (- x1 x2)])

(defn positive-side?
  "Returns true if the given point is on the \"positive side\" of the line. Positive and negative
  sides are deterministic and consistent, so you can use this with a line to partition space."
  [[p1 p2] p] (let [n (normal p1 p2)
                    d (minus p p1)]
                (pos? (dot n d))))

(defn edges-intersect?
  "Returns true if two edges intersect."
  [[p11 p12 :as e1] [p21 p22 :as e2]]
  (and (not= (positive-side? e1 p21) (positive-side? e1 p22))
       (not= (positive-side? e2 p11) (positive-side? e2 p12))))

(defn edge-intersection
  "Returns the point of intersection of two edges. Behavior is undefined if the edges do not
  intersect (use edges-intersect? to test), or if the two lines are parallel."
  [[p11 p12] [p21 p22]] (let [shift               (minus p21 p11)
                              [d1 d2]             [(minus p12 p11) (minus p22 p21)]
                              [[a c] [b d] [e f]] [d1 d2 shift]
                              factor              (/ 1.0 (- (* a d) (* b c)))
                              t                   (* factor (- (* e d) (* b f)))]
                          (plus* p11 d1 t)))

(defn split-edge
  "Takes a line and an edge, and returns two new edges, the first of which is on the positive side
  of the line and the second of which is on the negative side. The line and the edge must
  intersect."
  [line [p1 p2 :as edge]] (let [intersection (edge-intersection line edge)]
                            (if (positive-side? line p1)
                              [[p1 intersection] [intersection p2]]
                              [[p2 intersection] [intersection p1]])))

(defn degenerate?
  "Returns true if the edge's points are identical (i.e. the edge has no length)."
  [p1 p2] (< (dist² (minus p1 p2)) EPSILON))

(defn line-contains?
  "Returns true if a line contains a point."
  [[p1 p2] p] (let [n (normal p1 p2)
                    d (minus p p1)]
                (< (Math/abs (dot n d)) EPSILON)))

(defn line-coordinate
  "Returns the line-coordinate of the given point. Works best when the point is on the line."
  [[p1 p2] p] (dot p (minus p2 p1)))

(defn edges->region
  "Combines edges to form a new region. The bisecting line is required here because we need to infer
  edges where there are gaps."
  [bisecting-line edges] (let [points     (set (map vec (apply concat edges)))
                               edges      (filter #(not (degenerate? (first %) (second %))) edges)
                               gap-points (filter #(line-contains? bisecting-line %) points)

                               implied    (->> gap-points
                                               (sort-by #(line-coordinate bisecting-line %))
                                               (partition 2)
                                               (map vec))

                               forward    (->> (concat edges implied)
                                               (map vec)
                                               (group-by first))
                               forward    (into {} (for [[k v] forward] [k (map second v)]))

                               backward   (->> (concat edges implied)
                                               (map reverse)
                                               (map vec)
                                               (group-by first))
                               backward   (into {} (for [[k v] backward] [k (map second v)]))]

                           (loop [visited #{(first gap-points)}
                                  path    [(first gap-points)]]
                             (let [self (last path)
                                   next (first (filter (complement visited)
                                                       (concat (forward self) (backward self))))]
                               (if (or (nil? next) (visited next))
                                 (apply concat path)
                                 (recur (conj visited next) (conj path next)))))))

(defn bisect-region
  "Takes a region and returns two new regions, each representing one side of the axis-aligned
  bisection of the original. Any edge crossing the bisecting line is split, and any gaps created by
  bisection are filled by using segments from the bisecting line."
  [region] (let [[[minlat minlng]
                  [maxlat maxlng]] (region-bounding-box region)
                 [dlat dlng]       [(- maxlat minlat) (- maxlng minlng)]
                 [mlat mlng]       [(/ (+ maxlat minlat) 2.0)
                                   (/ (+ maxlng minlng) 2.0)]
                 bisecting-line    (if (> dlat dlng)
                                     [[mlat minlng] [mlat maxlng]]      ; split lat
                                     [[minlat mlng] [maxlat mlng]])     ; split lng

                 edges             (region-edges region)
                 positive-edges    (filter #(and (positive-side? bisecting-line (first %))
                                                 (positive-side? bisecting-line (second %))) edges)
                 negative-edges    (filter #(not (or (positive-side? bisecting-line (first %))
                                                     (positive-side? bisecting-line (second %))))
                                           edges)
                 edges-to-split    (filter #(not= (positive-side? bisecting-line (first %))
                                                  (positive-side? bisecting-line (second %))) edges)

                 splits            (map #(split-edge bisecting-line %) edges-to-split)
                 positive-splits   (map first  splits)
                 negative-splits   (map second splits)]

             [(edges->region bisecting-line (concat positive-edges positive-splits))
              (edges->region bisecting-line (concat negative-edges negative-splits))]))

(defn boxes-intersect?
  "Returns true if two bounding boxes contain any common area. (Algorithm from
  http://stackoverflow.com/questions/306316/determine-if-two-rectangles-overlap-each-other)"
  [[[x11 y11] [x12 y12]] [[x21 y21] [x22 y22]]]
  (and (<= x11 x22) (>= x12 x21)
       (<= y11 y22) (>= y12 y21)))

(defn box-contains?
  "Returns true if a bounding box contains the given point."
  [[[minlat minlng] [maxlat maxlng]] [lat lng]]
  (and (<= minlat lat maxlat)
       (<= minlng lng maxlng)))

(defn bounded-space-partition
  "Returns an indexed structure that can be used to test points. The structure is repeatedly
  bisected until each dimension is smaller than the given amount."
  [region max-edge-length]
  (let [[[minlat minlng] [maxlat maxlng] :as bound] (region-bounding-box region)
        [dlat dlng]                                 [(- maxlat minlat) (- maxlng minlng)]]
    (if (or (> dlat max-edge-length) (> dlng max-edge-length))
      {:bounding-box bound
       :children     (map #(bounded-space-partition % max-edge-length)
                          (bisect-region region))}

      {:bounding-box bound
       :region       region})))

(defn partitioned-poly-intersects?
  "Returns true if the partitioned polygon intersects the given box. The poly must be partitioned
  such that the bounding boxes end up being no larger than the one being tested for."
  [partitioned-poly box]
  (and (boxes-intersect? (:bounding-box partitioned-poly) box)
       (if-let [children (:children partitioned-poly)]
         (some #(partitioned-poly-intersects? % box) children)
         true)))

(defn partitioned-poly-contains?
  "Returns true if the partitioned poly contains the given point."
  [partitioned-poly p]
  (and (box-contains? (:bounding-box partitioned-poly) p)
       (if-let [children (:children partitioned-poly)]
         (some #(partitioned-poly-contains? % p) children)
         (region-contains? (first p) (second p) (:region partitioned-poly)))))
