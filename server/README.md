# OpenProtégé Web Server Foundation

This module provides the Java 21 / Spring Boot backend, identity/team/project
APIs, RDF/XML/Turtle ontology import/version/export, and an optional
OpenAI-compatible AI chat proxy. The React frontend is built into the Spring
Boot jar by the root Compose image. The desktop client, ontology editing and
restore/deletion, round-trip acceptance, browser E2E, and production deployment
acceptance are not complete.

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

The root Docker build requires npm registry access and builds the frontend
before packaging the Spring Boot static resources.

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
| `POST /api/teams/{teamId}/members` | Team Owner/Admin; assign Admin/Member by existing account UUID or email |
| `GET /api/teams/{teamId}/members` | Team Owner/Admin |
| `GET, POST /api/projects` | Authenticated user's project memberships; create a personal or team project |
| `GET, PUT /api/projects/{projectId}` | Read authorized metadata; Owner/Admin updates metadata |
| `GET, POST /api/projects/{projectId}/members` | List members / set a member role by existing account UUID or email; management access requires Project Owner/Admin |
| `POST /api/projects/{projectId}/ontologies/import` | Project Owner/Admin/Editor; multipart upload of RDF/XML or Turtle; creates immutable snapshot |
| `GET /api/projects/{projectId}/ontologies/versions` | Authorized project reader; list version metadata (`pageNum`, `pageSize`) |
| `GET /api/projects/{projectId}/ontologies/versions/{versionId}` | Authorized project reader; get version metadata |
| `GET /api/projects/{projectId}/ontologies/versions/{versionId}/export` | Authorized project reader, including anonymous public-project readers; optional `format=RDF/XML|Turtle` |
| `POST /api/projects/{projectId}/ontologies/versions/{versionId}/reasoning/validate` | Authorized project reader; OWL 2 DL profile, consistency and unsatisfiable named classes |
| `POST /api/projects/{projectId}/ontologies/versions/{versionId}/reasoning/hierarchy` | Authorized project reader; request `{"classIri":"https://example.org/Thing","direct":false}` for inferred super/subclasses |

Team-project membership requires current team membership and explicit project
membership. Anonymous users may only read public projects. Private projects
that the caller cannot access return `404` to avoid disclosing their existence.
The current invitation lifetime is 24 hours (an implementation default pending
product-policy confirmation). Sessions are held by the server's in-memory
servlet session store; a restart invalidates sessions and multi-instance
sharing is not configured. No ontology editing endpoint exists yet.
Ontology upload defaults to 500 MiB. Set `OPENPROTEGE_ONTOLOGY_MAX_FILE_SIZE`
to adjust the file limit; `OPENPROTEGE_ONTOLOGY_MAX_REQUEST_SIZE` adjusts the
multipart request limit. Maximum-size performance and memory use have not been
validated. Remote `owl:imports` are not fetched; parser resolution maps them to
a temporary local empty document and retains the import declaration.
Reasoning uses HermiT (OWL 2 DL); it is bounded to 100,000 axioms and 60
seconds by default. Configure `OPENPROTEGE_REASONING_MAX_AXIOMS` and
`OPENPROTEGE_REASONING_TIMEOUT` (for example `45s`) to tune these limits.
Only one reasoning task runs at a time; concurrent requests receive `503`,
oversized ontologies receive `413`, timeout receives `504`, profile failures
receive `422`, and unknown classes receive `404`. The axiom limit applies
after parsing, not as a parser memory/complexity limit. Cancellation and
resource behavior for adversarial or very large ontologies remain unverified.

## Optional AI chat

AI is disabled by default. Configure the server environment (never browser
variables) to enable an OpenAI-compatible provider:

```sh
export OPENPROTEGE_AI_ENABLED=true
export OPENPROTEGE_AI_PROVIDER=deepseek
export OPENPROTEGE_AI_BASE_URL=https://api.deepseek.com/v1
export OPENPROTEGE_AI_MODEL=deepseek-chat
export OPENPROTEGE_AI_API_KEY='<secret from your secret manager>'
```

Other providers may be used when they implement
`POST {base-url}/chat/completions`. `GET /api/ai/status` reports only whether
the provider is configured and its provider/model labels; it never returns a
key. `POST /api/ai/projects/{projectId}/chat` requires an authenticated user
with project read access. Prompts are sent to the configured external provider.
This first slice does not include ontology context, semantic search, AI
generated changes, or project write-back.

## Test

Integration tests start disposable PostgreSQL 17 containers and verify
readiness, migrations, invitation acceptance/replay, session login/logout,
CSRF, team/project authorization, ontology parsing, import/export, version
metadata, audit outcomes, editor/viewer access, OWL 2 DL validation,
consistency/unsatisfiable class reporting, and inferred class hierarchy.

```sh
mvn -Dapi.version=1.40 -f server/pom.xml test
```

Tests require a working Docker daemon. Flyway migrations create the identity,
team/project, and ontology file-version/audit schemas.

The API override is required in the current validation environment, whose
daemon rejects Testcontainers' default API 1.32. Use a version supported by
the target Docker daemon when running tests elsewhere. The Compose application
healthcheck probes database-aware readiness; if Docker bridge networking does
not allow the service to reach PostgreSQL, `docker compose up --wait` fails
rather than reporting a running-but-unready service as healthy.
