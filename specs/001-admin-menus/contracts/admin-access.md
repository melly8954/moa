# Contract: 관리 메뉴와 접근

새 API는 없다. 키트 API를 moa의 AC에 맞춰 어떻게 확인하는지만 적는다. 요청·응답 전체는 local 프로필의 `/v3/api-docs`가 기준이다.

## 내 메뉴

`GET /api/v1/menus/me`

| AC | 호출자 | 기대 |
|---|---|---|
| US1-AC1 | ADMIN | 200. "관리" 그룹 하나, 하위 메뉴 이름이 [data-model.md](../data-model.md) 표의 순서 |
| US2-AC1 | USER (역할 없음) | 200. 빈 목록 |

## 관리 화면이 쓰는 목록 API

| 화면 | 목록 API |
|---|---|
| 사용자 관리 | `GET /api/v1/admin/users` |
| 메뉴 관리 | `GET /api/v1/admin/menus` |
| 코드 관리 | `GET /api/v1/admin/code-groups` |
| 접속 이력 | `GET /api/v1/admin/login-histories` |
| 역할 관리 | `GET /api/v1/admin/roles` |

| AC | 호출자 | 기대 |
|---|---|---|
| US1-AC2 | ADMIN | 다섯 API 모두 200 |
| US2-AC2 | USER (역할 없음) | 다섯 API 모두 403, `code` = `A005`. 쓰기 요청(`POST /api/v1/admin/menus`)도 403 `A005` |
| US2-AC3 | 토큰 없음 | 다섯 API 모두 401, `code` = `A001` |
