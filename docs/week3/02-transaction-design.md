# 3주차 트랜잭션·잠금 설계

[요구사항](01-requirements.md)을 구현하기 위한 설계다. 아래의 목표 경로·계약·테스트는 아직 구현하지 않았다.
현재 구조는 corinB의 d30c06e 소스를 읽어 확인했으며, 실제 SQL·프록시·트랜잭션 참여의 실행 증거는 PR 02~03에서 남긴다.

## 1. 현재 구조와 보완 지점

| 흐름 | 현재 코드에서 확인한 경로 | 보완할 점 |
|---|---|---|
| 브랜드 삭제 | AdminBrandController → DeleteBrandUseCase → BrandService.execute → ActiveProductChecker·BrandRepository | 활성 상품이 있으면 거절함. 연결 상품의 도메인 행동·저장과 전체 롤백으로 확장 |
| 주문 생성 | OrderController → CreateOrderUseCase → OrderService.execute → ProductRepository 일반 조회·OrderRepository 저장 | DRAFT·품목만 저장하며 선점 없음. 이 계약 유지 |
| 주문 확정 | OrderController → ConfirmOrderUseCase → ConfirmOrderService.execute → ConfirmOrderWriter | 일반 조회 뒤 계산한 재고·잔액을 저장함. 조회 단계부터 동시 변경 보호 필요 |
| 충전 | PointController → ChargePointUseCase → PointService.execute → PointRepository·PointBillRepository | 일반 조회·잔액 변경·이력 저장 앞에 포인트 잠금 필요 |
| 상품 수정·삭제·재고 설정 | AdminProductController → 각 UseCase → ProductService.execute → ProductRepository | 같은 상품 행을 바꾸는 경로의 잠금 규칙 통일 |

확정의 실제 infrastructure 구현은 JdbcConfirmOrderWriter다.
load는 OrderRepository → 품목 순서의 ProductRepository → PointRepository로 도메인을 복원한다.
save는 재고 batch UPDATE → 포인트 UPDATE → PointBill INSERT → OrderBill INSERT → 주문 상태 UPDATE 순서다.

참고 코드:
[BrandService](../../apps/commerce-api/src/main/java/com/loopers/application/mall/brand/BrandService.java),
[ConfirmOrderService](../../apps/commerce-api/src/main/java/com/loopers/application/ordering/order/ConfirmOrderService.java),
[JdbcConfirmOrderWriter](../../apps/commerce-api/src/main/java/com/loopers/infrastructure/ordering/order/JdbcConfirmOrderWriter.java),
[ProductEntityMapper](../../apps/commerce-api/src/main/java/com/loopers/infrastructure/mall/product/ProductEntityMapper.java).

## 2. 트랜잭션 경계와 계층 책임

목표 호출 경로는 다음과 같다. Controller가 주입받은 Spring bean의 공개 execute를 호출할 때 프록시를 통과해야 한다.

```text
Controller
  → Spring proxy: 트랜잭션 시작/참여
    → application Service.execute: 대상·도메인 행동·저장 순서 조율
      → repository / ConfirmOrderWriter
        → 같은 DataSource·트랜잭션의 JPA / JDBC 작업
  ← 정상 반환 후 commit 또는 예외 전파 후 전체 rollback
```

- 기존 쓰기 Service.execute의 @Transactional과 기본 REQUIRED 경계를 유지한다.
- 별도 Service가 필요하더라도 프록시를 통과하는 공개 진입점에 경계를 둔다. 같은 객체의 자기 호출에 새 경계를 기대하지 않는다.
- 도메인과 Mapper는 순수 상태·변환 책임만 가진다. Spring/JPA 잠금 타입·annotation을 domain에 넣지 않는다.
- 도메인 repository에 잠금 조회 계약을 추가하고 infrastructure에서 구현한다. GET QueryDao는 읽기 전용을 유지한다.
- 상품별 REQUIRES_NEW, 부분 성공 반환, 내부 예외를 삼킨 정상 반환은 사용하지 않는다.
- 업무 실패는 기존 DomainException·ApplicationException으로 전파한다. 저장·잠금 오류도 바깥 경계까지 전파한다.
- 현재 업무 예외와 Spring 저장 예외의 타입·rollback 규칙을 실행 시 확인한다. checked 예외를 도입해 기본 rollback 규칙에서 빠지게 하지 않는다.
- Controller는 트랜잭션 밖에서 기존 ApiResponse와 HTTP 오류로 변환한다.

### JPA와 JDBC의 동일 트랜잭션 참여

기존 JpaConfig는 JPA repository와 트랜잭션 관리를 활성화하고 DataSourceConfig는 primary HikariDataSource를 제공한다.
이 구성만 보고 JPA와 JDBC의 실제 물리 트랜잭션 공유를 검증했다고 결론내리지 않는다.

- PR 03에서 실제 transaction manager와 JdbcClient·NamedParameterJdbcTemplate의 DataSource 연결을 확인한다.
- 주문 확정 JDBC를 별도 connection/autocommit이나 독립 transaction manager로 분리하지 않는다.
- JPA로 잠근 행의 JDBC 변경과 이력·주문 저장이 함께 롤백되는 통합 테스트를 실행한다.
- 도메인은 JPA Entity와 분리되어 있다. 브랜드·상품의 도메인 변경은 repository.save를 명시한다.
- 확정은 기존 예외 규칙대로 ConfirmOrderWriter.save를 명시하고 JDBC 저장을 유지한다.
- JDBC 변경 뒤 JPA 관리 객체는 이전 값을 담을 수 있다. 확정 도중 같은 객체를 다시 save하거나 그 객체로 DB 저장 결과를 검증하지 않는다.
- flush는 SQL 전송이며 commit이 아니다. 아직 반영하지 않은 변경을 잃는 무조건적 clear는 추가하지 않는다.
- 최종 DB 검증은 서비스 트랜잭션 종료 후 새 경계에서 수행한다.

## 3. 비관적 잠금 계약과 순서

### 저장 계약

기존 일반 조회를 모두 잠금 조회로 바꾸지 않는다. 변경 경로만 명시적으로 다음 계약을 사용하도록 확장한다.
아래 이름은 구현 목표이며 현재 repository에 존재한다고 기록하지 않는다.

| 계약 | 위치·용도 |
|---|---|
| BrandRepository.findByIdForUpdate | 브랜드 수정·삭제를 위한 단건 잠금 조회 |
| ProductRepository.findByIdForUpdate | 확정·상품 수정·삭제·최종 재고 설정의 단건 잠금 조회 |
| ProductRepository.findActiveIdsByBrandId | 브랜드 일괄 삭제 후보 ID 조회, 도메인 객체 변경 전에 ID만 수집 |
| OrderRepository.findByIdForUpdate | 같은 주문의 DRAFT 확인·확정 보호 |
| PointRepository.findByUserIdForUpdate | 주문 결제와 충전의 사용자별 잔액 보호 |

단건 잠금 조회는 Optional을 유지하고 infrastructure의 JPA PESSIMISTIC_WRITE로 구현한다.
기존 findById·findByUserId는 읽기·주문 생성 등 비변경 경로에 남긴다.
변경할 객체를 먼저 일반 조회해 복원한 뒤 나중에 잠금만 추가하지 않는다. 잠금 조회로 읽은 현재 상태를 판단에 사용한다.

### 경로별 획득 순서

| 변경 경로 | 잠금 순서 | 다른 자원 접근 |
|---|---|---|
| 주문 확정 | Order → Product ID 오름차순 → Point | 주문의 저장된 userId 사용, 새 이력 INSERT는 같은 트랜잭션 |
| 브랜드 일괄 삭제 | Brand → Product ID 오름차순 | Order·Point는 잠그거나 변경하지 않음 |
| 브랜드 수정 | Brand | 상품을 변경하지 않음 |
| 상품 수정·삭제·최종 재고 설정 | Product | 기존 응답용 Brand·likeCount 조회는 비잠금 조회 유지 |
| 포인트 충전 | Point | CHARGE 이력은 같은 트랜잭션에 저장 |

- 여러 상품은 중복 ID를 제거하고 정렬한 뒤 단건 잠금 조회를 순서대로 호출한다.
- 상품 잠금·저장 순서는 정렬하되 기존 주문 품목 스냅샷의 표시 순서를 불필요하게 변경하지 않는다.
- 상품을 잠근 뒤 Brand 잠금을 새로 획득하는 역방향 경로를 추가하지 않는다.
- ProductEntityMapper.apply는 재고를 포함한 여러 필드를 함께 반영한다. 정보 수정·삭제도 보호해야 오래된 재고의 덮어쓰기를 방지할 수 있다.
- 브랜드 수정도 오래된 deleted 값을 다시 저장하지 않도록 브랜드 잠금 조회를 사용한다.
- 잠금은 트랜잭션 종료 시 해제한다. 사용자 결제 대기, 외부 호출, sleep·테스트 장벽을 잠금 구간에 넣지 않는다.
- 일관된 순서는 교착 위험을 줄이는 설계다. 모든 교착이 사라진다고 보장하지 않는다.

### 오류 정책

재고 부족·잔액 부족·재확정은 기존 409 업무 오류다.
없는·삭제된 대상은 기존 404, 잘못된 입력은 기존 400을 유지한다.
잠금 시간 초과·교착·SQL 오류는 기술 오류로 기록하고 기존 500 응답을 유지한다.
자동 재시도·새 오류 응답 규격은 추가하지 않는다. 기존 DB 잠금 대기 설정은 임의 변경하지 않고 테스트 결과와 함께 기록한다.

## 4. 브랜드 일괄 삭제 흐름

1. BrandService.execute(Delete)의 프록시 경계로 진입한다.
2. 브랜드를 잠금 조회하고 존재·활성 여부를 검사한다.
3. 연결된 미삭제 상품 ID를 조회하여 오름차순으로 정렬한다. 재고 수량 조건을 넣지 않는다.
4. 각 상품을 ID 순서로 잠금 조회하고 현재 삭제 여부를 재확인한다. 이미 삭제된 상품은 건너뛴다.
5. 미삭제 상품마다 delete()와 repository.save를 호출한다.
6. 브랜드의 delete()와 repository.save를 호출한다.
7. 전부 정상 종료하면 commit하고 기존 성공 응답을 반환한다. 중간 예외는 전체 rollback한다.

삭제 제한에만 쓰이던 ActiveProductChecker와 해당 구현·오류 분기는 실제 참조를 확인해 그 책임 범위에서 정리한다.
기존 삭제 거절 테스트는 변경된 API 계약에 맞는 성공·실패 후 상태 보존 테스트로 대체한다.
검사를 통과시키려고 테스트를 삭제하거나 오류 검증을 약화시키는 것으로 처리하지 않는다.

도메인의 단건 delete 규칙을 사용하며 SQL bulk UPDATE로 우회하지 않는다.
주문·좋아요·결제 행을 연쇄 삭제하지 않는다. 목록·상세·좋아요·새 주문의 기존 삭제 필터를 함께 검증한다.
브랜드 삭제와 상품 등록의 동시 실행은 범위 밖이므로 후보 집합의 동시 추가 방지까지 보장했다고 기록하지 않는다.

## 5. 주문 확정과 관련 경로

1. ConfirmOrderService.execute의 프록시 경계로 진입한다.
2. JdbcConfirmOrderWriter.load에서 주문을 잠금 조회하고, 품목의 상품 ID를 정렬해 잠금 조회한다.
3. 저장된 주문 userId의 Point를 잠금 조회하고 ConfirmOrderLoad로 도메인을 전달한다.
4. 기존 도메인 행동으로 DRAFT, 상품 활성 여부·재고, 포인트 잔액을 검사하고 변경한다.
5. ConfirmOrderWriter.save가 재고 → 잔액 → USE 기록 → PAID 기록 → 주문 상태 순서로 JDBC 저장한다.
6. 성공 시 기존 결과를 반환하고, 어느 단계의 실패든 변경·기록 전체를 rollback한다.

Order.confirm의 메모리 상태 변경과 orders 테이블의 저장 시점을 구분한다.
도메인 검증 전에 상품을 잠그더라도 DRAFT 생성 시점의 재고 선점이 되는 것은 아니다.
주문 상태 보호는 주문 행 잠금으로 수행한다. 결제 기록의 유일성 제약만으로 중복 확정을 해결했다고 판단하지 않는다.

PointService의 충전과 ProductService의 변경 경로도 위 잠금 계약을 사용한다.
각 Service의 기존 트랜잭션·도메인 행동·save·성공 응답을 유지한다.
PointBill·OrderBill 소유권과 현재 ConfirmOrderWriter의 저장 예외 규칙을 바꾸는 리팩터링은 포함하지 않는다.

## 6. 검증 설계

테스트 ID와 기대 수치는 [요구사항](01-requirements.md)의 T01~T08을 기준으로 한다.
아래 신규 클래스명은 작성 예정 이름이며 실행 건수를 보고할 때 실제 작성한 이름으로 맞춘다.

### 실제 SQL 이후 전체 롤백

| 테스트 | 실패 주입 위치 | 별도 조회로 확인할 상태 |
|---|---|---|
| BrandRemovalTransactionTest | 첫 상품의 실제 저장·flush 이후 다음 상품 저장 경계에서 예외 | 브랜드·모든 상품·다른 대상·과거 주문의 요청 전 상태 |
| OrderTransactionTest | 실제 재고·잔액·USE 저장 이후 PAID 기록 INSERT 경계에서 예외 | 재고·잔액 복구, DRAFT 유지, 신규 USE·PAID 기록 없음 |

브랜드 테스트는 테스트 전용 repository decorator 등으로 실제 delegate 저장을 실행하고 EntityManager.flush 후 다음 저장을 실패시킨다.
주문 테스트는 테스트 전용 JDBC 실행 경계에서 해당 INSERT만 실패시키고 앞선 UPDATE·INSERT는 실제 DB에 보낸다.
테스트 설정은 해당 테스트에만 적용하고 운영 코드에 실패 flag·API·sleep을 추가하지 않는다.
실행한 변경 SQL과 실패 위치를 확인한 다음, 예외가 프록시 바깥으로 전달되고 트랜잭션이 종료된 후 재조회한다.
예외만 확인하거나 테스트 메서드 자체의 자동 rollback을 서비스 rollback의 증거로 사용하지 않는다.

### 갱신 유실 대조군

LostUpdateControlTest에서 독립 connection·transaction 2개가 commit된 재고 5를 잠금 없이 읽게 한다.
둘 다 5를 읽었다는 것을 확인한 뒤 쓰기를 허용하고, 각각 상수 4를 조건·version 없이 UPDATE한 뒤 commit한다.
두 commit 성공·최종 재고 4·불변식 위반을 assertion으로 확인하는 통과 테스트로 작성한다.
두 번째 UPDATE의 changed-row 수가 0인 차이를 업무 거절로 세지 않는다. SQL 오류·timeout은 재현 성공이 아니다.

### 실제 경쟁 테스트

- OrderConcurrencyTest: 재고 5/주문 8, 잔액 10,000원/4,000원 주문 3, 충전 2,000원/결제 7,000원의 필수 시나리오.
- 같은 주문 2건 동시 확정: 성공 1·재확정 거절 1, 재고·포인트 차감 및 USE·PAID 기록 각 1회.
- 서로 반대 순서의 상품 2개를 가진 주문: 충분한 재고·잔액에서 둘 다 성공하며 품목별 수량 불변식 일치.
- 관리자 재고 설정과 차감: 초기 재고 5, 최종 수량 10 설정과 1개 확정을 동시 실행해 둘 다 성공, 최종 재고는 직렬 순서에 따른 9 또는 10.
- 상품 정보 수정과 확정: 변경된 상품 정보와 확정 차감이 함께 남고, 과거 재고를 다시 저장하지 않음.
- 대표 API·기존 조회 회귀: 삭제 후 새 사용 제한, 기존 좋아요 취소, 과거 주문 스냅샷, 생성 시 재고·포인트·기록 불변.

실제 서비스 경쟁은 시작 latch만 사용한다. 읽기 이후 장벽은 대조군에만 둔다.
재고 테스트는 사용자도 분리해 같은 포인트 잠금이 상품 경쟁을 가리지 않게 한다.
포인트 테스트는 재고를 충분히 준비해 잔액 경쟁을 재고 부족이 가리지 않게 한다.
성공·업무 거절·기술 오류를 요청별로 수집하고 모든 worker 종료 후 DB 주문·품목·잔액·기록과 대조한다.

### DB·시간·자원

- 기존 MySqlTestContainersConfig의 MySQL 8.0과 DatabaseCleanUp을 사용한다. mock·메모리 DB로 잠금·롤백을 대신하지 않는다.
- 초기 데이터를 worker 시작 전에 commit한다. 테스트 전체를 하나의 부모 @Transactional로 감싸지 않는다.
- 대조군은 최소 worker·connection 2개, 최대 8건 경쟁은 worker 8개와 충분한 connection을 확보한다. 테스트의 기존 풀 상한은 10이다.
- 준비·완료 대기는 각각 60초, 정리 대기는 10초를 기본 테스트 한도로 두고 timeout을 기술 오류로 실패 처리한다.
- DB 잠금·JDBC 실행도 대기 한도와 맞는지 확인한다. finally에서 latch 해제, 작업 취소·rollback, executor·connection 종료를 수행한다.
- 작업이 남은 상태에서 truncate하지 않는다. 종료가 안 된 테스트는 성공으로 집계하지 않는다.
- HTTP는 대표 경로를 실제 Controller·application·repository·DB로 검증하고 동일 경계값을 모든 계층에 반복하지 않는다.

## 7. 완료 증거

각 구현 PR에 관련 테스트의 실행 명령·건수·실패·종료 상태, 실제 실패 SQL 위치, 잠금 SQL과 대상·순서를 기록한다.
기본 계층·네 Context 및 shared의 도메인 순수성 규칙을 유지하고 실제 변경 클래스를 검사한다.
현재 문서는 설계만 정리했다. 검사 실행·통과, 동시성 안전성, 실제 전체 롤백 검증 완료를 뜻하지 않는다.
