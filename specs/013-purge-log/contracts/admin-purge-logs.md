# API: 관리자 파기 이력

권한 `PURGE_LOG_READ`가 있어야 한다. 없으면 `001-admin-menus` FR-003대로 401(로그인 안 함) 또는 403(권한 없음).

## GET /api/v1/admin/purge-logs

파기 이력 목록. 최신순(`createdAt` 내림차순). 조건은 모두 선택이고 함께 주면 AND.

### 쿼리

| 이름 | 타입 | 뜻 |
|---|---|---|
| `from` | 날짜 (`2026-11-01`) | 시작일 (포함) |
| `to` | 날짜 | 종료일 (포함, 그날 끝까지). `from`보다 이르면 V001 |
| `target` | `LOGIN_HISTORY` \| `NOTIFICATION` \| `REFRESH_TOKEN` \| `RICH_TEXT_IMAGE` \| `MEMBER` | 대상 종류. 없으면 전체 |
| `page` | 정수, 0부터 | 기본 0 |
| `size` | 정수 1~100 | 기본 20 |

### 응답 200

`PageResponse<PurgeLogListResponse>` (접속 이력과 같은 쪽 나눔 모양)

| 필드 | 타입 | 뜻 |
|---|---|---|
| `id` | 정수 | 파기 이력 ID |
| `createdAt` | 시각 (시간대 포함) | 파기 일시 |
| `target` | 문자열 (위 enum) | 대상 종류 |
| `method` | `ROW_DELETE` \| `COLUMN_ERASE` | 방법 |
| `count` | 정수 | 건수 |
| `actor` | `SCHEDULER` \| `PERSON` | 처리자 |

- 개별 파기의 `actorUserId`·`targetUserId`는 이번 응답에 넣지 않는다. 쓰는 화면이 생기면(회원 탈퇴 스펙) 그때 더한다

### 오류

| 코드 | 언제 |
|---|---|
| V001 | `from`이 `to`보다 늦다, `size`가 범위 밖이다, `target`이 목록 밖이다 |
