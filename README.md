# Welcome to `fastutil`!

[![Maven Central](https://img.shields.io/maven-central/v/io.github.lionblazer/zerofastutil.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22io.github.lionblazer%22%20AND%20a:%22zerofastutil%22)
[![javadoc](https://javadoc.io/badge2/io.github.lionblazer/zerofastutil/javadoc.svg)](https://javadoc.io/doc/io.github.lionblazer/zerofastutil)

## Introduction

`fastutil` extends the [Java™ Collections
Framework](http://download.oracle.com/javase/1.5.0/docs/guide/collections/)
by providing type-specific maps, sets, lists and queues with a small
memory footprint and fast access and insertion; it also provides big
(64-bit) arrays, sets and lists, and fast, practical I/O classes for
binary and text files. It is free software distributed under the [Apache
License 2.0](http://www.apache.org/licenses/LICENSE-2.0.html).

The classes implement their standard counterpart interface (e.g., `Map`
for maps) and can be plugged into existing code. Moreover, they provide
additional features (such as bidirectional iterators) that are not
available in the standard classes.

Besides objects and primitive types, `fastutil` classes provide support
for _references_, that is, objects that are compared using the equality
operator rather than the `equals()` method.

The sources are generated using a C preprocessor, starting from a set of
driver files. You can peek at the `javadoc`-generated documentation. In
particular, the overview explains the design choices used in `fastutil`.

## Core jar

If the standard `fastutil` jar is too large, there is a _core_ jar
containing only data structures specific for integers, longs and doubles.
Note that those classes are duplicated in the standard jar, so if you are
depending on both (for example, because of transitive dependencies) you
should exclude the core jar.

You can also create a small, customized fastutil jar (which you can put in
your repo, local maven repo, etc.) using the `find-deps.sh` shell script.
It has mild prerequisites, as only the `jdeps` tool is required (bundled
with JDK 8). It can be used to identify all fastutil classes your project
uses and build a minimized jar only containing the necessary classes.

## Building

The project builds with Gradle only.

- Build and test: `./gradlew clean build`
- Build only: `./gradlew assemble`
- Run tests: `./gradlew test`
- Generate docs jar: `./gradlew javadocJar`
- Create distribution artifacts (`jar`, `sources`, `javadoc`) in `dist/lib`: `./gradlew dist`

During build, Gradle prepares full sources automatically. If type-specific
generated classes are already present in `src/`, local sources are used as-is.
Otherwise, Gradle resolves the matching `fastutil` sources artifact for the
current version and merges local fork changes on top.

## Publishing to Maven Central

Coordinates are configured for this fork:

- Group: `io.github.lionblazer`
- Artifact: `zerofastutil`

Prerequisites:

- Sonatype Central Portal namespace must be verified.
- Create a **User Token** in Central Portal.
- Have a GPG private key for artifact signing.

Put secrets in `~/.gradle/gradle.properties`:

```properties
sonatypeUsername=<Central Portal User Token username>
sonatypePassword=<Central Portal User Token password>
signingKey=<ASCII-armored private key; use \n for line breaks>
signingPassword=<GPG key passphrase>
```

Publish a release:

```bash
./gradlew clean publishReleaseToCentral
```

Publish a snapshot (set version to `*-SNAPSHOT` first):

```bash
./gradlew clean publishToSonatype
```

Detailed checklist: see `PUBLISHING.md`.

Quick runtime marker after publish:

```java
System.out.println(it.unimi.dsi.fastutil.ZeroFastUtilInfo.marker());
```

## Speed

`fastutil` provides in many cases the fastest implementations available.
You can find many other implementations of primitive collections (e.g.,
[HPPC](http://labs.carrotsearch.com/hppc.html),
[Koloboke](https://github.com/leventov/Koloboke), etc.). Sometimes authors
are a little bit quick in defining their implementations the “fastest
available“: the truth is, you have to take decisions in any
implementation. These decisions make your implementation faster or slower
in different scenarios. I suggest to _always_ test speed within your own
application, rather than relying on general benchmarks, and ask the
authors for suggestions about how to use the libraries in an optimal way.
In particular, when testing hash-based data structures you should always
set explicitly the load factor, as speed is strongly dependent on the
length of collision chains.

## Big Data Structures

With `fastutil` 6, a new set of classes makes it possible to handle very
large collections: in particular, collections whose size exceeds
2<sup>31</sup>. Big arrays are arrays-of-arrays handled by a wealth of
static methods that act on them as if they were monodimensional arrays
with 64-bit indices, and big lists provide 64-bit list access. The size of
a hash big set is limited only by the amount of core memory.

## Discussion

There is a [discussion group](http://groups.google.com/group/fastutil)
about `fastutil`. You can join or [send a
message](mailto:fastutil@googlegroups.com).
