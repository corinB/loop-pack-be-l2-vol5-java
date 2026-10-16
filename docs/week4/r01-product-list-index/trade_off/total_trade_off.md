# R01 트레이드오프

[요구사항](../requirement.md) · [계획](../plan.md) · [전체 요구사항](../../total_requirement.md)

2026-10-16 대화에서 확인한 사용자 결정과 후속 설계 항목을 구분한다. 결정은 구현·실험 완료를 뜻하지 않는다.

## 선택 현황

1. [인덱스 구성](01-index-layout.md): **기존 4개 유지 + 전체 최신순·가격순용 2개 추가**. 기존 브랜드별 인덱스를 전체용으로 교체하거나 모든 정렬을 하나의 인덱스로 합치지 않는다.
2. [전후 측정](02-before-after-benchmark.md): **상품 100만 개, 3종 요청, 동시 1명·10명, 사용자 JMeter 실행, Codex 도구 준비·실행 계획 분석**. JMeter 초보자 안내도 포함한다.
3. [전용 프로필의 SQL seed](03-profile-sql-seed.md): **`benchmark-seed` 전용 프로필 + ApplicationRunner + ResourceDatabasePopulator, 전체 적재·검증 한 TX, 검증 후 종료**. 브랜드·사용자 각 1,000개와 고정 분포를 사용한다.

## 남은 측정 설계

- warm-up·ramp-up·측정 횟수/시간·반복 수, 요청 timeout과 결과 파일 보존 방식.
- JMeter 설치 경로와 실제 실행 명령. seed·기준선 실행 절차는 현재 구현에서 재현 가능하게 남긴다.

현재 요청에서는 seed·변경 전 실행 계획까지 준비한다. JMeter 세부 설정은 부하 측정 준비 전에 구체화한다.
