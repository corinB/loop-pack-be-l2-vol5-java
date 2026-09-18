# PR 03 구현 계획: 좋아요 등록·취소·목록

## 목표

PR 02(`e55a95b`, PR #7)에서 Like 도메인·저장 구조·상품별 집계 테이블·주기 집계 스케줄러까지 준비됐다.
PR 03은 그 위에 **등록·취소 UseCase**와 **사용자별 좋아요 목록 조회 DAO**, 그리고 이를 연결하는 HTTP 계층을 추가한다.
브랜치는 `volume-2/pr-03-shopping`, 기준은 최신 `volume-2/main`이다.
관련 요구사항은 [01-requirements.md](01-requirements.md)의 R04·R05, 계약은 [05-use-cases.md](05-use-cases.md)·[06-read-models.md](06-read-models.md)를 따른다.

## 구현 범위

### 저장 계약 확장 (domain / infrastructure)

- `domain/shopping/like/LikeRepository`에 `findByUserIdAndProductId(userId, productId): Optional<Like>`와 `remove(userId, productId): void`를 추가한다. `Like.java` 본문은 바꾸지 않는다 (기존 구조적 검증 스타일이 `Brand`/`Product`와 이미 일치함을 확인했다).
- `LikeJpaRepository`에 파생 쿼리(`findByUserIdAndProductId`, `deleteByUserIdAndProductId`)를 추가한다.
- `LikeRepositoryImpl.save`에서 `DataIntegrityViolationException`을 잡아 기존 행을 다시 읽어 반환한다. 동시 요청 검증·잠금은 추가하지 않으며, 이 방어 코드는 유일성 제약과 조회 후 저장 사이의 경합에 대한 최소한의 안전장치일 뿐이다.

### 좋아요 등록·취소 (application)

- `application/shopping/like/LikeCommand`에 `Register(userId, productId)`, `Cancel(userId, productId)` record를 둔다.
- `RegisterLikeUseCase`, `CancelLikeUseCase` 인터페이스와 이를 함께 구현하는 `LikeService`를 추가한다(`BrandService`처럼 한 Service가 여러 UseCase를 구현).
- 등록: `ProductRepository.findById`로 활성 상품을 확인(없거나 삭제됨 → `ApplicationException(PRODUCT_NOT_FOUND)`) → 기존 관계가 없을 때만 저장. 이미 있으면 관계를 바꾸지 않고 성공 처리.
- 취소: 상품 조회 없이 지정 사용자·productId의 관계만 제거. 관계가 없어도 성공.
- 두 UseCase 모두 `@Transactional`. 사용자 존재 검사는 `@XUserId` resolver가 이미 수행하므로 Service에서 재검사하지 않는다.

### 내 좋아요 목록 (application / infrastructure)

- `application/shopping/like/LikeQueryDao#findByUserId(userId, PageCriteria): PageResult<LikeItem>`.
- `LikeItem` = `ProductSummary`의 필드(productId, name, price, brand: BrandSummary, likeCount) + `likedAt`.
- `infrastructure/shopping/like/JdbcLikeQueryDao`가 `product_likes ⋈ products ⋈ brands ⋈(LEFT) product_like_counts`를 `JdbcClient`로 조인한다. 조건: `user_id = :userId AND products.deleted = false`. 정렬은 `Like.createdAt DESC, productId DESC` 고정이라 QueryDSL 없이 `JdbcClient`로 충분하다(동적 정렬이 필요한 상품 목록만 QueryDSL을 쓴다는 기존 구분 유지). `@Transactional(readOnly = true)`.

### API 연결 (interfaces)

- `interfaces/api/shopping/like/LikeController` — `/api/v1/products/{productId}/likes`에 `POST`(등록)·`DELETE`(취소). `@XUserId long userId` 파라미터 사용 — PR 01에서 만든 resolver의 첫 실사용이다. 응답은 `ApiResponse.success()`(200, null).
- `interfaces/api/shopping/like/LikeQueryController` — `/api/v1/users/{userId}/likes`에 `GET`. 경로 userId는 헤더와 비교하지 않고 `UserQueryDao.findById`(resolver와 동일 DAO)로 존재만 확인 → 없으면 404. `page`/`size` 기본값 0/20.

### 저장기술 사용 구분 (JPA / JdbcClient / QueryDSL)

| 위치 | 기술 | 이유 |
|---|---|---|
| `LikeJpaRepository`/`LikeRepositoryImpl` (등록·취소 쓰기) | **JPA** (Spring Data JPA) | 단일 Entity(`LikeJpaEntity`)를 저장·삭제하는 단건 쓰기 경로. `Brand`/`Product`/`User`도 쓰기는 전부 JPA(`JpaRepository`+`RepositoryImpl`+`EntityMapper`)를 쓰는 기존 패턴을 그대로 따른다. 복잡한 동적 쿼리가 필요 없다. |
| `JdbcLikeQueryDao` (내 좋아요 목록 조회) | **JdbcClient** (Spring JDBC) | `product_likes ⋈ products ⋈ brands ⋈ product_like_counts` 4테이블 조인이지만 필터는 `userId` 하나, 정렬은 `Like.createdAt DESC, productId DESC`로 **고정**이다(요청마다 바뀌지 않음). 기존 `JdbcBrandQueryDao`/`JdbcUserQueryDao`/`JdbcProductLikeCountQueryDao`가 확립한 "단순·고정 조회는 JdbcClient" 패턴을 그대로 따른다. |
| **QueryDSL은 쓰지 않음** | — | QueryDSL은 지금 `QueryDslProductQueryDao` 한 곳에만 쓰인다. 상품 목록 API가 `brandId`(선택) 필터와 `sort`(`latest`/`price_asc`/`likes_desc`) 파라미터로 요청마다 조건·정렬이 **동적으로** 바뀌기 때문이다. 좋아요 목록은 필터·정렬이 고정 하나뿐이라 이 조건에 해당하지 않는다. [06-read-models.md](06-read-models.md)의 "단순/집계 = JdbcClient, 동적 상품 필터/정렬 = QueryDSL" 구분을 그대로 따른 것이다. |

`LikeRepositoryImpl.save`의 `DataIntegrityViolationException` 방어 코드는 JPA 계층이 던지는 예외를 잡는 것이라 별도 기술 선택 사항은 아니다.

## 테스트와 완료 조건

- `LikeRepositoryImpl`/`LikeJpaRepository`: 실제 DB로 조회·삭제·유일성 제약 검증(`LikeStorageIntegrationTest` 확장).
- `LikeService`: 실제 `Like`/`Product` 도메인 + mock repository로 — 정상 등록, 중복 등록 idempotent, 삭제 상품 등록 시 `PRODUCT_NOT_FOUND`, 취소 시 상품 미조회, 관계 없는 취소 idempotent, `DataIntegrityViolationException` 방어 코드가 idempotent 성공으로 이어지는지 검증.
- `JdbcLikeQueryDao`: 실제 DB로 페이지·정렬(동률 productId DESC)·삭제 상품 제외·집계 행 없을 때 likeCount 0 검증.
- `LikeController`/`LikeQueryController`: 실제 Controller+application+repository+DB로 등록/취소 200, 헤더 누락·형식 오류 E02, 없는 사용자·삭제 상품 E04, 목록 200/E04/E01(잘못된 페이지 파라미터) 검증.
- 완료 조건: `./gradlew :apps:commerce-api:check` 통과, [development-plan.md](development-plan.md)의 PR 03 완료 조건(반복 요청 성공, 관계·목록 즉시 반영, 숫자·인기순 지연 반영, 삭제 상품 처리) 각각 테스트로 커버.

## 커밋 순서

```
feat: 좋아요 관계 조회·삭제 저장 메서드 추가
- LikeRepository에 findByUserIdAndProductId, remove 추가
- LikeJpaRepository 파생 쿼리와 LikeRepositoryImpl 구현
- 유일성 제약 충돌 시 idempotent 처리, 통합 테스트 추가
```

```
feat: 좋아요 등록·취소 유스케이스 구현
- LikeCommand.Register/Cancel과 두 UseCase 인터페이스 추가
- LikeService 구현: 등록 시 활성 상품 확인, 취소 시 상품 미조회
- 정상·중복·삭제 상품·관계 없음 케이스 단위 테스트 추가
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

- 2026-09-18: 계획 작성만 완료. 구현·검증은 아직 시작하지 않았다.
