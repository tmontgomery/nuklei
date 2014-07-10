# Nuklei Micro Benchmarks

Various micro-benchmarks for Nuklei components using [JMH](http://openjdk.java.net/projects/code-tools/jmh/) as a harness.

## Build

You require the following to build Nuklei Benchmarks:

* Latest stable [Oracle JDK 8](http://www.oracle.com/technetwork/java/)
* 3.0.4 or later of [Maven](http://maven.apache.org/)

To build and install to local maven repository.

    $ mvn clean install

## Running Benchmarks

The benchmarks are placed inside an executable jar, called `microbenchmarks.jar`, in the `target` directory.

To list the benchmarks:

    $ java -jar target/microbenchmarks.jar -l
    
To get help from JMH for command line parameters:

    $ java -jar target/microbenchmarks.jar -h
