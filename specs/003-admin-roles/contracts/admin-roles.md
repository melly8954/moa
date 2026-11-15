# Contract: 운영자 역할

US1은 키트 API를 moa의 AC에 맞춰 어떻게 확인하는지만 적는다. US2~US4(2026-11-15)는 새 API 다섯 개를 쓴다(research R6~R8). 모두 역할 관리 권한(`ROLE_MANAGE`)이다. 요청·응답 전체는 local 프로필의 `/v3/api-docs`가 기준이다.

## 메뉴 트리 (US2, 새로)

`GET /api/v1/admin/roles/permission-tree` — `menus`(`menuId`, `name`, `enabled`, `permission`(`key`, `name`), `children`), `unlisted`(`programKey`, `name`, `permission`)

| AC | 준비 | 기대 |
|---|---|---|
| US2-AC1 | 메뉴 처음 구성 | 200. `menus`가 관리 메뉴 6개를 `001` FR-001 순서와 각 프로그램의 권한으로, `unlisted`는 빈 목록 |
| US2-AC2 | `POST /api/v1/admin/roles` (`permissions` = `["LOGIN_HISTORY_READ"]`) | `GET /api/v1/admin/roles/{id}`의 `permissions`가 그것 하나, `GET /api/v1/admin/roles`의 그 역할 권한 수 1 |
| US2-AC3 | 코드 관리 메뉴를 지운다 | `menus`에 코드 관리가 없고 `unlisted`에 `ADMIN_CODES`(권한 `CODE_MANAGE`) |

## 사용자별 역할 (US3, 새로)

`GET /api/v1/admin/roles/users?keyword=&page=&size=` — 페이지. 항목 `id`, `loginId`, `name`, `role`, `accessRoles`(`id`, `code`, `name`)

| AC | 호출 | 기대 |
|---|---|---|
| US3-AC1 | `keyword` 없음 (운영자 A는 역할 있음, B는 없음) | 관리자와 A만. A의 `accessRoles`에 그 역할 |
| US3-AC2 | `keyword` = B의 아이디 | B가 있고 `accessRoles`는 빈 목록 |
| US3-AC3 | `PUT /api/v1/admin/users/{B}/roles` 뒤 B로 `GET /api/v1/admin/login-histories` | 200. `keyword` 없는 목록에 B가 있다 |
| Edge | `PUT /api/v1/admin/users/{관리자}/roles`, `POST .../members` (관리자) | 409, `code` = `B001` |

## 구성원 (US4, 새로)

`GET /api/v1/admin/roles/{id}/members`, `POST /api/v1/admin/roles/{id}/members` (`userId`), `DELETE /api/v1/admin/roles/{id}/members/{userId}`

| AC | 호출 | 기대 |
|---|---|---|
| US4-AC1 | 운영자 둘이 가진 역할의 `GET .../members` | 200. 두 운영자(`id`, `loginId`, `name`) |
| US4-AC2 | 세 번째 운영자 `POST .../members` 뒤 그 운영자로 `GET /api/v1/admin/login-histories` | 2xx, 구성원 셋. 접속 이력 200 |
| US4-AC3 | `DELETE .../members/{운영자}` 뒤 그 운영자로 `GET /api/v1/admin/login-histories` | 2xx. 접속 이력 403 `A005` |
| Edge | 접속 이력 권한만 가진 역할 관리자(운영자)가 코드 관리 권한 역할의 구성원을 더한다 | 403, `code` = `A005` |

## 준비 (세 AC 공통)

| 순서 | 호출 | 기대 |
|---|---|---|
| 1 | 메뉴를 처음 구성으로 되돌리고, 운영자 계정(`USER`)을 만든다 | |
| 2 | ADMIN이 `POST /api/v1/admin/roles` (권한 `LOGIN_HISTORY_READ`만) | 201, `id` |
| 3 | ADMIN이 `PUT /api/v1/admin/users/{운영자 id}/roles` (그 역할 하나) | 성공 |

## AC별 확인 (운영자 토큰)

| AC | 호출 | 기대 |
|---|---|---|
| US1-AC1 | `GET /api/v1/menus/me` | 200. 최상위 항목 "접속 이력" 하나 |
| US1-AC2 | `GET /api/v1/admin/login-histories` | 200 |
| US1-AC3 | `GET /api/v1/admin/users` | 403, `code` = `A005` |
