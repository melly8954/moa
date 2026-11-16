---

description: "파티 (party) 작업 목록"
---

# Tasks: 파티 (party)

**Input**: `specs/008-party/`의 설계 문서

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/party-api.md, contracts/party-realtime.md, quickstart.md

**Tests**: 원칙 II에 따라 AC마다 테스트를 하나 둔다. 이름은 `@DisplayName("US<n>-AC<m> ...")`이다. 스토리마다 테스트를 먼저 쓰고, 실패하는 것을 본 뒤 구현한다. 실시간 AC(US9-AC1, AC3, AC7~AC9)는 API 테스트에서 "저장됨"을, `PartyRealtimeTest`에서 "전달됨"을 확인한다 (plan.md Testing). 테스트가 실패하면 테스트를 고치지 말고 spec.md부터 본다 (원칙 V).

**경로 줄임**:
- `BE` = `backend/src/main/java/com/moa`
- `BT` = `backend/src/test/java/com/moa`
- `MIG` = `backend/src/main/resources/db/migration`
- `FE` = `frontend/src`

## Format: `[ID] [P?] [Story] Description`

- **[P]**: 병렬 가능 (다른 파일, 의존 없음)
- **[Story]**: 작업이 속한 사용자 스토리

## 공통 규칙 (모든 작업)

- 계층은 Controller → Application → Service → Reader/Writer → Repository이고 도메인 이름은 `party`다
  - `@Transactional`은 Application에만 둔다
  - `ServiceException`은 Service에서만 던진다
  - Service는 다른 Service를 부르지 않는다. 여러 Service는 Application이 묶는다 (research.md R10, `ArchitectureTest`)
- 회원은 응답에 `{userId, displayName}`으로만 싣고 `login_id`는 싣지 않는다 (FR-027). `displayName`은 `users.name`이다
- 응답의 시각은 `ApiTimes.of`로 만든 OffsetDateTime이다
- 화면 문구는 `frontend/messages/ko.json`에만 두고 코드에 한글을 직접 쓰지 않는다 (`scripts/check-i18n.mjs`)

---

## Phase 1: Setup

**Purpose**: 의존성, 스키마, 시드

- [ ] T001 backend/build.gradle의 dependencies에 `implementation 'org.springframework.boot:spring-boot-starter-websocket'`을 더한다. 버전은 Spring Boot BOM을 따른다 (research.md R1)
- [ ] T002 [P] frontend/에서 `npm install --save-exact @stomp/stomp.js@<지금 최신 7.x>`를 실행해 frontend/package.json과 package-lock.json에 정확한 버전으로 넣는다 (plan.md Primary Dependencies)
- [ ] T003 `MIG/V2_6__parties.sql`을 만든다. data-model.md의 6개 테이블을 그대로 만든다
  - **`parties`**
    - `game_code VARCHAR(50) NOT NULL`, `mode_code VARCHAR(50) NULL`, `tier_min_code`·`tier_max_code VARCHAR(50) NULL`
    - `title VARCHAR(50) NOT NULL`, `description VARCHAR(1000) NOT NULL`, `capacity TINYINT NOT NULL`
    - `join_policy VARCHAR(20) NOT NULL`(`OPEN`·`APPROVAL`·`PRIVATE`), `status VARCHAR(20) NOT NULL`(`RECRUITING`·`CLOSED`)
    - `closed_by_host BOOLEAN NOT NULL`, `host_user_id BIGINT NOT NULL`, `position_enabled BOOLEAN NOT NULL`, `invite_code CHAR(8) NOT NULL`
    - 감사 컬럼, `deleted_at`·`deleted_by`, `active_key` 가상 컬럼(`V1_4__codes.sql`의 방식)
    - `UNIQUE(invite_code, active_key)`
    - 색인 `(status, join_policy, game_code, mode_code, id DESC)`
  - **`party_positions`**: `position_code VARCHAR(50) NOT NULL`, `slots TINYINT NOT NULL`, `UNIQUE(party_id, position_code)`
  - **`party_members`**
    - `role VARCHAR(20) NOT NULL`(`HOST`·`MEMBER`), `position_code`·`tier_code VARCHAR(50) NULL`, `joined_at DATETIME(6) NOT NULL`
    - `UNIQUE(user_id)`
  - **`party_join_requests`**
    - `position_code`·`tier_code VARCHAR(50) NULL`
    - `status VARCHAR(20) NOT NULL`(`PENDING`·`APPROVED`·`REJECTED`·`CANCELLED`), `decided_at DATETIME(6) NULL`
    - `pending_key` 가상 컬럼(PENDING이면 0, 아니면 id), `UNIQUE(party_id, user_id, pending_key)`
  - **`party_bans`**: `reason VARCHAR(20) NOT NULL`(`KICKED`·`REJECTED`), `UNIQUE(party_id, user_id)`
  - **`party_messages`**
    - `type VARCHAR(20) NOT NULL`(`CHAT`·`SYSTEM`), `sender_user_id BIGINT NULL`, `content VARCHAR(500) NULL`
    - `system_event VARCHAR(30) NULL`(`JOINED`·`LEFT`·`KICKED`·`HOST_CHANGED`), `subject_user_id BIGINT NULL`
    - 색인 `(party_id, id)`
  - 모든 테이블에 `created_at`, `updated_at`, `created_by`, `updated_by`가 있다
  - 파일 첫 줄 주석에 `008-party`와 "moa 기능 대역 V2_<스펙 번호>"(research.md R8)를 적는다
- [ ] T004 [P] `MIG/V2_6_1__party_codes.sql`을 만든다
  - spec.md FR-002 표의 그룹 12개(`GAME`, `LOL_MODE`, `LOL_POSITION`, `LOL_TIER`, `OVERWATCH_MODE`, `OVERWATCH_POSITION`, `OVERWATCH_TIER`, `VALORANT_MODE`, `VALORANT_POSITION`, `VALORANT_TIER`, `PUBG_MODE`, `PUBG_TIER`)와 코드를 `code_groups`, `codes`에 id를 고정해 INSERT한다
  - `created_by`·`updated_by`는 1이고, 정렬 순서는 표의 순서다. 티어는 낮은 것부터 10, 20, 30…으로 매긴다
  - 컬럼은 `V1_4__codes.sql`을 따른다
- [ ] T005 `BT/support/CodeSeed.java`를 만든다. `MenuSeed`와 같은 방식이다
  - `delete from codes`, `delete from code_groups` 뒤에 `V2_6_1__party_codes.sql`의 INSERT 문만 다시 실행하는 `restore()`를 둔다
  - 클래스 주석에 "DatabaseCleaner가 시드를 지우므로 되돌린다"(004 research.md R3)를 적는다
- [ ] T006 `BT/api/AdminCodesAcceptanceTest.java`의 `FR_001_GROUP_CODES`를 004 spec.md FR-001 표의 그룹 12개로 바꾸고, `@BeforeEach`에서 `codeSeed.restore()`를 부른다
  - 004 spec.md 표는 docs PR에서 이미 고쳤다 (원칙 IV·V)
  - backend/에서 `./gradlew dbTest --tests "*AdminCodesAcceptanceTest"`로 통과를 확인한다

---

## Phase 2: Foundational

**Purpose**: 모든 스토리가 쓰는 엔터티, 오류 코드, 실시간 기반, 테스트 지원, 프론트 뼈대

**⚠️ CRITICAL**: 이 단계가 끝나야 스토리 작업을 시작한다

### 백엔드

- [ ] T007 `BE/common/exception/ErrorCode.java`에 contracts/party-api.md "오류 코드" 표의 15개를 그 상태값 그대로 더한다
  - `A101` 403 방장만, `A102` 403 멤버만
  - `B101` 409 정원 참, `B102` 409 마감, `B103` 409 이미 파티에 속함, `B104` 409 참가 제한, `B105` 409 포지션 참
  - `B106` 400 포지션 필요, `B107` 404 유효하지 않은 초대, `B108` 409 대기 신청 있음, `B109` 409 인원을 지금보다 작게
  - `B110` 409 차지된 포지션 빼기·줄이기, `B111` 409 게임 바꾸기 불가, `B112` 409 이 참가 방식으로는 못 들어옴, `B113` 409 자기 자신 내보내기
- [ ] T008 [P] frontend/messages/ko.json의 `errors`에 T007의 15개 코드 문구를 더한다
- [ ] T009 [P] `BE/api/entity/party/`에 엔터티 6개를 만든다
  - `Party`: `SoftDeleteEntity` + `@SQLRestriction(SoftDeleteEntity.NOT_DELETED)`
  - `PartyPosition`, `PartyMember`, `PartyJoinRequest`, `PartyBan`, `PartyMessage`: `@HardDelete(reason)`. reason은 data-model.md 각 테이블 머리의 문장이다
  - enum `JoinPolicy`, `PartyStatus`, `PartyRole`, `JoinRequestStatus`, `BanReason`, `MessageType`, `SystemEvent`
  - 컬럼 길이와 NULL 여부는 T003과 같다
  - `Party`에 상태를 바꾸는 메서드를 둔다: `close(byHost)`, `reopen()`, `changeHost(userId)`, `changeJoinPolicy()`, `regenerateInviteCode(code)`. 전이 규칙은 data-model.md "상태 전이"다
- [ ] T010 [P] `BE/api/repository/jpa/party/`에 리포지토리 6개를 만든다
  - `PartyRepository`는 `SoftDeleteRepository`를 상속한다. 나머지는 `JpaRepository`다
  - `PartyRepository`에 `@Lock(PESSIMISTIC_WRITE)` `findForUpdateById(Long id)`를 둔다 (research.md R6, 선례 `RefreshTokenRepository`)
  - `PartyMessageRepository`에 커서 조회 두 가지를 둔다. `before`가 있으면 `id < before` 큰 순으로 51개를 가져와 뒤집는다. `after`가 있으면 `id > after` 작은 순으로 51개를 가져온다. 51번째가 있으면 `hasMore`다 (research.md R5)
- [ ] T011 `BE/api/repository/reader/party/PartyReader.java`, `BE/api/repository/writer/party/PartyWriter.java`를 만든다. 모집 페이지 목록은 QueryDSL로 `status = RECRUITING AND join_policy IN (OPEN, APPROVAL)`과 선택 조건 `game_code`, `mode_code`를 걸고 `id DESC`로 `Page`를 돌려준다. Writer의 멤버 저장은 `saveAndFlush`로 해 유니크 충돌이 그 자리에서 나게 한다
- [ ] T012 `BE/api/dto/party/`에 Dto를 만든다: `PartyDto`, `PartyPositionDto`, `PartyMemberDto`(userId, displayName, role, positionCode, tierCode, joinedAt), `PartyJoinRequestDto`, `PartyMessageDto`. 표시 이름은 `users.name`을 조인해 채운다
- [ ] T013 `BE/common/websocket/`에 실시간 기반을 만든다 (research.md R1, R3, R4)
  - `WebSocketConfig`(`@EnableWebSocketMessageBroker`)
    - STOMP 엔드포인트 `/ws`. 허용 출처는 `CorsProperties.allowedOrigins()`
    - simple broker `/topic`, `/queue`, user 접두사 `/user`
  - `/ws`를 여는 `PublicPaths` 빈
  - `StompAuthInterceptor`(`ChannelInterceptor`)
    - `CONNECT`의 `Authorization: Bearer`를 기존 JWT 검증으로 확인하고 `AuthPrincipal`을 세션 사용자로 둔다
    - 토큰이 없거나 틀리면 연결을 막는다
  - 클래스 주석에 "키트 헬퍼 없이 moa가 구성(plan.md Complexity Tracking)"을 적는다
- [ ] T014 `BE/api/controller/ws/party/PartySubscriptionGuard.java`를 만든다
  - `SUBSCRIBE /topic/parties/{id}`면 `PartyApplication.isMember(partyId, userId)`로 지금 멤버인지 확인하고, 아니면 막는다
  - T013의 인터셉터 체인에 등록한다
  - 구독을 강제로 끊기 위해 사용자별 구독 id를 기록한다 (contracts/party-realtime.md "멤버가 아니게 된 회원")
- [ ] T015 `BE/api/application/v1/party/PartyRealtimeNotifier.java`를 만든다
  - `@TransactionalEventListener(phase = AFTER_COMMIT)`로 파티 이벤트(record)를 받아 `SimpMessagingTemplate`으로 보낸다
  - 이벤트 6종(`MESSAGE`, `MEMBERS_CHANGED`, `PARTY_UPDATED`, `PARTY_DISSOLVED`, `KICKED`, `JOIN_REQUESTS_CHANGED`)과 주소는 contracts/party-realtime.md "이벤트" 표다
  - 모양은 `{type, partyId, payload, occurredAt}`이다
  - `KICKED`와 멤버가 나간 경우에는 그 회원의 `/topic/parties/{id}` 구독을 끊는다 (T014 기록 사용)
- [ ] T016 `BE/api/application/v1/party/PartyApplication.java`의 뼈대를 만든다
  - `isMember`
  - 멤버가 되는 공통 처리 `admit(partyId, userId, positionCode, tierCode, via)`. 순서는 contracts/party-api.md "멤버가 되는 모든 길"의 7단계다
    1. 파티 락
    2. 해산 R001
    3. 참가 제한 B104
    4. 이미 속함 B103
    5. 정원 B101
    6. 마감 B102. `via=APPROVAL`(방장 승인)이면 건너뛴다 (FR-008)
    7. 포지션 B105·B106
    8. 멤버 저장
    9. 나머지 대기 신청 CANCELLED
    10. 정원이 차면 `close(false)`
    11. JOINED 안내 메시지
    12. 이벤트 발행
  - 멤버 저장의 `DataIntegrityViolationException`은 `PartyMemberService`가 받아 B103으로 바꾼다 (research.md R6)
- [ ] T017 [P] `BT/support/PartyFixtures.java`를 만든다
  - `TestUsers`로 회원을 만들고, 파티와 멤버를 JdbcTemplate으로 바로 넣는 도우미를 둔다. 공개·승인·비공개 파티, 롤 포지션 파티, 멤버 n명
  - 시스템 계정(id 1)도 만든다
- [ ] T018 [P] `BT/support/RealtimeTest.java`를 만든다
  - `@Tag("db") @SpringBootTest(webEnvironment = RANDOM_PORT) @ActiveProfiles("test")`, Testcontainers MariaDB
  - `WebSocketStompClient`로 토큰을 실어 연결하는 `connect(userId)`
  - 메시지를 기다리는 `BlockingQueue` 기반 `subscribe(destination)`
  - `@AfterEach` DatabaseCleaner
  - `AcceptanceTest`의 토큰 만들기를 같이 쓴다

### 프론트

- [ ] T019 [P] `FE/api/party.ts`를 만들고 `FE/api/index.ts`의 `api.v1`에 `party`로 등록한다
  - 이 작업은 파일을 만들고 `api.v1.party`로 등록하는 데까지다. 함수는 비워 둔다
  - 함수는 각 스토리의 화면 작업이 그 스토리가 쓰는 경로만큼 더한다. 경로는 contracts/party-api.md를 따르고, 응답 타입은 `npm run gen:api` 뒤 `Schema<"...">`로 쓴다
- [ ] T020 [P] `FE/lib/realtime/`에 STOMP 연결을 만든다 (contracts/party-realtime.md "연결")
  - 연결 주소는 `NEXT_PUBLIC_WS_URL`이다. frontend/.env.example에 `NEXT_PUBLIC_WS_URL=ws://localhost:8080/ws`를 더한다
  - 연결할 때 zustand의 지금 액세스 토큰을 `Authorization`에 싣는다
  - 끊기면 다시 잇는다
  - 연결 상태(연결됨·끊김)를 훅으로 내보낸다
  - 파티 구독 훅 `usePartyChannel(partyId, onEvent)`와 개인 구독 훅 `useMyPartyQueue(onEvent)`를 둔다
- [ ] T021 [P] `FE/components/shell/menu.ts`의 `MENU`에 권한 없이 "모집"(`/recruit`)과 "내 파티"(`/my-party`)를 더하고, frontend/messages/ko.json `menu`에 `recruit`, `myParty`를 더한다 (research.md R11)
- [ ] T022 [P] frontend/messages/ko.json에 `party` 키를 만든다
  - 하위 묶음: `recruit`, `create`, `detail`, `join`, `invite`, `host`, `chat`, `myParty`, `confirm`
  - `chat.system`에 JOINED·LEFT·KICKED·HOST_CHANGED 안내 문구(`{name}` 자리 포함)를 둔다

**Checkpoint**: 스키마, 시드, 오류 코드, 실시간 기반, 테스트 지원이 있다. backend/에서 `./gradlew build`(ArchitectureTest 포함)가 통과한다

---

## Phase 3: User Story 1 - 회원이 모집 페이지에서 파티를 만든다 (Priority: P1) 🎯 MVP

**Goal**: 파티를 만들면 모집 페이지에 바로 올라가고 만든 회원이 방장이 된다

**Independent Test**: `./gradlew dbTest --tests "*PartyCreateAcceptanceTest"`가 통과한다. 화면에서 모집 페이지 → 만들기 → 파티 화면으로 옮겨 간다

- [ ] T023 [US1] `BT/api/party/PartyCreateAcceptanceTest.java`에 US1-AC1~AC5 테스트를 하나씩 쓴다 (`@BeforeEach codeSeed.restore()`)
  - AC1: `POST /api/v1/parties`가 201이고, `role`이 HOST인 멤버가 1명이다. `GET /parties`에 나온다
  - AC2: PRIVATE이면 응답에 `invite.code`(8자)가 있고, `GET /parties`에는 없다
  - AC3: 제목이 빔, 인원 1·11, 목록에 없는 게임 코드면 각각 400 V001이고 `errors`에 그 칸이 있다
  - AC4: 토큰이 없으면 401 A001이다
  - AC5: `GET /api/v1/codes/GAME`이 LOL, OVERWATCH, VALORANT, PUBG 순서다
- [ ] T024 [US1] T023을 돌려 AC5 말고는 실패하는지 확인한다
- [ ] T025 [US1] 백엔드 만들기 흐름을 구현한다
  - `BE/api/controller/v1/party/ApiPartyController.java`의 `POST /parties`
  - `request/PartyCreateRequest.java`(`@Size(min=1,max=50)` title, `@Size(max=1000)` description, capacity 2~10, joinPolicy)와 `toCommand`
  - `BE/api/application/v1/party/command/CreatePartyCommand.java`
  - `PartyApplication.create`
  - `BE/api/service/party/PartyService.java`
    - 게임 코드를 `CodeReader`가 아닌 Application에서 코드 Service로 확인한다. 다른 도메인이므로 Application이 묶는다
    - 이미 속함이면 B103
    - 초대 코드 8자를 만든다. 0 O 1 I를 뺀 대문자·숫자이고, 겹치면 다시 만든다
    - 방장 멤버를 저장한다
  - `response/PartyDetailResponse.java`
- [ ] T026 [US1] T023이 모두 통과하는지 확인한다
- [ ] T027 [US1] 파티 만들기 창을 만든다
  - 폼 스키마는 `FE/lib/schemas/party.ts`(zod. 제목 1~50, 소개 0~1000, 인원 2~10, 참가 방식 3종과 한 줄 설명)
  - 창은 `FE/app/(main)/recruit/create-party-dialog.tsx`
    - 게임은 `CodeSelect`(`GAME`)로 고른다
    - 잘못된 칸은 옆에 이유를 보이고 입력을 지우지 않는다
    - 만들면 `/parties/{id}`로 옮겨 간다. 비공개면 초대 링크 복사를 먼저 보인다 (spec.md 화면 "파티 만들기")
- [ ] T028 [P] [US1] `frontend/tests/unit/party-schema.test.ts`에 T027 스키마의 경계값 테스트를 쓴다 (제목 0·1·50·51자, 인원 1·2·10·11)

**Checkpoint**: US1이 혼자 동작한다

---

## Phase 4: User Story 2 - 회원이 모집 페이지에서 파티를 찾는다 (Priority: P1)

**Goal**: 모집 중인 공개·승인 파티를 최신순으로 보고, 게임과 모드로 거르고, 상세를 본다

**Independent Test**: `./gradlew dbTest --tests "*PartyBrowseAcceptanceTest"`

- [ ] T029 [US2] `BT/api/party/PartyBrowseAcceptanceTest.java`에 US2-AC1~AC5를 쓴다. 파티는 `PartyFixtures`로 넣는다
  - AC1: RECRUITING인 OPEN·APPROVAL만 `id DESC`로 나온다. CLOSED와 PRIVATE은 없다. 카드 항목은 contracts PartySummary다
  - AC2: `gameCode`로, 그리고 `gameCode`+`modeCode`로 거른다
  - AC3: 상세 항목은 PartyDetail이고 `me.relation`이 NONE이다. 비멤버에게는 `invite`가 없다
  - AC4: 비멤버가 PRIVATE를 열면 404 R001이다
  - AC5: 해산된 파티는 404 R001이다
- [ ] T030 [US2] T029가 실패하는지 확인한다
- [ ] T031 [US2] 백엔드 조회를 구현한다
  - `GET /parties`: `@RequestParam` page 기본 0 `@Min(0)`, size 기본 20 `@Min(1) @Max(100)`, gameCode, modeCode. modeCode만 있으면 V001. `PageResponse.from`으로 돌려준다
  - `GET /parties/{id}`
    - PRIVATE이고 멤버가 아니면 R001이다
    - `me.relation`을 채운다: NONE, MEMBER, HOST, PENDING, BANNED
  - 위치는 `ApiPartyController`, `PartyApplication`, `PartyService`, `PartyReader`다
- [ ] T032 [US2] T029가 통과하는지 확인한다
- [ ] T033 [US2] `FE/app/(main)/recruit/page.tsx`를 만든다 (spec.md 화면 "모집 페이지")
  - 위쪽: 파티 만들기 버튼(T027 창), 초대 코드 입력 칸(→ `/invite/{code}`), 게임 `CodeSelect`와 모드 `CodeSelect`(`<game>_MODE`. 게임을 고른 뒤에만 고를 수 있다)
  - 카드 목록: 게임, 모드, 제목, 참가 방식, 인원, 빈 포지션, 티어 조건, 방장
  - 끝에서 다음 쪽을 불러온다
  - 상태
    - 빈 목록 안내와 만들기 권유
    - 이미 파티에 속하면 만들기 대신 "내 파티로 가기"
    - 실패하면 다시 시도. `QueryState`를 쓴다
- [ ] T034 [US2] `FE/app/(main)/parties/[id]/page.tsx`의 뼈대를 만든다
  - 파티 정보와 멤버 목록(`member-list.tsx`: 표시 이름, 방장 표시, 포지션, 적은 티어, 포지션별 빈자리)을 보인다
  - 404면 not-found로 보낸다
  - 비멤버·멤버·방장에 따라 다른 영역은 각 스토리에서 채운다 (spec.md 화면 "파티 화면" 표)

**Checkpoint**: US1, US2가 함께 동작한다. 만든 파티를 다른 회원이 찾는다

---

## Phase 5: User Story 3 - 회원이 공개 파티에 참가한다 (Priority: P1)

**Goal**: 공개 파티에 바로 들어오고, 정원이 차면 자동 마감하고, 한 번에 한 파티만 속한다

**Independent Test**: `./gradlew dbTest --tests "*PartyJoinAcceptanceTest"`

- [ ] T035 [US3] `BT/api/party/PartyJoinAcceptanceTest.java`에 US3-AC1~AC7을 쓴다
  - AC1: `POST /parties/{id}/members`가 200이고 memberCount가 +1이다
  - AC2: 마지막 자리가 차면 status CLOSED, closedByHost false가 되고 `GET /parties`에서 빠진다
  - AC3: 정원이 차면 409 B101, 마감이면 409 B102
  - AC4: 이미 멤버면 409 B103
  - AC5: 한 자리 남은 파티에 두 회원이 `ExecutorService`와 `CountDownLatch`로 동시에 참가하면, 하나는 200이고 하나는 B101이며 멤버 수가 capacity와 같다
  - AC6: 다른 파티 멤버가 만들기·참가·신청·초대 입장을 하면 각각 409 B103이다. 신청과 초대 입장은 US4·US5에서 그 API가 생길 때 이 테스트에 더한다
  - AC7: 승인 신청 두 개를 넣어 둔 회원이 공개 파티에 참가하면 두 신청이 CANCELLED가 된다. 신청 행은 JdbcTemplate으로 넣는다
- [ ] T036 [US3] T035가 실패하는지 확인한다
- [ ] T037 [US3] 백엔드 참가를 구현한다
  - `BE/api/controller/v1/party/ApiPartyMemberController.java` `POST /parties/{id}/members`(`request/PartyJoinRequest.java`는 이름이 엔터티와 겹치므로 `PartyMemberCreateRequest`로 한다. positionCode?, tierCode?)
  - OPEN이 아니면 B112
  - T016 `admit` 완성: `BE/api/service/party/PartyMemberService.java`(인원 세기, 저장과 B103 변환), `PartyJoinRequestService.cancelPendingOf(userId)`, `PartyMessageService.system(JOINED)`
  - `MEMBERS_CHANGED`와 `MESSAGE` 이벤트를 발행한다
- [ ] T038 [US3] T035가 통과하는지 확인한다
- [ ] T039 [US3] 파티 화면에 비멤버의 참가 버튼을 붙인다 (`FE/app/(main)/parties/[id]/join-button.tsx`)
  - 공개면 "참가"
  - 정원 참·마감·다른 파티 속함·참가 제한이면 누를 수 없게 하고 이유를 보인다. 이유는 `me.relation`과 status로 정한다. 다른 파티에 속했으면 내 파티로 가는 길도 보인다
  - 성공하면 화면을 다시 불러온다. 실패하면 오류 코드 문구를 보인다

**Checkpoint**: 만들기 → 찾기 → 참가가 이어진다

---

## Phase 6: User Story 9 - 멤버가 파티 채팅으로 이야기하고 파티 변화를 바로 본다 (Priority: P1)

**Goal**: 멤버끼리 채팅하고, 멤버 변화가 새로고침 없이 보인다

**Independent Test**: `./gradlew dbTest --tests "*PartyChatAcceptanceTest" --tests "*PartyRealtimeTest"`

- [ ] T040 [US9] `BT/api/party/PartyChatAcceptanceTest.java`에 API 쪽 테스트를 쓴다
  - AC1(저장됨): `POST /parties/{id}/messages`가 201이고 sender, content, sentAt이 있다
  - AC2: 메시지 120개를 넣고, 새 멤버가 `GET messages`로 마지막 50개를 오래된 순으로 받고 `hasMore` true다. `before`로 그 앞 50개를 받는다
  - AC3(저장됨): 참가·나가기·내보내기·방장 바뀜 뒤 SYSTEM 메시지가 있다. 나가기·내보내기·방장 바뀜은 US6·US7 API가 생길 때 이 테스트에 더한다
  - AC4: 비멤버, 나간 회원, 내보내진 회원, 대기 신청자가 읽기·보내기를 하면 403 A102다
  - AC5: 빈 값과 501자는 400 V001이다
  - AC6: 해산 뒤 읽으면 404 R001이다
  - FR-018: `after`로 그 뒤 것만 받는다
- [ ] T041 [US9] `BT/api/party/PartyRealtimeTest.java`(`RealtimeTest` 상속)에 전달 쪽 테스트를 쓴다
  - AC1(전달됨): A가 보내면 B의 `/topic/parties/{id}` 구독에 `MESSAGE`가 1초 안에 온다
  - AC3(전달됨): 참가하면 `MESSAGE`(SYSTEM JOINED)가 온다
  - AC7: 참가하면 두 구독자에게 `MEMBERS_CHANGED`가 온다
  - AC8: 내보내면 그 회원의 `/user/queue/parties`에 `KICKED`가 오고 topic 구독이 끊긴다. 해산하면 `PARTY_DISSOLVED`가 온다. US7 API가 생길 때 채운다
  - AC9: 방장의 user queue에 `JOIN_REQUESTS_CHANGED`가 온다. US5 API가 생길 때 채운다
  - 구독 권한: 비멤버가 topic을 구독하면 막힌다. 토큰이 없으면 CONNECT가 막힌다
- [ ] T042 [US9] T040, T041이 실패하는지 확인한다
- [ ] T043 [US9] 백엔드 채팅을 구현한다
  - `BE/api/controller/v1/party/ApiPartyMessageController.java`: `GET /parties/{id}/messages?before|after&size=50`, `POST /parties/{id}/messages`(`@Size(min=1,max=500)` content)
  - `BE/api/application/v1/party/PartyMessageApplication.java`
  - `BE/api/service/party/PartyMessageService.java`: 멤버가 아니면 A102
  - 응답은 `{items, hasMore}`이다
  - 저장한 뒤 `MESSAGE` 이벤트를 발행한다
- [ ] T044 [US9] T040, T041 중 지금 있는 API로 확인할 수 있는 것이 통과하는지 확인한다
- [ ] T045 [US9] 채팅 패널을 만든다 (`FE/app/(main)/parties/[id]/chat-panel.tsx`, spec.md 화면 "파티 화면 - 채팅")
  - 오래된 것이 위, 새것이 아래다. 위로 올리면 `before`로 더 불러온다. `components/ui/message-scroller.tsx`, `message.tsx`를 쓴다
  - 보낸 사람과 시각을 보이고, 내 메시지는 구별한다
  - SYSTEM은 `party.chat.system` 문구로 다르게 보인다
  - 남은 글자 수를 보이고 500자를 넘으면 보내지 못한다
  - 연결이 끊기면 "끊김"을 표시한다
  - 다시 이으면 `after`로 채운다
- [ ] T046 [US9] 파티 화면에 실시간 반영을 붙인다 (`FE/app/(main)/parties/[id]/use-party-live.ts`)
  - `usePartyChannel`의 `MEMBERS_CHANGED`·`PARTY_UPDATED`는 TanStack Query 캐시를 고친다
  - `MESSAGE`는 채팅에 붙인다
  - `PARTY_DISSOLVED`와 `useMyPartyQueue`의 `KICKED`는 알리고 `/recruit`로 보낸다
- [ ] T047 [P] [US9] `frontend/tests/unit/party-live.test.ts`에 T046의 이벤트별 캐시 반영 테스트를 쓴다

**Checkpoint**: P1 전부(US1, US2, US3, US9)가 동작한다. 여기까지가 MVP다

---

## Phase 7: User Story 4 - 회원이 초대 코드나 링크로 들어온다 (Priority: P2)

**Goal**: 초대 코드·링크로 방식과 상관없이 바로 들어온다

**Independent Test**: `./gradlew dbTest --tests "*PartyInviteAcceptanceTest"`

- [ ] T048 [US4] `BT/api/party/PartyInviteAcceptanceTest.java`에 US4-AC1~AC7을 쓴다
  - AC1: 멤버의 상세에는 `invite.code`·`url`이 있다
  - AC2: PRIVATE 파티를 `POST /parties/invites/{code}/members`로 들어오면 200이다
  - AC3: 코드로 들어오면 200이다
  - AC4: APPROVAL 파티도 신청 없이 멤버가 된다
  - AC5: 없는 코드와 해산된 파티의 코드는 404 B107이다
  - AC6: 새로 만든 뒤 이전 코드는 404 B107이다. 코드 새로 만들기 API가 US7에서 생기기 전에는 JdbcTemplate으로 코드를 바꿔 확인한다
  - AC7: 정원 참·마감이면 B101·B102다
  - US3-AC6에 초대 입장 경우를 더한다
- [ ] T049 [US4] T048이 실패하는지 확인한다
- [ ] T050 [US4] 백엔드를 구현한다
  - `GET /parties/invites/{code}`: InvitePreview(joinable, reasonCode)
  - `POST /parties/invites/{code}/members`: T016 `admit`을 via=INVITE로 부른다
  - 위치는 `ApiPartyMemberController`, `PartyApplication`이다
- [ ] T051 [US4] T048이 통과하는지 확인한다
- [ ] T052 [US4] `FE/app/(main)/invite/[code]/page.tsx`를 만든다 (spec.md 화면 "초대 입장 화면")
  - 요약과 "들어가기"를 보인다
  - 이미 멤버면 파티 화면으로 바로 보낸다
  - 유효하지 않으면 안내와 모집 페이지로 가는 길을 보인다
  - 못 들어오는 이유가 있으면 버튼을 막는다
  - 로그인하지 않았으면 `AuthGate`의 `?next=`로 돌아온다 (research.md R11). 새로 만들 것은 없다
  - 파티 화면에 멤버용 초대 코드·링크 복사(`invite-box.tsx`)를 붙인다

---

## Phase 8: User Story 5 - 회원이 승인 파티에 신청하고 방장이 받는다 (Priority: P2)

**Goal**: 승인 파티의 신청, 승인·거절, 신청 취소

**Independent Test**: `./gradlew dbTest --tests "*PartyJoinRequestAcceptanceTest"`

- [ ] T053 [US5] `BT/api/party/PartyJoinRequestAcceptanceTest.java`에 US5-AC1~AC8을 쓴다
  - AC1: `POST /parties/{id}/join-requests`가 201 PENDING이다
  - AC2: 방장의 `GET .../join-requests`가 신청순이다
  - AC3: 승인하면 멤버가 되고, 마지막 자리면 CLOSED다
  - AC4: 거절하면 party_bans에 REJECTED가 남는다. 다시 신청하거나 초대로 들어오면 B104다
  - AC5: 정원이 찬 뒤 승인하면 B101이다
  - AC6: 취소하면 CANCELLED가 되고, 다시 신청할 수 있다
  - AC7: 대기 중에 또 신청하면 B108이다
  - AC8: 비방장이 목록·승인·거절을 하면 403 A101이다
  - 방장이 직접 마감한 파티에서도 자리가 있으면 승인이 된다 (FR-008)
  - US3-AC6에 신청 경우를 더한다
  - OPEN 파티에 신청하면 B112다
- [ ] T054 [US5] T053이 실패하는지 확인한다
- [ ] T055 [US5] 백엔드를 구현한다
  - `BE/api/controller/v1/party/ApiPartyJoinRequestController.java`: 경로 5개는 contracts/party-api.md "승인 신청"이다
  - `BE/api/service/party/PartyJoinRequestService.java`
    - 신청은 파티 락 안에서 B101·B102·B103·B104·B105·B106·B108·B112를 확인한다
    - 유니크 충돌은 B108로 바꾼다
  - 승인은 T016 `admit`(via=APPROVAL)을 쓰고 신청을 APPROVED로 바꾼다
  - 거절은 REJECTED와 ban이다
  - 신청 생성·취소·처리 뒤 방장에게 `JOIN_REQUESTS_CHANGED`를 보낸다
  - T041 AC9를 채운다
- [ ] T056 [US5] T053, T041 AC9가 통과하는지 확인한다
- [ ] T057 [US5] 화면을 붙인다
  - 비멤버 버튼에 "신청"과 "신청 대기 중 + 신청 취소"를 더한다 (T039 `join-button.tsx`)
  - 방장의 신청 목록(`FE/app/(main)/parties/[id]/join-requests.tsx`): 대기 수, 승인·거절, `JOIN_REQUESTS_CHANGED`면 다시 불러오기

---

## Phase 9: User Story 6 - 멤버가 나가고 파티가 이어지거나 끝난다 (Priority: P2)

**Goal**: 나가기, 방장 잇기, 마지막 멤버면 자동 해산

**Independent Test**: `./gradlew dbTest --tests "*PartyLeaveAcceptanceTest"`

- [ ] T058 [US6] `BT/api/party/PartyLeaveAcceptanceTest.java`에 US6-AC1~AC5를 쓴다
  - AC1: `DELETE /parties/{id}/members/me`가 204이고 인원이 -1이다
  - AC2: 자동 마감 파티가 RECRUITING으로 돌아온다
  - AC3: 방장이 나가면 `joined_at`이 가장 이른 멤버가 HOST가 되고 `host_user_id`도 바뀐다. 같은 시각이면 id가 작은 쪽이다
  - AC4: 마지막 멤버가 나가면 해산된다(404). 대기 신청도 정리된다
  - AC5: 나간 회원이 다시 참가할 수 있다
  - US9-AC3에 LEFT·HOST_CHANGED 안내를 더한다
- [ ] T059 [US6] T058이 실패하는지 확인한다
- [ ] T060 [US6] 백엔드를 구현한다 (`PartyApplication.leave`)
  - 파티 락 → 멤버 행 삭제 → 방장이면 다음 방장 → 남은 사람이 없으면 해산을 한다
  - 해산은 공통 메서드 `dissolve`다: 멤버·포지션 자리·대기 신청 정리와 `Party.delete(actorId)`. 메시지와 ban은 남긴다 (research.md R12)
  - 자리가 나면 `closedByHost`가 아닐 때만 reopen한다 (FR-008)
  - LEFT·HOST_CHANGED 안내와 `MEMBERS_CHANGED` 이벤트를 보낸다
  - 나간 회원의 구독을 끊는다
- [ ] T061 [US6] T058이 통과하는지 확인한다
- [ ] T062 [US6] 파티 화면에 "나가기"를 붙인다. 확인 창(`confirm`)에서 방장이면 "가장 먼저 들어온 멤버가 방장이 된다", 마지막이면 "파티가 해산된다"를 알린다. 나가면 `/recruit`로 보낸다

---

## Phase 10: User Story 7 - 방장이 파티를 관리한다 (Priority: P2)

**Goal**: 고치기, 방식 바꾸기, 내보내기, 마감·재개, 초대 코드 새로 만들기, 해산

**Independent Test**: `./gradlew dbTest --tests "*PartyHostAcceptanceTest"`

- [ ] T063 [US7] `BT/api/party/PartyHostAcceptanceTest.java`에 US7-AC1~AC9를 쓴다
  - AC1: `PUT /parties/{id}`로 고친다. 인원을 지금보다 작게 하면 B109다
  - AC2·AC3: `PUT .../join-policy`로 PRIVATE이 되면 모집 페이지에서 빠지고, 되돌리면 다시 보인다
  - AC4: APPROVAL에서 바꾸면 대기 신청이 CANCELLED가 된다
  - AC5: 내보내기 `DELETE .../members/{userId}`가 204다. 그 회원이 다시 참가·신청·초대 입장을 하면 B104다. 자기 자신을 내보내면 B113이다
  - AC6: `close` 뒤 참가·신청·초대 입장이 B102이고, `reopen`하면 돌아온다. 정원이 찼는데 reopen하면 B101이다
  - AC7: `POST .../invite-code` 뒤 이전 코드가 B107이다
  - AC8: `DELETE /parties/{id}`가 204이고 그 뒤 404이며 대기 신청이 정리된다
  - AC9: 비방장이 모두 하면 403 A101이다
  - US9-AC3에 KICKED 안내를 더한다
  - T041 AC8(KICKED, PARTY_DISSOLVED 전달)을 채운다
- [ ] T064 [US7] T063이 실패하는지 확인한다
- [ ] T065 [US7] 백엔드를 구현한다
  - 경로는 contracts/party-api.md "모집 페이지와 파티" 중 방장 줄 6개와 내보내기다
  - 모두 파티 락 안에서 방장 확인(A101)을 먼저 한다
  - 내보내기는 멤버 삭제, ban KICKED, KICKED 안내, 그 회원에게 `KICKED`, 구독 끊기다
  - 해산은 T060 `dissolve`와 `PARTY_DISSOLVED`다
  - 고치기·방식 바꾸기는 `PARTY_UPDATED`를 보낸다
- [ ] T066 [US7] T063, T041 AC8이 통과하는지 확인한다
- [ ] T067 [US7] 방장 동작을 붙인다 (`FE/app/(main)/parties/[id]/host-actions.tsx`)
  - 정보 고치기 창은 T027 폼을 다시 쓴다
  - 참가 방식 바꾸기, 마감·재개, 초대 코드 새로 만들기, 해산, 멤버마다 내보내기를 둔다
  - 확인 창은 spec.md 화면 "되돌리기 어려운 동작" 5가지다

---

## Phase 11: User Story 10 - 모드·포지션·티어 조건 (Priority: P2)

**Goal**: 방장이 조건을 걸고, 참가자는 빈 포지션을 골라 티어를 적고 들어온다

**Independent Test**: `./gradlew dbTest --tests "*PartyConditionAcceptanceTest"`

- [ ] T068 [US10] `BT/api/party/PartyConditionAcceptanceTest.java`에 US10-AC1~AC10을 쓴다
  - AC1: 롤에서 방장 MID, 모집 JUNGLE·TOP이면 capacity가 3이고 positions slots가 모두 1이다
  - AC2: 오버워치에서 TANK 1, DAMAGE 2, SUPPORT 2면 capacity가 5다
  - AC3: 조건이 없으면 positionEnabled false다
  - AC4: 배그에 포지션을 주면 V001이다
  - AC5: 빈 포지션으로 참가·신청·초대 입장을 하면 그 포지션의 filled가 +1이다(신청은 승인 뒤)
  - AC6: 찬 포지션은 B105이고 `details.openPositions`가 있다
  - AC7: 포지션을 안 고르면 400 B106이다
  - AC8: 범위 밖 티어도 200이고 멤버의 tierCode가 보인다
  - AC9: 나가면 빈자리가 된다
  - AC10: 차지된 포지션을 빼거나 줄이면 B110이다. 멤버가 있는 포지션 파티의 게임을 바꾸면 B111이다
  - 그 밖의 확인
    - 모드 코드가 그 게임의 `<game>_MODE`에 없으면 V001이다
    - tierMin의 정렬 순서가 tierMax보다 크면 V001이다
    - 롤 slots를 2로 주면 V001이다
- [ ] T069 [US10] T068이 실패하는지 확인한다
- [ ] T070 [US10] 백엔드를 구현한다
  - 만들기·고치기 요청에 `modeCode`, `tierMinCode`, `tierMaxCode`, `positions: [{positionCode, slots}]`, `hostPositionCode`를 더한다
  - 코드 확인은 Application이 공통코드 Service를 불러 한다. 그룹 이름은 `<gameCode>_MODE|_POSITION|_TIER` 규칙이다 (research.md R7)
  - 포지션이 있으면 capacity = slots 합이다. 범위는 2~10이다 (FR-022)
  - `admit`의 포지션 확인을 완성한다 (B105·B106)
  - 마지막 포지션 동시 입장은 파티 락으로 막는다 (FR-009)
  - 승인 때 신청 포지션이 찼으면 B105다
- [ ] T071 [US10] T068이 통과하는지 확인한다
- [ ] T072 [US10] 화면을 붙인다
  - T027 만들기 창에 게임별 칸을 더한다
    - 모드 `CodeSelect`
    - 롤: 내 포지션 + 모집할 포지션 체크
    - 발로란트·오버워치: 포지션별 자리 수
    - 배그: 포지션 칸 없음
    - 티어 최소·최대
    - 포지션 조건을 켜면 인원이 자동 계산된다
  - 참가 창(`FE/app/(main)/parties/[id]/join-dialog.tsx`, spec.md 화면 "참가 창")
    - 빈 포지션만 고를 수 있다
    - 티어는 선택이고 범위 밖이면 안내만 한다
    - B105면 남은 포지션을 다시 보인다
    - 조건이 없으면 창 없이 바로 처리한다
  - T028에 포지션 조건 스키마 테스트를 더한다

---

## Phase 12: User Story 8 - 회원이 내가 속한 파티를 본다 (Priority: P3)

**Goal**: 지금 속한 파티와 대기 신청을 한곳에서 본다

**Independent Test**: `./gradlew dbTest --tests "*MyPartyAcceptanceTest"`

- [ ] T073 [US8] `BT/api/party/MyPartyAcceptanceTest.java`에 US8-AC1·AC2를 쓴다
  - AC1: 신청 두 개가 신청순으로 나오고 party.title과 positionCode가 있다
  - AC2: PRIVATE 파티 멤버면 `party`와 `myRole`이 나온다
- [ ] T074 [US8] T073이 실패하는지 확인하고, `GET /parties/me`를 구현하고(`ApiPartyController`, `PartyApplication.myParty`), 통과하는지 확인한다
- [ ] T075 [US8] `FE/app/(main)/my-party/page.tsx`를 만든다 (spec.md 화면 "내 파티")
  - 파티 카드와 파티 화면 버튼을 보인다
  - 대기 신청 목록과 신청 취소를 보인다
  - 둘 다 없으면 모집 페이지로 가는 길을 보인다

---

## Phase 13: Polish & Cross-Cutting

- [ ] T076 [P] `frontend/e2e/party.spec.ts`에 spec.md "e2e로 확인할 흐름" 3개를 쓴다
  1. 공개 파티를 만들고 다른 회원이 참가해 채팅을 주고받는다
  2. 비공개 초대 링크로 들어온다
  3. 롤 포지션을 골라 들어온다
  - REST는 `page.route`, 실시간은 `page.routeWebSocket`으로 가짜 STOMP 프레임을 보낸다. 로그인은 `e2e/support/api.ts`의 `signedIn`이다
  - 화면마다 axe 검사를 넣는다. 기존 e2e와 같다
  - 흐름 1에 키보드만으로 하는 단계를 넣는다. 파티 만들기 창, 참가 창, 채팅 입력을 Tab·Enter·Esc로만 조작한다 (spec.md "## 화면" 머리)
- [ ] T077 research.md R4를 확인한다. `src/proxy.ts`로 WebSocket 업그레이드가 통하는지 로컬에서 본다
  - 통하면 `NEXT_PUBLIC_WS_URL` 대신 같은 출처 `/ws`로 바꾸고 research.md R4를 고친다
  - 안 통하면 지금대로 두고 R4에 "확인함"을 적는다
- [ ] T078 backend/에서 `./gradlew spotlessApply`, `./gradlew build dbTest`, frontend/에서 `npm run format`, `npm run check`, `npm run test:e2e`를 돌려 모두 통과하는지 확인한다
  - ArchitectureTest가 실패하면 research.md R10의 배치를 다시 본다
  - 테스트를 고쳐서 통과시키지 않는다 (원칙 V)
- [ ] T079 AC 추적을 확인한다. `grep -rhoE 'US[0-9]+-AC[0-9]+' backend/src/test frontend/e2e | sort -u | wc -l`이 67인지 본다. 빠진 AC가 있으면 그 스토리의 테스트를 더한다
- [ ] T080 specs/README.md 사용자 쪽 표에서 `008-party` 상태를 `스펙 작성 중`에서 `구현됨`으로 바꾼다
- [ ] T081 quickstart.md "화면 확인"을 사람이 직접 한다. 시험 계정 비밀번호가 필요하므로 에이전트는 하지 않는다 (원칙 VI)

---

## Dependencies & Execution Order

### 단계

- Phase 1(Setup) → Phase 2(Foundational) → 스토리들 → Phase 13(Polish)
- T003 → T005 → T006, T004 → T005
- T009 → T010 → T011 → T012. T013 → T014 → T015 → T016

### 스토리 사이

| 스토리 | 기대는 것 | 이유 |
|---|---|---|
| US1 | Foundational | |
| US2 | US1 | 만든 파티를 찾는다. 테스트는 Fixtures로 따로 돈다 |
| US3 | US1 | `admit` 공통 처리를 완성한다 |
| US9 | US3 | 멤버가 둘 있어야 한다 |
| US4 | US3 | `admit`(via=INVITE) |
| US5 | US3 | `admit`(via=APPROVAL) |
| US6 | US3 | `dissolve`를 만든다 |
| US7 | US6 | `dissolve`를 쓴다 |
| US10 | US3, US5 | 포지션 확인이 참가·신청·초대 모두에 걸린다 |
| US8 | US5 | 대기 신청을 보인다 |

- 한 스토리 안에서는 테스트 작성 → 실패 확인 → 백엔드 → 통과 확인 → 화면 순서다
- 뒤 스토리가 앞 스토리의 테스트(US3-AC6, US9-AC3, T041)에 경우를 더하는 작업이 있다. 그 작업은 그 뒤 스토리의 테스트 작업(T048, T053, T058, T063)에 들어 있다

## Parallel Example

```text
Phase 1: T001과 T002, T003과 T004는 함께 할 수 있다
Phase 2: T009(엔터티), T013(websocket 설정), T017·T018(테스트 지원), T019~T022(프론트 뼈대)는 서로 다른 파일이다
US3이 끝난 뒤: US9, US4, US5, US6은 백엔드 파일이 다르다
  - 테스트 클래스와 Controller는 따로 만든다
  - PartyApplication·join-button.tsx는 함께 고치므로 합칠 때 순서를 맞춘다
각 스토리 안: 백엔드 통과 확인 뒤 화면 작업과 단위 테스트(T028, T047)는 함께 할 수 있다
```

## Implementation Strategy

### MVP

Phase 1~2 → US1 → US2 → US3 → US9 (T001~T047)까지가 MVP다. 파티를 만들고, 찾고, 들어와서, 채팅한다.

여기서 멈추고 quickstart.md 2~4단계로 확인한 뒤 다음으로 간다.

### 이어서

1. US4(초대), US5(승인): 참가 방식 3종을 완성한다
2. US6(나가기), US7(방장 관리): 파티의 수명을 완성한다
3. US10(조건): 모집의 질을 높인다
4. US8(내 파티)

### PR 나누기

구현 브랜치는 `feat/008-party` 하나다. 스토리가 많아 리뷰하기 어려우면 MVP(T001~T047)를 첫 PR로 내고, 나머지를 같은 브랜치에서 이어 두 번째 PR로 내는 것을 권한다. 나눌지는 사람이 정한다.
