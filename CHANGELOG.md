# Changelog

## 2.1.1 to 3.0.0

* **Breaking change**: switch upstream `proj4j` to use `[org.locationtech.proj4j/proj4j "1.0.0"]`, changing namespace from `org.osgeo.proj4j` to `org.locationtech.proj4j`
* **Breaking change**: rename `proj4-string?` to `proj4-str?`, to maintain naming consistency in the API
* Allow `transform-geom` to use externally created `proj4j` `CoordinateTransform` objects
* `geo.io` readers and writers are now thread-safe
* Add `h3-line` function to H3 protocol, which returns the line of indexes between two cells
* Add `get-res-0-indexes` function for H3, which returns a collection of all indexes at resolution 0
* Add a `Feature` record type to `geo.spatial`, codifying the idea that a feature contains `:geometry` and `:properties`
* Add testing support for JDK11 and Clojure 1.10
* Bump `h3` to 3.4.0, enabling support for functions described above
* Bump other core dependencies to keep up with upstream changes: `jts2geojson` to 0.13.0, and `jts` to 1.16.1
* Bump internal dependencies for testing and documentation: `midje` to 1.9.6, `cheshire` to 5.8.1, and `lein-codox` to 0.10.6

## 2.1.0 to 2.1.1

* Fix reflection on functions in the `crs` namespace
* Bump JTS to 1.16.0, with support for XYZM coordinates
* Bump H3 to 3.1.0, with support for a new `h3-distance` function
* Bump internal dependencies

## 2.0.0 to 2.1.0

### New namespace `geo.h3` for interacing with [Uber's H3 tiling library](https://github.com/uber/h3)

This namespace contains a variety of functions for interoperating with H3 via Uber's [H3 Java Bindings](https://github.com/uber/h3-java). Some highlights include these functions implemented for H3 Cells expressed as either Longs or Strings:

**(Via protocol `geo.h3/H3Index`)**

This protocol is implemented for `String` and `Long` which are the 2 ways of representing an H3 cell.

* `to-string`
* `to-long`
* `h3->pt`
* `get-resolution`
* `k-ring`
* `k-ring-distances`
* `to-jts`
* `edge`
* `edge-origin`
* `edge-destination`
* `edges`
* `edge-boundary`
* `pentagon?`
* `is-valid?`
* `neighbors?`

**Via protocol `geo.h3/Polygonal`**

This protocol includes interfaces to H3's Polyfill functionality for tiling a polygon with H3 cells.

It is implemented for `ch.hsr.geohash.GeoHash`, `org.locationtech.spatial4j.shape.impl.RectangleImpl`, `org.locationtech.jts.geom.Polygon`, `org.locationtech.jts.geom.LinearRing`, and `org.locationtech.jts.geom.MultiPolygon`.

* `to-polygon`
* `polyfill`
* `polyfill-address`

**Additional H3 Functions**

* `compact`
* `uncompact`
* `multi-polygon`

Additionally, we've added the H3 `GeoCoord` class to `geo`'s `Shapelike` and `Point` protocols, so they can interoperate with all of the existing spatial types.

See the full API docs for more detailed information.

## 1.2.0 to 2.0.0

### JTS Major Version Upgrade

* Update to new version of JTS hosted by LocationTech org (https://github.com/locationtech/jts and https://mvnrepository.com/artifact/org.locationtech.jts/jts-core/1.15.0)

**Note** while this change is relatively minor with regard to the outward API for factual/geo, it is considered a major version upgrade because any of the JTS types returned by this library will have moved from the `com.vividsolutions` namespace to `org.locationtech`. So users of this library may need to update any type-specific code or imports accordingly.

### Proj4 Projection Support

Several new functions have been added to the `geo.jts` namespace to deal with CRS projections.

* `geo.jts` operations will default to SRID 4326, but will respect a geometry's existing SRID if set
* `geo.jts/transform-geom` to convert a JTS Geometry to an alternate CRS.
* `geo.jts/get-srid` to check the SRID of a JTS Geometry
* `geo.jts/set-srid` to check the SRID of a JTS Geometry
* `geo.jts` functions `linear-ring`, `polygon-wkt`, and `multi-polygon-wkt` now accept optional `srid` arguments for constructing new geometries in the desired SRID
* `geo.spatial/to-jts` New `Shapelike` protocol function for converting `Shapelike`s to JTS geometries. Accepts an optional SRID and defaults to 4326

See `geo.crs` for some helpers around checking and manipulating CRS reference IDs.

### GeoJSON Updates

* `geo.io/read-geojson` Can now read GeoJSON Features and FeatureCollections. Note that the interface of this has changed somewhat to accommodate the different structures of GeoJSON Geometries, Features, and FeatureCollections. `read-geojson` will always return a sequence of Feature maps containing `:geometry` and `:properties` keys
* Use `geo.io/parse-geojson` To access the raw GeoJSON entity (Geometry, Feature, or FeatureCollection) if needed
* Use `geo.io/to-geojson-feature-collection` or `geo.io/to-geojson-feature` to construct the appropriate GeoJSON from a map containing `{:properties {...} :geometry <JTS Geom>}`

### New Spatial Functions

* `geo.spatial/rand-point-in-radius` for getting a random point within a given radius of a given center point
* `geo.spatial/resegment` for partitioning JTS LineStrings into segments of a given max length

### Geohash performance improvements

* `geo.geohash/geohashes-intersecting` has been optimized significantly for tiling large geometries with small geohash levels.

## 1.2.0 to 1.2.1

* Minor perf improvement from removing a reflection call in our JTSShapeFactoryUsage

## 1.1.0 to 1.2.0

* Added new spatial function for splitting JTS linestrings into sub-strings based on a desired maximum segment length
* Added JTS functions for working with linestrings more conveniently

## 1.0.0 to 1.1.0

* Update dependencies, including JTS to 1.13 and Spatial4J to 0.6
* Add new `geo.io` namespace with functions for reading and writing common geo formats
* Fix intersecting-geohashes edge cases around dateline and origin-crossing geometries
