# OpenProtégé

An open platform for ontology engineering, semantic knowledge modeling, and collaborative governance.

> **Project stage: engineering baseline and draft requirements**
>
> This repository does not yet contain a runnable desktop application, web application, or backend service. The product direction described here is not a claim of delivered functionality. There are currently no verified installation, build, or run commands. Requirements and architecture materials remain drafts.

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

The audited repository HEAD is `d9caafee6858a958ea7a2944d574407a79f88309`. That commit contains only the initial README, with no application source, dependency manifest, build script, or tests. The engineering and requirements documents currently in the working tree are baseline drafts, not a runnable product.

Therefore:

- There is no runnable desktop or web product yet.
- There are no project-defined build, test, installation, or deployment commands.
- Ontology import, editing, validation, saving, export, and format round-trip fidelity have not been verified.
- Authentication, server-side authorization, project isolation, and collaboration have not been verified.
- AI, semantic search, and agent capabilities are not implemented features of the current checkout.
- The project license has not been selected; Apache-2.0 is being evaluated as a candidate.

See the [engineering documentation index](docs/engineering/README.md) for the audit baseline, limitations, and unresolved validation work.

## Documentation

| Document | Contents |
|---|---|
| [Product vision](docs/product/vision.md) | Product positioning, confirmed direction, and scope boundaries |
| [Draft SRS](docs/requirements/SRS.md) | Functional and non-functional requirements, separating product decisions from technical validation |
| [Draft use cases](docs/requirements/use-cases.md) | Local editing, team projects, file exchange, and AI-assisted scenarios |
| [Draft acceptance criteria](docs/requirements/acceptance-criteria.md) | Future verifiable acceptance conditions; current implementation is unverified |
| [Requirements traceability matrix](docs/requirements/traceability-matrix.csv) | Requirements mapped to acceptance criteria and implementation status |
| [Engineering audit and stage report](docs/engineering/README.md) | Repository baseline, architecture inventory, build/test status, security, upstreams, risks, and roadmap |

Requirements and engineering documents are primarily in Chinese. English translation status is listed in the [engineering documentation index](docs/engineering/README.md). This English README does not imply that the other documents have complete English translations.

## Development, build, and tests

The current checkout has no application source or build/test configuration, so there are no project installation, build, test, or launch instructions to provide yet. No build or test results are claimed.

Once the technology stack has been selected and implemented, this section will be updated with verified development commands, environment versions, and test instructions.

## Upstream projects and license

Protégé Desktop and WebProtégé are under technical evaluation; they should not be taken as components already integrated into OpenProtégé or as evidence of features already available here. See the [upstream audit](docs/engineering/upstream-audit.md) for the limited static review of pinned upstream commits and its validation boundaries.

The OpenProtégé license has not been selected. Apache-2.0 is only a candidate under evaluation; code reuse and distribution terms must not be inferred before license and dependency review is complete.

## Contributing

Contribution guidelines, a code of conduct, and a security reporting channel have not yet been established in this repository. Links will be added when those policies are published.

## Project links

- GitHub: <https://github.com/SpecOmega/openprotege>
- Target website: <https://openprotege.com> (a project target; current site status and service availability have not been verified)
