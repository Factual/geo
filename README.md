# Geo

[![Build Status](https://travis-ci.org/Factual/geo.svg?branch=master)](https://travis-ci.org/Factual/geo)

At Factual, we process a lot of spatial data. We're open-sourcing one of our
internal libraries for working with geospatial information, especially
geohashes. We want all Clojure programmers to be able to answer questions about
coordinates, distances, and polygon intersections. We think this library will
be particularly useful in concert with our [rich API of geospatial
information](http://developer.factual.com/).

We unify five open-source JVM geospatial libraries: The [JTS topology
suite](https://github.com/locationtech/jts),
[spatial4j](https://github.com/spatial4j/spatial4j),
[geohash-java](https://github.com/kungfoo/geohash-java),
[proj4j](https://github.com/locationtech/proj4j),
and [h3](https://github.com/uber/h3-java). Clojure
protocols allow these libraries' disparate representations of points, shapes,
and spatial reference systems to interoperate, so you can, for instance, ask
whether a JTS point is within a geohash, whether a geohash intersects a
spatial4j multipolygon, or whether a geohash's center point intersects with a
JTS polygon projected in a local state plane.

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
[factual/geo "3.0.0"]
```

## Documentation

Available on Github Pages here: http://factual.github.io/geo/3.0.0/index.html

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
#object[org.locationtech.jts.geom.Polygon 0x675302a "POLYGON ((-70 30, -70 30, -70 30, -70 30, -70 30))"]

user=> (gio/to-wkt (gio/read-wkt "POLYGON ((-70 30, -70 31, -71 31, -71 30, -70 30))"))
"POLYGON ((-70 30, -70 31, -71 31, -71 30, -70 30))"


user=> (gio/read-geojson "{\"type\":\"Feature\",\"geometry\":{\"type\":\"Point\",\"coordinates\":[0.0,0.0]},\"properties\":{\"name\":\"null island\"}}\")
[{:properties {:name \"null island\"}
  :geometry #object[org.locationtech.jts.geom.Point(...)]}]

user=> (->> "{\"type\":\"Polygon\",\"coordinates\":[[[-70.0,30.0],[-70.0,31.0],[-71.0,31.0],[-71.0,30.0],[-70.0,30.0]]]}"
            gio/read-geojson
            (map :geometry)
            (map gio/to-geojson))
["{\"type\":\"Polygon\",\"coordinates\":[[[-70.0,30.0],[-70.0,31.0],[-71.0,31.0],[-71.0,30.0],[-70.0,30.0]]]}"]

user=> (gio/to-wkb (gio/read-wkt "POLYGON ((-70 30, -70 31, -71 31, -71 30, -70 30))"))
#object["[B" 0xe62e731 "[B@e62e731"]

user=> (gio/read-wkb *1)
#object[org.locationtech.jts.geom.Polygon 0x6f85711c "POLYGON ((-70 30, -70 31, -71 31, -71 30, -70 30))"]
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

Wrapper for the locationtech JTS spatial library. Constructors for points,
coordinate sequences, rings, polygons, multipolygons, and so on.

Given a certain geometry, can transform using proj4j to a different coordinate
reference system.

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

Helper functions for dealing with common geospatial serialization formats.
Use these to read and write from WKT, GeoJSON, WKB in byte and in hex string
formats, and EWKB in byte and in hex string formats.

## geo.crs

Helper functions for dealing with transforms between coordinate reference
systems. Can create transformations used in the geo.jts namespace.

## geo.h3

Defines hexagonal cells using the com.uber.h3 library. Extends H3's GeoCoord
to support the Point protocol. H3 cells can be referenced either as strings or longs,
and the corresponding Java functions will be called accordingly using the H3Index protocol.

Given a certain H3 cell, can compute surrounding rings, get the boundary in JTS format,
or get the resolution.

Given a Shapelike geometry, can polyfill a list of H3 cells at a given level of resolution.

Given a list of H3 cells, can compact the list to remove redundant cells, uncompact the list
to a desired resolution, or merge contiguous cells into a JTS multipolygon.

Given two different H3 cells, can get the grid distance between them.

Can create unidirectional edges based on different configurations of cells, and can check
for the 12 pentagonal cells at each resolution.

IO functions return JTS Geometries.

# Tests

The project uses [Midje](https://github.com/marick/Midje/).

## How to run the tests

`lein midje` will run all tests.

`lein midje namespace.*` will run only tests beginning with "namespace.".

`lein midje :autotest` will run all the tests indefinitely. It sets up a
watcher on the code files. If they change, only the relevant tests will be
run again.

## Generating Codox Docs

* Checkout appropriate release branch (e.g. `release/2.0.0`)
* Generate docs with `lein codox` (This will generate the HTML/CSS/JS docs under `target/doc`)
* Move generated docs out of `target` into appropriate dir under `docs`: `mv target/doc/ docs/2.0.0`
* Commit changes, and merge that doc update to `master`

# Updating deps.edn

While the project is based on leiningen, the use of [depify](https://github.com/hagmonk/depify)
can create a `deps.edn` file to enable a `tools.deps.alpha`-based development workflow. When dependencies in
`project.clj` are updated, the `deps.edn` file can be updated manually or automatically. Based on
[depify's instructions](https://github.com/hagmonk/depify/blob/master/README.org#usage), the command to
update the `deps.edn` from the project root is:

`clj -A:depify | clj -A:zprint > deps.edn.tmp ; mv deps.edn.tmp deps.edn`

# License

This project and many of its dependencies are licensed under the Eclipse Public
License version 1.0.
