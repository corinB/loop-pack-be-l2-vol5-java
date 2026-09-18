# PR 06 구현 계획: 주문 확정·롤백

## 선행 조건 (해소됨)

- PR05(주문 생성·조회, `Order`/`OrderItem`/`OrderStatus`, `OrderBill`/`OrderBillStatus`)가 `volume-2/main`에 병합됐다(PR #10). 이 브랜치는 병합된 `volume-2/main` 위로 rebase했다.
- 같은 세션에서 PR05·PR06을 모두 진행하기로 해 별도 브랜치 조율(공유 파일 동결 등)은 필요 없다.

## 목표

PR06은 고객이 지정한 DRAFT 주문을 확정한다: 상품 재고·주문 사용자의 포인트를 차감하고, `USE` 타입 `PointBill`과 `OrderBill`을 기록한 뒤 주문을 `CONFIRMED`로 전환한다. 모든 처리는 하나의 트랜잭션으로 묶이며, 어느 단계든 실패하면 전체 롤백한다.
브랜치는 `volume-2/pr-06-order-confirm`, 기준은 PR05가 병합된 `volume-2/main`이다.
관련 요구사항은 [01-requirements.md](01-requirements.md)의 R08(전체), R09·R13(확정 이후 상세·결제 결과 조회 검증 부분), 업무 규칙은 [02-business-policies.md](02-business-policies.md)의 P23~P27, 계약은 [05-use-cases.md](05-use-cases.md)를 따른다.

## 작업 방식 (이번 세션·이 PR 전반에 적용)

- **선택적 테스트 실행**: 구현 중에는 변경과 직접 관련된 테스트 클래스만 `--tests`로 먼저 실행해 빠르게 반복하고, 논리적 커밋 직전마다 `./gradlew :apps:commerce-api:check` 전체(Checkstyle·ArchUnit·전체 테스트)를 실행해 회귀가 없는지 확인한다. 빌드 인프라의 태그 분리(@Tag 등)는 별도 결정 없이 도입하지 않는다.
- **애매하거나 중요한 결정은 추론하지 않고 질문**: 요구사항·업무 정책·기존 코드 어디에도 명시되지 않은 부분이거나 여러 해석이 가능한 설계 선택은 임의로 정하지 말고 사용자에게 먼저 확인한다. `docs/week2/conventions.md` 8절에도 동일한 원칙이 명시되어 있다.

## 범위

### 주문 확정

- 요구사항 R08: 지정한 DRAFT에서 재고·주문 사용자 포인트를 차감하고 결제 결과를 저장한다.
- 업무 규칙(P23~P27):
  - P23: 지정한 주문이 DRAFT인지 확인하고, 상품 활성 여부·수량·현재 재고·주문 사용자 잔액을 재검사한다.
  - P24: 확정 결제액은 저장된 주문 합계를 사용하며, 현재 상품 가격으로 재계산하지 않는다.
  - P25: 재고 차감 → 포인트 차감·기록 → 주문 확정을 하나의 트랜잭션으로 처리한다.
  - P26: 어느 단계든 실패하면 전체 롤백하며, 실패 기록은 별도로 남기지 않는다.
  - P27: 재확정은 409로 거절하고, 같은 주문의 성공 결제는 한 번만 저장한다.
- API: `POST /api/v1/orders/{orderId}/confirm` — 경로 `orderId`, 본문 없음 → 200 OrderDetail. 오류 E04(주문 없음/DRAFT 상품 삭제), E06(409, 재고 부족), E07(409, 포인트 잔액 부족), E08(409, 이미 CONFIRMED인 주문 재확정).
- 저장 경계(04-domain-model.md): 확정 한 번의 트랜잭션에 모든 대상 Product 재고, Point, `USE` PointBill, OrderBill, Order 상태 변경이 함께 들어간다.

### 설계 (PR05 코드 기준으로 확정)

- `domain.ordering.order.Order`: `status`를 가변 필드로 바꾸고 `confirm()` 추가(이미 CONFIRMED면 신규 `DomainErrorCode.ORDER_ALREADY_CONFIRMED` → 409/E08). `OrderRepositoryImpl`/`OrderEntityMapper`는 PR05에서 이미 상태 변경용 `apply()` 경로를 준비해뒀으므로 수정 불필요.
- `domain.pay.point.Point`: `use(Money amount)` 추가 — 잔액보다 크면 신규 `DomainErrorCode.INSUFFICIENT_POINT` → 409/E07(잔액 유지), 아니면 차감. `Stock.decrease`와 같은 방식으로 소유 타입 안에서 처리(`Money`에 범용 subtract 추가하지 않음).
- `domain.pay.point.PointBillType`에 `USE` 추가. `PointBill`에 `orderId`(nullable) 필드 추가 — CHARGE는 null, USE는 양수 필수. `use(userId, orderId, amount)` 팩터리 추가. `balanceAfter`는 포인트 이력 조회 API가 없는 이번 범위에서 쓰이지 않아 추가하지 않기로 확인함(2026-09-18 질의 응답).
- `application.ordering.order`: `ConfirmOrderUseCase`/`ConfirmOrderCommand(orderId)`/`ConfirmOrderResult(OrderResult, paymentAmount, paymentStatus)`/`ConfirmOrderService`(`@Transactional`) 추가. 순서: 주문 조회(404)→`confirm()`(409)→품목별 상품 활성·재고 확인·차감(404/409)→포인트 차감(409)→USE PointBill 저장→OrderBill 저장→Order 저장. `OrderView`에 `of(result, paymentAmount, paymentStatus)` 팩터리를 추가하고 기존 `from(result)`는 이를 호출하도록 정리.
- `interfaces.api.ordering.order.OrderController`에 `POST /{orderId}/confirm` 추가(본문·헤더 없음, 성공 200).
- `OrderQueryDao`/`JdbcOrderQueryDao`는 PR05에서 이미 `order_bills`를 LEFT JOIN해 조회하므로 수정 없이 확정 후 결제 결과가 자동으로 노출된다.

## 테스트와 완료 조건 (development-plan.md 기준)

- 완료 조건: 단일 트랜잭션 처리, 실제 결제 결과 조회 가능, 순차 재확정 시 409, 모든 실패 시나리오의 전체 롤백.
- `docs/week2/conventions.md` 7절을 따라 domain 단위(`Order.confirm`, `Point.use`, `PointBill.use`) → UseCase(`ConfirmOrderServiceTest`, mock repository, 협력 순서·실패 시 후속 처리 중단 검증) → 저장/트랜잭션 통합(flush/clear, 전체 롤백·재확정 검증) → HTTP E2E(`OrderApiE2ETest`에 `Confirm` nested 추가, PR05 조회 API 회귀 포함) 순서로 진행한다.
- 완료 조건: `./gradlew :apps:commerce-api:check` 통과.

## 커밋 순서

1. `docs: PR06 계획을 최신 Order 도메인 기준으로 갱신`
2. `feat: 포인트 사용과 결제 기록 규칙 구현` (Point.use, PointBillType.USE, PointBill.orderId)
3. `feat: 주문 확정 유스케이스와 API 연결` (Order.confirm, ConfirmOrderUseCase/Service/Controller)
4. `test: 주문 확정 전체 롤백과 재확정 검증` (저장/트랜잭션 통합 + HTTP E2E)
5. `docs: PR06 검증 결과 기록`

## 제외 범위

- Auth/인가, 동시성 제어(잠금·재시도·버전 검증) — 프로젝트 공통 범위 밖([AGENTS.md](../../AGENTS.md)).
- 주문 생성·고객/관리자 조회, OrderBill 저장 구조 준비 — PR05에서 이미 완료·병합됨.

## 진행 기록

- 2026-09-18: PR04 병합 직후 `volume-2/main`에서 브랜치 생성, 계획 문서 작성. 이 시점엔 PR05가 아직 진행 전이라 `Order` 도메인이 없어 실제 구현은 보류함.
- 2026-09-18: PR05가 `volume-2/main`에 병합됨(PR #10). 같은 세션에서 PR06도 직접 진행하기로 결정. 이 브랜치를 병합된 `volume-2/main` 위로 rebase(기존 계획 문서 커밋만 재적용, 충돌 없음)하고 위 "설계" 절을 PR05의 실제 코드에 맞춰 확정함.
