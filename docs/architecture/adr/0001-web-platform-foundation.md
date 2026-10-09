# ADR-0001: Web Platform Foundation

- Status: Accepted for the first Web foundation module
- Date: 2026-10-09
- Decision owner: OpenProtégé project owner

## Context

The audited repository had no application source, server, database, or
selected technology stack. The product baseline requires self-hosted Web
project management and collaboration, but project, user, and authorization
models are not yet frozen. A first increment must establish a runnable,
testable service and database without inventing those domain models.

## Decision

- Backend runtime and language: Java 21 with Spring Boot 3.
- Relational persistence: PostgreSQL, with Flyway as the schema migration
  mechanism.
- Web frontend: React and TypeScript are selected for the future Web UI; this
  ADR does not implement that UI.
- OWL parsing: OWLAPI is selected as the parser family for the future ontology
  ingestion module. The PoC's OWLAPI 4.5.29 result is evidence for one sample,
  not a final product dependency-version decision.
- The first module provides process startup, database connectivity, minimal
  readiness probes, local container orchestration, and integration tests
  against disposable PostgreSQL.
- No product domain schema or unauthenticated product API is introduced until
  the identity, project, and authorization module is reviewed.

## Consequences

- The first module is infrastructure only; it does not satisfy project
  creation, membership, or ontology workflows.
- Database schema migrations must be added with the reviewed business module
  that owns each schema change.
- Compose binds published ports to loopback by default. Operators must set
  database credentials explicitly.
- PostgreSQL, Spring Boot, and dependency versions need ongoing update and
  license review before a production release.
- No claim is made that the legacy WebProtégé codebase is being reused.

## Verification

Acceptance evidence is recorded with the module implementation. Required
checks: Java 21 Maven build, Docker-backed integration tests, Compose
configuration validation, and HTTP readiness with the PostgreSQL health
dependency active.

### Results (2026-10-09)

- **VERIFIED:** Java 21 / Maven 3.9.16 Testcontainers run against PostgreSQL
  17 passed 3 tests (0 failures/errors/skips), including readiness UP/DOWN,
  database query, and a test-only Flyway migration. The Docker API override
  `-Dapi.version=1.40` was required by the current daemon.
- **VERIFIED:** Compose configuration validation passed; the service image
  built. With host networking, the service connected to PostgreSQL and the
  readiness endpoint returned HTTP 200.
- **BLOCKED:** Compose bridge networking in the current execution environment
  timed out when connecting the service to `database:5432`. The Compose
  readiness healthcheck and `up --wait` now fail visibly rather than treating
  the process as ready. Re-run the Compose deployment check on a Docker host
  with working bridge networking before accepting this deployment path.
