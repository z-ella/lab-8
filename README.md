# Lab 8 — Optimistic locking for room reservations

This Java 11 / Maven project demonstrates JPA optimistic locking with Hibernate and an in-memory H2 database. `Reservation.version` is annotated with `@Version`; simultaneous transactions that update the same row cannot silently overwrite one another.

## Run

```sh
mvn clean test
mvn exec:java
```

The test suite proves a stale concurrent update is rejected and checks the service retry path. The simulator shows the first transaction succeeding, the stale transaction failing, and a retry applying the requested change with a fresh persistence context.

Each service operation opens and closes a short-lived `EntityManager`; writes use explicit resource-local transactions and roll back on failures.
