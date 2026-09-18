# PR 01 구현 계획: 공통 개발 기반과 사용자 입력

## 목표

기존 PR 01은 PR #4로 volume-2/main에 병합됐다(1971e49). 아래 내용은 최신 합의가 반영된 기반 계약이다.
이번 후속 브랜치는 volume-2/pr-01-query-dao-refactor이며 문서·사용자 조회 DAO·resolver·fixture만 보완한다.
상품 조회와 좋아요 주기 집계는 PR 02 이후 범위로 남긴다.

`volume-2/pr-01-foundation`에서 이후 기능 PR이 공유할 테스트 실행 기반, 정적 검사, 오류 모델,
fixture 사용자와 입력 검증을 완성한다. 공개 HTTP API는 추가하지 않는다.

## 구현 범위

### 테스트 실행 기반

- Gradle daemon은 플랫폼 경로 인코딩을 사용하고 Java 컴파일·테스트 내용은 UTF-8로 고정한다.
- Docker 29에서는 로컬 테스트 리소스 docker-java.properties에 api.version=1.44를 지정한다.
- 이 파일은 기존 .gitignore 정책에 따라 커밋하지 않는다. Docker 실행과 로컬 호환 설정을 준비한 뒤 검사한다.

### 정적 검사

- Checkstyle 10.26.1을 commerce-api main/test에 적용한다.
- `AvoidStarImport`, `UnusedImports`와 경고 0만 우선 강제한다.
- ArchUnit 1.5.0으로 계층 의존 방향과 신규 domain의 Spring·JPA·HTTP 독립성을 검사한다.
- 기존 Example은 계층 검사에 포함하되 신규 domain 순수성 검사에서는 제외한다.

### 오류 모델

- 신규 domain과 application은 각각 HTTP를 모르는 오류 코드와 예외를 사용한다.
- PR 01에서는 `INVALID_USER_ID`, `USER_NOT_FOUND`만 추가한다.
- interfaces가 두 오류를 기존 `ApiResponse`의 400·404 응답으로 변환한다.
- 기존 Example과 `CoreException`은 변경하지 않는다.

### User와 fixture

- Shopping의 User는 ID만 가진 순수 domain 모델이다.
- `users` 테이블은 할당된 ID를 저장하며 local 프로필에서 사용자 `1`, `2`를 없을 때만 생성한다.
- 테스트 fixture는 User 객체를 제공하고 테스트가 저장 시점을 명시한다.
- application의 UserQueryDao.findById가 Optional<UserQueryModel> 계약을 제공한다. 조회 record에는 ID만 둔다.
- infrastructure의 JdbcUserQueryDao는 기존 DataSource와 JdbcClient를 사용하며 공개 조회 메서드는 readOnly다.
- local fixture는 같은 DAO로 사용자 존재를 확인한다. UserRepository.existsById와 UserValidator는 제거한다.
- Point와 초기 잔액은 PR 04에서 연결한다.

### 공통 API 입력

- `@XUserId`와 argument resolver가 헤더 누락·형식·양수·long 범위를 검사한다.
- resolver는 형식 검사 후 UserQueryDao.findById로 존재를 확인한다. 없으면 USER_NOT_FOUND(404), 있으면 ID를 반환한다.
- 형식 오류 시 DAO를 호출하지 않는다. 쓰기 Service는 존재를 재검사하지 않으며 직접 호출자는 존재하는 ID를 제공한다.
- PR 01에서는 resolver 단위 테스트까지만 수행한다. 실제 HTTP 응답과 저장 상태는 첫 사용자 API부터 검증한다.
- commerce-api의 JSON 정수 입력은 문자열·빈 문자열·소수·null 보정을 허용하지 않는다.

## 테스트와 완료 조건

- 기존 테스트 전체 통과
- Checkstyle·ArchUnit·commerce-api `check` 통과
- domain/application 오류의 기본·사용자 지정 메시지와 400·404 매핑 검증
- User 생성·복원·Mapper·저장·조회 DAO·local 초기화 검증
- 실제 DB 조회, fixture 반복 실행과 기존 사용자 보존, resolver 없는 사용자·DAO 미호출 검증
- X-USER-ID 정상·누락·빈 값·문자·소수·공백·0·음수·long 초과 검증
- JSON 정수의 정상값과 문자열·빈 문자열·소수·null·long 초과 검증
- `git diff --check` 통과

## 커밋 순서

`docs: PR 단위 2주차 개발 계획 정리`

- 7개 PR의 범위·순서와 PR 01 상세 계획 반영
- 동시성 제외와 fixture·선행 저장 구조 도입 시점 동기화

`build: 테스트 실행 환경 호환성 보완`

- 한글 경로의 Gradle worker classpath 인코딩 보완
- Docker 29와 Testcontainers의 API 버전 호환

`build: Checkstyle과 ArchUnit 검사 연결`

- import 검사와 계층·domain 순수성 검사 연결
- commerce-api `check`에서 실제 검사 실행

`feat: 계층별 공통 오류 모델 구성`

- domain/application 내부 오류와 interfaces 매핑 추가
- 오류 코드·메시지 변환 테스트 추가

`feat: 학습용 사용자 fixture 구성`

- User domain·저장·local/test fixture 구성
- 기존 UserValidator 도입은 최초 PR 이력이며 후속 보완에서 DAO와 resolver 검증으로 대체

`feat: 공통 API 입력 검증 구성`

- X-USER-ID resolver와 단위 테스트 추가
- JSON 정수 coercion 제한과 단위 테스트 추가

## 제외 범위

- 회원가입·사용자 조회·인증·인가 API
- Point 생성과 초기 잔액
- 테스트 전용 HTTP Controller
- 동시성 제어와 동시 요청 테스트
- 원격 PR 생성·게시·병합

## 이번 보완의 커밋 순서

1. `docs: 조회 DAO와 좋아요 주기 집계 계획 반영`
2. 사용자 조회 DAO를 테스트부터 추가하고 관련 검사 Green 이후 커밋
3. resolver 존재 검사 전환을 테스트부터 추가하고 관련 검사 Green 이후 커밋
4. fixture 조회 전환·UserValidator와 existsById 제거 후 전체 검사 및 커밋

기존 미커밋 설정 변경은 보존하고 이번 커밋에서 제외한다. 검증된 작업 브랜치를 origin에 일반 푸시한다.
기준 브랜치 직접 변경·강제 푸시·PR 생성·병합은 하지 않는다.

## 보완 검증 기록 (2026-09-18)

- UserQueryDao / UserQueryModel / JdbcUserQueryDao를 추가하고 JPA와 동일한 DB에서 사용자 조회를 확인했다.
- resolver의 사용자 존재 검사와 fixture DAO 전환을 완료했다. UserValidator와 domain repository의 existsById는 제거했다.
- 각 변경은 테스트를 먼저 추가해 미구현 타입·생성자의 컴파일 실패(Red)를 확인한 뒤 구현했다.
- DAO 실제 DB 조회, resolver 정상·400·404·잘못된 입력의 DAO 미호출, fixture 반복·기존 사용자 보존이 Green이다.
- 기존 오류 매핑과 도메인·저장 테스트를 유지했고, 제거된 검증 책임은 resolver·DAO 테스트로 이전했다.
- 최종 `./gradlew :apps:commerce-api:check --console=plain` 성공: 테스트 63개, 실패 0, 오류 0, 건너뜀 0.
- Checkstyle main/test와 ArchUnit 계층·현재 구현된 Shopping domain 순수성 검사, git diff --check가 통과했다.
- 최초 DB 테스트는 Docker 연결 단계에서 실패했다. Docker 재기동 및 Git 제외 로컬 API 1.44 설정 후 재검증했다.
- 상품·좋아요 주기 집계·다른 Context 구현은 이번 검증 대상이 아니며 PR 02 이후로 남긴다.
- 원래 수정돼 있던 docker/infra-compose.yml과 modules/jpa/src/main/resources/jpa.yml은 이번 커밋에 포함하지 않았다.
