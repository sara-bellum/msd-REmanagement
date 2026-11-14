# Real Estate Management — Persistence Implementation

We extended our existing domain model with PostgreSQL persistence for Listings, Clients/Viewings and a deliberately limited transaction workflow. We kept the business rules in the domain classes and adapted repositories, application services and the factory to work with JPA.

This document describes the implemented persistence scope and the evidence provided by our tests. It does not treat the presence of a test or CI configuration as proof that a particular commit has passed: execution results must be checked in that commit's local build and GitHub Actions run.

## Contents

1. [What we implemented](#1-what-we-implemented)
2. [How we connected the layers](#2-how-we-connected-the-layers)
3. [Shared entities, identity and audit](#3-shared-entities-identity-and-audit)
4. [Listings and properties](#4-listings-and-properties)
5. [Requests and preferred zones](#5-requests-and-preferred-zones)
6. [Interests and viewings](#6-interests-and-viewings)
7. [Local child IDs and database keys](#7-local-child-ids-and-database-keys)
8. [JPA repositories and adapters](#8-jpa-repositories-and-adapters)
9. [Transactional application operations](#9-transactional-application-operations)
10. [Persistent factory and sample data](#10-persistent-factory-and-sample-data)
11. [Transaction initiation: integration-test walkthrough](#11-transaction-initiation-integration-test-walkthrough)
12. [Transaction cancellation: integration-test walkthrough](#12-transaction-cancellation-integration-test-walkthrough)
13. [Failure cases and rollback evidence](#13-failure-cases-and-rollback-evidence)
14. [Deferred transaction features](#14-deferred-transaction-features)
15. [Memory tests versus PostgreSQL evidence](#15-memory-tests-versus-postgresql-evidence)
16. [Docker, Flyway and GitHub Actions](#16-docker-flyway-and-github-actions)
17. [How we verify the implementation](#17-how-we-verify-the-implementation)
18. [Relevant files](#18-relevant-files)

## 1. What we implemented

We persisted the shared person/agent/zone model, properties and listings, image metadata, client requests, preferred zones, interests, viewings and the minimal transaction result with its initial agent participant.

| Area | Implemented persistence scope |
|---|---|
| Shared model | Persons, agents, zones and agent-zone assignments |
| Listings | Properties, listings, agent assignment/reassignment and image metadata |
| Clients | Requests, preferred zones and interests in specific listings |
| Viewings | Scheduling, confirmation and recorded feedback/state |
| Transactions | Initiation after a completed viewing, listing reservation, initial participant, cancellation and release |
| Technical support | Versioned SQL migrations, audit timestamps, optimistic versioning, demo initialization and PostgreSQL integration tests |

We preserved the advanced transaction behavior of the previous in-memory domain model. We did **not** make that entire workflow persistent in this delivery. Section 14 identifies the boundary explicitly.

## 2. How we connected the layers

We kept three responsibilities separate:

- **Domain classes** express valid state transitions and business rules. For example, `Interes` checks whether a completed viewing allows transaction initiation.
- **Application services** coordinate a use case: load existing entities, invoke domain behavior, save changes and publish workflow events within a transaction boundary.
- **Infrastructure adapters** implement the repository interfaces using Spring Data JPA and PostgreSQL.

Our services depend on interfaces such as `IInteresEntityRepository`, rather than on a specific storage implementation. We kept the in-memory implementations for the previous workflow tests and supplied JPA adapters for persistent execution.

For example, `ClientApplicationService` works through the interest repository interface. In PostgreSQL mode, `InteresRepositoryJpa` translates that API into operations on `InteresJpaRepository`. The shared adapter provides reusable CRUD operations.

We used constructor injection in our services and adapters. This made the required collaborators explicit without manually constructing the application object graph.

## 3. Shared entities, identity and audit

### The base entity

We mapped `Entitate` as a `@MappedSuperclass`. Its mappings are inherited by concrete entities; it does not require its own entity table.

| Field | Purpose |
|---|---|
| `persistenceId: Long` | Technical primary key generated from `entity_pk_seq` |
| `domainId: Integer` | Existing application identifier returned by `getId()` |
| `version: Long` | JPA `@Version` for detecting stale updates |
| `createdAt: Instant` | Technical creation timestamp |
| `updatedAt: Instant` | Technical update timestamp |

We preserved the existing `getId()` contract so services and commands did not need a complete identifier redesign. For root entities, the JPA adapter obtains new domain IDs from `domain_id_seq`. This database sequence coordinates allocation across application instances; gaps after rollback are expected.

We added protected no-argument constructors for JPA hydration while retaining validated constructors for ordinary creation. We did not add public setters that would allow callers to bypass business rules.

### Persons and agents

We mapped `Persoana` and `Agent` with `InheritanceType.JOINED`. A person is stored in `persoane`, and an agent has an additional row in `agenti` with the same technical primary key. We did not create a duplicate person record when the same individual is used as an agent.

A client or property owner is represented by an existing `Persoana`. We did not introduce a separate client table containing copied identity data.

### Equality and audit

We adapted equality to account for Hibernate entity classes and technical keys. Persisted children with the same local domain ID are distinguishable when their database keys differ. We kept hash codes stable when a generated key is assigned.

We attached `PersistenceAuditListener` to the base entity. `@PrePersist` initializes audit timestamps and `@PreUpdate` refreshes the update timestamp. These are technical persistence timestamps, not a complete historical audit trail.

We also added optimistic versioning. It detects conflicting updates to versioned rows; it does not by itself prove that every possible multi-user scheduling or reservation race is prevented.

## 4. Listings and properties

We separated the physical property from its commercial listing:

- `Proprietate` describes the asset: owner, zone, property type, address, area, rooms and other details.
- `Oferta` describes the listing: sale/rental type, asking price, description, state and assigned agent.
- `ImgOferta` stores image metadata associated with the listing.

We retained rules such as a registered owner, a positive asking price and a newly created listing starting in `NOUA`. Assignment and activation still use domain methods rather than direct state setters.

We mapped a listing's images as owned children with cascade and orphan removal. Removing an image from that collection can delete its database row. This does not delete the referenced property or owner.

We treated persons, agents, zones and properties as entities with independent lifecycles. We did not configure delete cascading from a listing to those shared entities. The agent's portfolio is an inverse relationship using `mappedBy`, while the listing's agent field owns the assignment link.

We tested creation and assignment after reloading, reassignment between agents and image deletion without losing the property or owner. These tests check stored relationships, rather than only checking that the original Java objects were updated.

## 5. Requests and preferred zones

We used `Cerere` to represent what a client is looking for: purchase/rental type, property type, budget range, desired area, room count and preferred locations.

`CerereZona` represents one preferred zone and its priority. We made these preference records children of the request with cascade and orphan removal. The referenced `Zona` remains shared; deleting a preference must not delete the geographical zone itself.

We preserved `Cerere.estePotrivitaCu(Oferta)` as the matching rule. It checks request/listing availability, compatible types, budget, area, room count and preferred zone, and excludes the client's own property. We kept this business behavior independent of the storage adapter.

We persisted enum values with `EnumType.STRING`, so the stored value is the enum name rather than an ordinal that could change when enum constants are reordered. We used explicit decimal precision/scale where mapped financial/request fields require it.

## 6. Interests and viewings

We distinguished a general request from interest in a concrete listing:

| Concept | Meaning |
|---|---|
| `Cerere` | What the client wants generally |
| `Interes` | The client's interest in one particular listing |
| `Vizionare` | A scheduled viewing and its eventual outcome |

We allowed an interest without a prior request, so its `cerere` reference is optional. When a request is supplied, existing domain rules check ownership, state and compatible listing/request types.

We made `Interes` the aggregate root for viewings. `Interes.vizionari` uses `mappedBy = "interes"`, cascade and orphan removal. `Vizionare.interes` owns the foreign-key relationship. We saved viewing changes through the interest aggregate instead of introducing a separate viewing repository.

We retained scheduling, confirmation and feedback behavior. The service checks agent appointment conflicts at the same exact date/time; we did not implement a duration-based overlap calendar or claim general concurrent scheduling exclusion.

The PostgreSQL test for requests and viewings verifies matching, the linked request, preferred zones, the `EFECTUATA` viewing state, `CONTINUA` decision, interest level and the child's reference back to the loaded parent.

## 7. Local child IDs and database keys

We retained local numbering within aggregates. For example:

| Parent interest | Viewing domain ID | Database primary key |
|---|---:|---|
| Interest A | 1 | A generated `persistenceId` |
| Interest B | 1 | A different generated `persistenceId` |

The application identifies a viewing in the context of its parent interest. A local viewing ID is not a globally unique viewing identifier. Database relationships use the technical keys, and child uniqueness is scoped to the parent where defined in the schema.

In `requestsMatchAndViewingFeedbackPersistWithLocalChildIds`, we checked that two viewing rows with `domain_id = 1` exist and that they have two distinct `persistence_id` values. We also checked that the two persisted child objects are not equal.

Those assertions demonstrate that local numbering did not cause database key collisions or incorrectly equate children of different parents.

## 8. JPA repositories and adapters

We introduced a shared Spring Data contract:

```java
@NoRepositoryBean
public interface DomainJpaRepository<T extends Entitate>
        extends JpaRepository<T, Long> {
    Optional<T> findByDomainId(Integer id);
}
```

The `Long` parameter describes the technical database key. `findByDomainId(Integer)` preserves application lookups by the existing domain identifier.

We created concrete Spring Data interfaces for the aggregate roots, plus adapters such as `CerereRepositoryJpa`, `InteresRepositoryJpa` and `RezultatRepositoryJpa`. They implement the interfaces already used by our services and inherit shared behavior from `JpaRepositoryAdapter`.

A small adapter is intentional: it does not need to reimplement `add`, `findById`, collection retrieval or removal if those methods are inherited. Spring Data generates the underlying entity repository implementation.

We added derived queries including `OfertaJpaRepository.findByStare(...)` and `InteresJpaRepository.findByClient_DomainId(...)`. The latter navigates to the client's domain ID, not its technical key.

We used `@Profile("memory")` on legacy implementations and `@Profile("!memory")` on JPA adapters. We configured explicit JPA repository scanning through `JpaPersistenceConfiguration`, keeping it inactive in memory mode.

Our adapter delegates CRUD to Spring Data. Although we use `EntityManager` for technical integration such as sequence-based domain-ID allocation, we did not implement the optional complete manual EntityManager CRUD repository as a separate bonus feature.

## 9. Transactional application operations

We placed Spring `@Transactional` boundaries around application service operations. A service call can load existing entities, execute domain rules and save related changes as one unit.

For clients, this covers request updates, interest registration and viewing operations. For transaction initiation/cancellation, it covers changes spanning the result, listing and interest.

We reused managed entities and Hibernate dirty checking. An update to a loaded related entity can be persisted within the surrounding transaction without building a second, disconnected copy of the same person, listing or interest.

We used `saveAndFlush` in the shared adapter to send pending SQL promptly. **Flush is not commit:** an error before successful transaction completion can still roll back the changes. Section 13 explains how we test this distinction.

We retained workflow event publication, but the existing workflow event history is held in memory. It is not a durable event log and is not evidence of committed database state; an in-memory event record may remain even if a later database operation rolls back.

## 10. Persistent factory and sample data

We kept `IRealEstateEntityFactory` as the construction contract. `JpaRealEstateEntityFactory` builds domain objects through their validated constructors and delegates sample initialization to `DemoDataSeeder`.

We made seeding transactional and opt-in through the demo profile or an explicit initialization call. The seeder uses a PostgreSQL advisory transaction lock, checks the durable `demo_dataset` marker, builds the fictional sample records and stores their domain IDs in that marker.

We created a fresh helper for each seeding attempt, avoiding process-local cached IDs from a failed, rolled-back attempt. Reading sample data uses the stored marker rather than assuming that a Java field initialized in a previous process still exists.

We tested repeated initialization across a new application context. This checks durable idempotence: restarting the context should reuse the sample dataset rather than duplicate it. A normal PostgreSQL startup without demo still performs migration/validation, but does not automatically insert sample data.

## 11. Transaction initiation: integration-test walkthrough

Our main happy-path test is `initiatesAndCancelsTransactionAcrossCommittedContexts` in `PostgresPersistenceTest`.

### Preparation

We call `scenario(true)`. That helper obtains sample IDs, assigns and activates the sale listing, registers the client's interest, schedules and confirms a viewing, and records feedback with `DecizieClient.CONTINUA`. Recording the result completes the viewing.

This setup matters because the transaction requires an active interest and a completed viewing. We did not bypass the prerequisites by inserting a result directly into SQL.

### Service call

We call `tranzactii.initiazaTranzactie(...)` with the interest ID, responsible agent and valid date. The JPA service reuses supported behavior from the base transaction service. The factory constructs `Rezultat`, the domain validates entry conditions, the listing is reserved, the interest enters the transaction state, the initial participant is added and the result is saved within one service transaction.

### Important assertions after commit and reload

| Assertion | What it demonstrates |
|---|---|
| Result state is `INITIAT` | The newly initiated result was stored and reloaded |
| Listing state is `REZERVATA` | The related listing change was persisted |
| `verificaRezervare(result)` succeeds | The listing's reservation belongs to this particular result |
| Interest state is `IN_TRANZACTIE` | The client's interest entered the persistent transaction workflow |
| Participant count is 1 | The initial transaction participant was cascaded correctly |
| Participant's result is the loaded result instance | The loaded association is consistent inside this persistence context |

`assertSame` here checks identity within one persistence context; we did not use it to claim that the exact same Java instance survives a restart.

## 12. Transaction cancellation: integration-test walkthrough

We then call `anuleazaTranzactie` with the stored result ID, cancellation reason and a valid later date. We load the result in another transaction before checking the outcome.

| Assertion | What it demonstrates |
|---|---|
| Result state is `ANULAT` | Cancellation was saved |
| Listing state is `ACTIVA` | The cancelled transaction released the listing |
| Interest state is `ACTIV` | The client interest returned to its active state |
| Reason equals `Clientul renunta` | The cancellation explanation survived reload |

We retained the cancelled result as a record instead of deleting the transaction. The implemented persistent cancellation scope is cancellation of an initiated transaction; we did not claim cancellation of signed contracts.

### The integration test

```java
  @Test
  void initiatesAndCancelsTransactionAcrossCommittedContexts() {
    Scenario s = scenario(true);
    Integer result =
        tranzactii.initiazaTranzactie(
            s.interesId(), s.sample().agentVanzareId(), s.sample().dataReferinta().plusDays(2));
    tx(
        () -> {
          Rezultat r = rezultate.getById(result);
          assertEquals(StareRezultat.INITIAT, r.getStare());
          assertEquals(StareOferta.REZERVATA, r.getOferta().getStare());
          r.getOferta().verificaRezervare(result);
          assertEquals(StareInteres.IN_TRANZACTIE, r.getInteres().getStare());
          assertEquals(1, r.getParticipanti().size());
          assertSame(r, r.getParticipanti().getFirst().getRezultat());
          return null;
        });
    tranzactii.anuleazaTranzactie(
        result, "Clientul renunta", s.sample().dataReferinta().plusDays(3));
    tx(
        () -> {
          Rezultat r = rezultate.getById(result);
          assertEquals(StareRezultat.ANULAT, r.getStare());
          assertEquals(StareOferta.ACTIVA, r.getOferta().getStare());
          assertEquals(StareInteres.ACTIV, r.getInteres().getStare());
          assertEquals("Clientul renunta", r.getMotivAnulare());
          return null;
        });
  }
```

We used `TransactionTemplate` in the test helper. Each helper call clears the persistence context, runs the supplied operation and flushes before returning; successful completion commits that transaction. The test class itself is not wrapped in one automatic rollback-only transaction.

The important evidence is the combination of service calls, successful commits and fresh reload assertions. Checking only the original result object immediately after mutation would not establish persistence.

## 13. Failure cases and rollback evidence

### Missing completed viewing

In `missingViewingCannotReserveListing`, we prepare `scenario(false)`, so the viewing is not completed. We assert that initiation throws `RegulaBusinessException`, that no result was stored and that the listing remains `ACTIVA` after reload.

The exception proves rejection; the later state/count assertions prove that rejection did not leave a reservation behind.

### Deliberate database failure

In `insertFailureRollsBackReservationAndInterest`, we temporarily add a constraint that rejects result insertion. We expect the initiation call to fail, remove the temporary constraint in `finally`, and inspect the stored state again.

```java
  @Test
  void insertFailureRollsBackReservationAndInterest() {
    Scenario s = scenario(true);
    jdbc.execute("ALTER TABLE rezultate ADD CONSTRAINT simulated_failure CHECK (false)");
    try {
      assertThrows(
          RuntimeException.class,
          () ->
              tranzactii.initiazaTranzactie(
                  s.interesId(),
                  s.sample().agentVanzareId(),
                  s.sample().dataReferinta().plusDays(2)));
    } finally {
      jdbc.execute("ALTER TABLE rezultate DROP CONSTRAINT simulated_failure");
    }
    assertEquals(0, rezultate.size());
    tx(
        () -> {
          assertEquals(StareOferta.ACTIVA, oferte.getById(s.sample().ofertaVanzareId()).getStare());
          assertEquals(StareInteres.ACTIV, interese.getById(s.interesId()).getStare());
          return null;
        });
  }
```

The important assertions are result count zero, listing `ACTIVA` and interest `ACTIV`. Together, they demonstrate rollback of related changes even though domain mutation and SQL execution had already begun. The failure is intentional and belongs only in the disposable test database.

### Invalid feedback and appointment conflicts

We also test rejected duplicate interests, conflicting exact-time appointments and invalid feedback. After a rejected interest-level value of 6, we reload the original viewing and confirm that it remains `CONFIRMATA` instead of becoming a partially updated completed viewing.

## 14. Deferred transaction features

We deliberately limited persistent transaction support. The following features remain deferred:

| Feature | PostgreSQL status in this delivery |
|---|---|
| Initiate transaction after a completed viewing | Implemented |
| Reserve listing and persist the initial agent participant | Implemented |
| Cancel an initiated transaction and release listing | Implemented |
| Add further participants through the persistent service | Deferred |
| Price proposals, counteroffers and acceptance/rejection | Deferred |
| Contract preparation, terms negotiation and signing | Deferred |
| Commission setup and advance/commission payment recording | Deferred |
| Transaction completion and related request satisfaction | Deferred |
| Rental closure and early termination as a persistent workflow | Deferred |

We left `Contract` and the proposal collection in `Rezultat` marked `@Transient`. They are not persisted through those fields. We did not add the corresponding contract/proposal tables in the minimal migration.

We prevented unsupported workflows from appearing to succeed and then losing their data:

- `JpaTranzactieApplicationService` explicitly rejects advanced service operations.
- Domain guards restrict advanced operations on persisted/reloaded results.
- `@PrePersist`/`@PreUpdate` reject advanced state built before its first save.
- The database constraint `ck_rezultat_minimal` limits stored result states to `INITIAT` and `ANULAT`.

We tested representative rejection paths through both the service and the loaded domain object. We also tested rejection of advanced in-memory state before its first save. We did not remove the guards simply to make more API methods callable.

Future implementation requires coordinated mappings, migrations, service changes and persistence tests. Existing memory behavior is useful as a business-rule specification, but does not replace that work.

## 15. Memory tests versus PostgreSQL evidence

We retained both forms of verification because they answer different questions.

| Test category | Evidence provided | What it does not prove |
|---|---|---|
| Pure domain tests | Rules and state transitions on Java objects | SQL mappings, committed data or restart behavior |
| Spring tests with `memory` profile | Service coordination using legacy in-memory repositories | Database persistence, foreign keys or SQL rollback |
| `FlywayMigrationTest` | SQL migration discovery/application and repeat execution on PostgreSQL | Full entity mappings or business service integration |
| `PostgresPersistenceTest` with `test` profile | Real PostgreSQL mappings, committed reloads, relationships and rollback | Deferred contract/payment persistence or exhaustive concurrency behavior |

For example, the previous negotiation or contract-completion tests can pass in memory while those same features are deliberately unavailable in PostgreSQL mode. We do not present those results as proof of persistent negotiation or contracts.

The PostgreSQL integration class contains 13 scenarios in the minimal reference implementation, including Listings, Clients/Viewings, factory behavior, minimal transactions, audit and scope guards. We report actual executed test totals from our build output; adding the independent Flyway test changes the suite compared with the original reference count.

## 16. Docker, Flyway and GitHub Actions

### Docker

We defined separate PostgreSQL services in `compose.yaml`: a persistent development database on host port 5432 and a disposable test database on host port 5433. This lets teammates reproduce the database configuration while keeping destructive tests away from development data.

We kept database records in PostgreSQL storage, not in Git. Sharing Compose configuration gives each developer a consistent local setup; it does not mean every laptop automatically connects to one shared server.

### Flyway

We placed the initial schema in `src/main/resources/db/migration/V1__persistence_minimal.sql`. Flyway applies versioned SQL and records migration history/checksums in `flyway_schema_history`.

We configured Hibernate with `ddl-auto=validate`: Hibernate checks mappings against the migrated schema instead of silently evolving it. Once a migration is applied, later changes belong in a new version. We do not edit an applied V1, including adding comments to its distributed SQL file.

### GitHub Actions

We configured the workflow to prepare JDK 25, create an isolated PostgreSQL service and run `./mvnw --batch-mode clean verify` on pull requests to main and pushes to main. The CI database is independent of development data and is configured through `TEST_DB_*` variables.

We use a successful run on the relevant commit as evidence. A configured workflow alone is not a passing CI result, and requiring the check before merge is a separate repository rule that must be enabled in GitHub.

## 17. How we verify the implementation

From the repository root in PowerShell:

```powershell
# Start the isolated test database before executing the suite.
docker compose --profile tests up -d --wait postgres-test
.\mvnw.cmd "-Dtest=FlywayMigrationTest" test
.\mvnw.cmd "-Dtest=PostgresPersistenceTest" test
.\mvnw.cmd clean verify
```

We use the targeted tests for diagnosis and the final full verification before merge. The integration suite checks the actual database name for the `_test` suffix before truncating application tables. This is a practical safeguard, not a substitute for selecting the correct disposable server. We do not run destructive suites concurrently against the same database.

For the persistent demo after integration:

```powershell
# Development/demo records use the persistent database, not the test service.
docker compose up -d --wait postgres
.\mvnw.cmd "-Dspring-boot.run.profiles=demo" spring-boot:run
```

We inspect `demo_dataset` and row counts, stop the application and start it again on the same database to check that initialization does not duplicate records. A normal PostgreSQL run without demo does not automatically seed sample data.

The final runtime configuration uses PostgreSQL by default. During incremental integration we temporarily used `spring.profiles.default=memory`; we remove that temporary line in L11 while preserving explicit memory profiles on the legacy tests.

We keep actual commands, commit identifiers, test summaries and CI links in our build-verification record. We do not claim a teammate's current main is fully integrated solely because this document describes the completed implementation.

## 18. Relevant files

Paths below are relative to the repository root.

| Purpose | Files |
|---|---|
| Shared identity/audit | `src/main/java/ro/agentie/domain/shared/Entitate.java`, `PersistenceAuditListener.java` in the same package |
| Persons and agents | `src/main/java/ro/agentie/domain/shared/Persoana.java`, `Agent.java`, `AgentZona.java`, `Zona.java` |
| Listings | `src/main/java/ro/agentie/domain/listing/Proprietate.java`, `Oferta.java`, `ImgOferta.java` |
| Clients/Viewings | `src/main/java/ro/agentie/domain/client/Cerere.java`, `CerereZona.java`, `Interes.java`, `Vizionare.java` |
| Minimal transaction model | `src/main/java/ro/agentie/domain/transaction/Rezultat.java`, `AgentRezultat.java` |
| Application boundaries | `src/main/java/ro/agentie/application/ListingApplicationService.java`, `ClientApplicationService.java`, `JpaTranzactieApplicationService.java` |
| JPA repositories/factory | `src/main/java/ro/agentie/infrastructure/jpa/` |
| PostgreSQL integration tests | `src/test/java/ro/agentie/PostgresPersistenceTest.java` |
| Independent migration test | `src/test/java/ro/agentie/FlywayMigrationTest.java` |
| Schema migration | `src/main/resources/db/migration/V1__persistence_minimal.sql` |
| Local infrastructure | `compose.yaml` |
| CI | `.github/workflows/build.yml` |
