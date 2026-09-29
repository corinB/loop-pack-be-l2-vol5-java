# R03 트레이드오프

[요구사항](../requirement.md) · [구현 계획](../plan.md) · [전체 요구사항](../../total_requirement.md) · [주제 문서 템플릿](../../_template.md)

**굵게 = 채택** · ~~취소선 = 미채택~~ · `[검토 전]` / `[검토 중]` = 미확정 · `[보류]` = 필요할 때 재검토

좋아요 등록·취소(1~3)와 조회 전환(4, 6)은 결정했고 구현 계획에 반영했다. 1~3은 구현 후 조정이 있었다. 집계(5)는 문답 전이다.

## 선택 현황

1. [좋아요의 도메인 구조](01-like-aggregate.md): **독립 Like 애그리거트 + UseCase(`execute(LikeCommand.*)` 형식으로 조정)** / ~~User 애그리거트 + 사용자 행 잠금~~ / ~~User 애그리거트, 잠금 없음~~ / ~~DAO 유지~~
2. [좋아요 중복 등록·취소 처리](02-like-duplicate.md): **Repository `save`의 HQL `insert … on conflict do nothing`, 취소는 JPQL delete 1회, 멱등 200 유지** / ~~사전 확인 + 경쟁 시 500~~ / ~~트랜잭션 밖에서 위반을 성공으로 변환~~ / ~~조회 후 삭제~~
3. [좋아요 등록 시 활성 상품 확인](03-like-product-check.md): **Service가 `findById` 후 삭제 여부를 확인해 `PRODUCT_NOT_FOUND`(구현 후 `ensureActive`에서 되돌림), 같은 트랜잭션, 잠금 없음** / ~~`ensureActive`의 `DELETED_PRODUCT` 허용~~ / ~~Repository 조건절~~ / ~~도메인 `LikePolicy`~~ / ~~`Like.create(userId, product)`~~ / ~~상품 공유 잠금~~
4. [조회 DAO의 QueryDSL 전환 방식](04-query-conversion.md): **`Projections.constructor` + 필요할 때만 Row, 주문 목록 2단계 유지, `QueryDsl*QueryDao`, 상품 쓰기 응답은 `findAdminProduct`로 다시 읽고 좋아요 수 DAO 제거, 기존 테스트 이름만 변경, 컨텍스트별 커밋** / ~~`@QueryProjection`~~ / ~~fetch join~~ / ~~엔티티 + batch fetch~~ / ~~Controller가 좋아요 수만 붙이기~~
5. 좋아요 집계 방식: 전체 재집계 유지(쿼리 개선) / 변경된 상품만 재집계 / 등록·취소 시 즉시 ±1 반영 `[검토 전]`
6. [테스트 코드의 JdbcClient 허용](04-query-conversion.md#테스트-코드의-jdbcclient-허용-6번): **테스트의 데이터 준비·검증에서는 허용** / ~~테스트도 JPA·QueryDSL로~~ / ~~새 테스트만 금지~~

주제를 시작할 때 템플릿을 복사해 `번호-주제.md` 형식으로 만들고 이 목록에 연결한다.

## 구현 계획으로 이어갈 것

1~3번은 [구현 계획](../plan.md)의 좋아요 등록·취소 절(커밋 1~5, 조정 커밋 6)로, 4·6번은 조회 전환 절로 구체화했다.
HQL on conflict는 MySQL에서 `insert … on duplicate key update user_id = product_likes.user_id`로 바뀌어 기대대로 동작했다(2번 남은 사항 해소).
5번의 결정에 따라 `LikeRepository.save`의 반환 형태가 바뀔 수 있다(1번 남은 사항).
