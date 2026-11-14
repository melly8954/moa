# Data Model: 관리 메뉴와 접근 (admin-menus)

새 테이블과 컬럼은 없다. 키트 `menus` 테이블의 스키마는 `V1_7__menus.sql`이 기준이다.

## 처음 구성 (FR-001과 시드의 대응)

모두 최상위 메뉴다 (`parent_id` 없음). 키트 시드(`V1_7`, `V1_7_1`)는 "관리" 그룹(`id` 1) 아래에 넣고,
moa의 `V2__flat_admin_menus.sql`이 최상위로 옮긴 뒤 그룹을 지운다(`deleted_at`).

| 순서 | 메뉴 (`name`) | `path` | `sort_order` | `permission` | 시드 파일 |
|---|---|---|---|---|---|
| 1 | 사용자 관리 | `/admin/users` | 5 | `USER_MANAGE` | `V1_7_1` |
| 2 | 메뉴 관리 | `/admin/menus` | 10 | `MENU_MANAGE` | `V1_7` |
| 3 | 코드 관리 | `/admin/codes` | 20 | `CODE_MANAGE` | `V1_7_1` |
| 4 | 접속 이력 | `/admin/login-histories` | 30 | `LOGIN_HISTORY_READ` | `V1_7_1` |
| 5 | 역할 관리 | `/admin/roles` | 40 | `ROLE_MANAGE` | `V1_7_1` |

## 규칙

- 메뉴마다 권한이 있다. 권한이 있는 메뉴만 보인다 (키트 동작, 스펙 Edge Cases)
- 이 표는 처음 배포할 때의 구성이다. 이후 변경은 메뉴 관리 화면에서 한다
