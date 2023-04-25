---
sidebar_position: 1
---

# DC Comics

DC (Dremio Cloud) Comics provides:

* a BoM defining/aligning all versions used by the framework and services can use it (Quarkus, Netty, ...)
* a single extension bringing all Quarkus extensions the services can use it
* a set of custom extensions for specific layers, eventually bringing new annotations and beans

## Core Values

For cloud applications, being the most reactive possible is a key criteria. So DC Comics chose to:

* Be build time oriented: only delegate to runtime the bean resolution (to enable dynamic module aggregation) and not the bean and model discovery nor proxy generation,
* Stay flexible: even if the model is generated at build time, you can generally still customize it by removing a bean and adding your own one,
* Be native friendly: some applications need to be native to start very fast and bypass the classloading and a bunch of JVM init. FOr that purpose, we ensure the framework is GraalVM friendly.

## Setup

See [setup](setup) page to see how to get your cloud application project started.

## Extensions

* [DC Meta](meta)
* [Logging & Observability](observability)
* [Identity](identity)
* [gRPC](grpc)
