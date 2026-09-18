# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Working rules for `apps/commerce-api`

Week-2 work on `apps/commerce-api` follows [AGENTS.md](AGENTS.md), which is the authoritative rule set (branching, package layout, layer dependencies, domain/UseCase conventions, testing, Checkstyle/ArchUnit gates, and what not to change). The rationale and examples behind those rules live in [docs/week2/conventions.md](docs/week2/conventions.md); PR scope and sequencing live in [docs/week2/development-plan.md](docs/week2/development-plan.md). Read AGENTS.md before making changes in this app.

Key points worth restating because they're easy to violate accidentally:
- Base branch is `volume-2/main`; work branches are `volume-2/pr-<n>-<name>` and never merge into each other directly.
- Auth/authz (Spring Security, ADMIN role, CSRF, ownership checks) is explicitly out of scope this iteration — do not add it. `X-USER-ID` is a fixture-user input, not authentication.
- Concurrency control (locking, version checks, retries, concurrent-request tests) is deferred to a later iteration — do not add it. Sequential re-confirmation rejection, DB uniqueness constraints, and full single-request rollback are still required.
- Never relax Checkstyle/ArchUnit rules or test expectations to make a check pass; fix the code instead.

## Commands

All commands run from the repo root; the `commerce-api` app is the only one with real feature code.

```bash
# start local infra (MySQL, etc.) the app depends on for the `local` profile
docker-compose -f ./docker/infra-compose.yml up

# optional local monitoring stack (prometheus/grafana on :3000, admin/admin)
docker-compose -f ./docker/monitoring-compose.yml up

# full check (build + tests + Checkstyle + ArchUnit) for commerce-api
./gradlew :apps:commerce-api:check

# tests only
./gradlew :apps:commerce-api:test

# a single test class or method
./gradlew :apps:commerce-api:test --tests "com.loopers.domain.shopping.user.UserTest"
./gradlew :apps:commerce-api:test --tests "com.loopers.domain.shopping.user.UserTest.메서드이름"

# Checkstyle only
./gradlew :apps:commerce-api:checkstyleMain :apps:commerce-api:checkstyleTest
```

Integration tests use Testcontainers (MySQL) via `modules/jpa`'s `MySqlTestContainersConfig`, so Docker must be running for any test that touches the DB.

## Architecture

Multi-module Gradle project (Java 21, Spring Boot). Module roles, per [README.md](README.md):
- `apps/*` — runnable Spring Boot applications: `commerce-api` (the active one), `commerce-batch`, `commerce-streamer`.
- `modules/*` — reusable, non-domain-specific configuration: `jpa`, `redis`, `kafka`. `modules/jpa` provides `BaseEntity`, `DataSourceConfig`/`JpaConfig`/`QueryDslConfig`, and test fixtures (`MySqlTestContainersConfig`, `DatabaseCleanUp`) shared across apps.
- `supports/*` — add-ons: `jackson`, `logging`, `monitoring`.

Root `build.gradle.kts` applies shared config to all subprojects (Java 21 toolchain, Lombok, JUnit5 + Mockito + Instancio + Testcontainers, jacoco), disables `bootJar`/`jar` appropriately per module type, and forces `apps/*` container projects (`apps`, `modules`, `supports` themselves) to skip their own tasks.

### `commerce-api` package structure

Packages are organized **layer → bounded context → feature**, not feature-first. Contexts are `mall`, `shopping`, `ordering`, `pay` (business domains from the Loopers assignment), plus a legacy `example` context kept as reference-only scaffolding (do not extend it, do not delete it).

```
com.loopers
├── interfaces.api.<context>.<feature>     # Request/Response DTOs, Controller, HTTP-facing validation
├── application.<context>.<feature>        # UseCase interface + *Service impl, Command/Result, QueryDao + query records
├── domain.<context>.<feature>             # pure domain model, Repository interface, domain exceptions
└── infrastructure.<context>.<feature>     # JpaEntity, Spring Data JpaRepository, RepositoryImpl, EntityMapper, JdbcClient-based QueryDao impl
```

Dependency direction is enforced by ArchUnit (`LayerArchitectureTest`, `DomainPurityArchitectureTest`):
- `domain` depends on nothing else in `com.loopers` (and, for the four real contexts + `domain.shared`, on no Spring/JPA/Servlet types, and not on `BaseEntity`).
- `application` must not depend on `interfaces` or `infrastructure`.
- `interfaces` must not depend on `infrastructure`.
- `infrastructure` must not depend on `interfaces`, nor on any `application.*Service` class (it may depend on `application` QueryDao contracts and query-record types).

Writes flow `interfaces → application UseCase → domain + infrastructure RepositoryImpl`. Reads bypass UseCase/Service entirely: a query-only Controller calls an `application` `QueryDao` contract directly, implemented in `infrastructure` with Spring JDBC `JdbcClient`. There is intentionally no query UseCase/Service layer.

Two exception hierarchies coexist by design (see `ApiControllerAdvice`): `DomainException`/`DomainErrorCode` for domain-layer business-rule failures, `ApplicationException`/`ApplicationErrorCode` for application-layer existence/cross-object checks, and the older `CoreException`/`ErrorType` from the `example` scaffolding — all three are mapped to the same `ApiResponse` contract via `ApiErrorMapper`.

`@XUserId` (in `interfaces.api.support`) resolves the `X-USER-ID` header by calling `UserQueryDao.findById`: malformed header → 400 without calling the DAO, well-formed but unknown user → 404, otherwise the numeric user id is injected into the controller method. Write-side Services do not re-check user existence — callers are expected to supply a valid user id already resolved upstream.

Checkstyle config is at [config/checkstyle/checkstyle.xml](config/checkstyle/checkstyle.xml), applied only to `commerce-api` (star imports and unused imports banned, zero warnings tolerated). `.editorconfig` caps line length at 130 outside `*Test.java` files, which are exempt.
