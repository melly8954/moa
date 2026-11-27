# Contract: 프로필·친구·팔로우 REST API (014)

모두 `/api/v1` 아래에 있고 로그인해야 부를 수 있다(없으면 401 A001, `009-mypage` FR-013 "로그인한 회원만"). 관리자·운영자 계정이 부르면 403 A301이다(research.md R9). 요청·응답 전체는 구현 뒤 local 프로필의 `/v3/api-docs`가 기준이다. 여기에는 경로, 누가 부르는지, 성공 응답, 오류 코드, AC와의 대응만 적는다.

회원은 주소와 응답에서 `publicId`(UUID)로 가리킨다. 순번 `userId`는 이 API의 주소에 나오지 않는다 (FR-003, research.md R1). 대상 회원이 없거나, 관리자 계정이거나, 탈퇴 유예 중·파기됐으면 404 R001이다 (research.md R15).

## 응답 모양

- **MemberSummary**: `{ publicId, displayName, tag, avatar, withdrawn }`. `tag`는 `0421` 같은 네 자리(없으면 `null`). 다른 기능의 응답에서 탈퇴한 회원은 `withdrawn: true`, `publicId: null`, `displayName`은 "탈퇴한 회원"이다(018 `MemberDisplay`). 이 기능의 목록에는 탈퇴 유예 중인 회원이 나오지 않는다
- **Relation** (보는 사람 기준): `{ friend, friendRequestId, follow, followRequestId }`
  - `friend`: `"NONE" | "REQUEST_SENT" | "REQUEST_RECEIVED" | "FRIENDS" | "SELF"`. `friendRequestId`는 `REQUEST_RECEIVED`일 때 수락·거절에 쓴다
  - `follow`: `"NONE" | "REQUESTED" | "FOLLOWING"`. 내 프로필이면 `NONE`이다. `followRequestId`는 `REQUESTED`일 때 취소 확인용으로 준다
- **MemberProfile**: `MemberSummary` + 009가 공개로 정한 항목(`ProfileVisibility.view` 결과) + `counts: { friends, followers, following }` + `relation` + `privateAccount: boolean` + `canViewActivity: boolean`
  - `canViewActivity`: 본인, 상대가 공개 계정, 내가 상대를 팔로우, 친구 중 하나면 `true` (FR-018, research.md R6)
- **피드 목록**: `{ items: [...], nextCursor }`. `?before={cursor}&size=20`(1~50). 더 없으면 `nextCursor: null` (research.md R5)

## 프로필

| 경로 | 누가 | 성공 | 오류 | AC |
|---|---|---|---|---|
| `GET /users/{publicId}/profile` | 회원 | 200 MemberProfile. 내 publicId면 `relation.friend = SELF`, `canViewActivity = true` | R001 | US1-AC1~AC5, US4-AC6 |

## 친구

| 경로 | 누가 | 성공 | 오류 | AC |
|---|---|---|---|---|
| `POST /users/{publicId}/friend-requests` | 회원 | 201 `{ result: "REQUESTED", requestId }`. 상대가 이미 나에게 요청했으면 200 `{ result: "FRIENDS" }`(수락과 같다) | R001, B301, B302, B303, B304, B305, B306 | US2-AC1, AC6~AC9 |
| `DELETE /users/{publicId}/friend-requests` | 보낸 회원 | 204. 내가 보낸 대기 요청을 취소 | R001 (대기 요청 없음) | US2-AC5 |
| `POST /friend-requests/{requestId}/accept` | 받은 회원 | 200 `{ result: "FRIENDS" }`. 서로 팔로우가 생기고 둘 사이 팔로우 요청이 지워진다 | R001 (없음·그사이 취소됨·내 요청 아님), B305, B306 | US2-AC3, US4-AC7 |
| `POST /friend-requests/{requestId}/reject` | 받은 회원 | 204. 거절 기록을 남기고 요청을 지운다 | R001 | US2-AC4 |
| `DELETE /users/{publicId}/friends` | 친구 | 204. 서로의 친구·팔로우를 지운다 | R001 (친구 아님) | US5-AC1 |
| `GET /friends?q&before&size` | 회원 | 200 피드 `{items: [MemberSummary + friendsSince]}`. 맺은 최근 순, `q`는 닉네임 포함 | V001 (size 범위) | US5-AC2, AC3, AC5 |
| `GET /friend-requests/received?before&size` | 회원 | 200 피드 `{items: [{requestId, member: MemberSummary, requestedAt}]}` | V001 | US2-AC2 |
| `GET /friend-requests/sent?before&size` | 회원 | 200 피드 (모양은 위와 같다) | V001 | US5-AC2 |
| `GET /friend-requests/received/count` | 회원 | 200 `{ count }`. 친구 화면 묶음 머리의 수 | | US5-AC2 |

## 팔로우

| 경로 | 누가 | 성공 | 오류 | AC |
|---|---|---|---|---|
| `POST /users/{publicId}/follow` | 회원 | 공개 계정이거나 친구면 201 `{ result: "FOLLOWING" }`. 비공개 계정이면 202 `{ result: "REQUESTED", requestId }` | R001, B303, B307, B308 | US3-AC1, AC4, US4-AC2 |
| `DELETE /users/{publicId}/follow` | 팔로우하는 회원 | 204. 친구여도 친구는 그대로 | R001 (팔로우 안 함) | US3-AC2, AC3 |
| `GET /users/{publicId}/followers?before&size` | 회원 | 200 피드 `{items: [MemberSummary + followedAt]}` 최근 순 | R001, B309, V001 | US3-AC5, US4-AC6 |
| `GET /users/{publicId}/following?before&size` | 회원 | 200 피드 (모양은 위와 같다) | R001, B309, V001 | US3-AC1, US4-AC6 |

## 팔로우 요청 (비공개 계정)

| 경로 | 누가 | 성공 | 오류 | AC |
|---|---|---|---|---|
| `DELETE /users/{publicId}/follow-requests` | 보낸 회원 | 204. 내가 보낸 대기 팔로우 요청을 취소 | R001 (대기 요청 없음) | US4-AC5 |
| `GET /follow-requests/received?before&size` | 회원 | 200 피드 `{items: [{requestId, member: MemberSummary, requestedAt}]}` | V001 | US4-AC3, US5-AC2 |
| `GET /follow-requests/received/count` | 회원 | 200 `{ count }` | | US5-AC2 |
| `POST /follow-requests/{requestId}/accept` | 받은 회원 | 200 `{ result: "FOLLOWING" }`. 요청한 회원이 팔로워가 된다 | R001 (없음·그사이 취소됨·공개 전환으로 이미 팔로우됨·내 요청 아님) | US4-AC3 |
| `POST /follow-requests/{requestId}/reject` | 받은 회원 | 204. 요청을 지운다. 기록을 남기지 않는다 | R001 | US4-AC4 |

## 계정 공개 여부

| 경로 | 누가 | 성공 | 오류 | AC |
|---|---|---|---|---|
| `GET /users/me/account-visibility` | 회원 | 200 `{ visibility: "PUBLIC" \| "PRIVATE" }`. 정한 적 없으면 `PUBLIC` | | US4-AC1 |
| `PUT /users/me/account-visibility` `{ visibility }` | 회원 | 200. `PUBLIC`으로 바꾸면 받은 대기 팔로우 요청이 모두 팔로우가 된다 | V001 | US4-AC8 |

경로는 21개다.

## 오류 코드

`ErrorCode`에 더한다. 화면 문구는 `messages/ko.json` `errors`에 더한다 (research.md R13).

| 코드 | 상태 | 뜻 |
|---|---|---|
| A301 | 403 | 관리자·운영자 계정은 친구·팔로우를 쓸 수 없다 (FR-013) |
| B301 | 409 | 이미 친구다 |
| B302 | 409 | 이미 친구 요청을 보냈다 |
| B303 | 400 | 자기 자신에게는 할 수 없다 |
| B304 | 409 | 지금은 이 회원에게 친구 요청을 보낼 수 없다 (거절 뒤 30일. 남은 기간은 알리지 않는다, US2-AC9) |
| B305 | 409 | 내 친구가 상한(5,000명)이다 |
| B306 | 409 | 상대의 친구가 상한이다 |
| B307 | 409 | 이미 팔로우하고 있다 |
| B308 | 409 | 이미 팔로우 요청을 보냈다 |
| B309 | 403 | 비공개 계정이라 팔로워만 볼 수 있다 (FR-018) |

## 다른 기능 응답에 더하는 것 (FR-002)

- `008-party`의 회원 응답 `{userId, displayName}`(멤버 목록, 신청 목록, 채팅 보낸 사람)에 `publicId`를 더한다. 이름을 누르면 `/users/{publicId}`로 간다. 008 화면 작업에 넣는다
- 프로필 활동 칸에 탭을 붙이는 기능(015 숏폼)은 자기 API에서 `ActivityVisibility.canView(viewer, owner)`가 거짓이면 403 B309를 준다 (research.md R16)

## 개인 알림 (FR-016, research.md R10)

`NotificationApplication.notify(userId, type, title, link)`로 보낸다. 개인 경로 `/user/queue/notifications`(007)로 간다.

| 종류 | 받는 사람 | 제목 | 연결 |
|---|---|---|---|
| `FRIEND_REQUESTED` | 요청 받은 회원 | `{이름}님이 친구 요청을 보냈습니다` | `/friends?tab=received` |
| `FRIEND_ACCEPTED` | 요청한 회원 | `{이름}님과 친구가 되었습니다` | `/users/{수락한 회원 publicId}` |
| `FOLLOW_STARTED` | 팔로우 받은 회원 (공개 계정) | `{이름}님이 회원님을 팔로우했습니다` | `/users/{팔로우한 회원 publicId}` |
| `FOLLOW_REQUESTED` | 팔로우 요청 받은 회원 (비공개 계정) | `{이름}님이 팔로우를 요청했습니다` | `/friends?tab=follow-requests` |
| `FOLLOW_REQUEST_ACCEPTED` | 팔로우 요청한 회원 | `{이름}님이 팔로우 요청을 수락했습니다` | `/users/{수락한 회원 publicId}` |
