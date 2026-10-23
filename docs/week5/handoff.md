# 5주차 다음 작업 인계

2026-10-22 R02 PR #25 병합 후 기준 최신화·R03 브랜치 생성과 요구사항/설계 문답을 진행했다. 2026-10-23 R03 첫 기능인 PG 조회 회로·fallback 연결과 관련 검사를 마쳤다. 자동 재조회·내부 반영 Retry는 후속 범위다. 다음 작업 때 현재 위치와 남은 범위를 먼저 확인한다.

## 현재 위치

- 현재 브랜치: `volume-5/r03-payment-resilience`. R02 병합 커밋 `42c22c2`에서 시작했다. 로컬 `volume-5/main`도 이 커밋으로 fast-forward했다.
- R01은 [PR #24](https://github.com/corinB/loop-pack-be-l2-vol5-java/pull/24)로 사용자 병합 완료. 당시 병합 기준은 `4b81541`이며 현재 기준은 R02 병합 `42c22c2`다.
- R02 기능 코드 검증 기준은 동기 callback 커밋 `73bc6cf`다. 이후 최종 변경은 검증 기록·문서다.
- R02는 [PR #25](https://github.com/corinB/loop-pack-be-l2-vol5-java/pull/25)로 게시·사용자 병합 완료. R03은 첫 기능까지 로컬 구현·검증했으며 원격 게시/PR은 아직 없다.
- [R02 결과](r02-pg-order-payment/result.md) → [재현 방법](r02-pg-order-payment/verification.md) → [계획](r02-pg-order-payment/plan.md) → [리뷰 기록](r02-pg-order-payment/review.md) 순서로 확인한다.
- 기존 사용자 변경 `docs/week4/r01-product-list-index/jmeter/product-list-ui.jmx`는 그대로 남긴다. 수정·staging·되돌리기 금지.

## 완료한 구현

재고 선차감 준비 TX·최초 PG 발송 권한, 실제 PG confirm, 검증 관찰 선저장, 주문 종결 CAS·공통 업무 반영과 한 번의 재고 반환을 연결했다. 범용 OrderRepository.save와 신규 POINT confirm 조율은 제거했다.
고객/관리자 결제 조회·소유권·관리자 1번 fixture·미완료 목록, 단일 조회 수동 대사·실행 인계, 동기 callback 권위 조회를 연결했다.
기능 커밋마다 비즈니스·사용자 경험·기술 리뷰를 받았고 중요한 보완은 사용자 승인 후 수정·재검사·재리뷰했다. 미해결 중요 지적은 없다.

## 완료한 검증

- 전체 check: PG 25건 + commerce 407건 = **432건**, 실패·오류·건너뛰기 0. Checkstyle·ArchUnit·기존 slow/example 포함.
- 별도 PG/commerce JVM 실험 10개: 동시 confirm·callback 먼저/늦은 생성 응답·유실 뒤 실패·관리자 재고 변경·삭제 상품 반환·성공 후 내부 rollback·commerce/PG 강제 종료와 재시작·생성 전 거절·callback 송신 timeout 뒤 실제 완료·반환 overflow 수동 복구.
- 새 실험 주문 45~54: 성공 5건/실패 처리 완료 5건, USE 0건. 성공 재고 3, 일반 실패 5, 관리자 수량 10 설정 후 실패 12. 재고/거래/주문 기록의 중복 효과 없음.
- 기존 POINT 주문 42의 CONFIRMED·7000·PAID·POINT·기록 시각을 보존했고 실제 GET도 200.
- JMeter 사용자 변경·두 원문 SHA-256 보존. 검사 기준 완화 없음. 최종 검증에서 추가 기능 수정은 없었다.

자동화 검사에서 HTTP 호출 횟수·거래 없음/충돌·4회 사용 fixture와 옛 실행 차단을 확인했다. 실제 commerce 재시작 실험은 사용 회차 0의 보존을 확인한 것이며 자동 4회 실행 검증이 아니다.
각 검증의 분담과 증거는 [결과](r02-pg-order-payment/result.md)에 남겼다. 새 변경이나 실패가 없으면 전체 check를 다시 반복하지 않는다.

## 다음 작업 범위

R03 [요구사항](r03-payment-resilience/requirement.md)·[결정 목록](r03-payment-resilience/trade_off/total_trade_off.md)·[상세 계획](r03-payment-resilience/plan.md)을 먼저 읽는다. 주요 정책 문답을 마치고 계획 초안의 세 관점 초회 리뷰를 받았다. [리뷰 기록](r03-payment-resilience/review.md)의 회차 commit 뒤 권한/시간 재확인은 사용자가 승인해 계획에 반영했고 세 관점 재리뷰를 마쳤다. 계획의 미해결 중요 지적은 없다. 첫 기능인 공유 PG 조회 회로·fallback·수동/callback 연결·기본 지표를 구현했다. 기능 리뷰의 알려진 충돌 제외 보완도 사용자 승인 후 적용했으며 세 관점 재리뷰에 미해결 중요 지적이 없다. 관련 검사 76건(실패·오류·skip 0)과 Checkstyle/ArchUnit을 통과했다. 기록은 위 R03 리뷰 문서에 남겼다. 다음은 계획의 두 번째 기능인 내부 반영 데드락 Retry다.

- 자동 조회 Retry와 공유 조회 CircuitBreaker는 Resilience4j 애노테이션이다. 기존 Spring Retry 선택은 대체했다. [연결/운영 문서](r03-payment-resilience/trade_off/03-resilience-library-wiring.md)를 따른다.
- 최초 confirm에서만 자동 시작한다. DB claim commit 후 전용 2스레드/큐2에 전달하며 접수 거절은 100ms 뒤 전달만 추가1회다. 대기 중도 RUNNING이다.
- 최초＋추가3회·1/3/10초±20%·최초전달부터30초다. 회차는 조회 직전 commit한다. 이미 시작한 조회의 저장/반영은 마무리하고 중단 후 자동 재개하지 않는다.
- 내부 반영은 확실히 rollback된 MySQL 데드락에만100ms 뒤 추가1회다. 다른 오류와 commit 불명확은 재시도하지 않고 저장 상태부터 확인한다.
- 회로 수치·정상/실패/제외·HTTP 분류는 [집계 문서](r03-payment-resilience/trade_off/02-lookup-circuit-breaker.md)를 따른다. Retry-After는 미지원이다.
- 운영 범위는 기존 조회＋로그＋기본 Prometheus 지표다. 주요 중단 사유를 기존 필드로 구분하고 별도 운영 API나 대시보드는 추가하지 않는다.
- 계획 리뷰의 중요 보완은 사용자와 구체안을 정한 뒤 반영한다. 실제 구현도 기능 커밋 전 세 관점 리뷰·필요 최소 검사 정책을 유지한다.
공유 조회 CircuitBreaker·fallback은 구현했으며 자동 조회·Resilience4j Retry는 아직 구현하지 않았다. 재시작 자동 회수·lease도 없다. 현재는 운영 목록 GET 후 명시적인 수동 POST로 복구한다.

확정한 선차감 정책·주문 CAS·관찰 선저장·파사드/TX 분리·복구 작업 CAS·고객/관리자 접근·수동 조회 최대 1회·동기 callback은 다시 미정으로 돌리지 않는다.
같은 주문·키·금액·실효 조건은 기존 상태를 반환하며 발송 시작 후 create를 다시 보내지 않는다. 실패 근거 없이 timeout/빈 조회만으로 재고를 복구하지 않는다. 신규 PG는 포인트를 쓰지 않고 과거 POINT는 유지한다.

## 이어서 지킬 작업 방식

- 기능 동작 변경만 커밋 전에 비즈니스·사용자 경험·기술 서브에이전트 리뷰를 받는다. 문서·검증 전용 변경은 별도 리뷰하지 않는다.
- 중요한 수정은 구체안을 사용자에게 설명하고 승인 후 수정·관련 검사·재리뷰한다.
- 테스트는 필요한 대표 사례만 추가한다. 관련 검사 후 필수 전체 check는 PR 마무리에 한 번 실행하고 변경/실패로 필요할 때만 반복한다. 기존 기준을 완화하지 않는다.
- 커밋은 `type: 한글 요약`·한글 상세 본문·명시 staging을 따른다. 원격 게시 범위를 임의로 확대하지 않는다.

## 로컬 실행 상태

- 이 작업에서 시작한 commerce/PG JVM과 전용 MySQL `payment-lab-verification`은 정지했다. 임시 실패/지연 trigger도 모두 제거했다. 데이터와 볼륨은 보존했다.
- 실험은 host MySQL 13316의 두 스키마만 사용했다. 기존 사용자 MySQL/Redis에는 변경·정지 작업을 하지 않았다.
- 기존 전용 DB에는 000~007이 적용돼 있다. 새 빈 DB가 아니면 000을 실행하지 않는다. 이전 실험 주문 43의 PG SUCCESS/commerce PENDING과 주문 44의 확정 거절도 그대로 남겼다.
- 프로필은 `payment-lab`, commerce8080/관리8082/PG8081, `ddl-auto=validate`·명시 SQL·INFO 콘솔이다. [실행 문서](../../docker/payment-lab/README.md)를 따른다.
- 이 PC의 JDK21은 `C:/Program Files/Java/jdk-21.0.10`. Gradle에는 JAVA_HOME을 지정했다.
- 무시 대상 `build/docker-api.init.gradle`은 Docker 테스트 JVM의 `api.version=1.44` 호환만 처리한다. 기기별 임시 설정·로그·helper는 커밋하지 않는다.
- 전체 검사 로그/집계는 `build/r02-full-check.log`, `build/r02-full-check-summary.json`, 각 모듈 `build/test-results/test/`. 실서버 로그/관찰은 `build/r02-final-{api,pg}-*.log`, `build/r02-final-lab-state.json`.
