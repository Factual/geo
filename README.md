# Geo

At Factual, we process a lot of spatial data. We're open-sourcing one of our
internal libraries for working with geospatial information, especially
geohashes. We want all Clojure programmers to be able to answer questions about
coordinates, distances, and polygon intersections. We think this library will
be particularly useful in concert with our [rich API of geospatial
information](http://developer.factual.com/).

We unify three open-source JVM geospatial libraries: The [JTS topology
library](http://www.vividsolutions.com/jts/JTSHome.htm),
[spatial4j](https://github.com/spatial4j/spatial4j), and
[geohash-java](https://clojars.org/la.tomoj/geohash-java). Clojure protocols
allow these libraries' disparate representations of points and shapes to
interoperate, so you can, for instance, ask whether a JTS point is within a
geohash, or whether a geohash intersects a spatial4j multipolygon.

In addition, we provide common scales and translation functions for unit
conversion: converting between steradians and surface areas; finding the radius
along the geoid, and some basic properties.

This library is incomplete; in particular, it is not as fast as it could be,
encounters bounded errors when translating between various geoid
representations, and is subject to singularities at the poles. Nonetheless, we
hope that it can be a canonical resource for geospatial computation in Clojure.

# Installation

Install via [clojars](https://clojars.org/factual/geo)

[![Clojars Project](https://img.shields.io/clojars/v/factual/geo.svg)](https://clojars.org/factual/geo)

Leiningen Dependency Vector:

```
[factual/geo "1.2.0"]
```

## Documentation

Available on Github Pages here: http://factual.github.io/geo/index.html

## Examples

```clj
; Load library
(require '[geo [geohash :as geohash] [jts :as jts] [spatial :as spatial] [io :as gio]])

; Find the earth's radius at the pole, in meters
user=> (spatial/earth-radius spatial/south-pole)
6356752.3

; Or at 45 degrees north
user=> (spatial/earth-radius (spatial/geohash-point 45 0))
6378137.0

; Distance between London Heathrow and LAX
user=> (def lhr (spatial/spatial4j-point 51.477500 -0.461388))
user=> (def lax (spatial/spatial4j-point 33.942495 -118.408067))
user=> (/ (spatial/distance lhr lax) 1000)
8780.16854531993 ; kilometers

; LHR falls within a 50km radius of downtown london
user=> (def london (spatial/spatial4j-point 51.5072 0.1275))
user=> (spatial/intersects? lhr (spatial/circle london 50000))
true

; But it's not in the downtown area
user=> (spatial/intersects? lhr (spatial/circle london 10000))
false

; At London, how many bits of geohash do we need for 100m features?
user=> (-> london (spatial/circle 50) geohash/shape->precision)
35

; Let's get the 35-bit geohash containing London's center:
user=> (def h (-> london (geohash/geohash 35)))
user=> h
#<GeoHash 1101000001000001000100100010101100000000000000000000000000000000 ->
(51.508026123046875,0.1263427734375) -> (51.50665283203125,0.127716064453125)
-> u10j4bs>

; How tall/fat is this geohash, through the middle?
user=> (geohash/geohash-midline-dimensions h)
[152.7895756415971 95.34671939564083]

; As a base32-encoded string, this hash is:
user=> (geohash/string h)
"u10j4bs"

; We can drop characters off a geohash to get strict supersets of that hash.
user=> (spatial/intersects? (geohash/geohash "u10j4") london)
true

; And we can show it's a strict superset by comparing the regions:
user=> (spatial/relate (geohash/geohash "u10j4bs") (geohash/geohash "u10j4"))
:within
user=> (spatial/relate (geohash/geohash "u10j4") (geohash/geohash "u10j4bs"))
:contains

; Versus, say, two adjacent hashes, which intersect along their edge
user=> (spatial/relate h (geohash/northern-neighbor h))
:intersects

; But two distant geohashes do *not* intersect
user=> (-> (iterate geohash/northern-neighbor h) (nth 5) (spatial/relate h))
:disjoint

; Find all 30-bit geohashes covering the 1km region around LHR
(-> lhr (spatial/circle 1000) (geohash/geohashes-intersecting 30) (->> (map geohash/string)))
("gcpsv3" "gcpsv4" "gcpsv5" "gcpsv6" "gcpsv7" "gcpsv9" "gcpsvd" "gcpsve" "gcpsvf" "gcpsvg" "gcpsvh" "gcpsvk" "gcpsvs" "gcpsvu")

; Or more directly
user=> (map geohash/string (geohash/geohashes-near lhr 1000 30))
("gcpsv3" "gcpsv4" "gcpsv5" "gcpsv6" "gcpsv7" "gcpsv9" "gcpsvd" "gcpsve" "gcpsvf" "gcpsvg" "gcpsvh" "gcpsvk" "gcpsvs" "gcpsvu")

; Reading JTS Geometries to/from common geo formats
user=> (gio/read-wkt "POLYGON ((-70 30, -70 30, -70 30, -70 30, -70 30))")
#object[com.vividsolutions.jts.geom.Polygon 0x675302a "POLYGON ((-70 30, -70 30, -70 30, -70 30, -70 30))"]

user=> (gio/to-wkt (gio/read-wkt "POLYGON ((-70 30, -70 31, -71 31, -71 30, -70 30))"))
"POLYGON ((-70 30, -70 31, -71 31, -71 30, -70 30))"

user=> (gio/read-geojson "{\"type\":\"Polygon\",\"coordinates\":[[[-70.0,30.0],[-70.0,31.0],[-71.0,31.0],[-71.0,30.0],[-70.0,30.0]]]}")
#object[com.vividsolutions.jts.geom.Polygon 0x57a1c5f5 "POLYGON ((-70 30, -70 31, -71 31, -71 30, -70 30))"]

user=> (gio/to-geojson (gio/read-geojson "{\"type\":\"Polygon\",\"coordinates\":[[[-70.0,30.0],[-70.0,31.0],[-71.0,31.0],[-71.0,30.0],[-70.0,30.0]]]}"))
"{\"type\":\"Polygon\",\"coordinates\":[[[-70.0,30.0],[-70.0,31.0],[-71.0,31.0],[-71.0,30.0],[-70.0,30.0]]]}"

user=> (gio/to-wkb (gio/read-wkt "POLYGON ((-70 30, -70 31, -71 31, -71 30, -70 30))"))
#object["[B" 0xe62e731 "[B@e62e731"]

user=> (gio/read-wkb *1)
#object[com.vividsolutions.jts.geom.Polygon 0x6f85711c "POLYGON ((-70 30, -70 31, -71 31, -71 30, -70 30))"]
```

# Namespace overview

## geo.spatial

Provides common interfaces for working with spatial objects and geodesics. All
units are in meters/radians/steradians unless otherwise specified; coordinates
are typically in long/lat "degrees" (which are *not* angular degrees). Provides
static spatial contexts for the earth, and some constants like the earth's
radii and circumferences, along with points like the poles.

Defines protocols for unified access to Points and Shapes, to allow different
geometry libraries to interoperate.

Basic utility functions for unit conversion; e.g. degrees<->radians.

Functions for computing earth radii, surface distances, and surface areas.

Constructors for shapes like bounding-boxes and circles, and utility functions
over shapes for their heights, centers, areas, etc. Can also compute
relationships between shapes: their intersections, contains, disjoint statuses,
etc.

## geo.poly

Polygon operations: primarily intersection testing, point-contains,
polygon->edges, bounding boxes, normal vectors, linear coordinates, bisections,
box intersections, bounded-space partitioning, and so on.

## geo.jts

Wrapper for the vividsolutions JTS spatial library. Constructors for points,
coordinate sequences, rings, polygons, multipolygons, and so on.

## geo.geohash

Defines geohashes using the ch.hsr.geohash library, and extends them to support
the Shapelike protocol; these geohashes can then interoperate with jts polygons
and other shapes, and support the full range of shape and intersection queries.

Given a geohash, can find neighbors in each directions, and compute concentric
square rings around that geohash. Extract centerpoints and dimensions, either
via horizontal/vertical extent or estimated areas. Estimate errors for specific
geohashes--error is largest at the equator and smallest at the poles.
Round-trip geohashes to and from base32.

Given a target shape on the geoid, can tell you how many bits of precision are
necessary to get geohashes on roughly that scale. Can compute all geohashes
intersecting a shape in general, and all geohashes within a radius of a point
in specific.

## geo.io

Helper functions for dealing with common geospatial serialization formats. Use these to read and write from WKT, GeoJSON, and WKB.

IO functions return JTS Geometries.

# Tests

The project uses [Midje](https://github.com/marick/Midje/).

## How to run the tests

`lein midje` will run all tests.

`lein midje namespace.*` will run only tests beginning with "namespace.".

`lein midje :autotest` will run all the tests indefinitely. It sets up a
watcher on the code files. If they change, only the relevant tests will be
run again.

# License

Licensing is complicated; this is my best effort at interpreting the legal
issues involved. If you think this is incorrect, please let me know.

This project and many of its dependencies are licensed under the Eclipse Public
License version 1.0. It has a runtime dependency on, but is not distributed
with, the LGPL-licensed [JTS Topology
Suite](http://www.vividsolutions.com/jts/JTSHome.htm). The
[LGPL](http://www.gnu.org/licenses/lgpl.html).

This library and JAR on Clojars are not AOT-compiled; they are distributed as
source code which is compiled by Clojure at runtime. It is not linked with JTS
but *does* make use of its interfaces, which makes it an "Application" under
the terms of the LGPL. Since the JAR is distributed as source, it does not
convey "object code".

When this library is compiled by Clojure (e.g. when AOT-compiled, or when a
program requiring this library is run), it will call functions on JTS
LGPL-licensed classes. This is, as I understand the LGPL, a "Combined Work". If
you wish to convey a combined work, you may, provided, as per the LGPL section
4, that:

a.) You give prominent notice that the LGPL is used (e.g. this license section)
b.) You accompany the combined work with a copy of the GNU GPL and this license
document (see LGPL.txt and GPL.txt)
c.) You display copyright notices during execution (not applicable here)
d.) Convey the minimal corresponding source, or use a suitable shared library mechanism (e.g. jars)
e.) Provide installation information (trivial; via Leiningen, Maven, etc)
