# R02 상품 상세 로컬 캐시 요구사항

## 목표와 범위

- 기준은 PR #22가 병합된 `origin/volume-4/main`의 `3786c0d`다. 작업 브랜치는 `volume-4/r02-product-detail-local-cache`다.
- 고객용 `GET /api/v1/products/{productId}`의 전체 `ProductDetailView`를 Caffeine 로컬 캐시에 저장한다.
- 구현 방식은 사용자가 선택한 Caffeine이며 교체 정책은 **W-TinyLFU**다. 순수 LRU 직접 구현으로 기록하지 않는다.
- 목록·관리자 조회·주문 판단은 기존 DB 경로를 유지한다. 공개 API·응답·오류·QueryDao 계약은 바꾸지 않는다.
- 같은 JVM의 기존 쓰기 경로를 대상으로 변경 후 순차 조회의 최신성을 검증한다. 다중 인스턴스·외부 SQL 전파와 Redis 장애·복구는 후속 과제다.

## 캐시와 기존 계약

- 키는 상품 ID, 값은 불변 상세 View다. 값은 `productId/name/price/brand/likeCount/description/stock`을 포함한다.
- 기본 설정은 `query.product-detail-cache.enabled=false`, `maximum-size=1000`, `ttl=30s`다. 활성화할 때 용량과 TTL이 양수가 아니면 기동을 거절한다.
- `expireAfterWrite`로 저장·교체 후 TTL을 계산한다. 읽기는 TTL을 연장하지 않는다. 미존재·삭제·예외는 저장하지 않는다.
- 상품과 브랜드가 모두 활성 상태여야 한다. 잘못된 ID는 기존 400, 미존재·삭제 상세는 기존 404를 유지한다.
- hit에서 원본 SQL이 없어야 하고 OFF/miss는 기존 QueryDSL SQL을 사용해야 한다. 실제 SQL 수는 실행 후에만 완료로 기록한다.
- 이미 트랜잭션이 있으면 캐시 조회와 저장을 모두 우회한다. 캐시 경계에는 TX를 걸지 않고 miss의 원본 DAO 프록시에만 기존 readOnly TX를 적용한다.

## 변경과 경쟁 조건

| 쓰기 | 성공 commit 뒤 무효화 |
|---|---|
| 상품 수정·삭제·재고 설정 | 해당 상품 |
| 주문 확정 재고 차감 | 전체 주문 TX가 commit된 뒤 해당 상품들 |
| 브랜드 수정·삭제 | 캐시된 DTO의 브랜드 ID로 소속 항목 제거 |
| 좋아요 관계 등록·취소 | 실제 관계 변경의 기존 이벤트로 해당 상품 제거 |
| 좋아요 delta flush | 실제 집계 DB 변경 commit 뒤 해당 상품들 제거 |
| 전체 재집계 | 상세 캐시 전체 제거 |

생성은 미존재 결과를 캐시하지 않아 추가 무효화가 필요 없다. rollback에서는 무효화하지 않는다.
좋아요 관계 commit과 집계 반영 commit은 다른 경계이며 기존 집계 지연을 유지한다.

캐시 조회·세대 확인·조건부 저장·무효화는 같은 짧은 잠금으로 보호하고 DB 조회는 잠금 밖에서 수행한다.
대상이 캐시에 없어도 모든 무효화에서 세대를 증가시킨다. 조회 중 세대가 바뀌면 옛 결과를 저장하지 않는다.
변경과 겹친 기존 요청의 DB 읽기 결과는 반환할 수 있으나 후속 요청에 재사용하지 않는다.
동시 miss의 중복 DB 조회는 허용하며 이번 단계에서 요청 병합은 구현하지 않는다.

## 완료 조건

- [ ] 기능·설정 단위 테스트: hit/miss/OFF, TTL 읽기 비연장, 미존재·예외 미저장, 용량, 잘못된 설정, 기존 TX 우회.
- [ ] 실제 MySQL 통합 테스트: 모든 영향 쓰기의 commit/rollback, 다른 상품·브랜드 보존, 늦은 채움 순서 제어.
- [ ] 실제 HTTP 응답 일치·400/404와 OFF/miss/hit SQL 수 확인.
- [ ] 모듈 check 및 Checkstyle·ArchUnit 통과.
- [ ] 동일 새 JAR의 OFF/ON × 1명/10명 × 2회 JMeter CLI 본 측정 8개 완료.
- [ ] 워밍업 20초/본 측정 60초/램프업 5초, 응답 timeout 30초 조건과 실제 표본·오류율·분위수 보존.
- [ ] 현재 100만 상품·보조 인덱스 6개·버퍼 풀 128MiB·주요 DB 설정 유지.
- [ ] 사용자 GUI JMX 수정 보존·커밋 제외, 재현 도구·원본 해시·비교 한계와 결과 기록.

상위 기준은 [4주차 전체 요구사항](../total_requirement.md), 결정 근거는 [설계 판단](trade_off/total_trade_off.md), 작업 순서는 [계획](plan.md)이다.
