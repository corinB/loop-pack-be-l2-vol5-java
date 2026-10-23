# 5주차 다음 작업 인계

2026-10-23 R03 로컬 구현과 최종 검증까지 완료했다. 다음 작업은 사용자 요청에 따른 원격 게시·PR 생성이다. 검증을 다시 처음부터 시작하지 않는다.

## 현재 위치와 먼저 읽을 문서

- 브랜치: `volume-5/r03-payment-resilience`, PR 대상: `volume-5/main`.
- R01 [PR #24](https://github.com/corinB/loop-pack-be-l2-vol5-java/pull/24), R02 [PR #25](https://github.com/corinB/loop-pack-be-l2-vol5-java/pull/25)는 사용자 병합 완료. R03은 R02 병합 `42c22c2`에서 시작했다.
- R03 최종 기능 커밋: `ae46776`. 이후 변경은 검증 결과·문서다. 아직 R03 원격 PR은 없다.
- [R03 결과](r03-payment-resilience/result.md) → [운영 확인](r03-payment-resilience/operations.md) → [재현 방법](r03-payment-resilience/verification.md) → [기능 리뷰](r03-payment-resilience/review.md) 순서로 읽는다. [기술 글](r03-payment-resilience/technical-article.md)·[상태 문서](states/total.md)도 정리했다.
- 사용자 변경 `docs/week4/r01-product-list-index/jmeter/product-list-ui.jmx`는 그대로 남긴다. 수정·staging·되돌리기 금지.

## 완료한 검증

- 필수 전체 check: **509건(PG25/commerce484), 실패·오류·건너뛰기 0**, 종료 코드 0. Checkstyle·ArchUnit과 기존 slow/example도 포함한다. 앞선 기능별 검사 수와 중복 합산하지 않는다.
- 독립 PG/commerce JVM의 **새 주문 55~68, 14건**: 응답 유실 후 자동 성공/실패·4회 소진·35초 DB 대기 뒤 시간 재확인·큐 만료·실행기 접수 거절·느린 조회의 OPEN/탐색/복귀·PG 재시작·성공 관찰 후 업무 rollback·회차 commit 후 HTTP 전 commerce 강제 종료/재시작.
- 최종 효과: 생성 POST14·PG 거래14·APPLIED14·CONFIRMED13·PAYMENT_FAILED1·주문 기록13·CONSUMED13·RESTORED1·포인트 USE0. 성공 재고3/실패 재고5. 중복 거래·주문 확정·재고 반환 없음.
- commerce 재시작 뒤 DB 사용 회차1·실행 ID·최근 시각이 유지됐다. GET/동일 confirm은 자동 조회를 재개하지 않고 명시적 수동 대사만 새 실행으로 인계했다.
- PG 성공 뒤 업무 오류는 SUCCESS 사실을 보존했다. 장애 주입 제거 뒤 PG가 정지한 상태에서도 추가 조회 없이 재적용했다.
- 과거 POINT 주문42의 CONFIRMED·7000·POINT·PAID·기록 시각과 실제 GET200을 보존했다. 원문 두 파일·사용자 JMeter 해시도 동일하다.
- 이번 최종 검증의 기능 수정은 없다. 검사 기준·테스트 기대값을 완화하지 않았다. 실제 DB 데드락 victim 선정 실험은 하지 않았고 예외 주입＋실제 rollback/새 TX 검사로 검증했다.

과거 R01 전체384건·R02 전체432건/실험10개는 각 결과 문서의 당시 기록이다. 새 변경/실패가 없다면 전체 check를 반복하지 않는다.

## 이어서 할 작업

사용자가 PR 게시를 요청하면 현재 브랜치와 변경 상태를 확인하고 `volume-5/main` 대상으로 게시한다. 로컬 설명 초안은 무시 대상 `build/r03-pr-body.md`에 있으며 저장소 템플릿과 결과 문서의 실측·한계를 반영했다. 기기별 초안이 없으면 [결과](r03-payment-resilience/result.md)에서 다시 작성할 수 있다. 이번 마무리는 원격 push·PR 생성을 포함하지 않는다.

완료한 정책을 미정으로 돌리지 않는다. 재고 선차감·확정 실패만 한 번 반환·미확인 유지, 관찰 먼저 commit/업무 TX 분리, 주문 CAS·복구 작업 실행 ID 보호·기존 접근 경계는 유지한다.

- Resilience4j `@Retry`와 조회 전용 `@CircuitBreaker`를 별도 빈으로 연결했다. create는 조회 회로/자동 Retry 밖이다.
- 최초 confirm만 자동 시작한다. claim commit 후 2스레드/큐2에 전달한다. 최대4회·1/3/10초±20%·최초전달부터30초다.
- 회차 commit 후 새 TX에서 실행 ID·RUNNING·확보 회차·완료를 재확인하고 HTTP 직전 시간을 확인한다.
- 자동 lease/재시작 회수는 없다. 담당자 GET 후 명시적 POST 대사이며 저장 종결 근거가 없을 때 요청당 PG 조회 최대1회다. 자동 예산을 초기화하지 않는다.
- 내부 반영은 전체 rollback 확인＋MySQL1213/40001에만100ms 뒤 추가1회다. commit 미확인은 저장 상태부터 읽는다.
- 회로 수치/오류 분류는 [집계 문서](r03-payment-resilience/trade_off/02-lookup-circuit-breaker.md)를 따른다. 회로/Retry 지표는 재시작하면 초기화되며 Retry-After는 미지원이다.

## 작업 방식과 자원 상태

기능 동작 변경만 커밋 전 비즈니스·사용자 경험·기술 서브에이전트 리뷰를 받는다. 중요 수정은 구체안의 사용자 승인 후 진행한다. 동작 변경 없는 문서·검증은 별도 리뷰하지 않는다. 필요한 최소 검사·Green 커밋·`type: 한글 요약`/한글 bullet 본문·명시 staging·검사 완화 금지를 유지한다.

- 자신의 commerce/PG JVM과 디버거는 종료했고 8080/8081/8082/5005 listener가 없다. 전용 MySQL `payment-lab-verification`도 정지했다. 데이터·볼륨은 남겼다.
- 임시 trigger는 모두 제거해 0개다. 기존 사용자 MySQL/Redis/다른 테스트 컨테이너는 변경·정지하지 않았다.
- 실험 DB는 호스트13316, commerce `loopers`/PG `loopers_test_pg`다. 기존 SQL000~007 적용 DB이며 새 빈 DB가 아니면 000을 실행하지 않는다.
- `payment-lab`은 commerce8080/관리8082/PG8081, `ddl-auto=validate`, INFO 콘솔이다. [실행 안내](../../docker/payment-lab/README.md)를 따른다.
- 이 PC의 JDK21: `C:/Program Files/Java/jdk-21.0.10`. 무시 대상 `build/docker-api.init.gradle`은 Docker API1.44 호환만 처리한다.
- 검사 로그/집계: `build/r03-full-check.log`, `build/r03-full-check.exit`, `build/r03-full-check-summary.json`. 실험 증거: `build/r03-final-lab-state.json`, `build/r03-final-{api,pg}-*.log`, `build/r03-final-metrics.prom`. 임시 계측·장애 주입 helper와 함께 커밋하지 않는다.
