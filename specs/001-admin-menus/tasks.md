---

description: "관리 메뉴와 접근 (admin-menus) 작업 목록"
---

# Tasks: 관리 메뉴와 접근 (admin-menus)

**Input**: `specs/001-admin-menus/`의 설계 문서

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/admin-access.md

**Tests**: 원칙 II에 따라 AC마다 테스트를 하나 둔다. 동작은 키트에 이미 있으므로 이 기능의 작업은 테스트가 전부다 (research.md R1). 운영 코드, 프론트, 마이그레이션은 고치지 않는다. 단 Phase 6은 마이그레이션, Phase 7(셸 나누기)은 프론트를 고친다. Phase 8·Phase 10은 키트 menu 헬퍼(백엔드·프론트)와 마이그레이션을 고친다.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: 병렬 가능 (다른 파일, 의존 없음)
- **[Story]**: 작업이 속한 사용자 스토리

모든 테스트는 한 파일에 들어가므로 [P] 작업은 없다.

## Phase 1: Setup

**Purpose**: 테스트 클래스 준비

- [X] T001 `AcceptanceTest`를 상속한 `AdminMenusAcceptanceTest`를 backend/src/test/java/com/moa/api/AdminMenusAcceptanceTest.java에 만든다. 클래스 주석에 스펙 `001-admin-menus`를 적는다. ADMIN 요청은 `bearer(2L, "ADMIN")`, 일반 회원 요청은 `bearer(3L, "USER")`로 만드는 도우미를 `MenuAcceptanceTest`와 같은 모양으로 둔다. 키트 `MenuAcceptanceTest`는 고치지 않는다

---

## Phase 2: Foundational

**Purpose**: 모든 메뉴 AC가 쓰는 처음 구성 복원

**⚠️ CRITICAL**: US1-AC1과 US2-AC1이 이 작업에 기댄다

- [X] T002 backend/src/test/java/com/moa/api/AdminMenusAcceptanceTest.java의 `@BeforeEach`에서 `delete from menus`로 표를 비운 뒤, `classpath:db/migration/V1_7__menus.sql`과 `classpath:db/migration/V1_7_1__admin_menus.sql`을 이 순서로 읽어 주석을 뺀 문 중 INSERT 문만 실행한다 (`CREATE TABLE`은 실행하지 않는다). 시드 값을 테스트에 다시 적지 않는다 (research.md R2)

**Checkpoint**: 테스트마다 처음 배포 때의 메뉴 구성으로 시작한다

---

## Phase 3: User Story 1 - 관리자가 관리 메뉴를 본다 (Priority: P1) 🎯 MVP

**Goal**: 관리자가 관리 메뉴 5개를 정해진 순서로 보고 각 화면의 목록을 쓸 수 있음을 고정한다

**Independent Test**: `./gradlew dbTest --tests "*AdminMenusAcceptanceTest"`에서 US1-AC1, US1-AC2가 통과한다

- [X] T003 [US1] `@DisplayName("US1-AC1 ...")` 테스트를 backend/src/test/java/com/moa/api/AdminMenusAcceptanceTest.java에 더한다. ADMIN으로 `GET /api/v1/menus/me`를 불러 200, 최상위 항목 이름이 사용자 관리, 메뉴 관리, 코드 관리, 접속 이력, 역할 관리 순서이고 하위 메뉴가 없음을 확인한다 (data-model.md 표)
- [X] T004 [US1] `@DisplayName("US1-AC2 ...")` 테스트를 backend/src/test/java/com/moa/api/AdminMenusAcceptanceTest.java에 더한다. ADMIN으로 contracts/admin-access.md의 목록 API 5개(`/api/v1/admin/users`, `/api/v1/admin/menus`, `/api/v1/admin/code-groups`, `/api/v1/admin/login-histories`, `/api/v1/admin/roles`)를 불러 모두 200임을 확인한다

**Checkpoint**: US1이 단독으로 검증된다

---

## Phase 4: User Story 2 - 권한이 없는 사용자는 백오피스에 들어오지 못한다 (Priority: P1)

**Goal**: 일반 회원과 비로그인 사용자의 관리 메뉴 노출과 관리 기능 접근이 막힘을 고정한다

**Independent Test**: `./gradlew dbTest --tests "*AdminMenusAcceptanceTest"`에서 US2-AC1~AC3가 통과한다

- [X] T005 [US2] `@DisplayName("US2-AC1 ...")` 테스트를 backend/src/test/java/com/moa/api/AdminMenusAcceptanceTest.java에 더한다. USER로 `GET /api/v1/menus/me`를 불러 200과 빈 목록임을 확인한다
- [X] T006 [US2] `@DisplayName("US2-AC2 ...")` 테스트를 backend/src/test/java/com/moa/api/AdminMenusAcceptanceTest.java에 더한다. USER로 목록 API 5개와 쓰기 요청 `POST /api/v1/admin/menus`(유효한 본문, 모양은 `MenuAcceptanceTest`와 같게)를 불러 모두 403, `$.code` = `A005`임을 확인한다
- [X] T007 [US2] `@DisplayName("US2-AC3 ...")` 테스트를 backend/src/test/java/com/moa/api/AdminMenusAcceptanceTest.java에 더한다. 토큰 없이 목록 API 5개를 불러 모두 401, `$.code` = `A001`임을 확인한다

**Checkpoint**: US1과 US2가 모두 검증된다

---

## Phase 5: Polish

**Purpose**: 전체 검사와 문서 정합성

- [X] T008 backend/에서 `./gradlew spotlessApply`, `./gradlew build dbTest`를 돌려 통과를 확인한다. 테스트가 실패하면 테스트를 고치지 말고 spec.md의 AC부터 확인한다 (원칙 V)
- [X] T009 specs/README.md 기능 지도에서 `001-admin-menus`의 상태를 `기획 중`에서 `구현됨`으로 바꾼다
- [ ] T010 specs/001-admin-menus/quickstart.md의 "화면 확인" 절차를 사람이 직접 수행한다. 시험 계정 비밀번호가 필요하므로 에이전트는 하지 않는다 (원칙 VI)

---

## Phase 6: 관리 메뉴를 최상위로 옮긴다 (2026-11-14 FR-001 변경)

**Purpose**: "관리" 그룹을 없애고 관리 메뉴 5개를 최상위에 둔다 (spec.md FR-001, research.md R1)

- [X] T011 backend/src/main/resources/db/migration/V2__flat_admin_menus.sql을 더한다. `parent_id` 1인 메뉴를 `parent_id` NULL로 옮기고, "관리" 그룹(`id` 1)을 `deleted_at`·`deleted_by`로 지운다. 키트 시드 `V1_7`·`V1_7_1`은 고치지 않는다
- [X] T012 backend/src/test/java/com/moa/support/MenuSeed.java가 `V2`도 읽어 INSERT 다음에 UPDATE 문을 실행하게 한다
- [X] T013 backend/src/test/java/com/moa/api/AdminMenusAcceptanceTest.java의 US1-AC1을 T003의 바뀐 기대로 고친다
- [X] T014 backend/src/test/java/com/moa/api/AdminRolesAcceptanceTest.java의 US1-AC1을 `003-admin-roles` contracts의 바뀐 기대(최상위 "접속 이력" 하나)로 고친다
- [X] T015 backend/에서 `./gradlew spotlessApply`, `./gradlew build dbTest`를 돌려 통과를 확인한다
- [ ] T016 specs/001-admin-menus/quickstart.md의 "화면 확인" 2번을 사람이 직접 확인한다

---

## Phase 7: 관리 셸과 사용자 셸 나누기 (2026-11-15 FR-001·FR-005·FR-006 변경)

**Purpose**: 관리 셸에는 관리 메뉴만, 공지·홈은 사용자 셸에 두고, 로그인 뒤 첫 화면을 사용자에 맞춘다 (research.md R5~R8). `006-notices` FR-010, `007-notifications` FR-005·FR-008 변경도 여기서 한다. 프론트만 고친다 (frontend/)

**셸 부품**

- [X] T017 frontend/src/components/shell/sidebar-shell.tsx가 `menu`(셸 메뉴)와 `homeHref`(로고 주소)를 인자로 받게 한다. 로그인하지 않았으면 계정 줄의 벨·계정 메뉴 대신 로그인 단추(`signInPath(지금 주소)`)를 보이고, 좁은 폭 머리 벨도 그리지 않는다 (007 FR-005)
- [X] T018 frontend/src/components/shell/use-menu.ts를 `useAdminMenu()`(서버 메뉴 `/menus/me`만)와 `useUserMenu()`(코드 메뉴 `menu.ts`의 홈·공지, "공지" 새 공지 점)로 나눈다. 새 공지 점(`useHasNewNotice`)과 서버 메뉴 조회는 로그인했을 때만 한다. `menu.ts` 주석을 "사용자 셸 메뉴"로 고친다
- [X] T019 frontend/src/components/shell/admin-shell.tsx(`useAdminMenu`, 로고 `/admin`)와 user-shell.tsx(`useUserMenu`, 로고 `/`)를 만들고, app-shell.tsx는 두 셸을 내보내게 바꾼다
- [X] T020 [P] frontend/src/components/shell/landing.ts에 `firstMenuPath(menus)`(위에서부터 처음 만나는 주소 있는 메뉴)와 `landingPath(menus)`(항목이 있으면 `/admin`, 없으면 `/`)를 만들고 frontend/tests/unit/landing.test.ts로 확인한다 (R6)
- [X] T021 [P] frontend/src/components/shell/account-menu.tsx에 "설정"(`/settings`)을 비밀번호 변경 위에 더한다. 주소는 frontend/src/lib/next-path.ts에 `SETTINGS_PATH`로, 글자는 frontend/messages/ko.json `shell.settings`로 둔다

**경로 묶음**

- [X] T022 frontend/src/app/(main)/admin/을 frontend/src/app/(admin)/admin/으로 옮기고, (admin)/layout.tsx(`AuthGate` + `AdminShell`)와 (admin)/admin/page.tsx(`firstMenuPath`로 보내고, 없으면 `/`)를 만든다
- [X] T023 frontend/src/app/(site)/를 frontend/src/app/(user)/로 바꾼다. (user)/layout.tsx는 지금 (site)/layout의 조용한 로그인 확인을 하고 `UserShell`로 감싼다. frontend/src/components/notice/site-header.tsx와 쓰지 않게 된 문구를 지운다 (006 FR-010)
- [X] T024 frontend/src/app/(main)/page.tsx를 frontend/src/app/(user)/(signed-in)/page.tsx로 옮기고 (user)/(signed-in)/layout.tsx에 `AuthGate`를 둔다
- [X] T025 frontend/src/app/(account)/layout.tsx를 만든다. `AuthGate` 안에서 `/menus/me`를 읽어 항목이 있으면 `AdminShell`, 없으면 `UserShell`로 감싼다(읽는 동안 대기 화면). (main)/notifications/와 (main)/account/를 이 묶음으로 옮기고 (main)/을 지운다 (R7)
- [X] T026 frontend/src/app/(account)/settings/page.tsx를 만들어 머리 "설정"과 `PushToggle`을 둔다. (account)/notifications/page.tsx에서 `PushToggle`을 뺀다. 문구는 frontend/messages/ko.json `settings`에 둔다 (007 FR-008, contracts/notifications-screens.md)
- [X] T027 frontend/src/app/(public)/sign-in/page.tsx가 로그인한 뒤 `?next=`가 있으면 그 주소, 없으면 `/menus/me`를 읽어 `landingPath`로 보낸다 (FR-006)

**화면 검사 (e2e, 백엔드는 가로챈다)**

- [X] T028 frontend/e2e/menu.spec.ts에 셸 AC를 둔다: `US1-AC1`(관리자 로그인 → 첫 관리 메뉴 화면, 사이드바에 관리 메뉴 6개만, 홈·공지 없음), `US2-AC1`(일반 회원 사이드바에 관리 메뉴 없음), `US2-AC4`(일반 회원 로그인 → `/`, 사이드바 홈·공지). 테스트 이름에 AC 번호를 적는다
- [X] T029 frontend/e2e/notices.spec.ts를 사용자 셸에 맞춘다: 로그인하지 않은 공지 목록·상세가 사이드바와 계정 줄 로그인 단추와 함께 보이고, 새 공지 점이 사용자 셸 "공지" 메뉴에 붙는다 (006 FR-010)
- [X] T030 frontend/e2e/notifications.spec.ts를 바꾼다: 브라우저 알림 스위치 검사를 `/settings`로 옮기고, 관리 셸에도 벨이 있음을 확인한다 (007 FR-005·FR-008)
- [X] T031 나머지 e2e(auth, screens 등)가 옮긴 경로와 첫 화면에 맞는지 확인하고 고친다. 동작이 바뀐 만큼만 고친다 (원칙 V)

**마무리**

- [X] T032 frontend/에서 `npm run format`, `npm run check`, 바꾼 e2e 묶음(`npx playwright test e2e/menu.spec.ts e2e/notices.spec.ts e2e/notifications.spec.ts e2e/auth.spec.ts`)을 돌린다. 전체 화면 검사는 PR CI로 본다
- [X] T033 specs/README.md 기능 지도의 001 상태에서 "셸 나누기는 기획 중"을 지운다
- [X] T034 specs/001-admin-menus/quickstart.md "화면 확인" 2·3번, specs/006-notices/quickstart.md 4·5번, specs/007-notifications/quickstart.md 3~5번을 사람이 직접 확인한다

---

## Phase 8: 메뉴 대상 (2026-11-15 FR-002·FR-005·FR-007 변경)

**Purpose**: 메뉴에 대상(관리자·사용자)을 두어 사용자 셸 메뉴도 메뉴 관리에서 다룬다 (research.md R9~R11). 키트 menu 헬퍼를 moa에서 먼저 고친다

**백엔드**

- [X] T035 backend/src/main/resources/db/migration/V2_4__menu_audience.sql을 더한다: `menus.audience VARCHAR(10) NOT NULL DEFAULT 'ADMIN'`, 사용자 메뉴 홈(`/`, `home`, 10)·공지(`/notices`, `megaphone`, 20)를 `USER`로 넣는다 (data-model.md)
- [X] T036 backend/src/main/java/com/moa/api/entity/menu/MenuAudience.java(`ADMIN`, `USER`)와 Menu.java의 `audience` 열을 더한다. 만들기·고치기가 대상을 받는다
- [X] T037 backend/src/main/java/com/moa/api/service/menu/MenuService.java: 보이는 트리를 대상별로 거른다. `ADMIN` 대상의 권한 칸이 빈 메뉴는 시스템 역할 ADMIN만, `USER` 대상의 권한 칸이 빈 메뉴는 모두(로그인하지 않은 사람 포함). 하위 메뉴는 부모 대상을 따르고, 부모 대상을 바꾸면 하위 메뉴도 바꾼다 (R9, R10)
- [X] T038 backend/src/main/java/com/moa/api/application/v1/menu/MenuApplication.java와 controller/v1/menu/ApiMenuController.java: `/menus/me`는 `ADMIN` 대상만, `GET /api/v1/menus/user`(`@PermitAll`, 로그인했으면 그 권한으로)를 더한다. controller/v1/admin/menu의 요청(`MenuCreateRequest`, 고치기 요청)과 `MenuTreeResponse`에 `audience`를 더한다 (contracts/admin-access.md)
- [X] T039 backend/src/test/java/com/moa/support/MenuSeed.java가 `V2_4`도 읽게 하고, backend/src/test/java/com/moa/api/AdminMenusAcceptanceTest.java에 `US3-AC1`~`US3-AC4`를 더한다 (contracts/admin-access.md)
- [X] T040 backend/src/test/java/com/moa/api/MenuAcceptanceTest.java(키트)에서 권한 칸이 빈 관리 메뉴의 보임을 보는 기대만 FR-002에 맞춰 고친다 (원칙 V, R8)

**프론트**

- [X] T041 frontend/src/types/menu.ts에 `MenuAudience`와 `audience`(관리 메뉴, 요청)를, frontend/src/api/menu.ts에 `user()`(`/api/v1/menus/user`)를 더한다. local 백엔드를 띄울 수 있으면 `npm run gen:api`로 생성 타입도 맞춘다
- [X] T042 frontend/src/components/shell/use-menu.ts의 `useUserMenu`가 `/menus/user`를 읽게 하고(로그인 여부와 상관없이, 로그인 상태가 바뀌면 다시 읽는다) 새 공지 점을 주소 `/notices` 항목에 붙인다. frontend/src/components/shell/menu.ts의 코드 메뉴 항목을 비우고 주석을 고친다. frontend/src/components/menu/icons.ts에 `megaphone`을 더한다(`home`은 이미 있다) (R11, 006 FR-010)
- [X] T043 frontend/src/app/(admin)/admin/menus/에 대상 탭(관리자 메뉴 / 사용자 메뉴)을 두고 탭마다 그 대상의 트리·순서를 보인다. 만들기는 지금 탭의 대상, 하위 메뉴 대화상자는 부모 대상을 보여 주기만 한다. 문구는 frontend/messages/ko.json `menuAdmin`에 둔다 (R11)
- [X] T044 frontend/e2e/support/api.ts의 `signedIn`·`signedOut`이 `/menus/user`를 홈·공지로 가로채게 하고, frontend/e2e/menu.spec.ts에 대상 탭(사용자 메뉴 탭에서 만들면 `audience` = `USER`로 보낸다)과 사용자 셸이 서버 사용자 메뉴를 그리는 검사를 더한다

**마무리**

- [X] T045 backend/에서 `./gradlew spotlessApply`, `./gradlew build`, `./gradlew dbTest --tests "*Menu*AcceptanceTest"`, frontend/에서 `npm run format`, `npm run check`, 바꾼 e2e 묶음(menu·notices·notifications·auth·screens)을 돌린다. 전체 검사는 PR CI로 본다
- [X] T046 specs/README.md 기능 지도의 001 상태에서 "메뉴 대상은 기획 중"을 지운다
- [X] T047 specs/001-admin-menus/quickstart.md "화면 확인" 3번을 사람이 직접 확인한다

---

## Phase 9: 넓은 폭 머리 줄 없애기 (2026-11-15 FR-005 변경)

**Purpose**: 넓은 폭에서 본문 위 머리 줄을 없애고 접기 단추를 사이드바 로고 줄로 옮긴다 (research.md R12). 프론트만 고친다

- [X] T048 frontend/src/components/shell/sidebar-shell.tsx: 넓은 폭은 로고 줄 오른쪽에 접기 단추를 두고, 아이콘만 남게 접으면 로고 자리를 펼치기 단추(평소 로고 글자, 마우스·키보드 초점에서 펼치기 아이콘)로 바꾼다. 본문 머리 줄은 좁은 폭에서만 그리고, 화면 이름 대신 사이트 이름(로고 주소 링크)을 둔다. 단추 이름은 frontend/messages/ko.json `shell`에 둔다. `screenTitle`을 쓰는 곳이 없어지면 그대로 둘지 지울지 검사(lint)에 맞춘다
- [X] T049 frontend/e2e/menu.spec.ts에 `US1-AC3`(넓은 폭: 머리 줄 없음, 로고 줄 접기 → 접힘, 로고 자리 펼치기 → 펼침)을 더하고, 머리 줄 사이드바 단추를 찾던 frontend/e2e/notices.spec.ts·screens.spec.ts를 바뀐 만큼만 고친다 (원칙 V)
- [X] T050 frontend/에서 `npm run format`, `npm run check`, 바꾼 e2e 묶음(menu·notices·notifications·screens)을 돌린다. 전체 화면 검사는 PR CI로 본다
- [X] T051 specs/001-admin-menus/quickstart.md "화면 확인" 2번의 접기·펼치기를 사람이 직접 확인한다

---

## Phase 10: 프로그램과 공개 범위 (2026-11-15 FR-002·FR-007~FR-009 변경)

**Purpose**: 메뉴에 걸 화면을 프로그램으로 코드에 등록하고, 메뉴는 프로그램을 고르게 한다. 사용자 메뉴는 권한 대신 공개 범위를 쓴다 (research.md R13~R17). 구현 브랜치는 `feat/001-menu-programs`

**백엔드**

- [X] T052 backend/src/main/java/com/moa/api/program/에 `ProgramDefinition`(key, name, path, audience, permission, signInRequired), `ProgramSource`, `ProgramRegistry`(시작 검사: 키·경로 겹침, 관리 프로그램은 등록된 권한 필수·로그인 필요 여부 없음, 사용자 프로그램은 권한 없음. 어기면 시작 실패)를 만든다. 모양은 `common/security/permission`의 권한 등록을 따른다 (R13)
- [X] T053 backend/src/main/java/com/moa/api/program/에 data-model.md 프로그램 표의 8개를 내는 `ProgramSource` 빈을 화면 주인별로 만든다(MenuPrograms, UserPrograms, CodePrograms, LoginHistoryPrograms, RolePrograms, NoticePrograms, HomePrograms). 권한 키는 `api/permission`의 상수를 쓴다
- [X] T054 backend/src/main/resources/db/migration/V2_5__menu_programs.sql: `menus`에 `program_key VARCHAR(50) NULL`, `visibility VARCHAR(10) NULL`을 더하고, 기존 메뉴의 `path`로 data-model.md 표의 키를 채운다(맞는 것이 없으면 `MISSING`, 경로 없는 묶음 메뉴는 비움). `audience` = `USER`인 메뉴는 `visibility` = `PUBLIC`. 그 뒤 `path`·`permission` 열을 지운다. backend/src/test/java/com/moa/support/MenuSeed.java가 V2_5까지 반영된 처음 구성을 되돌리게 고친다 (R14)
- [X] T055 backend/src/main/java/com/moa/api/entity/menu/Menu.java에서 `path`·`permission`을 `programKey`·`visibility`로 바꾸고 `MenuVisibility`(`PUBLIC`·`MEMBER`)를 더한다. backend/src/main/java/com/moa/api/service/menu/MenuService.java의 보이는 트리(관리 메뉴는 프로그램 권한, 사용자 메뉴는 공개 범위와 로그인 여부, 묶음은 보이는 하위 메뉴가 있을 때, 없는 프로그램은 뺌)와 만들기·고치기 검사(등록된 프로그램, 같은 대상, 로그인 필요 프로그램은 `MEMBER`만, 사용자 메뉴는 공개 범위 필수, 하위 메뉴는 프로그램 필수는 `V001`, 대상 바꾸기가 하위 메뉴와 부딪히면 `B001`)를 고친다. 경로 형식·겹침 검사는 지운다 (R14, R15)
- [X] T056 메뉴 API의 요청·응답을 고친다: backend/src/main/java/com/moa/api/controller/v1/admin/menu/request/{MenuCreateRequest, MenuModifyRequest}.java는 `path`·`permission` 대신 `programKey`·`visibility`, response/MenuTreeResponse.java는 `programKey`, `program`, `missingProgram`, `visibility`. 내 메뉴·사용자 메뉴 응답(`MyMenuResponse`)의 `path`는 프로그램 경로로 채우고 모양은 그대로 둔다. application/v1/menu의 명령도 맞춘다 (R15)
- [X] T057 backend/src/main/java/com/moa/api/controller/v1/admin/program/ApiAdminProgramController.java에 `GET /api/v1/admin/programs`(`@RequirePermission(MenuPermissions.MANAGE)`, 응답 `key`, `name`, `path`, `audience`, `permission`(`key`, `name`), `signInRequired`)를 만든다 (R15)
- [X] T058 backend/src/test/java/com/moa/support/TestPrograms.java에 테스트에서만 등록하는 로그인 필요 사용자 프로그램(예: `TEST_MEMBER_ONLY`, `/test-member-only`)을 `ProgramSource`로 둔다 (R17)
- [X] T059 backend/src/test/java/com/moa/api/AdminMenusAcceptanceTest.java: US3-AC2(`PUBLIC`)·US3-AC4(`ADMIN_CODES` 관리 메뉴)를 바뀐 스펙대로 고치고(원칙 V), `US3-AC5`, `US4-AC1`~`US4-AC5`를 contracts/admin-access.md 표대로 더한다
- [X] T060 backend/src/test/java/com/moa/api/MenuAcceptanceTest.java(키트)에서 요청의 `path`·`permission`을 `programKey`·`visibility`로 바꾸고, 경로 형식·겹침처럼 없어진 동작을 보는 테스트는 지운다. 남은 테스트의 확인 내용은 스펙이 바뀐 만큼만 고친다 (원칙 V)

**프론트**

- [X] T061 frontend/src/types/menu.ts(`Program` 포함), frontend/src/api/menu.ts(`admin.programs`)를 바뀐 API에 맞춘다. local 백엔드를 띄울 수 있으면 `npm run gen:api`로 생성 타입도 맞춘다
- [X] T062 frontend/src/app/(admin)/admin/menus/page.tsx에 "프로그램" 탭(조회 전용 표: 이름, 경로, 대상, 필요한 권한 이름 또는 로그인 필요)을 더하고, 메뉴 목록의 "필요한 권한" 열을 "프로그램"(없는 프로그램이면 경고 표시)·"보이는 사람"으로 바꾼다 (R16)
- [X] T063 frontend/src/app/(admin)/admin/menus/menu-dialog.tsx에서 경로·권한 입력을 없애고 종류(화면 메뉴 / 묶음 메뉴), 프로그램 선택 상자(지금 탭 대상만), 고른 프로그램의 경로·필요한 권한(읽기 전용), 사용자 메뉴의 공개 범위(로그인 필요 프로그램이면 "비회원 포함"을 끄고 이유를 보인다)를 둔다. frontend/src/lib/schemas/menu.ts와 frontend/messages/ko.json `menuAdmin` 문구를 맞춘다 (R16)
- [X] T064 frontend/e2e/menu.spec.ts에 대화상자(경로·권한 입력 없음, 프로그램 선택, 로그인 필요 프로그램의 "비회원 포함" 꺼짐)와 프로그램 탭 검사를 더하고, 경로를 입력하던 검사는 바뀐 만큼 고친다. frontend/e2e/support/api.ts의 가로채기 응답을 새 모양으로 맞춘다. frontend/e2e/screens.spec.ts가 처음 등록한 프로그램 경로를 모두 여는지 확인한다 (R17)

**마무리**

- [X] T065 backend/에서 `./gradlew spotlessApply`, `./gradlew build`, `./gradlew dbTest --tests "*Menu*AcceptanceTest" --tests "*AdminRolesAcceptanceTest"`, frontend/에서 `npm run format`, `npm run check`를 돌린다. e2e와 dbTest 전체는 로컬에서 돌리지 않고 PR CI로 본다 (docs/git.md "빌드·테스트")
- [X] T066 specs/README.md 기능 지도의 001 상태에서 "프로그램·공개 범위는 기획 중"을 지운다
- [X] T067 specs/001-admin-menus/quickstart.md "화면 확인" 3번을 사람이 직접 확인한다

---

## Phase 11: 관리 화면 정리 (2026-11-15 FR-001·FR-006·FR-008·FR-010~FR-012, US5 변경)

**Purpose**: 관리 메뉴를 운영·설정 묶음으로, 메뉴 관리를 트리 표·끌기·드로어·일괄 처리로, 공용 드로어와 머리 안내 (research.md R18~R23). 구현 브랜치는 `feat/001-menu-console`. 역할·사용자·공통코드 화면은 각 스펙에서 이 Phase의 부품을 쓴다

**백엔드**

- [X] T068 backend/src/main/resources/db/migration/V2_6__admin_menu_groups.sql: 묶음 "운영"·"설정"을 넣고 최상위 관리 메뉴를 `program_key`로 찾아 그 아래로 옮기고(순서는 data-model.md 묶음 표), 이름 "코드 관리"를 "공통코드"로 바꾼다. backend/src/main/java/com/moa/api/program/CodePrograms.java 이름도 "공통코드" (R18)
- [X] T069 backend/src/main/java/com/moa/api/service/menu/MenuService.java에 `setEnabled(ids, enabled)`, `deleteAll(ids)`를 더하고(한 트랜잭션, 없는 메뉴 R001, 하위 있는 묶음 B001, 고른 것끼리 부모·자식이면 허용), application과 backend/src/main/java/com/moa/api/controller/v1/admin/menu/ApiAdminMenuController.java에 `PUT /api/v1/admin/menus/enabled`, `POST /api/v1/admin/menus/bulk-delete`를 둔다 (R19)
- [X] T070 backend/src/test/java/com/moa/api/AdminMenusAcceptanceTest.java: US1-AC1을 묶음 모양으로 고치고(원칙 V), US5-AC3~AC5를 contracts/admin-access.md "메뉴 일괄 처리" 표대로 더한다. backend/src/test/java/com/moa/api/MenuAcceptanceTest.java에 일괄 API 규칙(없는 ID R001, 부모·자식 함께 지우기 허용)을 더한다. backend/src/test/java/com/moa/api/AdminRolesAcceptanceTest.java의 US1-AC1·US2-AC1 기대를 묶음 모양으로 고친다 (R23)

**프론트**

- [X] T071 frontend/에 `@dnd-kit/core`·`@dnd-kit/sortable`을 들이고(정확한 버전), frontend/src/components/kit/side-drawer.tsx(오른쪽 sheet, 좁은 폭 화면 전체, 저장 안 한 변경 확인)를 만든다. `PageHeader` 머리 안내는 메시지 `t.rich`의 `<link>`로 링크를 둔다 (R20, R21)
- [X] T072 frontend/src/components/shell/landing.ts가 묶음 안의 첫 화면 메뉴로 가게 고친다 (FR-006)
- [X] T073 frontend/src/app/(admin)/admin/menus/: 프로그램 탭을 없애고, 트리 표(menu-tree-table.tsx: 체크박스·손잡이·이름·보이는 사람·사용, 행 단추 없음), 일괄 줄(bulk-bar.tsx: 사용·사용 안 함·묶음으로 옮기기·삭제, 확인), 메뉴 드로어(menu-drawer.tsx: 지금 menu-dialog.tsx 내용, 상위 메뉴 칸 없음, 새 메뉴는 묶음만 고름), 끌기(키보드 포함, 놓으면 `/order`, 실패하면 되돌림, 좁은 폭은 "옮기기" 메뉴)를 만든다. frontend/src/api/menu.ts에 `setEnabled`·`bulkDelete`, 메시지 `menuAdmin`과 머리 안내(FR-011 문구, 역할 관리 링크) (R20, R22)
- [X] T074 frontend/e2e/menu.spec.ts: US5-AC1~AC5(행 단추 없음, 드로어, 키보드로 옮기기 → `/order`, 일괄 줄, 하위 있는 묶음 삭제 안내), 프로그램 탭 없음, 머리 안내 링크, 좁은 폭 드로어 화면 전체. US1-AC1(`SIX_ADMIN_MENUS`)과 로그인 뒤 첫 화면을 묶음 모양으로 고친다 (원칙 V)

**마무리**

- [X] T075 backend/에서 `./gradlew spotlessApply`, `./gradlew build`, `./gradlew dbTest --tests "*Menu*AcceptanceTest" --tests "*AdminRolesAcceptanceTest"`, frontend/에서 `npm run format`, `npm run check`. e2e와 dbTest 전체는 PR CI로 본다
- [ ] T076 specs/001-admin-menus/quickstart.md "화면 확인"에 묶음 사이드바, 끌기, 드로어, 일괄 처리 절차를 더하고 사람이 확인한다

---

## Phase 12: 사이드바 묶음 펴기·접기 (2026-11-18 FR-005 변경, US1-AC4~AC7)

**Purpose**: 묶음 메뉴를 펴고 접고, 상태를 브라우저에 기억하고, 사이드바 영역 사이에 선을 둔다 (research.md R24). 프론트만 고친다. 구현 브랜치는 `feat/001-sidebar-accordion`

**프론트**

- [ ] T077 frontend/src/components/shell/에 묶음 펴짐 상태 훅(예: `use-open-menus.ts`: `localStorage` `moa:sidebar-open-menus`, `useSyncExternalStore`, try/catch, 같은 탭 리스너 + `storage` 이벤트)을 만든다 (R24)
- [ ] T078 frontend/src/components/shell/sidebar-shell.tsx: 묶음 행을 토글로(`aria-expanded`·`aria-controls`·오른쪽 `ChevronRight` `rotate-90`), 펴짐 = 저장값 ?? 지금 화면이 그 안인가, 접힌 하위 목록은 그리지 않는다. 아이콘만 남게 접혔을 때 묶음을 누르면 사이드바를 펼치고 그 묶음을 편다. 머리 `border-b`, 바닥 `border-t`, 앱 이름 `font-semibold`. "키트로 옮길 것" 주석을 단다 (R24)

**테스트**

- [ ] T079 frontend/e2e/menu.spec.ts: US1-AC1을 묶음 펴짐 규칙대로 고치고(원칙 V), US1-AC4~AC7(펴기·접기 새로 고침 유지, 저장소 없이 하위 주소로 들어가면 그 묶음만 펴짐, 직접 접은 것이 먼저, 아이콘 모드 묶음 누르기)을 더한다. 사이드바 하위 링크를 세던 US1-AC3·frontend/e2e/notifications.spec.ts 등은 펴짐 규칙대로 고친다

**마무리**

- [ ] T080 frontend/에서 `npm run format`, `npm run check`. e2e는 PR CI로 본다
- [ ] T081 specs/001-admin-menus/quickstart.md "화면 확인" 2번에 묶음 펴기·접기(새로 고침 유지, 아이콘 모드)를 더하고 사람이 확인한다

---

## Dependencies & Execution Order

- T001 → T002 → US1(T003, T004)·US2(T005~T007) → T008 → T009, T010
- T011 → T012 → T013, T014 → T015 → T016
- Phase 12: T077 → T078 → T079 → T080 → T081
- Phase 11: T068 → T069 → T070 → T071 → T072, T073 → T074 → T075 → T076. 002·003·004의 관리 화면 정리가 T071(드로어·머리 안내)에 기댄다
- Phase 10: T052 → T053 → T054 → T055 → T056, T057 → T058 → T059, T060 → T061 → T062, T063 → T064 → T065 → T066, T067. 프론트 타입(T061)은 API 모양(T056·T057)이 정해지면 백엔드 테스트와 나란히 해도 된다. `003-admin-roles` Phase 5가 이 Phase에 기댄다
- Phase 9: T048 → T049 → T050 → T051
- Phase 8: T035 → T036 → T037 → T038 → T039, T040 → T041 → T042, T043 → T044 → T045 → T046, T047. 백엔드(T035~T040)와 프론트 타입(T041)은 API 모양이 정해지면 나란히 해도 된다
- Phase 7: T017 → T018 → T019 → T022~T026 → T027 → T028~T031 → T032 → T033, T034. T020·T021은 T017과 함께 해도 된다. T027은 T020에 기댄다
- US1과 US2는 서로 기대지 않는다. 둘 다 T002에 기댄다
- 같은 파일을 고치므로 병렬로 진행하지 않는다

## Parallel Example

없음. 모든 테스트가 `AdminMenusAcceptanceTest.java` 한 파일에 있다.

## Implementation Strategy

1. T001~T002로 처음 구성 복원까지 마친다
2. T003~T004로 US1을 검증한다 (MVP)
3. T005~T007로 US2를 검증한다
4. T008~T010으로 마무리하고 PR을 연다
