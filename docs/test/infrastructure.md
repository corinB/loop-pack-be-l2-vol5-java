# 인프라스트럭처 레이어 테스트

## 1. 개요 및 실행 환경

`infrastructure` 레이어 테스트는 JPA 엔티티/리포지토리, JDBC 기반 QueryDao, 비관적 잠금, 스케줄러, 로컬 fixture 초기화기를 대상으로 한다. 범위는 `apps/commerce-api/src/test/java/com/loopers/infrastructure/**`, 루트의 `CommerceApiContextTest`, `fixtures/UserFixture`, 그리고 `modules/jpa`·`modules/redis`의 `testFixtures`다.

### 컨테이너 구성

- `com.loopers.testcontainers.MySqlTestContainersConfig`(`modules/jpa/src/testFixtures`)는 `@Configuration` 빈으로, `commerce-api`가 `modules:jpa`의 `testFixtures` 의존성을 통해 컴포넌트 스캔으로 가져온다. `static` 블록에서 `mysql:8.0` 컨테이너를 기동하고 `datasource.mysql-jpa.main.*` 시스템 프로퍼티를 설정한다. 따라서 `@SpringBootTest`로 전체 컨텍스트를 올리는 테스트는 DB를 직접 건드리지 않아도 모두 Docker와 MySQL 컨테이너 기동을 필요로 한다.
- `com.loopers.testcontainers.RedisTestContainersConfig`(`modules/redis/src/testFixtures`)도 같은 방식으로 `redis:latest` 컨테이너를 기동하고 `datasource.redis.*` 프로퍼티를 설정한다. 그러나 `commerce-api`는 현재 Redis를 전혀 사용하지 않는다 — 이 컨테이너는 `commerce-api`가 `modules:redis`의 `testFixtures`를 의존하기 때문에 매 `@SpringBootTest` 컨텍스트 로딩마다 불필요하게 함께 기동된다.
- `com.loopers.utils.DatabaseCleanUp`(`modules/jpa/src/testFixtures`)은 `@Component` + `InitializingBean`으로, 컨텍스트 초기화 시 `EntityManager` 메타모델에서 `@Entity` + `@Table` 조합을 스캔해 테이블명 목록을 만든다. `truncateAllTables()`는 `SET FOREIGN_KEY_CHECKS = 0` → 각 테이블 `TRUNCATE` → `SET FOREIGN_KEY_CHECKS = 1` 순으로 실행한다. 현재 엔티티 11종(example, users, product_likes, product_like_counts, wallets, point_bills, orders, order_items, order_records, brands, products)에 대응하는 테이블을 매번 전부 TRUNCATE한다.

### 테스트 규모

대상 테스트 클래스 20개(단위 4개 + 통합/컨텍스트 16개), 총 테스트 케이스 49개.

## 2. 종류·컨텍스트별 테스트

### persistence

| 테스트 클래스 | 컨텍스트 | Spring 컨텍스트 | Docker | MySQL 고유 동작 | 정리 방식 | 검증 시나리오 | 테스트 수 | 비고 |
|---|---|---|---|---|---|---|---|---|
| [`BrandRepositoryIntegrationTest`](../../apps/commerce-api/src/test/java/com/loopers/infrastructure/persistence/mall/repository/BrandRepositoryIntegrationTest.java) | mall | @SpringBootTest | 필요 | - | `@Transactional` + `@AfterEach` TRUNCATE (중복) | 삭제 전용 조회(`findForDeletion`)의 상품 포함 여부, 브랜드 저장 시 연결 상품 삭제 상태 전파 및 무관 필드 보존 | 3 | |
| [`BrandFindForDeletionLockIntegrationTest`](../../apps/commerce-api/src/test/java/com/loopers/infrastructure/persistence/mall/repository/BrandFindForDeletionLockIntegrationTest.java) | mall | @SpringBootTest | 필요 | FOR UPDATE, innodb_lock_wait_timeout | `@AfterEach` TRUNCATE | `findForDeletion`이 브랜드 행뿐 아니라 딸린 상품 행까지 비관적 쓰기 잠금으로 보호하는지, 별도 커넥션으로 짧은 대기시간을 주고 잠금 대기 타임아웃(에러코드 1205)을 관찰 | 1 | 락/동시성 테스트 |
| [`ProductRepositoryIntegrationTest`](../../apps/commerce-api/src/test/java/com/loopers/infrastructure/persistence/mall/repository/ProductRepositoryIntegrationTest.java) | mall | @SpringBootTest | 필요 | - | `@Transactional` + `@AfterEach` TRUNCATE (중복) | 상품 수정 후 flush/clear해도 이름·설명·가격·재고·생성시각 보존 | 1 | |
| [`StockLostUpdateControlGroupTest`](../../apps/commerce-api/src/test/java/com/loopers/infrastructure/persistence/mall/repository/StockLostUpdateControlGroupTest.java) | mall | @SpringBootTest | 필요 | 잠금 없는 SELECT + 상수 UPDATE, `ConcurrentRequests` 유틸 | `@AfterEach` TRUNCATE | 잠금 없이 재고를 읽고 상수로 덮어쓰는 두 트랜잭션이 모두 성공해도 최종 재고가 어긋나는 갱신 유실을 재현 | 1 | 대조군(제품 코드를 전혀 호출하지 않음), 동시성 테스트 |
| [`OrderRepositoryIntegrationTest`](../../apps/commerce-api/src/test/java/com/loopers/infrastructure/persistence/ordering/repository/OrderRepositoryIntegrationTest.java) | ordering | @SpringBootTest | 필요 | native SQL(`order_records` COUNT) | `@Transactional` + `@AfterEach` TRUNCATE (중복) | 주문·품목 저장 후 flush/clear해도 스냅샷·합계 보존, 없는 주문 조회, 확정 시 `OrderRecord` cascade 저장/복원, 주문당 결제 기록 1건 | 4 | 최근 실행 5.8s(gradlew check 기준) |
| [`PointBillRepositoryIntegrationTest`](../../apps/commerce-api/src/test/java/com/loopers/infrastructure/persistence/pay/repository/PointBillRepositoryIntegrationTest.java) | pay | @SpringBootTest | 필요 | - | `@Transactional` + `@AfterEach` TRUNCATE (중복) | 충전/사용 기록 저장 후 flush/clear해도 타입·금액·주문ID 보존 | 2 | |
| [`WalletRepositoryIntegrationTest`](../../apps/commerce-api/src/test/java/com/loopers/infrastructure/persistence/pay/repository/WalletRepositoryIntegrationTest.java) | pay | @SpringBootTest | 필요 | - | `@Transactional` + `@AfterEach` TRUNCATE (중복) | 지갑 저장·충전 후 flush/clear해도 잔액 보존, 없는 사용자 빈 결과 | 2 | |
| [`LikeStorageIntegrationTest`](../../apps/commerce-api/src/test/java/com/loopers/infrastructure/persistence/shopping/entity/LikeStorageIntegrationTest.java) | shopping | @SpringBootTest | 필요 | unique constraint 위반 → `DataIntegrityViolationException` | `@AfterEach` TRUNCATE | 사용자·상품 유일 관계 위반, `product_like_counts` 유일 키 위반을 원시 JDBC INSERT로 검증 | 2 | |
| [`UserEntityMapperTest`](../../apps/commerce-api/src/test/java/com/loopers/infrastructure/persistence/shopping/entity/UserEntityMapperTest.java) | shopping | 없음(순수 단위) | 불필요 | - | 해당 없음 | 도메인↔Entity 왕복 변환 시 사용자 ID 보존 | 1 | |
| [`UserRepositoryIntegrationTest`](../../apps/commerce-api/src/test/java/com/loopers/infrastructure/persistence/shopping/repository/UserRepositoryIntegrationTest.java) | shopping | @SpringBootTest | 필요 | - | `@Transactional`만(TRUNCATE도 있음, 중복) | 할당 사용자 ID 저장 후 flush/clear해도 저장 상태 확인 | 1 | `UserFixture` 사용 |

### query

| 테스트 클래스 | 컨텍스트 | Spring 컨텍스트 | Docker | MySQL 고유 동작 | 정리 방식 | 검증 시나리오 | 테스트 수 | 비고 |
|---|---|---|---|---|---|---|---|---|
| [`JdbcOrderQueryDaoIntegrationTest`](../../apps/commerce-api/src/test/java/com/loopers/infrastructure/query/ordering/JdbcOrderQueryDaoIntegrationTest.java) | ordering | @SpringBootTest | 필요 | native SQL(`JdbcClient`, `created_at` 직접 UPDATE) | `@AfterEach` TRUNCATE만 | 최근 생성순 정렬, 다른 사용자 주문 제외, 결제 필드 포함, 없는 주문, 관리자 목록 전체 조회 | 6 | 최근 실행 8.9s |
| [`JdbcWalletQueryDaoIntegrationTest`](../../apps/commerce-api/src/test/java/com/loopers/infrastructure/query/pay/JdbcWalletQueryDaoIntegrationTest.java) | pay | @SpringBootTest + `@Transactional`(클래스 레벨) | 필요 | - | `@Transactional` + `@AfterEach` TRUNCATE (중복) | JPA로 저장한 잔액을 JDBC로 재조회, 없는 사용자 빈 결과 | 2 | |
| [`JdbcLikeQueryDaoIntegrationTest`](../../apps/commerce-api/src/test/java/com/loopers/infrastructure/query/shopping/JdbcLikeQueryDaoIntegrationTest.java) | shopping | @SpringBootTest | 필요 | native SQL(`JdbcClient` 직접 INSERT), QueryDSL 아님(JdbcClient) | `@AfterEach` TRUNCATE만 | 좋아요 상품을 브랜드·집계와 함께 최근순 반환, 동시각 상품ID 내림차순, 삭제 상품 제외, 브랜드 일괄삭제로 제외, 페이지네이션 | 6 | |
| [`JdbcUserQueryDaoIntegrationTest`](../../apps/commerce-api/src/test/java/com/loopers/infrastructure/query/shopping/JdbcUserQueryDaoIntegrationTest.java) | shopping | @SpringBootTest + `@Transactional`(클래스 레벨) | 필요 | - | `@Transactional`(rollback)만, 별도 TRUNCATE 없음 | JPA 저장 사용자를 JDBC 조회 모델로 조회, 없는 사용자는 빈 결과이며 기존 저장 상태 유지 | 2 | TRUNCATE 호출 없이 rollback에만 의존 |

### dao

| 테스트 클래스 | 컨텍스트 | Spring 컨텍스트 | Docker | MySQL 고유 동작 | 정리 방식 | 검증 시나리오 | 테스트 수 | 비고 |
|---|---|---|---|---|---|---|---|---|
| [`JpaConfirmOrderWriterTest`](../../apps/commerce-api/src/test/java/com/loopers/infrastructure/dao/ordering/JpaConfirmOrderWriterTest.java) | ordering | 없음(Mockito 순수 단위) | 불필요 | - | 해당 없음 | 주문→지갑→상품ID 오름차순(중복 제거) 잠금 조회 순서, 상품재고→지갑잔액→사용기록→주문(기록 cascade) 저장 순서를 `InOrder`로 검증 | 2 | 최근 실행 0.16s |
| [`JdbcLikeCommandDaoIntegrationTest`](../../apps/commerce-api/src/test/java/com/loopers/infrastructure/dao/shopping/JdbcLikeCommandDaoIntegrationTest.java) | shopping | @SpringBootTest | 필요 | `ON DUPLICATE KEY`류 멱등 처리(register/cancel), native SQL(`JdbcClient`) | `@AfterEach` TRUNCATE만 | 활성/삭제/미존재 상품 확인, 좋아요 등록(신규/중복 무시), 취소(존재/미존재 무시) | 7 | |

### scheduler

| 테스트 클래스 | 컨텍스트 | Spring 컨텍스트 | Docker | MySQL 고유 동작 | 정리 방식 | 검증 시나리오 | 테스트 수 | 비고 |
|---|---|---|---|---|---|---|---|---|
| [`LikeCountAggregationSchedulerTest`](../../apps/commerce-api/src/test/java/com/loopers/infrastructure/scheduler/shopping/LikeCountAggregationSchedulerTest.java) | shopping | 없음(Mockito 순수 단위) | 불필요 | - | 해당 없음 | UseCase에 실행 위임, 집계 실패가 전파되지 않아 다음 주기 실행 유지 | 2 | |

### initializer

| 테스트 클래스 | 컨텍스트 | Spring 컨텍스트 | Docker | MySQL 고유 동작 | 정리 방식 | 검증 시나리오 | 테스트 수 | 비고 |
|---|---|---|---|---|---|---|---|---|
| [`LocalUserFixtureInitializerTest`](../../apps/commerce-api/src/test/java/com/loopers/infrastructure/initializer/shopping/LocalUserFixtureInitializerTest.java) | shopping | 없음(인메모리 페이크 리포지토리) | 불필요 | - | 해당 없음 | 반복 실행해도 사용자1·2와 지갑만 한 번씩 저장, 기존 사용자·지갑 보존하며 누락분만 저장 | 2 | `InMemoryUserRepository`/`InMemoryWalletRepository` 사용 |
| [`LocalUserFixtureInitializerIntegrationTest`](../../apps/commerce-api/src/test/java/com/loopers/infrastructure/initializer/shopping/LocalUserFixtureInitializerIntegrationTest.java) | shopping | @SpringBootTest | 필요 | native SQL(`JdbcClient` id 목록 조회) | `@AfterEach` TRUNCATE만 | 실제 DB에서 `TransactionTemplate`로 두 번 실행해도 기존 사용자·fixture 보존 | 1 | `LocalUserFixtureInitializerTest`와 검증 내용 중복 |

### 컨텍스트 로딩

| 테스트 클래스 | 컨텍스트 | Spring 컨텍스트 | Docker | MySQL 고유 동작 | 정리 방식 | 검증 시나리오 | 테스트 수 | 비고 |
|---|---|---|---|---|---|---|---|---|
| [`CommerceApiContextTest`](../../apps/commerce-api/src/test/java/com/loopers/CommerceApiContextTest.java) | 전체 | @SpringBootTest | 필요 | - | 없음 | 애플리케이션 컨텍스트가 정상 로드되는지만 확인 | 1 | 다른 모든 `@SpringBootTest`가 매번 같은 것을 이미 검증하므로 중복 |

## 3. 다른 레이어와 겹치는 검증

- `OrderRepositoryIntegrationTest#enforcesOneOrderRecordPerOrder`는 주문을 한 번 확정·저장한 뒤 `order_records` 카운트가 1임을 확인할 뿐, 두 번째 저장을 시도해 유일성 제약을 실제로 검증하지 않는다. 같은 클래스의 `#savesConfirmedOrder_withCascadedOrderRecord`(확정 시 `OrderRecord`가 cascade로 저장·복원되는지 검증)가 저장 1회·카운트 1인 상태를 이미 포함하는 상위 시나리오라, `enforcesOneOrderRecordPerOrder`는 사실상 그 부분집합이다. 삭제 대상.
- `BrandRepositoryIntegrationTest#findsBrandForDeletion_withAllUndeletedProducts`(미삭제 상품 누락 없이 조회)는 같은 클래스의 `#savesBrand_propagatesDeleteToAllProducts_andKeepsUnrelatedFields`(연결 상품 목록을 조회해 삭제 전파를 검증)가 `findForDeletion`으로 동일한 상품 목록을 조회하는 과정을 이미 거치므로 암묵적으로 커버된다.
- `BrandRepositoryIntegrationTest#savesBrand_propagatesDeleteToAllProducts_andKeepsUnrelatedFields`(브랜드 저장 한 번으로 연결 상품까지 삭제 상태 전파)는 [`BrandApiE2ETest`](../../apps/commerce-api/src/test/java/com/loopers/interfaces/api/mall/controller/BrandApiE2ETest.java)`#deletesCascade_whenActiveProductExists`(활성 상품이 연결된 브랜드를 삭제하면 브랜드·상품이 함께 삭제)와 같은 cascade-delete 규칙을 검증한다. 인프라 레이어에서 이미 저장 단위의 전파를 직접 검증하므로, E2E 쪽 테스트는 HTTP 경로 자체 확인 외에는 중복이다.
- `JdbcLikeQueryDaoIntegrationTest#excludesProducts_deletedViaBrandBulkDelete`(브랜드 일괄 삭제로 상품이 삭제되면 좋아요 목록에서 제외)는 같은 클래스의 `#excludesDeletedProducts`(삭제된 상품은 목록과 전체 개수에서 제외)와 결과적으로 동일한 조건(상품이 삭제 상태)을 검증한다. 삭제 경로가 단건 삭제냐 브랜드 일괄 삭제냐만 다르고 쿼리 결과 검증은 중복.
- `JdbcLikeCommandDaoIntegrationTest`의 `Register#ignoresDuplicateRegistration`(이미 등록된 관계는 다시 저장하지 않고 그대로 성공)과 `Cancel#ignoresCancelOfMissingRelation`(존재하지 않는 관계를 취소해도 예외 없이 성공)이 DAO 레벨에서 멱등성을 이미 검증하므로, [`LikeApiE2ETest`](../../apps/commerce-api/src/test/java/com/loopers/interfaces/api/shopping/controller/LikeApiE2ETest.java)의 대응 케이스인 `#ignoresDuplicateRegistration`(이미 등록된 관계는 다시 등록해도 200이며 관계는 하나로 유지)과 `#ignoresCancelOfMissingRelation`(관계가 없어도 200을 그대로 반환)은 HTTP 계층 확인 외에는 같은 멱등성 로직을 다시 검증하는 셈이다.
- `JdbcOrderQueryDaoIntegrationTest`가 검증하는 목록 정렬(`returnsOrders_orderedByCreatedAtDescending`)·필터링(`excludesOtherUsersOrders`)은 [`OrderApiE2ETest`](../../apps/commerce-api/src/test/java/com/loopers/interfaces/api/ordering/controller/OrderApiE2ETest.java)의 `#returnsOnlyOwnOrders`(헤더로 지정한 사용자의 주문만 조회)가 같은 쿼리 결과를 HTTP 응답을 통해 다시 확인한다.

### 빈 자리

상품 목록 쿼리(`QueryDslProductQueryDao`)에 대한 인프라 레벨 통합 테스트가 없다. 정렬·페이징을 포함한 QueryDSL 동적 조건 SQL은 오직 [`ProductApiE2ETest`](../../apps/commerce-api/src/test/java/com/loopers/interfaces/api/mall/controller/ProductApiE2ETest.java)를 통해서만 간접적으로 검증되고 있어, DAO 단위로 격리된 케이스가 비어 있다.

## 4. 경량화로 바뀌는 것

- 다음 3개 테스트를 삭제한다.
  - `OrderRepositoryIntegrationTest#enforcesOneOrderRecordPerOrder`
  - `BrandRepositoryIntegrationTest#findsBrandForDeletion_withAllUndeletedProducts`
  - `JdbcLikeQueryDaoIntegrationTest#excludesProducts_deletedViaBrandBulkDelete`
- `@Transactional` 롤백과 `@AfterEach` TRUNCATE를 동시에 쓰는 7개 클래스(`BrandRepositoryIntegrationTest`, `ProductRepositoryIntegrationTest`, `OrderRepositoryIntegrationTest`, `PointBillRepositoryIntegrationTest`, `WalletRepositoryIntegrationTest`, `UserRepositoryIntegrationTest`, `JdbcWalletQueryDaoIntegrationTest`)는 `@Transactional` 롤백만 남긴다. TRUNCATE는 트랜잭션과 무관하게 자동 커밋되므로 롤백과 병행하는 것은 불필요한 이중 정리다.
- `DatabaseCleanUp`은 빈 테이블을 건너뛰도록 바꾼다(TRUNCATE 대상 테이블 수를 실행 시점에 줄인다).
- MySQL 컨테이너에 `withReuse(true)`를 적용한다. 로컬에서 재사용을 활성화하려면 `~/.testcontainers.properties`에 `testcontainers.reuse.enable=true`가 필요하다.
- `commerce-api`에서 `modules:redis`의 `testFixtures` 의존을 제거해, 쓰지 않는 Redis 컨테이너가 매 컨텍스트 로딩마다 기동되지 않게 한다.
- `@IntegrationTest` 메타 어노테이션을 도입해 하나의 Spring 컨텍스트를 여러 통합 테스트가 공유하도록 한다.
- `BrandFindForDeletionLockIntegrationTest`와 `StockLostUpdateControlGroupTest`에 `slow` 태그를 붙여, 별도의 `slowTest` 태스크로 분리하고 `check`에도 포함시킨다.

## 참고: 불일치 및 확인 사항

- 프롬프트에 제시된 `OrderRepositoryIntegrationTest` 5.8초, `JdbcOrderQueryDaoIntegrationTest` 8.9초, `JpaConfirmOrderWriterTest` 0.16초는 이번 조사에서 실제로 측정하지 않고 프롬프트에 주어진 값을 그대로 사용했다(Gradle 실행이 금지되어 있어 직접 계측 불가).
- `LikeStorageIntegrationTest`는 엔티티가 아닌 `JdbcClient` 원시 SQL로 `product_likes`/`product_like_counts` 유일 제약을 검증하는 테스트로, `infrastructure.persistence.shopping.entity` 패키지에 있지만 실제로는 엔티티 클래스를 직접 다루지 않는다.
