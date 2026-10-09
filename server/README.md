# OpenProtégé Web Server Foundation

This module provides the Java 21 / Spring Boot backend foundation and initial
identity, team, project, and authorization APIs. The React UI, desktop client,
ontology upload/processing/versioning, and production deployment acceptance
are not implemented.

## Local prerequisites

- Docker Engine and Docker Compose v2
- Java 21 and Maven 3.9+ for running the integration tests locally

Compose database/server ports bind to loopback only. Database credentials and
first-administrator bootstrap credentials must be supplied by the operator.
Never commit real credentials or use development passwords for a deployment.
Bootstrap credentials are consumed only when the database contains no users;
there is no default administrator password.

## Start the service and database

```sh
export DB_USER=openprotege
export DB_PASSWORD='replace-with-a-local-secret'
export ADMIN_BOOTSTRAP_EMAIL=admin@example.test
export ADMIN_BOOTSTRAP_PASSWORD='replace-with-a-long-local-secret'
docker compose up --build --wait
```

Remove the bootstrap password from the deployment environment after the first
administrator is created. On a database with users, bootstrap values are
ignored. Platform administrators issue invitations that return a single-use
token; the operator must deliver it securely. The application does not send
email.

Verify application readiness:

```sh
curl --fail http://127.0.0.1:8080/actuator/health/readiness
```

The readiness response reports only `UP` or `DOWN`; component details are not
exposed. Product APIs are under `/api`; public projects are readable
anonymously, while private projects and management operations require a
session. Obtain a CSRF token from `GET /api/auth/csrf`, send its value in
`X-XSRF-TOKEN`, then use `POST /api/auth/login`. Stop the services with
`docker compose down`; persistent database contents remain in `postgres_data`.
Removing data requires an explicit `docker compose down --volumes`.

## Initial API surface

All request/response bodies are JSON. State-changing requests require the CSRF
header described above.

| Method and path | Access |
|---|---|
| `GET /api/auth/me` | Authenticated account summary |
| `POST /api/auth/login`, `POST /api/auth/logout` | Local session login/logout |
| `POST /api/admin/invitations` | Platform administrator; returns a one-time token with `Cache-Control: no-store` |
| `POST /api/auth/invitations/accept` | Public invitation acceptance; sets the invited account password |
| `GET, POST /api/teams` | Authenticated user's teams; create a team |
| `POST /api/teams/{teamId}/members` | Team Owner/Admin; assign Admin/Member |
| `GET /api/teams/{teamId}/members` | Team Owner/Admin |
| `GET, POST /api/projects` | Authenticated user's project memberships; create a personal or team project |
| `GET, PUT /api/projects/{projectId}` | Read authorized metadata; Owner/Admin updates metadata |
| `GET, POST /api/projects/{projectId}/members` | List members / set a member role; management access requires Project Owner/Admin |

Team-project membership requires current team membership and explicit project
membership. Anonymous users may only read public projects. Private projects
that the caller cannot access return `404` to avoid disclosing their existence.
The current invitation lifetime is 24 hours (an implementation default pending
product-policy confirmation). Sessions are held by the server's in-memory
servlet session store; a restart invalidates sessions and multi-instance
sharing is not configured. No ontology editing endpoint exists yet.

## Test

Integration tests start disposable PostgreSQL 17 containers and verify
readiness, migrations, invitation acceptance/replay, session login/logout,
CSRF, and initial team/project authorization boundaries.

```sh
mvn -Dapi.version=1.40 -f server/pom.xml test
```

Tests require a working Docker daemon. The first identity/team/project Flyway
migration is part of the server; ontology and file/version schemas are not
implemented.

The API override is required in the current validation environment, whose
daemon rejects Testcontainers' default API 1.32. Use a version supported by
the target Docker daemon when running tests elsewhere. The Compose application
healthcheck probes database-aware readiness; if Docker bridge networking does
not allow the service to reach PostgreSQL, `docker compose up --wait` fails
rather than reporting a running-but-unready service as healthy.
