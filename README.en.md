# OpenProtégé

An open platform for ontology engineering, semantic knowledge modeling, and collaborative governance.

> **Project stage: Web backend foundation plus the first ontology file API slice**
>
> The Spring Boot/PostgreSQL foundation, local invited accounts/sessions, team/project APIs, and an initial RDF/XML/Turtle ontology import, version metadata, and export API slice are implemented. The React UI, desktop client, ontology editing/restore, round-trip acceptance, maximum-file resource tests, full collaboration, and Compose bridge deployment acceptance remain incomplete.

**Language:** [中文](README.md) | English

## Product direction

OpenProtégé aims to support individuals and teams working on ontology engineering through a platform with desktop and web experiences:

- **Desktop:** a complete local ontology-editing workflow.
- **Web:** project management and team collaboration, with public/private projects and role-based permissions.
- **Initial cross-client workflow:** user-initiated file import and export; real-time synchronization is deferred for evaluation.
- **Ontology formats:** OWL 2 is the core target, with RDF/XML, Turtle, and other common formats prioritized for validation.
- **Deployment direction:** self-hosting first.
- **AI extensions:** the initial direction is suggestion-based modeling and read-only semantic search; users must confirm any write that applies an AI suggestion.

These statements record confirmed product direction. They do not mean the technology has been selected, implemented, or validated. In particular, parsers and reasoners, desktop/web integration, role details, and real-time synchronization still require evaluation and decisions.

## Current repository status

The initial source-free baseline commit, `d9caafee6858a958ea7a2944d574407a79f88309`, contains only the README. The current working tree now includes a `server/` backend and PostgreSQL Compose foundation; do not mistake the initial repository state for the current code.

Current scope and boundaries:

- Java 21 / Spring Boot 3, PostgreSQL 17, Flyway schema, Actuator readiness, explicit first-admin bootstrap, local sessions/CSRF, one-time invitations, team/project APIs, and initial server-side role checks are implemented. PostgreSQL Testcontainers tests pass for invitation replay rejection, sessions/CSRF/logout, and team/project isolation boundaries; Compose bridge networking remains blocked in the current environment.
- The ontology API currently supports RDF/XML/Turtle import, project authorization, original-file version snapshots, version list/metadata, and export. Version editing/restore/deletion, the React UI, desktop client, full account recovery/brute-force protection, and production multi-instance session strategy are not implemented.
- Small ontology parsing/integration samples pass; Pizza round-trip fidelity and performance/resource safety at the 500 MiB maximum remain unverified.
- AI, semantic search, and agent capabilities are not implemented features of the current checkout.
- The project license has not been selected; Apache-2.0 is being evaluated as a candidate.

See the [engineering documentation index](docs/engineering/README.md) for the audit baseline, limitations, and unresolved validation work.

## Documentation

| Document | Contents |
|---|---|
| [Product vision](docs/product/vision.md) | Product positioning, confirmed direction, and scope boundaries |
| [Draft SRS](docs/requirements/SRS.md) | Functional and non-functional requirements, separating product decisions from technical validation |
| [Draft use cases](docs/requirements/use-cases.md) | Local editing, team projects, file exchange, and AI-assisted scenarios |
| [Draft acceptance criteria](docs/requirements/acceptance-criteria.md) | Future verifiable criteria; Web foundation AC-13 has test evidence, while product features remain unverified |
| [Requirements traceability matrix](docs/requirements/traceability-matrix.csv) | Requirements mapped to acceptance criteria and implementation status |
| [Identity/team/project data model](docs/architecture/data-model.md) | Initial entities, authorization boundaries, and open validation items |
| [ADR-0002](docs/architecture/adr/0002-identity-team-project-authorization.md) | Identity and project authorization implementation baseline |
| [Engineering audit and stage report](docs/engineering/README.md) | Repository baseline, architecture inventory, build/test status, security, upstreams, risks, and roadmap |

Requirements and engineering documents are primarily in Chinese. English translation status is listed in the [engineering documentation index](docs/engineering/README.md). This English README does not imply that the other documents have complete English translations.

## Development, build, and tests

The Web service module requires Docker Engine/Compose v2. All 5 tests (including PostgreSQL Testcontainers integration tests) passed in Java 21 / Maven 3.9.16 using Docker API `1.40`, covering the service foundation and identity/project authorization. This environment's `server/target` and default Maven cache are not writable, so validation ran against an isolated source copy and temporary Maven cache. On a writable checkout, run:

```sh
mvn -B -ntp -Dapi.version=1.40 -f server/pom.xml test
```

For a first empty database, set `ADMIN_BOOTSTRAP_EMAIL` and `ADMIN_BOOTSTRAP_PASSWORD`; the application has no default credentials. Ontology uploads default to 500 MiB and can be adjusted with `OPENPROTEGE_ONTOLOGY_MAX_FILE_SIZE`; this limit is not evidence that maximum-size performance has passed. See the [build and test baseline](docs/engineering/03-build-and-test-baseline.md) for Compose limitations. There is no Web UI or ontology editor yet.

## Upstream projects and license

Protégé Desktop and WebProtégé are under technical evaluation; they should not be taken as components already integrated into OpenProtégé or as evidence of features already available here. See the [upstream audit](docs/engineering/upstream-audit.md) for the limited static review of pinned upstream commits and its validation boundaries.

The OpenProtégé license has not been selected. Apache-2.0 is only a candidate under evaluation; code reuse and distribution terms must not be inferred before license and dependency review is complete.

## Contributing

Contribution guidelines, a code of conduct, and a security reporting channel have not yet been established in this repository. Links will be added when those policies are published.

## Project links

- GitHub: <https://github.com/SpecOmega/openprotege>
- Target website: <https://openprotege.com> (a project target; current site status and service availability have not been verified)
