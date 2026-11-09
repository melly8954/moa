---

description: "관리자·운영자 계정 (admin-users) 작업 목록"
---

# Tasks: 관리자·운영자 계정 (admin-users)

**Input**: `specs/002-admin-users/`의 설계 문서

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/admin-accounts.md

**Tests**: 원칙 II에 따라 AC마다 테스트를 하나 둔다. 동작은 키트에 이미 있으므로 이 기능의 작업은 테스트가 전부다 (research.md R1). 운영 코드, 프론트, 마이그레이션은 고치지 않는다.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: 병렬 가능 (다른 파일, 의존 없음)
- **[Story]**: 작업이 속한 사용자 스토리

모든 테스트는 한 파일에 들어가므로 [P] 작업은 없다.

## Phase 1: Setup

**Purpose**: 테스트 클래스 준비

- [X] T001 `AcceptanceTest`를 상속한 `AdminUsersAcceptanceTest`를 backend/src/test/java/com/moa/api/AdminUsersAcceptanceTest.java에 만든다. 클래스 주석에 스펙 `002-admin-users`를 적는다. `TestUsers`, `UserApplication`, `DatabaseCleaner`를 주입하고, `POST /api/v1/auth/login`을 부르는 `login(loginId, password)`와 응답 본문에서 JSON 값을 읽는 도우미를 `UserAdminAcceptanceTest`와 같은 모양으로 둔다. `@BeforeEach`에서 `databaseCleaner.clean()`을 불러 로컬 계정 SQL의 `admin`이 남지 않게 한다. 키트 테스트는 고치지 않는다

---

## Phase 2: Foundational

공통 선행 작업은 없다. 두 스토리는 각자 필요한 계정을 테스트 안에서 만든다.

---

## Phase 3: User Story 1 - 첫 배포 때 관리자가 생긴다 (Priority: P1) 🎯 MVP

**Goal**: 첫 관리자가 임시 비밀번호로 로그인해 비밀번호를 바꾸고 백오피스에 들어가는 흐름을 고정한다

**Independent Test**: `./gradlew dbTest --tests "*AdminUsersAcceptanceTest"`에서 US1-AC1이 통과한다

- [X] T002 [US1] `@DisplayName("US1-AC1 ...")` 테스트를 backend/src/test/java/com/moa/api/AdminUsersAcceptanceTest.java에 더한다. `SystemActor.call(() -> userApplication.bootstrapAdmin(아이디, 임시 비밀번호, null))`로 첫 관리자를 만들고 (아이디는 "영문·숫자·`.`·`_`·`-` 3~50자", 비밀번호는 "8자 이상"을 지키는 테스트 값), contracts/admin-accounts.md "첫 관리자"의 7단계를 순서대로 확인한다: 로그인 200, `passwordChangeRequired` true, `PUT /api/v1/users/me/password` 204, 새 비밀번호 로그인 200, 새 토큰으로 `passwordChangeRequired` false, 새 토큰으로 `GET /api/v1/admin/users` 200

**Checkpoint**: US1이 단독으로 검증된다

---

## Phase 4: User Story 2 - 관리자가 운영자 계정을 만든다 (Priority: P2)

**Goal**: 관리자가 만든 운영자 계정의 임시 비밀번호가 한 번만 보이고, 첫 로그인 때 변경 요구가 있음을 고정한다

**Independent Test**: `./gradlew dbTest --tests "*AdminUsersAcceptanceTest"`에서 US2-AC1이 통과한다

- [X] T003 [US2] `@DisplayName("US2-AC1 ...")` 테스트를 backend/src/test/java/com/moa/api/AdminUsersAcceptanceTest.java에 더한다. `testUsers.create`로 시스템 계정(1L)과 관리자(2L, `Role.ADMIN`)를 만들고, `bearer(2L, "ADMIN")`으로 contracts/admin-accounts.md "운영자 계정"의 4단계를 확인한다: `POST /api/v1/admin/users`(`role` = `USER`, 이름은 "필수, 50자 이하") 201과 `$.temporaryPassword` 존재, `GET /api/v1/admin/users/{id}` 200과 `$.temporaryPassword` 없음, 임시 비밀번호 로그인 200, 그 토큰의 `passwordChangeRequired` true

**Checkpoint**: US1과 US2가 모두 검증된다

---

## Phase 5: Polish

**Purpose**: 전체 검사와 문서 정합성

- [X] T004 backend/에서 `./gradlew spotlessApply`, `./gradlew build dbTest`를 돌려 통과를 확인한다. 테스트가 실패하면 테스트를 고치지 말고 spec.md의 AC부터 확인한다 (원칙 V)
- [X] T005 specs/README.md 기능 지도에서 `002-admin-users`의 상태를 `기획 중`에서 `구현됨`으로 바꾼다
- [ ] T006 specs/002-admin-users/quickstart.md의 "화면 확인" 절차를 사람이 직접 수행한다. 시험 계정 비밀번호가 필요하므로 에이전트는 하지 않는다 (원칙 VI)

---

## Dependencies & Execution Order

- T001 → US1(T002)·US2(T003) → T004 → T005, T006
- US1과 US2는 서로 기대지 않는다
- 같은 파일을 고치므로 병렬로 진행하지 않는다

## Parallel Example

없음. 모든 테스트가 `AdminUsersAcceptanceTest.java` 한 파일에 있다.

## Implementation Strategy

1. T001~T002로 첫 관리자 흐름을 검증한다 (MVP)
2. T003으로 운영자 계정 흐름을 검증한다
3. T004~T006으로 마무리하고 구현 PR을 연다
