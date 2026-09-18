# PR 04 구현 계획: 포인트 충전·조회 + 조회 인덱스 적용

## 목표

PR 03(`ebf47df`, PR #8)까지 `mall`(브랜드·상품)·`shopping`(사용자·좋아요) 컨텍스트가 완료됐다.
PR 04는 `pay` 컨텍스트에 **포인트 충전·잔액 조회**(R06)를 추가한다.
브랜치는 `volume-2/pr-04-pay`, 기준은 최신 `volume-2/main`이다.
관련 요구사항은 [01-requirements.md](01-requirements.md)의 R06, 업무 규칙은 [02-business-policies.md](02-business-policies.md)의 P06/P07/P18/P19, 계약은 [05-use-cases.md](05-use-cases.md)를 따른다.

이번 PR은 사용자 요청에 따라 포인트 기능과 함께 **조회 성능 최적화 인덱스**도 같은 브랜치에서 별도 커밋으로 다룬다. 코드베이스 전체에 인덱스 정의가 없었기 때문에(마이그레이션 도구 없이 Hibernate `ddl-auto: create`로 스키마 생성), 포인트 신규 테이블뿐 아니라 기존 `products`/`brands`/`product_likes` 조회 쿼리도 함께 검토해 이점이 있는 곳에 `@Table(indexes = ...)`를 추가한다.

## 범위

### 포인트 도메인/저장 (P06/P07/P18/P19)

- `domain.pay.point.Point`: `userId`, `balance`(`Money`). `zero(userId)`(초기 잔액 0), `restore(userId, balance)`, `charge(Money amount)` — `Money.add`의 오버플로 검사를 그대로 재사용(새 `DomainErrorCode` 불필요).
- `domain.pay.point.PointBill`: 충전 1건의 기록. `type`은 이번 PR에서 `CHARGE`만 정의(사용/차감 `USE`는 PR 06 주문 확정에서 실제 필요해질 때 추가).
- `PointRepository`/`PointBillRepository`(도메인 인터페이스) + `PointJpaEntity`/`PointBillJpaEntity`(JPA, `Point`는 `product_like_counts`처럼 `userId`를 PK로 사용) + Repository/Mapper 구현.
- `application.pay.point.PointQueryDao`(계약) + `infrastructure.pay.point.JdbcPointQueryDao` — 잔액 조회는 UseCase/Service 없이 JdbcClient로 직접 처리(기존 `JdbcUserQueryDao`/`JdbcProductLikeCountQueryDao`와 동일한 "단순 단건 조회" 패턴).

### 초기 잔액 fixture 연결

- `infrastructure.shopping.user.LocalUserFixtureInitializer`의 `run()`에서 사용자 저장 직후, 없을 때만 `Point.zero(userId)`를 저장하도록 연결한다(기존 잔액은 초기화하지 않음).

### 충전·조회 API

- `POST /api/v1/points/charge`: `@XUserId long userId` + 요청 본문 `amount`. `Money.positive(amount)`로 검증 후 `Point.charge` → 잔액·`PointBill(CHARGE)`을 한 트랜잭션에서 원자적으로 저장. 사용자 존재 확인은 `XUserIdArgumentResolver`가 이미 처리하므로 Service에서 재확인하지 않는다(기존 규칙과 동일).
- `GET /api/v1/points`: `@XUserId long userId`로 `PointQueryDao.findByUserId` 직접 호출.
- 오버플로/비양수 금액은 `DomainException(CALCULATION_OVERFLOW / NON_POSITIVE_MONEY)` → 기존 `ApiErrorMapper`가 이미 `BAD_REQUEST`로 매핑.

### 조회 인덱스 적용 (별도 커밋)

`@Table(indexes = {@Index(...)})`를 JPA 엔티티에 추가한다. Product 목록처럼 SELECT 컬럼이 많아 진짜 커버링 인덱스가 불가능한 곳은 WHERE/ORDER BY를 커버하는 복합 인덱스(필터+정렬 최적화, PK 조회는 남음)까지 포함한다.

| 대상 | 인덱스 | 목적 |
|---|---|---|
| `ProductJpaEntity` | `(deleted, brand_id, created_at DESC, id DESC)` | 상품 목록 LATEST 정렬 + 브랜드 필터 |
| `ProductJpaEntity` | `(deleted, brand_id, price ASC, id DESC)` | 상품 목록 PRICE_ASC 정렬 + 브랜드 필터 |
| `BrandJpaEntity` | `(deleted, created_at DESC, id DESC)` | 브랜드 목록 조회 |
| `LikeJpaEntity`(product_likes) | `(user_id, created_at DESC, product_id)` | 내 좋아요 목록 조회 — `product_likes` 쪽은 인덱스 온리 스캔 가능(사실상 진짜 커버링) |

- LIKES_DESC 정렬은 `like_count`가 `product_like_counts`(별도 테이블)에 있어 `products` 인덱스로 커버 불가 — 적용 대상에서 제외.
- `product_like_counts`(PK=`product_id`), `users`(PK=`id`)는 InnoDB 클러스터드 PK라 이미 사실상 커버링 — 추가 인덱스 없음.
- `product_likes`의 기존 유니크 제약 `uk_product_likes_user_product`(=`(user_id, product_id)` 인덱스)는 존재 확인·카운트 쿼리에 이미 충분해 유지, 이번엔 정렬용 인덱스만 추가.
- 검증은 자동화 테스트가 아니라 로컬 MySQL에서 `EXPLAIN`으로 수동 확인 후 이 문서의 "진행 기록"에 결과를 남긴다(사용자 결정).

## 테스트와 완료 조건

- `PointTest`/`PointBillTest`: 잔액 0 이상, 충전 시 오버플로·비양수 금액 거절.
- `PointStorageIntegrationTest`, `JdbcPointQueryDaoIntegrationTest`: 실제 DB 매핑·조회 검증.
- `PointServiceTest`(mock repository), `PointApiE2ETest`: 충전 성공/비양수/오버플로/사용자 없음, 조회 성공/사용자 없음.
- 완료 조건: `./gradlew :apps:commerce-api:check` 통과, [development-plan.md](development-plan.md)의 PR 04 완료 조건(금액 범위·계산 초과, 잔액과 CHARGE 기록의 원자적 저장) 테스트로 커버.
- 이번 세션은 각 작업 단계마다 관련 테스트 클래스만 `--tests`로 먼저 돌리고, 논리적 커밋 직전마다 전체 `check`를 실행하는 방식으로 진행한다(빌드 인프라의 태그 분리는 변경하지 않음, 세션 한정 작업 방식).

## 커밋 순서 (예정)

1. `feat: 포인트 도메인 모델 추가` — `Point`/`PointBill`/Repository 인터페이스, 단위 테스트
2. `feat: 포인트 저장·조회 인프라 구현` — JPA 엔티티/Repository/Mapper, `JdbcPointQueryDao`, 통합 테스트
3. `feat: 사용자 초기 포인트 fixture 연결` — `LocalUserFixtureInitializer` 확장
4. `feat: 포인트 충전 API 연결` — UseCase/Service/Controller, E2E 테스트
5. `feat: 포인트 잔액 조회 API 연결` — QueryController, E2E 테스트
6. `perf: 상품·브랜드·좋아요 조회에 인덱스 적용` — 기존 엔티티 인덱스 추가, EXPLAIN 검증 기록

## 제외 범위

- Auth/인가, 동시성 제어(잠금·재시도·버전 검증) — 프로젝트 공통 범위 밖([AGENTS.md](../../AGENTS.md)).
- `PointBill`의 `USE`(차감) 타입과 실제 차감 로직, 포인트 이력 조회 API — PR 06 담당.
- 인덱스 존재 자동화 테스트 — 수동 `EXPLAIN` 확인으로 대체(사용자 결정).
- 원격 브랜치 푸시·PR 생성·병합(별도 요청에 따름).

## 진행 기록

- 2026-09-18: 계획 수립. 커버링 인덱스 적용 범위와 세션 내 선택적 테스트 실행 방식을 사용자와 질의응답으로 확정한 뒤 구현 착수.
- 2026-09-18: 포인트 도메인·인프라·fixture·충전/조회 API 구현 완료(커밋 1~5), 매 커밋 직전 `./gradlew :apps:commerce-api:check` 통과 확인.
- 2026-09-18: `products`/`brands`/`product_likes`에 `@Table(indexes=...)` 적용 후 로컬 MySQL(`docker-compose -f ./docker/infra-compose.yml`)에 상품 약 6,264건·브랜드 5건·좋아요 100건을 채워 `EXPLAIN`으로 실제 사용 여부를 확인했다.
  - `idx_products_deleted_brand_created`/`idx_products_deleted_brand_price`: `brandId` 필터가 있는 목록 조회에서 `key`로 선택되고 `Using filesort` 없음(LATEST/PRICE_ASC 모두 확인). `brandId` 없이 전체 목록을 조회하면 `deleted` prefix만 쓰이고 정렬은 filesort로 처리됨 — 인덱스가 `brand_id`를 중간 컬럼으로 두기 때문에 예상된 동작이며, 이 PR에서 추가 조치는 하지 않는다.
  - `idx_brands_deleted_created`: 브랜드 목록 조회에서 `key`로 선택되고 `Using filesort` 없음.
  - `idx_product_likes_user_created`: 처음에는 `columnList`를 `user_id, created_at DESC, product_id`(오름차순)로 만들었는데, 기존 `JdbcLikeQueryDao`의 정렬이 `l.created_at DESC, p.id DESC`라 tie-break 방향이 맞지 않아 `EXPLAIN`에서 `Using temporary; Using filesort`가 그대로 남는 것을 발견했다. 인덱스를 `product_id DESC`로 고쳐도, `p.id`(조인된 `products` 테이블 컬럼)를 정렬 기준으로 쓰는 한 MySQL 옵티마이저가 조인을 넘어서는 정렬 보장을 인덱스만으로 증명하지 못해 여전히 filesort가 남았다. `ORDER BY` 기준 컬럼을 값이 동일한 구동 테이블 컬럼 `l.product_id DESC`로 바꾸자(`p.id = l.product_id`라 값은 같음) `Using index`만 남고 filesort가 사라졌다 — 인덱스 정의뿐 아니라 `JdbcLikeQueryDao`의 `ORDER BY` 절도 `l.product_id DESC`로 함께 수정했다(동작은 동일, 실행 계획만 개선).
  - `product_like_counts`(PK=`product_id`), `users`(PK=`id`)는 InnoDB 클러스터드 PK라 추가 인덱스 없이도 단건 조회가 이미 최적이라 그대로 두었다.
  - 검증에 사용한 임시 데이터는 확인 후 각 테이블 `TRUNCATE`로 정리했다(스키마 자체는 다음 로컬 기동 시 `ddl-auto: create`로 다시 생성됨).
