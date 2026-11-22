# Research: 회원 프로필 보기, 친구, 팔로우 (014)

스펙: [spec.md](./spec.md). 각 항목은 결정 / 이유 / 검토한 대안 순서다. 2026-11-22 dev(`f127bba`) 기준으로 코드를 확인했다.

## R1. 프로필 주소의 공개 ID (FR-003)

- **Decision**
  - 1:1 테이블 `tb_user_public_id`(PK `user_id`, `public_id CHAR(36)` UUID, 유니크)를 새로 둔다. 프로필 주소는 `/users/{publicId}`다
  - 회원(`USER`)이 생기는 두 길에서 행을 함께 만든다: 소셜 가입(`SocialAccountService`), 관리자의 계정 만들기(`UserAdminService.create`, 역할이 `USER`일 때). 지금 있는 회원은 마이그레이션이 채운다(삭제되지 않은 `USER`)
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
- 30일은 설정값 `app.friend.rejection-cooldown`(기본 `30d`)이다. 정리기 `FriendRejectionCleaner`가 기간이 지난 행을 지운다(R11, `docs/privacy.md` 보관 기간 표)
- **Rationale**: 요청 행은 거절하면 지워지므로(R3) 거절 사실은 따로 남겨야 한다. 기간이 지나면 쓸 데가 없으니 지운다
- **Alternatives considered**: 요청 행을 REJECTED 상태로 남기기: 쌍 유니크(R3)가 다시 요청을 막고, 대기 요청만 남긴다는 정의와 어긋난다

## R5. 피드 목록의 커서 (FR-011, FR-012)

- **Decision**: 친구·팔로워·팔로잉·받은 요청·보낸 요청 목록은 행 `id` 내림차순 커서다. `GET ...?before={id}&size=20`, 응답 `{items, nextCursor}`(더 없으면 `null`). 21개를 읽어 21번째가 있으면 다음이 있다
- 행 `id`는 늘어나는 값이라 "관계를 맺은 최근 순"과 같다. 친구 목록의 닉네임 거르기(`q`)는 `tb_user.name`의 포함 검색이고, 커서와 함께 쓴다
- **Rationale**: 원칙의 피드 목록(커서 무한 스크롤)이다. `008-party` research R5의 메시지 커서와 같은 방식이라 프론트 무한 스크롤을 함께 쓸 수 있다
- **Alternatives considered**: `created_at` 커서: 같은 시각이 겹치면 빠지거나 겹친다. 쪽 번호: 원칙상 피드 목록이 아니다

## R6. 팔로우와 팔로우 받기 범위 (FR-009, FR-010, US4)

- **Decision**
  - `tb_follow(follower_id, followee_id, created_at)`, `uk_follow_follower_id_followee_id`. 팔로잉 목록은 유니크의 앞 열로, 팔로워 목록은 `idx_follow_followee_id_id`로 찾는다
  - 팔로우 받기 범위는 1:1 `tb_user_follow_setting(user_id PK, follow_policy)`에 둔다. 행이 없으면 `ANYONE`이다. 바꿀 때만 행을 만든다
  - "친구만"(`FRIENDS_ONLY`)으로 바꾸는 트랜잭션에서, 그 회원을 팔로우하는 사람 중 친구가 아닌 회원의 `tb_follow` 행을 지운다 (FR-010, US4-AC4). 알리지 않는다
  - 친구 맺기(R2)는 범위와 상관없이 서로의 `tb_follow`를 넣는다(이미 있으면 그대로). 친구 끊기는 서로의 팔로우를 함께 지운다 (FR-007)
- **Rationale**: 팔로우 받기 범위는 회원 한 명의 설정이라 1:1이다. `tb_user_profile`(009)에 두면 행이 없을 수 있어(R1) 같은 문제가 생긴다
- **Alternatives considered**: 팔로우에 "승인 대기" 상태를 두기(인스타그램 비공개 계정): spec이 수락 없는 팔로우로 정했다

## R7. 수(친구·팔로워·팔로잉)

- **Decision**: 프로필을 열 때 `COUNT`로 센다. 세는 조건이 모두 인덱스의 앞 열이다. 따로 수를 저장하지 않는다
- **Rationale**: 친구는 5,000 상한이고 지금 회원 규모에서 COUNT가 충분히 빠르다. 저장된 수는 맺기·끊기·탈퇴마다 맞춰야 해서 어긋날 곳이 늘어난다
- **Alternatives considered**: 수를 칸에 두고 늘리고 줄이기: 팔로워가 매우 많은 회원이 생기면 그때 바꾼다(Complexity Tracking에 적지 않을 만큼의 단순 선택)

## R8. 다른 회원 프로필의 항목과 009 의존 (FR-001, FR-015)

- **Decision**
  - 프로필 항목과 공개 규칙은 009의 `ProfileVisibility.view(profile, viewer)`를 그대로 쓴다. 014는 수·관계·단추와 `publicId`만 더한다
  - 009의 디스코드 공개 범위 `FRIENDS` 판단("보는 사람이 그 회원의 친구인가")은 014의 `FriendService.areFriends`로 채운다. Application이 두 도메인을 묶는다(계층 규칙)
  - 009가 공개 범위 이름을 "친구만"으로 바꾸면(spec FR-015 제안) 값 이름은 `FRIENDS` 그대로 두고 문구만 바꾸면 된다
  - **구현 순서**: 009 구현(프로필 칸, `ProfileVisibility`, `MemberActivityPolicy`)이 014보다 먼저다. 014가 먼저면 프로필 페이지는 닉네임·태그·아바타 기본값과 수·관계만 보인다
- **Rationale**: 공개 규칙의 주인은 009다(`009-mypage` research R9). 같은 규칙을 두 곳에 두면 어긋난다

## R9. 관리자·운영자 막기 (FR-013)

- **Decision**: `MemberActivityPolicy.isManagerAccount(userId)`(009가 만든다. 009보다 먼저면 008·014 중 먼저 구현하는 쪽이 009 설계대로 만든다, `008-party` research R14)로 판단한다
  - 요청·수락·거절·취소·끊기·팔로우·팔로우 취소·범위 바꾸기·친구 화면 API를 부른 사람이 관리자 계정이면 403 `A301`
  - 대상이 관리자 계정이면 404 `R001`(프로필이 없다, US1-AC4·US2-AC8)
- **Rationale**: 009·008과 같은 기준과 같은 방식(서버에서 막고 화면은 사용자 셸이라 관리자에게 열리지 않는다)이다

## R10. 개인 알림 (FR-016)

- **Decision**: 요청·수락·팔로우를 처리한 같은 트랜잭션에서 `NotificationApplication.notify`를 부른다 (`008-party` research R13과 같은 방식)
  - `FRIEND_REQUESTED`: 요청 받은 회원에게. 연결 `/friends?tab=received`
  - `FRIEND_ACCEPTED`: 요청한 회원에게. 연결은 수락한 회원의 프로필 `/users/{publicId}`
  - `FOLLOW_STARTED`: 친구가 아닌 회원이 팔로우했을 때. 연결은 팔로우한 회원의 프로필. 친구 맺기로 생긴 팔로우에는 보내지 않는다
  - 동시 요청이 수락으로 바뀐 경우(R3)는 `FRIEND_ACCEPTED`만 먼저 보낸 쪽에 보낸다
- 종류 3개를 `NotificationType`에 더하고, `tb_notification.type`의 COMMENT를 같은 마이그레이션에서 고친다(`docs/db.md` "COMMENT"). 008이 먼저 들어가면 008의 `PARTY_` 6종도 COMMENT에 그대로 옮겨 적는다(`MODIFY COLUMN`은 정의 전체를 다시 쓴다)

## R11. 개인정보 보관 (`docs/privacy.md`)

- **Decision**: 보관 기간 표에 더할 줄
  - 친구 관계, 대기 친구 요청, 팔로우: 관계를 끊거나 요청이 처리되면 바로 지운다. 탈퇴 파기 때 그 회원이 한쪽인 행을 모두 지운다(그 회원과의 관계는 상대에게 쓸모가 없다)
  - 거절 기록: 30일 뒤 정리기가 지운다(R4). 탈퇴 파기 때도 지운다
  - 공개 ID, 팔로우 받기 범위: 탈퇴 파기 때 지운다
- 탈퇴 유예 중 처리(목록에서 "탈퇴한 회원")와 파기 실행은 회원 탈퇴 스펙이 이 표를 따른다 (spec Assumptions)
- **Rationale**: `docs/privacy.md` "개인정보를 담는 테이블을 새로 만드는 스펙은 보관 기간 표에 한 줄을 더한다"

## R12. 화면 경로와 메뉴 (FR-014)

- **Decision**
  - 프로필 `/users/{publicId}`, 팔로워·팔로잉 `/users/{publicId}/followers`·`/following`, 친구 화면 `/friends`(묶음은 `?tab=friends|received|sent`). 모두 로그인이 필요해 `(user)/(signed-in)`에 둔다 (`008-party` research R11과 같은 구조)
  - 친구 화면은 연결 화면 `FRIENDS`(`ScreenDefinition.user("FRIENDS", "친구", "/friends", true)`, `api/screen/FriendScreens.java`)와 사용자 메뉴 "친구"(회원만)로 연다. 메뉴 행은 마이그레이션이 `screen_key`로 찾아 없을 때만 넣는다 (`008-party` research R11)
  - 팔로우 받기 범위는 계정 설정 화면 `/settings`(`(account)/settings`)에 칸을 더한다
- **Rationale**: 001 FR-007·FR-008의 서버 메뉴 방식이다. 프로필은 메뉴로 열지 않아 연결 화면이 아니다

## R13. 오류 코드

- **Decision**: 친구·팔로우 권한 오류는 `A3xx`, 업무 오류는 `B3xx`다 (파티 `A1xx`·`B1xx`, 마이페이지 `A2xx`·`B2xx` 다음). 목록은 [contracts/friends-api.md](./contracts/friends-api.md)
- **Rationale**: `ErrorCode`의 접두 글자가 종류를 나타내고 기능마다 백 단위를 나눠 쓴다(`008-party` research R9)

## R14. 마이그레이션

- **Decision**: 파일 하나 `V<다음 번호>__friends_follow.sql`. 새 테이블 6개, 기존 회원의 공개 ID 채우기, 연결 화면 메뉴 시드, `tb_notification.type` COMMENT 고치기를 함께 둔다. 번호는 PR 직전에 dev를 병합하고 정한다 (`docs/db.md` "마이그레이션")
- 공개 ID 채우기는 MariaDB `UUID()`로 한다
