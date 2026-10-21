# 결제 보존 실험 실행 환경

테스트 PG와 commerce는 **별도 Java 프로세스**로 실행한다. MySQL 서버는 공유하지만 PG는 `loopers_test_pg`, commerce는 기존 `loopers` 스키마를 사용한다.

## 최초 준비

저장소 루트에서 실행한다. 기존 MySQL 볼륨은 그대로 사용하며 아래 명령은 테이블을 삭제하지 않는다.

```powershell
docker compose -f docker/infra-compose.yml up -d mysql redis-master redis-readonly
Get-Content -Raw -Encoding utf8 docker/payment-lab/001-pg-schema.sql |
    docker compose -f docker/infra-compose.yml exec -T mysql mysql -uroot -proot --default-character-set=utf8mb4
```

거래·결제 모델을 추가하는 커밋에서 필요한 테이블 SQL을 순서대로 추가한다. 앱은 SQL을 자동 실행하지 않으며, 필요한 SQL을 직접 적용한 뒤 실행한다.

PG 거래 테이블은 스키마 준비 다음에 적용한다.

```powershell
Get-Content -Raw -Encoding utf8 docker/payment-lab/002-pg-transactions.sql |
    docker compose -f docker/infra-compose.yml exec -T mysql mysql -uroot -proot --default-character-set=utf8mb4
```

## 실행

각각 다른 터미널에서 실행한다.

```powershell
./gradlew.bat :apps:test-pg:bootRun
./gradlew.bat :apps:commerce-api:bootRun --args='--spring.profiles.active=payment-lab'
```

PG 주소는 `http://127.0.0.1:8081`, commerce 기본 포트는 8080이다. PG에는 commerce 실행 클래스·Redis 의존성이 없다. PG는 기본으로 `payment-lab`을 사용한다.

commerce의 `payment-lab` 실행은 기존 commerce 테이블이 준비되어 있어야 한다. `local`을 함께 활성화하지 않는다. 기존 `local`/`test`의 격리용 테이블 재생성 설정은 그대로 유지한다.

## 재시작과 초기화

두 앱의 `payment-lab`은 `ddl-auto=validate`를 사용한다. 재시작은 테이블을 지우거나 만들지 않고, 기존 스키마가 코드와 맞는지만 검사한다.

보존 실험에서는 프로세스만 종료·재시작한다. `docker compose down -v`는 사용하지 않는다. 데이터 초기화가 필요하면 초기화 대상과 SQL을 직접 확인해 별도로 실행한다. 현재 준비 SQL에는 삭제 명령이 없다.

접속 설정을 바꿀 때 PG는 `PG_MYSQL_HOST/PORT/USER/PASSWORD`, commerce는 `COMMERCE_MYSQL_HOST/PORT/USER/PASSWORD` 환경 변수를 사용한다. 기본 계정은 로컬 학습용 `test_pg/test_pg`, `application/application`이다.
