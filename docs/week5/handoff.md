# 5주차 다음 작업 인계

2026-10-21 작업 인계에 2026-10-22 PR 게시 상태를 반영했다. 다음 작업 때 이 문서부터 읽는다.

## 현재 위치

- 브랜치: `volume-5/r01-pg-payment-foundation`.
- PR01 로컬 구현·검증 완료. 구현·검증 정리의 마지막 커밋은 `5e7e596`이다.
- 사용자 요청으로 기준/작업 브랜치를 원격 게시하고 [PR #24](https://github.com/corinB/loop-pack-be-l2-vol5-java/pull/24)를 생성했다. 대상은 `volume-5/main`이며 아직 병합하지 않았다. 기준 브랜치는 준비 커밋 `d443cdc`다.
- [PR01 결과](r01-pg-payment-foundation/result.md)에 구현 범위·검증·한계를 기록했다.
- 작업 폴더에는 기존 사용자 변경인 `docs/week4/r01-product-list-index/jmeter/product-list-ui.jmx`만 남긴다. 수정·staging·되돌리기 금지.

## 확인을 마친 것

- PG 25건·commerce 359건, 총 384건의 전체 검사 통과. 실패·오류·건너뛰기 0건이며 Checkstyle·ArchUnit을 포함한다.
- 실제 PG HTTP 연결, 응답 유실 후 성공/실패, PG 강제 종료 후 같은 거래의 완료·보존을 확인했다.
- 새 commerce JVM에서 관찰 결과·실효 조건·복구 작업의 사용 횟수와 차감 기록 보존을 확인했다.
- 초기/증분 SQL의 validate 기동과 기존 POINT 기록 보존을 확인했다.
- 승인한 설정 수정 후 commerce API 8080·PG 8081·commerce 관리 8082 동시 기동과 INFO 로그를 확인했다.
- 기능 커밋 전 세 관점 리뷰, 중요 수정의 사용자 승인·재리뷰를 마쳤다. 미해결 중요 지적은 없다.

## 다음에 시작할 일

1. PR01 [결과](r01-pg-payment-foundation/result.md)와 [계획](r01-pg-payment-foundation/plan.md)을 확인한다. 완료한 검사를 이유 없이 반복하지 않는다.
2. PR #24의 리뷰·CI 결과를 확인한다. 게시·PR 생성은 승인받아 완료했으며 병합은 아직 요청받지 않았다.
3. R01 병합 후 최신 `volume-5/main`에서 `volume-5/r02-pg-order-payment`를 생성한다. 지금 미리 생성하거나 작업 브랜치를 직접 합치지 않는다.
4. PR02 [요구사항](r02-pg-order-payment/requirement.md)·[남은 설계 질문](r02-pg-order-payment/trade_off/total_trade_off.md)을 사용자와 논의한다. 우선 준비 TX의 주문/재고 잠금 순서·발송 권한, 실제 재고 복구 경계와 API 소유권 범위를 구체화한다.
5. 합의한 뒤 PR02 상세 커밋·최소 검증 계획을 작성하고 세 관점 계획 리뷰를 거쳐 구현한다. 현재 [PR02 계획](r02-pg-order-payment/plan.md)은 틀만 있고 구현은 시작하지 않았다.

이미 정한 재고 선차감, 확정 실패 시 한 번 복구, 미확인 시 유지, 관찰 먼저 commit, 파사드/TX 컴포넌트 분리, 복구 작업 CAS는 다시 미정으로 돌리지 않는다.
기존 confirm은 아직 POINT 결제다. 실제 PG 주문·상품 수량 복구·callback 수신·수동 대사는 PR02, 자동 재조회·회로·fallback은 PR03이다.

## 이어서 지킬 작업 방식

- 기능 구현만 커밋 전에 비즈니스·사용자 경험·기술 서브에이전트 리뷰를 받는다.
- 중요한 수정은 구체안을 사용자에게 물어보고 승인 후 수정·관련 검사·재리뷰한다. 사소한 보완까지 승인 대상으로 확대하지 않는다.
- 테스트는 필요한 부분만 최소로 작성한다. 관련 검사 후 필수 전체 검사는 마무리에 실행하며 기존 기준은 완화하지 않는다.
- 커밋은 `type: 한글 요약`, 본문은 한글 상세, 명시 staging을 따른다.

## 로컬 실행 상태

- 이번 작업의 PG·commerce·probe JVM은 종료됐고, 전용 MySQL 컨테이너 `payment-lab-verification`도 정지했다. 실험 DB 데이터는 남아 있다.
- 기존 MySQL·Redis와 볼륨은 보존했다. 실제 사용자 로컬 commerce DB는 과거 스키마여서 실험에는 별도 MySQL 포트 13316을 사용했다.
- 실행/SQL/probe 재현은 [실행 문서](../../docker/payment-lab/README.md)를 따른다. 기존 데이터에 빈 DB용 `000` SQL을 적용하지 않는다.
- 이 PC의 JDK 21은 `C:/Program Files/Java/jdk-21.0.10`이다. Gradle 실행 시 `JAVA_HOME`을 지정했다.
- Docker 테스트의 로컬 API 호환은 무시 대상 `build/docker-api.init.gradle`의 `api.version=1.44`로 처리했다. 기기별 임시 설정은 커밋하지 않았다.
- 검증 로그는 로컬 `build/`와 각 모듈의 `build/test-results/test/`에 있다. 원문 두 파일은 첨부와 SHA-256이 일치한다.
