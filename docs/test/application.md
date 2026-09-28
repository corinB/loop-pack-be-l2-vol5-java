# 애플리케이션 레이어 테스트

## 1. 개요

`application` 레이어 테스트는 두 종류로 나뉜다.

| 종류 | 특징 | 예시 |
|---|---|---|
| 서비스 단위 테스트 | Mockito만 사용, Spring 컨텍스트·Docker 불필요, 순수 자바 객체(POJO) 조립 | `ConfirmOrderServiceTest`, `OrderServiceTest`, `WalletServiceTest`, `ApplicationExceptionTest` |
| 통합 테스트 | `@SpringBootTest`(MOCK 환경) + Testcontainers MySQL, 실제 트랜잭션·롤백·비관적 락·동시성까지 검증 | `ConfirmOrderIntegrationTest`, `ConfirmOrderConcurrencyIntegrationTest`, `ConfirmOrderSqlRollbackIntegrationTest`, `DeleteBrandRollbackIntegrationTest`, `LikeCountAggregationIntegrationTest` |

통합 테스트는 실행 전 Docker가 떠 있어야 하며(`docker-compose -f ./docker/infra-compose.yml up`), `modules/jpa`의 `MySqlTestContainersConfig`를 통해 Testcontainers MySQL을 띄운다. 각 통합 테스트 클래스는 `@AfterEach`에서 `DatabaseCleanUp.truncateAllTables()`로 모든 테이블을 비운다.

실행 방법.

```bash
# application 패키지 전체
./gradlew :apps:commerce-api:test --tests "com.loopers.application.*"

# 클래스 단위
./gradlew :apps:commerce-api:test --tests "com.loopers.application.ordering.service.ConfirmOrderConcurrencyIntegrationTest"
```

집계: 클래스 9개(테스트 헬퍼 `ConcurrentRequests` 제외), 테스트 메서드 총 25개 — 단위 테스트 클래스 4개(15개 메서드), 통합 테스트 클래스 5개(10개 메서드).

## 2. 컨텍스트별 테스트

### mall

| 테스트 클래스 | 종류 | Spring 컨텍스트 | Docker | 검증 시나리오 | 테스트 수 | 최근 측정 시간 |
|---|---|---|---|---|---|---|
| [DeleteBrandRollbackIntegrationTest](../../apps/commerce-api/src/test/java/com/loopers/application/mall/service/DeleteBrandRollbackIntegrationTest.java) | 통합 | MOCK + spy(ProductJpaRepository) | 필요 | 두 번째 상품 저장이 실패하면 브랜드·상품 변경 전체를 롤백하고 다른 대상(과거 확정 주문 등)은 영향받지 않는다 | 1 | 별도 컨텍스트 분리, 측정 시간 미기록 |

### ordering

| 테스트 클래스 | 종류 | Spring 컨텍스트 | Docker | 검증 시나리오 | 테스트 수 | 최근 측정 시간 |
|---|---|---|---|---|---|---|
| [ConfirmOrderServiceTest](../../apps/commerce-api/src/test/java/com/loopers/application/ordering/service/ConfirmOrderServiceTest.java) | 단위 | none | 불필요 | 재고 차감·포인트 사용 후 기록/상태 저장, 없는 주문 거절, 이미 확정된 주문 거절, 삭제된 상품 거절, 재고 부족 거절, 포인트 부족 거절, 상품 가격 변동과 무관하게 저장된 합계로 결제 | 7 | 약 2.5초(대부분 Mockito 초기화) |
| [OrderServiceTest](../../apps/commerce-api/src/test/java/com/loopers/application/ordering/service/OrderServiceTest.java) | 단위 | none | 불필요 | 품목별 스냅샷·합계 저장, 동일 상품 수량 합산, 없는 상품 거절, 삭제된 상품 거절, 중복 수량 합산 오버플로 거절 | 5 | 약 0.19초 |
| [ConfirmOrderIntegrationTest](../../apps/commerce-api/src/test/java/com/loopers/application/ordering/service/ConfirmOrderIntegrationTest.java) | 통합 | MOCK(공유 컨텍스트) | 필요 | 재고·잔액 차감 후 USE/PAID 기록을 남기고 CONFIRMED 저장, 두 번째 품목 재고 부족 시 전체 롤백, 포인트 부족 시 전체 롤백, 브랜드 일괄 삭제로 상품이 삭제되면 확정 거절, 이미 확정된 주문 재확정 거절 | 5 | 13.8초 |
| [ConfirmOrderConcurrencyIntegrationTest](../../apps/commerce-api/src/test/java/com/loopers/application/ordering/service/ConfirmOrderConcurrencyIntegrationTest.java) | 통합 | MOCK(공유 컨텍스트) | 필요 | 같은 주문 동시 재확정 시 1회만 성공, 재고 5에 8명이 동시 주문하면 재고만큼만 성공, 잔액이 허용하는 만큼만 동시 결제 성공, 충전과 결제 동시 실행 시 둘 다 성공, 관리자 재고 설정과 주문 확정 동시 실행 시 순차 실행과 동일한 결과, 두 상품을 반대 순서로 담은 두 주문이 동시에 확정돼도 데드락 없이 둘 다 성공 (R02 필수 6개 시나리오) | 6 | 23.1초 |
| [ConfirmOrderSqlRollbackIntegrationTest](../../apps/commerce-api/src/test/java/com/loopers/application/ordering/service/ConfirmOrderSqlRollbackIntegrationTest.java) | 통합 | MOCK + spy(OrderRepository) | 필요 | 재고·잔액·기록까지 실제 SQL로 반영된 뒤 마지막 저장 단계에서 실패해도 확정 전체가 롤백된다 | 1 | 별도 컨텍스트 분리, 측정 시간 미기록 |

### pay

| 테스트 클래스 | 종류 | Spring 컨텍스트 | Docker | 검증 시나리오 | 테스트 수 | 최근 측정 시간 |
|---|---|---|---|---|---|---|
| [WalletServiceTest](../../apps/commerce-api/src/test/java/com/loopers/application/pay/service/WalletServiceTest.java) | 단위 | none | 불필요 | 충전 시 잔액 저장 후 CHARGE 기록을 순서대로 저장, 충전 후 잔액이 범위를 초과하면 저장 없이 거절, 비양수 충전액은 저장 없이 거절 | 3 | 미기록(단위 테스트 수준으로 빠름) |

### shopping

| 테스트 클래스 | 종류 | Spring 컨텍스트 | Docker | 검증 시나리오 | 테스트 수 | 최근 측정 시간 |
|---|---|---|---|---|---|---|
| [LikeCountAggregationIntegrationTest](../../apps/commerce-api/src/test/java/com/loopers/application/shopping/service/LikeCountAggregationIntegrationTest.java) | 통합 | MOCK + spy(JdbcLikeCountAggregationDao) | 필요 | 전체 관계 COUNT를 저장하고 관계가 사라진 기존 집계는 0으로 갱신, 집계 저장 중 실패하면 앞선 0 초기화도 함께 롤백 | 2 | 별도 컨텍스트 분리, 측정 시간 미기록 |

### support

| 테스트 클래스 | 종류 | Spring 컨텍스트 | Docker | 검증 시나리오 | 테스트 수 | 최근 측정 시간 |
|---|---|---|---|---|---|---|
| [ApplicationExceptionTest](../../apps/commerce-api/src/test/java/com/loopers/application/support/error/ApplicationExceptionTest.java) | 단위 | none | 불필요 | 별도 메시지가 없으면 오류 코드 메시지를 사용, 별도 메시지가 있으면 해당 메시지를 사용 | 2 | 미기록 |

테스트 헬퍼(테스트 클래스가 아니므로 위 표에 포함하지 않음).

| 파일 | 역할 |
|---|---|
| [support/concurrency/ConcurrentRequests.java](../../apps/commerce-api/src/test/java/com/loopers/support/concurrency/ConcurrentRequests.java) | 여러 `Callable` 작업을 `CountDownLatch`로 시작 시점을 맞춰 동시에 실행하고, 각 결과를 `Outcome`(성공값 또는 예외)으로 모아 반환하는 동시성 테스트 전용 유틸리티. `ConfirmOrderConcurrencyIntegrationTest`에서 사용 |

## 3. 다른 레이어와 겹치는 검증

`ConfirmOrderServiceTest`의 다음 6개 케이스는 다른 테스트와 중복되거나 실질적 검증력이 없다.

| 케이스 | 문제 | 겹치는 테스트 |
|---|---|---|
| `rejectsExecute_whenStockIsInsufficient` | 도메인 정책 + 통합 테스트와 중복 | [OrderConfirmationPolicyTest](../../apps/commerce-api/src/test/java/com/loopers/domain/ordering/policy/OrderConfirmationPolicyTest.java), `ConfirmOrderIntegrationTest#rollsBackEverything_whenSecondItemStockIsInsufficient` |
| `rejectsExecute_whenPointIsInsufficient` | 도메인 정책 + 통합 테스트와 중복 | `OrderConfirmationPolicyTest`, `ConfirmOrderIntegrationTest#rollsBackEverything_whenPointIsInsufficient` |
| `rejectsExecute_whenOrderIsAlreadyConfirmed` | 도메인 정책 + 통합 테스트와 중복 | `OrderConfirmationPolicyTest`, `ConfirmOrderIntegrationTest#rejectsReconfirm_withoutAdditionalChangesOrRecords` |
| `rejectsExecute_whenProductIsDeleted` | 도메인 정책 + 통합 테스트와 중복 | `OrderConfirmationPolicyTest`, `ConfirmOrderIntegrationTest#rejectsConfirm_whenProductDeletedViaBrandBulkDelete` |
| `rejectsExecute_whenOrderDoesNotExist` | 목(mock)을 예외를 던지도록 스텁한 뒤 그대로 예외가 나오는지만 확인 — 실질적으로 아무것도 증명하지 못함 | 실제 커버리지는 [OrderApiE2ETest](../../apps/commerce-api/src/test/java/com/loopers/interfaces/api/ordering/controller/OrderApiE2ETest.java)의 `Confirm#returnsNotFound_whenOrderDoesNotExist` |
| `confirmsOrder_andSavesStockPointAndRecords` | 정책 해피패스 + 통합 테스트와 중복 | `OrderConfirmationPolicyTest`, `ConfirmOrderIntegrationTest#confirmsOrder_withStockPointAndRecords` |

`usesStoredTotalAmount_ignoringCurrentProductPrice`는 다른 곳에서 검증되지 않는 고유 케이스이므로 유지한다.

그 밖의 레이어 간 중복.

- `ConfirmOrderIntegrationTest`의 상태 검증(재고·잔액·기록·주문 상태를 DB까지 확인)은 `OrderApiE2ETest`의 확정 관련 테스트가 HTTP 상태 코드·오류 코드 확인만으로 축소될 수 있게 하는 기준(reference) 역할을 한다.
- `ConfirmOrderSqlRollbackIntegrationTest`는 `OrderApiE2ETest#returnsInternalServerError_whenSaveFailsAfterRealSql`의 상위 집합이며, 해당 E2E 테스트는 삭제 대상이다.
- `WalletServiceTest#rejectsOverflow_withoutSavingAnything`, `#rejectsNonPositiveAmount_withoutSavingAnything`은 [WalletApiE2ETest](../../apps/commerce-api/src/test/java/com/loopers/interfaces/api/pay/controller/WalletApiE2ETest.java)의 상태 검증 케이스를 이미 커버한다.

공백(gap): `Brand`/`Product`/`Like` 애플리케이션 서비스에는 서비스 단위 테스트가 없다. 해당 분기는 현재 E2E 테스트에서만 커버된다.

## 4. 경량화로 바뀌는 것

- `ConfirmOrderServiceTest`의 위 6개 중복 케이스를 삭제한다(`usesStoredTotalAmount_ignoringCurrentProductPrice`만 유지).
- 공용 메타 애노테이션 `@IntegrationTest`를 도입한다. `@SpringBootTest`(MOCK) + 타입 레벨 `@MockitoSpyBean`(`OrderRepository`, `ProductJpaRepository`, `JdbcLikeCountAggregationDao`)을 묶어, 현재 4개로 분리된 Spring 컨텍스트를 하나로 공유하게 한다.
- `ConfirmOrderConcurrencyIntegrationTest`에 `slow` 태그를 붙여 `test` 태스크에서 제외하고 `slowTest` 태스크에서만 실행한다. 두 태스크 모두 `check`에는 포함된다.
- `DatabaseCleanUp`이 빈 테이블은 건너뛰도록 한다.
- MySQL Testcontainers를 재사용(reuse)하도록 한다.
