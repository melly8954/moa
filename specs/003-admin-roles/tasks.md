---

description: "운영자 역할 (admin-roles) 작업 목록"
---

# Tasks: 운영자 역할 (admin-roles)

**Input**: `specs/003-admin-roles/`의 설계 문서

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/admin-roles.md

**Tests**: 원칙 II에 따라 AC마다 테스트를 하나 둔다. 동작은 키트에 이미 있으므로 이 기능의 작업은 테스트가 전부다 (research.md R1). 운영 코드, 프론트, 마이그레이션은 고치지 않는다.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: 병렬 가능 (다른 파일, 의존 없음)
- **[Story]**: 작업이 속한 사용자 스토리

## Phase 1: Setup

**Purpose**: 메뉴 처음 구성 복원을 001과 함께 쓰게 하고, 테스트 클래스를 준비한다 (research.md R3)

- [X] T001 backend/src/test/java/com/moa/support/MenuSeed.java에 `@Component` `MenuSeed`를 만든다. `restore()`는 `delete from menus`로 표를 비운 뒤 `classpath:db/migration/V1_7__menus.sql`, `classpath:db/migration/V1_7_1__admin_menus.sql`을 이 순서로 읽어 `--` 줄 주석을 뺀 문 중 INSERT 문만 실행한다. 지금 backend/src/test/java/com/moa/api/AdminMenusAcceptanceTest.java의 `restoreInitialMenus`·`insertStatementsOf`와 같은 동작이다
- [X] T002 [P] backend/src/test/java/com/moa/api/AdminMenusAcceptanceTest.java의 `@BeforeEach`가 `MenuSeed.restore()`를 부르게 바꾸고, 옮겨 간 `insertStatementsOf`와 쓰지 않게 된 import·상수를 지운다. AC 테스트 메서드의 확인 내용은 바꾸지 않는다 (원칙 V)
- [X] T003 [P] `AcceptanceTest`를 상속한 `AdminRolesAcceptanceTest`를 backend/src/test/java/com/moa/api/AdminRolesAcceptanceTest.java에 만든다. 클래스 주석에 스펙 `003-admin-roles`를 적는다. `MenuSeed`, `TestUsers`를 주입한다
- [X] T004 backend/에서 `./gradlew dbTest --tests "*AdminMenusAcceptanceTest"`를 돌려 001 AC 5개가 그대로 통과하는지 확인한다

---

## Phase 2: Foundational

**Purpose**: 세 AC가 함께 쓰는 "접속 이력만 보는 운영자" 준비 (contracts/admin-roles.md "준비")

**⚠️ CRITICAL**: US1의 모든 AC가 이 작업에 기댄다

- [X] T005 backend/src/test/java/com/moa/api/AdminRolesAcceptanceTest.java의 `@BeforeEach`에서 `menuSeed.restore()`를 부르고, `testUsers.create`로 시스템 계정(1L), 관리자(2L, `Role.ADMIN`), 운영자(10L, `Role.USER`)를 만든다. `bearer(2L, "ADMIN")`으로 `POST /api/v1/admin/roles`를 불러 역할을 만든다 (코드는 "영문 대문자로 시작하고 영문 대문자·숫자·`_` 50자 이하" 예: `SECURITY`, 이름은 "필수, 50자 이하" 예: 보안 담당, `permissions` = `["LOGIN_HISTORY_READ"]`). 201과 응답의 `id`를 확인한 뒤 `PUT /api/v1/admin/users/10/roles`(`roleIds` = 그 id 하나)로 운영자에게 주고 성공(2xx)을 확인한다. 운영자 요청은 `bearer(10L, "USER")`로 만든다

**Checkpoint**: 테스트마다 처음 메뉴 구성과 "접속 이력만 보는 운영자"로 시작한다

---

## Phase 3: User Story 1 - 운영자는 받은 역할의 메뉴와 기능만 쓴다 (Priority: P2) 🎯 MVP

**Goal**: 역할로 권한 일부만 받은 운영자가 그 메뉴와 기능만 쓸 수 있음을 고정한다

**Independent Test**: `./gradlew dbTest --tests "*AdminRolesAcceptanceTest"`에서 US1-AC1~AC3이 통과한다

- [X] T006 [US1] `@DisplayName("US1-AC1 ...")` 테스트를 backend/src/test/java/com/moa/api/AdminRolesAcceptanceTest.java에 더한다. 운영자로 `GET /api/v1/menus/me`를 불러 200, 최상위 항목이 "접속 이력" 하나임을 확인한다 (메뉴 최상위 배치는 `001-admin-menus` FR-001)
- [X] T007 [US1] `@DisplayName("US1-AC2 ...")` 테스트를 backend/src/test/java/com/moa/api/AdminRolesAcceptanceTest.java에 더한다. 운영자로 `GET /api/v1/admin/login-histories`를 불러 200임을 확인한다
- [X] T008 [US1] `@DisplayName("US1-AC3 ...")` 테스트를 backend/src/test/java/com/moa/api/AdminRolesAcceptanceTest.java에 더한다. 운영자로 `GET /api/v1/admin/users`를 불러 403, `$.code` = `A005`임을 확인한다

**Checkpoint**: US1이 검증된다

---

## Phase 4: Polish

**Purpose**: 전체 검사와 문서 정합성

- [X] T009 backend/에서 `./gradlew spotlessApply`, `./gradlew build dbTest`를 돌려 통과를 확인한다. 테스트가 실패하면 테스트를 고치지 말고 spec.md의 AC부터 확인한다 (원칙 V)
- [X] T010 specs/README.md 기능 지도에서 `003-admin-roles`의 상태를 `기획 중`에서 `구현됨`으로 바꾼다
- [ ] T011 specs/003-admin-roles/quickstart.md의 "화면 확인" 절차를 사람이 직접 수행한다. 시험 계정 비밀번호가 필요하므로 에이전트는 하지 않는다 (원칙 VI)

---

## Dependencies & Execution Order

- T001 → T002·T003 → T004 → T005 → T006~T008 → T009 → T010, T011
- T002와 T003은 다른 파일이라 함께 진행할 수 있다
- T004가 실패하면 T002를 되돌리고 T001부터 다시 본다. 001 테스트의 확인 내용을 고쳐 맞추지 않는다

## Parallel Example

```text
T001이 끝난 뒤:
  T002 AdminMenusAcceptanceTest.java를 MenuSeed로 바꾸기
  T003 AdminRolesAcceptanceTest.java 만들기
```

## Implementation Strategy

1. T001~T004로 시드 복원을 공유하고 001이 그대로인지 확인한다
2. T005~T008로 US1을 검증한다 (MVP이자 이 기능의 전부)
3. T009~T011로 마무리하고 구현 PR을 연다
