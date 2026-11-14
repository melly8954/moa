# Contract: 운영자 역할

새 API는 없다. 키트 API를 moa의 AC에 맞춰 어떻게 확인하는지만 적는다. 요청·응답 전체는 local 프로필의 `/v3/api-docs`가 기준이다.

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
