# PR 05 구현 계획: 주문 생성·조회

## 목표

PR04(포인트, PR #9)까지 `mall`(브랜드·상품)·`shopping`(사용자·좋아요)·`pay`(포인트) 컨텍스트가 완료됐다.
PR05는 `ordering` 컨텍스트에 **주문 생성(DRAFT)과 고객·관리자 조회**를 추가하고, `pay`에 **결제 조회용 OrderBill 저장 구조**를 준비한다(실제 결제 기록 생성은 PR06 담당).
브랜치는 `volume-2/pr-05-order-create-query`, 기준은 `volume-2/main`(PR04 병합 시점)이다.
관련 요구사항은 [01-requirements.md](01-requirements.md)의 R07·R09·R13(목록/상세 부분), 업무 규칙은 [02-business-policies.md](02-business-policies.md)의 P20~P22·P28~P31, 계약은 [05-use-cases.md](05-use-cases.md)·[06-read-models.md](06-read-models.md)를 따른다.

**이 문서는 범위·요구사항·완료 조건을 정리하는 단계이며, 아직 실제 코드는 없다.** 클래스·패키지 단위의 세부 설계(어떤 파일을 만들지, JDBC/QueryDSL 중 무엇을 쓸지 등)는 구현 착수 시 코드베이스를 다시 확인하고 필요하면 사용자에게 확인한 뒤 정한다.

## 작업 방식 (이번 세션·이 PR 전반에 적용)

- **선택적 테스트 실행**: 구현 중에는 변경과 직접 관련된 테스트 클래스만 `--tests`로 먼저 실행해 빠르게 반복하고, 논리적 커밋 직전마다 `./gradlew :apps:commerce-api:check` 전체(Checkstyle·ArchUnit·전체 테스트)를 실행해 회귀가 없는지 확인한다. 빌드 인프라의 태그 분리(@Tag 등)는 별도 결정 없이 도입하지 않는다.
- **애매하거나 중요한 결정은 추론하지 않고 질문**: 요구사항·업무 정책·기존 코드 어디에도 명시되지 않은 부분이거나 여러 해석이 가능한 설계 선택은 임의로 정하지 말고 사용자에게 먼저 확인한다. `docs/week2/conventions.md` 8절에도 동일한 원칙이 명시되어 있다.

## 범위

### 주문 생성 (DRAFT)

- 요구사항 R07: 여러 품목의 수량·단가·합계를 DRAFT로 저장하고, 재고·포인트 차감은 하지 않는다.
- 업무 규칙(P20~P22): 한 품목 이상, 같은 상품은 수량을 합산한 뒤 저장. 활성 상품·양수 수량을 확인하고 상품명·단가는 생성 시점 스냅샷으로 저장. 생성은 DRAFT 저장만 수행하며 재고 검사·예약·차감은 하지 않는다.
- API: `POST /api/v1/orders` — `X-USER-ID` + 본문 `items: [{productId, quantity}]` → 201 OrderDetail. 오류 E01(입력 검증), E02/E04(헤더·사용자), E09(중복 합산 수량·품목 금액·총액 계산 초과).
- 도메인 모델(04-domain-model.md 기준): `Order{id, userId, status, totalAmount, createdAt, items}`, `OrderItem{productId, productName, unitPrice, quantity, amount}` — OrderItem은 Order 내부 Entity이며 단독 변경 API는 없다.

### 고객·관리자 조회

- 요구사항 R09(고객 목록/상세), R13(관리자 목록/상세 중 조회 부분 — 확정·결제 결과는 PR06에서 채워짐).
- API:
  - `GET /api/v1/orders` — `X-USER-ID`, `page`/`size` → 200 `Page<OrderView>`. 오류 E02/E04.
  - `GET /api/v1/orders/{orderId}` → 200 OrderDetail. 오류 E04.
  - `GET /api-admin/v1/orders` — `page`/`size` → 200 `Page<AdminOrderView>`. 오류 E01.
  - `GET /api-admin/v1/orders/{orderId}` → 200 AdminOrderDetail. 오류 E04.
- 페이지·정렬 정책(P28~P31)을 따른다.

### 결제 조회용 저장 구조 (Pay 컨텍스트, OrderBill)

- `development-plan.md`: "PR 05에 Pay 소유의 OrderBill 저장·조회 구조와 `orderId` 유일성 제약을 준비한다. DRAFT의 결제 필드는 null이며 결제 결과 조합은 저장 fixture로 검증한다. 실제 결제 기록 생성은 PR 06에서 연결한다."
- 즉 이번 PR은 `OrderBill`의 저장 구조(테이블·엔티티·`orderId` 유일성 제약)만 준비하고, 실제 행 생성(결제 확정)은 하지 않는다. DRAFT 주문 조회 시 결제 관련 필드는 null로 응답한다.

## 테스트와 완료 조건 (development-plan.md 기준)

- 완료 조건: 수량 합산·스냅샷·합계 정확성, 생성 시 재고·포인트 차감이 없음, 사용자별 필터·페이지네이션, 결제 필드 계약(DRAFT는 null) 확인.
- 구현 착수 시 `docs/week2/conventions.md` 7절(테스트 계층별 방법)을 따라 domain 단위 → 저장/조회 통합 → HTTP E2E 순서로 테스트를 준비한다. 구체적인 테스트 클래스 구성은 실제 설계 확정 후 이 문서에 추가한다.
- 완료 조건: `./gradlew :apps:commerce-api:check` 통과.

## 커밋 순서 (예정, 구현 착수 시 확정)

`development-plan.md`의 권장 커밋 흐름: "주문·품목 규칙 → 생성·저장 → 결제 조회 구조·고객·관리자 조회". 실제 커밋 단위는 구현 착수 시 세분화한다.

## 제외 범위

- Auth/인가, 동시성 제어(잠금·재시도·버전 검증) — 프로젝트 공통 범위 밖([AGENTS.md](../../AGENTS.md)).
- 주문 확정(재고·포인트 차감, CONFIRMED 전환, 실제 결제 기록 생성) — PR06 담당.
- 원격 브랜치 푸시 이후의 PR 생성·병합 — 별도 요청에 따름(이번 세션은 브랜치+계획 문서 준비까지).

## 진행 기록

- 2026-09-18: PR04 병합 직후 `volume-2/main`에서 브랜치 생성, 계획 문서 작성. 실제 구현은 아직 착수하지 않음. PR06이 이 PR의 Order/OrderItem·OrderBill 저장 구조에 의존하므로, 두 PR을 동시에 진행하더라도 실제 주문 확정 코드 작성은 이 PR의 진행 상황을 참고해야 한다([pr-06-order-confirm-plan.md](pr-06-order-confirm-plan.md) 참고).
- 2026-09-18: PR06이 위에 스택할 수 있도록 기반 커밋(`3b56af6`, "feat: 주문 도메인과 OrderBill 저장 구조 추가")을 먼저 만들어 푸시함. `domain.ordering.order`의 `Order`/`OrderItem`/`OrderStatus`(DRAFT/CONFIRMED)와 `OrderRepository`, `domain.pay.orderbill`의 `OrderBill`/`OrderBillStatus`(PAID)와 `OrderBillRepository`, 각 JPA 인프라(`order_bills`는 `order_id` 유일성 제약 포함)와 domain/infrastructure 테스트까지 포함. `Order.confirm()`처럼 PR06이 필요로 하는 상태 전이 메서드는 아직 없으며, 추가·계약 변경 시 PR06과 사전 공유하기로 함. `./gradlew :apps:commerce-api:check` 통과 확인. 이후 이 PR의 나머지 범위(주문 생성 Service, 고객/관리자 조회 DAO·Controller)를 계속 진행.
- 2026-09-18: 이 PR의 남은 범위를 마무리함. `880cbfd`(주문 생성 UseCase·API — 중복 품목 수량 합산, 활성 상품·스냅샷 검증, DRAFT 저장), `05fcf79`(고객·관리자 조회 API — `OrderQueryDao`/`JdbcOrderQueryDao`가 Order+OrderItem+OrderBill을 한 메서드에서 묶어 조회, DRAFT 결제 필드는 null), `11a1385`(HTTP E2E 테스트) 순서로 커밋·푸시함. `Order`/`OrderBill` 계약은 기반 커밋 이후 변경하지 않음. `./gradlew :apps:commerce-api:check` 통과 확인. 남은 것은 원격 병합 여부에 대한 별도 요청뿐이며, 이 PR 자체의 구현·테스트는 완료.
