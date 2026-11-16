# Contract: 파티 실시간 (STOMP)

서버가 브라우저에 먼저 알리는 길이다. 보내기는 모두 REST로 한다 ([party-api.md](./party-api.md), research.md R2).

## 연결

- 주소: `{NEXT_PUBLIC_WS_URL}`. 백엔드 경로는 `/ws`이고 `PublicPaths`로 연다 (research.md R3, R4)
- `CONNECT` 프레임에 `Authorization: Bearer <액세스 토큰>`을 싣는다. 토큰이 없거나 틀리면 ERROR 프레임을 보내고 연결을 닫는다
- 연결이 끊기면 클라이언트는 다시 잇는다. 이을 때마다 지금 액세스 토큰을 쓰고, 이은 뒤 `GET /parties/{id}/messages?after={마지막 id}`로 빠진 메시지를 채우고 파티 화면을 다시 불러온다 (FR-018)

## 구독 주소

| 주소 | 누가 구독 | 받는 것 |
|---|---|---|
| `/topic/parties/{id}` | 그 파티의 지금 멤버. 아니면 ERROR 프레임 | 채팅 메시지, 파티 변화 |
| `/user/queue/parties` | 본인 | 나에게만 오는 일 (내보내짐, 방장에게 오는 신청 변화) |

멤버가 아니게 된 회원(나감, 내보내짐)의 `/topic/parties/{id}` 구독은 서버가 끊는다.

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

- 구독 권한과 이벤트 전달은 실제 서버를 띄우는 실시간 통합 테스트에서 STOMP 클라이언트로 확인한다 (plan.md Testing)
- 화면 e2e는 Playwright `page.routeWebSocket`으로 가짜 서버를 둔다
