# 패키지 구조 리팩토링 계획

[체크리스트](checklist.md) · [결정 기록](context-notes.md) · 결과: `result.md`(작업 후 작성)

작업 브랜치: `volume-3/refacto` · 기준 브랜치: `volume-3/main` · 대상: `apps/commerce-api` (main + test)

상태: 커밋 1(domain)·커밋 2(infrastructure)·커밋 3(application)·커밋 4(interfaces) 완료.
커밋 4는 사용자 요청으로 ArchUnit·전체 테스트 없이 컴파일 + Checkstyle만 통과 확인했다(추후 커밋 5 전에 실행 필요). 커밋 5(결과 문서) 진행 예정.

## 배경

현재 모든 레이어가 `layer.context.feature`(예: `application.mall.product`)로 나뉘어 있고, feature 폴더 하나에 역할이 다른 파일이 섞여 있다.
`application.mall.product`에는 UseCase·Service·Command·Result·QueryDao·조회 record 15개가 평평하게 놓여 있고,
조회 모델 이름도 `BrandDetail`, `AdminProduct`, `LikeItem`, `UserQueryModel`처럼 제각각이다.
이번 작업은 feature 대신 **종류(kind)** 기준으로 파일을 다시 묶고 조회 모델 이름을 통일한다. 동작 변경은 없다.

## 목표 구조

- `domain.<ctx>.<kind>`, `application.<ctx>.<kind>`, `interfaces.api.<ctx>.<kind>` — 컨텍스트 우선
- `infrastructure.<kind>.<ctx>[.<subkind>]` — infrastructure만 종류 우선(사용자 결정)
- `<ctx>`: `mall`, `ordering`, `pay`, `shopping`
- 손대지 않는 패키지: `domain.shared`, `domain.support`, `application.common`, `application.support`,
  `interfaces.api` 루트 파일과 `interfaces.api.support`, `support.*`, 모든 `example` 패키지,
  테스트의 `architecture`, `fixtures`, `support`, `CommerceApiContextTest`

## 작업 규칙 (실행자는 반드시 지킨다)

1. **컨텍스트 간 이동 금지.** 파일은 지금 속한 컨텍스트 안에서만 움직인다(예: `ProductLikeCountQueryDao`는 mall에 남는다).
2. **로직 변경 금지.** 바뀌는 것은 `package` 선언, `import`, 아래 명시한 클래스 이름, 아래 명시한 접근제어자뿐이다.
3. **파일 이동은 `git mv`** 로 한다. 파일 첫 줄의 한국어 헤더 주석은 그대로 둔다.
4. **테스트는 대상 클래스와 같은 커밋·같은 패키지로 이동**한다. 아래 매핑표에 테스트 위치를 명시했다.
5. **접근제어 확대는 `UserJpaRepository` → `public` 한 건만 허용**한다. mapper는 JpaEntity와 같은 `entity` 폴더에 두므로
   JpaEntity의 package-private 생성자는 그대로 둔다. 그 밖에 가시성 때문에 컴파일이 깨지면 **임의로 넓히지 말고 멈추고 보고**한다.
6. 같은 패키지였다가 다른 패키지로 갈라진 타입은 명시적 `import`를 추가한다. Checkstyle은 와일드카드 import와
   미사용 import를 금지하므로 `import ...*;`를 쓰지 않고, 이동 후 필요 없어진 import는 지운다.
7. 이름 변경은 **타입 이름만** 바꾼다. 변수명·메서드명·JSON 필드명은 바꾸지 않는다(응답 JSON 형태 불변).
8. Checkstyle·ArchUnit 규칙이나 테스트 기대값을 완화하지 않는다.
9. 커밋 메시지는 기존 형식을 따른다: `타입: 한글 요약` 제목 + 빈 줄 + `- ` 불릿 본문. **`Co-Authored-By` 줄을 넣지 않는다.**

## 커밋 계획

| # | 커밋 제목 | 범위 |
|---|---|---|
| 0 | `docs: 패키지 구조 리팩토링 계획과 결정 사항 정리` | docs/refactor/plan.md, checklist.md, context-notes.md |
| 1 | `refactor: domain 패키지를 컨텍스트·종류 구조로 재배치` | domain + 이를 import하는 모든 파일 |
| 2 | `refactor: infrastructure 패키지를 종류·컨텍스트 구조로 재배치` | infrastructure + 관련 테스트 |
| 3 | `refactor: application 패키지를 컨텍스트·종류 구조로 재배치하고 조회 모델명을 View로 통일` | application + 이를 import하는 infrastructure/interfaces/테스트 |
| 4 | `refactor: interfaces 패키지를 컨텍스트·종류 구조로 재배치` | interfaces + 관련 테스트 |
| 5 | `docs: 패키지 리팩토링 결과와 구조 설명 갱신` | docs/refactor/result.md, CLAUDE.md의 "commerce-api package structure", AGENTS.md 32행 |

각 레이어 커밋에는 해당 단계의 checklist.md 체크와 context-notes.md 추가 기록을 함께 포함한다.

## 커밋 1 — domain (`com.loopers.domain.<ctx>.<kind>`)

| 새 패키지 | 파일 |
|---|---|
| `domain.mall.model` | Brand, Product, Stock |
| `domain.mall.repository` | BrandRepository, ProductRepository |
| `domain.ordering.model` | Order, OrderItem, OrderStatus, OrderConfirmation |
| `domain.ordering.policy` | OrderConfirmationPolicy |
| `domain.ordering.repository` | OrderRepository |
| `domain.pay.model` | Wallet, PointBill, PointBillType, OrderBill, OrderBillStatus |
| `domain.pay.repository` | WalletRepository, PointBillRepository, OrderBillRepository |
| `domain.shopping.model` | User |
| `domain.shopping.repository` | UserRepository |

테스트: BrandTest·ProductTest·StockTest → `domain.mall.model`, OrderTest·OrderItemTest → `domain.ordering.model`,
OrderConfirmationPolicyTest → `domain.ordering.policy`, WalletTest·PointBillTest·OrderBillTest → `domain.pay.model`,
UserTest → `domain.shopping.model`. `domain.shared.MoneyTest`, `domain.support.error.DomainExceptionTest`는 그대로.

참고: `OrderItem.amountAsMoney()`는 package-private이지만 Order와 OrderItem이 모두 `domain.ordering.model`로 가므로 영향 없다.
기존 `domain/mall/brand`, `domain/mall/product`, `domain/ordering/order`, `domain/pay/orderbill`, `domain/pay/wallet`,
`domain/shopping/user` 폴더(main·test)는 비워서 없앤다.

## 커밋 2 — infrastructure (`com.loopers.infrastructure.<kind>.<ctx>`)

| 새 패키지 | 파일 |
|---|---|
| `infrastructure.persistence.mall.entity` | BrandJpaEntity, BrandEntityMapper, ProductJpaEntity, ProductEntityMapper |
| `infrastructure.persistence.mall.jpa` | BrandJpaRepository, ProductJpaRepository |
| `infrastructure.persistence.mall.repository` | BrandRepositoryImpl, ProductRepositoryImpl |
| `infrastructure.persistence.ordering.entity` | OrderJpaEntity, OrderItemJpaEntity, OrderEntityMapper |
| `infrastructure.persistence.ordering.jpa` | OrderJpaRepository |
| `infrastructure.persistence.ordering.repository` | OrderRepositoryImpl |
| `infrastructure.persistence.pay.entity` | WalletJpaEntity, WalletEntityMapper, PointBillJpaEntity, PointBillEntityMapper, OrderBillJpaEntity, OrderBillEntityMapper |
| `infrastructure.persistence.pay.jpa` | WalletJpaRepository, PointBillJpaRepository, OrderBillJpaRepository |
| `infrastructure.persistence.pay.repository` | WalletRepositoryImpl, PointBillRepositoryImpl, OrderBillRepositoryImpl |
| `infrastructure.persistence.shopping.entity` | UserJpaEntity, UserEntityMapper, LikeJpaEntity, ProductLikeCountJpaEntity |
| `infrastructure.persistence.shopping.jpa` | UserJpaRepository (**`public`으로 변경**) |
| `infrastructure.persistence.shopping.repository` | UserRepositoryImpl |
| `infrastructure.query.mall` | JdbcBrandQueryDao, QueryDslProductQueryDao, JdbcProductLikeCountQueryDao, ProductQueryRow |
| `infrastructure.query.ordering` | JdbcOrderQueryDao, OrderHeaderRow, OrderItemRow (Row는 package-private 유지) |
| `infrastructure.query.pay` | JdbcWalletQueryDao |
| `infrastructure.query.shopping` | JdbcUserQueryDao, JdbcLikeQueryDao |
| `infrastructure.dao.ordering` | JpaConfirmOrderWriter |
| `infrastructure.dao.shopping` | JdbcLikeCommandDao, JdbcLikeCountAggregationDao |
| `infrastructure.scheduler.shopping` | LikeCountAggregationScheduler |
| `infrastructure.initializer.shopping` | LocalUserFixtureInitializer |

테스트:

| 새 패키지 | 테스트 |
|---|---|
| `infrastructure.persistence.mall.repository` | BrandRepositoryIntegrationTest, BrandFindForDeletionLockIntegrationTest, ProductRepositoryIntegrationTest, StockLostUpdateControlGroupTest |
| `infrastructure.persistence.ordering.repository` | OrderRepositoryIntegrationTest |
| `infrastructure.persistence.pay.repository` | WalletRepositoryIntegrationTest, PointBillRepositoryIntegrationTest, OrderBillRepositoryIntegrationTest |
| `infrastructure.persistence.shopping.entity` | UserEntityMapperTest, LikeStorageIntegrationTest |
| `infrastructure.persistence.shopping.repository` | UserRepositoryIntegrationTest |
| `infrastructure.query.ordering` | JdbcOrderQueryDaoIntegrationTest |
| `infrastructure.query.pay` | JdbcWalletQueryDaoIntegrationTest |
| `infrastructure.query.shopping` | JdbcUserQueryDaoIntegrationTest, JdbcLikeQueryDaoIntegrationTest |
| `infrastructure.dao.ordering` | JpaConfirmOrderWriterTest |
| `infrastructure.dao.shopping` | JdbcLikeCommandDaoIntegrationTest |
| `infrastructure.scheduler.shopping` | LikeCountAggregationSchedulerTest |
| `infrastructure.initializer.shopping` | LocalUserFixtureInitializerTest, LocalUserFixtureInitializerIntegrationTest |

참고: `modules/jpa`의 `JpaConfig`는 `@EnableJpaRepositories("com.loopers.infrastructure")`, `@EntityScan("com.loopers")`이므로
새 위치도 그대로 스캔된다. 설정 파일은 수정하지 않는다. `infrastructure.example`은 그대로 둔다.

## 커밋 3 — application (`com.loopers.application.<ctx>.<kind>`)

종류 폴더: `usecase`(UseCase 인터페이스), `service`(구현), `command`, `result`, `query`(QueryDao·조회 View·조회 조건),
`dao`(infrastructure가 구현하는 쓰기 전용 계약과 그 입출력 record).

| 새 패키지 | 파일 |
|---|---|
| `application.mall.usecase` | CreateBrandUseCase, UpdateBrandUseCase, DeleteBrandUseCase, CreateProductUseCase, UpdateProductUseCase, DeleteProductUseCase, SetProductStockUseCase |
| `application.mall.service` | BrandService, ProductService |
| `application.mall.command` | BrandCommand, ProductCommand |
| `application.mall.result` | BrandResult, ProductResult |
| `application.mall.query` | BrandQueryDao, ProductQueryDao, ProductLikeCountQueryDao, ProductCriteria, ProductSort, BrandView, BrandSummaryView, ProductSummaryView, ProductDetailView, AdminProductView |
| `application.ordering.usecase` | ConfirmOrderUseCase, CreateOrderUseCase |
| `application.ordering.service` | ConfirmOrderService, OrderService |
| `application.ordering.command` | ConfirmOrderCommand, OrderCommand |
| `application.ordering.result` | ConfirmOrderResult, OrderResult, OrderItemResult |
| `application.ordering.query` | OrderQueryDao, OrderView, AdminOrderView, OrderItemView |
| `application.ordering.dao` | ConfirmOrderWriter, ConfirmOrderLoad |
| `application.pay.usecase` | ChargeWalletUseCase |
| `application.pay.service` | WalletService |
| `application.pay.command` | WalletCommand |
| `application.pay.result` | WalletResult |
| `application.pay.query` | WalletQueryDao |
| `application.shopping.usecase` | LikeCountAggregationUseCase |
| `application.shopping.service` | LikeCountAggregationService |
| `application.shopping.query` | LikeQueryDao, LikedProductView, UserQueryDao, UserView |
| `application.shopping.dao` | LikeCommandDao, LikeCountAggregationDao |

타입 이름 변경(파일명·타입 선언·모든 참조):

| 기존 | 변경 |
|---|---|
| BrandDetail | BrandView |
| BrandSummary | BrandSummaryView |
| ProductSummary | ProductSummaryView |
| ProductDetail | ProductDetailView |
| AdminProduct | AdminProductView |
| LikeItem | LikedProductView |
| UserQueryModel | UserView |

OrderView·AdminOrderView·OrderItemView는 이미 `*View`라 그대로다. 변수명·메서드명은 바꾸지 않는다.
이름 변경은 단어 경계 기준으로 치환한다(`ProductSummary`를 바꿀 때 `ProductSummaryView`가 다시 치환되지 않도록 주의).

테스트: DeleteBrandRollbackIntegrationTest → `application.mall.service`,
ConfirmOrderServiceTest·ConfirmOrderIntegrationTest·ConfirmOrderConcurrencyIntegrationTest·ConfirmOrderSqlRollbackIntegrationTest·OrderServiceTest → `application.ordering.service`,
WalletServiceTest → `application.pay.service`, LikeCountAggregationIntegrationTest → `application.shopping.service`.
`application.support.error.ApplicationExceptionTest`는 그대로.

이 커밋은 이동·이름 변경된 타입을 참조하는 infrastructure·interfaces·테스트의 import도 함께 고친다.
ArchUnit의 `INFRASTRUCTURE_APPLICATION_SERVICE_DEPENDENCY_RULE`(`application\..*Service` 이름 매칭)은 새 구조에서도 그대로 유효하다.

## 커밋 4 — interfaces (`com.loopers.interfaces.api.<ctx>.<kind>`)

| 새 패키지 | 파일 |
|---|---|
| `interfaces.api.mall.controller` | AdminBrandController, BrandQueryController, AdminProductController, ProductQueryController |
| `interfaces.api.mall.dto` | BrandApiDto, ProductApiDto |
| `interfaces.api.ordering.controller` | OrderController, OrderQueryController, AdminOrderQueryController |
| `interfaces.api.ordering.dto` | OrderApiDto |
| `interfaces.api.pay.controller` | WalletController, WalletQueryController |
| `interfaces.api.pay.dto` | WalletApiDto |
| `interfaces.api.shopping.controller` | LikeController, LikeQueryController |

테스트: BrandApiE2ETest·ProductApiE2ETest → `interfaces.api.mall.controller`, OrderApiE2ETest → `interfaces.api.ordering.controller`,
WalletApiE2ETest → `interfaces.api.pay.controller`, LikeApiE2ETest → `interfaces.api.shopping.controller`.
ApiControllerAdviceTest, ContractClassificationTest, ExampleV1ApiE2ETest, `interfaces.api.support.*` 테스트는 그대로.

## 검증

레이어 커밋마다 저장소 루트에서 실행하고 모두 통과해야 커밋한다.

```bash
./gradlew :apps:commerce-api:compileJava :apps:commerce-api:compileTestJava :apps:commerce-api:checkstyleMain :apps:commerce-api:checkstyleTest
./gradlew :apps:commerce-api:test --tests "com.loopers.architecture.*"
```

추가 확인:
- `git grep`으로 이번 커밋에서 옮긴 기존 패키지명(예: `com.loopers.domain.mall.brand`)과 바뀐 기존 타입명이 `apps/commerce-api/src`에 남아 있지 않다.
- 옮긴 뒤 빈 폴더가 남지 않는다.
- `git diff -M --stat`에서 이동 파일이 rename으로 잡히고, 내용 diff는 package/import/이름/허용된 접근제어뿐이다.

커밋 4 이후, 커밋 5 전에 전체 검사를 실행한다(Docker 필요). 결과는 result.md에 기록한다.

```bash
./gradlew :apps:commerce-api:check
```
