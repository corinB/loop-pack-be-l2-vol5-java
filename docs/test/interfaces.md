# 인터페이스 레이어 테스트

## 1. 개요

`interfaces` 레이어 테스트는 크게 두 종류로 나뉜다.

- **E2E 테스트** (`*ApiE2ETest`, `ExampleV1ApiE2ETest`, `ContractClassificationTest`): `@SpringBootTest(webEnvironment = RANDOM_PORT)` + `TestRestTemplate`로 실제 HTTP 요청을 보내고, 컨트롤러부터 DB까지 전체 스택을 검증한다. Testcontainers(MySQL)를 사용하므로 Docker가 필요하다.
- **web-support 단위 테스트** (`ApiControllerAdviceTest`, `ApiJacksonConfigTest`, `XUserIdArgumentResolverTest`): 스프링 컨텍스트 없이(또는 `@JsonTest`처럼 최소 슬라이스만) 예외 매핑, Jackson 역직렬화 규칙, 헤더 파라미터 리졸버를 단위 수준에서 검증한다. Docker가 필요 없다.

또한 `architecture.LayerArchitectureTest`는 ArchUnit으로 `domain`/`application`/`interfaces`/`infrastructure` 사이의 의존 방향을 정적으로 검증하는 아키텍처 테스트다.

### Spring 컨텍스트 구성

`BrandApiE2ETest`, `ProductApiE2ETest`, `WalletApiE2ETest`, `LikeApiE2ETest`, `ExampleV1ApiE2ETest`, `ContractClassificationTest`는 모두 순수한 `@SpringBootTest(webEnvironment = RANDOM_PORT)`만 선언하므로 동일한 시그니처의 Spring 컨텍스트 하나를 공유해서 재사용된다. 반면 `OrderApiE2ETest`는 `@MockitoSpyBean private OrderRepository orderRepository`를 추가로 선언하기 때문에, 빈 오버라이드 조합이 달라져 별도의(자신만의) RANDOM_PORT 컨텍스트를 새로 띄운다. 이 스파이는 `Confirm#returnsInternalServerError_whenSaveFailsAfterRealSql` 한 테스트에서만 실제로 사용되고(`save` 호출에 `doAnswer`로 개입 후 예외를 던져 저장 실패를 재현), 나머지 `OrderApiE2ETest` 테스트에서는 `verify` 없이 실제 구현을 그대로 통과시킨다.

### 실행 방법

```bash
# 전체 interfaces 레이어 포함 전체 테스트
./gradlew :apps:commerce-api:test

# 특정 클래스
./gradlew :apps:commerce-api:test --tests "com.loopers.interfaces.api.ordering.controller.OrderApiE2ETest"

# 특정 중첩 그룹의 특정 메서드
./gradlew :apps:commerce-api:test --tests "com.loopers.interfaces.api.ordering.controller.OrderApiE2ETest\$Confirm.returnsConflict_whenStockIsInsufficient"
```

E2E 테스트는 DB에 붙으므로 실행 전 `docker-compose -f ./docker/infra-compose.yml up`으로 로컬 인프라가 떠 있어야 한다(Testcontainers가 직접 컨테이너를 띄우긴 하지만 Docker 데몬 자체는 필요).

### 개수 요약

| 구분 | 클래스 수 | 테스트(케이스) 수 |
|---|---|---|
| E2E (mall/ordering/pay/shopping) | 5 | 47 |
| E2E (공통 example/contract) | 2 | 7 |
| web-support 단위 테스트 | 3 | 24 |
| 아키텍처 테스트 | 1 | 5 |
| **합계** | **11** | **83** |

세부 산출은 2절 표의 "테스트 수" 열을 합산한 값이다. `ApiJacksonConfigTest`·`XUserIdArgumentResolverTest`의 파라미터라이즈드 테스트는 값 개수만큼 별도 테스트 케이스로 실행되며, 위 합계와 표의 각 셀 모두 그 실제 실행 건수를 기준으로 표기했다.

## 2. 컨텍스트별 테스트

### mall (브랜드·상품)

| 테스트 클래스 | 중첩 그룹 | Spring 컨텍스트 | Docker | 검증 시나리오 (DisplayName 요약) | 무엇을 단언하나 | 테스트 수 |
|---|---|---|---|---|---|---|
| [BrandApiE2ETest](../../apps/commerce-api/src/test/java/com/loopers/interfaces/api/mall/controller/BrandApiE2ETest.java) | ManageBrand | 공유 RANDOM_PORT | O | 등록·조회·수정·삭제 CRUD 전체 흐름; 활성 상품이 있는 브랜드 삭제 시 상품까지 cascade 삭제; 미삭제/기삭제 상품 혼재 시 처리; 없는 브랜드 삭제 404; 이미 삭제된 브랜드 재삭제 404 | HTTP + DB 상태(`brands`/`products.deleted` 컬럼 직접 조회) | 5 |
| [ProductApiE2ETest](../../apps/commerce-api/src/test/java/com/loopers/interfaces/api/mall/controller/ProductApiE2ETest.java) | ManageAndQueryProduct | 공유 RANDOM_PORT | O | 등록·수정·재고설정·조회·삭제 + 좋아요수/가격/최신 정렬과 페이징(QueryDSL 기반); 브랜드 일괄삭제 후 목록·상세 제외; 잘못된 정렬/페이지/가격 400과 미생성 확인 | HTTP + DB 상태(`products` 카운트) | 3 |

### ordering (주문)

| 테스트 클래스 | 중첩 그룹 | Spring 컨텍스트 | Docker | 검증 시나리오 (DisplayName 요약) | 무엇을 단언하나 | 테스트 수 / 최근 시간 |
|---|---|---|---|---|---|---|
| [OrderApiE2ETest](../../apps/commerce-api/src/test/java/com/loopers/interfaces/api/ordering/controller/OrderApiE2ETest.java) | Create | 전용 RANDOM_PORT(`@MockitoSpyBean OrderRepository`) | O | 동일 상품 중복 시 수량 합산 DRAFT 생성; 삭제 상품 포함 시 404·미생성; 품목 비어있으면 400; 없는 사용자 404 | HTTP + DB 상태(재고, 주문 개수) | 4 tests / 8.2s |
| | Confirm | 〃 | O | 재고·포인트 충분 시 200+결제결과; 확정 후 고객/관리자 조회에도 결제결과 노출; 없는 주문 404; 브랜드 일괄삭제 후에도 과거 결제결과 보존; 재고부족 409(상태유지); 포인트부족 409(상태유지); 이미 확정된 주문 재확정 409; 브랜드 일괄삭제로 상품 삭제된 뒤 확정 시 404(DRAFT 유지); 실제 SQL 반영 후 저장 실패 시 500 전체 롤백 | HTTP + DB 상태(재고, 지갑 잔액, `orders.status`, `point_bills`/`order_records` 카운트) | 9 tests / 20.0s |
| | CustomerQuery | 〃 | O | 헤더 사용자 본인 주문만 조회; orderId 상세 조회(총액); 없는 주문 404 | HTTP만(+ 응답 바디 값) | 3 tests / 5.7s |
| | AdminQuery | 〃 | O | 전체 구매자 주문을 userId와 함께 조회; 없는 주문 404 | HTTP만 | 2 tests / 3.6s |

### pay (지갑/포인트)

| 테스트 클래스 | 중첩 그룹 | Spring 컨텍스트 | Docker | 검증 시나리오 (DisplayName 요약) | 무엇을 단언하나 | 테스트 수 |
|---|---|---|---|---|---|---|
| [WalletApiE2ETest](../../apps/commerce-api/src/test/java/com/loopers/interfaces/api/pay/controller/WalletApiE2ETest.java) | Charge | 공유 RANDOM_PORT | O | 정상 충전 시 200+잔액증가+CHARGE 기록; 0 이하 충전 400(잔액유지); 잔액 오버플로우 400(잔액유지); 헤더 누락 400; 헤더 형식오류 400; 없는 사용자 404 | HTTP + DB 상태(잔액, `point_bills` 카운트) | 6 |
| | FindBalance | 공유 RANDOM_PORT | O | 저장된 잔액 조회 200; 헤더 누락 400; 헤더 형식오류 400; 없는 사용자 404 | HTTP만 | 4 |

### shopping (좋아요)

| 테스트 클래스 | 중첩 그룹 | Spring 컨텍스트 | Docker | 검증 시나리오 (DisplayName 요약) | 무엇을 단언하나 | 테스트 수 |
|---|---|---|---|---|---|---|
| [LikeApiE2ETest](../../apps/commerce-api/src/test/java/com/loopers/interfaces/api/shopping/controller/LikeApiE2ETest.java) | Register | 공유 RANDOM_PORT | O | 정상 등록 200+관계저장; 중복 등록해도 관계 1개 유지; 헤더 누락 400; 헤더 형식오류 400; 없는 사용자 404; 삭제된 상품 404(관계 미생성) | HTTP + DB 상태(`product_likes` 카운트) | 6 |
| | Cancel | 공유 RANDOM_PORT | O | 존재하는 관계 취소 200+제거; 관계 없어도 200; 브랜드 일괄삭제로 상품이 지워져도 기존 좋아요 취소는 정상 동작 | HTTP + DB 상태 | 3 |
| | FindMyLikes | 공유 RANDOM_PORT | O | 사용자 좋아요 목록 반환; 없는 사용자 404 | HTTP만 | 2 |

### 공통 (example / 계약 분류 / web-support / architecture)

| 테스트 클래스 | 중첩 그룹 | Spring 컨텍스트 | Docker | 검증 시나리오 (DisplayName 요약) | 무엇을 단언하나 | 테스트 수 |
|---|---|---|---|---|---|---|
| [ExampleV1ApiE2ETest](../../apps/commerce-api/src/test/java/com/loopers/interfaces/api/ExampleV1ApiE2ETest.java) | Get | 공유 RANDOM_PORT | O | 존재하는 ID 조회 성공; 숫자 아닌 ID 400; 존재하지 않는 ID 404 | HTTP + 응답 바디 값 | 3 |
| [ContractClassificationTest](../../apps/commerce-api/src/test/java/com/loopers/interfaces/api/ContractClassificationTest.java) | (없음) | 공유 RANDOM_PORT | O | 존재하는 숫자 ID 200/SUCCESS/데이터; 숫자 아닌 ID 400/FAIL; 존재하지 않는 숫자 ID 404/FAIL; 미매핑 URL 404/FAIL | HTTP + `ApiResponse` 메타(result/errorCode) | 4 |
| [ApiControllerAdviceTest](../../apps/commerce-api/src/test/java/com/loopers/interfaces/api/ApiControllerAdviceTest.java) | (없음) | 없음(단위) | X | `DomainException(INVALID_USER_ID)` → 400 매핑; `ApplicationException(USER_NOT_FOUND)` → 404 매핑 | `ApiControllerAdvice.handle` 반환값(상태/메타) | 2 |
| [ApiJacksonConfigTest](../../apps/commerce-api/src/test/java/com/loopers/interfaces/api/support/ApiJacksonConfigTest.java) | (없음) | `@JsonTest` 슬라이스 | X | JSON 정수 → long 역직렬화; 문자열/빈문자열/소수는 정수로 보정하지 않음(3케이스); null은 primitive 기본값으로 보정하지 않음; long 범위 초과 정수 거절 | `ObjectMapper.readValue` 성공/예외 | 6 |
| [XUserIdArgumentResolverTest](../../apps/commerce-api/src/test/java/com/loopers/interfaces/api/support/XUserIdArgumentResolverTest.java) | SupportsParameter | 없음(Mockito 단위) | X | `@XUserId` 붙은 long/Long 파라미터만 지원 | `resolver.supportsParameter` 반환값, `userQueryDao` 미호출 | 1 |
| | ValidHeader | 〃 | X | 존재 사용자 확인 후 양의 long 반환(3케이스); 헤더명 대소문자 무시; 없는 사용자는 `USER_NOT_FOUND`로 거절 | 리졸버 반환값 + `userQueryDao` 호출 검증 | 5 |
| | InvalidHeader | 〃 | X | 누락/빈 헤더는 DB조회 없이 필수입력 오류(2케이스); 형식·범위 오류는 DB조회 없이 거절(8케이스: `abc`,`1.0`," 1","1 "," ","0","-1",overflow) | `CoreException(ErrorType.BAD_REQUEST)` + `userQueryDao` 미호출 | 10 |
| [LayerArchitectureTest](../../apps/commerce-api/src/test/java/com/loopers/architecture/LayerArchitectureTest.java) | (없음, `@ArchTest` 필드 5개) | 없음(ArchUnit 정적 분석) | X | `domain`은 `application`/`interfaces`/`infrastructure`에 의존 금지; `application`은 `interfaces`/`infrastructure`에 의존 금지; `interfaces`는 `infrastructure`에 의존 금지; `infrastructure`는 `interfaces`에 의존 금지; `infrastructure`는 `application.*Service`에 의존 금지 | 클래스 의존 그래프(컴파일된 바이트코드 분석) | 5 |

## 3. 다른 레이어와 겹치는 검증

- **삭제 대상**
  - `OrderApiE2ETest.CustomerQuery#returnsOrderDetail`: 동일한 `GET /api/v1/orders/{id}` 경로가 `Confirm#preservesPaymentResult_afterBrandBulkDelete`와 `Confirm#exposesPaymentResult_inCustomerAndAdminQueries`에서 이미 커버됨.
  - `BrandApiE2ETest#deletesCascade_whenActiveProductExists`: `BrandRepositoryIntegrationTest`의 전파(propagation) 검증 및 `#managesBrand`와 중복.
  - `BrandApiE2ETest#deletesOnlyUndeletedProducts_whenMixedWithAlreadyDeletedProduct`: 이미 삭제된 상품과 미삭제 상품 모두 `deleted = true`를 단언하기 때문에, 이 단언만으로는 "이미 삭제된 상품 상태를 실제로 그대로 유지했는지"와 "다시 삭제 처리해도 결과가 같은지"를 구분할 수 없음(멱등 처리이든 재삭제이든 같은 값이 관찰됨). `BrandTest#keepsAlreadyDeletedProductState_whenBrandDeleted`가 도메인 레벨에서 상태를 더 명확히 구분해 검증.
  - `LikeApiE2ETest.Register#ignoresDuplicateRegistration`, `Cancel#ignoresCancelOfMissingRelation`: `JdbcLikeCommandDaoIntegrationTest`에서 같은 멱등성을 인프라 레벨로 이미 검증.
  - `WalletApiE2ETest.Charge#returnsBadRequest_whenBalanceOverflows`: `WalletServiceTest`/`WalletTest`에서 동일 규칙을 상태 레벨로 검증.
  - `WalletApiE2ETest`의 `Charge`/`FindBalance` 헤더 누락·형식오류 400 (총 4개 테스트): `XUserIdArgumentResolverTest`가 리졸버 단위로 이미 촘촘히 커버. `LikeApiE2ETest`는 헤더 누락/형식오류 각 1개씩만 유지해 컨트롤러 배선(wiring) 확인용으로 남김.
  - `OrderApiE2ETest.Create`, `WalletApiE2ETest.Charge`, `WalletApiE2ETest.FindBalance`의 없는 사용자 404: `XUserIdArgumentResolverTest`(리졸버가 `USER_NOT_FOUND`를 던짐) + `ApiControllerAdviceTest#mapsUserNotFoundToNotFound`(그 예외가 404로 매핑됨)의 조합으로 이미 커버. `LikeApiE2ETest`는 1개만 유지.
  - `OrderApiE2ETest.Confirm#returnsInternalServerError_whenSaveFailsAfterRealSql`: 롤백 검증 부분은 `ConfirmOrderSqlRollbackIntegrationTest`가 더 넓은 시나리오(superset)로 이미 검증. 500 응답 바디 형식만 `ApiControllerAdviceTest`의 "일반 예외 → 500" 케이스로 옮기면, 이 테스트가 요구하는 스파이 빈과 전용 컨텍스트 자체가 사라짐.

- **HTTP 상태 + 에러코드만 남기고 트림**
  - `OrderApiE2ETest.Confirm#returnsNotFound_whenProductDeletedViaBrandBulkDelete`, `#returnsConflict_whenStockIsInsufficient`, `#returnsConflict_whenPointIsInsufficient`, `#confirmsOrder_andReturnsPaymentResult`: 재고/포인트/주문 상태 등 도메인 상태 변화는 `ConfirmOrderIntegrationTest`가 이미 검증하므로, E2E에서는 HTTP 상태 코드와 에러코드만 확인하면 충분.
  - `WalletApiE2ETest.Charge#returnsBadRequest_whenAmountIsNotPositive`: 금액 유효성 규칙 자체는 `WalletServiceTest`가 상태 레벨로 검증하므로, E2E는 400 응답만 확인.

- **고유하게 유지**
  - `ProductApiE2ETest`: QueryDSL 기반 정렬(좋아요순/가격순/최신순)·페이징 조합은 이 클래스에서만 커버.
  - `LikeApiE2ETest.Cancel#cancelsLike_whenProductWasDeletedViaBrandBulkDelete`, `Register#returnsNotFound_whenProductIsDeleted`: 상품 삭제 상태와 좋아요 등록/취소의 상호작용은 다른 레이어에 없음.
  - `OrderApiE2ETest`의 `preservesPaymentResult_afterBrandBulkDelete` / `exposesPaymentResult_inCustomerAndAdminQueries`: 확정된 결제 결과가 브랜드 일괄삭제나 다른 조회 API를 거쳐도 유지·노출되는지는 E2E에서만 확인 가능.
  - `ExampleV1ApiE2ETest` ≈ `ContractClassificationTest`: 둘 다 `/api/v1/examples/*` 경로를 대상으로 거의 동일한 성공/400/404 시나리오를 검증한다(4절 참고).

## 4. 경량화로 바뀌는 것

3절에서 정리한 삭제·트림 대상을 반영하면 다음이 바뀐다.

- **삭제**: `OrderApiE2ETest.CustomerQuery#returnsOrderDetail`, `BrandApiE2ETest#deletesCascade_whenActiveProductExists`, `BrandApiE2ETest#deletesOnlyUndeletedProducts_whenMixedWithAlreadyDeletedProduct`, `LikeApiE2ETest.Register#ignoresDuplicateRegistration`, `LikeApiE2ETest.Cancel#ignoresCancelOfMissingRelation`, `WalletApiE2ETest.Charge#returnsBadRequest_whenBalanceOverflows`, 헤더 400 중복 4개(`WalletApiE2ETest` Charge 2개 + FindBalance 2개), 없는 사용자 404 중복 3개(`OrderApiE2ETest.Create`, `WalletApiE2ETest.Charge`, `WalletApiE2ETest.FindBalance`).
- **트림(단언 축소)**: `OrderApiE2ETest.Confirm`의 상태 전이 계열 4개, `WalletApiE2ETest.Charge#returnsBadRequest_whenAmountIsNotPositive`는 DB 상태 단언을 빼고 HTTP 상태 + 에러코드만 남긴다.
- **`returnsInternalServerError_whenSaveFailsAfterRealSql` 제거**: 롤백 검증은 `ConfirmOrderSqlRollbackIntegrationTest`로 이관하고, 500 응답 바디 포맷 검증만 `ApiControllerAdviceTest`에 "일반(미분류) 예외 → 500" 케이스로 새로 추가한다. 이 변경으로 `OrderApiE2ETest`가 전용 컨텍스트를 유지할 이유였던 `@MockitoSpyBean OrderRepository`가 사라지고, `OrderApiE2ETest`도 다른 E2E와 같은 공유 RANDOM_PORT 컨텍스트를 쓸 수 있게 된다.
- **`@E2ETest` 메타 애노테이션 도입**: 지금은 각 E2E 클래스가 `@SpringBootTest(webEnvironment = RANDOM_PORT)`를 개별 선언하지만, `@IntegrationTest`가 이미 구성해 둔 것과 같은 스파이 빈 집합을 공유하는 `@E2ETest` 메타 애노테이션(RANDOM_PORT + 동일 스파이 세트)을 만들어 위 클래스들이 물리적으로도 하나의 Spring 컨텍스트만 로드하도록 정리한다.
- **example 태그 분리**: `ExampleV1ApiE2ETest`(그리고 `ContractClassificationTest`도 대상 검토)에 `example` 태그를 붙여 일반 `test` 태스크 실행 대상에서 제외하고, `slowTest`(또는 `check`) 태스크에서만 돌게 한다. `ExampleV1ApiE2ETest`와 `ContractClassificationTest`가 사실상 같은 경로(`/api/v1/examples/*`)를 검증하는 중복이라는 점도 이 분리를 뒷받침한다.
- **`ApiControllerAdviceTest` 확장**: 위에서 옮겨온 "일반 예외 → 500" 케이스가 추가되어, `DomainException`/`ApplicationException` 매핑 2개에 더해 총 3개 케이스가 된다.

이 문서는 위 변경들이 아직 적용되기 전, 현재 코드베이스 상태를 기준으로 작성되었다. `@E2ETest`, `example` 태그, `ApiControllerAdviceTest`의 500 케이스는 모두 아직 존재하지 않는 제안 사항이다.
