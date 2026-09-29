# R03 구현 계획

[요구사항](requirement.md) · [트레이드오프](trade_off/total_trade_off.md) · [전체 요구사항](../total_requirement.md)

작업 브랜치: `volume-3/r03-jdbc-and-like-aggregation` · PR 대상: `volume-3/main`

상태: 좋아요 등록·취소 커밋 1~5 구현·검증 완료(Sonnet 위임, check 통과). 구현 후 조정(커밋 6)과 조회 전환(커밋 7~11)은 계획 합의, 구현 위임 전. 집계 절은 트레이드오프 문답 후 추가한다.
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
    → product.isDeleted()                 삭제면 ApplicationException(PRODUCT_NOT_FOUND) → 404 (커밋 6에서 ensureActive·DELETED_PRODUCT에서 조정)
    → LikeRepository.save(Like.create)    insert … on conflict do nothing (0 또는 1행)

DELETE /api/v1/products/{productId}/likes
  LikeController.cancel → LikeService.cancel (REQUIRED)
    → LikeRepository.delete               delete … where userId and productId (0 또는 1행)
```

- 상품 조회와 저장은 같은 트랜잭션이며 잠금은 없다. 조회와 저장 사이에 상품이 삭제되는 경쟁은 허용한다.
- `X-USER-ID` 사용자 존재 확인은 기존 `@XUserId` resolver가 맡는다. Service는 다시 확인하지 않는다.
- 제거 대상은 `application/shopping/dao/LikeCommandDao`, `infrastructure/dao/shopping/JdbcLikeCommandDao`, `JdbcLikeCommandDaoIntegrationTest`다. 집계 DAO(`LikeCountAggregationDao`, `JdbcLikeCountAggregationDao`)와 조회 DAO는 이 절에서 건드리지 않는다.

### 커밋 1 — 좋아요 도메인 모델과 저장소 계약

- [x] `LikeTest`(POJO)를 먼저 쓴다. create가 id·likedAt을 비우는지, restore가 모든 값을 채우는지, userId·productId가 양수가 아니면 `IllegalArgumentException`인지 확인한다. restore는 id·likedAt 누락도 거절한다.
- [x] `domain/shopping/model/Like.java`, `domain/shopping/repository/LikeRepository.java`를 추가한다. 참고: `domain/mall/model/Brand.java`의 create·restore와 구조 검증 스타일.
- [x] 커밋: `refactor: 좋아요 도메인 모델과 저장소 계약 추가`

### 커밋 2 — 좋아요 저장소 JPA 구현

- [x] `infrastructure/persistence/shopping/repository/LikeRepositoryIntegrationTest`(`@IntegrationTest`)를 먼저 쓴다. 새 등록 1행, 같은 등록을 반복해도 예외 없이 1행이고 `created_at`도 바뀌지 않음, 취소하면 0행, 없는 좋아요를 취소해도 예외 없음을 확인한다. 검증 쿼리는 기존 `JdbcLikeCommandDaoIntegrationTest.countLikes`처럼 JdbcClient를 쓴다.
- [x] `LikeJpaRepository`(`@Modifying @Query`의 HQL insert·JPQL delete)와 `LikeRepositoryImpl`을 추가한다.
- [x] **중단 조건:** HQL on conflict가 MySQL에서 실행되지 않거나, 중복이 예외를 내거나, 기존 행을 바꾸면 구현을 멈추고 실행 SQL 로그와 함께 보고한다. native 쿼리로 대체하지 않는다.
- [x] 테스트 로그에서 HQL insert가 바뀐 실제 MySQL SQL을 확인해 아래 검증 기록에 남긴다.
- [x] 커밋: `refactor: 좋아요 저장소 JPA 구현 추가`

### 커밋 3 — 좋아요 등록·취소 유스케이스

- [x] `LikeServiceTest`(Mockito)를 먼저 쓴다. 상품이 없으면 `PRODUCT_NOT_FOUND`이고 save 미호출, 삭제된 상품이면 `DELETED_PRODUCT`이고 save 미호출, 활성 상품이면 해당 userId·productId로 save 호출, 취소는 delete를 호출하고 상품을 조회하지 않음을 확인한다. 참고: `OrderServiceTest`.
- [x] `application/shopping/usecase/RegisterLikeUseCase`, `CancelLikeUseCase`, `application/shopping/service/LikeService`를 추가한다. 여러 UseCase를 한 Service가 구현하는 방식은 `BrandService`, 상품 확인은 `OrderService`의 `findById` + `ensureActive`를 따른다.
- [x] 커밋: `refactor: 좋아요 등록·취소 유스케이스 추가`

### 커밋 4 — 좋아요 API 전환과 JDBC 좋아요 DAO 제거

- [x] `LikeController`가 UseCase를 호출하도록 바꾼다.
- [x] `LikeCommandDao`, `JdbcLikeCommandDao`, `JdbcLikeCommandDaoIntegrationTest`를 삭제한다. 활성 상품 확인 테스트는 커밋 3의 Service 단위 테스트가 대신한다.
- [x] `LikeApiE2ETest`가 기대값 변경 없이 통과하는지 확인한다. 삭제된 상품의 응답 본문 에러 코드 문자열이 바뀌면 기록하고 보고한다.
- [x] 커밋: `refactor: 좋아요 API를 유스케이스로 전환하고 JDBC 좋아요 DAO 제거`

### 커밋 5 — 레이어별 테스트 문서 갱신

- [x] `docs/test/{domain,application,infrastructure,interfaces}.md`에서 `JdbcLikeCommandDao` 관련 언급, 테스트 목록, 집계 수를 실제에 맞게 갱신한다.
- [x] 커밋: `docs: 좋아요 전환에 맞춰 레이어별 테스트 문서 갱신`

### 검증

- [x] `./gradlew :apps:commerce-api:test --tests "*Like*"`
- [x] `./gradlew :apps:commerce-api:check`를 실행하고 실행 건수·실패·skip·종료 결과를 기록한다.
- [x] 운영 코드에서 `JdbcClient` 사용처가 조회 DAO 6개와 집계 DAO만 남았는지 grep으로 확인한다.
- [x] 기록: HQL insert의 실제 SQL, check 결과.

검증 기록(커밋 1~5 시점)
- HQL insert의 실제 SQL: `insert into product_likes(user_id,product_id,created_at) values (?,?,?) as excluded(user_id,product_id,created_at) on duplicate key update user_id=product_likes.user_id`. 중복이면 값이 바뀌지 않는 갱신이다.
- 취소 SQL: `delete lje1_0 from product_likes lje1_0 where lje1_0.user_id=? and lje1_0.product_id=?`.
- `./gradlew :apps:commerce-api:check` BUILD SUCCESSFUL. test 219건, slowTest 17건, 실패·오류·skip 0건. Checkstyle·ArchUnit 통과.
- 운영 코드의 JdbcClient: 조회 DAO 6개(`JdbcBrandQueryDao`, `JdbcProductLikeCountQueryDao`, `JdbcOrderQueryDao`, `JdbcWalletQueryDao`, `JdbcLikeQueryDao`, `JdbcUserQueryDao`)와 `JdbcLikeCountAggregationDao`만 남음.
- 계획과 다른 점: `LikeRepositoryIntegrationTest`에 클래스 단위 `@Transactional`을 붙였다. `@Modifying` 쿼리는 트랜잭션 밖에서 `TransactionRequiredException`이 나기 때문이며, `BrandRepositoryIntegrationTest`와 같은 방식이다. UseCase는 `register`·`cancel` 메서드로 구현됐고, 삭제된 상품의 응답 코드가 `DELETED_PRODUCT`로 바뀌었다. 뒤의 두 가지는 커밋 6에서 조정한다.

### 커밋 6 — 좋아요 UseCase 형식과 삭제 상품 응답 코드 조정

결정: [UseCase 형식](trade_off/01-like-aggregate.md#구현-후-조정--usecase-형식), [PRODUCT_NOT_FOUND로 되돌리기](trade_off/03-like-product-check.md#구현-후-조정--product_not_found로-되돌리기).

- [ ] `LikeServiceTest`를 먼저 고친다. 삭제된 상품이면 `ApplicationException(PRODUCT_NOT_FOUND)`이고 save를 호출하지 않는지 확인한다. 호출은 `execute(LikeCommand.Register)`·`execute(LikeCommand.Cancel)`로 바꾼다.
- [ ] `application/shopping/command/LikeCommand`(`Register(long userId, long productId)`, `Cancel(long userId, long productId)` record)를 추가한다. 참고: `application/mall/command/BrandCommand`.
- [ ] `RegisterLikeUseCase`·`CancelLikeUseCase`를 `execute(LikeCommand.*)`로 바꾸고, `LikeService`와 `LikeController`를 맞춘다.
- [ ] `LikeService.register`는 `productRepository.findById(productId).filter(product -> !product.isDeleted())`가 비면 `ApplicationException(PRODUCT_NOT_FOUND)`를 던진다. `ensureActive`는 호출하지 않는다.
- [ ] `LikeApiE2ETest`의 삭제된 상품 케이스에 응답 본문 에러 코드가 `PRODUCT_NOT_FOUND`인지 확인하는 단언 하나를 추가한다. 회귀를 막기 위한 것이며, 새 테스트 메서드는 만들지 않는다. 기존 E2E의 코드 단언 방식을 참고한다.
- [ ] 커밋: `refactor: 좋아요 유스케이스를 커맨드 형식으로 맞추고 삭제 상품 응답 코드를 유지`

## 조회 DAO의 QueryDSL 전환

### 설계

결정 근거는 [조회 전환 방식](trade_off/04-query-conversion.md)을 따른다.

- 조회는 `JPAQueryFactory`와 `Projections.constructor`로 만들고, 엔티티를 조회하지 않는다. Q타입은 기존 JPA 엔티티의 Q클래스(`QBrandJpaEntity`, `QOrderJpaEntity`, `QOrderItemJpaEntity`, `QOrderRecordJpaEntity`, `QLikeJpaEntity`, `QProductJpaEntity`, `QProductLikeCountJpaEntity`, `QWalletJpaEntity`, `QUserJpaEntity`)를 쓴다. 참고: `infrastructure/query/mall/QueryDslProductQueryDao`.
- 필드가 1:1이면 View를 바로 만든다. 중첩 View·변환·여러 View가 필요할 때만 infra Row를 둔다(`OrderHeaderRow`, `OrderItemRow` 유지. Like는 `BrandSummaryView` 중첩 때문에 Row 또는 중첩 Projection 중 단순한 쪽).
- `application` 계약(QueryDao 인터페이스, View)과 공개 메서드 시그니처는 바꾸지 않는다. 각 DAO의 `@Transactional(readOnly = true)`는 유지한다.
- SQL 의미는 그대로 옮긴다. 조건(`deleted = false` 등), 정렬(`created_at DESC, id DESC` 등), `LIMIT/OFFSET`, `COUNT`, `COALESCE(like_count, 0)`, LEFT/INNER JOIN 구분을 유지한다. 매핑된 연관관계가 있으면 연관 경로로, 없으면 `on`의 id 비교로 조인한다.
- 주문 목록은 헤더 페이지 조회 → 품목 `IN` 조회의 2단계를 유지한다. fetch join과 엔티티 조회는 쓰지 않는다.
- 클래스 이름은 `Jdbc*QueryDao` → `QueryDsl*QueryDao`, 테스트 클래스도 같은 규칙으로 바꾼다. 테스트 기대값은 바꾸지 않는다.

### 커밋 7 — 상품 쓰기 응답을 조회 경로로 만들고 좋아요 수 조회 DAO 제거

- [ ] `CreateProductUseCase`·`UpdateProductUseCase`·`SetProductStockUseCase`가 상품 id(`long`)를 반환하도록 바꾼다. `ProductService`에서 `ProductLikeCountQueryDao` 의존과 `result(...)`를 제거한다. 브랜드 활성 확인(`findBrand` → `brand.ensureActive()`)은 업무 규칙이므로 유지한다.
- [ ] `AdminProductController`의 생성·수정·재고 설정이 UseCase의 id로 `productQueryDao.findAdminProduct(id)`를 호출해 응답을 만든다. 비어 있으면 기존 `notFound()`를 쓴다. 생성은 기존처럼 201이다.
- [ ] 쓰이지 않게 된 `ProductResult`, `AdminProductView.from(ProductResult)`, `application/mall/query/ProductLikeCountQueryDao`, `infrastructure/query/mall/JdbcProductLikeCountQueryDao`를 삭제한다.
- [ ] 관련 단위 테스트(`ProductServiceTest` 등이 있으면)를 반환값 변경에 맞춘다. 관리자 상품 E2E 기대값은 바꾸지 않고 통과해야 한다.
- [ ] 커밋: `refactor: 상품 쓰기 응답을 조회 경로로 만들고 좋아요 수 조회 DAO 제거`

### 커밋 8 — mall 조회 DAO 전환

- [ ] `JdbcBrandQueryDao` → `QueryDslBrandQueryDao`(단건, 페이지, `COUNT`). `BrandApiE2ETest`·관리자 브랜드 E2E가 기대값 변경 없이 통과한다.
- [ ] 커밋: `refactor: 브랜드 조회 DAO를 QueryDSL로 전환`

### 커밋 9 — pay 조회 DAO 전환

- [ ] `JdbcWalletQueryDao` → `QueryDslWalletQueryDao`. `JdbcWalletQueryDaoIntegrationTest` → `QueryDslWalletQueryDaoIntegrationTest`(이름만 변경).
- [ ] 커밋: `refactor: 지갑 조회 DAO를 QueryDSL로 전환`

### 커밋 10 — shopping 조회 DAO 전환

- [ ] `JdbcUserQueryDao` → `QueryDslUserQueryDao`. `@XUserId` resolver가 모든 요청에서 호출하므로 조회는 id 한 컬럼만 가져온다.
- [ ] `JdbcLikeQueryDao` → `QueryDslLikeQueryDao`(`product_likes` ⋈ `products` ⋈ `brands` ⟕ `product_like_counts`, 정렬 `created_at DESC, product_id DESC`, `COUNT`).
- [ ] 두 통합 테스트는 이름만 변경한다.
- [ ] 커밋: `refactor: 사용자·좋아요 조회 DAO를 QueryDSL로 전환`

### 커밋 11 — ordering 조회 DAO 전환

- [ ] `JdbcOrderQueryDao` → `QueryDslOrderQueryDao`. 사용자별·관리자 목록(헤더 페이지 + 품목 `IN`), 단건(헤더 + 품목), `COUNT`를 옮긴다. 주문 기록은 LEFT JOIN으로 유지한다.
- [ ] `JdbcOrderQueryDaoIntegrationTest` → `QueryDslOrderQueryDaoIntegrationTest`(이름만 변경).
- [ ] 커밋: `refactor: 주문 조회 DAO를 QueryDSL로 전환`

### 커밋 12 — 테스트 문서 갱신

- [ ] `docs/test/*.md`에서 바뀐 클래스 이름과 삭제된 DAO를 반영한다.
- [ ] 커밋: `docs: 조회 DAO 전환에 맞춰 레이어별 테스트 문서 갱신`

### 검증

- [ ] 커밋마다 해당 컨텍스트 테스트(`--tests "*Brand*"` 등)를 실행한다.
- [ ] 마지막에 `./gradlew :apps:commerce-api:check`를 실행하고 건수·실패·skip을 기록한다.
- [ ] 운영 코드의 JdbcClient가 `JdbcLikeCountAggregationDao` 하나만 남았는지 grep으로 확인한다.
- [ ] 주문 목록 1회 조회의 실행 SQL이 COUNT·헤더·품목 3회인지 테스트 로그로 확인해 기록한다.

## 좋아요 집계

트레이드오프 5번 문답 후 추가한다.
