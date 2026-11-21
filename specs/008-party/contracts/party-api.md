# Contract: 파티 REST API

모두 `/api/v1` 아래에 있고, 로그인해야 부를 수 있다(없으면 401 A001). 요청·응답 전체는 구현 뒤 local 프로필의 `/v3/api-docs`가 기준이다. 여기에는 경로, 누가 부르는지, 성공 응답, 오류 코드, AC와의 대응만 적는다.

회원은 응답에서 늘 `{ userId, displayName }`으로 나간다. 로그인 아이디는 싣지 않는다 (FR-027). 시각은 `ApiTimes.of`의 OffsetDateTime이다.

## 응답 모양

- **PartySummary**(모집 페이지 카드)
  - id, gameCode, modeCode, title, joinPolicy
  - memberCount, capacity
  - openPositions: `[{positionCode, remaining}]`
  - tierMinCode, tierMaxCode
  - host: `{userId, displayName}`
  - createdAt
- **PartyDetail**
  - PartySummary의 항목 전부
  - description, status, closedByHost, positionEnabled
  - positions: `[{positionCode, slots, filled}]`
  - members: `[{userId, displayName, role, positionCode, tierCode, joinedAt}]`
  - me: `{relation: NONE|MEMBER|HOST|PENDING|BANNED, joinRequestId?}`
  - invite: `{code, url}`. 멤버에게만 싣는다 (US4-AC1)
- **InvitePreview**
  - id, gameCode, modeCode, title, host, memberCount, capacity
  - positions
  - me.relation, joinable, reasonCode

## 모집 페이지와 파티

| 호출 | 누가 | 성공 | 오류 | AC |
|---|---|---|---|---|
| `GET /parties?gameCode&modeCode&page=0&size=20` | 누구나 | 200 `PageResponse<PartySummary>`. `RECRUITING`이고 `OPEN`·`APPROVAL`인 것만 최신순 | V001 (모드만 있고 게임이 없음) | US2-AC1, US2-AC2, FR-004 |
| `POST /parties` | 파티에 속하지 않은 회원 | 201 PartyDetail. 만든 회원이 HOST | V001 (칸 검증, 목록에 없는 코드, 포지션 없는 게임에 포지션, 자리 합이 2~10을 벗어남), B103 | US1-AC1~AC3, US10-AC1~AC4, US3-AC6 |
| `GET /parties/{id}` | 누구나. PRIVATE는 멤버만 | 200 PartyDetail | R001 (없음, 해산, 멤버가 아닌 회원의 PRIVATE) | US2-AC3~AC5, FR-014 |
| `PUT /parties/{id}` | 방장 | 200 PartyDetail. 게임, 모드, 제목, 소개, 인원, 포지션, 티어 | A101, V001, B109 (인원이 지금보다 작음), B110 (차지된 포지션 빼기·줄이기), B111 (멤버가 있는 포지션 파티의 게임 바꾸기) | US7-AC1, US10-AC10 |
| `PUT /parties/{id}/join-policy` `{joinPolicy}` | 방장 | 200. APPROVAL에서 바뀌면 대기 신청을 CANCELLED로 | A101 | US7-AC2~AC4 |
| `POST /parties/{id}/close` | 방장 | 200. CLOSED, closedByHost=true | A101 | US7-AC6 |
| `POST /parties/{id}/reopen` | 방장 | 200. RECRUITING | A101, B101 (정원 참) | US7-AC6 |
| `POST /parties/{id}/invite-code` | 방장 | 200 `{code, url}`. 이전 코드는 못 쓴다 | A101 | US7-AC7 |
| `DELETE /parties/{id}` | 방장 | 204. 해산 | A101 | US7-AC8 |

## 참가와 나가기

| 호출 | 누가 | 성공 | 오류 | AC |
|---|---|---|---|---|
| `POST /parties/{id}/members` `{positionCode?, tierCode?}` | OPEN 파티의 비멤버 | 200 PartyDetail | B101 (정원 참), B102 (마감), B103 (이미 이 파티나 다른 파티에 속함), B104 (참가 제한), B105 (포지션 참), B106 (포지션 안 고름), B112 (OPEN이 아님) | US3-AC1~AC6, US10-AC5~AC8 |
| `GET /parties/invites/{code}` | 누구나 | 200 InvitePreview | B107 (없는 코드, 바뀐 코드, 해산) | 화면 "초대 입장 화면" |
| `POST /parties/invites/{code}/members` `{positionCode?, tierCode?}` | 비멤버 | 200 PartyDetail. 방식과 상관없이 바로 멤버. 그 회원의 대기 신청은 모두 CANCELLED | B107, B101, B102, B103, B104, B105, B106 | US4-AC2~AC7 |
| `DELETE /parties/{id}/members/me` | 멤버 | 204. 방장이면 다음 방장으로 넘김. 마지막이면 해산 | A102 | US6-AC1~AC5 |
| `DELETE /parties/{id}/members/{userId}` | 방장 | 204. tb_party_ban에 KICKED | A101, R001 (멤버 아님), B113 (자기 자신) | US7-AC5 |

멤버가 되는 모든 길(참가, 승인, 초대 입장)에서 처리 순서는 같다.

1. 파티 락을 잡는다.
2. 해산, 참가 제한, 이미 속함, 정원, 마감, 포지션 순서로 확인한다.
   - 정원을 마감보다 먼저 본다. 정원이 차면 자동으로 CLOSED가 되므로, 순서가 반대면 꽉 찬 파티에서 B101 대신 B102가 나간다 (US3-AC3)
   - 방장의 승인은 마감을 확인하지 않는다. 마감 중에도 자리가 있으면 승인할 수 있다 (FR-008)
   - 승인 신청을 받을 때(`POST /parties/{id}/join-requests`)도 정원, 마감 순서로 확인한다
3. 멤버 행을 넣는다.
4. 나머지 대기 신청을 CANCELLED로 바꾼다.
5. 정원이 차면 CLOSED로 바꾼다.
6. 안내 메시지(JOINED)를 남긴다.
7. 커밋한 뒤 실시간 알림을 보낸다.

## 승인 신청

| 호출 | 누가 | 성공 | 오류 | AC |
|---|---|---|---|---|
| `POST /parties/{id}/join-requests` `{positionCode?, tierCode?}` | APPROVAL 파티의 비멤버 | 201 `{joinRequestId}` PENDING | B101, B102, B103 (다른 파티에 속함), B104, B105, B106, B108 (이미 대기 중), B112 (APPROVAL이 아님) | US5-AC1, US5-AC7 |
| `GET /parties/{id}/join-requests` | 방장 | 200 `[{joinRequestId, user, positionCode, tierCode, appliedAt}]` 대기 중만 신청순 | A101 | US5-AC2, US5-AC8 |
| `POST /parties/{id}/join-requests/{joinRequestId}/approve` | 방장 | 200 PartyDetail | A101, B101, B105, B103 (신청자가 그사이 다른 파티에 들어감), R001 (대기 중이 아님) | US5-AC3, US5-AC5 |
| `POST /parties/{id}/join-requests/{joinRequestId}/reject` | 방장 | 204. tb_party_ban에 REJECTED | A101, R001 | US5-AC4 |
| `DELETE /parties/{id}/join-requests/me` | 신청자 | 204. CANCELLED | R001 (대기 신청 없음) | US5-AC6 |

## 내 파티

| 호출 | 누가 | 성공 | AC |
|---|---|---|---|
| `GET /parties/me` | 회원 | 200 `{party: PartySummary + myRole \| null, joinRequests: [{joinRequestId, party: {id, title, gameCode}, positionCode, appliedAt}]}` | US8-AC1, US8-AC2, US3-AC7 |

## 채팅

| 호출 | 누가 | 성공 | 오류 | AC |
|---|---|---|---|---|
| `GET /parties/{id}/messages?before={id}&size=50` | 멤버 | 200 `{items: [Message], hasMore}`. 오래된 것부터. `before`가 없으면 마지막 50개 | A102, R001 (해산) | US9-AC2, US9-AC4, US9-AC6 |
| `GET /parties/{id}/messages?after={id}` | 멤버 | 200 `{items, hasMore}`. 그 id 뒤의 것 (다시 연결된 뒤) | A102 | FR-018 |
| `POST /parties/{id}/messages` `{content}` | 멤버 | 201 Message. 커밋 뒤 `/topic/parties/{id}`로 보낸다 | A102, V001 (빈 값, 500자 넘음), R001 | US9-AC1, US9-AC5 |

Message의 항목은 id, type(`CHAT`·`SYSTEM`), sender(`{userId, displayName}`), content, systemEvent, subject(`{userId, displayName}`), sentAt이다.

## 오류 코드

`ErrorCode`에 더한다. 화면 문구는 `messages/ko.json` `errors`에 더한다 (research.md R9).

| 코드 | 상태 | 뜻 |
|---|---|---|
| A101 | 403 | 방장만 할 수 있다 |
| A102 | 403 | 파티 멤버만 할 수 있다 |
| B101 | 409 | 정원이 찼다 |
| B102 | 409 | 모집이 마감됐다 |
| B103 | 409 | 이미 파티에 속해 있다 (이 파티든 다른 파티든) |
| B104 | 409 | 이 파티에는 들어올 수 없다 (내보내짐, 거절됨) |
| B105 | 409 | 그 포지션은 자리가 찼다. `details.openPositions`에 남은 포지션 |
| B106 | 400 | 포지션을 골라야 한다 |
| B107 | 404 | 유효하지 않은 초대다 |
| B108 | 409 | 이미 대기 중인 신청이 있다 |
| B109 | 409 | 모집 인원을 지금 인원보다 작게 할 수 없다 |
| B110 | 409 | 멤버가 있는 포지션은 빼거나 줄일 수 없다 |
| B111 | 409 | 멤버가 있는 포지션 파티는 게임을 바꿀 수 없다 |
| B112 | 409 | 이 참가 방식으로는 들어올 수 없다 (OPEN이 아닌데 참가, APPROVAL이 아닌데 신청) |
| B113 | 409 | 자기 자신을 내보낼 수 없다 |

US1-AC3·US9-AC5처럼 칸이 잘못된 경우는 기존 V001에 `errors[]`(칸별 이유)를 붙여 알린다.
