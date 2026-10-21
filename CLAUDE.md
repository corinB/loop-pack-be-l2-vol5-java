# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Working rules for `apps/commerce-api`

The repo is preparing Week-5 work. [docs/week5/total_requirement.md](docs/week5/total_requirement.md) is authoritative for Week-5 branching, PR boundaries, preparation status, and acceptance requirements. The base branch is `volume-5/main`, created from `volume-4/main` at `9eff402`. Only requirements, design questions, and implementation-plan scaffolds are prepared; feature implementation and verification have not started. Detailed design and commit/TDD plans are decided when each PR starts. [AGENTS.md](AGENTS.md) retains its historical Week-2 title; its current technical conventions (package layout, layer dependencies, domain/UseCase conventions, testing, Checkstyle/ArchUnit gates, what not to change) still apply.

Week-5 uses three PRs: R01 `volume-5/r01-pg-payment-foundation` (test PG, HTTP adapter, payment/stock-debit record models), R02 `volume-5/r02-pg-order-payment` (stock debit before PG payment, idempotency, callback/reconciliation), and R03 `volume-5/r03-payment-resilience` (lookup resilience, integrated failure verification, submission documentation). Create each work branch from the latest `volume-5/main` when its work starts and target that base; do not create all work branches during preparation or merge work branches directly.

The user requires a small independently runnable local test PG that commerce calls over actual HTTP. PG transactions must be stored in a dedicated PG schema on the existing MySQL server and survive a PG restart. The earlier in-memory storage choice was withdrawn; an in-process HTTP fixture alone does not complete this requirement. Payment-create requests carry a selected test preset and optional overrides; unspecified conditions retain the preset values. A separate scenario control API and the axis-only option were withdrawn. Preserve the original paths/basic fields and record the added request options as a user-approved extension, without modifying the reference text. PG stores the transaction and applicable test conditions and completes it automatically; processing time and create/lookup response delays are independent. Runtime module, ports, schema/access grants, initialization, presets, exact option fields/source and callback processing remain design questions. PG background execution/restart recovery is undecided and deferred while discussing commerce payment-attempt ownership and recovery.

Commerce payment attempts are independent pay aggregates with their own repository and a scalar orderId reference. Preserve OrderRecord as the Order-owned confirmation record. Application code coordinates order, attempt and stock within the same commerce database transaction. Attempt state transitions, stock-debit record ownership and exact persistence design are still undecided.

The confirmed stock policy is debit and commit before calling PG outside the DB transaction, keep the debit on verified success, and restore once on definitive failure. PENDING/UNKNOWN keeps stock debited. The historical single-transaction point-confirmation rule does not apply unchanged to this PG boundary. Preserve past POINT payment facts, and never charge points for new PG payments. Design details remain open in the per-PR trade-off lists; preparation documents are not implementation or test results.

Week-4 history remains in [docs/week4/total_requirement.md](docs/week4/total_requirement.md). Its base was `volume-4/main`, created from `corinB` at `72ae005` (the final Week-3 merge). Caffeine local caching was the final Week-4 choice; Redis is outside that selected scope, not unfinished work. Preserve the original assignment and prior decisions as history. HTTP performance testing uses JMeter; collect SQL counts and execution plans separately from application/MySQL observations.

Key points worth restating because they're easy to violate accidentally:
- Week-5 base is `volume-5/main`; the three work branch names and boundaries are fixed above. Their detailed plans remain unwritten. Historical bases were `volume-4/main` and `volume-3/main`; do not use historical week-specific branch instructions for new Week-5 work.
- Commit messages are a `type: <one Korean summary sentence>` subject (e.g. `refactor: 좋아요 등록·취소 유스케이스 추가`), a blank line, then a body of `- <Korean detail>` bullets (hyphen, not asterisk) describing what changed and why (classes/files touched, decisions, verification), with **no** `Co-Authored-By`/AI attribution trailer — this applies to delegated agents' commits too.
- Existing APIs use fixture-user input (`X-USER-ID`), not authentication, and historically excluded auth/authz, including ownership checks. Week-5's original ownership/admin requirements differ from that policy. Record the gap and resolve its application in R02's design before implementing it; this preparation does not decide to add Spring Security, ADMIN roles, CSRF, or change existing API access behavior.
- Concurrency control is **no longer blanket-excluded**: R02 (`docs/week3/r02-order-consistency/`) explicitly brings locking/version-check/retry/concurrent-request testing into scope for order-confirm/stock/point consistency. Don't assume the old Week-2 "skip concurrency" rule still holds — check the specific requirement's `requirement.md` and `trade_off/` docs before adding or omitting it.
- Implementation topics use the existing documentation workflow under `docs/week5/`: `requirement.md` (scope/rules/scenarios), `trade_off/total_trade_off.md` + numbered topic files added after discussion (alternatives, decision, open questions), `plan.md` (currently a scaffold; fill in commit-by-commit TDD steps after design), `result.md` (actual outcome/deviations, created after implementation and verification). Historical folders stay in place. Week-4 R01 included the explicitly activated benchmark seed and pre-index baseline; see [its historical plan](docs/week4/r01-product-list-index/plan.md). Keep fixture JDBC isolated in `Jdbc*` infrastructure classes and a separate transaction; business writes remain JPA. Preserve the completed indexes and Caffeine cache behavior when connecting Week-5 stock debit/restoration.
- `volume-3/r01-brand-bulk-delete` (brand bulk-delete cascades to owned products in one transaction with full rollback on failure) is implemented, reviewed, and merged into `volume-3/main` — see [docs/week3/r01-brand-bulk-delete/result.md](docs/week3/r01-brand-bulk-delete/result.md).
- `volume-3/r07-brand-delete-bulk` (R07, see [plan.md](docs/week3-2/r07-brand-delete-bulk/plan.md)): brand delete is now `BrandService` → `findByIdForUpdate` (brand row only) → `brand.delete()` → `brandRepository.save` → `productRepository.deleteAllByBrandId` (one JPQL bulk UPDATE of the brand's active products on the `(deleted, brand_id, …)` index). The brand→products association, `Brand.products`, `restoreForDeletion` and `findForDeletion` were removed; product creation reads the brand with `findByIdForShare` (`FOR SHARE`) so it is ordered against a concurrent brand delete.
- `volume-3/r02-order-consistency` (order-confirm atomicity plus stock/point concurrency control via JPA `PESSIMISTIC_WRITE` locked reads) is implemented and merged (PR #14).
- `volume-3/refacto` (merged into `volume-3/main`, PR #15): a package-structure refactor of `apps/commerce-api` from `layer.context.feature` to `layer.context.kind` (see [docs/refactor/plan.md](docs/refactor/plan.md) and [docs/refactor/result.md](docs/refactor/result.md)), which also carried `OrderBill` (pay) into ordering's `OrderRecord`, a 1:1 child entity of the `Order` aggregate (see R02 trade-offs [10](docs/week3/r02-order-consistency/trade_off/10-order-record-ownership.md) and [11](docs/week3/r02-order-consistency/trade_off/11-order-record-aggregate.md)), plus a test-slimming pass that cut redundant cross-layer coverage and tagged heavy tests (see [docs/refactor/test-slimming.md](docs/refactor/test-slimming.md)).
- Follow-up optimization refactors R03–R06 (JDBC→QueryDSL/JPA plus like-count aggregation, like-sort index, order-confirm locking, repository save/flush) are tracked in [docs/week3-2/total_requirement.md](docs/week3-2/total_requirement.md), which is authoritative for their order, branches (`volume-3/r0N-<name>`, cut from the latest `volume-3/main`), and workflow (one Q&A per trade-off topic, then `plan.md`, then implementation delegated to a Sonnet agent). Data-access rule from that doc: reads use QueryDSL, writes use JPA, and `JdbcClient` is reserved for batch work (like-count aggregation) that never shares a transaction with JPA writes. R03 is merged (PR #16) — see its [result.md](docs/week3-2/r03-jdbc-and-like-aggregation/result.md). R04 (like counts moved to `products.like_count` with like-sort indexes, cached no-filter total count) is merged (PR #18) — see its [result.md](docs/week3-2/r04-like-sort-index/result.md). R07 (brand delete via bulk product UPDATE) is merged (PR #19). R08 (removal of `JdbcClient` from test code plus an ArchUnit guard, see [result.md](docs/week3-2/r08-test-jdbc-removal/result.md)) is merged (PR #20). R09 (order-confirm facade, see [result.md](docs/week3-2/r09-order-confirm-facade/result.md)) is on `volume-3/r09-order-confirm-facade`. R05 (order-confirm locking) and R06 (repository save/flush) were dropped before their trade-off Q&A (2026-10-09); their `requirement.md` files are kept only as records — do not implement them unless the user revives them.
- Never relax Checkstyle/ArchUnit rules or test expectations to make a check pass; fix the code instead.

R02's final cache choice is [Caffeine product-detail caching](docs/week4/r02-product-detail-local-cache/requirement.md): W-TinyLFU, full immutable customer detail View, opt-in (default off), maximum 1000 entries, expire-after-write 30s, synchronous after-commit invalidation and generation-guarded fills. Preserve the QueryDao contract and bypass caching inside an existing transaction. The measured single-JVM repeated reads reduced DB calls without a separate cache server. Multi-instance sharing and propagation of direct SQL changes are unsupported; Redis performance and failure/recovery were not compared or verified. See [the CLI measurement guide](docs/week4/r02-product-detail-local-cache/jmeter-guide.md) for OFF/ON experiments against the existing fixture without schema or server-setting changes.

## Commands

All commands run from the repo root; the `commerce-api` app is the only one with real feature code.

```bash
# start local infra (MySQL, etc.) the app depends on for the `local` profile
docker-compose -f ./docker/infra-compose.yml up

# optional local monitoring stack (prometheus/grafana on :3000, admin/admin)
docker-compose -f ./docker/monitoring-compose.yml up

# full check (build + tests + Checkstyle + ArchUnit) for commerce-api
./gradlew :apps:commerce-api:check

# fast tests (excludes JUnit tags `slow` and `example`)
./gradlew :apps:commerce-api:test

# heavy tests only (concurrency/locking tests tagged `slow`, plus `example` scaffolding tests)
./gradlew :apps:commerce-api:slowTest

# a single test class or method
./gradlew :apps:commerce-api:test --tests "com.loopers.domain.shopping.model.UserTest"
./gradlew :apps:commerce-api:test --tests "com.loopers.domain.shopping.model.UserTest.메서드이름"

# Checkstyle only
./gradlew :apps:commerce-api:checkstyleMain :apps:commerce-api:checkstyleTest
```

`check` (and `build`) runs everything in a single `test` task execution: when either is on the command line, `apps/commerce-api/build.gradle.kts` drops the `slow`/`example` exclusion from `test`, so one JVM and one set of Spring contexts cover all tests (plus Checkstyle), and `slowTest` is not run. Standalone `test` still excludes those tags, and `slowTest` still runs only them. `check` is the only task that covers everything.

Integration tests use Testcontainers (MySQL only — commerce-api tests do not start a Redis container) via `modules/jpa`'s `MySqlTestContainersConfig`, so Docker must be running for any test that touches the DB. To reuse the MySQL container across local runs, add `testcontainers.reuse.enable=true` to `~/.testcontainers.properties` (has no effect, and no CI impact, if absent). In a git worktree, Testcontainers needs `apps/commerce-api/src/test/resources/docker-java.properties` (git-ignored, `api.version=1.44`) copied in, or it fails to connect to Docker with `BadRequestException (Status 400)`.

## Architecture

Multi-module Gradle project (Java 21, Spring Boot). Module roles, per [README.md](README.md):
- `apps/*` — runnable Spring Boot applications: `commerce-api` (the active one), `commerce-batch`, `commerce-streamer`.
- `modules/*` — reusable, non-domain-specific configuration: `jpa`, `redis`, `kafka`. `modules/jpa` provides `BaseEntity`, `DataSourceConfig`/`JpaConfig`/`QueryDslConfig`, and test fixtures (`MySqlTestContainersConfig`, `DatabaseCleanUp`) shared across apps.
- `supports/*` — add-ons: `jackson`, `logging`, `monitoring`.

Root `build.gradle.kts` applies shared config to all subprojects (Java 21 toolchain, Lombok, JUnit5 + Mockito + Instancio + Testcontainers, jacoco), disables `bootJar`/`jar` appropriately per module type, and forces `apps/*` container projects (`apps`, `modules`, `supports` themselves) to skip their own tasks.

### `commerce-api` package structure

Packages are organized **layer → bounded context → kind**, not feature-first (as of the `volume-3/refacto` package refactor — see [docs/refactor/plan.md](docs/refactor/plan.md)). Contexts are `mall`, `shopping`, `ordering` (order, with its `OrderRecord` 1:1 child), `pay` (wallet + point bill) (business domains from the Loopers assignment), plus a legacy `example` context kept as reference-only scaffolding (do not extend it, do not delete it).

```
com.loopers
├── interfaces.api.<context>.{controller,dto}          # Controller (HTTP-facing validation), Request/Response DTOs
├── application.<context>.{usecase,service,facade,command,result,query,dao,event}
│                                                       # UseCase interface, *Service impl, Command, Result,
│                                                       # QueryDao + query records, write-side dao contracts,
│                                                       # application events (e.g. ProductLikeChangedEvent)
├── domain.<context>.{model,repository,policy}          # pure domain model, Repository interface, policy objects
└── infrastructure.{persistence,query,dao,scheduler,initializer}.<context>[.<subkind>]
                                                        # infrastructure is kind-first, then context:
                                                        # persistence.<context>.{entity,jpa,repository} — JpaEntity + EntityMapper,
                                                        # Spring Data JpaRepository, RepositoryImpl
                                                        # query.<context> — QueryDSL-based QueryDao impl
                                                        # dao.<context> — write-side/batch dao impl (e.g. JdbcLikeCountAggregationDao)
                                                        # scheduler.<context>, initializer.<context>
```

Read models (application `query`) all end in `*View` (e.g. `BrandView`, `ProductSummaryView`, `UserView`); write results (application `result`) end in `*Result`. `dao` under `application.<context>` holds write-side contracts that `infrastructure.dao.<context>` implements (`LikeCountAggregationDao`) — distinct from `query`'s read-only `QueryDao` contracts. EntityMappers live in the same `entity` package as their JpaEntity, since JpaEntity constructors are package-private and only the mapper calls them.

Dependency direction is enforced by ArchUnit (`LayerArchitectureTest`, `DomainPurityArchitectureTest`):
- `domain` depends on nothing else in `com.loopers` (and, for the four real contexts + `domain.shared`, on no Spring/JPA/Servlet types, and not on `BaseEntity`).
- `application` must not depend on `interfaces` or `infrastructure`.
- `interfaces` must not depend on `infrastructure`.
- `infrastructure` must not depend on `interfaces`, nor on any `application.*Service` class (it may depend on `application` QueryDao contracts and query-record types).

Writes flow `interfaces → application UseCase → domain + infrastructure RepositoryImpl`. Reads bypass UseCase/Service entirely: a query-only Controller calls an `application` `QueryDao` contract directly. All QueryDao implementations use QueryDSL (`QueryDsl*QueryDao`, backed by `modules/jpa`'s `QueryDslConfig`) with `Projections.constructor` into Views or infra Row records — never entity selects (R03, [trade-off 04](docs/week3-2/r03-jdbc-and-like-aggregation/trade_off/04-query-conversion.md)). Writes use JPA; `JdbcClient` remains only in the batch like-count aggregation DAO (enforced by `LayerArchitectureTest.JDBC_DEPENDENCY_RULE`: no class in the layer packages other than `Jdbc*` classes may depend on `org.springframework.jdbc`). Tests no longer use `JdbcClient`/`JdbcTemplate` either (R08): data setup/verification goes through `JPAQueryFactory` (QueryDSL) and domain repositories. There is intentionally no query UseCase/Service layer. The no-brand-filter total count of the product list is cached briefly by `ExpiringCountCache` (`query.product-count-cache.ttl`, default 30s, `0s` in the `test` profile so tests never see a stale total).

Order confirmation (R09) is orchestrated by `application.ordering.facade.ConfirmOrderFacade` (`@Transactional`, the only transaction boundary): `OrderService.lockForConfirm` → `WalletService.lockByUserId` → `ProductService.decreaseStocks` (ascending product-id locks, validate all in item order, then decrease and save) → `WalletService.pay` → `OrderService.confirm`. The service methods carry no `@Transactional`; a facade may depend on other contexts' `application.*.service` classes. Lock order and error priority come from this call order.

Likes and like counts span several classes (R03, [trade-offs 05–09](docs/week3-2/r03-jdbc-and-like-aggregation/trade_off/total_trade_off.md)):
- Writes go `RegisterLikeUseCase`/`CancelLikeUseCase` (`execute(LikeCommand.*)`) → `LikeService` → `LikeRepository` (independent `Like` aggregate). Registration is a native `INSERT IGNORE` so the duplicate path stays idempotent (200) and `save`/`delete` return `boolean` = "row actually added/removed".
- Only when that is true does `LikeService` publish `ProductLikeChangedEvent(productId, ±1)`; `LikeCountDeltaBuffer` collects it via `@TransactionalEventListener(AFTER_COMMIT)` into an in-memory per-product delta map.
- `LikeCountAggregationScheduler` flushes the map every 5s (`LikeCountDeltaFlushService` → `JdbcLikeCountAggregationDao.addDeltas`, a JDBC batch `UPDATE products SET like_count = GREATEST(0, like_count + ?)` in ascending product-id order, matching R02's lock order; unknown product ids are ignored and failed batches are merged back). The count lives in `products.like_count` (R04; `ProductJpaEntity` maps it `insertable/updatable = false` so JPA saves never overwrite it, and the domain `Product` does not carry it), indexed by `idx_products_deleted_like` / `idx_products_deleted_brand_like` so likes-sort needs no filesort. `LikeCountStartupAggregator` (`SmartInitializingSingleton`, before the web server starts) runs the full recount once (`recountAll()`, a single UPDATE-JOIN against `product_likes`) to repair deltas lost on a crash. Both are disabled by `scheduler.like-count.enabled=false` in the `test` profile, so like counts in tests only change when a test calls the aggregation/flush use cases itself.
- Like counts shown to clients are therefore eventually consistent (≤5s); like/unlike membership is immediate.

Domain classes split validation into two exception styles. Structural invariants checked at construction/`restore` time (non-null/positive id, non-null `createdAt`, positive foreign-key ids) throw a plain `IllegalArgumentException` with a Korean message — see `Brand.restore`, `Product`'s constructor (`brandId` check). Business-rule validation that carries a stable external error code (name/description length, price/stock rules, deleted-state guards) throws `DomainException` with a `DomainErrorCode` entry instead. `User` is the one outlier that uses `DomainException(INVALID_USER_ID)` for its structural id check — don't copy it as the template for new structural checks.

All four contexts now have full domain/infrastructure/application/interfaces implementations from Week 2/3: `mall` (brand, product), `shopping` (user, like — including the aggregation scheduler), `ordering` (order, order record), `pay` (wallet, point bill). Use `domain.mall.model`/`application.mall.service`/`domain.shopping.model` as the cleanest reference for new work.

Two exception hierarchies coexist by design (see `ApiControllerAdvice`): `DomainException`/`DomainErrorCode` for domain-layer business-rule failures, `ApplicationException`/`ApplicationErrorCode` for application-layer existence/cross-object checks, and the older `CoreException`/`ErrorType` from the `example` scaffolding — all three are mapped to the same `ApiResponse` contract via `ApiErrorMapper`.

`@XUserId` (in `interfaces.api.support`) resolves the `X-USER-ID` header by calling `UserQueryDao.findById`: malformed header → 400 without calling the DAO, well-formed but unknown user → 404, otherwise the numeric user id is injected into the controller method. Write-side Services do not re-check user existence — callers are expected to supply a valid user id already resolved upstream.

Checkstyle config is at [config/checkstyle/checkstyle.xml](config/checkstyle/checkstyle.xml), applied only to `commerce-api` (star imports and unused imports banned, zero warnings tolerated). `.editorconfig` caps line length at 130 outside `*Test.java` files, which are exempt.

### Testing

Spring-context tests should use the shared meta-annotations in `com.loopers.support.test`: `@IntegrationTest` (`MOCK` web environment) and `@E2ETest` (`RANDOM_PORT`). Both declare the same type-level `@MockitoSpyBean` set (`OrderRepository`, `ProductJpaRepository`, `JdbcLikeCountAggregationDao`), so all tests using them share just two Spring contexts instead of spinning up a new one per class-level spy combination — use them for any new Spring test rather than declaring `@SpringBootTest`/`@MockitoSpyBean` directly. Layer-specific testing conventions are documented per layer in `docs/test/{domain,application,infrastructure,interfaces}.md`.
