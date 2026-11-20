# Data Model: 관리 메뉴와 접근 (admin-menus)

> 2026-11-21: 지금 스키마는 `V1__schema.sql`, 처음 데이터는 `V2__seed.sql`에 합쳐져 있다 (`docs/db.md`). 아래 마이그레이션 파일 이름은 합치기 전 기록이다.

새 테이블은 없다. 키트 `tb_menu` 테이블의 스키마는 `V1_7__menus.sql`이 기준이고, moa가 `audience` 열을 더한다 (2026-11-15, `V2_4__menu_audience.sql`, research R9).
이어서 `program_key`·`visibility` 열을 더하고 `path`·`permission` 열을 지운다 (2026-11-15, `V2_5__menu_programs.sql`, research R14). 아래 처음 구성 표의 `path`·`permission`은 `V2_5` 전의 시드 값이고, `V2_5`가 이 값으로 프로그램을 찾는다.

## 관리 메뉴 묶음 (FR-001, 2026-11-15, `V2_6__admin_menu_groups.sql`, research R18)

| 묶음 | `icon` | `sort_order` | 하위 메뉴 (`program_key`, `sort_order`) |
|---|---|---|---|
| 운영 | `briefcase` | 10 | `ADMIN_USERS` 10, `ADMIN_NOTICES` 20, `ADMIN_LOGIN_HISTORIES` 30 |
| 설정 | `settings` | 20 | `ADMIN_ROLES` 10, `ADMIN_MENUS` 20, `ADMIN_CODES` 30 (이름 "코드 관리", 2026-11-21에 "공통코드"에서 바꿈) |

아래 "처음 구성" 표는 `V2_6` 전의 최상위 구성이다.

## 프로그램 (FR-008, 코드에 등록, research R13)

테이블이 아니라 `ProgramSource` 빈이 낸다.

| `key` | 이름 | `path` | `audience` | `permission` | `signInRequired` |
|---|---|---|---|---|---|
| `ADMIN_USERS` | 사용자 관리 | `/admin/users` | `ADMIN` | `USER_MANAGE` | - |
| `ADMIN_MENUS` | 메뉴 관리 | `/admin/menus` | `ADMIN` | `MENU_MANAGE` | - |
| `ADMIN_CODES` | 코드 관리 | `/admin/codes` | `ADMIN` | `CODE_MANAGE` | - |
| `ADMIN_LOGIN_HISTORIES` | 접속 이력 | `/admin/login-histories` | `ADMIN` | `LOGIN_HISTORY_READ` | - |
| `ADMIN_ROLES` | 역할 관리 | `/admin/roles` | `ADMIN` | `ROLE_MANAGE` | - |
| `ADMIN_NOTICES` | 공지 관리 | `/admin/notices` | `ADMIN` | `NOTICE_MANAGE` | - |
| `HOME` | 홈 | `/` | `USER` | - | false |
| `NOTICES` | 공지 | `/notices` | `USER` | - | false |

## `tb_menu.program_key`, `tb_menu.visibility` (FR-007, FR-009)

| 열 | 형 | 값 | 설명 |
|---|---|---|---|
| `program_key` | `VARCHAR(50) NULL` | 프로그램 키, `MISSING` | 비면 묶음 메뉴(1단계에만). 등록되지 않은 키면 없는 프로그램 |
| `visibility` | `VARCHAR(10) NULL` | `PUBLIC`, `MEMBER` | 사용자 화면 메뉴의 공개 범위. 관리 메뉴와 묶음 메뉴는 비운다 |

`V2_5`는 기존 메뉴의 `path`로 프로그램을 찾아 `program_key`를 채우고, 사용자 메뉴의 `visibility`를 `PUBLIC`으로 둔 뒤 `path`·`permission` 열을 지운다. 경로가 맞는 프로그램이 없으면 `MISSING`이다.

## `tb_menu.audience` (FR-007)

| 열 | 형 | 값 | 설명 |
|---|---|---|---|
| `audience` | `VARCHAR(10) NOT NULL DEFAULT 'ADMIN'` | `ADMIN`, `USER` | 메뉴 대상. `ADMIN`은 관리 셸, `USER`는 사용자 셸에 보인다. 하위 메뉴는 부모와 같다 |

## 처음 구성 (FR-001과 시드의 대응)

모두 최상위 메뉴다 (`parent_id` 없음). 키트 시드(`V1_7`, `V1_7_1`)는 "관리" 그룹(`id` 1) 아래에 넣고,
moa의 `V2__flat_admin_menus.sql`이 최상위로 옮긴 뒤 그룹을 지운다(`deleted_at`). 공지 관리는 `006-notices`의 `V2_2__notice_admin_menu.sql`이 처음부터 최상위에 넣는다.

| 순서 | 메뉴 (`name`) | `path` | `sort_order` | `permission` | 시드 파일 |
|---|---|---|---|---|---|
| 1 | 사용자 관리 | `/admin/users` | 5 | `USER_MANAGE` | `V1_7_1` |
| 2 | 메뉴 관리 | `/admin/menus` | 10 | `MENU_MANAGE` | `V1_7` |
| 3 | 코드 관리 | `/admin/codes` | 20 | `CODE_MANAGE` | `V1_7_1` |
| 4 | 접속 이력 | `/admin/login-histories` | 30 | `LOGIN_HISTORY_READ` | `V1_7_1` |
| 5 | 역할 관리 | `/admin/roles` | 40 | `ROLE_MANAGE` | `V1_7_1` |
| 6 | 공지 관리 | `/admin/notices` | 50 | `NOTICE_MANAGE` | `V2_2` (moa, `006-notices`) |

## 사용자 메뉴 처음 구성 (FR-007, `V2_4`)

| 순서 | 메뉴 (`name`) | `path` | `icon` | `sort_order` | `permission` | `audience` |
|---|---|---|---|---|---|---|
| 1 | 홈 | `/` | `home` | 10 | 없음 | `USER` |
| 2 | 공지 | `/notices` | `megaphone` | 20 | 없음 | `USER` |

위 관리 메뉴 표의 메뉴는 모두 `audience` = `ADMIN`이다.

## 규칙

- 관리 메뉴는 가리키는 프로그램의 권한을 가진 사용자에게만 보인다. 묶음 메뉴는 보이는 하위 메뉴가 있을 때만 보인다 (FR-002)
- 사용자 메뉴는 `PUBLIC`이면 로그인하지 않은 사람까지 모두, `MEMBER`면 로그인한 사용자에게만 보인다. 로그인이 필요한 프로그램은 `MEMBER`만 된다 (FR-007)
- 없는 프로그램을 가리키는 메뉴는 보이지 않는다 (스펙 Edge Cases)
- 이 표는 처음 배포할 때의 구성이다. 이후 변경은 메뉴 관리 화면에서 한다
