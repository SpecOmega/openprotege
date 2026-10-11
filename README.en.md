# OpenProtégé

An open platform for ontology engineering, semantic knowledge modeling, and collaborative governance.

> **Project stage: Web workspace, backend APIs, and first ontology/AI/reasoning slices**
>
> The Spring Boot/Web foundation supports selectable PostgreSQL or MySQL persistence, local invited accounts/sessions, team/project APIs, a React web workspace, RDF/XML/Turtle ontology import/version/export/history restore, an initial HermiT OWL 2 DL profile/consistency/explanation/classification and read-only SWRL reasoning slice, and an opt-in OpenAI-compatible AI chat proxy configured with server-side secrets. The desktop client, ontology entity editing, round-trip acceptance, maximum-file resource testing, full collaboration, browser E2E testing, and Compose bridge deployment acceptance remain incomplete.

**Language:** [中文](README.md) | English

## Product direction

OpenProtégé aims to support individuals and teams working on ontology engineering through a platform with desktop and web experiences:

- **Desktop:** a complete local ontology-editing workflow.
- **Web:** project management and team collaboration, with public/private projects and role-based permissions.
- **Initial cross-client workflow:** user-initiated file import and export; real-time synchronization is deferred for evaluation.
- **Ontology formats:** OWL 2 is the core target, with RDF/XML, Turtle, and other common formats prioritized for validation.
- **Deployment direction:** self-hosting first.
- **AI extensions:** the initial direction is suggestion-based modeling and read-only semantic search; users must confirm any write that applies an AI suggestion.

These statements record confirmed product direction. They do not mean all capabilities are implemented or validated. HermiT is selected for the first OWL 2 DL reasoning slice; desktop/web integration, role details, and real-time synchronization still require evaluation and decisions.

## Current repository status

The initial source-free baseline commit, `d9caafee6858a958ea7a2944d574407a79f88309`, contains only the README. The current working tree now includes a `server/` backend and PostgreSQL Compose foundation; do not mistake the initial repository state for the current code.

Current scope and boundaries:

- Java 21 / Spring Boot 3, selectable PostgreSQL 17/MySQL 8.4 Flyway schemas, Actuator readiness, explicit first-admin bootstrap, local sessions/CSRF, one-time invitations, team/project APIs, and initial server-side role checks are implemented. PostgreSQL and MySQL Testcontainers suites pass; Compose bridge networking remains blocked in the current environment.
- The React UI includes local login/invitation acceptance, team/project listing and creation, project member settings, ontology snapshot import/version browsing/download/format conversion/history restore as a new immutable snapshot, and AI chat. Browser flows have not received E2E acceptance. Ontology entity editing/deletion, the desktop client, full account recovery/brute-force protection, and production multi-instance sessions are not implemented.
- Small ontology parsing/integration samples pass; Pizza round-trip fidelity and performance/resource safety at the 500 MiB maximum remain unverified.
- HermiT provides OWL 2 DL profile/consistency checks, unsatisfiable-class and bounded global inconsistency explanations, class hierarchy and individual classification; one Manchester-syntax SWRL rule can be applied transiently without saving changes. The initial service is limited to one active worker, 100,000 axioms, and 60 seconds by default; timeout cancellation, concurrent load, large/adversarial ontology resource use, and browser E2E remain unverified.
- AI chat can be configured for an OpenAI-compatible provider (with a DeepSeek configuration template); it is disabled by default, API keys remain server-side, and the chat neither reads ontologies nor writes projects. Semantic search and Agents are not implemented; real provider connectivity and privacy review remain outstanding.
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
| [Development and deployment guide](docs/guides/development-and-deployment.md) | Step-by-step setup, native database configuration, dev run, Compose deployment, backups, and troubleshooting (Chinese) |
| [Chinese end-to-end developer scenario](docs/guides/examples/business-scenario-zh.md) | Community-care ontology, roles, executable API workflow, and UI/E2E boundary |
| [English end-to-end developer scenario](docs/guides/examples/business-scenario-en.md) | Learning ontology, role permissions, executable API workflow, and UI/E2E limits |
| [Identity/team/project data model](docs/architecture/data-model.md) | Initial entities, authorization boundaries, and open validation items |
| [ADR-0002](docs/architecture/adr/0002-identity-team-project-authorization.md) | Identity and project authorization implementation baseline |
| [Engineering audit and stage report](docs/engineering/README.md) | Repository baseline, architecture inventory, build/test status, security, upstreams, risks, and roadmap |

Requirements and engineering documents are primarily in Chinese. English translation status is listed in the [engineering documentation index](docs/engineering/README.md). This English README does not imply that the other documents have complete English translations.

## Development, build, and tests

Local development does not require Docker. It requires Java 21, Maven 3.9+,
Node.js 22+, and a locally installed/running PostgreSQL 17 or MySQL 8.4
service. Docker Engine/Compose v2 is only needed for production-style
container deployment and optional containerized tests.

Build and run the frontend separately:

```sh
npm ci --prefix web
npm --prefix web run dev
```

### Local development and debugging

Spring profiles combine `APP_PROFILE` and `DB_VENDOR`; the default local mode
is `dev,postgres`. Dev connects directly to the database on `127.0.0.1` and
does not start or depend on a database container. The dev profile enables
application-package DEBUG logging and development error messages. Production
Compose continues to use `prod`. Vite proxies `/api` and `/actuator` to
`http://localhost:8080`; override the target with `VITE_API_TARGET`.

For a full setup walkthrough—including database installation/creation,
PowerShell instructions, first login, production deployment, backups, and
troubleshooting—see the [step-by-step development and deployment guide
(Chinese)](docs/guides/development-and-deployment.md).

```sh
cp .env.dev.example .env
# Optionally change the local-only credentials in .env.
# Create the configured database and user in your local PostgreSQL/MySQL service
# and make sure that service is running.
# Terminal 1 (Bash): run the backend
set -a; . ./.env; set +a
mvn -f server/pom.xml spring-boot:run
# Terminal 2: install dependencies once, then run the hot-reloading Web client
npm ci --prefix web
npm --prefix web run dev
```

For PostgreSQL, create a role and database with
`CREATE ROLE openprotege LOGIN PASSWORD '<local-dev-password>';` and
`CREATE DATABASE openprotege OWNER openprotege;`. For MySQL, set
`DB_VENDOR=mysql` and `DB_PORT=3306` in `.env`, then create and grant access to
the configured database/user:

```sql
CREATE DATABASE openprotege CHARACTER SET utf8mb4;
CREATE USER 'openprotege'@'127.0.0.1' IDENTIFIED BY '<same local password as DB_PASSWORD>';
GRANT ALL PRIVILEGES ON openprotege.* TO 'openprotege'@'127.0.0.1';
```

The application applies the selected Flyway migrations at startup.

With the VS Code Java extension, press F5 and select `OpenProtégé Server (dev)`
when `.env` exists at the workspace root and your local database is running.
The launch starts the Vite frontend task as well. The production Docker image
continues to serve the Web bundle from the backend on the same origin.

### Switch between PostgreSQL and MySQL

PostgreSQL 17 is the default for development and production. For local
development, set `DB_VENDOR` to `postgres` or `mysql`, point `DB_HOST` at the
local database, and set `DB_PORT` (5432 for PostgreSQL, 3306 for MySQL). Each
engine uses its own JDBC driver and Flyway migrations. MySQL ontology content
uses `LONGBLOB`, and UUIDs are stored in canonical text form. Selecting another
engine does not migrate data; back up data and configure a database account for
each engine.

For production Compose, the database data volume must be dedicated to its
engine; never share one volume between PostgreSQL and MySQL. Example MySQL
Compose settings:

```dotenv
APP_PROFILE=prod
DB_VENDOR=mysql
DB_IMAGE=mysql:8.4
DB_PORT=3306
DB_CONTAINER_PORT=3306
DB_VOLUME=mysql_data
DB_DATA_PATH=/var/lib/mysql
MYSQL_ROOT_PASSWORD=<a separate strong root password>
```

Keep `DB_USER`, `DB_PASSWORD`, `DB_NAME`, and administrator bootstrap settings
from `.env`. This selects an engine for a new database; it is not a
PostgreSQL-to-MySQL migration tool. Back up existing data and verify
`DB_VOLUME` points to the dedicated target volume before running
`docker compose up --build --wait`.

Run backend tests (including PostgreSQL Testcontainers integration tests) with Docker API `1.40`:

```sh
mvn -B -ntp -Dapi.version=1.40 -f server/pom.xml test
```

### Production Compose deployment (local)

For detailed PostgreSQL/MySQL configuration, operations, and data-protection
instructions, see the [production deployment chapter of the step-by-step guide
(Chinese)](docs/guides/development-and-deployment.md#九使用-docker-compose-部署生产模式).
Quick start:

```sh
cp .env.example .env
chmod 600 .env
# Edit .env with dedicated database credentials and administrator credentials.
# Bootstrap password must be 12–128 characters.
docker compose config --quiet
docker compose up --build --wait
curl --fail http://127.0.0.1:8080/actuator/health/readiness
```

Application and database ports bind to `127.0.0.1` by default. Configure a
separately managed TLS reverse proxy before external access. The administrator
is bootstrapped only on an empty database; remove the bootstrap password after
the first successful login. `docker compose down` preserves the named database
volume; removing volumes can permanently delete database data. Upload/reasoning
limits and optional server-side AI configuration are documented in the
step-by-step guide.

## Upstream projects and license

Protégé Desktop and WebProtégé are under technical evaluation; they should not be taken as components already integrated into OpenProtégé or as evidence of features already available here. See the [upstream audit](docs/engineering/upstream-audit.md) for the limited static review of pinned upstream commits and its validation boundaries.

The OpenProtégé license has not been selected. Apache-2.0 is only a candidate under evaluation; code reuse and distribution terms must not be inferred before license and dependency review is complete.

## Contributing

Contribution guidelines, a code of conduct, and a security reporting channel have not yet been established in this repository. Links will be added when those policies are published.

## Project links

- GitHub: <https://github.com/SpecOmega/openprotege>
- Target website: <https://openprotege.com> (a project target; current site status and service availability have not been verified)
