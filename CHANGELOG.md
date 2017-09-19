# Changelog

## 1.1.0 to 1.2.0

* Added new spatial function for splitting JTS linestrings into sub-strings based on a desired maximum segment length
* Added JTS functions for working with linestrings more conveniently

## 1.0.0 to 1.1.0

* Update dependencies, including JTS to 1.13 and Spatial4J to 0.6
* Add new `geo.io` namespace with functions for reading and writing common geo formats
* Fix intersecting-geohashes edge cases around dateline and origin-crossing geometries

We'll also be tracking the ongoing JTS migration as the project moves from `com.vividsolutions` into `org.locationtech`. We'll try to put out another release with the latest JTS/Spatial4J versions once that is sorted out.

More info: https://github.com/locationtech/jts/issues/78
