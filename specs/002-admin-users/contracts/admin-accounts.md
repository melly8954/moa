# Contract: 관리자·운영자 계정

새 API는 없다(2026-11-21 절은 응답 칸과 규칙만 바꾼다). 키트 API를 moa의 AC에 맞춰 어떻게 확인하는지만 적는다. 요청·응답 전체는 local 프로필의 `/v3/api-docs`가 기준이다.

## 첫 관리자 (US1-AC1)

| 순서 | 호출 | 기대 |
|---|---|---|
| 1 | 빈 DB에서 첫 관리자를 만든다 (`UserApplication.bootstrapAdmin`) | 만들어진다 |
| 2 | `POST /api/v1/auth/login` (그 아이디와 임시 비밀번호) | 200, `accessToken` |
| 3 | `GET /api/v1/users/me` | `passwordChangeRequired` = `true` |
| 4 | `PUT /api/v1/users/me/password` | 204 |
| 5 | 새 비밀번호로 다시 로그인 | 200 |
| 6 | 새 토큰으로 `GET /api/v1/users/me` | `passwordChangeRequired` = `false` |
| 7 | 새 토큰으로 `GET /api/v1/admin/users` | 200 |

## 운영자 계정 (US2-AC1)

| 순서 | 호출 | 기대 |
|---|---|---|
| 1 | 최고 관리자가 `POST /api/v1/admin/users` (`role` = `ADMIN`, 2026-11-21 전에는 `USER`) | 201, `temporaryPassword`가 있다 |
| 2 | 최고 관리자가 `GET /api/v1/admin/users/{id}` | 200, 본문에 `temporaryPassword`가 없다 |
| 3 | 그 아이디와 임시 비밀번호로 `POST /api/v1/auth/login` | 200 |
| 4 | 그 토큰으로 `GET /api/v1/users/me` | `passwordChangeRequired` = `true` |

## 관리자 계정과 다루기 범위 (2026-11-21, research R9·R10)

응답 `UserAdminResponse`에 `superAdmin`, `protection`(`SUPER_ADMIN_ONLY`·`WIDER_PERMISSIONS`·null)을 더한다. "사용자 관리 운영자"는 `USER_MANAGE`만 묶은 역할을 받은 `ADMIN` 계정이다.

| AC | 호출 | 기대 |
|---|---|---|
| US1-AC1 | 부트스트랩 계정으로 `GET /api/v1/menus/me` | 관리 메뉴 전부. 그 계정이 최고 관리자 역할 구성원 |
| US2-AC1 | 새 `ADMIN` 계정으로 `GET /api/v1/menus/me` | 200 빈 목록 |
| US3-AC1 | `GET /api/v1/admin/users` | 운영자 `accessRoles` = 보안 담당, 최고 관리자 `superAdmin` = true, 역할 없는 `ADMIN`은 빈 목록 |
| US3-AC3 | `PUT /api/v1/admin/users/{id}/roles` (보안 담당 + 공지 담당) | 204. 그 계정 `GET /api/v1/admin/notices` 200 |
| US4-AC1 | 사용자 관리 운영자가 회원에게 이름·잠그기·비밀번호 초기화·`PUT /{id}/role` (`ADMIN`) | 모두 성공 |
| US4-AC2 | 같은 운영자가 접속 이력 운영자에게 `POST /{id}/password-reset` | 403 `A005`. 목록의 그 행 `protection` = `WIDER_PERMISSIONS` |
| US4-AC3 | 같은 운영자가 최고 관리자에게 `PUT /{id}/lock` | 403 `A005`. `protection` = `SUPER_ADMIN_ONLY` |
| US4-AC5 | 최고 관리자가 역할 둘인 운영자에게 `PUT /{id}/role` (`USER`) | 204. `GET /api/v1/admin/users/{id}/roles` 빈 목록, 그 계정의 리프레시 토큰이 끊김 |
| FR-007 | 하나뿐인 최고 관리자를 잠그기·지우기·회원으로 바꾸기 | 409 `B001` (내 행이면 지금처럼 `B001`) |
