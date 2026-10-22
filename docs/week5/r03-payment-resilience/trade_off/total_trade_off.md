# R03 설계 질문·결정 목록

상태: **R02 병합 후 PR03 설계 문답 중·구현 전**. 2026-10-22 최신 기준 `42c22c2`에서 `volume-5/r03-payment-resilience`를 생성했다. R02 공통 업무 반영·수동 대사·callback·작업 CAS를 재사용하며 남은 문답 뒤 상세 구현 계획을 확정한다.

## 확정한 제약

- 회복 도구는 기존 거래 조회를 제어한다. create 자동 재시도는 허용하지 않는다.
- 조회 실패·회로 차단은 결제의 최종 실패 증거가 아니며, fallback은 저장 상태와 확인 불가를 표현한다.
- 처리 중·결과 미확인에서는 차감 상태를 유지하고, 검증한 확정 실패만 한 번 복구한다.
- 실제 호출 횟수·외부 거래·DB 결과로 검증하며 미실행·skip을 완료로 세지 않는다.
- 사용자 결정으로 최초 조회 후 추가 재조회 최대 3회, 간격 1초·3초·10초와 지터 ±20%를 적용한다. 실패/수동 전환 기준도 01에서 확정했다.
- 정상 실행은 @Retryable + DB 진행 기록을 사용한다. 장애/재시작 뒤 자동 재개는 담당자 수동 대사로 변경했다. 자동 만료/회수·주기 조회 스케줄러는 도입하지 않는다.
- 조회 직전 해당 회차 사용을 DB에 commit한다. 기록 뒤 HTTP 전에 종료돼도 소비를 취소하지 않고 남은 예산을 보존한다. 재시작 자동 재개는 하지 않는다.
- 자동·수동 대사·callback PG 조회는 하나의 회로를 공유한다. 생성 POST는 적용 범위 밖이고 저장 종결 근거의 내부 재적용은 OPEN에도 가능하다.
- 자동 작업은 회로 차단 시 즉시 수동 전환한다. 먼저 commit한 회차는 소비한 채 유지하고 실제 HTTP 0회·회로 표본 제외와 구분한다.
- 최초 자동 조회는 작업 시작 후 별도 대기 없이 실행한다. 실행기가 명확히 접수 거절하면 100ms 뒤 전달만 1회 재시도하며 재거절은 수동 전환이다. 전달 자체는 PG 조회/생성·자동 회차 소비가 아니다.
- 회로 실패는 timeout·통신 오류·5xx·408·429·잘못된 정상 응답이다. 검증한 SUCCESS/FAILED/PENDING과 계약상 거래 없음은 정상, 다른 4xx·식별 충돌·우리 DB/접수 실패·회로 차단은 집계 제외다. 상세 경로/처리 표는 [02](02-lookup-circuit-breaker.md)를 따른다.
- [학습용 최소 보완](../../r01-pg-payment-foundation/trade_off/10-learning-scope-completion.md)으로 관찰 먼저 commit/업무 TX 분리·수동 실행 ID 교체·commerce 지터/예약 정보 영속화 제외를 확정했다. PG의 DB 예정 거래 주기 조회는 commerce 재시작 자동 조회와 별개다.

## 앞으로 논의할 질문

| 번호 | 주제 | 확정할 내용 |
|---|---|---|
| [01](01-reconciliation-retry-policy.md) | 자동 재확인·수동 전환 | 최초+추가 3회·1/3/10초·지터 ±20%·실패 분류 확정. 하위 timeout은 R01 연결 1초/풀 대기 1초/응답 2초 채택. 총 예산·구체적인 예외 매핑·내부 반영 예산은 미정 |
| [02](02-lookup-circuit-breaker.md) | circuit breaker·카운팅 | 공유 조회 회로·차단 시 즉시 수동·집계 분류 확정. 집계 창/최소 표본·실패/느림 수치·OPEN 대기·탐색 수는 미정 |
| 03 | 연결 방식·fallback | @Retryable 방향 채택. 구체적인 의존성 버전·프록시 적용·회로 연결·확인 불가 응답과 관측 지점은 미정 |
| 04 | 결정적 장애 실험 | 가짜 시간/fixture·동시성 제어, 내부 저장 실패·재시작 실험, 유한 대기·자원 정리 |
| 05 | 결과·기술 글 | 실제 증거 보존 위치, 대표 결과·남은 미확인 한계, 기술 글 주제와 제출 정리 |
| [06](06-reconciliation-execution.md) | 자동 재확인 저장·실행 | 정상 @Retryable + DB 기록·중단 시 수동 대사. 첫 조회 즉시·명확한 접수 거절은 100ms 뒤 전달만 1회 재시도 확정. 자동 시작 대상·실행기/선점 상세는 미정 |
| [07](07-retry-attempt-accounting.md) | 횟수 소비·진행 중 종료 | 조회 직전 commit. 회로 차단도 확보한 자동 회차 소비·즉시 수동, 수동/callback은 자동 회차 소비 없음. 동시 실행 상세는 미정 |

01·02·06·07과 R01의 [HTTP 설정](../../r01-pg-payment-foundation/trade_off/03-http-client-settings.md)·[관찰 결과값 반환](../../r01-pg-payment-foundation/trade_off/04-adapter-fixture.md)에서 위 정책을 확정했다. 다음은 회로 집계 창·최소 표본·차단/느림 수치다. 결과값을 재조회/회로에 연결하는 상세 방식·전체 예산·의존성 버전·구체적인 응답 필드·관측 지표·글 주제도 아직 미정이다.
R01의 [상태 표현](../../r01-pg-payment-foundation/trade_off/05-payment-attempt-state.md)은 확인한 결과와 복구 진행을 나누기로 확정했다.
R01의 [복구 진행 기록 저장 경계](../../r01-pg-payment-foundation/trade_off/06-recovery-progress-storage.md)는 별도 pay 작업 모델로 확정했다.
[재고 차감·복구 기록](../../r01-pg-payment-foundation/trade_off/07-stock-adjustment-boundary.md)은 mall의 별도 모델·같은 TX 복구로 후속 확정했다.
R02의 [파사드·TX 경계](../../r02-pg-order-payment/trade_off/01-facade-transaction-boundary.md)는 A안, R01의 [실행 권한 확보](../../r01-pg-payment-foundation/trade_off/08-recovery-claim-coordination.md)는 멱등성 키 + 조건부 상태 갱신 + 실행 식별자의 C로 변경했다. 자동 만료·회수는 보류하고 [중단 작업 조회·운영 담당자 수동 대사](../../r02-pg-order-payment/trade_off/06-interrupted-payment-reconcile.md) 방향을 채택했다.
결정 후 번호별 설계 문서에 대안·근거·비용·한계를 기록하고 계획을 채운다.

[요구사항](../requirement.md) · [계획 틀](../plan.md) · [전체 요구사항](../../total_requirement.md)
