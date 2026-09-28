# 패키지 구조 리팩토링 결정 기록

[계획](plan.md) · [체크리스트](checklist.md)

계획 단계에서 사용자와 문답으로 합의한 결정과 그 이유를 기록한다. 작업 중 새 결정이 생기면 아래에 이어 붙인다.

## 계획 단계 결정

1. **feature 대신 종류로 묶는다.** `layer.context.feature.File`을 `layer.context.<종류>.File`로 바꾼다.
   feature 폴더 하나에 UseCase·Service·Command·Result·QueryDao·조회 record가 섞여 있어 역할별로 찾기 어렵기 때문이다.
2. **infrastructure만 종류 우선이다.** `infrastructure.<종류>.<ctx>` (`persistence`, `query`, `dao`, `scheduler`, `initializer`).
   persistence 아래는 `<ctx>.{entity,jpa,repository}`로 한 번 더 나눈다. 사용자가 persistence를 최상위 묶음으로 두길 원했고,
   조회 DAO·스케줄러 등도 같은 방식으로 맞췄다.
3. **mapper는 entity 폴더에 둔다.** JpaEntity 8개의 생성자가 package-private이고 mapper만 호출한다.
   mapper를 따로 두면 생성자 8개를 public으로 열어야 해서, 접근제어를 지키는 쪽을 골랐다.
   그 결과 public 확대는 `UserJpaRepository` 한 건뿐이다(UserRepositoryImpl이 다른 폴더에서 사용).
4. **domain 종류는 `model`, `repository`, `policy`다.** `OrderConfirmationPolicy`만 policy로 간다.
5. **application 종류는 `usecase`, `service`, `command`, `result`, `query`, `dao`다.**
   `ConfirmOrderWriter`, `LikeCommandDao`, `LikeCountAggregationDao`처럼 infrastructure가 구현하는 쓰기 전용 계약은
   port 대신 `dao`라는 이름을 쓴다. `ConfirmOrderLoad`는 그 계약의 입출력이므로 같은 `dao`에 둔다.
6. **조회 모델은 모두 `*View`로 통일한다.** BrandDetail→BrandView, BrandSummary→BrandSummaryView,
   ProductSummary→ProductSummaryView, ProductDetail→ProductDetailView, AdminProduct→AdminProductView,
   LikeItem→LikedProductView, UserQueryModel→UserView. 쓰기 결과는 기존대로 `*Result`다.
   타입 이름만 바꾸고 변수·메서드명과 응답 JSON은 그대로 둔다.
7. **interfaces 종류는 `controller`, `dto`다.**
8. **컨텍스트 간 이동은 하지 않는다.** mall에 있는 like 관련 파일(`ProductLikeCountQueryDao` 등)도 그대로 둔다.
9. **테스트는 대상 클래스와 같은 커밋·같은 패키지로 옮긴다.** E2E는 controller, Service·통합 테스트는 service,
   Repository 통합 테스트는 persistence repository로 간다.
10. **커밋은 문서 1 + 레이어별 1 + 결과 문서 1**, 총 6개다. 순서는 domain → infrastructure → application → interfaces.
    타입은 `refactor:`, 기존 커밋처럼 한글 요약 + 불릿 본문, `Co-Authored-By` 없음.
11. **검증은 매 커밋 컴파일 + Checkstyle + ArchUnit, 마지막에 `check` 전체**로 한다(통합 테스트는 Docker가 필요해 속도를 우선).
12. **실행은 Sonnet 에이전트에게 레이어 단위로 위임**하고, 레이어마다 결과를 검토해 사용자에게 보고한 뒤 다음으로 넘어간다.

## 작업 중 기록

- 커밋 1(domain) 실행 중 `OrderConfirmationPolicy`가 `ordering.policy`로 가면서, 같은 패키지에 있던
  `Order`·`OrderItem`·`OrderStatus`·`OrderConfirmation`(모두 `ordering.model`로 이동)에 대한 명시적 import 4개를
  추가로 붙여야 했다. `Brand`는 `Product`와 같은 패키지(`mall.model`)로 함께 이동해 기존 `import Product`가
  중복(redundant) import가 되어 제거했다. `OrderConfirmation`은 `Order`와 같은 패키지로 갔지만 `Product`·`Wallet`·
  `PointBill`은 다른 패키지로 갈라져 그 세 개만 import를 갱신했다.
- 커밋 2(infrastructure) 실행 중 JpaEntity/EntityMapper가 `entity` 폴더로, JpaRepository가 `jpa` 폴더로,
  RepositoryImpl이 `repository` 폴더로 갈라지면서 같은 feature 안에서만 서로 참조하던 타입들도 명시적 import가
  필요해졌다: 각 `*JpaRepository`는 자신의 `*JpaEntity`를, 각 `*RepositoryImpl`은 자신의 `*JpaRepository`·
  `*EntityMapper`·(필요한 경우) `*JpaEntity`를 새로 import했다. `BrandJpaEntity`·`BrandEntityMapper`는 같은 패키지가
  된 `ProductJpaEntity`에 대한 기존 import가 중복이 되어 제거했다. QueryDSL이 생성하는 `Q*JpaEntity`도 원본
  엔티티와 같은 새 패키지에 생성되므로 `QueryDslProductQueryDao`의 `QBrandJpaEntity`·`QProductLikeCountJpaEntity`·
  `QProductJpaEntity` import를 새 경로로 맞췄다. 계획대로 접근제어 확대는 `UserJpaRepository` → `public` 한 건뿐이다.
- 커밋 3(application) 실행 중 UseCase·Service·조회 View가 `usecase`/`service`/`command`/`result`/`query`/`dao`로
  갈라지면서, 같은 feature 안에서만 서로 참조하던 타입 거의 전부에 명시적 import가 필요해졌다: 각 UseCase 인터페이스는
  자신의 `Command`·`Result`를, `BrandService`·`ProductService`·`ConfirmOrderService`·`OrderService`·`WalletService`·
  `LikeCountAggregationService`는 자신이 구현하는 UseCase 인터페이스와 `Command`·`Result`·(필요한 경우) `dao` 타입을,
  `OrderView`·`OrderItemView`·`AdminProductView`는 자신이 감싸는 `Result` 타입을 새로 import했다. 테스트 쪽도 같은
  이유로 `ConfirmOrderCommand`/`ConfirmOrderResult`/`ConfirmOrderWriter`/`ConfirmOrderLoad`,
  `OrderCommand`/`OrderResult`, `WalletCommand`/`WalletResult`, `BrandCommand`, `LikeCountAggregationUseCase` 등을
  새로 import했다(단, 테스트와 같은 패키지로 옮겨진 `ConfirmOrderService`/`OrderService`/`WalletService`는 같은
  패키지라 import가 필요 없어 추가하지 않음). 조회 모델 7개 이름 변경은 단어 경계 치환으로 처리했고,
  `findAdminProduct(s)`처럼 이름을 포함하는 메서드명은 그대로 유지됐다.
- 커밋 4(interfaces) 실행 중 사용자 요청으로 ArchUnit·전체 테스트는 실행하지 않고 컴파일(`compileJava`,
  `compileTestJava`)과 Checkstyle(`checkstyleMain`, `checkstyleTest`)만 통과를 확인했다. ArchUnit 회귀 확인은
  커밋 5 전 전체 `check` 실행 때로 미룬다. Controller와 DTO가 `controller`/`dto`로 갈라지면서, 같은 feature
  안에서 암묵적으로(같은 패키지) DTO를 참조하던 `AdminBrandController`(`BrandApiDto`), `AdminProductController`
  (`ProductApiDto`), `OrderController`(`OrderApiDto`), `WalletController`·`WalletQueryController`
  (`WalletApiDto`)에 명시적 import를 추가했다. `BrandQueryController`·`ProductQueryController`·
  `LikeController`·`LikeQueryController`는 자기 컨텍스트의 DTO를 쓰지 않아 추가 import가 없었다. 테스트 쪽도
  같은 이유로 `BrandApiE2ETest`(`BrandApiDto`), `OrderApiE2ETest`(`OrderApiDto`), `WalletApiE2ETest`
  (`WalletApiDto`)에 새 import를 추가했고, `ProductApiE2ETest`는 이미 있던 `BrandApiDto`의 cross-feature import
  경로를 `mall.dto`로 갱신하면서 `ProductApiDto` import를 새로 추가했다.
