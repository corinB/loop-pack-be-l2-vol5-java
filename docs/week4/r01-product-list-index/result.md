# R01 진행 결과: 100만 seed와 JMeter 측정 준비

[요구사항](requirement.md) · [계획](plan.md) · [재현 절차](baseline/README.md) · [seed 실행 계획 해석](baseline/seed-explain-review.md)

2026-10-16 사용자 승인 조건으로 **상품 100만 개와 실제 좋아요 관계 적재·검증을 완료**했다.
현재까지 확인한 실행 계획은 seed INSERT의 사전 `EXPLAIN`·`FORMAT=TREE`다.
세 상품 목록 HTTP의 실제 SQL·바인딩·응답을 수집하고 JMeter 각 1건의 사전 확인을 통과했다.
상품 목록의 EXPLAIN·EXPLAIN ANALYZE, 신규 인덱스 적용과 사용자의 JMeter 본 측정은 아직 완료하지 않았다.

## 1. 승인된 적재 실행

| 항목 | 실제 실행 내용 |
|---|---|
| DB | `week4_r01_index_benchmark`, Docker MySQL 8.0.46 |
| 실행 코드·JAR | `98da9d2`, `commerce-api-98da9d2.jar`, Java 21.0.10 |
| 실행기 | `benchmark-seed` 단독 프로필의 `ApplicationRunner` |
| 실행 조건 | `benchmark.seed.enabled=true`, `ddl-auto=none` |
| 트랜잭션 | 제한 3,600초, 전체 적재·관계 및 기대값 검증을 한 TX로 수행 |
| 적재 순서 | 상품·좋아요의 대량 INSERT 3개에 각각 삽입 PK 오름차순 지정 |
| 버퍼 풀 | 실행 전후 `134217728` bytes, 128MiB |
| 시작 | 2026-10-16 13:07:08.708 KST |
| 종료 | 2026-10-16 13:10:19.795 KST |
| 프로세스 전체 경과 | 191.086초 |
| 종료 코드 | 0 |

원문에서 [실행 요약](baseline/seed-run.json), [완료 로그 발췌](baseline/seed-completion.txt),
[설정 전후 대조](baseline/seed-settings.txt)를 저장소 문서에 보존했다.

로그에서 `ProductSeedResult`의 모든 값이 기대값과 일치한 완료 메시지, JPA·Hikari 종료를 확인했다.
실행 전후 대조한 주요 DB 설정값 11개는 일치하며 이번 승인 실행에서는 전역 설정을 변경하지 않았다.
종료 후 남은 InnoDB 트랜잭션은 0개였다.

191.086초는 JVM·Spring 기동, 전체 적재·검증·커밋과 종료를 포함하는 한 번의 프로세스 경과 시간이다.
개별 INSERT의 실행 시간이나 HTTP 지연·처리량을 측정한 값으로 해석하지 않는다.

## 2. 실제 데이터와 인덱스 검증

적재 완료 뒤 [검증 SQL](baseline/verify-data.sql)을 읽기 전용으로 실행했다.
[실제 검증 출력](baseline/seed-verification.txt)에서 아래 값과 모든 불일치 0건을 확인했다.

| 항목 | 실제 결과 |
|---|---:|
| 브랜드 / 삭제 브랜드 | 1,000 / 10 |
| 사용자 / 지갑 / 전체 잔액 | 1,000 / 1,000 / 0 |
| 상품 전체 / 활성 / 삭제 | 1,000,000 / 950,400 / 49,600 |
| 전체 상품 가격 합계 | 50,950,000,000 |
| 실제 좋아요 관계 / 저장된 좋아요 합계 | 2,597,980 / 2,597,980 |
| 좋아요 0개 상품 / 좋아요 100개 상품 | 166,338 / 1,000 |
| 재고 0개 상품 | 100,000 |
| 브랜드 7 전체 / 활성 상품 | 1,000 / 960 |
| 브랜드 7 활성 상품 가격 합계 | 49,004,000 |
| 생성 시각 가짓수 | 500 |

브랜드별 상품 1,000개 조건, 실제 좋아요 중복, 상품·좋아요·지갑의 고아 참조, 저장 좋아요 집계,
삭제 브랜드의 활성 상품, 상품·좋아요 분포 공식 검사는 모두 불일치 0건이었다.
검증 출력의 세션·전역 시간대는 `SYSTEM`이며, 별도 실행 후 설정 확인에서 MySQL 시스템 시간대가 UTC임을 확인했다.

`SHOW INDEX FROM products`는 PRIMARY와 다음 기존 보조 인덱스 4개를 반환했다. 새 전체 최신순·가격순 인덱스는 적용하지 않았다.

- `idx_products_deleted_brand_created`: `deleted, brand_id, created_at DESC, id DESC`
- `idx_products_deleted_brand_price`: `deleted, brand_id, price ASC, id DESC`
- `idx_products_deleted_like`: `deleted, like_count DESC, id DESC`
- `idx_products_deleted_brand_like`: `deleted, brand_id, like_count DESC, id DESC`

## 3. 사전 실행 계획과 검사 범위

적재 전에 [빈 테이블·기존 인덱스·128MiB 사전 상태](baseline/seed-preflight.txt)를 확인하고,
[INSERT 6개에 대한 EXPLAIN SQL](baseline/seed-explain-before.sql)과 [계획 원문](baseline/seed-explain-before.txt)을 보존했다.
사전 검토에서는 INSERT·통계 갱신·`EXPLAIN ANALYZE`를 실행하지 않았다.

상품은 숫자 CTE 1,000개와 브랜드를 조합하고, 좋아요는 상품 인덱스를 읽어 관계 후보를 만든다.
대량 INSERT 3개의 정렬은 PK순 삽입을 위한 비용이며, 각 좋아요 INSERT 사이의 전체 PK 순서까지 연속인 것은 아니다.
이전 적재·롤백의 통계가 남은 상태여서 사전 계획의 예상 rows를 실제 건수로 사용하지 않는다.
이 원문에는 실제 rows·loops·단계별 실행 시간이 없으므로 상품 목록의 조회 개선 근거로 해석하지 않는다.

현재 실행 코드와 같은 소스의 `:apps:commerce-api:check`는 앞서 통과했다.
테스트 XML 합계는 **284건, 실패 0, 오류 0, skip 0**이며 Checkstyle·ArchUnit을 포함하는 모듈 검사 결과다.
이는 seed의 프로세스 경과 시간과 별도의 사전 코드 검증이며, 실제 100만 상품 적재 시간에 포함하지 않는다.
소량 통합 테스트는 SQL 실패·검증 실패 시 전체 rollback, 잘못된 대상 DB·기존 데이터·추가 인덱스 거절을 다룬다.
적재 후 같은 실행기를 호출했을 때 INSERT 전에 `brands`의 기존 데이터를 감지해 종료 코드 1로 거절했다.
[차단 후 검증](baseline/seed-existing-data-guard.txt)에서 5개 테이블의 건수·가격 합계·좋아요 합계와 128MiB 버퍼 풀을 유지했다.

## 4. 이전 시도와 남은 작업

첫 600초 제한 실행은 검증 단계에서 시간 초과로 실패했고 전체 rollback 뒤 5개 대상 테이블이 비어 있음을 확인했다.
이후 승인되지 않은 버퍼 풀 1GiB 변경·재실행은 사용자 지적으로 중단했다.
버퍼 풀을 원래 128MiB로 되돌리고 백그라운드 rollback 종료·테이블 0건을 확인한 뒤,
INSERT 계획을 설명하고 새로 명시적인 승인을 받아 이번 적재를 실행했다.
실패·중단 시도는 이번 성공 실행과 분리하며, 실행 시간의 개선 배수나 버퍼 풀 변경의 성능 효과를 주장하지 않는다.

현재 같은 데이터·기존 인덱스 상태에서 조회 앱과 SQL·응답 기준값을 준비했다.
목록 SQL의 통계 갱신·EXPLAIN·EXPLAIN ANALYZE, 사용자 본 측정, 새 인덱스 2개 적용과 전후 비교는 남아 있다.

## 5. JMeter 직접 실행 준비와 확인 범위

사용자는 [직접 실행 가이드](jmeter-guide.md)의 명령으로 본 측정을 실행한다.
최종 선택은 **3종 조회 × 동시 1명/10명 × 2회 반복**, 각 run의 워밍업 20초와 본 측정 60초다.
기존 3회 반복에서 시간을 줄여 본 측정은 12개, 설정 시간 16분·실제 예상 약 17~20분으로 준비했다.
각 실행의 ramp-up 5초는 실행 시간 안에 포함한다.

| 파일 | 역할 |
|---|---|
| [JMX](jmeter/product-list.jmx) | 시나리오별 단일 HTTP 요청, 안전한 기본 1명·1회, full/light 검증 |
| [기대 응답](jmeter/expected-responses.json) | seed 공식을 독립 계산한 3개 시나리오의 전체 응답 |
| [시작](jmeter/start-benchmark-api.ps1)·[종료](jmeter/stop-benchmark-api.ps1) | 전용 DB·ddl-auto=none·집계 false·COUNT TTL 0s, 기록한 API 프로세스만 관리 |
| [실행 래퍼](jmeter/run-jmeter.ps1) | 사용자 실행, 실행 조건 검사·워밍업/측정 분리·JTL/HTML·manifest 보존 |
| [결과 저장 속성](jmeter/results.properties) | CSV JTL과 오류 메시지 저장, 전체 응답 저장 비활성화 |
| [배포판 정보](jmeter/distribution.json) | 루트의 JMeter 5.6.3과 공식 ZIP·SHA-512 기록 |

실제로 실행한 확인은 다음과 같다.

- SQL 로그를 켠 앱에 세 GET을 각 한 번 호출했다. [HTTP 원문](baseline/http-before/README.md)에서 모두 HTTP 200,
  목록 1개·COUNT 1개로 요청당 2 SQL을 확인했다. COUNT TTL은 0s다.
- 응답 60개 항목의 모든 필드·정렬과 페이지를 독립 기대값과 대조해 일치했다.
- 이 SQL 수집 앱만 종료하고 SQL·바인딩 로그 OFF인 앱으로 다시 기동했다.
- 같은 실행 래퍼의 Smoke 모드로 세 시나리오를 1명·1회, 총 3요청 실행했다.
  모두 full 검증·JTL 실패 0건·HTML 생성을 통과했다. [요약·원본](baseline/jmeter-smoke/README.md)을 보존했다.
- 사전 확인 전후 데이터·기존 인덱스 4개·버퍼 풀 128MiB와 주요 설정 11개가 같았다.
- PowerShell 스크립트·XML·JSON은 정적으로 검사했다. 프로세스 생성 시각은 JSON 날짜 변환을 고려해 UTC ticks로 대조한다.

**본 측정은 0회이며 mock 테스트도 실행하지 않았다.** 시간 기반 루프와 전체 조합은 코드 검토만 수행했다.
단일 사전 표본으로 지연 분포·처리량·개선 배수를 보고하지 않는다.
사용자가 실행 여부를 확인한 이후에는 추가 JMeter 실행 없이 파일과 안내만 마무리했다.
자동화 스크립트는 실패 표본·0표본·도구 오류에서 원본을 남기고 중단한다.

### Windows PowerShell 5.1 한글 경로 보완

사용자 실행에서 `LiteralPath`가 null이라는 오류가 발생했다. Windows PowerShell 5.1의 기본 읽기 인코딩이
한국어 ANSI여서, PowerShell 7이 BOM 없는 UTF-8로 기록한 경로 파일의 `바탕 화면`이 깨지는 것을 재현했다.
metadata JSON을 기본 인코딩으로 읽는 부분도 같은 영향을 받았다. 이 시도는 API 사전 검사 전에 실패했다.

가이드의 경로 파일 읽기와 실행·종료 스크립트의 JSON 읽기에 `-Encoding UTF8`을 명시했다.
세 PowerShell 스크립트는 UTF-8 BOM으로 저장해 5.1에서도 한글 코드 문자열을 보존하고,
잘못 읽은 경로는 파일 존재 검사에서 원인을 안내하도록 보완했다.
PowerShell 5.1.26100과 7.6.5에서 수정한 경로·metadata·API 프로세스 사전 검사 부분을 분리해 통과를 확인했다.
이 검증에서는 Java·JMeter·HTTP·DB 작업을 실행하지 않았다.
