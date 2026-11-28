---

description: "회원 프로필 보기, 친구, 팔로우 (friends-follow) 작업 목록"
---

# Tasks: 회원 프로필 보기, 친구, 팔로우 (friends-follow)

**Input**: `specs/014-friends-follow/`의 설계 문서

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/friends-api.md, quickstart.md

**먼저 있는 것**: `009-mypage`(`ProfileVisibility`, `MemberActivityPolicy`)와 `018-member-withdrawal`(`User` 기본 조회, `MemberPurgedEvent`, `MemberDisplay`)은 dev에 구현돼 있다 (research.md R8, R9, R15). 구현 브랜치는 그 뒤의 dev에서 딴다

**Tests**: 원칙 II에 따라 AC마다 테스트를 하나 둔다. 이름은 `@DisplayName("US<n>-AC<m> ...")`이다. 테스트는 기능 코드(백엔드·프론트)를 모두 만든 뒤 PR 직전에 쓴다(`CLAUDE.md`). 그래서 스토리 단계에는 구현만 두고, 테스트는 Phase 9에 모았다. 테스트가 실패하면 테스트를 고치지 말고 spec.md부터 본다 (원칙 V).

**검사**: 로컬에서는 빠른 검사(`./gradlew build`, `npm run check`)만 돌린다. 인수 테스트(`dbTest`)와 e2e는 PR CI로 본다. 사용자가 요청할 때만 로컬에서 돌린다 (`CLAUDE.md`).

**경로 줄임**:
- `BE` = `backend/src/main/java/com/moa`
- `BT` = `backend/src/test/java/com/moa`
- `MIG` = `backend/src/main/resources/db/migration`
- `FE` = `frontend/src`

## Format: `[ID] [P?] [Story] Description`

- **[P]**: 병렬 가능 (다른 파일, 의존 없음)
- **[Story]**: 작업이 속한 사용자 스토리

## 공통 규칙 (모든 작업)

- 계층은 Controller → Application → Service → Reader/Writer → Repository이고 도메인 이름은 `friend`, `follow`, `member`(프로필 읽기), `social`(탈퇴 정리)이다
  - `@Transactional`은 Application에만 둔다
  - `ServiceException`은 Service에서만 던진다
  - Service는 다른 Service를 부르지 않는다. 친구와 팔로우, 009 프로필, 007 알림, 018 탈퇴를 함께 쓰는 일은 Application이 묶는다 (`ArchitectureTest`)
- 회원은 주소와 응답에서 `publicId`로 가리킨다. 순번 `userId`는 이 기능의 주소와 응답에 싣지 않고, `login_id`도 싣지 않는다 (FR-003, `008-party` FR-027)
- 회원 한 명을 응답에 싣는 모양은 `MemberSummary { publicId, displayName, tag, avatar, withdrawn }` 하나다 (contracts "응답 모양")
- 상대 회원은 늘 `User` 기본 조회(`@SQLRestriction(User.ACTIVE)`: 삭제·탈퇴 유예 제외)로 찾거나 그와 조인한다. 그래서 탈퇴 유예 중인 회원은 목록·수·요청에서 빠지고 대상으로 찾으면 `R001`이다. 관계 행은 지우지 않는다 (FR-020, research.md R15)
- 관리자·운영자 막기 (research.md R9)
  - 부른 사람이 관리자 계정이면 403 `A301`
  - 대상이 관리자 계정이거나 없거나 탈퇴했으면 404 `R001`
  - 판단은 009의 `MemberActivityPolicy.isManagerAccount(userId)`다
- 피드 목록은 모두 `?before={id}&size=20`(1~50)이고, `size + 1`개를 읽어 다음이 있으면 `nextCursor`를 준다. 응답은 `{items, nextCursor}`다 (research.md R5)
- 응답의 시각은 `ApiTimes.of`로 만든 OffsetDateTime이다
- 화면 문구는 `frontend/messages/ko.json`에만 두고 코드에 한글을 직접 쓰지 않는다 (`scripts/check-i18n.mjs`)

---

## Phase 1: Setup

**Purpose**: 함께 고치는 문서, 스키마, 시드, 알림 종류, 파기 대상, 설정값. 더하는 의존성은 없다

- [ ] T001 다른 스펙 문서를 그 자리에서 고친다 (원칙 IV, plan.md "함께 고치는 문서")
  - 2026-11-28 `docs/014-cross-spec`에서 001과 `docs/privacy.md`를 고쳤다(010 계정 설정 칸, 018 FR-017 014 줄도 함께). 남은 것은 008 계약의 `publicId`와, 구현을 마칠 때 001 US2-AC4의 "지금은 …" 메뉴 문구에 "친구"를 더하는 것(T035와 함께)이다
  - `specs/001-admin-menus/spec.md`: FR-007 사용자 메뉴 표에 "친구"(회원만), FR-008 연결 화면 표에 `FRIENDS` `/friends`(로그인 필요) 줄을 더한다. US2-AC4의 "(지금은 홈, 공지, 모집, 내 파티)"처럼 사용자 메뉴를 세는 문구도 함께 맞춘다 (FR-014)
  - `docs/privacy.md`
    - 보관 기간 표에 research.md R11의 줄을 더한다: 친구 관계·대기 친구 요청·팔로우·대기 팔로우 요청, 친구 요청 거절 기록(30일), 공개 ID·계정 공개 여부
    - "탈퇴 회원 파기"에 예외 한 줄: 관계(친구·팔로우·요청)는 다른 회원과 엮여도 지운다 (FR-020)
  - `specs/008-party/contracts/party-api.md`: 회원 응답 `{userId, displayName}`(멤버 목록, 신청 목록, 채팅 보낸 사람)에 `publicId`를 더한다 (FR-002)
- [ ] T002 `MIG/V<다음 번호>__friends_follow.sql`을 만든다. data-model.md 그대로이고 규칙은 `docs/db.md`다 (research.md R14)
  - 번호는 지금 정하지 않는다. 파일 이름은 임시로 dev의 마지막 번호 + 1(2026-11-27 기준 `V14`)로 두고, PR 직전에 dev를 병합한 뒤 다시 마지막 번호 + 1로 맞춘다. CI `migration-numbers`가 막으면 번호를 올린다
  - 파일 머리 주석: `-- moa: 회원 공개 ID, 친구, 팔로우 테이블과 친구 메뉴, 알림 종류, 파기 대상. 스펙 specs/014-friends-follow (data-model.md, FR-003, FR-014, FR-016, FR-020)`
  - 테이블 7개: `tb_user_public_id`, `tb_friend`, `tb_friend_request`, `tb_friend_rejection`, `tb_follow`, `tb_follow_request`, `tb_user_account_visibility`
    - 열, 타입, NULL 여부, 인덱스·유니크 이름은 data-model.md 표다. 모든 테이블과 열에 COMMENT를 단다(`id`, 감사 열 제외). `DEFAULT`와 외래 키는 두지 않는다
    - `tb_user_public_id`: `user_id BIGINT NOT NULL` PK, `public_id CHAR(36) NOT NULL`, `created_at DATETIME(6) NOT NULL`. 감사 열 없음. 테이블 COMMENT `'회원 공개 ID (PK: 1:1 테이블; 감사 열 없음: 만든 뒤 바뀌지 않음)'`
    - `tb_friend`·`tb_friend_request`·`tb_follow`·`tb_follow_request`: 감사 열 4개(`BaseEntity`)
    - `tb_friend_rejection`: 감사 열 없음, `rejected_at DATETIME(6) NOT NULL`. 테이블 COMMENT `'친구 요청 거절 기록 (감사 열 없음: 30일 뒤 정리기가 지움)'`
    - `tb_user_account_visibility`: `user_id BIGINT NOT NULL` PK, `visibility VARCHAR(20) NOT NULL`(`PUBLIC`, `PRIVATE`), 시각 열만(`BaseTimeEntity`). 테이블 COMMENT `'계정 공개 여부 (PK: 1:1 테이블; 감사 열 없음: 회원 본인 설정)'`
    - 엔진·문자셋·정렬은 `V1__schema.sql`의 다른 테이블과 같다
  - 기존 회원의 공개 ID 채우기: `role = 'USER' AND deleted_at IS NULL AND purged_at IS NULL`인 회원마다 `UUID()`로 한 행을 넣는다. 탈퇴 유예 중인 회원도 넣는다. 이미 행이 있으면 넣지 않는다 (research.md R1, R14)
  - 메뉴 시드: `tb_menu`에 사용자 메뉴 "친구"(`screen_key` = `FRIENDS`, audience `USER`, visibility `MEMBER`)를 지금 사용자 메뉴 뒤에 넣는다. `screen_key`로 찾고 이미 있으면 넣지 않는다 (research.md R12)
  - `tb_notification.type`의 COMMENT에 `FRIEND_REQUESTED`, `FRIEND_ACCEPTED`, `FOLLOW_STARTED`, `FOLLOW_REQUESTED`, `FOLLOW_REQUEST_ACCEPTED`를 더한다. `MODIFY COLUMN`으로 지금 정의(`VARCHAR(30) NOT NULL`)를 그대로 옮겨 적고, 008이 먼저 들어갔으면 `PARTY_` 6종도 그대로 남긴다 (research.md R10)
  - `tb_purge_log.target`의 COMMENT에 `SOCIAL_RELATION`을 더한다. `V13__member_withdrawal.sql`처럼 `MODIFY COLUMN target VARCHAR(30) NOT NULL COMMENT '파기 대상 종류 (LOGIN_HISTORY, NOTIFICATION, REFRESH_TOKEN, RICH_TEXT_IMAGE, MEMBER, SOCIAL_RELATION)'`로 쓴다. 그 사이 다른 값이 더해졌으면 그대로 남긴다 (research.md R15)
  - 다 쓰면 `bootRun`으로 한 번 띄워 Flyway 적용과 `ddl-auto: validate` 통과를 본다. 엔터티(T007)가 생긴 뒤에 본다
- [ ] T003 [P] `BE/api/dto/notification/NotificationType.java`에 `FRIEND_REQUESTED`, `FRIEND_ACCEPTED`, `FOLLOW_STARTED`, `FOLLOW_REQUESTED`, `FOLLOW_REQUEST_ACCEPTED`를 더한다 (FR-016)
- [ ] T004 [P] `backend/src/main/resources/application.yaml`에 `app.friend.rejection-cooldown: 30d`, `app.friend.max-friends: 5000`을 더하고, 이 둘을 읽는 `@ConfigurationProperties` 레코드 `BE/api/service/friend/FriendProperties.java`를 만든다 (FR-005, FR-006, research.md R4)
- [ ] T005 [P] `BE/common/purgelog/PurgeTarget.java`에 `SOCIAL_RELATION`(주석 "친구·팔로우 관계 (탈퇴 회원 파기, specs/014-friends-follow)")을 더한다. 파기 이력 화면의 대상 이름 문구가 enum마다 있으면 ko.json에도 더한다 (research.md R15)

---

## Phase 2: Foundational

**Purpose**: 모든 스토리가 쓰는 엔터티, 공개 ID, 오류 코드, 회원 요약, 활동 공개 판단, 화면·메뉴, 프론트 뼈대

**⚠️ CRITICAL**: 이 단계가 끝나야 스토리 작업을 시작한다

### 백엔드

- [ ] T006 `BE/common/exception/ErrorCode.java`에 contracts "오류 코드" 표의 10개를 그 상태값 그대로 더한다
  - `A301` 403 관리자·운영자 계정은 친구·팔로우를 쓸 수 없음
  - `B301` 409 이미 친구, `B302` 409 이미 친구 요청 보냄, `B303` 400 자기 자신, `B304` 409 지금은 친구 요청할 수 없음(거절 뒤 30일)
  - `B305` 409 내 친구 상한, `B306` 409 상대 친구 상한, `B307` 409 이미 팔로우, `B308` 409 이미 팔로우 요청 보냄, `B309` 403 비공개 계정이라 팔로워만 볼 수 있음
- [ ] T007 [P] 엔터티를 만든다. 모두 `@Table(name = "tb_…")`을 적고, 인덱스·유니크는 엔터티에 적지 않는다 (`docs/db.md` "엔터티")
  - `BE/api/entity/user/UserPublicId.java`: `@Id userId`, `publicId`(36자), `createdAt`. 만든 뒤 바뀌지 않는다
  - `BE/api/entity/friend/Friend.java`, `FriendRequest.java`: `BaseEntity` + `@HardDelete(reason)`. reason은 data-model.md "관계 테이블은 이력이 필요 없어 하드 삭제다"
    - `FriendRequest`는 만들 때 `pair_low_id = min(requester, receiver)`, `pair_high_id = max(...)`를 생성자에서 채운다
  - `BE/api/entity/friend/FriendRejection.java`: 감사 열 없음, `@HardDelete`. `renew(now)`로 `rejectedAt`만 새로 한다
  - `BE/api/entity/follow/Follow.java`, `FollowRequest.java`(`requesterId`, `targetId`): `BaseEntity` + `@HardDelete`
  - `BE/api/entity/follow/UserAccountVisibility.java`(`BaseTimeEntity`, `@Id userId`, `change(visibility)`), enum `AccountVisibility { PUBLIC, PRIVATE }`
- [ ] T008 [P] `BE/api/repository/jpa/{user,friend,follow}/`에 리포지토리 7개(`JpaRepository`)를 만들고, `BE/api/repository/{reader,writer}/{user,friend,follow}/`에 Reader·Writer를 만든다
  - Writer의 `tb_friend_request`·`tb_friend`·`tb_follow`·`tb_follow_request` 저장은 `saveAndFlush`로 해 유니크 충돌이 그 자리에서 나게 한다 (research.md R3)
  - 커서 조회는 QueryDSL로 `id < before` 큰 순 `size + 1`개다. 친구는 `user_id`, 팔로워는 `followee_id`, 팔로잉은 `follower_id`, 받은 친구 요청은 `receiver_id`, 보낸 친구 요청은 `requester_id`, 받은 팔로우 요청은 `target_id`로 건다
  - 목록과 수는 상대 회원 열을 `User`(엔터티 기본 조회가 탈퇴 유예를 뺀다)와 조인한다. 조인 없이 세면 탈퇴 유예 중인 회원이 수에 들어간다 (US5-AC5)
  - 수 세기: 친구, 팔로워, 팔로잉, 받은 친구 요청, 받은 팔로우 요청 (research.md R7)
- [ ] T009 `BE/api/service/user/UserPublicIdService.java`를 만들고 회원이 생기는 두 길에 붙인다 (research.md R1)
  - `create(userId)`: UUID를 만들어 저장한다. `findUserId(publicId)`: 없으면 `R001`. `findPublicIds(userIds)`: 여러 회원을 한 번에
  - `BE/api/service/user/SocialAccountService.java`의 `signUp`에서 `User` 저장 바로 뒤에 행을 만든다. Service가 Service를 부를 수 없으므로 `UserPublicIdWriter`를 직접 쓰거나, 가입 Application(`SocialLoginApplication`)에서 `UserPublicIdService.create`를 부른다. ArchitectureTest가 통과하는 쪽을 고른다
  - `BE/api/service/user/UserAdminService.java`의 `create`에서 역할이 `USER`일 때만 만든다. 키트 파일을 고치는 것이므로 그 줄에 `// moa: 회원 공개 ID (specs/014-friends-follow research R1)` 주석을 단다 (plan.md Complexity Tracking)
- [ ] T010 `BE/api/dto/member/MemberSummaryDto.java`와 이를 만드는 `BE/api/application/v1/member/MemberSummaryAssembler.java`(Application 계층)를 만든다
  - 회원 ID 목록을 받아 `User` 기본 조회의 `name`, 공개 ID(T009), 009 프로필의 태그·아바타를 한 번에 읽어 `{publicId, displayName, tag, avatar, withdrawn}`로 만든다. 목록마다 N+1이 생기지 않게 한다
  - 기본 조회에 없는 회원(탈퇴·삭제·파기)은 018의 `MemberDisplay.nameOf`로 이름을 정하고 `withdrawn: true`, `publicId: null`, 태그·아바타 `null`이다 (FR-002, 018 FR-008). 이 기능의 목록은 T008의 조인으로 이미 빠지므로 다른 기능(008 등)이 이 경우를 쓴다
  - 프로필 행이 없는 회원(관리자가 만든 회원)은 태그 `null`, 아바타 `default`다 (009 research R14)
- [ ] T011 활동 공개 판단을 만든다 (FR-018, research.md R6)
  - `BE/api/service/follow/AccountVisibilityService.java`: `visibilityOf(userId)`(행이 없으면 `PUBLIC`)
  - `BE/api/application/v1/member/ActivityVisibility.java`(`@Component`, Application 계층): `canView(viewerId, ownerId)` = 본인, 또는 `PUBLIC`, 또는 viewer가 owner를 팔로우(`FollowService.isFollowing`), 또는 친구(`FriendService.areFriends`). 015 등 활동 칸 탭을 붙이는 기능이 쓰도록 클래스 주석에 그 뜻을 적는다 (research.md R16)
  - `FriendService.areFriends`, `FollowService.isFollowing`은 이 작업에서 읽기 메서드로 먼저 만든다
- [ ] T012 [P] `BE/api/screen/FriendScreens.java`에 `ScreenDefinition.user("FRIENDS", "친구", "/friends", true)`를 등록한다. `NoticeScreens`와 같은 방식이다. 메뉴 줄은 T002가 넣고 프론트 `menu.ts`에는 더하지 않는다 (FR-014, research.md R12)

### 프론트

- [ ] T013 [P] frontend/messages/ko.json을 채운다
  - `errors`에 T006의 10개 코드 문구
  - `profile`(프로필 페이지, 수, 자물쇠 표시, "비공개 계정입니다", "마이페이지에서 고치기"), `friend`(친구 화면, 묶음 넷, 단추, 빈 묶음, 끊기 확인 "서로의 팔로우도 끊긴다"), `follow`(팔로우·팔로우 요청·요청됨·요청 취소 단추, 비공개 계정 팔로우 취소 확인 "다시 팔로우하려면 요청해야 한다", 팔로워·팔로잉 목록), `settings.accountVisibility`(공개·비공개와 한 줄 설명)
- [ ] T014 [P] `FE/api/member.ts`, `FE/api/friend.ts`, `FE/api/follow.ts`를 만들고 `FE/api/index.ts`의 `api.v1`에 `member`, `friend`, `follow`로 등록한다. 함수는 각 스토리 작업이 그 스토리의 경로만큼 더한다. 응답 타입은 `npm run gen:api` 뒤 `Schema<"...">`로 쓴다
- [ ] T015 [P] `FE/lib/use-cursor-list.ts`에 커서 피드 목록 훅을 만든다. TanStack Query `useInfiniteQuery`로 `{items, nextCursor}`를 이어 붙이고, 끝에 닿으면 다음 묶음을 부르는 감시 요소(IntersectionObserver)를 함께 내보낸다. 008 채팅도 쓸 수 있게 응답 모양 말고는 이 기능을 모르게 둔다 (research.md R5)
- [ ] T016 [P] `FE/components/member/member-link.tsx`를 만든다. `MemberSummary`를 받아 아바타와 `닉네임#0421`을 보이고 `/users/{publicId}`로 잇는다. `withdrawn`이면 "탈퇴한 회원" 글자만 보이고 링크가 아니다 (FR-002)
- [ ] T017 [P] `FE/components/member/profile-tabs.ts`에 활동 칸 탭 등록 목록을 만든다. 항목 모양은 `{ key, labelKey, order, Component }`이고 처음은 빈 배열이다. 파일 머리 주석에 "다른 기능이 자기 탭을 한 줄 더한다. 첫 탭은 015 숏폼"을 적는다. `order`로 정렬해 돌려주는 함수를 함께 둔다 (FR-019, research.md R16)

**Checkpoint**: 스키마, 공개 ID, 오류 코드, 회원 요약, 활동 공개 판단, 화면 등록, 프론트 뼈대가 있다. backend/에서 `./gradlew build`(ArchitectureTest 포함), frontend/에서 `npm run check`가 통과한다

---

## Phase 3: User Story 1 - 다른 회원의 프로필을 본다 (Priority: P1) 🎯 MVP

**Goal**: 이름을 누르면 공개 규칙대로 그 회원의 프로필과 수, 나와의 관계, 활동 칸이 보인다

**Independent Test**: 회원 둘로 한 사람이 다른 사람의 프로필을 열어 공개 항목만 보이는지 본다. 자동 테스트는 T037, T043

- [ ] T018 [US1] 백엔드 프로필 읽기를 구현한다
  - `BE/api/service/user/ProfileVisibility.java`(009)에 `view(ProfileDto profile, Long viewerUserId, boolean viewerIsFriend)`를 더한다. 디스코드 태그는 `MEMBERS`면 모두에게, `FRIENDS`면 `viewerIsFriend`일 때만 보인다. 기존 `view(profile, viewerUserId)`는 `viewerIsFriend = false`로 새 메서드를 부르게 하고, "친구 기능이 생기기 전까지" 주석을 지금 규칙으로 고친다. 클래스 주석에 `specs/014-friends-follow FR-015`를 더한다 (research.md R8)
  - `BE/api/controller/v1/member/ApiMemberProfileController.java`의 `GET /users/{publicId}/profile`
  - `BE/api/application/v1/member/MemberProfileApplication.java`(`@Transactional(readOnly = true)`)
    1. 부른 사람이 관리자면 `A301`
    2. `UserPublicIdService.findUserId` → `User` 기본 조회. 없거나, 대상이 관리자거나, 탈퇴면 `R001` (US1-AC4)
    3. 009 프로필 읽기로 `ProfileDto`를 얻고 `ProfileVisibility.view(profile, viewer, areFriends)`로 공개 항목을 거른다
    4. 수 셋(T008), `relation`, `privateAccount`(T011 `visibilityOf`), `canViewActivity`(T011 `canView`)를 붙인다
  - `relation` 계산은 `FriendService.areFriends`, `FriendRequestService.findPending(a, b)`, `FollowService.isFollowing`, `FollowRequestService.findPending(requester, target)`을 Application이 묶는다
    - `friend`: 내 publicId면 `SELF`, 친구면 `FRIENDS`, 내가 보낸 대기 요청이면 `REQUEST_SENT`, 받은 요청이면 `REQUEST_RECEIVED`(이때 `friendRequestId`), 아니면 `NONE`
    - `follow`: 자기 자신이면 `NONE`, 팔로우 중이면 `FOLLOWING`, 내가 보낸 대기 팔로우 요청이면 `REQUESTED`(이때 `followRequestId`), 아니면 `NONE`
  - 이 단계에서는 Service에 위 읽기 메서드만 만든다. 쓰기는 각 스토리가 더한다
  - `response/MemberProfileResponse.java`
- [ ] T019 [US1] 프로필 페이지를 만든다
  - `FE/app/(user)/(signed-in)/users/[publicId]/page.tsx`: 머리(아바타, 닉네임#태그, 자기소개, 수 셋, 관계 단추, 비공개면 자물쇠) → 009 공개 항목 → 활동 칸 순서다 (spec "프로필 페이지")
    - 수를 누르면 `followers/`, `following/`으로 간다. `canViewActivity`가 거짓이면 누를 수 없다. 친구 수는 숫자만 보인다(다른 회원의 친구 목록은 없다)
    - 활동 칸: `profile-tabs`(T017)가 비면 그리지 않는다. 있으면 `order` 순 탭이고 첫 탭이 열린다. `canViewActivity`가 거짓이면 탭 대신 "비공개 계정입니다"를 보이고 탭 내용을 부르지 않는다 (US1-AC7, US4-AC6)
  - 로그인하지 않았으면 `(signed-in)`의 AuthGate가 로그인 화면으로 보냈다 그 주소로 돌려보낸다 (US1-AC3). 따로 만들 것이 없는지 확인만 한다
  - `R001`이면 찾을 수 없음 화면을 보인다 (US1-AC4)
  - 내 프로필(`SELF`)이면 관계 단추 대신 "마이페이지에서 고치기"(`/account/profile`)를 보인다 (US1-AC5)
  - `FE/components/member/relation-buttons.tsx`: spec "프로필 페이지" 관계 단추 표대로 `relation`·`privateAccount`를 단추로 바꾼다. 상태와 단추를 고르는 함수는 `FE/lib/relation.ts`로 따로 두어 단위 테스트를 할 수 있게 한다. 누르는 동작은 US2~US5가 잇는다
- [ ] T020 [US1] 이름 링크를 쓴다 (US1-AC6, FR-002)
  - 친구·팔로워·팔로잉 목록, 받은·보낸 친구 요청, 받은 팔로우 요청의 각 줄은 `member-link`를 쓴다(각 스토리 화면 작업에서 지킨다)
  - 파티 화면(멤버 목록, 신청 목록, 채팅 보낸 사람)은 008 구현이 T001의 `publicId`로 `member-link`를 쓴다. 008이 이미 구현돼 있으면 여기서 008 응답에 `publicId`를 더하고 세 곳을 `member-link`로 바꾼다

**Checkpoint**: US1이 혼자 동작한다 (관계 단추는 보이기만 한다)

---

## Phase 4: User Story 2 - 친구 요청을 보내고 받는다 (Priority: P1)

**Goal**: 요청 → 수락이면 친구가 되고 서로 팔로우한다. 거절·취소, 동시 요청, 거절 뒤 30일이 동작한다

**Independent Test**: 회원 둘로 요청 → 수락까지 해서 서로의 친구 목록에 보이는지 본다. 자동 테스트는 T038

- [ ] T021 [US2] 친구 Service들을 만든다
  - `BE/api/service/friend/FriendRequestService.java`
    - `request(requester, receiver)`: 자기 자신 `B303`, 이미 친구 `B301`, 내가 보낸 대기 요청 있음 `B302`를 본다
    - 상대가 보낸 대기 요청이 있으면 저장하지 않고 "수락으로 바꿈" 결과를 돌려준다 (FR-004, US2-AC6)
    - 저장 때 쌍 유니크 충돌(`DataIntegrityViolationException`)이 나면 다시 읽어, 상대 요청이면 같은 결과를, 내 요청이면 `B302`를 준다 (research.md R3, 동시 요청)
    - `cancel(requester, receiver)`, `findForReceiver(requestId, receiver)`(없거나 내 것이 아니면 `R001`), `delete(request)`
  - `BE/api/service/friend/FriendRejectionService.java`
    - `checkNotBlocked(requester, receiver, now)`: receiver가 requester를 거절한 지 `rejection-cooldown`이 안 됐으면 `B304`. 남은 기간은 응답에 싣지 않는다 (US2-AC9)
    - 거절한 쪽이 먼저 보내는 방향은 보지 않는다
    - `record(requester, receiver, now)`: 같은 쌍이 있으면 `renew`
  - `BE/api/service/friend/FriendService.java`
    - `befriend(a, b)`: 두 사람 각각 친구 수 `>= max-friends`면 `B305`(나)·`B306`(상대)다. (a, b)와 (b, a) 두 행을 넣는다 (research.md R2)
  - `BE/api/service/follow/FollowService.java`: `followBoth(a, b)`. 서로의 `tb_follow`를 넣고, 이미 있으면 그대로 둔다. 공개 여부는 보지 않는다 (FR-007, US2-AC3)
  - `BE/api/service/follow/FollowRequestService.java`: `deleteBetween(a, b)`. 두 방향의 대기 팔로우 요청을 지운다 (FR-007, US4-AC7)
- [ ] T022 [US2] `BE/api/application/v1/friend/FriendApplication.java`에 요청·수락·거절·취소를 구현한다. 모두 앞에서 관리자 막기(공통 규칙)를 한다
  - `request`
    1. 대상 찾기 `R001`
    2. 30일 확인 `B304`
    3. `FriendRequestService.request`
    4. 수락으로 바뀌면 `acceptInternal`, 아니면 `{REQUESTED, requestId}`를 돌려준다
  - `accept(requestId)`: `findForReceiver` → `acceptInternal`
  - `acceptInternal`: 요청 지우기 → `befriend` → `followBoth` → `FollowRequestService.deleteBetween` → `{FRIENDS}`
  - `reject(requestId)`: `findForReceiver` → `record` → 요청 지우기. 알리지 않는다
  - `cancel(publicId)`: 내가 보낸 대기 요청이 없으면 `R001`
  - 받은·보낸 요청 목록: 커서 조회 + `MemberSummaryAssembler` → `{requestId, member, requestedAt}`
- [ ] T023 [US2] `BE/api/controller/v1/friend/ApiFriendRequestController.java`에 경로 6개를 둔다 (contracts "친구")
  - `POST /users/{publicId}/friend-requests`: `REQUESTED`면 201, 수락으로 바뀌면 200
  - `DELETE /users/{publicId}/friend-requests` 204
  - `POST /friend-requests/{requestId}/accept` 200, `POST /friend-requests/{requestId}/reject` 204
  - `GET /friend-requests/received`, `GET /friend-requests/sent`. `size`는 `@Min(1) @Max(50)`, 넘으면 `V001`
- [ ] T024 [US2] 화면을 잇는다
  - `FE/components/member/relation-buttons.tsx`: 친구 추가, 요청 취소, 수락·거절을 `api.v1.friend`로 잇는다. 끝나면 그 회원의 프로필 쿼리를 다시 부른다. 오류는 `errors` 문구로 보이고, `B301`·`B302`면 프로필을 다시 불러 지금 관계를 보인다 (US2-AC7)
  - `FE/app/(user)/(signed-in)/friends/page.tsx`: 친구 화면의 틀(`?tab=friends|received|sent|follow-requests`)과 "받은 요청" 묶음을 만든다. 각 줄은 `member-link`, 요청한 시각, 수락·거절 단추다. 목록은 `use-cursor-list`다
  - 수락할 요청이 그사이 사라졌으면(`R001`) 알리고 목록을 다시 부른다 (Edge Cases)

**Checkpoint**: US1·US2로 요청 → 수락 → 서로 팔로우가 화면에서 된다

---

## Phase 5: User Story 3 - 공개 계정을 팔로우한다 (Priority: P2)

**Goal**: 공개 계정은 수락 없이 팔로우·취소하고, 팔로워·팔로잉 목록을 본다

**Independent Test**: 회원 둘로 팔로우 → 취소를 해서 목록과 수가 바뀌는지 본다. 자동 테스트는 T039

- [ ] T025 [US3] 공개 계정 팔로우를 구현한다
  - `FollowService.follow(follower, followee)`: `tb_follow`를 넣는다. 유니크 충돌은 `B307`로 바꾼다. `unfollow`: 없으면 `R001`
  - `BE/api/application/v1/follow/FollowApplication.java`
    - `follow(publicId)`: 관리자 막기, 대상 찾기, 자기 자신 `B303`, 이미 팔로우 `B307`. 대상이 `PUBLIC`이거나 친구면 `FollowService.follow` → `{FOLLOWING}`. `PRIVATE`이고 친구가 아니면의 갈래는 T027이 채운다
    - `unfollow(publicId)`: 친구 사이의 팔로우 취소도 친구는 건드리지 않는다 (US3-AC3)
    - 팔로워·팔로잉 목록: 대상이 없으면 `R001`, `ActivityVisibility.canView`가 거짓이면 `B309`(FR-018). 커서 + `MemberSummaryAssembler` → `MemberSummary + followedAt`
  - `BE/api/controller/v1/follow/ApiFollowController.java`
    - `POST /users/{publicId}/follow`: `FOLLOWING`이면 201, `REQUESTED`면 202. `DELETE /users/{publicId}/follow` 204
    - `GET /users/{publicId}/followers`, `GET /users/{publicId}/following`
- [ ] T026 [US3] 화면을 잇는다
  - `relation-buttons.tsx`의 팔로우·팔로우 취소를 잇는다. 응답 `result`로 단추를 바꾼다(`FOLLOWING` → 팔로우 중, `REQUESTED` → 요청됨)
  - `FE/app/(user)/(signed-in)/users/[publicId]/followers/page.tsx`, `following/page.tsx`: `use-cursor-list`로 최근 순, 끝까지 내리면 다음 묶음 (US3-AC5). 각 줄은 `member-link`다. `B309`면 "비공개 계정입니다"를 보인다

**Checkpoint**: US3이 혼자 동작한다

---

## Phase 6: User Story 4 - 비공개 계정으로 팔로우를 승인제로 받는다 (Priority: P2)

**Goal**: 공개·비공개를 고르고, 비공개 계정은 팔로우 요청을 수락·거절하며, 활동은 팔로워·친구에게만 보인다

**Independent Test**: 비공개로 바꾼 뒤 다른 회원이 팔로우 요청을 보내고, 수락 전에는 활동이 보이지 않고 수락 뒤에 보이는지 본다. 자동 테스트는 T040

- [ ] T027 [US4] 팔로우 요청과 공개 여부를 구현한다 (FR-010, FR-017, research.md R6)
  - `FollowRequestService`
    - `request(requester, target)`: 이미 대기 요청이 있으면 `B308`(유니크 충돌도 `B308`). 저장해 `requestId`를 돌려준다
    - `cancel(requester, target)`: 없으면 `R001`. `findForTarget(requestId, target)`: 없거나 내 것이 아니면 `R001`. `delete(request)`
    - `acceptAll(target)`: 그 회원이 받은 대기 요청을 모두 `tb_follow`로 옮기고(이미 팔로우면 건너뛴다) 지운 뒤 옮긴 수를 돌려준다
  - `AccountVisibilityService.change(userId, visibility)`: 행이 없으면 만든다
  - `FollowApplication`
    - `follow`의 `PRIVATE`이고 친구가 아닌 갈래: `FollowRequestService.request` → `{REQUESTED, requestId}` (US4-AC2)
    - `cancelRequest(publicId)`, `acceptRequest(requestId)`(요청 지우기 → `FollowService.follow` → `{FOLLOWING}`. 이미 팔로우가 됐으면 요청만 지운다), `rejectRequest(requestId)`(요청만 지운다. 기록·알림 없음, US4-AC4)
    - 받은 팔로우 요청 목록·수: 커서 + `MemberSummaryAssembler` → `{requestId, member, requestedAt}`
    - `changeVisibility(visibility)`: 한 트랜잭션에서 `change` → `PUBLIC`이면 `acceptAll`. 비공개로 바꿀 때는 아무것도 지우지 않는다. 알리지 않는다 (US4-AC8)
  - `BE/api/controller/v1/follow/ApiFollowRequestController.java`: `DELETE /users/{publicId}/follow-requests` 204, `GET /follow-requests/received`, `GET /follow-requests/received/count`, `POST /follow-requests/{requestId}/accept` 200, `POST /follow-requests/{requestId}/reject` 204
  - `BE/api/controller/v1/follow/ApiAccountVisibilityController.java`: `GET /users/me/account-visibility`, `PUT /users/me/account-visibility` `{ visibility }`. 값이 없거나 enum 밖이면 `V001`
- [ ] T028 [US4] 화면을 잇는다
  - `FE/app/(account)/settings/page.tsx`에 계정 공개 여부 칸(공개 / 비공개 라디오와 한 줄 설명)을 `PushToggle` 아래에 더한다. 화면 머리 주석에 `specs/014-friends-follow FR-010`을 더한다. 바꾸면 저장하고 결과를 알린다 (US4-AC1)
  - `relation-buttons.tsx`: 비공개 계정에는 "팔로우 요청", 요청했으면 "요청됨 · 요청 취소"를 보인다. 비공개 계정의 팔로우 취소는 `ConfirmDialog`로 한 번 더 묻는다 (spec "프로필 페이지")
  - `friends/page.tsx`에 "팔로우 요청" 묶음(수 표시, 수락·거절)을 더한다

**Checkpoint**: US4가 혼자 동작한다

---

## Phase 7: User Story 5 - 친구를 끊고 관계를 관리한다 (Priority: P2)

**Goal**: 친구 화면의 네 묶음, 닉네임 거르기, 친구 끊기, 사용자 메뉴 "친구", 탈퇴 회원의 관계 처리

**Independent Test**: 친구 둘 중 한 사람이 끊고 두 사람의 친구·팔로우 목록을 본다. 자동 테스트는 T041

- [ ] T029 [US5] 백엔드를 구현한다
  - `FriendService.unfriend(a, b)`: 두 행을 지운다. 친구가 아니면 `R001`. `FollowService.unfollowBoth(a, b)`: 서로의 팔로우를 지운다(없으면 그대로)
  - `FriendApplication.unfriend`는 두 일을 한 트랜잭션에서 묶는다. 알리지 않는다 (FR-007, US5-AC1)
  - `FriendApplication.friends(q, before, size)`: 커서 + `q`가 있으면 `tb_user.name` 포함 검색 → `MemberSummary + friendsSince`(`created_at`) (US5-AC3)
  - `FriendApplication.receivedCount`
  - `BE/api/controller/v1/friend/ApiFriendController.java`: `DELETE /users/{publicId}/friends` 204, `GET /friends`, `GET /friend-requests/received/count`
- [ ] T030 [US5] 화면을 잇는다
  - `friends/page.tsx`에 "친구" 묶음(거르기 입력, 친구 끊기)과 "보낸 요청" 묶음(요청 취소)을 더하고, 머리에 받은 요청 수를 보인다 (US5-AC2)
  - 친구 끊기는 `ConfirmDialog`로 한 번 더 묻는다. `relation-buttons.tsx`의 친구 끊기도 같은 확인을 쓴다
  - 사용자 메뉴 "친구"는 T002·T012로 서버 메뉴에 이미 있다. 로그인하지 않으면 보이지 않는지(`MEMBER`) 화면에서 확인한다 (US5-AC4)
  - 빈 묶음 안내, 불러오기 실패 시 다시 시도 (spec "친구 화면")
- [ ] T031 [US5] 탈퇴 회원 파기 때 관계를 지운다 (FR-020, research.md R15)
  - `BE/api/service/social/SocialRelationPurgeService.java`: `purge(userId)`. 7개 테이블에서 그 회원이 한쪽인 행을 지우고 지운 건수 합을 돌려준다(`tb_friend`는 `user_id`·`friend_user_id` 양쪽, 요청·거절·팔로우·팔로우 요청은 두 열 양쪽, `tb_user_public_id`, `tb_user_account_visibility`). `purgeLeftovers()`: `purged_at IS NOT NULL`인 회원의 남은 행을 같은 방식으로 지운다(기본 조회 밖이므로 native 쿼리)
  - `BE/api/application/v1/social/SocialRelationPurgeListener.java`: `MemberPurgedEvent`를 `@TransactionalEventListener(phase = AFTER_COMMIT)` + `@Transactional(propagation = REQUIRES_NEW)`로 받아 `purge`를 부르고, 건수가 1 이상이면 같은 트랜잭션에서 `PersonalDataPurgedEvent(PurgeTarget.SOCIAL_RELATION, PurgeMethod.ROW_DELETE, count, null, userId)`를 낸다. 실패는 로그로 남기고 다시 던지지 않는다(018이 되돌아가지 않는다)
  - `BE/api/scheduler/SocialRelationCleaner.java`: `NotificationCleaner`와 같은 방식(`@Scheduled`, 매일 새벽)으로 두 가지를 한다. 30일 지난 `tb_friend_rejection` 행 지우기(research.md R4)와 `purgeLeftovers`(파기 리스너가 실패한 경우 다시 하기). 지운 건수마다 파기 이력 이벤트를 낸다(거절 기록은 `byScheduler`, 남은 관계는 회원마다)
  - 유예 중·복구에는 붙이지 않는다. T008의 조인이 유예 중인 회원을 가리고, 복구하면 그대로 돌아온다 (US5-AC5)

**Checkpoint**: US5가 혼자 동작한다

---

## Phase 8: User Story 6 - 친구 요청과 새 팔로워를 알림으로 받는다 (Priority: P2)

**Goal**: 친구 요청 받음·수락됨, 새 팔로워, 팔로우 요청 받음·수락됨이 개인 알림으로 온다. 그 밖의 일에는 알림이 없다

**Independent Test**: 요청·수락·팔로우를 각각 하고 받는 사람에게만 알림이 생기는지 본다. 자동 테스트는 T042

- [ ] T032 [US6] 알림을 보낸다 (FR-016, research.md R10). 처리한 같은 트랜잭션에서 `NotificationApplication.notify(userId, type, title, link)`를 부른다. 제목의 `{이름}`은 한 일을 한 회원의 `tb_user.name`이다
  - `FriendApplication.request`가 `REQUESTED`면 받은 회원에게 `FRIEND_REQUESTED`. 제목 `{이름}님이 친구 요청을 보냈습니다`, 연결 `/friends?tab=received`
  - `acceptInternal`(수락, 동시 요청이 수락으로 바뀐 경우 포함): 먼저 요청한 회원에게 `FRIEND_ACCEPTED`. 제목 `{이름}님과 친구가 되었습니다`, 연결 `/users/{수락한 회원 publicId}`. `followBoth`로 생긴 팔로우에는 보내지 않는다 (US6-AC2)
  - `FollowApplication.follow`가 `FOLLOWING`(공개 계정, 친구 아님)이면 받은 회원에게 `FOLLOW_STARTED`. 제목 `{이름}님이 회원님을 팔로우했습니다`, 연결 `/users/{팔로우한 회원 publicId}`
  - `follow`가 `REQUESTED`면 비공개 계정 주인에게 `FOLLOW_REQUESTED`. 제목 `{이름}님이 팔로우를 요청했습니다`, 연결 `/friends?tab=follow-requests` (US6-AC5)
  - `acceptRequest`: 요청한 회원에게 `FOLLOW_REQUEST_ACCEPTED`. 제목 `{이름}님이 팔로우 요청을 수락했습니다`, 연결 `/users/{수락한 회원 publicId}`. 주인에게 `FOLLOW_STARTED`는 보내지 않는다 (US6-AC6)
  - 친구·팔로우 요청 거절·취소, 친구 끊기, 팔로우 취소, 공개 여부 바꾸기(`acceptAll` 포함)에는 부르지 않는다 (US6-AC4)
- [ ] T033 [P] [US6] `FE/components/notification/realtime-toasts.tsx`의 관련 화면 판단에 친구·팔로우 종류를 더한다 (007 FR-007). `FRIEND_REQUESTED`는 `/friends?tab=received`, `FOLLOW_REQUESTED`는 `/friends?tab=follow-requests`를 보고 있으면, 나머지 셋은 그 연결 프로필을 보고 있으면 토스트를 띄우지 않는다. 받은 요청 목록·수 쿼리를 다시 부르도록 `RESYNC_KEYS`에도 친구·팔로우 요청 쿼리 키를 더한다

**Checkpoint**: 모든 스토리의 기능 코드가 있다

---

## Phase 9: 테스트 (PR 직전)

**Purpose**: 기능 코드를 모두 만든 뒤 AC마다 테스트를 쓴다 (원칙 II, `CLAUDE.md`)

- 백엔드 인수 테스트는 `AcceptanceTest`를 상속한다. 클래스 이름은 plan.md 구조의 이름이다
- 로컬에서는 `./gradlew build`, `npm run check`로 컴파일과 단위 테스트까지만 본다. 인수 테스트와 e2e의 통과는 PR CI로 본다
- 테스트가 실패하면 테스트를 고치지 말고 spec.md부터 본다 (원칙 V)

### 테스트 지원

- [ ] T034 [P] `BT/support/FriendFixtures.java`를 만든다. `TestUsers`로 회원을 만들고 공개 ID, 친구 두 행, 대기 친구 요청, 거절 기록(시각을 정해서), 팔로우, 대기 팔로우 요청, 계정 공개 여부를 JdbcTemplate으로 바로 넣는 도우미를 둔다. 관리자 계정과 탈퇴 유예 중인 회원(`withdrawn_at`을 채워서)도 만들 수 있게 한다
- [ ] T035 `BT/api/AdminMenusAcceptanceTest.java`의 사용자 메뉴 기대값을 001 FR-007 표(T001에서 "친구" 더함)에 맞춘다. 로그인하지 않은 사람에게는 "친구"가 없다. 기대값의 근거인 001 표를 먼저 고쳤다 (원칙 V)
- [ ] T036 009의 `ProfileVisibilityTest`에서 디스코드 `FRIENDS`의 기대값을 바꾼다: 두 인자 `view`는 그대로 안 보이고, 세 인자 `view`는 `viewerIsFriend`일 때만 보인다. 근거는 이 스펙 FR-015다 (원칙 V. 기존 기대값이 "친구 기능 전까지"라는 잠정 규칙이었다)

### 백엔드 인수 테스트

- [ ] T037 [P] [US1] `BT/api/friend/MemberProfileAcceptanceTest.java`에 US1-AC1~AC5를 쓴다
  - AC1: 나이·성별 비공개, 디스코드 "전체 회원"인 회원의 프로필에 디스코드 태그가 있고 나이·성별이 없다. 디스코드 "친구만"이면 친구에게만 보인다(FR-015)
  - AC2: 친구 3, 팔로워 5, 팔로잉 2를 넣고 `counts`가 그 값이다
  - AC3: 토큰이 없으면 401 A001이다. 로그인 뒤 돌아오는 것은 e2e(T044)에서 본다
  - AC4: 관리자 계정, 없는 publicId, 탈퇴 유예 중인 회원은 404 R001이다. 관리자 계정이 부르면 403 A301이다
  - AC5: 내 publicId면 `relation.friend`가 `SELF`, `follow`가 `NONE`이고, 내가 비공개여도 `canViewActivity`가 true다
  - AC6·AC7은 화면이라 e2e(T044)와 단위 테스트(T043)에서 본다
- [ ] T038 [P] [US2] `BT/api/friend/FriendRequestAcceptanceTest.java`에 US2-AC1~AC9를 쓴다
  - AC1: 201 `REQUESTED`이고 `tb_follow`에 행이 없다. 상대 프로필의 `relation.friend`가 `REQUEST_SENT`다
  - AC2: 받은 요청 셋이 `id DESC`이고 각 줄에 `member.tag`, `requestedAt`이 있다
  - AC3: 수락하면 `tb_friend` 두 행, `tb_follow` 두 행이고 요청 행이 없다. 한쪽이 비공개 계정이어도 같다
  - AC4: 거절하면 요청 행이 없고 `tb_friend_rejection`에 한 행이다
  - AC5: 취소하면 받은 요청 목록에서 빠진다
  - AC6: B가 A에게 보내면 200 `FRIENDS`다. 그리고 `ExecutorService`와 `CountDownLatch`로 두 회원이 동시에 서로 요청하면 `tb_friend`가 두 행, 요청 행이 0이다
  - AC7: 이미 친구면 409 B301, 이미 보냈으면 409 B302다
  - AC8: 자기 자신은 400 B303, 관리자 계정은 404 R001이다
  - AC9: 거절 기록을 29일 전으로 넣으면 409 B304이고 응답에 남은 기간이 없다. 31일 전이면 201이다. 거절한 B가 A에게 보내는 것은 201이다
  - 상한(FR-005): `max-friends`를 작게 둔 테스트 설정이나 행을 채워서 B305·B306을 본다
- [ ] T039 [P] [US3] `BT/api/friend/FollowAcceptanceTest.java`에 US3-AC1~AC5를 쓴다
  - AC1: 공개 계정을 팔로우하면 201 `FOLLOWING`이고, B의 followers와 A의 following에 서로가 있으며 `tb_friend`는 없다
  - AC2: 취소하면 204이고 목록에서 빠진다
  - AC3: 친구 사이에서 A가 취소해도 `tb_friend` 두 행과 B→A 팔로우가 남는다
  - AC4: 자기 자신 400 B303, 관리자 404 R001, 이미 팔로우 409 B307
  - AC5: 팔로워 25명이면 첫 묶음 20개와 `nextCursor`, 둘째 묶음 5개와 `nextCursor: null`이다
- [ ] T040 [P] [US4] `BT/api/friend/PrivateAccountAcceptanceTest.java`에 US4-AC1~AC8을 쓴다
  - AC1: 정한 적 없으면 `PUBLIC`이다
  - AC2: 비공개 B를 친구 아닌 A가 팔로우하면 202 `REQUESTED`이고 `tb_follow`가 없으며 B의 팔로워 수가 그대로다. 다시 누르면 409 B308이다
  - AC3: B가 수락하면 `tb_follow`(A→B) 하나이고 B→A와 `tb_friend`는 없다
  - AC4: 거절하면 요청 행이 없고 A는 바로 다시 202를 받는다
  - AC5: A가 취소하면 B의 받은 팔로우 요청에서 빠진다
  - AC6: 팔로워·친구가 아닌 A가 B의 프로필을 열면 `privateAccount` true, `canViewActivity` false이고 공개 항목·수는 있다. followers·following은 403 B309다. 팔로워 C와 친구 D는 200이다
  - AC7: A가 팔로우 요청을 보낸 뒤 친구가 되면 서로 `tb_follow`가 있고 `tb_follow_request`가 없다
  - AC8: 팔로워 C가 있는 B가 비공개로 바꿔도 C의 행이 남는다. 요청 둘이 있는 비공개 B가 공개로 바꾸면 `tb_follow` 둘이 생기고 요청이 0이며 `tb_notification`이 늘지 않는다
- [ ] T041 [P] [US5] `BT/api/friend/FriendManageAcceptanceTest.java`에 US5-AC1~AC6을 쓴다
  - AC1: 끊으면 `tb_friend`와 서로의 `tb_follow`가 없고 B에게 알림이 없다
  - AC2: 친구 12명, 받은 요청 2개, 보낸 요청 1개, 받은 팔로우 요청 3개면 `GET /friends`가 12개, `friend-requests/received/count`가 2, `sent`가 1개, `follow-requests/received/count`가 3이다
  - AC3: `q`로 닉네임 일부를 주면 그 글자가 든 친구만 나온다
  - AC4: 회원의 사용자 메뉴에 "친구"가 있고 `/friends`로 간다. 로그인하지 않으면 없다
  - AC5: 친구 B와 팔로워 C를 018 탈퇴 API로 탈퇴시키면 A의 친구·팔로워 목록과 `counts`에서 빠진다. 관계 행은 남는다. B를 018 복구로 되돌리면 다시 보인다
  - AC6: B를 018 파기 처리로 파기하면(테스트에서 파기 정리를 직접 부른다) 7개 테이블에 B가 한쪽인 행이 없고, `tb_purge_log`에 `SOCIAL_RELATION`, 대상 B인 줄이 하나 있다
  - 관리자 계정이 `GET /friends`를 부르면 403 A301이다 (FR-013)
- [ ] T042 [P] [US6] `BT/api/friend/FriendNotificationAcceptanceTest.java`에 US6-AC1~AC6을 쓴다
  - 알림마다 "받는 사람에게 그 종류의 개인 알림이 하나 생기고, 한 사람에게는 생기지 않는다"를 `tb_notification`(또는 알림 API)으로 본다. 토스트·브라우저 알림 모양은 007 테스트가 본다
  - AC1: 친구 요청하면 B에게 `FRIEND_REQUESTED` 하나, A에게 없다. 연결이 `/friends?tab=received`다
  - AC2: 수락하면 A에게 `FRIEND_ACCEPTED` 하나이고 `FOLLOW_STARTED`는 아무에게도 없다
  - AC3: 친구 아닌 A가 공개 계정 B를 팔로우하면 B에게 `FOLLOW_STARTED` 하나다
  - AC4: 친구·팔로우 요청 거절·취소, 끊기, 팔로우 취소, 공개 여부 바꾸기 뒤 알림 수가 늘지 않는다
  - AC5: 비공개 B에게 팔로우 요청하면 B에게 `FOLLOW_REQUESTED` 하나이고 연결이 `/friends?tab=follow-requests`다
  - AC6: B가 수락하면 A에게 `FOLLOW_REQUEST_ACCEPTED` 하나이고 B에게 `FOLLOW_STARTED`가 없다
  - 제목이 FR-016 표 그대로인지 한 번씩 본다

### 프론트

- [ ] T043 [P] 단위 테스트를 쓴다
  - `frontend/tests/unit/friend-relation.test.ts`: `FE/lib/relation.ts`의 상태 표 테스트(spec "프로필 페이지" 표의 여섯 줄 × 공개·비공개, `SELF`)
  - `frontend/tests/unit/friend-cursor-list.test.ts`: `use-cursor-list`의 이어 붙이기와 `nextCursor: null`에서 멈추기
  - `frontend/tests/unit/profile-tabs.test.ts`: 등록 목록이 비면 활동 칸이 없고, 둘을 넣으면 `order` 순이며 첫 탭이 열린다 (US1-AC7). 시험용 탭은 테스트 안에서만 넣는다
- [ ] T044 [P] `frontend/e2e/friends.spec.ts`에 핵심 흐름 2개를 쓴다 (plan.md Testing)
  1. 로그인하지 않은 채 프로필 주소를 열어 로그인 화면을 거쳐 돌아온다(US1-AC3). 친구 추가 → (다른 회원) 받은 요청에서 수락 → 두 사람의 수가 바뀐다. 이름 링크로 프로필을 연다(US1-AC6)
  2. 비공개 계정의 프로필에 자물쇠와 "비공개 계정입니다"가 보이고, "팔로우 요청" → "요청됨"이 된다. 주인이 친구 화면의 팔로우 요청에서 수락한다
  - REST는 `page.route`로 흉내 낸다. 로그인은 `e2e/support/api.ts`의 `signedIn`이다
  - 화면마다 axe 검사를 넣는다. 흐름 1에 키보드만으로 하는 단계(Tab·Enter로 친구 추가, 끊기 확인 창 Esc)를 넣는다

### 추적

- [ ] T045 AC 추적을 확인한다. `grep -rhoE 'US[0-9]+-AC[0-9]+' backend/src/test/java/com/moa/api/friend frontend/tests/unit frontend/e2e/friends.spec.ts | sort -u | wc -l`이 41인지 본다(다른 기능과 섞이지 않게 014 파일로 좁힌다). 빠진 AC가 있으면 그 스토리의 테스트를 더한다

---

## Phase 10: Polish & Cross-Cutting

- [ ] T046 로컬 빠른 검사를 돌린다: backend/에서 `./gradlew spotlessApply`, `./gradlew build`, frontend/에서 `npm run format`, `npm run check`
  - ArchitectureTest가 실패하면 공통 규칙의 계층 배치(특히 T009, T010, T011, T031)를 다시 본다
  - 인수 테스트와 e2e는 PR CI 결과로 보고, 실패하면 그때 고친다. 테스트를 고쳐서 통과시키지 않는다 (원칙 V)
- [ ] T047 `specs/README.md` 사용자 쪽 표에서 `014-friends-follow` 상태를 `스펙 작성 중`에서 `구현됨`으로 바꾼다. PR 설명에 009 담당에게 알릴 것(`ProfileVisibility` 세 인자 `view`, `ProfileVisibilityTest` 기대값)과 018 담당에게 알릴 것(파기됨에 붙은 관계 정리)을 적는다
- [ ] T048 quickstart.md "화면 확인"을 사람이 직접 한다. 시험 계정 비밀번호가 필요하므로 에이전트는 하지 않는다 (원칙 VI). `user2`도 사람이 더한다

---

## Dependencies & Execution Order

### 단계

- Phase 1(Setup) → Phase 2(Foundational) → 스토리들(Phase 3~8) → Phase 9(테스트) → Phase 10(Polish)
- T002 → T007(엔터티로 `validate` 확인). T003·T004·T005는 T002와 함께 할 수 있다
- T007 → T008 → T009 → T010 → T011
- 테스트 단계: T034 → T037~T042, T001 → T035

### 스토리 사이

| 스토리 | 기대는 것 | 이유 |
|---|---|---|
| US1 | Foundational | 읽기만 한다 |
| US2 | US1 | 프로필의 관계 단추에서 요청한다 |
| US3 | US1 | 프로필의 팔로우 단추. US2와는 따로 된다 |
| US4 | US3 | `follow`의 비공개 갈래를 채운다 |
| US5 | US2, US4 | 맺은 친구를 끊는다. 친구 화면의 네 묶음을 완성한다 |
| US6 | US2, US4 | 요청·수락·팔로우·팔로우 요청에 알림을 단다 |

- 한 스토리 안에서는 백엔드 → 화면 순서다
- `relation-buttons.tsx`, `friends/page.tsx`, `FriendApplication`, `FollowApplication`은 여러 스토리가 고치므로 나란히 하면 합칠 때 순서를 맞춘다

## Parallel Example

```text
Phase 1: T003, T004, T005는 T002와 다른 파일이다
Phase 2: T007(엔터티), T012(화면 등록), T013~T017(프론트 뼈대)은 서로 다른 파일이다
US1이 끝난 뒤: US2(friend 패키지)와 US3(follow 패키지)의 백엔드는 다른 파일이다
US5의 T031(social 패키지)은 T029·T030과 다른 파일이다
Phase 9: 테스트 클래스 T037~T042와 프론트 테스트 T043~T044는 모두 다른 파일이다
```

## Implementation Strategy

### MVP

Phase 1~2 → US1 → US2 (T001~T024)까지가 MVP다. 프로필을 열고, 친구 요청을 보내고, 수락하면 서로 팔로우한다. 009 디스코드 공개 범위 "친구만"이 이것으로 동작한다.

여기서 멈추고 quickstart.md 2~5단계로 확인한 뒤 다음으로 간다.

### 이어서

1. US3(공개 계정 팔로우), US4(비공개 계정과 팔로우 요청)
2. US5(친구 끊기·친구 화면 완성·탈퇴 회원 정리)
3. US6(알림)
4. Phase 9 테스트, Phase 10 마무리

### PR 나누기

구현 브랜치는 `feat/014-friends-follow` 하나다. 한 PR을 권한다. 나눈다면 MVP(US1·US2)와 그 테스트(T034~T038, T043)를 첫 PR로 낸다. 나눌지는 사람이 정한다.
