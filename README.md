# Distributed Job Processing System

Java workers process jobs over MQTT.

Course project for CSC 364 at Cal Poly, implemented by James Riggs. Originally hosted under the JOMJOMJ0M account.

## What it does

A coordinator manages a bounded operation queue, runs local workers, and dispatches work to remote workers through MQTT. Locks and semaphores coordinate shared state; timed-out jobs are reassigned and late results are checked against pending jobs.

## Run

Use Java 17+, Maven, and a local MQTT broker listening on port 1883. In separate terminals:

```sh
cd "MQTT Coordinator"
mvn compile exec:java
```

```sh
cd "MQTT Remote-worker"
mvn compile exec:java -Dexec.args="worker-1 tcp://localhost:1883 1000"
```

The coordinator accepts broker address, local-worker count, and timeout in milliseconds as optional arguments. The remote worker accepts worker ID, broker address, and simulated solve delay. Press Ctrl+C to stop.

This is a local educational system; it is not a production service. The broker defaults to localhost without authentication. Build/test and end-to-end MQTT behavior should be checked separately when changing the implementation.
