# Changelog

## 1.2.0 to 2.0.0

* Update to new version of JTS hosted by LocationTech org (https://github.com/locationtech/jts and https://mvnrepository.com/artifact/org.locationtech.jts/jts-core/1.15.0)

**Note** while this change is relatively minor with regard to the outward API for factual/geo, it is considered a major version upgrade because any of the JTS types returned by this library will have moved from the `com.vividsolutions` namespace to `org.locationtech`. So users of this library may need to update any type-specific code or imports accordingly.

## 1.2.0 to 1.2.1

* Minor perf improvement from removing a reflection call in our JTSShapeFactoryUsage

## 1.1.0 to 1.2.0

* Added new spatial function for splitting JTS linestrings into sub-strings based on a desired maximum segment length
* Added JTS functions for working with linestrings more conveniently

## 1.0.0 to 1.1.0

* Update dependencies, including JTS to 1.13 and Spatial4J to 0.6
* Add new `geo.io` namespace with functions for reading and writing common geo formats
* Fix intersecting-geohashes edge cases around dateline and origin-crossing geometries
