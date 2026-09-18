# PR 03 구현 계획: 좋아요 등록·취소·목록

## 목표

PR 02(`e55a95b`, PR #7)에서 Like 도메인·저장 구조·상품별 집계 테이블·주기 집계 스케줄러까지 준비됐다.
PR 03은 그 위에 **등록·취소**와 **사용자별 좋아요 목록 조회**, 그리고 이를 연결하는 HTTP 계층을 추가한다.
브랜치는 `volume-2/pr-03-shopping`, 기준은 최신 `volume-2/main`이다.
관련 요구사항은 [01-requirements.md](01-requirements.md)의 R04·R05, 계약은 [05-use-cases.md](05-use-cases.md)·[06-read-models.md](06-read-models.md)를 따른다.

## 설계 변경: JPA 쓰기 구조 폐기, JDBC 전용으로 대체

최초 계획은 PR 02의 `Like`(domain) + `LikeJpaEntity`/`LikeJpaRepository`/`LikeRepositoryImpl`(JPA)을 확장해
`RegisterLikeUseCase`/`CancelLikeUseCase` + `LikeService`(Brand/Product와 동일한 UseCase/Service 패턴)로 등록·취소를 구현하는 것이었다.
구현 중 사용자 요청으로 **Service 계층 없이 JDBC로 직접 처리**하는 방식으로 바꿨다. 그 결과:

- PR 02에서 만든 `Like`, `LikeRepository`(domain), `LikeJpaEntity`, `LikeJpaRepository`, `LikeRepositoryImpl`, `LikeEntityMapper`를 **삭제**했다.
  집계 스케줄러(`LikeCountAggregationScheduler`/`JdbcLikeCountAggregationDao`)는 원래도 `product_likes` 테이블을 JdbcClient로 직접 읽었으므로 영향이 없다.
- 등록·취소는 `application/shopping/like/LikeCommandDao`(계약) + `infrastructure/shopping/like/JdbcLikeCommandDao`(JdbcClient 구현)로 대체했다. UseCase/Command/Service는 없다.
- 상품 활성 확인·중복 등록 방지·관계 삭제를 모두 SQL로 직접 처리한다. `ApplicationException(PRODUCT_NOT_FOUND)`을 던지는 판단은 (Service가 없으므로) `LikeController`가 `existsActiveProduct` 조회 결과를 보고 직접 던진다 — 기존 `BrandQueryController`/`AdminBrandController`가 `Optional`을 보고 직접 `ApplicationException`을 던지는 것과 같은 자리다.

## 구현 범위 (실제 구현)

### 좋아요 등록·취소 (application / infrastructure)

- `application/shopping/like/LikeCommandDao`: `boolean existsActiveProduct(long productId)`, `void register(long userId, long productId)`, `void cancel(long userId, long productId)`.
- `infrastructure/shopping/like/JdbcLikeCommandDao`가 `JdbcClient`로 구현:
  - `existsActiveProduct`: `products` 테이블에서 `id`·`deleted = false` 확인.
  - `register`: 관계 존재 확인 → 없으면 INSERT. `DataIntegrityViolationException`이 나도 동시 등록으로 이미 존재하면 idempotent 성공 처리(동시 요청 테스트는 추가하지 않는다).
  - `cancel`: 조건 없이 DELETE(존재하지 않아도 그대로 성공).
- `LikeController`(`/api/v1/products/{productId}/likes`)가 등록 전 `existsActiveProduct`를 확인해 없거나 삭제된 상품이면 `ApplicationException(PRODUCT_NOT_FOUND)`을 던진다. 취소는 상품 확인 없이 바로 `cancel`을 호출한다.

### 내 좋아요 목록 (application / infrastructure)

- `application/shopping/like/LikeQueryDao#findByUserId(userId, PageCriteria): PageResult<LikeItem>`.
- `LikeItem` = `ProductSummary`의 필드(productId, name, price, brand: BrandSummary, likeCount) + `likedAt`.
- `infrastructure/shopping/like/JdbcLikeQueryDao`가 `product_likes ⋈ products ⋈ brands ⋈(LEFT) product_like_counts`를 `JdbcClient`로 조인한다. 조건: `user_id = :userId AND products.deleted = false`. 정렬은 `created_at DESC, productId DESC` 고정.

### API 연결 (interfaces)

- `interfaces/api/shopping/like/LikeController` — `/api/v1/products/{productId}/likes`에 `POST`(등록)·`DELETE`(취소). `@XUserId long userId` 파라미터 사용 — PR 01에서 만든 resolver의 첫 실사용이다. 응답은 `ApiResponse.success()`(200, null).
- `interfaces/api/shopping/like/LikeQueryController` — `/api/v1/users/{userId}/likes`에 `GET`. 경로 userId는 헤더와 비교하지 않고 `UserQueryDao.findById`(resolver와 동일 DAO)로 존재만 확인 → 없으면 404. `page`/`size` 기본값 0/20.

### 저장기술 사용 구분 (JDBC / QueryDSL)

| 위치 | 기술 | 이유 |
|---|---|---|
| `JdbcLikeCommandDao` (등록·취소 쓰기) | **JdbcClient** | 단건 존재 확인 + INSERT/DELETE뿐이라 JPA·domain 객체 없이도 충분하다는 사용자 결정. |
| `JdbcLikeQueryDao` (내 좋아요 목록 조회) | **JdbcClient** | `product_likes ⋈ products ⋈ brands ⋈ product_like_counts` 4테이블 조인이지만 필터는 `userId` 하나, 정렬은 `created_at DESC, productId DESC`로 **고정**이다. 기존 `JdbcBrandQueryDao`/`JdbcUserQueryDao`/`JdbcProductLikeCountQueryDao`가 확립한 "단순·고정 조회는 JdbcClient" 패턴을 그대로 따른다. |
| **QueryDSL은 쓰지 않음** | — | QueryDSL은 지금 `QueryDslProductQueryDao` 한 곳에만 쓰인다. 상품 목록 API가 `brandId`(선택) 필터와 `sort` 파라미터로 요청마다 조건·정렬이 **동적으로** 바뀌기 때문이다. 좋아요 관련 쿼리는 전부 필터·정렬이 고정이라 이 조건에 해당하지 않는다. |

## 테스트와 완료 조건

- `JdbcLikeCommandDaoIntegrationTest`(신규): 실제 DB로 `existsActiveProduct`(존재/삭제/없음), `register`(신규 저장·중복 idempotent), `cancel`(제거·관계 없음 idempotent) 검증.
- `LikeStorageIntegrationTest`(수정): `Like`/`LikeRepository` 삭제로 domain 경유 테스트를 걷어내고, `product_likes`/`product_like_counts`의 DB 유일성 제약만 JdbcClient로 직접 검증.
- `JdbcLikeQueryDaoIntegrationTest`(신규): 실제 DB로 최근 좋아요순 정렬, 동률 productId DESC, 삭제 상품 제외, 페이지네이션, 집계 행 없을 때 likeCount 0 검증.
- `LikeApiE2ETest`(신규): 실제 Controller+application+DB로 등록/취소 200, 헤더 누락·형식 오류 400, 없는 사용자·삭제 상품 404, 중복 등록/관계 없는 취소 idempotent, 목록 200/404 검증.
- 완료 조건: `./gradlew :apps:commerce-api:check` 통과, [development-plan.md](development-plan.md)의 PR 03 완료 조건(반복 요청 성공, 관계·목록 즉시 반영, 숫자·인기순 지연 반영, 삭제 상품 처리) 각각 테스트로 커버.

## 커밋 순서

```
refactor: 좋아요 등록·취소를 JDBC 전용 구조로 전환
- PR02의 Like domain/JPA 쓰기 구조(Like, LikeRepository, LikeJpaEntity, LikeJpaRepository, LikeRepositoryImpl, LikeEntityMapper) 삭제
- LikeCommandDao 계약과 JdbcLikeCommandDao 구현 추가
- LikeStorageIntegrationTest를 JdbcClient 전용 검증으로 축소, JdbcLikeCommandDaoIntegrationTest 추가
```

```
feat: 내 좋아요 목록 조회 DAO 구현
- LikeQueryDao와 LikeItem 조회 record 추가
- JdbcLikeQueryDao로 Like·Product·Brand·집계 조인 구현
- 페이지·정렬·삭제 상품 제외 통합 테스트 추가
```

```
feat: 좋아요 등록·취소·목록 API 연결
- LikeController(등록·취소)와 LikeQueryController(목록) 추가
- XUserIdArgumentResolver 첫 실사용 연결
- 등록·취소·목록의 정상·오류 케이스 E2E 테스트 추가
```

## 제외 범위

- 동시 요청 잠금·재시도·버전 검증(동시성은 다음 학습으로 이관)
- 좋아요 집계의 즉시 반영(숫자·인기순은 기존 주기 집계 스케줄러가 그대로 처리, 이번 PR에서 갱신 로직을 바꾸지 않음)
- 원격 브랜치 푸시·PR 생성·병합(별도 요청에 따름)

## 진행 기록

- 2026-09-18: 계획 수립 후 구현 착수. 구현 중 사용자 결정으로 쓰기 경로를 JPA에서 JDBC 전용으로 전환(위 "설계 변경" 참조).
- `LikeJpaEntity`를 삭제했다가 `product_likes` 테이블이 Hibernate `ddl-auto: create`로 더 이상 생성되지 않는 문제(`Table 'loopers.product_likes' doesn't exist`)를 발견해, `product_like_counts`/`ProductLikeCountJpaEntity`와 동일한 패턴으로 **스키마 생성 전용** `LikeJpaEntity`(Repository/Mapper 없음)를 다시 추가했다.
- `./gradlew :apps:commerce-api:check` 성공: 테스트 109개, 실패 0, 오류 0, 건너뜀 0. Checkstyle main/test, ArchUnit 레이어·도메인 순수성 검사 통과.
- 원격 브랜치 푸시·PR 생성은 아직 하지 않았다(별도 요청에 따름).
