# Contract: 운영자 역할

US1은 키트 API를 moa의 AC에 맞춰 어떻게 확인하는지만 적는다. US2~US4(2026-11-15)는 새 API 다섯 개를 쓴다(research R6~R8). 모두 역할 관리 권한(`ROLE_MANAGE`)이다. 요청·응답 전체는 local 프로필의 `/v3/api-docs`가 기준이다.

## 메뉴 트리 (US2, 새로)

`GET /api/v1/admin/roles/permission-tree` — `menus`(`menuId`, `name`, `enabled`, `permission`(`key`, `name`), `children`), `unlisted`(`screenKey`, `name`, `permission`)

| AC | 준비 | 기대 |
|---|---|---|
| US2-AC1 | 메뉴 처음 구성 | 200. `menus`가 관리 메뉴 6개를 `001` FR-001 순서와 각 연결 화면의 권한으로, `unlisted`는 빈 목록 |
| US2-AC2 | `POST /api/v1/admin/roles` (`permissions` = `["LOGIN_HISTORY_READ"]`) | `GET /api/v1/admin/roles/{id}`의 `permissions`가 그것 하나, `GET /api/v1/admin/roles`의 그 역할 권한 수 1 |
| US2-AC3 | 코드 관리 메뉴를 지운다 | `menus`에 코드 관리가 없고 `unlisted`에 `ADMIN_CODES`(권한 `CODE_MANAGE`) |

## 관리 화면 정리 (2026-11-15, research R12·R13)

아래 "사용자별 역할"과 "구성원"의 한 사람 API는 이 절의 API로 바뀐다.

| AC | 호출 | 기대 |
|---|---|---|
| US3-AC3 | `POST /api/v1/admin/roles/bulk-delete` (`ids` 둘) | 204. 둘 다 없고 구성원에게서도 빠짐. 다룰 수 없는 역할이 섞이면 403 `A005`, 아무것도 안 지워짐 |
| US4-AC1 | `GET /api/v1/admin/roles/{id}/members?size=20` (구성원 21명) | 200. `content` 20개, `totalElements` 21, 모두 `member` = true |
| US4-AC2 | `GET .../members?keyword=op` → `POST .../members` (`userIds` 둘) | 검색 결과에 `member` 표시. 204, 두 사람 접속 이력 200 |
| US4-AC3 | `POST .../members/remove` (`userIds` 둘) | 204. 두 사람 접속 이력 403 `A005` |
| US4-AC4 | `POST .../members` (`userIds`에 관리자 포함) | 409 `B001`. 아무도 안 더해짐 |

## 관리자 계정과 최고 관리자 역할 (2026-11-21, research R16~R18)

이 절이 위 "관리 화면 정리"의 US4-AC4(`ADMIN`이면 B001)를 바꾼다. 이제 `USER`면 B001이다. 아래 "운영자"는 그 권한을 묶은 역할을 받은 `ADMIN` 계정이다.

응답에 더하는 칸:

- `GET /api/v1/admin/roles` 항목: `fixed`, `permissionNames`, `grantable`, `blockReason`(`SUPER_ADMIN_ONLY`·`WIDER_PERMISSIONS`·null). 최고 관리자 역할이 맨 앞
- `GET /api/v1/admin/roles/{id}/members` 항목: `role`을 빼고 `superAdmin`. 검색은 `ADMIN` 계정만

| AC | 호출 | 기대 |
|---|---|---|
| US1-AC4 | 역할 없는 `ADMIN`으로 `GET /api/v1/menus/me`, `GET /api/v1/admin/login-histories` | 200 빈 목록, 403 `A005` |
| US2-AC1 | 부트스트랩 뒤 `GET /api/v1/admin/roles` | 첫 항목 `code` = `SUPER_ADMIN`, `fixed` = true. `GET .../members`에 첫 관리자 |
| US2-AC2 | 최고 관리자 역할에 `PUT /api/v1/admin/roles/{id}`, `DELETE`, `bulk-delete`(다른 역할과 섞어) | 409 `B001`. 아무것도 안 바뀜 |
| US2-AC3 | 테스트 연결 화면을 등록한 뒤 최고 관리자로 그 화면의 API | 200 |
| US2-AC4 | 최고 관리자 자신이 최고 관리자 역할에 `POST .../members`, `POST .../members/remove` (2026-11-22 한 명 고정) | 409 `B001`. 구성원은 그대로 한 명 |
| US2-AC5 | 최고 관리자가 `PUT /api/v1/admin/users/{운영자}/roles`에 최고 관리자 역할을 넣기, `PUT /api/v1/admin/users/2/roles`로 최고 관리자 역할 빼기. 목록의 최고 관리자 역할 | 409 `B001`. 최고 관리자 역할 `grantable` = false, `blockReason` = `FIXED` |
| US5-AC4 | `GET .../members?keyword=` (회원·최고 관리자 섞임), 다른 역할에 최고 관리자 계정 `POST .../members` | 회원 없음, 최고 관리자 행 `superAdmin` = true. 409 `B001` |
| FR-002 | 회원에게 `POST .../members`, `PUT /api/v1/admin/users/{userId}/roles` | 409 `B001` |
| FR-008 | 사용자 관리·권한 관리 권한을 가진 운영자가 `USER_MANAGE`를 담은 `POST /api/v1/admin/roles` | 403 `A005` |
| FR-010·SC-006 | DB에 최고 관리자 역할 줄을 하나 더 넣는다(SQL) | DB가 거부(유일 제약 `uk_user_access_role_super_slot`) |

## 사용자별 역할 (US3, 새로 — 2026-11-15 관리 화면 정리로 없앰)

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
