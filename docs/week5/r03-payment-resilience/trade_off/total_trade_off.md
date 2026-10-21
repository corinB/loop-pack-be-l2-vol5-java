# R03 설계 질문·결정 목록

상태: **일부 설계 논의 중·구현 전**. R01 진행 중 복구 정책을 먼저 논의하며, R01/R02의 실제 호출 경로와 검증을 확인한 뒤 상세 구현 계획을 확정한다.

## 확정한 제약

- 회복 도구는 기존 거래 조회를 제어한다. create 자동 재시도는 허용하지 않는다.
- 조회 실패·회로 차단은 결제의 최종 실패 증거가 아니며, fallback은 저장 상태와 확인 불가를 표현한다.
- 처리 중·결과 미확인에서는 차감 상태를 유지하고, 검증한 확정 실패만 한 번 복구한다.
- 실제 호출 횟수·외부 거래·DB 결과로 검증하며 미실행·skip을 완료로 세지 않는다.
- 사용자 결정으로 최초 조회 후 추가 재조회 최대 3회, 간격 1초·3초·10초와 지터 ±20%를 적용한다. 실패/수동 전환 기준도 01에서 확정했다.
- 정상 실행은 @Retryable + DB 진행 기록을 사용한다. 장애/재시작 뒤 자동 재개는 담당자 수동 대사로 변경했다. 자동 만료/회수·주기 조회 스케줄러는 도입하지 않는다.
- 조회 직전 해당 회차 사용을 DB에 commit한다. 기록 뒤 HTTP 전에 종료돼도 소비를 취소하지 않고 남은 예산을 보존한다. 재시작 자동 재개는 하지 않는다.
- [학습용 최소 보완](../../r01-pg-payment-foundation/trade_off/10-learning-scope-completion.md)으로 관찰 먼저 commit/업무 TX 분리·수동 실행 ID 교체·commerce 지터/예약 정보 영속화 제외를 확정했다. PG의 DB 예정 거래 주기 조회는 commerce 재시작 자동 조회와 별개다.

## 앞으로 논의할 질문

| 번호 | 주제 | 확정할 내용 |
|---|---|---|
| [01](01-reconciliation-retry-policy.md) | 자동 재확인·수동 전환 | 최초+추가 3회·1/3/10초·지터 ±20%·실패 분류 확정. 하위 timeout은 R01 연결 1초/풀 대기 1초/응답 2초 채택. 총 예산·구체적인 예외 매핑·내부 반영 예산은 미정 |
| 02 | circuit breaker | 적용 경로, 표본/실패·지연 조건, 업무 거절 제외, OPEN 대기·탐색 수·실패 표본 단위 |
| 03 | 연결 방식·fallback | @Retryable 방향 채택. 구체적인 의존성 버전·프록시 적용·회로 연결·확인 불가 응답과 관측 지점은 미정 |
| 04 | 결정적 장애 실험 | 가짜 시간/fixture·동시성 제어, 내부 저장 실패·재시작 실험, 유한 대기·자원 정리 |
| 05 | 결과·기술 글 | 실제 증거 보존 위치, 대표 결과·남은 미확인 한계, 기술 글 주제와 제출 정리 |
| [06](06-reconciliation-execution.md) | 자동 재확인 저장·실행 | 수정 C: 정상 @Retryable + DB 기록, 중단 시 운영 담당자 수동 대사. 재시작 자동 재개는 대체됨. 실행기·동시 조율은 미정 |
| [07](07-retry-attempt-accounting.md) | 횟수 소비·진행 중 종료 | A: 조회 직전 DB 기록/commit 채택. 미실행 회차가 소비될 수 있는 비용 수용. 회로 OPEN·동시 실행 조율은 미정 |

01·06·07과 R01의 [HTTP 설정](../../r01-pg-payment-foundation/trade_off/03-http-client-settings.md)·[관찰 결과값 반환](../../r01-pg-payment-foundation/trade_off/04-adapter-fixture.md)은 확정했다. 결과값 분류를 재조회/회로에 연결하는 상세 방식·전체 예산·회로 설정·의존성 버전·구체적인 응답 필드·관측 지표·글 주제는 미정이다.
R01의 [상태 표현](../../r01-pg-payment-foundation/trade_off/05-payment-attempt-state.md)은 확인한 결과와 복구 진행을 나누기로 확정했다.
R01의 [복구 진행 기록 저장 경계](../../r01-pg-payment-foundation/trade_off/06-recovery-progress-storage.md)는 별도 pay 작업 모델로 확정했다.
[재고 차감·복구 기록](../../r01-pg-payment-foundation/trade_off/07-stock-adjustment-boundary.md)은 mall의 별도 모델·같은 TX 복구로 후속 확정했다.
R02의 [파사드·TX 경계](../../r02-pg-order-payment/trade_off/01-facade-transaction-boundary.md)는 A안, R01의 [실행 권한 확보](../../r01-pg-payment-foundation/trade_off/08-recovery-claim-coordination.md)는 멱등성 키 + 조건부 상태 갱신 + 실행 식별자의 C로 변경했다. 자동 만료·회수는 보류하고 [중단 작업 조회·운영 담당자 수동 대사](../../r02-pg-order-payment/trade_off/06-interrupted-payment-reconcile.md) 방향을 채택했다.
결정 후 번호별 설계 문서에 대안·근거·비용·한계를 기록하고 계획을 채운다.

[요구사항](../requirement.md) · [계획 틀](../plan.md) · [전체 요구사항](../../total_requirement.md)
