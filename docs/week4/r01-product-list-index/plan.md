# R01 구현·측정 계획

[요구사항](requirement.md) · [트레이드오프](trade_off/total_trade_off.md) · [JMeter 안내](jmeter-guide.md) · [전체 요구사항](../total_requirement.md)

작업 브랜치: `volume-4/r01-product-list-index` · PR 대상: `volume-4/main`

상태: **브랜치·문서 준비 단계**. 아래 순서는 후속 작업 계획이며 실행 결과가 아니다.
측정 세부 설정의 미정 항목은 [선택 현황](trade_off/total_trade_off.md)에 남겼다. 후속 구현 전에 확정한다.

## 1. 현재 요청의 완료 기준

- [x] `volume-4/main`에서 이번 PR 작업 브랜치를 만든다.
- [x] 기존 4개 유지·추가 2개·상품 100만 개·요청 3종·동시 1명/10명·사용자 JMeter 실행 결정을 기록한다.
- [x] 실행 계획을 Codex가 수집·분석하는 범위와 초보자 JMeter 안내를 기록한다.
- [x] 현재 DB·도구 확인 사실과 아직 구현·측정하지 않은 작업을 구분한다.
- [x] 문서 링크·상태·공백과 코드/DB 변경 여부를 최종 확인한다.

## 2. 후속 작업 순서

| 순서 | 작업 | 검증·산출물 |
|---|---|---|
| 1 | 측정 세부 조건 확정 | seed 분포·고정 브랜드·warm-up·ramp-up·시간/횟수·반복 수·timeout·결과 경로 |
| 2 | 격리 실험 환경·기준선 준비 | 현재 전체 모델의 스키마·100만 seed, 기존 4개 확인, 정확한 건수·분포·좋아요 집계 일치 |
| 3 | 변경 전 실제 SQL·계획 확보 | 3종 요청의 목록·COUNT SQL, 요청당 SQL 수, EXPLAIN·EXPLAIN ANALYZE·응답 기준값 |
| 4 | JMeter 파일·초보자 절차 준비 | 3종 × 1명/10명 파라미터, 1건 확인·warm-up·실측·리포트 저장 명령과 smoke 확인 |
| 5 | 사용자 변경 전 측정 | 데이터·기존 4개 인덱스 상태를 유지한 JTL·HTML·환경·표본 기록 |
| 6 | 인덱스 2개 구현·적용 | 엔티티 `@Index` 2개와 적용 SQL, 실제 SHOW INDEX로 기존 4개+추가 2개 확인 |
| 7 | 변경 후 비교 | 동일 조건의 계획·응답 재수집과 사용자 JMeter 측정, 결과 일치·비용 변화 분석 |
| 8 | 회귀·결과 정리 | 관련 테스트·Checkstyle·ArchUnit·check 실행 결과, `result.md`·전체 요구사항 갱신 |

현재 요청에서는 2~8단계의 코드·DDL·seed·JMX 작성과 실행을 시작하지 않는다.
인덱스를 먼저 적용해 변경 전 측정 기회를 잃지 않도록 기준선 수집 순서를 지킨다.

## 3. 구현·실험 때 유지할 경계

- 도메인·Controller·QueryDSL Projection·COUNT 정책·정렬·페이지 API를 함께 바꾸지 않는다.
- 앱을 띄울 때 현재 기본 local 설정을 그대로 사용해 데이터를 재생성하지 않도록 전용 DB URL과 `ddl-auto`를 명시한다.
- 실제 datasource 키는 `datasource.mysql-jpa.main.jdbc-url`이다. URL·버전·커밋·인덱스·설정을 측정 결과와 함께 남긴다.
- 현재 앱은 사용자·지갑 초기화 등 다른 테이블도 사용하므로 과거 R04의 brands/products 부분 스키마만 재사용하지 않는다.
- seed의 실제 사용자–상품 좋아요 관계와 `products.like_count`를 맞춘다. 측정 중 쓰기·집계·캐시 비용을 전후 동일하게 고정한다.
- 기존 공유 DB·인덱스·컨테이너를 지우지 않는다. 이 작업의 격리 실험 자원과 추가 인덱스만 관리한다.
- 변경 후 SQL 수가 같더라도 읽은 행·정렬·지연이 줄었는지 확인한다. COUNT가 남는 비용도 별도로 설명한다.
- 측정 파일은 단계·시나리오·동시 부하·반복 회차가 구분되고 기존 결과를 덮어쓰지 않게 준비한다.

## 4. 검사 계획

문서만 바꾸는 현재 단계에서는 링크·공백·Git 상태를 확인한다. 기능 테스트와 JMeter를 실행했다고 기록하지 않는다.
후속 인덱스 변경에서는 기존 상품 목록 DAO·API 회귀를 먼저 실행하고, 완료 시 모듈 check로 Checkstyle·ArchUnit도 확인한다.

```powershell
.\gradlew.bat :apps:commerce-api:test --tests '*ProductQuery*Test' --tests '*ProductApiE2ETest'
.\gradlew.bat :apps:commerce-api:check
```

테스트 수·실패·skip·종료 결과를 확인한다. JMX는 소량 HTTP 요청으로 실제 결과까지 확인한 뒤 사용자 부하 측정에 넘긴다.
사용자 JMeter 원본과 SQL 계획이 모이기 전에는 성능 개선 검증을 완료로 표시하지 않는다.
