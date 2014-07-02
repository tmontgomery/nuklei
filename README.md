# Nuklei

Micro services toolkit

## License (See LICENSE file for full license)

Copyright 2014 Kaazing Corporation, All rights reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

## Attributions

AtomicBuffer and AtomicBufferTest are taken from [SBE](https://github.com/real-logic/simple-binary-encoding).
Queuing mechanism highly inspired by [Martin Thompson](https://github.com/mjpt777),
[Gil Tene](https://github.com/giltene), [Nitsan Wakart](https://github.com/nitsanw), and discussions on the
[Mechanical Sympathy Google Group](https://groups.google.com/forum/#!forum/mechanical-sympathy)

## Build

You require the following to build Nuklei:

* Latest stable [Oracle JDK 8](http://www.oracle.com/technetwork/java/)
* 3.0.4 or later of [Maven](http://maven.apache.org/)

    $ mvn install

## First class entities

- __Nukleus__: interface for service. Also the service itself.
- __Bond__: multiple-producer-single-consumer (MPSC) queue/mailbox between Nuklei.
- __Elektron__: messages sent to a Nuklei. Nuklei share Elektrons through Bonds.
- __Kompound__: collection of interconnected Nuklei. Also the layout of the collection.
- __Molekule__: ?? (maybe use this instead of Kompound?)
- __Partikle__: ??

Other entities

- __Runtime__: runtime around a Nukleus. Might be dedicated thread, or donated thread, pooled thread, or Quasar-type.
- __?__: state per Bond

TODOs

- Do Nuklei have multiple Bonds? May not be necessary if Bond is MPSC?
- Bonds can have/hold state (equivalent to state per socket, e.g.)

- ~~Flush out AtomicBuffer for access to field types (from SBE CodecUtils)~~
- ~~Flush out Flyweights (Elektrons?)~~
- Flush out AtomicBuffer additions for MPSC (Bond?)
- Flush out MPSC (Bond?)
- Flush out Runtimes
    - Dedicated Thread
    - Donated Thread
    - Dedicated Thread Pool
    - Quasar-based (?)
- Benchmark Framework (JMH)
- Flush out Kompound
