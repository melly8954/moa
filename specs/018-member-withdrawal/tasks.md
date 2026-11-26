---

description: "회원 탈퇴 (member-withdrawal) 작업 목록"
---

# Tasks: 회원 탈퇴 (member-withdrawal)

**Input**: `specs/018-member-withdrawal/`의 설계 문서

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/withdrawal-api.md, contracts/screens.md, quickstart.md

**Tests**: 원칙 II에 따라 AC마다 테스트를 하나 둔다. CLAUDE.md에 따라 기능 코드를 먼저 끝내고 테스트 작업은 맨 끝(Phase 8)에 둔다. 예외는 T002다(사용자 지시: Hibernate 조인 시험을 구현 첫 작업으로). 로컬에서는 빠른 검사를 돌리되, T002의 시험, T013과 T060의 백엔드 테스트 전체(`dbTest` 포함), T061의 e2e 전체는 사용자 지시로 로컬에서 돌린다.

## Format: `[ID] [P?] [Story?] Description`

- **[P]**: 병렬 가능 (다른 파일, 의존 없음)
- **[Story]**: US1 탈퇴, US2 복구, US3 파기, US4 다른 기능이 붙는 자리
- **[공용]**: 고치기 전에 바꿀 내용을 보고하고 승인받는다 (plan "공용 파일")
- **[haily, 승인됨]**: haily(melly8954) 코드. 2026-11-25 사용자 승인. 승인한 범위를 넘으면 멈추고 묻는다
- 백엔드 패키지 `backend/src/main/java/com/moa/`는 `B/`, 테스트 `backend/src/test/java/com/moa/`는 `BT/`, 프론트 `frontend/src/`는 `F/`로 줄여 쓴다

---

## Phase 1: Setup (확인과 시험)

**Purpose**: 설계가 기대는 것을 먼저 확인한다

- [X] T001 구현 브랜치 `feat/018-member-withdrawal`을 최신 `dev`에서 딴 뒤 확인한다: backend/src/main/resources/db/migration의 마지막 번호(계획 때 `V12`), B/common/exception/ErrorCode.java에 `A401`·`A402`·`A403`이 없는지, 008·014·017 구현이 dev에 들어왔으면 `tb_user.name`·`UserRepository`·`tb_user_profile`을 읽는 새 곳이 있는지(있으면 research R8의 "막히지 않는 넷"과 plan "담당자 알림 목록"을 먼저 고치고 보고한다)
- [X] T002 Hibernate 조건 시험: BT/api/UserRestrictionAcceptanceTest.java(`AcceptanceTest` 상속)를 만들어 **지금 있는** `deleted_at IS NULL` 조건이 지운 `User`에 붙는지 본다: `UserRepository.findById`, QueryDSL `selectFrom(user)`, 다른 엔터티(`UserSocialAccount`)에서 `user`로 가는 왼쪽 조인·안쪽 조인(`on` 조건), `JPAExpressions` 서브쿼리. native 쿼리로 읽은 `User`가 나오고 고친 값이 저장되는지도 본다. `.\gradlew.bat dbTest --tests "*UserRestrictionAcceptanceTest"`로 돌린다(사용자 지시). 결과를 research.md R8 "확인할 것"에 적는다. **조인에 조건이 붙지 않으면 멈추고 보고한다** — **이번 구현**: 7개 통과. 모두 조건이 붙는다 (research R8)
- [X] T003 plan.md "공용 파일"과 "haily 코드" 표의 바꿀 내용을 초안으로 보고하고 승인받는다. haily 코드 다섯 건(007 `notify` 한 줄과 지우기 메서드, 006 지우기 메서드, 013 종류 "회원", `tb_purge_log.target` COMMENT)은 2026-11-25에 승인됐다. 기존 동작은 바꾸지 않고 더하기만 한다. 승인 전에는 [공용] 작업을 시작하지 않는다

**Checkpoint**: 조인 시험 결과가 R8에 적혔고 공용 파일이 승인됐다

---

## Phase 2: Foundational (칸, 기본 조회 조건, 상태)

**Purpose**: 모든 스토리가 기대는 것. `User` 조건이 바뀌므로 이 단계 끝에 백엔드 테스트 전체를 돌린다

- [X] T004 backend/src/main/resources/db/migration/V13__member_withdrawal.sql (번호는 PR 직전에 다시 본다): 머리 주석 `-- moa: 회원 탈퇴 시각·파기 시각. 스펙 specs/018-member-withdrawal (FR-009·FR-012·FR-013·FR-019, data-model.md)`
  - `ALTER TABLE tb_user ADD COLUMN withdrawn_at DATETIME(6) NULL COMMENT '탈퇴한 시각 (NULL: 탈퇴 안 함)' AFTER password_change_required, ADD COLUMN purged_at DATETIME(6) NULL COMMENT '개인정보 파기 시각 (NULL: 파기 안 함)' AFTER withdrawn_at`, 인덱스 `idx_user_purged_at_withdrawn_at (purged_at, withdrawn_at)`, `idx_user_purged_at_deleted_at (purged_at, deleted_at)`
  - [haily, 승인됨] `ALTER TABLE tb_purge_log MODIFY COLUMN target VARCHAR(30) NOT NULL COMMENT '파기 대상 종류 (LOGIN_HISTORY, NOTIFICATION, REFRESH_TOKEN, RICH_TEXT_IMAGE, MEMBER)'`
  - `uk_user_login_id_active_key`와 `active_key`는 고치지 않는다. 행을 넣거나 고치지 않는다
- [X] T005 [공용] B/api/entity/user/User.java: `withdrawnAt`, `purgedAt`(읽기만), `withdraw(now)`(값이 있으면 그대로), `restore()`, `isWithdrawn()`. `@SQLRestriction("deleted_at IS NULL AND withdrawn_at IS NULL")`을 상수 `User.ACTIVE`로 두고 쓴다. 조건의 이유(research R8)를 주석에 적는다
- [X] T006 [공용] B/api/repository/jpa/user/UserRepository.java·B/api/repository/reader/user/UserReader.java: `findByLoginId`를 native(`select * from tb_user where login_id = :loginId and deleted_at is null`)로 바꾼다. `findIncludingWithdrawn(id)`, `findForUpdateIncludingWithdrawn(id)`(`for update`), `existsWithdrawn(id)`를 native로 더한다(모두 `deleted_at is null`, `existsWithdrawn`은 `deleted_at`을 보지 않음). 관리자 코드(`UserAdminReader`, `UserAdminService`, `RoleUserReader`)는 고치지 않는다 (T005 뒤)
- [X] T007 [P] [공용] B/common/exception/ErrorCode.java: `ACCOUNT_WITHDRAWN`(A401, 401), `ACCOUNT_PURGE_PENDING`(A402, 401), `RECOVERY_NOT_AVAILABLE`(A403, 400). frontend/messages/ko.json `errors`에 세 문구
- [X] T008 [P] B/api/service/user/WithdrawalProperties.java(`app.withdrawal`: `grace-days` 기본 30, `cleanup-cron` 기본 `0 20 5 * * *`)와 [공용] backend/src/main/resources/application.yaml
- [X] T009 [P] B/api/dto/user/: `MemberWithdrawnEvent`, `MemberRestoredEvent`, `MemberPurgedEvent`(회원 ID, 일어난 때), `MemberStatusDto`(상태 ACTIVE·WITHDRAWN·DELETED·PURGED, 탈퇴한 때, 파기한 때)
- [X] T010 B/api/service/user/MemberStatusService.java: `find(userId)`(native라 유예 중·지움·파기 모두 나온다, FR-018), `isWithdrawn(userId)`, `purgeAt(withdrawnAt)`, 유예가 남았는지. 파기된 회원의 `withdrawn_at`도 준다 (T006·T008·T009 뒤)
- [X] T011 [P] B/api/service/user/MemberDisplay.java: `nameOf(Optional<User>)`가 비었으면 "탈퇴한 회원". 상수로 두고 T035의 고정값과 함께 쓴다 (FR-008)
- [X] T012 `.\gradlew.bat spotlessApply`, `.\gradlew.bat build`, `bootRun`으로 Flyway와 `ddl-auto: validate` 통과를 본다 (사용자가 띄운다)
- [X] T013 **백엔드 테스트 전체**: `.\gradlew.bat build`와 `.\gradlew.bat dbTest`를 로컬에서 돌린다(사용자 지시). T002의 시험도 새 조건(`withdrawn_at`)으로 넓혀 함께 돈다. **깨지는 기존 테스트가 있으면 고치지 말고 이유와 함께 보고하고 멈춘다** (원칙 V)

**Checkpoint**: `User` 조건이 바뀌어도 기존 기능이 그대로다. 관리자 코드는 바뀌지 않았다

---

## Phase 3: User Story 1 - 회원이 스스로 탈퇴한다 (Priority: P1) 🎯 MVP

**Goal**: 계정 관리 탭에서 닉네임을 입력해 탈퇴하고, 다음 요청부터 막히고, 누구에게도 보이지 않는다

**Independent Test**: 두 브라우저로 로그인해 한쪽에서 탈퇴하고, 다른 쪽이 막히는지와 관리자 목록에서 사라지는지 본다

### 백엔드

- [X] T014 [US1] B/common/security/access/: `AccountAccessChecker`(`isBlocked(Long userId)`), `AccountAccessFilter`(인증된 요청에서 막혔으면 `SecurityContext`를 비우고 `RestAuthenticationEntryPoint.ERROR_CODE_ATTRIBUTE`에 `ACCESS_TOKEN_EXPIRED`), `HttpSecurityCustomizer` 빈(`addFilterAfter(…, JwtAuthenticationFilter.class)`). 키트 `SecurityConfig`·`JwtAuthenticationFilter`는 고치지 않는다 (research R2)
- [X] T015 [US1] B/api/service/user/WithdrawnAccountChecker.java: `AccountAccessChecker` 구현, `existsWithdrawn` (T006·T014 뒤)
- [X] T016 [US1] B/api/realtime/: moa `WebSocketMessageBrokerConfigurer`(inbound 인터셉터: 탈퇴한 회원의 `CONNECT` 거절), 세션 기억(`WebSocketHandlerDecoratorFactory` + `SessionConnectedEvent`·`SessionDisconnectEvent`)과 `closeAll(userId)`. 키트 `WebSocketConfig`·`StompAuthInterceptor`는 고치지 않는다 (R3) — **이번 구현**: 회원↔세션은 Spring `SimpUserRegistry`, 인터셉터는 채널 맨 끝에 덧붙여 키트 다음에 돈다 (research R3)
- [X] T017 [US1] [haily, 승인됨] B/api/service/notification/NotificationService.java와 그 reader·writer·repository: `deleteSubscriptionsOf(userId)` (FR-021). 메서드 더하기만
- [X] T018 [US1] [haily, 승인됨] B/api/application/v1/notification/NotificationApplication.java: `notify` 맨 앞에 `MemberStatusService.isWithdrawn(userId)`면 null을 돌려주는 한 줄. 반환에 `@Nullable`. 주석에 근거(FR-022, `docs/privacy.md` 원칙) (T010 뒤)
- [X] T019 [US1] B/api/service/user/MemberWithdrawalService.java: `withdraw(userId, nickname, now)`. 잠긴 행(`findForUpdateIncludingWithdrawn`)에서 이미 탈퇴했으면 그 시각을 돌려주고, 아니면 `NicknamePolicy.normalize`로 맞춘 닉네임이 다르면 V001(칸 `nickname`), 같으면 `user.withdraw(now)` (R4)
- [X] T020 [US1] B/api/application/v1/user/MemberWithdrawalApplication.java: `TransactionTemplate` 안에서 `MemberActivityPolicy.requireMypageAllowed` → T019 → `AuthTokenService.revokeAll` → `deleteSubscriptionsOf` → `MemberWithdrawnEvent`. 커밋 뒤 `closeAll(userId)`. 로그에는 회원 ID만 (R4·R9, T016·T017·T019 뒤)
- [X] T021 [US1] B/api/controller/v1/user/ApiMyWithdrawalController.java(+ request, response): `POST /api/v1/users/me/withdrawal` `{nickname}` → 200 `{purgeAt}`, `RefreshTokenTransport.clear` (contracts/withdrawal-api.md)

### 프론트

- [ ] T022 [P] [US1] F/types/withdrawal.ts, F/api/withdrawal.ts(`withdraw`, `recovery`, `recover`), [공용] F/api/index.ts에 `withdrawal`
- [ ] T023 [US1] F/components/profile/mypage-frame.tsx(머리, 탭 링크 "프로필"·"계정 관리", 권한 확인. 지금 탭은 prop)와 F/app/(account)/account/profile/page.tsx가 그것을 쓰게. 저장하지 않은 프로필이 있으면 기존 `useUnsavedChanges`가 탭 링크를 잡는다. 기존 `profile-page.dom.test.tsx`가 그대로 통과해야 한다 (R10)
- [ ] T024 [US1] F/components/account-management/: `items.ts`(키·순서·컴포넌트 배열, 지금은 빈 배열), `withdraw-section.tsx`(맨 아래 오른쪽 빨간 단추), `withdraw-dialog.tsx`(안내 세 가지, 닉네임 입력, 같아야 켜짐, 닫으면 비움, V001은 칸 아래). F/app/(account)/account/profile/manage/page.tsx (contracts/screens.md)
- [ ] T025 [US1] 탈퇴 성공 뒤: `disablePush` → 로그아웃 → `/sign-in?withdrawnUntil=<시각>`. F/components/auth/withdrawal-notice.tsx와 [공용] F/app/(public)/sign-in/page.tsx, [공용] F/lib/next-path.ts(계정 관리·복구 경로), [공용] frontend/messages/ko.json

**Checkpoint**: 탈퇴하고 다음 요청부터 막힌다

---

## Phase 4: User Story 2 - 30일 안에 다시 로그인해 복구한다 (Priority: P1)

**Goal**: 본인 확인에 성공한 자리에서만 복구 안내를 주고, 복구하면 탈퇴 전 그대로다

**Independent Test**: 탈퇴한 회원으로 로그인해 안내를 보고 복구한 뒤 프로필과 관리자 목록을 본다

- [ ] T026 [US2] B/common/security/recovery/: `RecoveryCookies`(`withdrawal_recovery`, 회원 ID·탈퇴 시각 서명, 발급자 `…:recovery`, 10분, HttpOnly, `SameSite=Strict`, Path `/api/v1/auth/recovery`), `WithdrawnAccountException extends ServiceException`(회원 ID, 탈퇴 시각), 높은 순서의 `@RestControllerAdvice`(쿠키를 쓰고 같은 `ErrorResponse`). 키트 `GlobalExceptionHandler`·`ApiAuthTokenController`는 고치지 않는다 (R5)
- [ ] T027 [US2] [공용] B/api/service/user/UserService.java `authenticate`: 비밀번호 틀림 A006 → 잠김 A007 → 유예 중 `WithdrawnAccountException`(A401) → 파기 대기 A402. A401·A402도 `LoginAttemptedEvent.failed`. **탈퇴 확인은 비밀번호가 맞은 다음에만 한다**(사용자 조건). 틀린 비밀번호에서는 탈퇴를 보지 않고 지금과 같은 A006이다 (T006·T007·T010·T026 뒤)
- [ ] T028 [US2] [공용] B/api/service/user/SocialAccountService.java `findLinkedUser`를 `findIncludingWithdrawn`으로, B/api/dto/user/SocialLoginUserDto.java에 탈퇴 상태, B/api/application/v1/auth/SocialLoginApplication.java `logIn`: 잠김 → 유예 중(쿠키 + `/sign-in/recover`, 실패 이력 A401) → 파기 대기(`oauthError=purgePending`, A402) (010 콜백 표)
- [ ] T029 [US2] B/api/service/user/MemberWithdrawalService.java `restore(userId, withdrawnAt, now)`: 잠긴 행에서 쿠키의 탈퇴 시각과 같은지(아니면 A403), 잠김 A007, 유예 지남 A402, 아니면 `user.restore()` (T019 뒤)
- [ ] T030 [US2] B/api/application/v1/user/AccountRecoveryApplication.java: 트랜잭션 안에서 T029 → `revokeAll` → `MemberRestoredEvent`. 커밋 뒤 `AuthTokenService.issue`, `LoginAttemptedEvent.succeeded`. `getRecovery`(쿠키의 회원이 지금도 그 탈퇴 상태인지 보고 `purgeAt`)
- [ ] T031 [US2] B/api/controller/v1/auth/ApiAccountRecoveryController.java(`@PermitAll`): `GET`·`POST /api/v1/auth/recovery` (contracts/withdrawal-api.md)
- [ ] T032 [US2] F/app/(public)/sign-in/recover/page.tsx: 완전 삭제 예정 시각, "복구하기", "취소". A403이면 `/sign-in`. 성공하면 로그인과 같이 `next` 또는 첫 화면 (T022·T025 뒤)
- [ ] T033 [US2] [공용] F/app/(public)/sign-in/page.tsx: A401이면 `/sign-in/recover`(`next` 붙임). [공용] F/components/auth/oauth-error-alert.tsx에 `purgePending`, ko.json 문구

**Checkpoint**: 탈퇴와 복구가 함께 된다 (MVP)

---

## Phase 5: User Story 3 - 유예 기간이 지난 회원을 파기한다 (Priority: P1)

**Goal**: 스스로 탈퇴한 회원과 관리자가 지운 회원을 30일 뒤 `docs/privacy.md`대로 파기하고 이력을 남긴다

**Independent Test**: 시각을 앞당긴 회원들을 두고 정리를 돌려 저장된 것과 파기 이력을 본다

- [ ] T034 [US3] [haily, 승인됨] B/common/purgelog/PurgeTarget.java에 `MEMBER`(주석 "회원 (탈퇴·관리자 삭제)"), F/types/purge-log.ts, F/app/(admin)/admin/purge-logs/page.tsx `TARGETS`, ko.json `purgeLog` 종류 이름 "회원"
- [ ] T035 [US3] B/api/repository/{jpa,reader,writer}/user/: 파기 대상 ID(native, research R6의 두 조건, 시스템 계정 제외), 행 잠금과 다시 확인용 조회(native `for update`), 회원 행 덮기(native UPDATE, data-model "파기할 때"), 프로필·시간대·게임 정보·소셜·약관 동의 행 삭제. `tb_user_game_profile`은 엔터티가 없으므로 native DELETE
- [ ] T036 [US3] [haily, 승인됨] B/api/service/notification/NotificationService.java(알림·푸시 구독 지우기), B/api/service/notice/NoticeService.java(공지 확인 지우기)와 그 writer. 메서드 더하기만
- [ ] T037 [P] [US3] B/common/security/token/RefreshTokenEraser.java: 그 회원의 `tb_refresh_token` 행 DELETE. 키트 `RefreshTokenRepository`·`AuthTokenService`는 고치지 않는다
- [ ] T038 [US3] B/api/service/user/MemberPurgeService.java: 대상 ID 찾기, `eraseIfDue(userId, cutoff, now)`(잠금 → 다시 확인 → 회원 도메인 행 삭제 → 회원 행 덮기, 대상이 아니면 거짓)
- [ ] T039 [US3] B/api/application/v1/user/MemberPurgeApplication.java: 회원마다 `TransactionTemplate` 하나로 T038 → T036 → T037 → `PersonalDataPurgedEvent(MEMBER, COLUMN_ERASE, 1, null, userId)` → `MemberPurgedEvent`. 한 회원이 실패하면 회원 ID와 예외 클래스 이름만 로그에 남기고 계속한다. 파기한 수를 돌려준다 (R6·R9)
- [ ] T040 [US3] B/api/scheduler/MemberPurgeCleaner.java: `@Scheduled(cron = "${app.withdrawal.cleanup-cron:0 20 5 * * *}", zone = "${app.withdrawal.cleanup-zone:Asia/Seoul}")` (서버 시간대와 상관없이 Asia/Seoul 05:20). 파기한 수를 로그에, 유예가 끝난 뒤 4일이 지나도 남은 회원이 있으면 그 수를 ERROR로

**Checkpoint**: 두 종류의 회원이 파기되고 이력이 남는다

---

## Phase 6: User Story 4 - 다른 기능이 계정 관리와 탈퇴에 붙는다 (Priority: P2)

**Goal**: 탭 항목 구조와 세 이벤트가 다른 기능이 붙을 수 있는 모양인지 확인한다. 붙는 것은 없다

**Independent Test**: 시험용 항목과 시험용 리스너를 붙여 본다

- [ ] T041 [US4] F/components/account-management/items.ts: 항목을 순서대로 그리고 "탈퇴하기"를 늘 맨 아래에 두는지, 빈 배열이면 빈 자리가 없는지 (T024에서 만든 것을 다듬는다). 항목을 더하는 방법을 주석에 적는다
- [ ] T042 [US4] T009 이벤트 세 개의 Javadoc에 붙이는 방법을 적는다: `@TransactionalEventListener(AFTER_COMMIT)`, "탈퇴됨"은 되돌릴 수 있는 것만, 개인정보 삭제는 "파기됨"에, 실패 기록과 다시 시도는 붙이는 기능의 몫 (FR-017)

---

## Phase 7: Polish

- [ ] T043 backend/에서 `.\gradlew.bat spotlessApply`, `.\gradlew.bat build`
- [ ] T044 frontend/에서 `npm run gen:api`(사용자가 띄운 local 백엔드)로 F/types/schema.d.ts를 다시 만들고 T022의 손으로 쓴 타입을 바꾼 뒤 `npm run format`, `npm run check`
- [ ] T045 문서: `docs/privacy.md` 보관 기간 표의 "회원 탈퇴 스펙" 칸을 `018`로, specs/013-purge-log contracts·data-model의 값 목록에 `MEMBER`, specs/README.md 018 상태. 013(haily 문서)은 바뀐 줄을 보고한다
- [ ] T046 quickstart.md "수동 확인"을 사람이 직접 한다
- [ ] T047 plan.md "담당자 알림 목록"을 PR 설명에 적어 각 담당에게 알린다 (사람이 한다)

---

## Phase 8: Tests (맨 끝, CLAUDE.md)

**Purpose**: AC마다 테스트 하나 (원칙 II), 이름에 AC 번호

### AC와 테스트

| AC | 테스트 | 작업 |
|---|---|---|
| US1-AC1 탭과 빨간 단추 | 프론트 단위 | T053 |
| US1-AC2 확인 창 안내 | 프론트 단위 | T053 |
| US1-AC3 닉네임이 다르면 꺼짐 | 프론트 단위 | T053 |
| US1-AC4 서버가 닉네임 확인 | 인수 | T048 |
| US1-AC5 탈퇴·로그아웃·복구 기한 | 인수 + e2e | T048, T055 |
| US1-AC6 다른 브라우저 다음 요청부터 막힘 | 인수 (A002, 재발급 A003) | T048 |
| US1-AC7 다른 사람에게 안 보임, "탈퇴한 회원" | 인수(기본 조회) + 백엔드 단위(`MemberDisplay`) | T048, T052 |
| US1-AC8 유예 중 저장된 것 그대로, 파기 이력 없음 | 인수 | T048 |
| US1-AC9 취소·닫기 | 프론트 단위 | T053 |
| US1-AC10 관리자 계정 막힘 | 인수 (A201) | T048 |
| US1-AC11 푸시 구독 모두 지움, 복구해도 안 돌아옴 | 인수 | T048 |
| US1-AC12 유예 중 새 알림 없음 | 인수 | T048 |
| US1-AC13 관리자 목록에 없음, 복구하면 나옴 | 인수 | T048 |
| US2-AC1 소셜 로그인 → 복구 안내 | 인수 | T049 |
| US2-AC2 비밀번호 로그인 → 복구 안내 | 인수 | T049 |
| US2-AC3 틀린 비밀번호로는 모름 | 인수 (유예 중·파기 대기인 탈퇴 회원에 틀린 비밀번호 → 탈퇴하지 않은 회원·없는 아이디와 똑같은 A006 응답, 쿠키 없음, 접속 이력도 A006) | T049 |
| US2-AC4 복구하면 그대로 | 인수 + e2e | T049, T055 |
| US2-AC5 취소하면 탈퇴 상태 그대로 | 프론트 단위 | T054 |
| US2-AC6 복구한 회원은 파기되지 않음 | 인수 | T050 |
| US2-AC7 다시 탈퇴하면 다시 30일 | 인수 | T049 |
| US2-AC8 잠긴 회원 | 인수 (A007, 쿠키 없음) | T049 |
| US2-AC9 유예 지남·파기 전 | 인수 (A402, `purgePending`) | T049 |
| US2-AC10 본인 확인 없는 복구 | 인수 (쿠키 없음·위조·옛 쿠키 A403) | T049 |
| US2-AC11 관리자가 지운 회원은 복구 안 됨 | 인수 | T049 |
| US2-AC12 아이디 붙잡기 | 인수 (관리자 만들기 R002) | T049 |
| US2-AC13 이메일 붙잡기 | 인수 (`emailInUse`) | T049 |
| US3-AC1 회원 행의 개인정보 열 | 인수 (모든 문자열 칸에 원래 값 없음) | T050 |
| US3-AC2 회원만 쓰는 행 | 인수 | T050 |
| US3-AC3 알림·공지 확인·푸시·리프레시 토큰 | 인수 | T050 |
| US3-AC4 30일 안 된 회원·활동 회원 | 인수 | T050 |
| US3-AC5 파기 이력 | 인수 | T050 |
| US3-AC6 이력 실패 때 되돌림 | 인수 (`PurgeLogRepository` 저장을 실패시킴) | T051 |
| US3-AC7 한 명이 실패해도 나머지 | 인수 | T051 |
| US3-AC8 다시 돌아도 그대로 | 인수 | T050 |
| US3-AC9 접속 이력은 남음 | 인수 | T050 |
| US3-AC10 닉네임·태그 풀림 | 인수 | T050 |
| US3-AC11 관리자가 지운 회원·관리자 계정 | 인수 | T050 |
| US3-AC12 재가입 제한 기록 없음, 새 가입 | 인수 | T050 |
| US3-AC13 아이디·이메일 풀림 | 인수 | T050 |
| US3-AC14 로그에 개인정보 없음 | 인수 (로그 모으기) | T051 |
| US3-AC15 이미 지운 회원도 처음에 | 인수 | T050 |
| US3-AC16 관리자가 지운 소셜 회원의 재가입 | 인수 | T050 |
| US4-AC1 항목 순서와 맨 아래 단추 | 프론트 단위 | T053 |
| US4-AC2 항목이 없으면 빈 자리 없음 | 프론트 단위 | T053 |
| US4-AC3 탈퇴됨 | 인수 (시험용 리스너) | T051 |
| US4-AC4 처리가 실패해도 그대로 | 인수 (시험용 리스너) | T051 |
| US4-AC5 복구됨 | 인수 (시험용 리스너) | T051 |
| US4-AC6 파기됨 | 인수 (시험용 리스너) | T051 |

### 작업

- [ ] T048 BT/api/MemberWithdrawalAcceptanceTest.java(`AcceptanceTest` 상속, `CodeSeed.restore()`): US1의 "인수" AC. Edge: 동시에 두 번 탈퇴, 탈퇴와 겹친 재발급, 탈퇴 뒤 로그인 없이 되는 API
- [ ] T049 BT/api/MemberWithdrawalAcceptanceTest.java에 US2의 "인수" AC(US2-AC6 제외). Edge: 다시 탈퇴한 뒤의 옛 복구 쿠키, 유예 중 잠김
- [ ] T050 [P] BT/api/MemberPurgeAcceptanceTest.java: US2-AC6, US3-AC1~AC5·AC8~AC13·AC15·AC16. 파기를 동시에 두 번, 복구와 파기를 동시에, 지운 회원의 아이디를 새 계정이 쓰는 중
- [ ] T051 [P] BT/api/MemberPurgeFailureAcceptanceTest.java(스파이·시험용 리스너, 따로 뜨는 컨텍스트): US3-AC6·AC7·AC14, US4-AC3~AC6
- [ ] T052 [P] BT/api/service/user/: `MemberDisplayTest`, 복구 쿠키 단위 테스트(위조·만료·용도 다름)
- [ ] T053 [P] frontend/tests/unit/account-management.dom.test.tsx: US1-AC1·AC2·AC3·AC9, US4-AC1·AC2
- [ ] T054 [P] frontend/tests/unit/recover-page.dom.test.tsx: US2-AC5, A403이면 로그인 화면, 로그인 A401이면 복구 화면
- [ ] T055 [P] frontend/e2e/withdrawal.spec.ts: 핵심 흐름 둘 — US1-AC5(탈퇴 → 로그인 화면 안내), US2-AC4(로그인 → 복구 → 첫 화면)
- [ ] T056 [P] BT/api/WithdrawalRealtimeAcceptanceTest.java(`RealtimeAcceptanceTest` 상속): 탈퇴하면 열린 연결이 닫히고 다시 연결할 수 없다 (FR-006)
- [ ] T057 기존 인수·단위·e2e 테스트를 바꿀 것이 나오면 고치지 않고 이유와 함께 먼저 묻는다 (원칙 V)
- [ ] T058 PR 직전 `git fetch` 뒤 dev를 병합하고 마이그레이션 번호를 마지막 + 1로 정한다(파일 이름 바꾸기), `bash scripts/check-migration-numbers.sh`
- [ ] T059 `npm run check`
- [ ] T060 **백엔드 테스트 전체**: `.\gradlew.bat build`, `.\gradlew.bat dbTest`를 로컬에서 돌린다 (사용자 지시)
- [ ] T061 push 전에 **e2e 전체**(`npm run test:e2e`)를 로컬에서 돌린다. [공용] 프론트 파일(`sign-in`, `next-path.ts`, `ko.json`)을 바꿨기 때문이다
- [ ] T062 **첫 실행 확인** (구현 PR 병합 전): 관리자가 지운 지 30일이 넘은 회원 수를 세어 보여주고 사용자 확인을 받는다. 처음 돌 때 되돌릴 수 없이 한꺼번에 파기되기 때문이다 (FR-019). 조회만 한다: `SELECT COUNT(*) FROM tb_user WHERE role = 'USER' AND deleted_at <= NOW(6) - INTERVAL 30 DAY AND id <> 1`(시스템 계정 제외). 로컬 DB에서 세고, 데이터가 있는 다른 환경(개발·운영)이 있으면 사람이 같은 조회를 돌려 수를 알린다. 수를 PR 설명에 적고 haily에게 알린다

---

## Dependencies & Execution Order

- T001 → T002 → T003 → Phase 2. T002에서 조인에 조건이 붙지 않으면 멈춘다
- Phase 2: T004 → T005 → T006 → T010. T007·T008·T009·T011은 따로. T012 → T013이 끝나야 스토리를 시작한다
- US1: T014 → T015, T019 → T020 → T021. T017·T018은 T010 뒤. 프론트 T022 → T023 → T024 → T025
- US2는 US1의 T019·T022·T025 뒤. T026 → T027·T028 → T029 → T030 → T031, 프론트 T032·T033
- US3는 Phase 2만 끝나면 US1·US2와 따로 할 수 있다. T035 → T038 → T039 → T040
- US4는 T009·T024 뒤
- Phase 7 → Phase 8 → PR. T058은 PR 바로 전, T060·T061은 push 바로 전, T062는 PR 병합 전
- [공용] 작업은 T003의 승인 뒤에만 시작한다. [haily, 승인됨] 작업은 승인한 범위 안에서 한다

## Parallel Example

```text
Phase 2 안: T007, T008, T009, T011 (서로 다른 파일)
US1 안: T014·T016 (백엔드 보안)와 T022 (프론트 타입)
US3 안: T037과 T034
Phase 8 안: T050, T051, T052, T053, T054, T055, T056
```

## Implementation Strategy

1. **MVP**: Phase 1 → 2 → 3(US1) → 4(US2). 확인 창이 복구를 약속하므로 둘은 함께 나간다
2. US3(파기). 첫 탈퇴 30일 뒤부터 반드시 돌아야 하므로 같은 PR에 넣는다
3. US4 → Polish → 테스트(맨 끝) → PR
