# Implementation Plan: 관리 메뉴와 접근 (admin-menus)

**Branch**: `feat/001-admin-menus` | **Date**: 2026-11-09 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/001-admin-menus/spec.md`

## Summary

관리 메뉴 5개의 처음 구성과 권한 없는 접근의 차단은 키트(`springboot/v0.3.1` admin 프리셋, `web-next/v0.3.0` admin 프리셋)에 이미 있다.
moa가 할 일은 키트 시드의 "관리" 그룹을 없애 메뉴를 최상위로 옮기고, 그 구성이 스펙과 같음을 AC 테스트로 고정하는 것이다.
운영 코드는 바꾸지 않고 마이그레이션 하나를 더한다 ([research.md](./research.md) R1).

**셸 나누기 (2026-11-15, FR-005·FR-006)**: 프론트만 바꾼다. 한 셸(`(main)`)과 셸 없는 공지 화면(`(site)`)을 관리 셸·사용자 셸 두 벌로 나눈다.
두 셸은 키트 사이드바 셸 하나를 메뉴만 바꿔 쓴다. 백엔드와 API는 바꾸지 않는다 ([research.md](./research.md) R5~R8).

**메뉴 대상 (2026-11-15, FR-007)**: 메뉴에 대상(`ADMIN`·`USER`)을 더해 사용자 셸 메뉴도 메뉴 관리에서 다룬다. 키트 menu 헬퍼(백엔드 열·API, 메뉴 관리 화면)를 moa에서 먼저 고치고 키트로 옮긴다 ([research.md](./research.md) R9~R11).

**머리 줄 (2026-11-15, FR-005)**: 프론트 `SidebarShell`만 바꾼다. 넓은 폭의 본문 머리 줄을 없애고 접기 단추를 사이드바 로고 줄로 옮긴다. 좁은 폭 머리 줄은 남기고 화면 이름 대신 사이트 이름을 둔다 ([research.md](./research.md) R12).

**프로그램과 공개 범위 (2026-11-15, FR-002·FR-007~FR-009)**: 메뉴에 걸 화면을 프로그램으로 코드에 등록하고, 메뉴는 경로·권한 대신 프로그램을 가리킨다. 사용자 메뉴는 권한 대신 공개 범위(`PUBLIC`·`MEMBER`)를 가진다. 셸이 쓰는 메뉴 API의 모양은 그대로다. 키트 menu 헬퍼(백엔드 등록·열·API, 메뉴 관리 화면)를 moa에서 먼저 고치고 키트로 옮긴다 ([research.md](./research.md) R13~R17).

**관리 화면 정리 (2026-11-15, FR-001·FR-006·FR-008·FR-010~FR-012, US5)**: 관리 메뉴를 운영·설정 묶음으로 옮기는 마이그레이션, 메뉴 일괄 API 둘(사용 여부, 지우기), 메뉴 관리 화면을 트리 표·끌기·드로어·일괄 처리로 다시 짜고 프로그램 탭을 없앤다. 관리 화면이 함께 쓰는 드로어 부품과 머리 안내를 둔다. 끌기는 `@dnd-kit`을 들인다 ([research.md](./research.md) R18~R23). 역할·사용자·공통코드 화면은 각 스펙(003·002·004)에서 같은 틀로 바꾼다

**사이드바 묶음 펴기·접기 (2026-11-18, FR-005, US1-AC4~AC7)**: 프론트 `SidebarShell`만 바꾼다. 묶음 메뉴를 처음에 접고(지금 화면이 있는 묶음만 펴고) 펴고 접은 상태를 브라우저에 기억한다. 머리·메뉴·바닥 사이에 선을 둔다. 백엔드와 API는 바꾸지 않는다 ([research.md](./research.md) R24)

## Technical Context

**Language/Version**: Java 21 (Spring Boot 4.1.2), TypeScript 5 (Next 16.3)

**Primary Dependencies**: psw-dev-kit menu·user-admin·code·login-history·rbac 헬퍼 (백엔드·프론트 짝)

**Storage**: MariaDB. 메뉴는 `menus` 테이블, 처음 구성은 Flyway 시드 (`V1_7`, `V1_7_1`)와 moa의 위치 변경 (`V2`)

**Testing**: 인수 테스트 `AcceptanceTest` (MockMvc + Testcontainers MariaDB, `./gradlew dbTest`). 화면은 키트 e2e(Playwright)

**Target Platform**: 웹 백오피스

**Project Type**: 웹 서비스 (모노레포 `backend/`, `frontend/`)

**Performance Goals**: 해당 없음. 새 요청 경로가 없다

**Constraints**: 키트 동작을 다시 적지 않는다 (원칙 III). 키트 코드는 고치지 않는다

**Scale/Scope**: 인수 테스트 클래스 1개, AC 5개. 셸 나누기는 프론트 경로 묶음 3개, 셸 부품, e2e (AC 3개: US1-AC1, US2-AC1, US2-AC4)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| 원칙 | 판정 | 근거 |
|---|---|---|
| I. 스펙이 먼저다 | 통과 | 새 동작을 만들지 않는다. 셸 나누기(2026-11-15)는 spec.md FR-005·FR-006을, 프로그램과 공개 범위(2026-11-15)는 FR-002·FR-007~FR-009를 먼저 고친 뒤 한다. spec.md에 `[NEEDS CLARIFICATION]`이 없다 |
| II. AC마다 테스트 하나 | 통과 | US1-AC1~US2-AC3에 API 통합 테스트를 하나씩 둔다. 이름에 AC 번호를 적는다 |
| III. 키트를 따른다 | 통과 | 키트 헬퍼와 시드를 그대로 쓰고, 메뉴 위치만 moa 마이그레이션으로 바꾼다 (원칙 III의 "메뉴 구성"). 스펙의 머리에 출처가 있다 |
| IV. 기존 기능은 제자리에서 고친다 | 통과 | 새 기능 폴더를 만들지 않는다 |
| V. 테스트를 고쳐서 통과시키지 않는다 | 통과 | 키트 테스트를 고치지 않는다. moa AC 테스트는 스펙(FR-001)이 바뀐 만큼만 고친다 |
| VI. 시크릿을 다루지 않는다 | 통과 | 인수 테스트는 토큰을 직접 만든다. 로컬 계정 비밀번호를 쓰지 않는다 |

Phase 1 설계 뒤 다시 확인했다. 위반이 없다.

## Project Structure

### Documentation (this feature)

```text
specs/001-admin-menus/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/admin-access.md
└── tasks.md             # /speckit-tasks가 만든다
```

### Source Code (repository root)

```text
backend/src/main/resources/db/migration/
└── V2__flat_admin_menus.sql      # 관리 메뉴를 최상위로 옮기고 "관리" 그룹을 지운다
backend/src/test/java/com/moa/
├── api/AdminMenusAcceptanceTest.java   # 이 기능의 AC 테스트
├── api/AdminRolesAcceptanceTest.java   # 003. 메뉴 모양이 바뀐 만큼 US1-AC1을 고친다
└── support/MenuSeed.java              # 처음 구성 되돌리기에 V2를 더한다
```

셸 나누기 (2026-11-15):

```text
frontend/src/app/
├── (admin)/layout.tsx              # AuthGate + 관리 셸. /admin/... (지금 (main)/admin을 옮긴다)
├── (admin)/admin/page.tsx          # 볼 수 있는 첫 관리 메뉴로 보낸다 (R6)
├── (user)/layout.tsx               # 사용자 셸. 로그인 없이 열린다 (조용한 로그인 확인, 지금 (site)/layout)
├── (user)/notices/                 # 공지 목록·상세 (지금 (site)/notices)
├── (user)/(signed-in)/layout.tsx   # AuthGate
├── (user)/(signed-in)/page.tsx     # 첫 화면 / (지금 (main)/page)
├── (account)/layout.tsx            # AuthGate + 관리 메뉴가 있으면 관리 셸, 없으면 사용자 셸 (R7)
├── (account)/notifications/        # 알림 화면 (브라우저 알림 스위치는 뺀다)
├── (account)/settings/page.tsx     # 설정: 브라우저 알림 스위치 (007 FR-008)
└── (account)/account/password/     # 비밀번호 변경 (지금 (main)/account)
frontend/src/components/shell/
├── sidebar-shell.tsx               # 메뉴·로고 주소를 받는다. 로그인하지 않았으면 계정 줄에 로그인 단추 (키트로 옮길 것)
├── admin-shell.tsx, user-shell.tsx # 관리 메뉴(서버 메뉴) / 사용자 메뉴(menu.ts: 홈, 공지)로 셸을 그린다
├── use-menu.ts                     # 서버 메뉴와 코드 메뉴를 나눠 돌려준다
├── account-menu.tsx                # 설정, 비밀번호 변경, 로그아웃
└── landing.ts                      # 로그인 뒤 첫 화면 정하기 (R6)
frontend/src/app/(public)/sign-in/page.tsx  # 로그인 뒤 landing으로 보낸다
frontend/e2e/                       # menu·auth·notices·notifications e2e를 바뀐 셸에 맞춘다
```

메뉴 대상 (2026-11-15):

```text
backend/src/main/resources/db/migration/V2_4__menu_audience.sql   # audience 열, 사용자 메뉴 홈·공지 (R9)
backend/src/main/java/com/moa/api/
├── entity/menu/{Menu.java, MenuAudience.java}                    # 대상 enum과 열
├── service/menu/MenuService.java                                 # 대상별 보이는 트리, 하위 메뉴는 부모 대상 (R9, R10)
├── application/v1/menu/MenuApplication.java                      # 관리 메뉴(me), 사용자 메뉴(user)
└── controller/v1/menu/ApiMenuController.java, controller/v1/admin/menu/**  # GET /menus/user, 요청·응답 audience
backend/src/test/java/com/moa/
├── api/AdminMenusAcceptanceTest.java   # US3-AC1~AC4
├── api/MenuAcceptanceTest.java         # 키트. 권한 칸이 빈 관리 메뉴의 보임이 바뀐 만큼 (R8)
└── support/MenuSeed.java               # V2_4도 읽는다
frontend/src/
├── api/menu.ts, types/menu.ts          # user(), audience
├── components/shell/{use-menu.ts, menu.ts}   # 사용자 셸은 /menus/user, 코드 메뉴 비움 (R11)
├── components/menu/icons.ts            # megaphone (home은 이미 있다)
└── app/(admin)/admin/menus/            # 대상 탭, 대화상자 (R11)
frontend/e2e/{menu.spec.ts, support/api.ts}   # 사용자 메뉴 가로채기, 대상 탭
```

프로그램과 공개 범위 (2026-11-15):

```text
backend/src/main/java/com/moa/api/program/                    # 새 패키지 (R13). menu 헬퍼가 갖는다
├── ProgramDefinition.java, ProgramSource.java
├── ProgramRegistry.java                                        # 모으고 시작할 때 검사: 키 형식, 키·경로 겹침, 권한 등록
└── {MenuPrograms, UserPrograms, CodePrograms, LoginHistoryPrograms, RolePrograms, NoticePrograms, HomePrograms}.java
backend/src/main/java/com/moa/common/security/permission/PermissionRegistry.java  # find(key): 권한 이름을 보이려고
backend/src/main/resources/db/migration/V2_5__menu_programs.sql  # program_key·visibility, path·permission 지움 (R14)
backend/src/main/java/com/moa/api/
├── entity/menu/{Menu.java, MenuVisibility.java}
├── service/menu/MenuService.java                              # 보이는 트리, 만들기·고치기 검사 (R14, R15)
├── application/v1/menu/** , controller/v1/admin/menu/**      # 요청·응답 programKey·visibility
└── controller/v1/admin/program/ApiAdminProgramController.java # GET /admin/programs (메뉴 관리 권한)
backend/src/test/java/com/moa/
├── api/AdminMenusAcceptanceTest.java   # US3-AC2·AC4·AC5, US4-AC1~AC5 (R17)
├── api/MenuAcceptanceTest.java         # 키트. 요청 모양이 바뀐 만큼
├── support/MenuSeed.java               # V2_5도 읽는다
└── support/TestPrograms.java           # 테스트에서만 등록하는 로그인 필요 프로그램
frontend/src/
├── api/menu.ts, types/menu.ts          # admin.programs(), Program
└── app/(admin)/admin/menus/            # 프로그램 탭, 목록 열, 대화상자 (R16)
frontend/e2e/{menu.spec.ts, screens.spec.ts, support/api.ts}
```

관리 화면 정리 (2026-11-15):

```text
backend/src/main/resources/db/migration/V2_6__admin_menu_groups.sql  # 운영·설정 묶음, 메뉴 옮기기, 공통코드 이름 (R18)
backend/src/main/java/com/moa/api/
├── program/CodePrograms.java                                   # 이름 "공통코드"
├── service/menu/MenuService.java                               # setEnabled(ids), deleteAll(ids) (R19)
├── application/v1/menu/MenuApplication.java
└── controller/v1/admin/menu/{ApiAdminMenuController, request/MenuEnabledRequest, request/MenuIdsRequest}.java
backend/src/test/java/com/moa/api/{AdminMenusAcceptanceTest, MenuAcceptanceTest, AdminRolesAcceptanceTest}.java
frontend/
├── package.json                                                # @dnd-kit/core, @dnd-kit/sortable (R20)
├── src/components/kit/side-drawer.tsx                          # 공용 드로어 (R21)
├── src/components/shell/landing.ts                             # 묶음 안 첫 화면 메뉴 (R18)
├── src/app/(admin)/admin/menus/{page, menu-tree-table, menu-drawer, bulk-bar}.tsx  # 트리 표·드로어·일괄 줄 (R22)
├── src/api/menu.ts, src/types/menu.ts                          # setEnabled, bulkDelete
└── messages/ko.json                                            # menuAdmin 문구, 머리 안내
frontend/e2e/{menu.spec.ts, auth.spec.ts}
```

**Structure Decision**: 인수 테스트는 키트와 같은 `com.moa.api` 패키지에 둔다. 마이그레이션은 키트 것을 고치지 않고 moa 것을 하나 더한다. 셸 나누기는 프론트만 고치고, 키트 셸 파일을 고친 것은 키트로 옮길 목록에 올린다 (원칙 III).

## Complexity Tracking

위반이 없어 비워 둔다.
