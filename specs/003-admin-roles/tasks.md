---

description: "운영자 역할 (admin-roles) 작업 목록"
---

# Tasks: 운영자 역할 (admin-roles)

**Input**: `specs/003-admin-roles/`의 설계 문서

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/admin-roles.md

**Tests**: 원칙 II에 따라 AC마다 테스트를 하나 둔다. 동작은 키트에 이미 있으므로 이 기능의 작업은 테스트가 전부다 (research.md R1). 운영 코드, 프론트, 마이그레이션은 고치지 않는다. 단 Phase 5는 키트 rbac 헬퍼(백엔드·프론트)를 고친다(마이그레이션은 없다).

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

## Phase 5: 메뉴 트리와 사용자 화면 (2026-11-15 FR-004~FR-008 변경)

**Purpose**: 역할 창에서 관리 메뉴를 체크해 권한을 묶고, 역할 관리에서 사용자별 역할과 구성원을 다룬다 (research.md R6~R11). `001-admin-menus` Phase 10(프로그램)이 끝난 뒤 한다. 구현 브랜치는 `feat/003-role-users`

**백엔드**

- [ ] T012 backend/src/main/java/com/moa/api/application/v1/role/AccessRoleApplication.java에 권한 트리 만들기(관리 메뉴 트리, 꺼진 메뉴 포함, 없는 프로그램 메뉴는 뺌, 메뉴에 없는 관리 프로그램)를 더하고 backend/src/main/java/com/moa/api/controller/v1/admin/role/ApiAdminRoleController.java에 `GET /api/v1/admin/roles/permission-tree`와 응답을 둔다 (R6)
- [ ] T013 사용자별 역할 목록: backend/src/main/java/com/moa/api/repository/reader/에 업무 역할이 있거나 `ADMIN`인 사용자(시스템 계정 제외), 아이디·이름 검색, 아이디 순 페이지 조회를 더하고 `GET /api/v1/admin/roles/users`와 응답(`id`, `loginId`, `name`, `role`, `accessRoles`)을 둔다 (R7)
- [ ] T014 구성원: backend/src/main/java/com/moa/api/service/role/AccessRoleService.java에 구성원 조회·더하기(있으면 그대로)·빼기를 더하고(`RoleGrantPolicy.canManage`로 `A005`, 대상이 `ADMIN`이면 `B001`, 없는 역할·사용자 `R001`), `replaceRolesOfUser`도 대상이 `ADMIN`이면 `B001`로 막는다. 컨트롤러에 `GET`·`POST /api/v1/admin/roles/{id}/members`, `DELETE /api/v1/admin/roles/{id}/members/{userId}`를 둔다 (R8)
- [ ] T015 backend/src/test/java/com/moa/api/AdminRolesAcceptanceTest.java에 `US2-AC1`~`US4-AC3`과 Edge(관리자에게 역할 주기 `B001`, 다룰 수 없는 역할의 구성원 더하기 `A005`)를 contracts/admin-roles.md 표대로 더한다. 키트 rbac 테스트 중 `ADMIN`에게 업무 역할을 주는 것이 있으면 스펙이 바뀐 만큼만 고친다 (원칙 V)

**프론트**

- [ ] T016 frontend/src/types/role.ts, frontend/src/api/role.ts에 트리·사용자별 목록·구성원을 더한다. local 백엔드를 띄울 수 있으면 `npm run gen:api`로 생성 타입도 맞춘다
- [ ] T017 frontend/src/app/(admin)/admin/roles/role-dialog.tsx의 권한 칸을 메뉴 트리 체크로 바꾼다(묶음 체크는 하위 모두, 같은 권한 노드는 함께, 꺼진 메뉴 표시, 키는 작은 글자, "메뉴에 없는 프로그램" 묶음) (R10)
- [ ] T018 frontend/src/app/(admin)/admin/roles/page.tsx에 "역할"·"사용자별 역할" 탭을 두고, 역할 표에 "구성원" 단추와 members-dialog.tsx(목록, 빼기, 사용자 찾아 더하기)를, user-roles-tab.tsx(검색, 표, `ADMIN`은 "모든 권한", "역할 변경" 대화상자)를 만든다. 역할 체크 폼은 frontend/src/components/role/user-roles.tsx에서 옮긴다. 문구는 frontend/messages/ko.json `role` (R10)
- [ ] T019 frontend/src/app/(admin)/admin/users/user-extras.tsx에서 업무 역할 메뉴와 대화상자를 빼고, 옮긴 뒤 쓰지 않는 frontend/src/components/role/user-roles.tsx를 지운다 (R9, `002-admin-users` FR-005)
- [ ] T020 frontend/e2e/roles.spec.ts(없으면 만든다)에 역할 대화상자 트리 체크(키 입력 없음), 두 탭, 구성원 대화상자, 사용자 관리 "관리" 메뉴에 역할 항목 없음을 더한다. 사용자 관리의 역할 주기를 보던 e2e가 있으면 바뀐 만큼 고친다 (R11)

**마무리**

- [ ] T021 backend/에서 `./gradlew spotlessApply`, `./gradlew build`, `./gradlew dbTest --tests "*AdminRolesAcceptanceTest" --tests "*AccessRole*"`, frontend/에서 `npm run format`, `npm run check`, 바꾼 e2e 묶음(roles·users)을 돌린다. 전체 검사는 PR CI로 본다
- [ ] T022 specs/README.md 기능 지도의 003 상태에서 "메뉴 트리·사용자 화면은 기획 중"을 지운다
- [ ] T023 specs/003-admin-roles/quickstart.md "화면 확인" 2·3번을 사람이 직접 확인한다

---

## Dependencies & Execution Order

- T001 → T002·T003 → T004 → T005 → T006~T008 → T009 → T010, T011
- Phase 5: `001-admin-menus` T052~T060 → T012 → T013, T014 → T015 → T016 → T017, T018 → T019 → T020 → T021 → T022, T023. T013·T014는 다른 파일이 섞이지 않으면 나란히 해도 된다
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
