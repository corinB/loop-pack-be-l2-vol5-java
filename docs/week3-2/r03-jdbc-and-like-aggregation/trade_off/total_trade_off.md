# R03 트레이드오프

[요구사항](../requirement.md) · [구현 계획](../plan.md) · [전체 요구사항](../../total_requirement.md) · [주제 문서 템플릿](../../_template.md)

**굵게 = 채택** · ~~취소선 = 미채택~~ · `[검토 전]` / `[검토 중]` = 미확정 · `[보류]` = 필요할 때 재검토

좋아요 등록·취소 주제(1~3)는 결정했고 구현 계획에 반영했다. 조회 전환·집계 주제는 문답 전이다.

## 선택 현황

1. [좋아요의 도메인 구조](01-like-aggregate.md): **독립 Like 애그리거트 + UseCase** / ~~User 애그리거트 + 사용자 행 잠금~~ / ~~User 애그리거트, 잠금 없음~~ / ~~DAO 유지~~
2. [좋아요 중복 등록·취소 처리](02-like-duplicate.md): **Repository `save`의 HQL `insert … on conflict do nothing`, 취소는 JPQL delete 1회, 멱등 200 유지** / ~~사전 확인 + 경쟁 시 500~~ / ~~트랜잭션 밖에서 위반을 성공으로 변환~~ / ~~조회 후 삭제~~
3. [좋아요 등록 시 활성 상품 확인](03-like-product-check.md): **Service가 `findById` + `ensureActive`, 같은 트랜잭션, 잠금 없음** / ~~Repository 조건절~~ / ~~도메인 `LikePolicy`~~ / ~~`Like.create(userId, product)`~~ / ~~상품 공유 잠금~~
4. 조회 DAO의 QueryDSL 전환 방식: Projection 전용 규칙, 주문 목록 2단계 조회 유지 `[검토 전]`
5. 좋아요 집계 방식: 전체 재집계 유지(쿼리 개선) / 변경된 상품만 재집계 / 등록·취소 시 즉시 ±1 반영 `[검토 전]`
6. 테스트 코드의 데이터 준비·검증에서 JdbcClient 허용 여부 `[검토 전]`

주제를 시작할 때 템플릿을 복사해 `번호-주제.md` 형식으로 만들고 이 목록에 연결한다.

## 구현 계획으로 이어갈 것

1~3번은 [구현 계획](../plan.md)의 좋아요 등록·취소 절(커밋 1~5)로 구체화했다.
HQL on conflict가 MySQL에서 기대대로 동작하지 않으면 구현을 멈추고 대체안을 다시 문답한다(2번 남은 사항).
5번의 결정에 따라 `LikeRepository.save`의 반환 형태가 바뀔 수 있다(1번 남은 사항).
