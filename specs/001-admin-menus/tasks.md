---

description: "관리 메뉴와 접근 (admin-menus) 작업 목록"
---

# Tasks: 관리 메뉴와 접근 (admin-menus)

**Input**: `specs/001-admin-menus/`의 설계 문서

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/admin-access.md

**Tests**: 원칙 II에 따라 AC마다 테스트를 하나 둔다. 동작은 키트에 이미 있으므로 이 기능의 작업은 테스트가 전부다 (research.md R1). 운영 코드, 프론트, 마이그레이션은 고치지 않는다. 단 Phase 6은 마이그레이션, Phase 7(셸 나누기)은 프론트를 고친다.

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
- [ ] T034 specs/001-admin-menus/quickstart.md "화면 확인" 2·3번, specs/006-notices/quickstart.md 4·5번, specs/007-notifications/quickstart.md 3~5번을 사람이 직접 확인한다

---

## Dependencies & Execution Order

- T001 → T002 → US1(T003, T004)·US2(T005~T007) → T008 → T009, T010
- T011 → T012 → T013, T014 → T015 → T016
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
