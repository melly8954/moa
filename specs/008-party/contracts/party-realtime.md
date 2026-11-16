# Contract: 파티 실시간 (STOMP)

서버가 브라우저에 먼저 알리는 길이다. 보내기는 모두 REST로 한다 ([party-api.md](./party-api.md), research.md R2).

## 연결

알림과 같은 연결 하나를 쓴다. 연결·인증·다시 잇기는 `007-notifications` contracts/realtime.md "연결"을 따른다 (research.md R1, R4).

- 주소 `ws(s)://<백엔드>/ws`(프론트 서버 환경 변수 `WS_URL`), CONNECT 헤더 `Authorization: Bearer <액세스 토큰>`
- 끊기면 `RealtimeProvider`가 5초 뒤 다시 잇는다. 파티 화면은 다시 이은 뒤 `GET /parties/{id}/messages?after={마지막 id}`로 빠진 메시지를 채우고 파티 화면을 다시 불러온다 (FR-018, research.md R5)

## 구독 주소

| 주소 | 누가 구독 | 받는 것 |
|---|---|---|
| `/topic/parties/{id}` | 그 파티의 지금 멤버. 아니면 ERROR 프레임 | 채팅 메시지, 파티 변화 |
| `/user/queue/parties` | 본인 | 나에게만 오는 일 (내보내짐, 방장에게 오는 신청 변화) |

- 경로 이름은 007 규칙(`/topic/<기능>`, `/user/queue/<기능>`)을 따른다
- 멤버가 아니게 된 회원(나감, 내보내짐)의 `/topic/parties/{id}` 구독은 서버가 끊는다
- 화면은 멤버일 때만 `/topic/parties/{id}`를 구독한다. 막힌 구독이 알림과 함께 쓰는 연결을 끊을 수 있기 때문이다 (research.md R3)

## 이벤트

모든 이벤트는 `{ type, partyId, payload, occurredAt }` 모양이다. 트랜잭션이 커밋된 뒤에만 보낸다.

| type | 주소 | payload | 화면이 할 일 | AC |
|---|---|---|---|---|
| `MESSAGE` | topic | Message ([party-api.md](./party-api.md) 채팅) | 채팅 끝에 붙인다 | US9-AC1, US9-AC3 |
| `MEMBERS_CHANGED` | topic | `{members, positions, memberCount, capacity, status, hostUserId}` | 멤버 목록, 빈자리, 방장 표시를 바꾼다 | US9-AC7 |
| `PARTY_UPDATED` | topic | PartyDetail의 파티 정보 부분 | 정보와 참가 방식을 바꾼다 | FR-028 |
| `PARTY_DISSOLVED` | topic | `{}` | 알리고 모집 페이지로 보낸다 | US9-AC8 |
| `KICKED` | user queue | `{partyId}` | 알리고 모집 페이지로 보낸다 | US9-AC8 |
| `JOIN_REQUESTS_CHANGED` | 방장의 user queue | `{pendingCount}` | 신청 목록을 다시 불러온다 | US9-AC9 |

## 확인 방법

- 구독 권한과 이벤트 전달은 기존 `RealtimeAcceptanceTest`를 상속한 `PartyRealtimeAcceptanceTest`에서 STOMP 클라이언트로 확인한다 (plan.md Testing)
- 화면 e2e는 Playwright `page.routeWebSocket`으로 가짜 서버를 둔다
