# Data Model: 알림 (notifications)

moa 마이그레이션 `V2_3__notifications.sql`을 더한다. 전체 알림(공지 게시)은 저장하지 않는다.

## 개인 알림 (`notifications`)

| 칸 | 형식 | 규칙 |
|---|---|---|
| `id` | BIGINT, 자동 증가 | |
| `user_id` | BIGINT | 받는 사람. 필수 |
| `type` | VARCHAR(30) | `NotificationType` (지금은 `SYSTEM`, 기능이 생길 때 더함, research R5) |
| `title` | VARCHAR(200) | 필수, 1~200자 |
| `link` | VARCHAR(500) | 관련 화면 경로. `/`로 시작하는 moa 경로만 |
| `created_at` | DATETIME(6) | 만든 시각 |
| `read_at` | DATETIME(6) NULL | 읽은 시각. 비어 있으면 안 읽음 |

- 인덱스: 목록 `(user_id, created_at)`, 안 읽은 수 `(user_id, read_at)`, 정리 `(created_at)`
- 목록 정렬: `created_at DESC, id DESC`
- 지우기: 하드 삭제 (`@HardDelete`). 90일 지나면 정리기가 지운다 (FR-009, research R9). 사용자가 지우는 기능은 없다
- 감사 필드를 두지 않는다. 보내는 쪽이 시스템일 수 있고, 받는 사람만 읽음을 바꾼다

### 상태

```text
안 읽음(read_at 없음) ──누름·모두 읽음──→ 읽음(read_at = 지금)
```

- 이미 읽은 알림을 다시 누르면 `read_at`을 바꾸지 않는다

## 브라우저 알림 구독 (`push_subscriptions`)

| 칸 | 형식 | 규칙 |
|---|---|---|
| `id` | BIGINT, 자동 증가 | |
| `user_id` | BIGINT | 구독한 사용자 |
| `endpoint` | VARCHAR(1000) | 브라우저 푸시 서비스 주소. 유일 (`uk_push_subscriptions_endpoint`, 앞 1000자) |
| `p256dh` | VARCHAR(200) | 브라우저 공개 키 |
| `auth` | VARCHAR(100) | 인증 비밀 |
| `created_at` | DATETIME(6) | |

- 같은 `endpoint`로 다시 저장하면 그 행의 `user_id`·키를 바꾼다 (공유 PC, research R8)
- 끄기·로그아웃 때 지운다. 푸시 서버가 404·410을 돌려주면 지운다 (research R6)
- 하드 삭제 (`@HardDelete`)

## 전체 알림 (저장 안 함)

| 종류 | 내용 | 경로 |
|---|---|---|
| 공지 게시 | 공지 `id`, `title`, `link` = `/notices/{id}` | `/topic/notices` |

메시지 모양은 [contracts/realtime.md](./contracts/realtime.md).
