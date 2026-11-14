# Contract: 관리 메뉴와 접근

키트 API를 moa의 AC에 맞춰 어떻게 확인하는지 적는다. 메뉴 대상(FR-007)으로 사용자 메뉴 API 하나와 메뉴 요청·응답의 `audience`가 더해진다. 요청·응답 전체는 local 프로필의 `/v3/api-docs`가 기준이다.

## 내 메뉴

`GET /api/v1/menus/me`

| AC | 호출자 | 기대 |
|---|---|---|
| US1-AC1 | ADMIN | 200. 최상위 항목 6개(하위 메뉴 없음), 이름이 [data-model.md](../data-model.md) 표의 순서 |
| US2-AC1 | USER (역할 없음) | 200. 빈 목록 |
| US3-AC4 | USER (역할 없음) | 권한 칸이 빈 `ADMIN` 대상 메뉴를 더해도 200. 빈 목록 |

대상 `ADMIN` 메뉴만 준다. 권한 칸이 빈 메뉴는 시스템 역할 `ADMIN`에게만 보인다 (research R10).

## 사용자 메뉴 (새로)

`GET /api/v1/menus/user` — 로그인 없이 부른다. 응답 모양은 `/menus/me`와 같다 (`id`, `name`, `path`, `icon`, `children`)

| AC | 호출자 | 기대 |
|---|---|---|
| US3-AC2 | 토큰 없음, USER | 200. 홈, 공지, 그리고 권한 없이 더한 `USER` 대상 메뉴. `ADMIN` 대상 메뉴는 없다 |
| US3-AC3 | 토큰 없음 | 관리자가 끈 사용자 메뉴는 없다 |

## 메뉴 관리 (대상)

- `GET /api/v1/admin/menus` 응답의 메뉴마다 `audience`(`ADMIN`·`USER`)
- `POST /api/v1/admin/menus`, `PUT /api/v1/admin/menus/{id}` 요청에 `audience`. 하위 메뉴는 보낸 값과 상관없이 부모를 따른다

| AC | 호출자 | 기대 |
|---|---|---|
| US3-AC1 | ADMIN | `GET /api/v1/admin/menus`에 관리 메뉴 6개(`ADMIN`)와 홈·공지(`USER`)가 대상과 함께 온다 |

## 관리 화면이 쓰는 목록 API

| 화면 | 목록 API |
|---|---|
| 사용자 관리 | `GET /api/v1/admin/users` |
| 메뉴 관리 | `GET /api/v1/admin/menus` |
| 코드 관리 | `GET /api/v1/admin/code-groups` |
| 접속 이력 | `GET /api/v1/admin/login-histories` |
| 역할 관리 | `GET /api/v1/admin/roles` |
| 공지 관리 | `GET /api/v1/admin/notices` |

| AC | 호출자 | 기대 |
|---|---|---|
| US1-AC2 | ADMIN | 여섯 API 모두 200 |
| US2-AC2 | USER (역할 없음) | 여섯 API 모두 403, `code` = `A005`. 쓰기 요청(`POST /api/v1/admin/menus`)도 403 `A005` |
| US2-AC3 | 토큰 없음 | 여섯 API 모두 401, `code` = `A001` |
