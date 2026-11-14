# Contract: 실시간 (STOMP)과 웹 푸시

키트 websocket 헬퍼의 규칙을 따른다. 구독 경로 규칙은 spec FR-010과 Assumptions.

## 연결

- 주소: `ws(s)://<백엔드>/ws` (프론트는 서버 환경 변수 `WS_URL`, research R2)
- CONNECT 프레임 헤더 `Authorization: Bearer <액세스 토큰>`. 없거나 무효하면 거절 (키트)
- 로그인하지 않은 화면은 연결하지 않는다 (FR-001)

## 구독 경로

| 경로 | 받는 사람 | 메시지 (JSON) |
|---|---|---|
| `/topic/notices` | 연결된 모두 | {type: "NOTICE_PUBLISHED", id, title, link} |
| `/user/queue/notifications` | 그 사용자의 연결된 모든 화면 | {type: "NOTIFICATION_CREATED", id, notificationType, title, link, createdAt} |

- 다른 기능의 경로 이름도 같은 규칙: 전체 `/topic/<기능>`, 한 사용자 `/user/queue/<기능>`, 보내기 `/app/<기능>/...`
- 보내는 때는 DB 커밋 뒤 (research R4)

## 웹 푸시 페이로드 (개인 알림만)

```json
{ "id": 42, "title": "새 댓글이 달렸어요", "link": "/posts/7" }
```

- 서비스 워커가 화면이 보이는 상태면 띄우지 않고, 아니면 `title`로 브라우저 알림을 띄운다. 누르면 `link`를 연다 (research R7)

## AC와 확인 방법

| AC | 백엔드 (`NotificationsAcceptanceTest`) | 화면 (`e2e/notifications.spec.ts`) |
|---|---|---|
| US1-AC1 | ADMIN 공지 게시 → `/topic/notices` 구독자가 {type, id, title} 받음 | STOMP 흉내로 메시지 → 토스트에 제목, "공지" 메뉴 점 |
| US1-AC2 | 임시 저장·고치기 → 일정 시간 안에 아무것도 오지 않음 | |
| US1-AC3 | 토큰 없이 연결 → 거절 | 로그아웃 화면은 연결하지 않음 |
| US1-AC4 | | 토스트 누르면 `/notices/{id}` |
| US3-AC1 | `notify` → 그 사용자 `/user/queue/notifications`로 받음 | 메시지 → 토스트, 벨 숫자 하나 증가 |
| US3-AC2 | 두 사용자 연결, 한 사람에게 `notify` → 다른 사람은 받지 않음 | |
| US3-AC3 | 같은 사용자로 두 연결 → 둘 다 받음 | |
| US4-AC4 | | `Notification.permission = "denied"` 흉내 → 꺼짐 + 브라우저 설정 안내 |
| US4-AC2 | (화면 직접 확인) 브라우저 알림 누르면 관련 화면 | |
