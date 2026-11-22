---

description: "마이페이지 (mypage) 작업 목록"
---

# Tasks: 마이페이지 (mypage)

**Input**: `specs/009-mypage/`의 설계 문서

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/profile-api.md, contracts/users-me-and-screens.md, quickstart.md

**Tests**: 원칙 II에 따라 AC마다 테스트를 하나 둔다(AC 34개, 아래 Phase 9 "AC와 테스트" 표). CLAUDE.md에 따라 기능 코드(백엔드·프론트)를 먼저 끝내고 테스트 작업은 맨 끝(Phase 9)에 둔다. 로컬에서는 빠른 검사(`.\gradlew.bat build`, `npm run check`)만 돌리고, DB 테스트와 e2e는 PR CI로 본다.

## Format: `[ID] [P?] [Story?] [공용?] Description`

- **[P]**: 병렬 가능 (다른 파일, 의존 없음)
- **[Story]**: 작업이 속한 사용자 스토리 (US1~US6)
- **[공용]**: 다른 기능·키트·여러 기능이 함께 쓰는 파일을 고치는 작업. **구현 때 그 파일을 고치기 전에 바꿀 내용을 사용자에게 보고하고 확인받는다** (공용 작업 7개 — plan.md "공용 파일". 2026-11-19 T009·T030, 2026-11-22 T005가 빠짐)
- **(010 순서)**: `010-social-login`과 같은 파일·같은 판단을 고치는 작업. **2026-11-22: 010이 dev에 먼저 들어갔다(PR #67)**. 그래서 "009 먼저" 경우를 지우고 dev에 있는 010 코드에 맞춰 적었다 (research R11·R14)
  - 010이 dev에 만든 것: 010 마이그레이션(`*__social_login.sql`)의 `tb_user_profile`(최소형)과 기존 회원 채우기, 엔터티 `UserProfile`·`UserProfileReader`·`UserProfileWriter`, `NicknamePolicy`·`NicknameTags`·B201, 가입 때 프로필 행, 관리자 이름 바꾸기의 태그 맞추기, `/users/me`의 `hasPassword`, 계정 바(`account-bar.tsx`)의 아이디 줄·비밀번호 변경 숨김. 009는 이것들을 새로 만들지 않는다
  - 010이 만들지 않은 것: 관리자·운영자 판단(`MemberActivityPolicy`). 009가 만든다 (T011)
- **(잠정)**: 스펙 [NEEDS CLARIFICATION](닉네임 규칙, 변경 제한, 만 14세 미만, 나이·성별 공개 개별 여부, 친구·팔로워, 보기 범위)에 기댄 부분. 확정되면 research "열린 질문의 처리"의 자리만 고친다
- 경로는 저장소 루트 기준이다. 백엔드 패키지 `backend/src/main/java/com/moa/`는 `B/`로 줄여 쓴다

---

## Phase 1: Setup (번호·순서 확인, 공통코드 의존, 아바타 그림)

**Purpose**: 구현 직전에 겹칠 수 있는 번호와 010·008 병합 상태를 다시 확인해 "(010 순서)" 작업의 경우를 정한다

- [ ] T001 구현 직전 dev 최신을 다시 확인한다: `git fetch` 뒤 010 마이그레이션(`*__social_login.sql`)의 `tb_user_profile`이 `origin/dev`에 있는지 본다(2026-11-22 있음). 마이그레이션 번호는 지금 정하지 않는다: 파일 이름은 `V<다음 번호>__mypage.sql`이고 PR 직전에 dev를 병합해 마지막 번호 + 1로 정한다(`docs/db.md`, research R12). B/common/exception/ErrorCode.java에 `A201`·`B202`~`B204`가 비어 있는지 본다(`B201`은 010이 넣었다). 다르면 이 tasks.md, data-model.md, contracts/profile-api.md의 번호를 먼저 고친다
- [ ] T002 공통코드 의존을 확인한다 (research R13): (1) `PLAY_TIME`은 009 마이그레이션이 지금 dev의 `tb_code_group`·`tb_code`에 넣는다. 기다릴 것이 없다(시스템 그룹 구분과 004 Phase 6 의존은 2026-11-20에 없어졌다). (2) dev에 `GAME`, `LOL_TIER`·`LOL_POSITION`, `OVERWATCH_TIER`·`OVERWATCH_POSITION`, `VALORANT_TIER`·`VALORANT_POSITION`, `PUBG_TIER`(008 공통코드 시드)가 있는지 본다. **2026-11-22 없다**: 게임 정보(US5, T037~T040)는 008 시드가 dev에 들어간 뒤에 구현한다. US1~US4·US6은 기다리지 않는다. US5를 먼저 하려면 008 담당과 정한다: (a) 008 병합을 기다린다, (b) 008 시드를 009 PR로 먼저 가져온다(008 담당 동의 필요). 같은 그룹·항목을 009 마이그레이션에 다시 넣지 않는다
- [ ] T003 [P] 사람이 확인한다 (research R10, (잠정)): 기본 아바타 8개 키 `default`, `cat`, `dog`, `fox`, `bear`, `rabbit`, `penguin`, `robot`의 그림(SVG)을 정한다. 라이선스가 분명한 그림만 쓴다

**Checkpoint**: 번호, 010·008 순서, 아바타 그림이 정해졌다

---

## Phase 2: Foundational (데이터, 오류 코드, 규칙 클래스, 관리자·운영자 판단, 공용 프론트 준비)

**Purpose**: 모든 스토리가 쓰는 테이블·엔터티·오류 코드·닉네임 규칙·관리자 판단·프로필 행 만들기·API 틀

**⚠️ CRITICAL**: US1~US6 모두 이 단계에 기댄다

- [ ] T004 [공용] backend/src/main/resources/db/migration/V<다음 번호>__mypage.sql 한 파일을 만든다 (data-model.md, `docs/db.md`). 번호는 PR 직전 dev 마지막 + 1, 010의 `tb_user_profile` 다음. 머리 주석 `-- moa: 마이페이지 프로필 칸, 플레이 가능 시간대, 게임 정보, 공통코드 PLAY_TIME. 스펙 specs/009-mypage (FR-006~FR-012, data-model.md, research.md R12·R13)`. 차례대로:
  1. `ALTER TABLE tb_user_profile ADD COLUMN` 8개 — `avatar_key` VARCHAR(30) NOT NULL DEFAULT 'default', `bio` VARCHAR(500) NULL, `birth_year` SMALLINT NULL, `gender` VARCHAR(20) NULL, `age_gender_public` BOOLEAN NOT NULL DEFAULT FALSE, `discord_tag` VARCHAR(32) NULL, `discord_visibility` VARCHAR(20) NOT NULL DEFAULT 'PRIVATE', `nickname_changed_at` DATETIME(6) NULL. `DEFAULT`는 기존 행을 채우려고 NOT NULL 칸 셋(`avatar_key`, `age_gender_public`, `discord_visibility`)에만 둔다. 열 COMMENT는 data-model 표 그대로. 테이블 COMMENT(010)는 그대로 둔다
  2. `tb_user_profile_play_time`(`user_id` BIGINT NOT NULL, `play_time_code` VARCHAR(50) NOT NULL, PK `(user_id, play_time_code)`). 테이블 COMMENT `'플레이 가능 시간대 (PK: 컬렉션 테이블; 감사 열 없음: 프로필과 함께 저장)'`
  3. `tb_user_game_profile`(`id` BIGINT AUTO_INCREMENT PK, `user_id` BIGINT NOT NULL, `game_code` VARCHAR(50) NOT NULL, `tier_code` VARCHAR(50) NULL, `position_code` VARCHAR(50) NULL, `game_account_id` VARCHAR(50) NULL, `BaseEntity` 감사 열 `created_at`·`updated_at`·`created_by`·`updated_by`, `UNIQUE KEY uk_user_game_profile_user_id_game_code (user_id, game_code)`). `user_id` 인덱스는 따로 두지 않는다(유일 제약의 앞 열). 테이블 COMMENT `'회원 게임 정보'`
  4. 공통코드 `PLAY_TIME`: `tb_code_group`에 활성 `PLAY_TIME` 행이 없을 때만 그룹(이름 "플레이 가능 시간대", 설명 "마이페이지 플레이 가능 시간대 선택지 (009-mypage)", `enabled` 참, `sort_order` = 가장 큰 값 + 1)을, `tb_code`에 활성 행이 없는 항목만 `WEEKDAY_MORNING` 평일 오전, `WEEKDAY_EVENING` 평일 저녁, `WEEKEND_ALL_DAY` 주말 종일, `DAWN` 새벽(`sort_order` 0~3)을 넣는다. 고정 id를 쓰지 않고 있는 행은 고치지 않는다 (data-model "공통코드 시드", `004-admin-codes` FR-002)
  - 기존 회원 채우기는 010 마이그레이션이 했으므로 넣지 않는다 (research R14). 다 쓰면 CI와 같은 `bash scripts/check-migration-numbers.sh`를 돌리고, `bootRun`으로 Flyway 적용과 `ddl-auto: validate`를 본다. 구현 전에 바꿀 내용을 보고하고 확인받는다
- [ ] ~~T005~~ (2026-11-22 T004로 합침) `PLAY_TIME` 시드를 따로 `_1` 파일로 두지 않는다. 기능 하나의 스키마 변경과 그 데이터는 파일 하나다 (`docs/db.md`, research R12)
- [ ] T006 [P] B/api/entity/user/ 아래 엔터티·enum: `UserProfile`(010이 만든 엔터티(`@Table(name = "tb_user_profile")`, `BaseTimeEntity`)에 009 칸을 더한다. 010의 `changeNickname(key, tag)`는 그대로 두고 닉네임 바꾼 시각은 `markNicknameChanged(now)` 같은 메서드로 따로 적는다. 프로필 저장 `update(...)`. 009 필드 `avatarKey`·`ageGenderPublic`·`discordVisibility`는 필드 기본값을 `"default"`·`false`·`PRIVATE`로 둬서 010 가입(`new UserProfile(userId, key, tag)`)이 만든 행도 NOT NULL 칸이 비지 않게 한다 (data-model "프로필")), `UserGameProfile`(`@Table(name = "tb_user_game_profile")`, `BaseEntity` + `@HardDelete(reason = "회원이 지우면 행을 지운다. 개인정보라 남기지 않는다")`), 플레이 가능 시간대(`UserProfile`의 `@ElementCollection` + `@CollectionTable(name = "tb_user_profile_play_time", joinColumns = @JoinColumn(name = "user_id"))`, 저장은 "보낸 목록으로 바꾸기"), 엔터티에 `@Index`·`uniqueConstraints`는 적지 않는다(`docs/db.md` "엔터티"), enum `Gender`(`MALE`, `FEMALE`, `UNDISCLOSED`), `DiscordVisibility`(`PRIVATE`, `FRIENDS`, `MEMBERS`), `DefaultAvatar`(`default`, `cat`, `dog`, `fox`, `bear`, `rabbit`, `penguin`, `robot` 순서, 키 ↔ enum, 모르는 키는 `default`로) (data-model, research R6·R7·R10)
- [ ] T007 [P] B/api/repository/{jpa,reader,writer}/user/ 아래: `UserProfileReader`(010이 만든 것에 `findForUpdate(userId)` = `SELECT … FOR UPDATE`가 없으면 더한다), `UserProfileWriter`(010이 만든 것을 쓴다), `UserGameProfileReader`(`findAllByUserId` `created_at` 순, `find(userId, gameCode)`, `countByUserId`), `UserGameProfileWriter`(저장, 지우기). 기존 user reader/writer 모양대로
- [ ] T008 [P] [공용] B/common/exception/ErrorCode.java에 contracts/profile-api.md 표의 코드 4개를 더한다(`NICKNAME_TAGS_EXHAUSTED`(B201)는 010이 넣었다): `MYPAGE_NOT_ALLOWED`(A201, 403), `NICKNAME_CHANGE_TOO_SOON`(B202, 409), `GAME_PROFILE_LIMIT`(B203, 409), `GAME_PROFILE_DUPLICATED`(B204, 409). 번호는 T001 결과를 따른다. 구현 전에 바꿀 내용을 보고하고 확인받는다
- [ ] ~~T009~~ (2026-11-19 없앰) 닉네임 중복 조회는 두지 않는다. 닉네임은 겹쳐도 되고 구별은 태그가 한다 (research R4). `UserReader`·`UserRepository`는 고치지 않는다
- [ ] T010 [P] B/api/service/user/ProfileProperties.java(`@ConfigurationProperties("app.profile")`, `nicknameCooldown` `Duration` 기본 `30d`) (research R5, (잠정) FR-004). application.yaml은 고치지 않는다(기본값으로 충분)
- [ ] T011 B/api/service/user/MemberActivityPolicy.java `isManagerAccount(userId)`: 시스템 역할이 `ADMIN`이면 참 (research R2. 회원(`USER`)은 업무 역할을 받지 않으므로 "`ADMIN`이거나 업무 역할을 가진 계정"과 같다, `003-admin-roles` FR-002). 010은 이 판단을 만들지 않았으므로 새로 만든다. 010 US5가 `socialLinkAllowed`를 더할 때 이것을 쓰도록 PR 설명에 적는다
- [ ] T012 [P] `NicknamePolicy`·`NicknameTags`는 010이 B/api/service/user/에 만들었다 (research R4). 009는 새로 만들지 않고 그대로 쓴다. 규칙이 이 기능에 모자라면 그 클래스를 고치고 010 테스트가 그대로 통과하는지 본다
- [ ] T013 [P] B/api/service/user/ProfileVisibility.java (잠정, FR-013): `view(profile, viewer)` — 로그인하지 않으면 없음, 나이·성별은 `age_gender_public`일 때만, 디스코드 태그는 `MEMBERS`일 때만(`FRIENDS`는 친구 기능 전까지 아무에게도 안 보임), 나머지는 모두. 이 규칙을 쓰는 API·화면은 만들지 않는다 (research R9)
- [ ] T014 [P] B/api/service/user/GameStatsLinks.java: 게임 코드별 주소 틀(잠정, 구현 때 주소를 다시 확인): `LOL` `https://www.op.gg/summoners/kr/{id}`(`#` → `-`), `VALORANT` `https://tracker.gg/valorant/profile/riot/{id}/overview`(`#` 인코딩), `OVERWATCH` `https://www.overbuff.com/players/{id}`(`#` → `-`), `PUBG` `https://pubg.op.gg/user/{id}`. 게임 아이디는 URL 인코딩. 틀이 없거나 아이디가 비면 `null` (research R8)
- [ ] T015 B/api/service/user/ProfileService.java 틀: `getMyProfile(userId)` — `nickname`·`nicknameTag`를 함께 준다. 행이 없으면 기본값(`nicknameTag` null, 아바타 `default`, 빈 값, `ageGenderPublic` 거짓, `discordVisibility` `PRIVATE`), `age` = 올해 − `birthYear`(값이 있을 때만), 목록에서 빠진 아바타 키는 `default`, `nicknameChangeableAt` = `nickname_changed_at` + 쿨다운(지금보다 뒤일 때만, 아니면 null). `requireProfileForUpdate(userId)` — `findForUpdate`, 없으면 `nickname_key` = `NicknamePolicy.key(tb_user.name)`, `nickname_tag` = `NicknameTags.assign(key)`로 만든 뒤 잠근다(research R8·R14 "없으면 만든 뒤"). 처음 진입(GET)에서는 행을 만들지 않는다
- [ ] T016 B/api/application/v1/user/ProfileApplication.java와 B/api/controller/v1/user/ApiMyProfileController.java 틀: 모든 마이페이지 API(프로필·닉네임·게임 정보) 앞에서 `MemberActivityPolicy.isManagerAccount`가 참이면 403 `MYPAGE_NOT_ALLOWED`(A201) (FR-016, research R2). `GET /api/v1/profile/avatars`는 막지 않는다. `SecurityConfig`는 고치지 않는다(로그인 필요는 기본 규칙)
- [ ] T017 [P] frontend/src/types/profile.ts(새: `MyProfile`, `GameProfiles`, 요청 타입 — contracts/profile-api.md 모양), frontend/src/api/index.ts에 `api.v1.profile.get()`, `save(body)`, `changeNickname(nickname)`, `addGame`, `updateGame`, `deleteGame`, `avatars()`를 더한다 (타입은 T044의 `npm run gen:api` 뒤 정리)
- [ ] T018 [P] frontend/src/lib/avatars.ts(새: 키 → `/avatars/<키>.svg`, 모르는 키는 `default`)와 frontend/public/avatars/<키>.svg 8개(T003 그림)
- [ ] T019 [P] [공용] frontend/src/lib/next-path.ts에 `PROFILE_PATH = "/account/profile"`을 `PASSWORD_PATH`·`SETTINGS_PATH` 옆에 더한다. 키트 web-next 파일이다. 구현 전에 바꿀 내용을 보고하고 확인받는다
- [ ] T020 [공용] frontend/messages/ko.json에 새 `profile` 묶음(화면 제목, 칸 라벨, 필수·선택 표시, 닉네임 바꾸기 단추·"{date}부터 바꿀 수 있어요"·태그가 다 찬 닉네임·규칙 안내, 나이 표시, 성별 3개, 공개 토글, 디스코드 공개 범위 3개·형식 안내, 시간대 제목, 게임 정보 제목·"{count}/{max}"·추가 단추·모달 제목·"랭크는 직접 입력한 값이라 확인되지 않은 값이에요"·고치기·지우기·전적 링크, 아바타 고르기, 저장 안내)과 `shell.mypage`("마이페이지")를 더한다. 문구 검사(`npm run check`) 규칙을 따른다. 구현 전에 바꿀 내용을 보고하고 확인받는다

**Checkpoint**: 테이블·규칙·관리자 판단·API 틀·공용 프론트 준비가 끝났다

---

## Phase 3: User Story 1 - 내 프로필을 본다 (Priority: P1) 🎯 MVP

**Goal**: 회원이 계정 바 톱니 메뉴의 "마이페이지"로 `/account/profile`에 들어가 자기 프로필을 본다. 계정 바는 닉네임·아바타를 보이고 회원의 로그인 아이디를 숨긴다. 관리자·운영자는 항목이 없고 주소로 들어와도 막힌다

**Independent Test**: quickstart 1~3·22 — 계정 바에 닉네임·기본 아바타, 톱니 메뉴에 "마이페이지"가 보이고, 마이페이지의 모든 항목이 비어 있음으로 보이며, `admin`은 항목이 없고 권한 없음 화면이다

- [ ] T021 [US1] ApiMyProfileController에 `GET /api/v1/users/me/profile` → `ProfileService.getMyProfile` (contracts/profile-api.md GET 모양: `nickname`, `avatar`, `bio`, `birthYear`, `age`, `gender`, `ageGenderPublic`, `discordTag`, `discordVisibility`, `playTimes[{code,name}]`, `nicknameChangeableAt`, `games{count,max,items}`) (US1-AC1·AC4). 꺼진 공통코드 값도 이름과 함께 보인다(`includeDisabled`로 이름 찾기)
- [ ] T022 [US1] [공용] B/api/controller/v1/user/response/UserGetResponse.java(과 그 값을 만드는 서비스·`UserDto`)에 `avatar`(string, 행이 없거나 목록에서 빠진 키면 `default`)와 `mypageAllowed`(boolean, `!MemberActivityPolicy.isManagerAccount`)를 더한다 (contracts/users-me-and-screens.md, FR-014·FR-016). 010이 더한 `hasPassword`는 그대로 둔다. 키트 골격 파일이다. 구현 전에 바꿀 내용을 보고하고 확인받는다
- [ ] T023 [P] [US1] [공용] frontend/src/types/user.ts `Me`에 `avatar`, `mypageAllowed`를 더한다. 010이 더한 `hasPassword`는 그대로 둔다. 테스트·e2e의 `Me` 모의 값(`e2e/support/api.ts`의 `E2E_ME` 등)도 함께 채운다. 키트 web-next 파일이다. 구현 전에 바꿀 내용을 보고하고 확인받는다
- [ ] T024 [US1] [공용] frontend/src/components/shell/account-bar.tsx(사이드바 바닥 계정 바, `001-admin-menus` FR-005. dev가 키트 `account-menu.tsx`를 바꾼 파일): 사람 아이콘 자리에 아바타(`lib/avatars.ts`), 이름 자리에 닉네임(`name`). 회원(`mypageAllowed`)에게는 아이디 줄(툴팁 포함)을 숨긴다. 톱니 메뉴의 설정 앞에 `mypageAllowed`일 때만 "마이페이지"(`PROFILE_PATH`)를 둔다: 화면 모드, (언어), 마이페이지, 설정, 비밀번호 변경. 로그아웃은 따로 단추다. 010의 "`hasPassword`가 참이 아니면 아이디 줄·비밀번호 변경 숨김"은 그대로 둔다 (FR-014, US1-AC3·AC6, research R11, contracts/users-me-and-screens.md). 구현 전에 바꿀 내용을 보고하고 확인받는다
- [ ] T025 [US1] frontend/src/app/(account)/account/profile/page.tsx(새): `(account)` 묶음의 `AuthGate`·`AccountShell`을 그대로 쓴다(US1-AC2는 따로 만들지 않음, research R1). `useMe()`의 `mypageAllowed`가 거짓이면 기존 권한 없음 화면(`001-admin-menus` FR-003)을 보이고 API를 부르지 않는다(US1-AC7). 아니면 `api.v1.profile.get()`으로 프로필 편집 화면을 보인다. 로그인 방식·소셜 연결 영역은 두지 않는다(US1-AC5)
- [ ] T026 [US1] frontend/src/components/profile/profile-form.tsx(새): 아바타, 닉네임(필수 표시), 자기소개·출생 연도(옆에 계산한 나이)·성별·"나이·성별 공개" 토글 하나·디스코드 태그와 공개 범위·플레이 가능 시간대(모두 선택 표시), 게임 정보 영역 자리. 비어 있는 항목은 비어 있음으로 보인다 (FR-001, FR-017, US1-AC1·AC4·AC8). 각 칸의 저장 동작은 US2~US6에서 더한다

**Checkpoint**: 계정 바와 마이페이지로 내 프로필을 본다 (MVP 첫 조각)

---

## Phase 4: User Story 2 - 닉네임을 바꾼다 (Priority: P1)

**Goal**: 회원이 닉네임을 규칙·변경 제한을 지켜 바꾸고, 계정 바와 다른 기능의 표시 이름이 바로 바뀐다. 닉네임은 겹쳐도 되고 태그는 비어 있으면 그대로, 아니면 새로 받는다

**Independent Test**: quickstart 4~8 — 닉네임을 바꾸면 계정 바가 바로 바뀌고, 바로 다시 바꾸면 "언제부터"가 보이며, 규칙이 막히고, 다른 회원과 같은 닉네임은 다른 태그로 저장된다

- [ ] T027 [US2] B/api/service/user/NicknameService.java `change(userId, raw)`: 한 트랜잭션에서 `NicknamePolicy.normalize` → 지금 이름과 같으면 아무것도 바꾸지 않고 돌려준다(시각 그대로, Edge Cases) → `validate`(V001) → `ProfileService.requireProfileForUpdate` → `nickname_changed_at`이 있고 + 쿨다운 전이면 B202(오류에 `data` 없음, 첫 변경은 바로, US2-AC6) → `NicknameTags.keepOrAssign(newKey, currentTag)`(다 차면 B201, US2-AC7·AC8) → `User.rename(nickname)`, `UserProfile.changeNickname(key, tag, now)` → flush. `uk_user_profile_nickname_key_nickname_tag` 위반은 010 `NicknameTags`의 다시 하기 규칙(새 트랜잭션, 최대 3번, 그래도면 B201)을 따른다 (research R4·R5, FR-003·FR-004)
- [ ] T028 [US2] ApiMyProfileController에 `PUT /api/v1/users/me/nickname` 요청 `{ nickname }` → 200 `{ nickname, nicknameTag, nicknameChangeableAt }`. B202 오류에는 `data`를 싣지 않는다(키트 `BusinessException`·`ErrorResponse`를 고치지 않음) (contracts "닉네임 바꾸기", US2-AC1·AC3~AC6)
- [ ] ~~T029~~ (2026-11-19 없앰) 가입 때 프로필 행(`nickname_key`·`nickname_tag`) 만들기는 010 가입이 한다 (research R14)
- [ ] ~~T030~~ (2026-11-19 010으로 옮김) `UserAdminService.rename`의 `nickname_key`·태그 맞추기는 010이 한다 (research R14, `010-social-login` tasks)
- [ ] T031 [US2] frontend/src/components/profile/nickname-field.tsx(새): zod로 길이 2~12자·한글·영문·숫자·`_`만 먼저 막고(잠정), 예약어·태그 다 참(B201)·변경 제한(B202)은 서버 응답을 칸 아래에 보인다. 마이페이지 머리에 `닉네임#태그`를 보인다(US1-AC1, 태그가 `null`이면 닉네임만). `nicknameChangeableAt`이 있으면 "{date}부터 바꿀 수 있어요"를 미리 보이고 입력·단추를 막는다. B202를 받으면(동시 저장·우회) `profile`을 다시 읽어 같은 안내를 보인다. 따로 "닉네임 바꾸기" 단추로 저장하고, 성공하면 저장 안내 뒤 `me`·`profile` 쿼리를 무효화해 계정 바를 새로고침 없이 바꾼다 (FR-005, SC-002, US2-AC1·AC3~AC5)

**Checkpoint**: 닉네임을 바꾸면 모든 화면에 새 닉네임이 보인다 (MVP)

---

## Phase 5: User Story 3 - 자기소개, 나이·성별, 디스코드 태그 (Priority: P2)

**Goal**: 자기소개·출생 연도·성별·공개 토글·디스코드 태그·공개 범위를 저장하고 지운다

**Independent Test**: quickstart 9~12

- [ ] T032 [US3] ProfileService `save(userId, request)`(닉네임·게임 정보 제외): `requireProfileForUpdate` 뒤 모든 칸을 보낸 값으로 바꾼다(`null`·빈 배열은 "비움", US3-AC5). 검증(V001 칸 오류): `bio` 500자 이내, `birthYear` 1900~올해(미래 연도 불가, (잠정) 만 14세 미만은 막지 않음), `gender`는 `Gender` 값, `discordTag`는 앞뒤 공백만 지우고 `^(?!.*\.\.)[a-z0-9_.]{2,32}$`(대문자는 형식 오류, 소문자로 바꾸지 않음), `discordVisibility`는 `DiscordVisibility` 값이고 보내지 않으면 `PRIVATE` (FR-006~FR-008, research R6·R7, US3-AC1~AC5). 응답은 GET과 같은 모양
- [ ] T033 [US3] ApiMyProfileController에 `PUT /api/v1/users/me/profile`과 요청 DTO(B/api/controller/v1/user/request/ProfileSaveRequest.java: `avatar`, `bio`, `birthYear`, `gender`, `ageGenderPublic`, `discordTag`, `discordVisibility`, `playTimes`)를 더한다. 길이·형식은 Bean Validation, 목록·연도 범위는 서비스 (contracts "프로필 저장")
- [ ] T034 [US3] frontend profile-form.tsx에 자기소개(500자 카운터), 출생 연도(옆에 나이), 성별(남성·여성·밝히지 않음), "나이·성별 공개" 토글 하나(잠정), 디스코드 태그와 공개 범위(비공개·친구·팔로워만·전체 회원, 기본 비공개)를 넣고 "저장" 단추로 `api.v1.profile.save`. 서버 V001 `errors[].field`를 그 칸에 보인다. 성공하면 `profile`·`me` 쿼리 무효화 (US3-AC1~AC5)

**Checkpoint**: 소개·나이·성별·디스코드를 적고 지운다

---

## Phase 6: User Story 4 - 기본 아바타를 고른다 (Priority: P2)

**Goal**: moa의 기본 아바타 목록에서 하나를 골라 저장하고 마이페이지·계정 바에 보인다

**Independent Test**: quickstart 13·14

- [ ] T035 [US4] ApiMyProfileController(또는 같은 패키지의 작은 컨트롤러)에 `GET /api/v1/profile/avatars` → `{ avatars: [...] }`(`DefaultAvatar` 순서). ProfileService `save`의 `avatar`는 `DefaultAvatar` 키만 받고 목록 밖이면 V001 (FR-009, US4-AC2, research R10). 이미지 올리기 API는 만들지 않는다
- [ ] T036 [US4] frontend/src/components/profile/avatar-picker.tsx(새): "고르기" 단추 → `api.v1.profile.avatars()` 목록의 그림 중 하나를 고른다. 올리기·주소 입력은 없다. 프로필 저장(T034)과 함께 보내고, 저장되면 마이페이지와 계정 바(T024)에 새 아바타가 보인다 (US4-AC1·AC2)

**Checkpoint**: 아바타를 바꾼다

---

## Phase 7: User Story 5 - 게임 정보를 적는다 (Priority: P2)

**Goal**: 게임정보 추가 모달로 게임을 최대 3개, 게임마다 하나 더하고, 고치고, 지운다. 전적 사이트 링크를 본다. **008 공통코드 시드(`GAME`·`<게임>_TIER`·`<게임>_POSITION`)가 dev에 들어간 뒤에 한다** (2026-11-22 dev에 없음, T002, research R13)

**Independent Test**: quickstart 15~20

- [ ] T037 [US5] B/api/service/user/GameProfileService.java: `add(userId, request)` — `ProfileService.requireProfileForUpdate`로 프로필 행을 잠근 뒤(동시 추가 막기) 같은 게임이 있으면 B204, 개수가 3이면 B203. 검증(V001): `gameCode`는 켜진 `GAME` 코드, `tierCode`는 켜진 `<gameCode>_TIER`, `positionCode`는 켜진 `<gameCode>_POSITION`(그룹이 없는 게임 `PUBG`면 반드시 null), `gameAccountId` 50자. 공통코드는 B/api/service/code/CodeService.java로 본다. `update(userId, gameCode, request)` — 게임은 바꾸지 않고 랭크·포지션·아이디만, 같은 검증(꺼진 코드로 남은 값을 그대로 보내면 V001), 없으면 404 R001. `delete(userId, gameCode)` — 없어도 그대로. 응답 `games{count, max: 3, items[]}`, 항목마다 코드 이름(`includeDisabled`)과 `GameStatsLinks`의 `statsUrl` (FR-010·FR-011, research R8, US5-AC1~AC7·AC9)
- [ ] T038 [US5] ApiMyProfileController에 `POST /api/v1/users/me/games`(201), `PUT /api/v1/users/me/games/{gameCode}`(200), `DELETE /api/v1/users/me/games/{gameCode}`(204)와 요청 DTO를 더한다 (contracts)
- [ ] T039 [P] [US5] frontend/src/components/profile/game-profile-list.tsx(새): 제목 옆 "{count}/{max}", 게임마다 게임·랭크·포지션·게임 아이디(적은 그대로)와 `statsUrl`이 있을 때만 전적 사이트 링크(`target="_blank"`, `rel="noopener noreferrer"`), 고치기·지우기, "게임정보 추가" 단추(3/3이면 꺼짐) (US5-AC6·AC7·AC9)
- [ ] T040 [P] [US5] frontend/src/components/profile/game-profile-dialog.tsx(새): 게임 선택(`lib/codes.ts`로 `GAME`, 이미 있는 게임은 뺌) → 그 게임의 `<게임>_TIER`·`<게임>_POSITION`(그룹이 없으면 포지션 칸 없음), 게임 아이디(50자). 안내 문구 "랭크는 직접 입력한 값이라 확인되지 않은 값이에요". 고치기도 같은 모달(게임 고정). B203·B204는 모달 안 안내. 성공하면 `profile` 쿼리 무효화 (US5-AC1~AC5·AC8)

**Checkpoint**: 게임 정보 셋까지 적고 전적 링크를 연다

---

## Phase 8: User Story 6 - 플레이 가능 시간대를 정한다 (Priority: P3)

**Goal**: 시간대를 여러 개 골라 저장하고 모두 풀 수 있다

**Independent Test**: quickstart 21

- [ ] T041 [US6] ProfileService `save`의 `playTimes`: 켜진 `PLAY_TIME` 코드만 받고(아니면 V001), 겹치면 하나로, 보낸 목록으로 바꾼다(빈 배열이면 모두 지움). 응답은 `{code, name}` 목록이고 이미 저장된 꺼진 코드도 이름과 함께 보인다 (FR-012, US6-AC1·AC2, data-model)
- [ ] T042 [US6] frontend profile-form.tsx에 `PLAY_TIME` 여러 개 선택(`lib/codes.ts`)을 넣어 프로필 저장(T034)에 함께 보낸다 (US6-AC1·AC2)

**Checkpoint**: 모든 스토리가 동작한다

---

## Phase 9: Polish & Cross-Cutting

- [ ] T043 backend/에서 `.\gradlew.bat spotlessApply`, `.\gradlew.bat build` (DB 테스트 제외)
- [ ] T044 frontend/에서 `npm run gen:api`(local 백엔드 실행 중)로 `src/types/schema.d.ts`를 다시 만들고, T017·T023의 손으로 쓴 타입을 생성 타입으로 바꾼 뒤 `npm run format`, `npm run check`
- [ ] T045 specs/009-mypage/quickstart.md "수동 확인" 1~22를 사람이 직접 수행한다. 8번(파티 멤버 목록)은 008 구현 뒤에 한다
- [ ] T046 spec "나중에 고칠 곳" 목록(001 FR-005, 002 FR-004·FR-007 — 이름 바꾸기의 태그 맞추기와 `닉네임#태그` 표시 포함, 004 FR-001 표에 `PLAY_TIME`, 008 FR-027·가정, 친구·팔로우 기능, README 경계 줄)을 PR 설명에 적어 각 담당에게 알린다 (사람이 한다. 이 기능에서 그 스펙들을 고치지 않는다)
- [ ] T047 PR 설명에 010과 겹친 작업(T011·T022·T023·T024)을 dev의 010 코드 위에 어떻게 더했는지 적는다(`MemberActivityPolicy`는 009가 새로 만들었고 010 US5가 쓴다). 키트로 옮길 것도 적는다: 계정 바의 아바타·마이페이지 항목(T024), `UserGetResponse`·`Me` 칸(T022·T023) (원칙 III)

---

## Phase 10: Tests (맨 끝, CLAUDE.md)

**Purpose**: AC마다 테스트 하나 (원칙 II), 이름에 AC 번호(`@DisplayName("US1-AC1 …")`, 프론트는 `it("US1-AC3 …")`)

### AC와 테스트

| AC | 테스트 | 작업 |
|---|---|---|
| US1-AC1 내 프로필 항목·빈 값 | 인수 | T049 |
| US1-AC2 로그인 없이 들어오면 로그인 뒤 돌아옴 | e2e | T053 |
| US1-AC3 계정 바에 닉네임, 아이디 없음 | 프론트 단위 | T052 |
| US1-AC4 기본 아바타 (`profile`, `/users/me`) | 인수 | T049 |
| US1-AC5 로그인 방식·소셜 연결 없음 | 프론트 단위 | T052 |
| US1-AC6 관리자·운영자 메뉴 항목 없음 | 인수(`mypageAllowed` 거짓) + 프론트 단위(항목 숨김) | T049, T052 |
| US1-AC7 관리자·운영자 주소로 들어오면 권한 없음 | 인수(모든 API 403 A201) + 프론트 단위(권한 없음 화면) | T049, T052 |
| US1-AC8 필수·선택 표시 | 프론트 단위 | T052 |
| US2-AC1 닉네임 바꾸기 | 인수 + e2e(계정 바 즉시 반영) | T049, T053 |
| US2-AC2 파티 멤버 목록의 새 닉네임 | 인수 (008 구현 뒤, 아니면 `/users/me`의 `name`으로 확인하고 PR에 적음) | T049 |
| US2-AC3 규칙 위반(빈 값·공백·1자·13자·글자·예약어) | 인수 + 프론트 단위(zod) | T049, T052 |
| US2-AC4 30일 안 다시 바꾸기 → B202, GET의 `nicknameChangeableAt` | 인수 + 프론트 단위("{date}부터") | T049, T052 |
| US2-AC5 같은 닉네임(대소문자만 다른 것 포함) 저장, 다른 태그 | 인수 | T049 |
| US2-AC6 첫 변경은 바로 | 인수 | T049 |
| US2-AC7 바꿀 때 태그 그대로 / 쓰이면 새 태그 | 인수 | T049 |
| US2-AC8 태그 다 찬 닉네임 B201 | 인수 (그 `nickname_key`에 9999행을 직접 넣어 둠) | T049 |
| US3-AC1 자기소개 500자 | 인수 | T049 |
| US3-AC2 출생 연도·성별·공개 토글, 미래 연도 | 인수 | T049 |
| US3-AC3 디스코드 태그·공개 범위, 기본 비공개 | 인수 | T049 |
| US3-AC4 디스코드 형식 오류 | 인수 | T049 |
| US3-AC5 값 지우기 | 인수 | T049 |
| US4-AC1 아바타 바꾸기 (`profile`, `/users/me`) | 인수 | T049 |
| US4-AC2 목록 밖 아바타 | 인수 | T049 |
| US5-AC1 게임 정보 더하기 | 인수 | T049 |
| US5-AC2 넷째 게임 B203 | 인수 | T049 |
| US5-AC3 같은 게임 B204 | 인수 | T049 |
| US5-AC4 포지션 없는 게임 | 인수(`PUBG`에 포지션 → V001) + 프론트 단위(칸 없음) | T049, T052 |
| US5-AC5 목록에 없는 랭크·포지션 | 인수 | T049 |
| US5-AC6 전적 사이트 링크 | 인수(`statsUrl`, 틀 없는 게임 null) | T049 |
| US5-AC7 게임 정보 지우기 | 인수 | T049 |
| US5-AC8 랭크 미확인 안내 | 프론트 단위 | T052 |
| US5-AC9 "1/3", 3/3이면 단추 꺼짐 | 인수(`count`·`max`) + 프론트 단위 | T049, T052 |
| US6-AC1 시간대 고르기 | 인수 | T049 |
| US6-AC2 시간대 모두 풀기 | 인수 | T049 |

### 작업

- [ ] T048 backend/src/test/java/com/moa/support/ 테스트 지원: `DatabaseCleaner`가 모든 테이블을 비워(`TRUNCATE`) 마이그레이션의 공통코드 시드가 남지 않으므로, `tb_code_group`·`tb_code`에 `PLAY_TIME` 그룹과 항목 4개를 넣는 지원 코드(US5를 할 때는 `GAME`·`<게임>_TIER`·`<게임>_POSITION`도. 008이 같은 지원 코드를 먼저 두면 거기에 더한다. 004가 지운 `TestCodeGroups`에 기대지 않는다)와 관리자 계정(`ADMIN`, 최고 관리자는 `AcceptanceTest.superAdmin()`) 회원을 만드는 도우미 (research R13)
- [ ] T049 backend/src/test/java/com/moa/api/MyPageAcceptanceTest.java(`AcceptanceTest` 상속, `@Tag("db")`): 위 표의 "인수" AC 29개를 `@DisplayName("US1-AC1 …")`으로 하나씩 둔다. Edge Case도 하나씩 둔다: 같은 닉네임으로 저장하면 `nickname_changed_at`이 그대로, 대소문자만 바꾸면 태그 그대로, 프로필 행이 없는 회원이 처음 저장하면 행과 태그가 생김, 프로필 행이 없는 회원이 동시에 두 번 처음 저장해도 둘 다 성공하고 행은 하나(PK 충돌 다시 하기, research R14. 3번 다 실패하면 R002), 프로필 행이 없는 회원의 GET은 기본값이고 행을 만들지 않음, 꺼진 공통코드 값이 이름과 함께 보임, 목록에서 빠진 아바타 키는 `default`, 로그인 없이 401 A001
- [ ] T050 [P] backend 단위 테스트(DB 없음): `NicknamePolicyTest`는 010이 둔다(009가 규칙을 고쳤을 때만 더한다), `ProfileVisibilityTest`(로그인 없음·나이·성별 공개·디스코드 `PRIVATE`/`FRIENDS`/`MEMBERS`), `GameStatsLinksTest`(게임별 틀, `#` 처리, 공백 인코딩, 틀 없는 게임 null)
- [ ] T051 [P] 기존 인수 테스트가 깨지지 않는지 보고 필요한 것만 더한다: `UserAccountAcceptanceTest`(`/users/me`에 `avatar`·`mypageAllowed` 칸, 010의 `hasPassword`와 함께), `AdminUsersAcceptanceTest`(이름 바꾸기의 태그 맞추기는 010 테스트가 본다). 깨지면 원칙 V대로 스펙부터 확인한다
- [ ] T052 [P] frontend 단위 테스트: 위 표의 "프론트 단위" AC 10개 — account-bar(`tests/unit/account-bar.dom.test.tsx`에 더한다. US1-AC3 닉네임·아바타 보임·회원은 `loginId` 없음, US1-AC6 `mypageAllowed` 거짓이면 톱니 메뉴에 마이페이지 없음, 010의 `hasPassword` 숨김 테스트도 그대로 통과), profile page(US1-AC5 소셜 영역 없음, US1-AC7 권한 없음 화면, US1-AC8 필수·선택 표시), nickname-field(US2-AC3 zod 1자·13자·허용되지 않는 글자, US2-AC4 "{date}부터"·단추 꺼짐), game-profile-dialog(US5-AC4 `PUBG` 포지션 칸 없음, US5-AC8 안내 문구), game-profile-list(US5-AC9 "1/3"·"3/3" 단추 꺼짐)
- [ ] T053 [P] frontend/e2e/mypage.spec.ts: 핵심 흐름만 — US1-AC2(로그아웃 상태로 `/account/profile` → 로그인 → 마이페이지), US2-AC1(닉네임 바꾸기 → 계정 바에 새로고침 없이 반영) (원칙 II "e2e는 핵심 흐름만")
- [ ] T054 로컬에서는 `.\gradlew.bat build`, `npm run check`만 돌린다. `dbTest`와 e2e는 PR CI로 확인하고, 사용자가 요청할 때만 로컬에서 `.\gradlew.bat dbTest --tests "*MyPageAcceptanceTest"`를 돌린다

---

## Dependencies & Execution Order

- Phase 1 → Phase 2 → 스토리. T001은 모든 작업보다 먼저이고, T001이 010·오류 코드를, T002가 공통코드 의존(US5가 008 시드를 기다리는지)과 T048을 정한다
- Phase 2 안: T004 → T006·T007 (테이블 → 엔터티), T012 → T015 (규칙 → 프로필 행 만들기), T011 → T016 (판단 → API 틀), T003 → T018 (그림)
- US1(Phase 3)과 US2(Phase 4)가 MVP. US2는 T015·T016·T026 뒤
- US3(Phase 5), US4(Phase 6), US6(Phase 8)은 같은 `PUT /users/me/profile`(T032·T033)과 같은 폼(T034)을 쓰므로 US3 → US4 → US6 순서가 편하다. US5(Phase 7)는 US1 뒤 따로 할 수 있다
- T024(계정 바)는 T022·T023 뒤
- Phase 9 → Phase 10(테스트) 순서
- [공용] 작업 7개(T004·T008·T019·T020·T022·T023·T024, 2026-11-22 T005는 T004로 합쳐 빠짐. T009·T030은 2026-11-19에 빠짐)는 착수 전 보고·확인이 끝나야 시작한다. 보고는 묶어서 한 번에 해도 된다. 공용 파일은 plan.md "공용 파일" 표
- 010과 겹친 작업 4개: T011(010이 만들지 않아 009가 새로)·T022·T023·T024(dev의 010 변경을 지키고 더함). 010이 이미 한 T009·T029·T030은 없앴다

## Parallel Example

```text
Phase 2 안: T006, T007, T008, T010, T012, T013, T014, T017, T018, T019 (서로 다른 파일)
US1 안: T023 (프론트 타입)와 T021·T022 (백엔드)
US5 안: T039, T040 (프론트 목록·모달) — 백엔드 T037·T038과 동시에
Phase 10 안: T050, T051, T052, T053
```

## Implementation Strategy

1. **MVP**: Phase 1 → 2 → 3(US1) → 4(US2). 내 프로필 보기와 닉네임 바꾸기, 계정 바의 닉네임 표시만으로 배포 검토 가능
2. US3(소개·나이·성별·디스코드) → US4(아바타) → US5(게임 정보) → US6(시간대)
3. Polish → 테스트(맨 끝) → PR. 잠정 작업(닉네임 규칙·예약어, 변경 제한 30일, 만 14세 미만, 나이·성별 공개 개별, 친구·팔로워, 보기 범위, 전적 사이트 틀, 아바타 그림)은 확정되는 대로 research "열린 질문의 처리"의 자리를 고친다
