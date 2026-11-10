# Data Model: 접속 이력 (admin-login-history)

새 테이블과 컬럼은 없다. 키트 `login_histories`가 기준이다.

## 접속 이력

| 스펙의 항목 | 응답 필드 |
|---|---|
| 입력한 아이디 | `loginId` |
| 성공 여부 | `success` (실패면 `failureCode`) |
| 접속 IP | `ip` |
| 기기 종류, OS, 브라우저 | `deviceType`, `os`, `browser` |
| 시각 | `createdAt` (시간대 포함) |

## 보관 기간

| 설정 | 지금 | 바꾼 뒤 |
|---|---|---|
| `app.login-history.retention-days` 기본값 (`application.yaml`) | 365 | 90 |
| `LOGIN_HISTORY_RETENTION_DAYS` (`backend/.env.example`) | 365 | 90 |

생성 시각이 "지금 − 보관 기간"보다 이전인 이력은 매일 정리 때 지워진다 (키트).

## 조회 조건 (키트)

- 아이디: 정확히 일치, 50자 이하
- 결과: 성공만 또는 실패만, 없으면 전체
- 기간: 시작일·종료일 포함
- 정렬: 최신순
