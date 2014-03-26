# Nuklei

High performance micro services framework

First class entities

- __Nukleus__: interface for service. Also the service itself.
- __Bond__: multiple-producer-single-consumer (MPSC) queue/mailbox between Nuklei.
- __Elektron__: messages sent to a Nuklei. Nuklei share Elektrons through Bonds.
- __Kompound__: collection of interconnected Nuklei. Also the layout of the collection.

Other entities

- __Runtime__: runtime around a Nukleus. Might be dedicated thread, or donated thread, pooled thread, or Quasar-type.

TODOs

- Flush out AtomicBuffer for access to field types (from SBE CodecUtils) and for MPSC Bond
- Flush out Elektron (flyweights)
- Flush out MPSC Bond
- Flush out Runtimes
    - Dedicated Thread
    - Donated Thread
    - Dedicated Thread Pool
    - Quasar-based
- Benchmark Framework
- Flush out Kompound
