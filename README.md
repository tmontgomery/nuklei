# Nuklei

Micro services toolset

Attribution(s)

AtomicBuffer and AtomicBufferTest are taken from [SBE](https://github.com/real-logic/simple-binary-encoding).

First class entities

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
