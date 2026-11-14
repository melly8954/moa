# Data Model: 관리 메뉴와 접근 (admin-menus)

새 테이블은 없다. 키트 `menus` 테이블의 스키마는 `V1_7__menus.sql`이 기준이고, moa가 `audience` 열을 더한다 (2026-11-15, `V2_4__menu_audience.sql`, research R9).

## `menus.audience` (FR-007)

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
| 1 | 홈 | `/` | `house` | 10 | 없음 | `USER` |
| 2 | 공지 | `/notices` | `megaphone` | 20 | 없음 | `USER` |

위 관리 메뉴 표의 메뉴는 모두 `audience` = `ADMIN`이다.

## 규칙

- 메뉴마다 권한이 있다. 권한이 있는 메뉴만 보인다 (키트 동작, 스펙 Edge Cases)
- 권한 칸이 빈 메뉴: `ADMIN` 대상이면 시스템 역할 `ADMIN`만, `USER` 대상이면 로그인하지 않은 사람까지 모두 본다 (FR-002, FR-007)
- 이 표는 처음 배포할 때의 구성이다. 이후 변경은 메뉴 관리 화면에서 한다
