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

(레이어 작업 중 생긴 결정·예외를 여기에 추가한다.)
