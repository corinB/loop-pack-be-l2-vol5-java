# R01 구현·측정 계획

[요구사항](requirement.md) · [트레이드오프](trade_off/total_trade_off.md) · [JMeter 안내](jmeter-guide.md) · [전체 요구사항](../total_requirement.md)

작업 브랜치: `volume-4/r01-product-list-index` · PR 대상: `volume-4/main`

상태: **SQL seed 실행기와 변경 전 기준선 준비 진행**. 2026-10-16 사용자 승인 계획을 반영했다.
이 문서의 미완료 항목은 실행 결과가 아니며, 구현·검증 후 실제 상태를 갱신한다.

## 1. 이번 실행 범위

- `benchmark-seed` 전용 프로필 → ApplicationRunner → `.sql` 실행 → 검증·정상 종료.
- 상품 100만 개, 브랜드·사용자 각 1,000개와 실제 좋아요 관계 준비.
- 기존 상품 인덱스 4개 상태의 실제 HTTP SQL·응답·EXPLAIN·EXPLAIN ANALYZE 수집·분석.
- 추가 인덱스 2개와 JMeter 부하 측정은 다음 단계. 기존 API·모델·Projection·쿼리·정렬·페이지 계약 유지.

## 2. 커밋과 실행 순서

### 커밋 1 — 확정 계획 반영

- [x] 전용 프로필·SQL 실행·원샷 종료와 seed 분포를 요구사항·트레이드오프에 반영한다.
- [x] 문서 링크·공백을 검사하고 `docs: 전용 프로필 SQL seed와 변경 전 기준선 계획 반영`으로 커밋한다.

### 커밋 2 — 실행기·seed 구현

- [ ] 전용 프로필을 추가한다. 웹 서버·집계·Redis health 비활성화, 기본 ddl-auto=none, 명시적 전용 DB 연결.
- [ ] ProductSeedRunner는 프로필과 기본 false인 enable 조건으로 격리한다.
- [ ] JdbcProductSeedLoader가 대상 DB·빈 테이블·기존 인덱스를 검사하고 ResourceDatabasePopulator로 INSERT SQL을 실행한다.
- [ ] 적재·검증을 한 TX에서 처리하고 실패 시 rollback한다. 기존 데이터 삭제·TRUNCATE는 제공하지 않는다.
- [ ] main은 run 반환 뒤 seed 프로필에서만 context를 닫는다. 일반 앱 기동은 유지한다.
- [ ] 소량 SQL로 성공·실패 rollback·대상/기존 데이터 거절, 프로필·enable 격리를 검증한다.
- [ ] 관련 검사 후 `feat: 실험용 프로필의 SQL seed 실행기 추가`로 커밋한다.

### 실행·커밋 3 — 실제 변경 전 기준선

- [ ] 기존 loopers와 별도의 새 스키마가 비어 있음을 확인하고 접근 권한을 준비한다.
- [ ] seed를 끈 상태에서 현재 전체 엔티티의 스키마를 한 번 생성한 뒤 ddl-auto=none으로 전환한다.
- [ ] SQL seed를 실행하고 건수·가격·관계·집계·삭제 불변식을 검증한다. 재실행은 거절되는지 확인한다.
- [ ] 기존 상품 인덱스 4개와 새 2개 미적용 상태를 SHOW INDEX로 남긴다.
- [ ] seed 프로필 없이 조회 앱을 띄운다. 집계 비활성화·COUNT TTL 0s·명시적 실험 DB URL을 사용한다.
- [ ] 전체 최신순·전체 가격순·브랜드 7 가격순을 page=0,size=20으로 호출한다. 실제 SQL·바인딩·SQL 수·응답을 저장한다.
- [ ] 통계 갱신 후 각 목록·COUNT SQL의 준비 실행 1회, EXPLAIN·EXPLAIN ANALYZE 1회를 수집한다.
- [ ] key·rows·loops·정렬·시간을 분석하고 baseline/에 실제 자료와 재현 명령을 보존한다.
- [ ] 관련 테스트·Checkstyle·ArchUnit·모듈 check의 건수·실패·skip·종료 결과를 확인한다.
- [ ] 결과·한계·후속 작업을 result.md와 상위 문서에 기록하고 `docs: 상품 100만 개의 인덱스 추가 전 실행 계획 기록`으로 커밋한다.

## 3. 후속 JMeter·인덱스 추가

기준선 확보 뒤 3종 요청 × 동시 1명/10명의 JMeter 파일·초보자 절차와 warm-up·ramp-up·반복 수·timeout을 확정한다.
사용자가 기존 4개 상태에서 측정한 다음 새 인덱스 2개를 적용하고, 같은 조건의 응답·계획·JMeter 결과를 비교한다.
SQL 캡처와 부하 측정은 분리하며, JMeter 결과가 모이기 전에는 전체 성능 개선 검증을 완료로 표시하지 않는다.

## 4. 유지할 경계와 검사

Hibernate DDL은 Runner보다 먼저 실행된다. 최초 schema 생성 전에 실제 대상 URL·빈 스키마를 확인한다.
JDBC 의존은 Jdbc 클래스로 제한하며 일반 업무 쓰기와 JPA 트랜잭션에 fixture 작업을 섞지 않는다.
기존 공유 DB·인덱스·컨테이너를 삭제하지 않는다. 실제 SQL은 전체 Projection·JOIN·삭제 조건·동점 기준을 보존한다.

```powershell
.\gradlew.bat :apps:commerce-api:test --tests '*ProductSeed*Test' --tests '*ProductQuery*Test' --tests '*ProductApiE2ETest'
.\gradlew.bat :apps:commerce-api:check
```

새 테스트의 실제 클래스명에 맞춰 선택하고 테스트 0개를 성공으로 기록하지 않는다.
