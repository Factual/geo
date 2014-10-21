# Geo

Lots of code for working with geographical stuff: points, distances, regions, geohashes, etc.

## geo.spatial

Provides common interfaces for working with spatial objects and geodesics. All
units are in meters/radians/steradians unless otherwise specified. Provides
static spatial contexts for the earth, and some constants like the earth's
radii and circumferences, along with points like the poles.

Defines protocols for unified access to Points and Shapes, to allow different geometry libraries to interoperate.

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

Wrapper for the vividsolutions JTS spatial library. Constructors for points, coordinate sequences, rings, polygons, multipolygons, and so on.

## geo.geohash

Defines geohashes using the ch.hsr.geohash library, and extends them to support the Shapelike protocol; these geohashes can then interoperate with jts polygons and other shapes, and support the full range of shape and intersection queries.

Given a geohash, can find neighbors in each directions, and compute concentric square rings around that geohash. Extract centerpoints and dimensions, either via horizontal/vertical extent or estimated areas. Estimate errors for specific geohashes--error is largest at the equator and smallest at the poles. Round-trip gephashes to and from base32.

Given a target shape on the geoid, can tell you how many bits of precision are necessary to get geohashes on roughly that scale. Can compute all geohashes intersecting a shape in general, and all geohashes within a radius of a point in specific.

# Tests

The project uses [Midje](https://github.com/marick/Midje/).

## How to run the tests

`lein midje` will run all tests.

`lein midje namespace.*` will run only tests beginning with "namespace.".

`lein midje :autotest` will run all the tests indefinitely. It sets up a
watcher on the code files. If they change, only the relevant tests will be
run again.
