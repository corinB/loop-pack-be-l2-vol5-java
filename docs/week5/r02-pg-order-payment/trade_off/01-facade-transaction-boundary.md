# 파사드 조율과 트랜잭션 경계

[설계 질문 목록](total_trade_off.md) · [요구사항](../requirement.md) · [mall 재고 모델 결정](../../r01-pg-payment-foundation/trade_off/07-stock-adjustment-boundary.md)

상태: **A·후속 검증 결과 저장 TX 분리 채택·세부 설계 전·구현 전**.

## 사용자 결정

2026-10-21 사용자는 조율 파사드와 준비·공통 결과 반영용 TX 컴포넌트를 분리하는 A를 선택했다.
조율 파사드는 전체 흐름을 긴 TX로 감싸지 않고, 준비·반영은 각각 별도 Spring 빈의 공개 @Transactional 진입점으로 둔다.
callback·수동 대사·자동 재확인의 검증된 결과도 공통 결과 반영 TX 컴포넌트를 사용한다.
컴포넌트/입출력 계약이 늘어나는 비용을 수용한다. 정확한 클래스명·Context 배치·계약·잠금 순서는 후속 설계에서 정한다.
후속 [학습용 최소 보완](../../r01-pg-payment-foundation/trade_off/10-learning-scope-completion.md)으로 PG 검증 결과를 먼저 짧은 별도 TX에서 commit하고 공통 업무 반영 TX를 호출하기로 확정했다. 준비/반영 분리의 A 원칙을 유지하며 관찰 저장 진입점이 추가된다. 별도 관찰 모델·범용 이벤트 엔진을 만드는 선택은 아니다.

## 현재 코드와 사용자 제안

사용자는 mall 재고 모델·동일 TX 복구를 확정한 뒤 파사드 패턴 적용도 필요하다고 제안했다.
현재 [ConfirmOrderFacade](../../../../apps/commerce-api/src/main/java/com/loopers/application/ordering/facade/ConfirmOrderFacade.java)는 이미 주문·지갑·상품 application 서비스를 조율한다.
기존 execute 전체의 @Transactional은 포인트 결제용 단일 TX이며, PG 호출을 그 안에 추가하는 것은 확정한 TX 밖 HTTP 정책과 맞지 않는다.
파사드 조율 규칙은 유지하면서 PG 흐름에 맞게 TX 경계를 분리한다. 정확한 클래스명·Context 배치·컴포넌트 수는 아직 미정이다.

## 두 안에 공통인 역할

```text
application의 전체 흐름 조율
  → 준비 TX A
      주문/상품 검증·재고 차감
      mall 차감 기록·결제 시도·발송 사실 저장
    commit
  → TX 밖 PG HTTP
  → 검증 결과 저장 TX
      검증된 PG 관찰/생성 전 확정 거절 사실을 기존 pay 시도에 저장
    commit
  → 공통 결과 반영 TX B
      먼저 저장한 검증 결과와 기존 시도/주문 상태 확인
      성공: 주문 확정·차감 재고 유지
      확정 실패: 실제 재고 복구·복구 완료 기록
      관련 주문/결제/복구 작업 변경을 함께 보호
    commit
```

- ordering·pay·mall의 application 서비스를 파사드가 조율한다. domain에 다른 Context의 실행 순서나 HTTP를 넣지 않는다.
- 생성 응답·callback 권위 조회·수동 대사·자동 재확인의 검증된 결과는 공통 결과 반영 경계를 사용한다.
- timeout·조회 0건·재시도 소진·수동 전환은 임의 실패/복구를 만들지 않는다.
- PG 호출·재조회 대기 동안 DB TX/잠금을 유지하지 않는다. 복구 작업 생성/전달 시점과 실행 권한 확보의 상세 순서는 별도 설계 대상이다.
- 실제 재고 증가·복구 완료 기록은 TX B에서 함께 commit/rollback한다. 외부 PG 사실 자체는 로컬 rollback으로 되돌릴 수 없다.
- 검증 결과 저장 TX는 TX B가 열리기 전에 commit한다. TX B 실패가 먼저 저장한 결과를 지우지 않으며, 신뢰할 종결 결과가 저장돼 있으면 PG 재조회 없이 같은 사실을 재적용할 수 있다. 저장한 생성 전 확정 거절은 빈 목록/404로 재증명하지 않는다.
- 관찰 저장 전 종료된 미확인이나 저장 commit 결과 불명확은 저장 상태부터 확인한다. 없는 근거를 만들어 실패/재고 복구하지 않고 create도 다시 보내지 않는다.

## A. 흐름 조율 파사드 + 별도 짧은 TX 컴포넌트 · 채택

- 전체 조율 진입점은 긴 TX를 열지 않고 준비/반영의 공개 유스케이스 경계를 호출한다.
- 준비·검증 결과 저장·공통 업무 반영은 별도 Spring bean의 @Transactional 진입점으로 둔다. 하위 Context 서비스는 기존처럼 TX 안의 업무 행동을 수행한다.
- 같은 클래스 내부 호출에만 @Transactional을 붙여 프록시가 적용된 것으로 가정하지 않는다.
- 이득: HTTP와 DB 경계가 드러나고 공통 결과 반영 경로를 재사용하기 쉽다. 기존 application 파사드/서비스 관례와 연결된다.
- 비용: 준비·반영 컴포넌트와 입출력 계약이 늘어난다. 지나치게 잘게 나누지 않도록 역할 기준을 유지해야 한다.

## B. 한 조율 파사드에서 TransactionTemplate으로 TX 구간 지정 · 미채택

- 한 파사드 안에서 준비/반영 구간을 TransactionTemplate 콜백으로 감싸고 PG HTTP는 콜백 밖에 둔다.
- 이득: 단계별 TX의 시작/종료와 전체 순서를 한곳에서 볼 수 있으며 별도 TX 컴포넌트 수를 줄일 수 있다.
- 비용: 흐름 조율 코드에 프로그램 방식 TX 제어가 섞인다. 공통 결과 반영의 재사용 책임을 별도로 정해야 한다.
- 기존 넓은 @Transactional을 유지한 채 TransactionTemplate만 더해서 HTTP가 TX 밖이 됐다고 판단하지 않는다.

## 선택 이유와 남은 질문

A는 공통 결과 반영을 callback·수동 대사·자동 재확인에서 호출하기 쉽고 기존 파사드/서비스 관례를 유지할 수 있어 채택했다.
전체 흐름을 하나의 @Transactional로 감싸지 않는다. 기존 단일 TX를 유지한 채 PG 호출을 추가하는 구성은 채택한 정책과 다르다.

- 구체적인 클래스명·Context·UseCase 계약과 오류 반환 경계.
- 결제 시도·복구 작업·주문·상품·mall 재고 기록의 잠금 순서와 실행 권한 확보 방식.
- 먼저 commit할 PG 관찰/거절 사실의 정확한 필드·저장 계약, 내부 반영 실패 기록·재적용 입력. 관찰/업무 반영 TX 분리는 확정이다.
- 복구 작업 전달·장애 중단 후 담당자 수동 대사·callback 경합과 재조회 횟수 기록 연결. 재시작 자동 재개는 후속 결정으로 대체됐다.
- 실제 HTTP 시점에 활성 DB TX가 없고 준비/반영 rollback이 보호되는지 검증할 테스트.

후속 결정으로 R01의 [복구 작업 실행 권한 확보](../../r01-pg-payment-foundation/trade_off/08-recovery-claim-coordination.md)는 멱등성 키 + 조건부 상태 갱신 + 실행 식별자의 C로 변경했다. 현재 후속 질문 순서는 [R01 목록](../../r01-pg-payment-foundation/trade_off/total_trade_off.md)을 따른다.

참고: [Spring의 선언적 TX와 프록시 호출](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html),
[TransactionTemplate](https://docs.spring.io/spring-framework/reference/data-access/transaction/programmatic.html).
