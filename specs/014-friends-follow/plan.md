# Implementation Plan: 회원 프로필 보기, 친구, 팔로우 (friends-follow)

**Branch**: `docs/014-friends-follow-plan` (설계), `feat/014-friends-follow` (구현) | **Date**: 2026-11-22, 2026-11-27 고침 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/014-friends-follow/spec.md`

## Summary

회원은 다른 회원의 프로필 페이지(009의 공개 규칙)를 보고, 친구 요청·수락으로 친구를 맺고, 공개 계정은 수락 없이, 비공개 계정은 팔로우 요청과 승인으로 팔로우한다. 친구가 되면 서로 팔로우한다. 비공개 계정의 활동(팔로워·팔로잉 목록, 프로필 활동 칸)은 팔로워와 친구에게만 보인다. 친구·팔로우 요청, 수락, 새 팔로워는 007 개인 알림으로 간다. 탈퇴 유예 중인 회원은 목록에서 빠지고, 파기되면 관계를 지운다(018).

설계의 뼈대는 이렇다 (research.md).

- 프로필 주소는 새 1:1 테이블의 공개 ID(UUID)를 쓴다 (R1)
- 친구는 한 쌍을 두 행으로 둔다 (R2)
- 대기 친구 요청은 쌍마다 하나를 DB 유니크로 막아 동시 요청도 친구 한 쌍으로 모은다 (R3)
- 팔로우 요청은 `tb_follow`의 상태가 아니라 따로 둬서, 팔로워 조회는 늘 승인된 것만 본다 (R6)
- 목록은 모두 행 ID 커서의 피드 목록이고, 상대 회원은 `User` 기본 조회와 조인해 탈퇴 유예 중인 회원을 뺀다 (R5, R15)
- 프로필의 활동 칸은 다른 기능이 탭을 붙이는 틀이다. 첫 탭은 015 숏폼이다 (R16)

## Technical Context

**Language/Version**: Java 21 (Spring Boot 4.1.2), TypeScript 5 (Next 16.3, React 19.2)

**Primary Dependencies**: 기존 골격(Spring MVC, JPA + QueryDSL, Flyway, JWT), 007 `NotificationApplication`, 009 `ProfileVisibility`·`MemberActivityPolicy`(dev에 구현됨, R8·R9), 013 `PersonalDataPurgedEvent`, 018 `MemberPurgedEvent`·`MemberDisplay`·`User` 기본 조회(dev에 구현됨, R15). 프론트는 기존(TanStack Query, axios, zod, next-intl, shadcn). 더하는 의존성은 없다

**Storage**: MariaDB 11.4. 새 테이블 7개 `tb_user_public_id`, `tb_friend`, `tb_friend_request`, `tb_friend_rejection`, `tb_follow`, `tb_follow_request`, `tb_user_account_visibility`. 마이그레이션 파일 하나 `V<다음 번호>__friends_follow.sql`(R14). 자세한 것은 [data-model.md](./data-model.md)

**Testing**
- 테스트는 기능 코드를 모두 만든 뒤 PR 직전에 쓴다 (`CLAUDE.md`). 로컬은 `./gradlew build`, `npm run check`만, 인수 테스트와 e2e는 PR CI
- API 통합: 기존 `AcceptanceTest`(MockMvc + Testcontainers). AC마다 하나
- 동시성: US2-AC6(서로 동시에 요청)은 두 스레드로 동시에 보내 친구 한 쌍만 생기는지 본다
- 알림: 받는 사람에게 그 종류의 개인 알림이 하나 생기고 한 사람에게는 없는지 `tb_notification`으로 본다 (US6)
- 탈퇴: 유예(US5-AC5)는 018 탈퇴 API로 만들고, 파기(US5-AC6)는 018 파기 처리를 부른 뒤 관계 행과 `tb_purge_log`를 본다
- 프론트: Vitest(관계 단추 상태 표, 커서 이어 붙이기), Playwright e2e 2개(요청 → 수락 → 서로 팔로우, 비공개 계정의 팔로우 요청 → 수락)

**Target Platform**: 웹 (데스크톱·휴대폰 브라우저)

**Project Type**: 웹 서비스 (모노레포 `backend/`, `frontend/`)

**Performance Goals**: 프로필(수 셋 포함)과 목록 한 묶음(20개)이 1초 안에 보인다

**Constraints**
- 회원 순번을 주소에 드러내지 않는다 (FR-003)
- 로그인 아이디를 응답에 싣지 않는다 (`008-party` FR-027과 같다)
- 계층 규칙은 ArchitectureTest를 따른다. 009·007·018 도메인은 Application이 묶는다
- 비공개 계정의 활동은 서버가 막는다. 화면이 숨기는 것만으로 두지 않는다 (FR-018)

**Scale/Scope**
- 사용자 스토리 6개, AC 41개, 화면 4개(프로필, 친구 화면, 팔로워·팔로잉 목록, 설정의 계정 공개 여부 칸)
- REST API 21개, 개인 알림 5종

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| 원칙 | 판정 | 근거 |
|---|---|---|
| I. 스펙이 먼저다 | 통과 | spec.md에 이 기능의 `[NEEDS CLARIFICATION]`이 없다(2026-11-22, 11-27 답함). plan에서 정한 값(30일, 5,000명, 커서 크기)은 spec과 research에 있다 |
| II. AC마다 테스트 하나 | 통과 | AC 41개마다 테스트를 둔다. 이름에 AC 번호를 적는다. e2e는 핵심 흐름 2개만이다 |
| III. 키트를 따른다 | 통과 (Complexity Tracking) | 계층·엔터티·오류·페이지 규칙은 키트 골격을 따른다. 키트 user-admin의 `UserAdminService.create`에 공개 ID 만들기 한 줄을 더한다(R1). 010이 같은 서비스의 `rename`을 이미 고친 선례가 있다 |
| IV. 기존 기능은 제자리에서 고친다 | 통과 | 001 메뉴·연결 화면 표, 007 알림 종류, 008 회원 응답의 `publicId`, 009 `ProfileVisibility`의 친구 판단(R8), 013 `PurgeTarget`, `docs/privacy.md` 보관 기간 표와 "탈퇴 회원 파기"는 그 자리에서 고친다. 009의 "친구만"은 009가 이미 반영했다 |
| V. 테스트를 고쳐서 통과시키지 않는다 | 통과 | `AdminMenusAcceptanceTest`의 사용자 메뉴 기대값은 001 FR-007 표를 먼저 고친 뒤 맞춘다. `ProfileVisibilityTest`의 `FRIENDS` 기대값은 009 spec FR-013과 이 스펙 FR-015로 근거가 바뀐 뒤 맞춘다 |
| VI. 시크릿을 다루지 않는다 | 통과 | 새 비밀값이 없다. 시험 회원 `user2`의 비밀번호는 사람이 넣는다 |
| 기술 제약: DB 규칙 | 통과 | 테이블·열·인덱스 이름, COMMENT, 1:1 테이블의 PK·감사 열 이유는 data-model.md에 `docs/db.md`대로 적었다 |
| 기술 제약: 목록 종류 | 통과 | 친구·팔로워·팔로잉·받은 친구 요청·보낸 친구 요청·받은 팔로우 요청 모두 피드 목록(커서). 이유는 spec FR-011·FR-012와 research R5 |

Phase 1 설계 뒤 다시 확인했다(2026-11-27 고친 뒤 포함). 위반이 없다.

## Project Structure

### Documentation (this feature)

```text
specs/014-friends-follow/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── friends-api.md
├── checklists/requirements.md
└── tasks.md
```

함께 고치는 문서 (구현 PR 또는 그 전의 docs PR):
- `specs/001-admin-menus/spec.md`: FR-007·FR-008 표에 "친구"(회원만)
- `docs/privacy.md`: 보관 기간 표에 관계·요청·거절 기록·공개 ID·공개 여부 줄, "탈퇴 회원 파기"에 관계는 지운다는 예외 (research.md R11)
- `specs/008-party/contracts/party-api.md`: 회원 응답에 `publicId`
- `specs/015-game-shortform/spec.md`: 프로필 활동 칸의 숏폼 탭, 비공개 계정 숏폼의 공개 범위 (spec "다른 스펙에 반영할 곳")
- `specs/README.md`: 014 상태

### Source Code (repository root)

```text
backend/
├── src/main/java/com/moa/
│   ├── common/exception/ErrorCode.java                 # A301, B301~B309
│   ├── common/purgelog/PurgeTarget.java                # SOCIAL_RELATION 더하기 (013)
│   └── api/
│       ├── controller/v1/member/                       # ApiMemberProfileController (GET /users/{publicId}/profile)
│       ├── controller/v1/friend/                       # ApiFriendController, ApiFriendRequestController
│       ├── controller/v1/follow/                       # ApiFollowController, ApiFollowRequestController, ApiAccountVisibilityController
│       ├── application/v1/member/                      # MemberProfileApplication, MemberSummaryAssembler, ActivityVisibility
│       ├── application/v1/friend/                      # FriendApplication (요청·수락·거절·끊기, 알림)
│       ├── application/v1/follow/                      # FollowApplication (팔로우, 팔로우 요청, 공개 여부)
│       ├── application/v1/social/                      # SocialRelationPurgeListener (018 파기됨)
│       ├── service/friend/                             # FriendService, FriendRequestService, FriendRejectionService, FriendProperties
│       ├── service/follow/                             # FollowService, FollowRequestService, AccountVisibilityService
│       ├── service/social/                             # SocialRelationPurgeService (7개 테이블에서 한 회원 지우기)
│       ├── service/user/                               # UserPublicIdService. ProfileVisibility(009)에 친구 판단 인자를 더한다
│       ├── repository/{reader,writer,jpa}/{friend,follow,user}/
│       ├── entity/friend/                              # Friend, FriendRequest, FriendRejection
│       ├── entity/follow/                              # Follow, FollowRequest, UserAccountVisibility, AccountVisibility
│       ├── entity/user/UserPublicId.java
│       ├── dto/{friend,follow,member}/
│       ├── dto/notification/NotificationType.java      # 친구·팔로우 5종
│       ├── screen/FriendScreens.java                   # 연결 화면 FRIENDS
│       └── scheduler/SocialRelationCleaner.java        # 30일 지난 거절 기록, 파기된 회원의 남은 관계 지우기
├── src/main/resources/
│   ├── application.yaml                                # app.friend.rejection-cooldown, app.friend.max-friends
│   └── db/migration/V<다음 번호>__friends_follow.sql   # 테이블 7개 + 공개 ID 채우기 + 메뉴 시드 + COMMENT 둘
└── src/test/java/com/moa/
    ├── support/FriendFixtures.java                     # 회원·친구·팔로우·요청·공개 여부를 바로 넣는 도우미
    ├── api/AdminMenusAcceptanceTest.java               # 사용자 메뉴 기대값 (001 표 기준)
    └── api/friend/                                     # MemberProfileAcceptanceTest, FriendRequestAcceptanceTest,
                                                        #   FollowAcceptanceTest, PrivateAccountAcceptanceTest,
                                                        #   FriendManageAcceptanceTest, FriendNotificationAcceptanceTest

frontend/
├── messages/ko.json                                    # friend·follow·profile 키, errors의 A301·B301~B309
├── src/
│   ├── api/friend.ts, api/follow.ts, api/member.ts, api/index.ts
│   ├── lib/use-cursor-list.ts                          # 커서 피드 목록 공용 훅 (008 채팅도 쓸 수 있다)
│   ├── lib/relation.ts                                 # relation → 단추 고르기 (단위 테스트 대상)
│   ├── components/member/member-link.tsx               # 이름을 누르면 /users/{publicId} (008 화면도 쓴다)
│   ├── components/member/relation-buttons.tsx          # spec "프로필 페이지" 관계 단추 표
│   ├── components/member/profile-tabs.ts               # 활동 칸 탭 등록 목록 (015가 숏폼 탭을 더한다, R16)
│   ├── components/notification/realtime-toasts.tsx     # 기존(007). 관련 화면: 친구 화면의 요청 묶음, 상대 프로필
│   ├── app/(user)/(signed-in)/
│   │   ├── users/[publicId]/                           # 프로필, followers/, following/
│   │   └── friends/                                    # 친구 화면 (?tab=friends|received|sent|follow-requests)
│   └── app/(account)/settings/                         # 기존. 계정 공개 여부 칸을 더한다
├── tests/unit/friend-*.test.ts
└── e2e/friends.spec.ts
```

**Structure Decision**: 백엔드는 기존 `com.moa.api` 계층을 그대로 쓰고, 도메인을 `member`(프로필 읽기), `friend`, `follow`, `social`(두 도메인에 걸친 탈퇴 정리)로 나눈다. 프로필 읽기는 009의 프로필과 014의 관계를 묶으므로 Application(`MemberProfileApplication`)에서 조합한다. 활동을 볼 수 있는지(`ActivityVisibility`)는 친구와 팔로우 둘 다 보므로 Application 계층에 두고 015도 쓴다. 프론트는 사용자 셸의 로그인 필요 묶음 `(user)/(signed-in)`에 화면을 두고, 이름 링크(`member-link`)와 탭 등록(`profile-tabs`)은 다른 기능도 쓰는 공용 부품으로 둔다.

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| 키트 user-admin `UserAdminService.create`에 공개 ID 만들기를 더한다 (원칙 III) | 관리자가 만든 회원도 프로필 주소가 있어야 이름 링크가 깨지지 않는다 (R1) | 프로필을 처음 열 때 만들기: 목록·파티 화면이 링크를 만들 때 쓰기 트랜잭션이 필요해진다. 키트로 옮길 목록에 올린다 |
| 친구 한 쌍을 두 행으로 둔다 | 목록·수·여부가 한 열 조건이 된다 (R2) | 한 행 두 열: 모든 조회가 OR와 상대 고르기를 해야 한다 |
| 대기 친구 요청에 쌍 정규화 열(`pair_low_id`, `pair_high_id`)을 둔다 | 방향과 상관없이 쌍마다 하나를 DB가 막아 동시 요청을 락 없이 모은다 (R3) | 회원 행 락: 키트 `tb_user`를 락 대상으로 쓰고 교착을 피하려 순서를 맞춰야 한다 |
| 팔로우 요청을 `tb_follow`와 다른 테이블로 둔다 | 팔로워 목록·수·피드가 상태 조건 없이 늘 승인된 팔로우만 본다 (R6) | `tb_follow.status`: 조건 하나가 빠지면 비공개 계정의 승인 전 요청자가 팔로워로 보인다 |
| 파기 때 다른 회원과 엮인 관계 행도 지운다 (`docs/privacy.md` 기본 규칙의 예외) | 관계는 그 회원과의 관계 자체라 상대에게 쓸모가 없고, 남기면 회원 ID가 "누구와 친구였나"로 이어진다 (R15) | 남기기: "남은 회원 ID가 어떤 개인정보와도 이어지지 않아야 파기가 끝난 것이다"에 어긋난다 |
