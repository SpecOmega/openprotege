# English end-to-end developer scenario: learning services ontology

This scenario uses fictional learning-service data to demonstrate invited accounts, roles, a team project, ontology files, and reasoning through the development backend APIs. The fixture is [en-learning-ontology.ttl](./en-learning-ontology.ttl); the executable workflow is [run_business_scenario.py](./run_business_scenario.py).

> The script creates a team, a private project, four invited accounts, and ontology versions in the configured database. It does not clean up data. Run it only against a local development or disposable acceptance database, never production or a database containing real data. Every run creates a new set of records.

## Prerequisites

1. Follow the [development and deployment guide](../development-and-deployment.md) to configure Java 21, Maven, Node.js, and a local PostgreSQL/MySQL database.
2. Start the backend and local database. The frontend can be started separately with `npm --prefix web run dev`. The default backend URL is `http://127.0.0.1:8080`.
3. Have a valid platform administrator in the database. On first startup, its credentials come from `.env` values `ADMIN_BOOTSTRAP_EMAIL` and `ADMIN_BOOTSTRAP_PASSWORD`.
4. Install Python 3.10+ (the script uses only the standard library; no pip packages are required).

From the repository root, set administrator credentials in the current shell; do not place real passwords in the script or commit them:

```sh
export ADMIN_BOOTSTRAP_EMAIL='your-local-admin-email'
export ADMIN_BOOTSTRAP_PASSWORD='your-local-admin-password'
export DEMO_MEMBER_PASSWORD='a-local-only-password-at-least-12-chars'
python3 docs/guides/examples/run_business_scenario.py \
  --ontology docs/guides/examples/en-learning-ontology.ttl \
  --team-name 'Learning Services Team' \
  --project-name 'Learning Knowledge Model' \
  --iris 'https://example.org/openprotege/en-learning' \
  --individual alice
```

Windows PowerShell:

```powershell
$env:ADMIN_BOOTSTRAP_EMAIL = 'your-local-admin-email'
$env:ADMIN_BOOTSTRAP_PASSWORD = 'your-local-admin-password'
$env:DEMO_MEMBER_PASSWORD = 'a-local-only-password-at-least-12-chars'
python docs/guides/examples/run_business_scenario.py `
  --ontology docs/guides/examples/en-learning-ontology.ttl `
  --team-name 'Learning Services Team' `
  --project-name 'Learning Knowledge Model' `
  --iris 'https://example.org/openprotege/en-learning' `
  --individual alice
```

The script defaults to `http://127.0.0.1:8080`; set `OPENPROTEGE_BASE_URL` or pass `--base-url` for another backend address. Success ends with `PASS` and prints created record IDs and demo-account emails. All four demo accounts use the supplied `DEMO_MEMBER_PASSWORD`, so you can sign into the frontend with each role. Do not reuse the administrator or any real account password. Unexpected HTTP statuses, connection failures, or mismatched reasoning results exit non-zero with an explicit error.

## Business and authorization path

| Identity | Team role | Project role | Operations demonstrated |
|---|---|---|---|
| Local platform administrator | Team `OWNER` | Project `OWNER` | Create team/private project, invite and assign members, full management |
| `team-admin-*` | `ADMIN` | `ADMIN` | Read team members, manage project members, update project metadata |
| `editor-*` | `MEMBER` | `EDITOR` | Import ontology and restore a historical version as a new snapshot; cannot manage members or project metadata |
| `viewer-*` | `MEMBER` | `VIEWER` | Read project and export ontology; cannot import/restore or manage members |
| `team-only-*` | `MEMBER` | Not assigned | Cannot see the private project despite team membership; explicit project membership is required |

The platform administrator issues single-use invitations; the local script immediately accepts each token returned by the API. The product does not send email, so production invitation tokens must be delivered through a secure channel. A team-project member must belong to the team and also be explicitly assigned to the project.

The script performs and asserts these steps:

1. Log in as administrator, create a team, issue four invitations, and activate the accounts.
2. Assign team roles, create a private team project, and assign project `ADMIN`, `EDITOR`, and `VIEWER`.
3. Verify that a team-only account is denied access to the private project; Editor/Viewer cannot manage project membership; Editor cannot update project metadata.
4. Import the Turtle ontology as Editor; export it as Viewer and compare the bytes; verify Viewer import is denied.
5. Run HermiT profile/consistency checks as Viewer and confirm the `Student` individual is inferred to be a `Person`.
6. Preview the SWRL rule `Student(?x) -> ActiveStudent(?x)` for the individual and verify its inferred type; this does not modify the ontology version.
7. Update project metadata as Team Admin; restore the source version as Editor into a new immutable snapshot; compare the restored export with the source and verify that both versions remain.

## Frontend actions and known limits

The frontend at `http://localhost:5173` supports login, browsing teams/projects, project creation, project-member assignment, ontology import/history/export/restore, and Reasoner/Rules panels. The project created by the script appears in the project list. Owner/Admin/Editor ontology writes and Viewer read-only restrictions are enforced server-side.

The executable workflow calls HTTP APIs directly, not a browser. It exercises real backend paths but **is not browser E2E acceptance** and does not prove that every frontend control is visible/usable for every role. Browser E2E, invitation email delivery, account suspension/revocation, entity-level ontology editing, concurrent conflicts, 500 MiB load, and reasoning timeout/concurrency limits remain unverified. If the flow fails against a local database, frontend, or configuration, retain the exact error and environment details and record it in the [known gaps](../../engineering/known-gaps.md); do not report a failed step as passed.
