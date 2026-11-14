# Contract: 알림 HTTP API

`/api/v1/notifications`. 모두 로그인이 필요하다 (없으면 401 `A001`). 역할·권한은 묻지 않고, 늘 자기 알림만 다룬다. 요청·응답 전체는 local 프로필의 `/v3/api-docs`가 기준이다.

## 알림함

| 요청 | 설명 | 응답 |
|---|---|---|
| `GET /api/v1/notifications?page=&size=` | 내 개인 알림, 최신순. 벨 펼침은 `size=10` | 200 `PageResponse` {id, type, title, link, createdAt, read} |
| `GET /api/v1/notifications/unread-count` | 안 읽은 수 | 200 {count} |
| `PUT /api/v1/notifications/{id}/read` | 읽음으로 (이미 읽었으면 그대로) | 204. 내 알림이 아니거나 없으면 404 `R001` |
| `PUT /api/v1/notifications/read-all` | 내 알림 모두 읽음으로 | 204 |

## 브라우저 알림 구독

| 요청 | 설명 | 응답 |
|---|---|---|
| `GET /api/v1/notifications/push/public-key` | VAPID 공개 키 | 200 {publicKey}. 웹 푸시를 끈 서버(키 없음)면 200 {publicKey: null} |
| `POST /api/v1/notifications/push-subscriptions` | 이 브라우저 구독 저장. 본문 {endpoint, keys: {p256dh, auth}} | 204. 같은 endpoint면 내 것으로 바꾼다 |
| `DELETE /api/v1/notifications/push-subscriptions?endpoint=` | 이 브라우저 구독 지우기 (끄기, 로그아웃 전) | 204. 없으면 그대로 204 |

## 다른 기능이 부르는 것 (HTTP 아님)

```text
NotificationApplication.notify(userId, NotificationType type, String title, String link)
```

- 부르는 쪽 트랜잭션에서 저장하고, 커밋 뒤 실시간·웹 푸시로 보낸다 (research R4, R5)

## AC와 확인 방법

| AC | 확인 |
|---|---|
| US2-AC1 | 사용자에게 `notify` 3번 → `GET unread-count` = 3 |
| US2-AC2 | 알림 12개 → `GET ?size=10` 10개, 최신순, 칸 {title, createdAt, read} |
| US2-AC3 | `PUT {id}/read` → 204, `unread-count` 하나 줄고 목록의 그 알림 `read`=true. 화면 이동은 e2e |
| US2-AC4 | `PUT read-all` → `unread-count` = 0 |
| US2-AC5 | 두 사용자에게 알림 → 각자 목록에 자기 것만. 남의 알림 `PUT {id}/read`는 404 `R001` |
| US2-AC6 | 토큰 없이 네 API → 401 `A001`. 화면은 AuthGate가 로그인으로 보낸다 |
| US4-AC1 | 구독 저장(가짜 푸시 서버 주소) → `notify` → 가짜 서버가 요청 하나를 받는다 |
| US4-AC3 | 구독 저장 → `DELETE` → `notify` → 가짜 서버가 받지 않는다 |
| US4-AC5 | 구독 저장 → 로그아웃 흐름(`DELETE` 뒤 로그아웃) → `notify` → 받지 않는다 |
