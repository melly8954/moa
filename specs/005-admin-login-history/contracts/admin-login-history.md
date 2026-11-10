# Contract: 접속 이력

새 API는 없다. 키트 API를 moa의 AC에 맞춰 어떻게 확인하는지만 적는다. 요청·응답 전체는 local 프로필의 `/v3/api-docs`가 기준이다.

## US1-AC1

| 순서 | 호출 | 기대 |
|---|---|---|
| 1 | `POST /api/v1/auth/login` (그 아이디, 틀린 비밀번호, 접속 IP와 User-Agent) | 실패 이력이 남는다 |
| 2 | `POST /api/v1/auth/login` (그 아이디, 맞는 비밀번호, 같은 IP와 User-Agent) | 성공 이력이 남는다 |
| 3 | 다른 아이디로 한 번 로그인 | 다른 이력이 남는다 |
| 4 | ADMIN이 `GET /api/v1/admin/login-histories?loginId={그 아이디}` | 200. 2건, `content[0]`은 성공, `content[1]`은 실패(`failureCode` = `A006`). 두 건 모두 `ip`가 준 주소이고 `deviceType`·`os`·`browser`가 채워져 있다 |

## FR-002 (보관 기간)

| 순서 | 동작 | 기대 |
|---|---|---|
| 1 | 89일 전 이력 하나, 91일 전 이력 하나를 넣는다 | |
| 2 | `LoginHistoryCleaner.deleteExpired()` | 91일 전 이력만 지워지고 89일 전 이력은 남는다 |
