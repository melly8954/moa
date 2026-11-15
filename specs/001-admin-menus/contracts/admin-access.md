# Contract: 관리 메뉴와 접근

키트 API를 moa의 AC에 맞춰 어떻게 확인하는지 적는다. 메뉴 대상(FR-007)으로 사용자 메뉴 API 하나와 메뉴 요청·응답의 `audience`가 더해진다. 프로그램(FR-008·FR-009)으로 프로그램 목록 API가 더해지고, 메뉴 요청·응답의 `path`·`permission`이 `programKey`·`visibility`로 바뀐다(research R15). 요청·응답 전체는 local 프로필의 `/v3/api-docs`가 기준이다.

## 프로그램 (새로)

`GET /api/v1/admin/programs` — 메뉴 관리 권한(`MENU_MANAGE`). 프로그램마다 `key`, `name`, `path`, `audience`, `permission`(`key`, `name`, 사용자 프로그램은 `null`), `signInRequired`

| AC | 호출자 | 기대 |
|---|---|---|
| US4-AC1 | ADMIN | 200. [data-model.md](../data-model.md) 프로그램 표의 8개. 쓰기 API는 없다 |
| US4-AC1 | USER (역할 없음) | 403, `code` = `A005` |

## 메뉴 만들기 (프로그램)

`POST /api/v1/admin/menus` — 요청 `name`, `icon`, `sortOrder`, `parentId`, `audience`, `programKey`(묶음 메뉴면 비움), `visibility`(사용자 메뉴: `PUBLIC`·`MEMBER`)

| AC | 요청 | 기대 |
|---|---|---|
| US4-AC2 | `audience` = `ADMIN`, `programKey` = `NOTICES`(사용자 프로그램) | 400, `code` = `V001` |
| US4-AC2 | `audience` = `ADMIN`, `programKey` = `ADMIN_CODES` | 201. 응답 `program.path` = `/admin/codes`, `program.permissionName` = 코드 관리 |
| US4-AC3 | `audience` = `USER`, 로그인 필요 시험 프로그램, `visibility` = `PUBLIC` | 400, `code` = `V001`. `MEMBER`면 201 |
| US4-AC4 | 묶음 메뉴(`programKey` 없음) 아래 `ADMIN_CODES` 메뉴 | 코드 관리 권한만 받은 운영자의 `GET /api/v1/menus/me`에 그 묶음과 하위 메뉴(`path` = `/admin/codes`) |
| US4-AC5 | 하위 메뉴 없는 묶음 메뉴 | 201. ADMIN의 `GET /api/v1/menus/me`에 없다 |

## 공개 범위 (사용자 메뉴)

| AC | 준비 | 기대 |
|---|---|---|
| US3-AC2 | `visibility` = `PUBLIC` 사용자 메뉴 | 토큰 없음·USER의 `GET /api/v1/menus/user`에 있고 `GET /api/v1/menus/me`에 없다 |
| US3-AC4 | `programKey` = `ADMIN_CODES` 관리 메뉴 | USER(역할 없음)의 `GET /api/v1/menus/me`는 빈 목록 |
| US3-AC5 | `visibility` = `MEMBER` 사용자 메뉴 | 토큰 없음의 `GET /api/v1/menus/user`에 없고 USER에게는 있다 |

## 내 메뉴

`GET /api/v1/menus/me`

| AC | 호출자 | 기대 |
|---|---|---|
| US1-AC1 | ADMIN | 200. 최상위 항목 6개(하위 메뉴 없음), 이름이 [data-model.md](../data-model.md) 표의 순서 |
| US2-AC1 | USER (역할 없음) | 200. 빈 목록 |

대상 `ADMIN` 메뉴만 준다. 메뉴가 가리키는 프로그램의 권한으로 거른다 (research R15). US3-AC4는 아래 "공개 범위" 표에 있다.

## 사용자 메뉴 (새로)

`GET /api/v1/menus/user` — 로그인 없이 부른다. 응답 모양은 `/menus/me`와 같다 (`id`, `name`, `path`, `icon`, `children`)

| AC | 호출자 | 기대 |
|---|---|---|
| US3-AC3 | 토큰 없음 | 관리자가 끈 사용자 메뉴는 없다 |

US3-AC2·AC5는 위 "공개 범위" 표에 있다.

## 메뉴 관리 (대상)

- `GET /api/v1/admin/menus` 응답의 메뉴마다 `audience`(`ADMIN`·`USER`)
- `POST /api/v1/admin/menus`, `PUT /api/v1/admin/menus/{id}` 요청에 `audience`. 하위 메뉴는 보낸 값과 상관없이 부모를 따른다
- 응답의 메뉴마다 `programKey`, `program`(`name`, `path`, `permissionName`, `signInRequired`), `missingProgram`, `visibility`. `path`·`permission`은 없다

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
