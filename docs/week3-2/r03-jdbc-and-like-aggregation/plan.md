# R03 구현 계획

[요구사항](requirement.md) · [트레이드오프](trade_off/total_trade_off.md) · [전체 요구사항](../total_requirement.md)

작업 브랜치: `volume-3/r03-jdbc-and-like-aggregation` · PR 대상: `volume-3/main`

상태: 좋아요 등록·취소 절(커밋 1~5) 계획 합의, 구현 위임 전. 조회 전환·집계 절은 트레이드오프 문답 후 이 문서에 이어서 추가한다.
브랜치는 `volume-3/main`에 아직 병합되지 않은 `volume-3/refacto`에서 분기했다. refacto가 병합되면 main 기준으로 맞추고, 그 전에는 PR을 만들지 않는다.

## 문서와 진행 원칙

각 구현 커밋은 **실패 테스트 확인 → 구현 → 리팩터링 → 관련 테스트 통과 → 커밋** 순서로 진행한다. 테스트와 해당 구현을 함께 커밋한다.
구현은 이 문서를 기준으로 Sonnet 에이전트에 위임하고, 결과 diff·커밋·테스트는 직접 확인한 뒤 체크박스를 갱신한다.
테스트는 가볍게 유지한다. 도메인·Service는 단위 테스트로, 저장은 공유 `@IntegrationTest` 통합 테스트 1개로 확인하고 동시성 테스트는 추가하지 않는다.

## 좋아요 등록·취소

### 설계

결정 근거는 [도메인 구조](trade_off/01-like-aggregate.md), [중복 처리](trade_off/02-like-duplicate.md), [상품 확인](trade_off/03-like-product-check.md)을 따른다.

| 계층 | 요소 | 책임 |
|---|---|---|
| interfaces | `LikeController` | id 형식 검증 후 `RegisterLikeUseCase`·`CancelLikeUseCase` 호출. HTTP 계약(경로·200·404) 유지 |
| application | `RegisterLikeUseCase`, `CancelLikeUseCase`, `LikeService` | `@Transactional`. 등록은 상품 조회·활성 확인 후 저장, 취소는 상품 확인 없이 삭제 |
| domain | `shopping.model.Like`, `shopping.repository.LikeRepository` | Like는 `id`·`userId`·`productId`·`likedAt`, `create(userId, productId)`(id·likedAt null), `restore(...)`. 양수 id가 아니면 `IllegalArgumentException`. Repository는 `void save(Like)`(중복이면 무시), `void delete(long userId, long productId)` |
| infrastructure | `persistence.shopping.jpa.LikeJpaRepository`, `persistence.shopping.repository.LikeRepositoryImpl` | HQL `insert … on conflict do nothing`, JPQL delete. `created_at`은 구현에서 `Instant.now()` |

호출과 트랜잭션 경계는 다음과 같다.

```
POST   /api/v1/products/{productId}/likes
  LikeController.register → LikeService.register (REQUIRED)
    → ProductRepository.findById         없으면 ApplicationException(PRODUCT_NOT_FOUND) → 404
    → Product.ensureActive                삭제면 DomainException(DELETED_PRODUCT)   → 404
    → LikeRepository.save(Like.create)    insert … on conflict do nothing (0 또는 1행)

DELETE /api/v1/products/{productId}/likes
  LikeController.cancel → LikeService.cancel (REQUIRED)
    → LikeRepository.delete               delete … where userId and productId (0 또는 1행)
```

- 상품 조회와 저장은 같은 트랜잭션이며 잠금은 없다. 조회와 저장 사이에 상품이 삭제되는 경쟁은 허용한다.
- `X-USER-ID` 사용자 존재 확인은 기존 `@XUserId` resolver가 맡는다. Service는 다시 확인하지 않는다.
- 제거 대상은 `application/shopping/dao/LikeCommandDao`, `infrastructure/dao/shopping/JdbcLikeCommandDao`, `JdbcLikeCommandDaoIntegrationTest`다. 집계 DAO(`LikeCountAggregationDao`, `JdbcLikeCountAggregationDao`)와 조회 DAO는 이 절에서 건드리지 않는다.

### 커밋 1 — 좋아요 도메인 모델과 저장소 계약

- [ ] `LikeTest`(POJO)를 먼저 쓴다. create가 id·likedAt을 비우는지, restore가 모든 값을 채우는지, userId·productId가 양수가 아니면 `IllegalArgumentException`인지 확인한다. restore는 id·likedAt 누락도 거절한다.
- [ ] `domain/shopping/model/Like.java`, `domain/shopping/repository/LikeRepository.java`를 추가한다. 참고: `domain/mall/model/Brand.java`의 create·restore와 구조 검증 스타일.
- [ ] 커밋: `refactor: 좋아요 도메인 모델과 저장소 계약 추가`

### 커밋 2 — 좋아요 저장소 JPA 구현

- [ ] `infrastructure/persistence/shopping/repository/LikeRepositoryIntegrationTest`(`@IntegrationTest`)를 먼저 쓴다. 새 등록 1행, 같은 등록을 반복해도 예외 없이 1행이고 `created_at`도 바뀌지 않음, 취소하면 0행, 없는 좋아요를 취소해도 예외 없음을 확인한다. 검증 쿼리는 기존 `JdbcLikeCommandDaoIntegrationTest.countLikes`처럼 JdbcClient를 쓴다.
- [ ] `LikeJpaRepository`(`@Modifying @Query`의 HQL insert·JPQL delete)와 `LikeRepositoryImpl`을 추가한다.
- [ ] **중단 조건:** HQL on conflict가 MySQL에서 실행되지 않거나, 중복이 예외를 내거나, 기존 행을 바꾸면 구현을 멈추고 실행 SQL 로그와 함께 보고한다. native 쿼리로 대체하지 않는다.
- [ ] 테스트 로그에서 HQL insert가 바뀐 실제 MySQL SQL을 확인해 아래 검증 기록에 남긴다.
- [ ] 커밋: `refactor: 좋아요 저장소 JPA 구현 추가`

### 커밋 3 — 좋아요 등록·취소 유스케이스

- [ ] `LikeServiceTest`(Mockito)를 먼저 쓴다. 상품이 없으면 `PRODUCT_NOT_FOUND`이고 save 미호출, 삭제된 상품이면 `DELETED_PRODUCT`이고 save 미호출, 활성 상품이면 해당 userId·productId로 save 호출, 취소는 delete를 호출하고 상품을 조회하지 않음을 확인한다. 참고: `OrderServiceTest`.
- [ ] `application/shopping/usecase/RegisterLikeUseCase`, `CancelLikeUseCase`, `application/shopping/service/LikeService`를 추가한다. 여러 UseCase를 한 Service가 구현하는 방식은 `BrandService`, 상품 확인은 `OrderService`의 `findById` + `ensureActive`를 따른다.
- [ ] 커밋: `refactor: 좋아요 등록·취소 유스케이스 추가`

### 커밋 4 — 좋아요 API 전환과 JDBC 좋아요 DAO 제거

- [ ] `LikeController`가 UseCase를 호출하도록 바꾼다.
- [ ] `LikeCommandDao`, `JdbcLikeCommandDao`, `JdbcLikeCommandDaoIntegrationTest`를 삭제한다. 활성 상품 확인 테스트는 커밋 3의 Service 단위 테스트가 대신한다.
- [ ] `LikeApiE2ETest`가 기대값 변경 없이 통과하는지 확인한다. 삭제된 상품의 응답 본문 에러 코드 문자열이 바뀌면 기록하고 보고한다.
- [ ] 커밋: `refactor: 좋아요 API를 유스케이스로 전환하고 JDBC 좋아요 DAO 제거`

### 커밋 5 — 레이어별 테스트 문서 갱신

- [ ] `docs/test/{domain,application,infrastructure,interfaces}.md`에서 `JdbcLikeCommandDao` 관련 언급, 테스트 목록, 집계 수를 실제에 맞게 갱신한다.
- [ ] 커밋: `docs: 좋아요 전환에 맞춰 레이어별 테스트 문서 갱신`

### 검증

- [ ] `./gradlew :apps:commerce-api:test --tests "*Like*"`
- [ ] `./gradlew :apps:commerce-api:check`를 실행하고 실행 건수·실패·skip·종료 결과를 기록한다.
- [ ] 운영 코드에서 `JdbcClient` 사용처가 조회 DAO 6개와 집계 DAO만 남았는지 grep으로 확인한다.
- [ ] 기록: HQL insert의 실제 SQL, check 결과.

## 조회 DAO의 QueryDSL 전환

트레이드오프 4·6번 문답 후 추가한다.

## 좋아요 집계

트레이드오프 5번 문답 후 추가한다.
