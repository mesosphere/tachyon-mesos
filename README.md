# Tachyon-Mesos

> _A Mesos Framework for Tachyon, a memory-centric distributed file system._

## Usage

```bash
$ sbt "run {tachyonUrl} {mesosMaster}"
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
