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

## Phase 6: 관리 화면 정리 (2026-11-15 FR-006~FR-008, US3 변경)

**Purpose**: 업무 역할 열과 사용자 드로어 (research.md R6~R8). `001-admin-menus` T071, `003-admin-roles` T024 뒤에 한다

- [x] T007 backend/src/main/java/com/moa/api/controller/v1/admin/user/response/UserAdminResponse.java에 `accessRoles`(`id`, `name`)를 더하고 backend/src/main/java/com/moa/api/application/v1/user/UserAdminApplication.java의 목록이 `RoleUserService`의 업무 역할 조회를 한 번 불러 붙인다 (R6)
- [x] T008 backend/src/test/java/com/moa/api/AdminUsersAcceptanceTest.java에 US3-AC1(목록 응답의 `accessRoles`)을 더한다
- [x] T009 frontend/src/app/(admin)/admin/users/: 업무 역할 열(ADMIN "모든 권한"), 행을 누르면 사용자 드로어(정보, 업무 역할 보기만 + 역할 관리 링크, 이름 바꾸기·시스템 역할·잠그기·초기화·삭제 단추, 막는 규칙 그대로), 만들기도 드로어(저장 직후 임시 비밀번호 한 번), 행 "관리" 메뉴 단추 없앰, user-extras 자리를 드로어 단추로. 머리 안내(FR-008 문구, 역할 관리 링크). frontend/src/types/user-admin.ts에 `accessRoles` (R7)
- [x] T010 frontend/e2e/user-admin.spec.ts를 R8대로 고친다 (원칙 V)
- [x] T011 backend/에서 `./gradlew spotlessApply`, `./gradlew build`, `./gradlew dbTest --tests "*AdminUsersAcceptanceTest" --tests "*UserAdminAcceptanceTest"`, frontend/에서 `npm run format`, `npm run check`. e2e와 dbTest 전체는 PR CI로 본다. (2026-11-16: 빠른 검사만 로컬에서 하고, 인수 테스트도 CLAUDE.md대로 PR CI로 본다)
- [ ] T012 specs/002-admin-users/quickstart.md "화면 확인"에 드로어·업무 역할 열 절차를 더하고(5~7 더함) 사람이 확인한다

---

## Phase 7: 관리자 계정과 드로어 역할 주기 (2026-11-21 FR-001·FR-003·FR-005~FR-008, US1~US4 변경)

**Purpose**: 계정 종류(회원·관리자), 드로어 역할 체크, 내 권한 안쪽 계정만 다루기 (research.md R9~R12). `003-admin-roles` Phase 7과 한 구현 브랜치 `feat/003-admin-accounts`에서, 그 백엔드(T033~T038) 뒤에 한다

**백엔드**

- [X] T013 backend/src/main/java/com/moa/api/service/user/UserAdminService.java: `create`·`changeRole`의 "ADMIN만"을 없애고, `ADMIN` → `USER`면 역할을 모두 빼고 토큰을 끊는다. `requireManageable`을 최고 관리자 대상은 최고 관리자만, `ADMIN` 대상은 권한이 내 안쪽일 때만으로 바꾸고, 마지막 최고 관리자를 잠그기·지우기·회원으로 바꾸기 B001 (R9)
- [X] T014 backend/src/main/java/com/moa/api/controller/v1/admin/user/response/UserAdminResponse.java에 `superAdmin`·`protection`을 더하고 backend/src/main/java/com/moa/api/application/v1/user/UserAdminApplication.java의 목록·한 사람이 한 번에 모아 붙인다. 응답 설명의 "ADMIN은 accessRoles가 빈다"를 고친다 (R10)

**프론트**

- [X] T015 frontend/src/app/(admin)/admin/users/user-drawer.tsx: 위 칸(계정 종류), 관리자 계정의 역할 칸(권한 관리 권한이 있으면 체크 목록·이유·`permissionNames`·역할 저장, 없으면 이름만), 회원 안내, `protection`이면 단추 끄고 이유. user-dialogs.tsx의 계정 종류 바꾸기에 빠질 역할 확인, `CreateUserDrawer`의 `canGrantAdmin` 없앰. frontend/src/types/user-admin.ts에 `superAdmin`·`protection` (R11)
- [X] T016 frontend/src/app/(admin)/admin/users/page.tsx: 역할 열(최고 관리자·역할 없음·이름들·빈 칸), `protectedRow`·`iAmAdmin`을 `protection`으로 바꾼다. frontend/messages/ko.json `userAdmin`의 계정 종류·회원·역할·머리 안내(FR-008) 문구, 쓰지 않는 키 지우기 (R11)

**테스트 (마지막)**

- [X] T017 backend/src/test/java/com/moa/api/AdminUsersAcceptanceTest.java를 contracts/admin-accounts.md "관리자 계정과 다루기 범위" 표대로 고치고 더한다. 운영자는 `ADMIN` 계정으로 만든다. backend/src/test/java/com/moa/api/UserAdminAcceptanceTest.java `adminProtection`을 R9 규칙으로 바꾸고 PR 설명에 적는다 (원칙 V, R12)
- [X] T018 frontend/e2e/user-admin.spec.ts를 R12대로 고친다 (원칙 V)
- [ ] T019 specs/002-admin-users/quickstart.md "화면 확인"에 계정 종류·드로어 역할 체크·막힌 행 절차를 더하고 사람이 확인한다 (T012를 갈음한다)

---

## Phase 8: 최고 관리자 계정 고정과 개발자 DB 절차 (2026-11-21·22 FR-007·FR-009, US1-AC2·AC3, US4-AC3)

**Purpose**: 최고 관리자 계정 행은 본인 이름 바꾸기만, 되돌리기는 DB SQL 절차 (research.md R13~R15). `003-admin-roles` Phase 8과 한 구현 브랜치 `feat/003-single-super-admin`

**백엔드**

- [X] T020 backend/src/main/java/com/moa/api/application/v1/user/UserAdminApplication.java·backend/src/main/java/com/moa/api/service/user/UserAdminService.java: 최고 관리자 대상의 계정 종류·잠그기·풀기·초기화·삭제 B001(`requireNotSuperAdmin`), 이름 바꾸기는 본인만, `requireSuperAdminRemains` 호출 없앰 (R13)
- [X] T021 backend/src/main/resources/db/ops/super-admin-reset-password.sql·super-admin-transfer.sql(맨 위 값 줄, 한 트랜잭션, 확인 표 검사, 지우기 먼저, 토큰 폐기, 끝에 수 확인), specs/002-admin-users/quickstart.md "운영: 최고 관리자 되돌리기"와 맞추기 (R14)

**프론트**

- [X] T022 frontend/src/app/(admin)/admin/users/user-drawer.tsx(최고 관리자 행: 이름 바꾸기만, 본인 안내)·user-roles.tsx(최고 관리자 역할 체크 칸 늘 꺼짐과 이유, 최고 관리자 계정은 "최고 관리자 (모든 권한)"만), frontend/messages/ko.json 문구 (R13)

**테스트 (마지막)**

- [X] T023 backend/src/test/java/com/moa/api/AdminUsersAcceptanceTest.java: US1-AC2·AC3(SQL 파일 실행), US4-AC3·FR-007(최고 관리자 행 B001·A005, 본인 이름 200), 절차 멈춤. `lastSuperAdminIsProtected`를 바꾼다. frontend/e2e/user-admin.spec.ts 최고 관리자 행·역할 칸 (원칙 V, R15)
- [ ] T024 specs/002-admin-users/quickstart.md "화면 확인"에 최고 관리자 본인 행을 더하고, "운영: 최고 관리자 되돌리기"를 개발자가 한 번 따라 해 본다 (사람 확인)

---

## Phase 9: 드로어 한 폼과 행 클릭 (2026-11-22 FR-007·FR-008, US3·US4)

**Purpose**: 행 아무 곳이나 눌러 열고, 드로어를 계정 정보 한 폼 + 계정 상태 + 계정 삭제 구역으로 (research.md R16). 백엔드는 바꾸지 않는다. 구현 브랜치 `feat/002-user-drawer-form`

**프론트**

- [ ] T025 frontend/src/components/kit/data-table.tsx에 `onRowClick`(행에 `ROW_CLICKABLE`·`openOnRowClick`), frontend/src/app/(admin)/admin/users/page.tsx 아이디 칸을 밑줄 없는 단추로, 행 클릭으로 드로어 열기 (R16)
- [ ] T026 frontend/src/app/(admin)/admin/users/user-drawer.tsx·user-roles.tsx: 이름 입력란·계정 종류 나눔 단추·역할 체크 칸 한 폼과 아래 저장·취소(바꾼 채 닫으면 묻기, 계정 종류가 바뀌면 확인 창), 역할 목록에서 최고 관리자 역할 빼기, 계정 상태 구역(로그인 막기 스위치·비밀번호 초기화), 계정 삭제 구역. user-dialogs.tsx의 `RenameDialog`·`RoleChangeDialog` 없애기 (R16)
- [ ] T027 frontend/messages/ko.json `userAdmin`: 머리 설명(FR-008), 로그인 막기·계정 상태·계정 삭제·저장 문구, 쓰지 않는 키 지우기 (R16)

**테스트 (마지막)**

- [ ] T028 frontend/e2e/user-admin.spec.ts: 행을 눌러 열기, 한 폼 저장(이름·역할 한 번에), 계정 종류 확인 창, 최고 관리자 역할이 목록에 없음, 로그인 막기 스위치, 막힌 행에서 칸·스위치·단추 꺼짐. 기존 "역할 저장"·"이름 바꾸기" 단추 테스트를 고친다 (원칙 V)
- [ ] T029 specs/002-admin-users/quickstart.md "화면 확인" 6·10을 새 드로어로 사람이 확인한다

---

## Phase 10: 소셜 계정 표시와 비밀번호 초기화 이유 (2026-11-24 FR-004·FR-007, US3-AC5)

**Purpose**: 목록에서 소셜 계정을 알아보고, 드로어에서 비밀번호 초기화가 꺼진 이유를 보인다 (research.md R17). 마이그레이션 없음. 구현 브랜치 `feat/002-social-mark`

**백엔드**

- [ ] T030 backend/src/main/java/com/moa/api/repository/jpa/user/UserSocialAccountRepository.java·repository/reader/user/UserSocialAccountReader.java: 여러 회원의 연결 제공자를 한 번에 읽는 조회 (R17)
- [ ] T031 backend/src/main/java/com/moa/api/dto/user/UserAdminRowDto.java·application/v1/user/UserAdminApplication.java(`rowFacts`)·controller/v1/admin/user/response/UserAdminResponse.java: `socialProviders`를 붙이고 응답에 더한다 (R17)

**프론트**

- [ ] T032 frontend/src/types/schema.d.ts: `npm run gen:api`로 `socialProviders`를 받는다
- [ ] T033 frontend/src/app/(admin)/admin/users/page.tsx 아이디 칸 제공자 배지, user-drawer.tsx 로그인 방식 줄과 비밀번호 줄 설명, frontend/messages/ko.json `userAdmin` 문구 (FR-004·FR-007, R17)

**테스트 (마지막)**

- [ ] T034 backend/src/test/java/com/moa/api/AdminUsersAcceptanceTest.java: US3-AC5 (`socialProviders`, 소셜 회원 비밀번호 초기화 B001) (원칙 V)
- [ ] T035 frontend/e2e/user-admin.spec.ts: 소셜 회원 행의 배지, 드로어의 로그인 방식("구글")·꺼진 초기화 단추와 설명. 운영자 드로어의 로그인 방식("아이디·비밀번호")은 지금의 US3-AC2 테스트에 더한다
- [ ] T036 specs/002-admin-users/quickstart.md "화면 확인"에 소셜 회원 확인을 더하고 사람이 확인한다

---

## Phase 11: 사용자 일괄 삭제 (2026-11-24 FR-006·FR-007, US3-AC6·US4-AC6)

**Purpose**: 목록에서 여러 계정을 체크해 한꺼번에 지운다 (research.md R18). 마이그레이션 없음. 구현 브랜치 `feat/002-users-bulk-delete`

**백엔드**

- [ ] T037 backend/src/main/java/com/moa/api/application/v1/user/UserAdminApplication.java에 `deleteAll(actor, ids)`: 한 트랜잭션에서 고른 계정을 모두 검사(없는 계정 R001, `rowFacts`를 한 번에 읽어 `protection` A005, 최고 관리자·내 계정 B001)한 뒤 계정마다 지우고 역할을 빼고 토큰을 끊는다. 검사와 지우기는 `delete`와 같은 서비스 메서드를 쓴다 (R18)
- [ ] T038 backend/src/main/java/com/moa/api/controller/v1/admin/user/: `UserIdsRequest`(`ids`, 메뉴·역할의 `IdsRequest`와 같은 검증)와 `ApiAdminUserController`의 `POST /bulk-delete`(204, `@Operation` 설명에 한 트랜잭션·막는 코드) (R18)

**프론트**

- [ ] T039 frontend/src/types/schema.d.ts: `npm run gen:api`로 `bulk-delete`를 받는다. 지금 schema.d.ts가 최신이 아니면 frontend/src/types/user-admin.ts와 API 함수에 직접 둔다
- [ ] T040 frontend/src/app/(admin)/admin/users/page.tsx: `DataTable` `selection`(`canSelect`: `protection` 없음·최고 관리자 아님·내 계정 아님, `rowLabel`: 아이디), 표 위 `BulkToolbar`(고른 수, 삭제), 확인 창, 끝나면 고른 것 풀고 목록 다시 읽기, 쪽·검색이 바뀌면 고른 것 풀기. frontend/messages/ko.json `userAdmin` 일괄 삭제 문구 (FR-006, R18)

**테스트 (마지막)**

- [ ] T041 backend/src/test/java/com/moa/api/AdminUsersAcceptanceTest.java: contracts/admin-accounts.md "일괄 삭제" 표대로 US3-AC6·US4-AC6, B001·R001 섞임 (원칙 V)
- [ ] T042 frontend/e2e/user-admin.spec.ts: 둘을 체크해 일괄 삭제(확인 창), 내 행·최고 관리자 행의 꺼진 체크 칸, 체크 칸을 눌러도 드로어가 열리지 않음
- [ ] T043 specs/002-admin-users/quickstart.md "화면 확인"에 일괄 삭제를 더하고 사람이 확인한다

---

## Dependencies & Execution Order

- Phase 11: T037 → T038 → T039 → T040 → T041 → T042 → T043
- Phase 10: T030 → T031 → T032 → T033 → T034 → T035 → T036. `010-social-login`의 연결 테이블(V10)이 있어야 한다
- Phase 8: `003-admin-roles` T049~T050 → T020 → T021 → `003-admin-roles` T051 → T022 → `003-admin-roles` T052~T054 → T023 → `003-admin-roles` T055 → T024
- Phase 7: `003-admin-roles` T033~T039 → T013 → T014 → T015 → T016 → (`001-admin-menus` T089~T093) → `003-admin-roles` T043~T046 → T017 → T018 → `003-admin-roles` T047 → T019
- Phase 6: `001-admin-menus` T071, `003-admin-roles` T024 → T007 → T008 → T009 → T010 → T011 → T012
- T001 → US1(T002)·US2(T003) → T004 → T005, T006
- US1과 US2는 서로 기대지 않는다
- 같은 파일을 고치므로 병렬로 진행하지 않는다

## Parallel Example

없음. 모든 테스트가 `AdminUsersAcceptanceTest.java` 한 파일에 있다.

## Implementation Strategy

1. T001~T002로 첫 관리자 흐름을 검증한다 (MVP)
2. T003으로 운영자 계정 흐름을 검증한다
3. T004~T006으로 마무리하고 구현 PR을 연다
