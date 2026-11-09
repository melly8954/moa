# Contract: 관리자·운영자 계정

새 API는 없다. 키트 API를 moa의 AC에 맞춰 어떻게 확인하는지만 적는다. 요청·응답 전체는 local 프로필의 `/v3/api-docs`가 기준이다.

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
| 1 | ADMIN이 `POST /api/v1/admin/users` (`role` = `USER`) | 201, `temporaryPassword`가 있다 |
| 2 | ADMIN이 `GET /api/v1/admin/users/{id}` | 200, 본문에 `temporaryPassword`가 없다 |
| 3 | 그 아이디와 임시 비밀번호로 `POST /api/v1/auth/login` | 200 |
| 4 | 그 토큰으로 `GET /api/v1/users/me` | `passwordChangeRequired` = `true` |
