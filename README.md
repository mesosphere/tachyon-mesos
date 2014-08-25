# Tachyon-Mesos

> _A Mesos Framework for Tachyon, a memory-centric distributed file system._

## Word of Warning

_This is the outcome of a brief hackathon, suitable only for fun and collaboration._

## Prerequisites

- A Mesos cluster
- Java JDK
- SBT

## Usage

```bash
$ sbt "run {tachyonUrl} {mesosMaster} {zookeeperAddress}"
```

Where `mesosMaster` is of the same form that the `mesos-slave` program accepts
and where `zookeeperAddress` is of the form `host:port`.

Alternatively, if developing within the vagrant VM:

```bash
$ bin/run-dev
```

## Design

### Tachyon-Mesos Scheduler

The scheduler:

- registers as a framework with a Mesos master, naturally
- links against the tachyon library and starts a Tachyon Master
  in the same process.
- launches Tachyon worker processes on Mesos

### Assumptions

- Fault tolerance for the Tachyon-Mesos scheduler can be achieved by running
  with a meta-framework such as Marathon
