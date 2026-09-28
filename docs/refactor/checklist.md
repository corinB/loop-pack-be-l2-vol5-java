# 패키지 구조 리팩토링 체크리스트

[계획](plan.md) · [결정 기록](context-notes.md)

## 커밋 0 — 문서
- [x] plan.md · checklist.md · context-notes.md 작성
- [x] `docs: 패키지 구조 리팩토링 계획과 결정 사항 정리` 커밋

## 커밋 1 — domain
- [ ] main 파일을 `domain.<ctx>.{model,repository,policy}`로 `git mv`
- [ ] 테스트를 대상 클래스와 같은 패키지로 `git mv`
- [ ] package 선언과 모든 참조 import 갱신 (application·infrastructure·interfaces·테스트 포함)
- [ ] 기존 feature 폴더(main·test) 제거 확인
- [ ] 컴파일 + Checkstyle + ArchUnit 통과
- [ ] 기존 패키지명 `git grep` 잔여 0건
- [ ] `refactor: domain 패키지를 컨텍스트·종류 구조로 재배치` 커밋

## 커밋 2 — infrastructure
- [ ] main 파일을 `infrastructure.{persistence,query,dao,scheduler,initializer}.<ctx>`로 `git mv`
- [ ] `UserJpaRepository`만 `public`으로 변경 (그 외 접근제어 변경 없음)
- [ ] 테스트를 매핑표대로 `git mv`
- [ ] package 선언과 import 갱신
- [ ] 기존 feature 폴더(main·test) 제거 확인
- [ ] 컴파일 + Checkstyle + ArchUnit 통과
- [ ] 기존 패키지명 `git grep` 잔여 0건
- [ ] `refactor: infrastructure 패키지를 종류·컨텍스트 구조로 재배치` 커밋

## 커밋 3 — application
- [ ] main 파일을 `application.<ctx>.{usecase,service,command,result,query,dao}`로 `git mv`
- [ ] 조회 모델 7개 이름을 `*View`로 변경 (파일명·선언·참조)
- [ ] 테스트를 `application.<ctx>.service`로 `git mv`
- [ ] package 선언과 infrastructure·interfaces·테스트의 import 갱신
- [ ] 기존 feature 폴더(main·test) 제거 확인
- [ ] 컴파일 + Checkstyle + ArchUnit 통과
- [ ] 기존 패키지명·기존 타입명 `git grep` 잔여 0건
- [ ] `refactor: application 패키지를 컨텍스트·종류 구조로 재배치하고 조회 모델명을 View로 통일` 커밋

## 커밋 4 — interfaces
- [ ] main 파일을 `interfaces.api.<ctx>.{controller,dto}`로 `git mv`
- [ ] E2E 테스트를 `interfaces.api.<ctx>.controller`로 `git mv`
- [ ] package 선언과 import 갱신
- [ ] 기존 feature 폴더(main·test) 제거 확인
- [ ] 컴파일 + Checkstyle + ArchUnit 통과
- [ ] 기존 패키지명 `git grep` 잔여 0건
- [ ] `refactor: interfaces 패키지를 컨텍스트·종류 구조로 재배치` 커밋

## 커밋 5 — 결과 문서
- [ ] `./gradlew :apps:commerce-api:check` 전체 통과 (Docker 필요)
- [ ] result.md 작성 (실제 결과·계획과의 차이·검증 수치)
- [ ] CLAUDE.md "commerce-api package structure" 설명 갱신
- [ ] AGENTS.md 32행 패키지 규칙 갱신
- [ ] `docs: 패키지 리팩토링 결과와 구조 설명 갱신` 커밋
