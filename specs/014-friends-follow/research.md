# Research: 회원 프로필 보기, 친구, 팔로우 (014)

스펙: [spec.md](./spec.md). 각 항목은 결정 / 이유 / 검토한 대안 순서다. 2026-11-22 dev(`f127bba`) 기준으로 썼고, 2026-11-27 dev(`48ea437`, 009 마이페이지·018 회원 탈퇴 구현 뒤) 기준으로 R6·R8·R9·R10·R11을 고치고 R15·R16을 더했다.

## R1. 프로필 주소의 공개 ID (FR-003)

- **Decision**
  - 1:1 테이블 `tb_user_public_id`(PK `user_id`, `public_id CHAR(36)` UUID, 유니크)를 새로 둔다. 프로필 주소는 `/users/{publicId}`다
  - 회원(`USER`)이 생기는 두 길에서 행을 함께 만든다: 소셜 가입(`SocialAccountService.signUp`), 관리자의 계정 만들기(`UserAdminService.create`, 역할이 `USER`일 때). 지금 있는 회원은 마이그레이션이 채운다(삭제·파기되지 않은 `USER`. 탈퇴 유예 중인 회원 포함, R14)
  - 관리자 계정(`ADMIN`)에는 만들지 않는다. 회원이 관리자로 바뀌어도(`changeRole`) 행은 두되, 프로필은 `ADMIN`이면 404다 (US1-AC4)
  - 회원을 응답에 실을 때(`{userId, displayName}`) 이 기능의 화면과 파티 화면에는 `publicId`를 더한다. 이름을 누르면 프로필로 가야 하기 때문이다 (FR-002)
- **Rationale**: `docs/db.md` "키"가 주소에 드러나는 ID는 순번 대신 `public_id`(UUID)로 하라고 정한다. `tb_user`는 키트 골격이라 칸을 더하지 않는다(`009-mypage` research R3와 같은 이유). `tb_user_profile`(010·009)에 두면 행이 없는 회원(관리자가 만든 회원, `009-mypage` research R14)의 링크를 만들 수 없다
- **Alternatives considered**
  - `tb_user_profile`에 칸 추가: 위 이유로 행이 없는 회원이 생긴다. 그 회원의 행을 미리 만들려면 닉네임 태그도 정해야 해서 009·010의 규칙을 건드린다
  - `닉네임#태그`를 주소로 쓰기: 닉네임을 바꾸면 링크가 깨진다
  - 회원 순번을 그대로 쓰기: `docs/db.md`에 어긋나고 가입 순서가 드러난다

## R2. 친구 관계 저장 (FR-004, FR-007)

- **Decision**: 친구 한 쌍을 두 행으로 저장한다. `tb_friend(user_id, friend_user_id, created_at)`이고, A와 B가 친구면 (A, B)와 (B, A) 두 행이다. 맺기와 끊기는 두 행을 한 트랜잭션에서 넣고 지운다
- **Rationale**: "X의 친구 목록", "X의 친구 수", "X와 Y가 친구인가"가 모두 `user_id = X` 하나의 조건이라 조회와 인덱스가 단순하다. 피드 목록의 커서(R5)도 한 열로 된다. 친구 상한(5,000)이 있어 행이 두 배여도 작다
- **Alternatives considered**: 한 쌍을 한 행(`user_low_id`, `user_high_id`)으로: 목록마다 두 열을 OR로 찾고 상대 ID를 골라 내야 한다

## R3. 친구 요청과 동시 요청 (FR-004, US2-AC6, Edge Cases)

- **Decision**
  - `tb_friend_request(requester_id, receiver_id, pair_low_id, pair_high_id, created_at)`. 두 회원 사이에 대기 요청은 하나뿐이라 `uk_friend_request_pair_low_id_pair_high_id`로 막는다(방향과 상관없이 한 쌍에 하나)
  - 수락·거절·취소하면 요청 행을 지운다. 대기 중인 것만 남는다 (spec Key Entities)
  - 요청 보내기: 상대가 이미 나에게 보낸 요청이 있으면 새로 만들지 않고 그 요청을 수락한다. 두 사람이 동시에 보내면 한쪽의 INSERT가 쌍 유니크에 걸리고, Service가 그 충돌을 받아 상대 요청의 수락으로 바꾼다
- **Rationale**: 대기 요청을 쌍마다 하나로 DB가 막으면, 동시 요청도 락 없이 하나의 결과(친구 한 쌍)로 모인다. `008-party` research R6처럼 유니크 충돌을 업무 결과로 바꾸는 선례가 있다
- **Alternatives considered**: 두 회원 행을 비관적 락으로 잡기: `tb_user`(키트)를 락 대상으로 쓰게 되고 순서를 맞춰야 교착이 안 난다

## R4. 거절 뒤 30일 (FR-006, US2-AC9)

- **Decision**: `tb_friend_rejection(requester_id, receiver_id, rejected_at)`에 거절을 남긴다. 요청을 보낼 때 "그 상대가 나를 거절한 지 30일이 안 됐는가"를 본다. 거절한 쪽(receiver)이 먼저 보내는 것은 막지 않는다. 같은 쌍을 다시 거절하면 `rejected_at`만 새로 한다(`uk_friend_rejection_requester_id_receiver_id`)
- 30일은 설정값 `app.friend.rejection-cooldown`(기본 `30d`)이다. 정리기 `SocialRelationCleaner`가 기간이 지난 행을 지운다(R11, R15)
- 팔로우 요청 거절(R6)에는 이 기록을 쓰지 않는다. spec FR-017이 곧바로 다시 요청하도록 정했다
- **Rationale**: 요청 행은 거절하면 지워지므로(R3) 거절 사실은 따로 남겨야 한다. 기간이 지나면 쓸 데가 없으니 지운다
- **Alternatives considered**: 요청 행을 REJECTED 상태로 남기기: 쌍 유니크(R3)가 다시 요청을 막고, 대기 요청만 남긴다는 정의와 어긋난다

## R5. 피드 목록의 커서 (FR-011, FR-012)

- **Decision**: 친구·팔로워·팔로잉·받은 친구 요청·보낸 친구 요청·받은 팔로우 요청 목록은 행 `id` 내림차순 커서다. `GET ...?before={id}&size=20`, 응답 `{items, nextCursor}`(더 없으면 `null`). 21개를 읽어 21번째가 있으면 다음이 있다
- 행 `id`는 늘어나는 값이라 "관계를 맺은 최근 순"과 같다. 친구 목록의 닉네임 거르기(`q`)는 `tb_user.name`의 포함 검색이고, 커서와 함께 쓴다
- 모든 목록은 상대 회원을 `User` 기본 조회(삭제·탈퇴 제외, `User.ACTIVE`)와 조인해 거른다 (R15)
- **Rationale**: 원칙의 피드 목록(커서 무한 스크롤)이다. `008-party` research R5의 메시지 커서와 같은 방식이라 프론트 무한 스크롤을 함께 쓸 수 있다
- **Alternatives considered**: `created_at` 커서: 같은 시각이 겹치면 빠지거나 겹친다. 쪽 번호: 원칙상 피드 목록이 아니다

## R6. 팔로우, 팔로우 요청, 계정 공개 여부 (FR-009, FR-010, FR-017, FR-018, US3, US4)

2026-11-27에 "팔로우 받기 범위(누구나/친구만)"를 이 결정으로 바꿨다.

- **Decision**
  - `tb_follow(follower_id, followee_id, created_at)`, `uk_follow_follower_id_followee_id`. 팔로잉 목록은 `idx_follow_follower_id_id`로, 팔로워 목록은 `idx_follow_followee_id_id`로 찾는다
  - 계정 공개 여부는 1:1 `tb_user_account_visibility(user_id PK, visibility)`에 둔다. 값은 `PUBLIC`, `PRIVATE`이고 행이 없으면 `PUBLIC`이다. 바꿀 때만 행을 만든다
  - 팔로우 요청은 `tb_follow_request(requester_id, target_id, created_at)`, `uk_follow_request_requester_id_target_id`. 대기 중인 것만 남고 수락·거절·취소하면 지운다(친구 요청 R3과 같다). 한 방향에 하나라 쌍 정규화 열은 필요 없다. 반대 방향 요청은 서로 다른 일이다(A가 B를, B가 A를 팔로우하고 싶다)
  - `POST /users/{publicId}/follow` 한 경로가 상대에 따라 갈린다
    1. 자기 자신 `B303`, 이미 팔로우 `B307`
    2. 상대가 `PUBLIC`이거나 친구면: `tb_follow`에 넣는다 → 201 `FOLLOWING`
    3. 상대가 `PRIVATE`이고 친구가 아니면: 이미 요청했으면 `B308`, 아니면 `tb_follow_request`에 넣는다 → 202 `REQUESTED`
  - 수락: 요청을 지우고 `tb_follow`를 넣는다. 거절: 요청만 지운다. 거절 기록은 남기지 않는다(FR-017)
  - 공개로 바꾸기: 한 트랜잭션에서 그 회원이 받은 대기 요청을 모두 `tb_follow`로 옮기고 지운다(`INSERT ... SELECT` 후 `DELETE`, 이미 팔로우면 건너뛴다). 알리지 않는다
  - 비공개로 바꾸기: 행만 바꾼다. 기존 팔로우는 그대로다
  - 친구 맺기(R2)는 공개 여부와 상관없이 서로의 `tb_follow`를 넣고(이미 있으면 그대로) 두 사람 사이의 대기 팔로우 요청을 양쪽 모두 지운다. 친구 끊기는 서로의 팔로우를 함께 지운다 (FR-007)
  - 활동을 볼 수 있는가(`canViewActivity`): 본인이거나, 상대가 `PUBLIC`이거나, 내가 상대를 팔로우하거나, 친구다 (FR-018). 팔로워·팔로잉 목록 API는 이것이 거짓이면 403 `B309`다. 활동 칸에 탭을 붙이는 기능(015)도 같은 판단을 쓰도록 `ActivityVisibility.canView(viewer, owner)` 하나를 둔다(Application 계층에서 R16)
- **Rationale**
  - 공개 여부는 회원 한 명의 설정이라 1:1이다. `tb_user_profile`(009)에 두면 행이 없을 수 있어(R1) 같은 문제가 생긴다
  - 팔로우 요청을 `tb_follow`의 상태 열(대기/승인)로 두지 않은 것은, 팔로워 목록·수·피드가 모두 "승인된 팔로우"만 봐야 해서다. 상태 열이면 모든 조회에 조건이 붙고 하나라도 빠지면 비공개 계정의 승인 전 요청자가 팔로워로 보인다
  - 한 경로(`POST .../follow`)로 둔 것은 화면이 상대의 공개 여부를 미리 알 필요 없이 결과(`FOLLOWING`/`REQUESTED`)로 단추를 바꾸게 하려는 것이다. 사이에 상대가 공개 여부를 바꿔도 서버 판단이 맞다
- **Alternatives considered**
  - `tb_follow.status`(PENDING/ACCEPTED): 위 이유
  - 팔로우 요청 거절에도 30일: spec FR-017이 정하지 않았다. 반복 요청이 문제가 되면 친구 거절 기록(R4)과 같은 방식으로 더할 수 있다
  - 공개로 바꿀 때 대기 요청을 지우기: spec Clarifications(2026-11-27)가 팔로우로 바꾸기로 정했다(인스타그램과 같다)

## R7. 수(친구·팔로워·팔로잉)

- **Decision**: 프로필을 열 때 `COUNT`로 센다. 세는 조건이 모두 인덱스의 앞 열이다. 상대 회원을 `User` 기본 조회와 조인해 탈퇴 유예 중인 회원을 뺀다(R15). 따로 수를 저장하지 않는다
- **Rationale**: 친구는 5,000 상한이고 지금 회원 규모에서 COUNT가 충분히 빠르다. 저장된 수는 맺기·끊기·탈퇴·복구마다 맞춰야 해서 어긋날 곳이 늘어난다
- **Alternatives considered**: 수를 칸에 두고 늘리고 줄이기: 팔로워가 매우 많은 회원이 생기면 그때 바꾼다

## R8. 다른 회원 프로필의 항목과 009 (FR-001, FR-015)

009가 dev에 구현됐다(#149). 코드 기준으로 정한다.

- **Decision**
  - 프로필 항목과 공개 규칙은 009의 `ProfileVisibility.view(ProfileDto profile, Long viewerUserId)`(`api/service/user/ProfileVisibility.java`, 정적 메서드)를 쓴다. 014는 수·관계·단추·활동 칸과 `publicId`만 더한다
  - 지금 `view`는 디스코드 `FRIENDS`를 아무에게도 보이지 않는다(그 자리에 "친구 기능이 생기기 전까지" 주석). 014가 `view(profile, viewerUserId, boolean viewerIsFriend)`를 더해 `FRIENDS`면 친구에게만 보이게 하고, 기존 두 인자 `view`는 `viewerIsFriend = false`로 넘긴다. 기존 기능을 그 자리에서 고치는 것이다(원칙 IV). 009 담당에게 PR에서 알린다
  - 친구인지는 014의 `FriendService.areFriends`로 Application(`MemberProfileApplication`)이 구해 넘긴다(계층 규칙)
  - 프로필 읽기는 009의 `ProfileService`(또는 `ProfileApplication`이 쓰는 읽기)로 `ProfileDto`를 얻는다. 행이 없는 회원(관리자가 만든 회원)은 닉네임과 기본 아바타만 보인다(009 research R14)
  - 009가 공개 범위 이름을 "친구만"으로 바꾸면(spec FR-015 제안) 값 이름은 `FRIENDS` 그대로 두고 문구만 바꾸면 된다. 009 화면은 이미 "친구만"으로 보인다
- **Rationale**: 공개 규칙의 주인은 009다(`009-mypage` research R9). 같은 규칙을 두 곳에 두면 어긋난다
- **Alternatives considered**: 014가 `VisibleProfile`을 받아 디스코드 칸만 다시 채우기: 규칙이 두 곳으로 나뉜다

## R9. 관리자·운영자 막기 (FR-013)

- **Decision**: 009의 `MemberActivityPolicy.isManagerAccount(userId)`(`api/service/user/MemberActivityPolicy.java`, `@Component`)를 쓴다. 회원이 없으면(삭제·탈퇴 유예 포함) `R001`을 던진다
  - 요청·수락·거절·취소·끊기·팔로우·팔로우 취소·팔로우 요청 처리·공개 여부 바꾸기·친구 화면 API를 부른 사람이 관리자 계정이면 403 `A301`. `requireMypageAllowed`처럼 `requireSocialAllowed(userId)`를 이 클래스에 더하지 않고 014 Application에서 판단한다(009 코드에 014 오류 코드를 넣지 않는다)
  - 대상이 관리자 계정이면 404 `R001`(프로필이 없다, US1-AC4·US2-AC8)
- **Rationale**: 009·008과 같은 기준과 같은 방식(서버에서 막고 화면은 사용자 셸이라 관리자에게 열리지 않는다)이다

## R10. 개인 알림 (FR-016)

- **Decision**: 요청·수락·팔로우를 처리한 같은 트랜잭션에서 `NotificationApplication.notify`를 부른다 (`008-party` research R13과 같은 방식)
  - `FRIEND_REQUESTED`: 요청 받은 회원에게. 연결 `/friends?tab=received`
  - `FRIEND_ACCEPTED`: 요청한 회원에게. 연결은 수락한 회원의 프로필 `/users/{publicId}`
  - `FOLLOW_STARTED`: 친구가 아닌 회원이 공개 계정을 팔로우했을 때. 연결은 팔로우한 회원의 프로필. 친구 맺기·팔로우 요청 수락·공개 전환으로 생긴 팔로우에는 보내지 않는다
  - `FOLLOW_REQUESTED`: 비공개 계정에 팔로우 요청이 왔을 때 그 계정 주인에게. 연결 `/friends?tab=follow-requests`
  - `FOLLOW_REQUEST_ACCEPTED`: 팔로우 요청이 수락됐을 때 요청한 회원에게. 연결은 수락한 회원의 프로필
  - 동시 친구 요청이 수락으로 바뀐 경우(R3)는 `FRIEND_ACCEPTED`만 먼저 보낸 쪽에 보낸다
  - 탈퇴 유예 중인 회원에게 가는 알림은 007이 저장하지 않는다(`018` FR-022). 014는 거르지 않는다
- 종류 5개를 `NotificationType`에 더하고, `tb_notification.type`의 COMMENT를 같은 마이그레이션에서 고친다(`docs/db.md` "COMMENT"). 008이 먼저 들어가면 008의 `PARTY_` 6종도 COMMENT에 그대로 옮겨 적는다(`MODIFY COLUMN`은 정의 전체를 다시 쓴다)

## R11. 개인정보 보관 (`docs/privacy.md`)

- **Decision**: 보관 기간 표에 더할 줄
  - 친구 관계, 대기 친구 요청, 팔로우, 대기 팔로우 요청: 관계를 끊거나 요청이 처리되면 바로 지운다. 탈퇴 유예 중에는 남기고, 파기 때 그 회원이 한쪽인 행을 모두 지운다(R15)
  - 친구 요청 거절 기록: 30일 뒤 정리기가 지운다(R4). 파기 때도 지운다
  - 공개 ID, 계정 공개 여부: 파기 때 지운다
  - "탈퇴 회원 파기"의 "다른 회원과 엮인 행: 남긴다"에 예외 한 줄을 더한다: 관계(친구·팔로우·요청)는 그 회원과의 관계 자체라 상대에게 쓸모가 없으므로 지운다
- **Rationale**: `docs/privacy.md` "개인정보를 담는 테이블을 새로 만드는 스펙은 보관 기간 표에 한 줄을 더한다"

## R12. 화면 경로와 메뉴 (FR-014)

- **Decision**
  - 프로필 `/users/{publicId}`, 팔로워·팔로잉 `/users/{publicId}/followers`·`/following`, 친구 화면 `/friends`(묶음은 `?tab=friends|received|sent|follow-requests`). 모두 로그인이 필요해 `(user)/(signed-in)`에 둔다 (`008-party` research R11과 같은 구조)
  - 친구 화면은 연결 화면 `FRIENDS`(`ScreenDefinition.user("FRIENDS", "친구", "/friends", true)`, `api/screen/FriendScreens.java`)와 사용자 메뉴 "친구"(회원만)로 연다. 메뉴 행은 마이그레이션이 `screen_key`로 찾아 없을 때만 넣는다 (`008-party` research R11)
  - 계정 공개 여부는 계정 설정 화면 `/settings`(`(account)/settings`)에 칸을 더한다
- **Rationale**: 001 FR-007·FR-008의 서버 메뉴 방식이다. 프로필은 메뉴로 열지 않아 연결 화면이 아니다

## R13. 오류 코드

- **Decision**: 친구·팔로우 권한 오류는 `A3xx`, 업무 오류는 `B3xx`다 (파티 `A1xx`·`B1xx`, 마이페이지 `A2xx`·`B2xx` 다음). 목록은 [contracts/friends-api.md](./contracts/friends-api.md). 11-22 판의 `B308`(친구만 팔로우 가능)은 계정 공개 여부로 바뀌며 없앴고, 그 번호를 "이미 팔로우 요청함"에 쓴다(아직 구현 전이라 번호가 쓰인 적이 없다)
- **Rationale**: `ErrorCode`의 접두 글자가 종류를 나타내고 기능마다 백 단위를 나눠 쓴다(`008-party` research R9)

## R14. 마이그레이션

- **Decision**: 파일 하나 `V<다음 번호>__friends_follow.sql`(2026-11-27 dev 마지막은 `V13__member_withdrawal.sql`). 새 테이블 7개, 기존 회원의 공개 ID 채우기, 연결 화면 메뉴 시드, `tb_notification.type`과 `tb_purge_log.target`의 COMMENT 고치기를 함께 둔다. 번호는 PR 직전에 dev를 병합하고 정한다 (`docs/db.md` "마이그레이션")
- 공개 ID 채우기는 MariaDB `UUID()`로 한다. 대상은 `role = 'USER' AND deleted_at IS NULL AND purged_at IS NULL`이다. 탈퇴 유예 중인 회원(`withdrawn_at`이 있음)도 넣어, 복구했을 때 공개 ID가 없는 회원이 생기지 않게 한다(R15)

## R15. 탈퇴·복구·파기 (FR-020, `018-member-withdrawal`)

018이 dev에 구현됐다(#152). `User`의 기본 조회(`@SQLRestriction(User.ACTIVE)` = 삭제 안 됨 그리고 `withdrawn_at IS NULL`)가 탈퇴 유예 중·파기된 회원을 숨기고, 이벤트 `MemberWithdrawnEvent`·`MemberRestoredEvent`·`MemberPurgedEvent`(`api/dto/user/`)를 낸다.

- **Decision**
  - 유예 중: 리스너를 붙이지 않는다. 관계 행을 그대로 두고, 목록·수·요청 목록·프로필이 상대 회원을 `User` 기본 조회와 조인해 거른다(R5, R7). 그러면 복구(`withdrawn_at`이 풀림)만으로 그대로 돌아온다. 요청·팔로우 대상 찾기도 기본 조회로 하므로 유예 중인 회원은 `R001`이다
  - 이름을 보이는 곳에서 회원이 기본 조회에 없으면 018의 `MemberDisplay.nameOf`로 "탈퇴한 회원"을 보이고 `MemberSummary.withdrawn = true`다(008 화면 등, FR-002)
  - 복구됨: 붙이지 않는다. 마이그레이션이 유예 중인 회원의 공개 ID도 만들어 두므로(R14) 복구하면 그대로다
  - 파기됨: `SocialRelationPurgeListener`가 `MemberPurgedEvent`를 `@TransactionalEventListener(AFTER_COMMIT)` + 새 트랜잭션으로 받아, 그 회원이 한쪽인 행을 7개 테이블에서 지운다(`tb_friend` 양쪽, `tb_friend_request`·`tb_friend_rejection`·`tb_follow`·`tb_follow_request` 양쪽, `tb_user_public_id`, `tb_user_account_visibility`). 지운 건수 합이 1 이상이면 같은 트랜잭션에서 `PersonalDataPurgedEvent(SOCIAL_RELATION, ROW_DELETE, count, null, userId)`를 낸다(013이 파기 이력 한 줄로 남긴다). `PurgeTarget`에 `SOCIAL_RELATION`을 더한다
  - 실패하면(018 FR-017 "실패 기록과 다시 시도는 붙이는 기능이 맡는다") 매일 도는 `SocialRelationCleaner`가 `purged_at IS NOT NULL`인 회원의 남은 관계 행을 다시 지운다. 같은 정리기가 30일 지난 친구 거절 기록도 지운다(R4). 018 파기에서 5일 안(`docs/privacy.md`)을 지킨다
- **Rationale**
  - 유예 중에 행을 지우면 복구할 때 되살릴 길이 없다. 018 FR-017이 "탈퇴됨"에는 되돌릴 수 있는 처리만 붙이라고 한다. 조인으로 거르면 붙일 처리 자체가 없다
  - 파기 리스너를 따로 트랜잭션으로 두는 것은 018 이벤트 주석("커밋된 뒤에만 돌고, 처리가 실패해도 파기는 되돌아가지 않는다")을 따른 것이다
- **Alternatives considered**
  - 탈퇴 때 관계를 숨김 열로 표시: 모든 조회에 조건이 하나 더 붙고, 복구 때 되돌리는 처리가 또 필요하다. 기본 조회 조인이 이미 같은 일을 한다
  - 파기 때 관계를 남기기(`docs/privacy.md` 기본 규칙): 상대에게 보이지도 않는 행이 개인(누구와 친구였나)을 이어 준다. "남은 회원 ID가 어떤 개인정보와도 이어지지 않아야 파기가 끝난 것이다"에 어긋난다

## R16. 프로필의 활동 칸 (FR-019)

- **Decision**
  - 프로필 페이지는 머리 → 009 공개 항목 → 활동 칸 순서다. 활동 칸의 탭은 프론트에서 등록한다: `FE/components/member/profile-tabs.ts`에 `{ key, labelKey, order, Component }` 목록을 두고, 다른 기능이 자기 탭을 한 줄 더한다. 014는 빈 목록으로 시작하고, 015가 `shortforms` 탭(3열 격자)을 더한다
  - 탭 안의 내용은 각 기능의 API가 준다(015는 `GET /users/{publicId}/shortforms`). 각 API는 R6의 `ActivityVisibility.canView`로 막는다. 프로필 응답의 `canViewActivity`가 거짓이면 화면은 탭 대신 "비공개 계정입니다"를 보이고 탭 API를 부르지 않는다
  - 탭 목록이 비면 활동 칸을 그리지 않는다(US1-AC7)
- **Rationale**
  - 숏폼이 보이는 개인 피드는 새 스펙을 만들지 않는다(spec Clarifications 2026-11-27). 페이지와 공개 규칙의 주인은 014이고, 탭 내용의 주인은 각 기능이다. 018 FR-016의 계정 관리 탭(다른 기능이 항목을 더하는 구조)과 같은 방식이다
  - 서버가 탭 목록을 주지 않고 프론트가 등록하는 것은, 탭이 정적이고(기능마다 하나) 서버 메뉴처럼 관리자가 바꿀 일이 없기 때문이다
- **Alternatives considered**
  - 숏폼 격자를 014가 직접 그리기: 015의 썸네일·뷰어 규칙을 014가 알아야 한다
  - 새 스펙(개인 피드): 페이지가 둘로 나뉘거나 014 프로필과 겹친다
