# 좋아요 등록 시 활성 상품 확인

[← 전체 선택 현황](total_trade_off.md)

## 판단할 문제

지금은 `LikeController`가 `existsActiveProduct`(readOnly 트랜잭션 ①)와 `register`(트랜잭션 ②)를 따로 호출한다.
①과 ② 사이에 상품이 삭제되면 삭제된 상품에 좋아요 행이 생긴다.
그래도 내 좋아요 목록은 `deleted = false`로 거르고, 취소도 되므로 실제 피해는 쓰이지 않는 행 하나다.
UseCase를 도입하면서 확인을 어디서, 어떤 방식으로 할지 정해야 한다.

> **채택 — Service가 `productRepository.findById` 후 `product.ensureActive()`, 같은 트랜잭션, 잠금 없음**
>
> 상품이 없으면 `ApplicationException(PRODUCT_NOT_FOUND)`, 삭제된 상품이면 `DomainException(DELETED_PRODUCT)`를 던지며 둘 다 404로 응답한다.
> "활성 상품" 규칙은 `Product.ensureActive` 한 곳에만 둔다. `OrderService`와 같은 방식이다.
> 대신 EXISTS보다 조금 무거운 PK 조회와 도메인 변환 비용, 잠금이 없어 남는 삭제 경쟁을 감수한다.

## 흐름 비교

```mermaid
flowchart TB
    subgraph A["채택 · findById + ensureActive"]
        direction LR
        A1["LikeService"] --> A2["ProductRepository.findById"] --> A3["product.ensureActive"] --> A4["Like 저장"]
    end
    subgraph B["미채택 · Repository 조건절"]
        direction LR
        B1["LikeService"] --> B2["existsActiveById<br/>(deleted = false)"] --> B3["Like 저장"]
    end
    subgraph C["미채택 · 상품 공유 잠금"]
        direction LR
        C1["LikeService"] --> C2["상품 FOR SHARE"] --> C3["Like 저장"]
    end
    A ~~~ B ~~~ C
    style A fill:#e8f5e9,stroke:#2e7d32
    style B fill:#f5f5f5,stroke:#9e9e9e
    style C fill:#f5f5f5,stroke:#9e9e9e
```

## 장단점 비교

| 기준 | findById + ensureActive | Repository 조건절 (`existsByIdAndDeletedFalse`) | 도메인 `LikePolicy` | `Like.create(userId, product)` | 상품 공유 잠금 |
|---|---|---|---|---|---|
| 비용 | PK 조회 + 도메인 변환 | EXISTS 1회 | PK 조회 + 변환 | PK 조회 + 변환 | 잠금 조회 |
| 규칙 위치 | `Product.ensureActive` 한 곳 | 도메인과 쿼리 두 곳 | 한 곳 (Policy가 위임) | 한 곳 | 한 곳 |
| 새 코드 | 없음 | Repository 메서드 + JPA 메서드 | 한 줄짜리 Policy 클래스 | Like 팩토리 변경 | 잠금 메서드 |
| 컨텍스트 의존 | application이 mall 도메인 사용 (`OrderService` 선례) | 같음 | shopping policy → mall 모델 (`OrderConfirmationPolicy` 선례) | shopping **모델**이 mall 모델에 의존 (선례 없음) | 같음 |
| 삭제 경쟁 | 허용 | 허용 | 허용 | 허용 | 차단, 대신 주문 확정·재고 설정과 경합 |

## 옵션별 판단

> **채택 — findById + ensureActive:** 사용자 판단이다. 규칙이 한 곳에 남고 새 클래스가 필요 없다. 비용 차이는 PK 한 행 조회 수준이다. `DELETED_PRODUCT`는 `ApiErrorMapper`에서 이미 404로 바뀌고 메시지도 같으므로, "삭제된 상품이면 404"라는 기존 E2E 기대값이 유지된다.

> **미채택 — Repository 조건절:** 가장 가볍지만, 사용자가 원하지 않았다. `ProductRepository.existsActive`를 추가하고 구현체에서 `existsByIdAndDeletedFalse`를 쓰는 방식이다. "활성"의 정의가 도메인과 쿼리 두 곳에 생긴다.

> **미채택 — 도메인 `LikePolicy`:** 규칙에 이름이 붙고 단위 테스트가 쉬워지지만, `ensureActive` 한 줄을 감싸는 클래스가 하나 늘어난다.

> **미채택 — `Like.create(userId, product)`:** 기존 코드에서 컨텍스트를 넘는 것은 Policy뿐이고, 모델은 원시값만 받는다(`OrderItem.create`). 이 관례를 깨고 shopping 모델이 mall 모델에 의존하게 된다.

> **미채택 — 상품 공유 잠금:** 쓰이지 않는 행 하나를 막으려고 인기 상품의 좋아요와 주문 확정이 서로 기다리게 만드는 것은 비용이 크다.
