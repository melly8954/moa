---
description: "Task list for 013-purge-log"
---

# Tasks: 개인정보 파기 이력 (purge-log)

**Input**: Design documents from `specs/013-purge-log/`

**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [research.md](./research.md), [data-model.md](./data-model.md), [contracts/admin-purge-logs.md](./contracts/admin-purge-logs.md)

**Tests**: 원칙 II(AC마다 테스트 하나)에 따라 둔다. 저장소 규칙(`CLAUDE.md`)대로 기능 코드를 먼저 끝내고 테스트 작업은 맨 끝(Phase 7)에 둔다.

**Organization**: 사용자 스토리별로 나눈다. 경로는 저장소 루트 기준이다. 백엔드 패키지 루트는 `backend/src/main/java/com/moa/`(아래 `…/moa/`)다.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: 다른 파일이고 끝나지 않은 작업에 기대지 않아 함께 할 수 있다
- **[Story]**: 그 작업이 속한 사용자 스토리 (US1, US2, US3)

---

## Phase 1: Setup

**Purpose**: 설정 키

- [X] T001 backend/src/main/resources/application.yaml의 `app:` 아래에 `purge-log:` 문서를 더한다: `retention-days: ${PURGE_LOG_RETENTION_DAYS:365}`, `cleanup-cron: ${PURGE_LOG_CLEANUP_CRON:0 10 5 * * *}`. 위 주석에 "파기 이력 보관 기간과 정리 시각. specs/013-purge-log FR-006"을 적는다 (data-model.md "설정")

---

## Phase 2: Foundational (모든 스토리가 기대는 것)

**Purpose**: 파기 이력 저장 단위. 끝나야 US1~US3을 시작한다

- [X] T002 [P] …/moa/common/purgelog/PurgeTarget.java enum을 만든다: `LOGIN_HISTORY`, `NOTIFICATION`, `REFRESH_TOKEN`, `RICH_TEXT_IMAGE` (data-model.md "값")
- [X] T003 [P] …/moa/common/purgelog/PurgeMethod.java enum을 만든다: `ROW_DELETE`, `COLUMN_ERASE`
- [X] T004 [P] …/moa/common/purgelog/PurgeActor.java enum을 만든다: `SCHEDULER`, `PERSON`
- [X] T005 …/moa/common/purgelog/PurgeLog.java 엔터티를 만든다: `@Table(name = "tb_purge_log")`, `BaseTimeEntity` 상속, `@HardDelete(reason = "보관 기간(1년)이 지난 파기 이력은 정리기가 지운다. 개인정보가 없어 기록 없이 지운다")`. 필드 `target`(`@Enumerated(STRING)`, 길이 30, NOT NULL), `purgeMethod`(길이 20, NOT NULL), `purgedCount`(int, NOT NULL, 1 이상), `actorType`(길이 20, NOT NULL), `actorUserId`(Long, NULL), `targetUserId`(Long, NULL). 저장 뒤 고치는 메서드를 두지 않는다. 생성자는 `PersonalDataPurgedEvent`를 받는다 (data-model.md "규칙": `SCHEDULER`면 두 ID는 NULL)
- [X] T006 …/moa/common/purgelog/PersonalDataPurgedEvent.java record를 만든다: `target`, `method`, `count`, `actorUserId`, `targetUserId`. 편의 생성 `byScheduler(PurgeTarget target, int count)`(`ROW_DELETE`, 두 ID null)를 둔다 (data-model.md "이벤트")
- [X] T007 …/moa/common/purgelog/PurgeLogRepository.java (`JpaRepository<PurgeLog, Long>`)를 만든다: 목록 조회(기간·종류 조건, 최신순, 쪽 나눔)는 접속 이력 `LoginHistoryRepository`의 조회 방식을 따르고, `@Modifying` `deleteCreatedBefore(LocalDateTime cutoff)`를 둔다
- [X] T008 backend/src/main/resources/db/migration/V<다음 번호>__purge_log.sql을 만든다. 번호는 PR 직전에 dev를 병합하고 마지막 번호 + 1로 정한다(`docs/db.md`). 머리 주석 `-- moa: 개인정보 파기 이력 테이블과 관리 메뉴. 스펙 specs/013-purge-log (FR-001·FR-007)`. `tb_purge_log`를 data-model.md 표 그대로 만든다(열 COMMENT 문구 그대로, 테이블 COMMENT `'개인정보 파기 이력 (개인정보 없음)'`, `idx_purge_log_created_at (created_at)`, `idx_purge_log_target_created_at (target, created_at)`, `DEFAULT` 없음)

**Checkpoint**: 파기 이력을 저장할 수 있다

---

## Phase 3: User Story 1 - 정리기가 개인정보를 지우면 파기 이력이 남는다 (Priority: P1) 🎯 MVP

**Goal**: 정리 4개가 1건 이상 지우면 같은 트랜잭션에서 파기 이력이 남는다. 0건이면 남지 않는다. 기록 저장이 실패하면 삭제도 되돌려진다

**Independent Test**: 보관 기간이 지난 데이터를 넣고 정리기를 부른 뒤 `tb_purge_log`를 본다 (Phase 7 T027)

- [X] T009 [US1] …/moa/common/purgelog/PurgeLogRecorder.java를 만든다: `@Component`, `@EventListener`(동기)로 `PersonalDataPurgedEvent`를 받아 `count`가 0 이하면 아무것도 하지 않고(FR-003), 아니면 `PurgeLog`를 저장한다. 예외를 잡지 않는다 — 저장 실패가 정리의 트랜잭션을 되돌린다(FR-004, research.md R1). 클래스 주석에 `LoginHistoryRecorder`와 반대로 실패를 던지는 이유를 적는다
- [X] T010 [P] [US1] …/moa/common/helper/loginhistory/LoginHistoryCleaner.java(키트)에 `ApplicationEventPublisher`를 받고, `deleteExpired()`에서 지운 뒤 `publisher.publishEvent(PersonalDataPurgedEvent.byScheduler(PurgeTarget.LOGIN_HISTORY, deleted))`를 더한다. 삭제·로그는 그대로 둔다. 바로 위에 `// moa: 파기 이력 (specs/013-purge-log). 키트로 옮길 것` 주석
- [X] T011 [P] [US1] …/moa/common/security/token/RefreshTokenCleaner.java(키트)에 같은 방식으로 `PurgeTarget.REFRESH_TOKEN` 이벤트를 더한다
- [X] T012 [P] [US1] …/moa/common/helper/richtext/RichTextImageCleaner.java(키트)에 같은 방식으로 `PurgeTarget.RICH_TEXT_IMAGE` 이벤트를 더한다. `RichTextImageService.deleteExpiredTemps()`가 돌려준 수를 변수로 받아 로그와 이벤트에 함께 쓴다. `RichTextImageService`는 고치지 않는다 — 파일을 먼저 지우는 순서 그대로 (FR-004, research.md R3)
- [X] T013 [P] [US1] …/moa/api/application/v1/notification/NotificationApplication.java의 `@Transactional deleteExpired(int retentionDays)`에서 지운 수를 받아 `PurgeTarget.NOTIFICATION` 이벤트를 낸 뒤 돌려준다 (이미 있는 `ApplicationEventPublisher`를 쓴다. 트랜잭션 경계는 Application, research.md R2)

**Checkpoint**: 정리 4개가 파기 이력을 남긴다

---

## Phase 4: User Story 2 - 관리자가 파기 이력을 목록으로 확인한다 (Priority: P2)

**Goal**: 관리 메뉴 "운영 → 파기 이력"에서 기간·종류로 찾는다. `PURGE_LOG_READ`가 없으면 막힌다

**Independent Test**: 파기 이력을 여러 날짜·종류로 넣고 관리자와 권한 없는 운영자로 목록을 연다 (Phase 7 T028~T030)

- [X] T014 [P] [US2] …/moa/api/permission/PurgeLogPermissions.java를 만든다: `LoginHistoryPermissions`와 같은 모양으로 `READ = "PURGE_LOG_READ"`, 이름 "파기 이력 조회", 묶음 "파기 이력"
- [X] T015 [P] [US2] …/moa/api/screen/PurgeLogScreens.java를 만든다: `@Order(45)`, `ScreenDefinition.admin("ADMIN_PURGE_LOGS", "파기 이력", "/admin/purge-logs", PurgeLogPermissions.READ)` (T014 뒤)
- [X] T016 [US2] T008의 마이그레이션 파일 끝에 메뉴 행을 더한다: `tb_menu`에 `parent_id` 10(운영), 이름 '파기 이력', `screen_key` 'ADMIN_PURGE_LOGS', 아이콘 'file', `sort_order` 40, `enabled` TRUE, `audience` 'ADMIN', `visibility` NULL, 감사 열은 시스템 계정 1 (V2__seed.sql 접속 이력 행과 같은 모양, id는 자동)
- [X] T017 [P] [US2] …/moa/api/dto/purgelog/PurgeLogDto.java와 …/moa/api/repository/reader/purgelog/PurgeLogReader.java를 만든다: Reader는 `PurgeLogRepository`로 기간(`from` 0시 ~ `to` 다음 날 0시 전)·종류 조건 쪽을 최신순으로 돌려준다 (`LoginHistoryReader`와 같은 모양)
- [X] T018 [US2] …/moa/api/service/purgelog/PurgeLogService.java를 만든다: `from`이 `to`보다 늦으면 V001 `ServiceException`, 아니면 Reader 결과를 DTO 쪽으로 (`LoginHistoryService`와 같은 모양)
- [X] T019 [US2] …/moa/api/application/v1/purgelog/PurgeLogApplication.java와 `query/PurgeLogQuery.java`(`from`, `to`, `target`)를 만든다: `@Transactional(readOnly = true) getPage(query, pageable)`
- [X] T020 [US2] …/moa/api/controller/v1/admin/purgelog/ApiAdminPurgeLogController.java와 `request/PurgeLogSearchRequest.java`, `response/PurgeLogListResponse.java`를 만든다: `@RequirePermission(PurgeLogPermissions.READ)`, `GET /api/v1/admin/purge-logs`, 쿼리·응답·오류는 contracts/admin-purge-logs.md 그대로(`createdAt`은 `ApiTimes.of`로 시간대 포함). `ApiAdminLoginHistoryController`와 같은 모양
- [X] T021 [P] [US2] frontend/src/types/purge-log.ts, frontend/src/api/purge-log.ts를 만들고 frontend/src/api/index.ts의 `api.v1`에 `purgeLog`를 더한다 (`login-history.ts`와 같은 모양, 백엔드가 떠 있으면 `npm run gen:api` 뒤 `Schema<"PurgeLogListResponse">` 사용)
- [X] T022 [US2] frontend/messages/ko.json에 `purgeLog` 문구를 더하고 frontend/src/app/(admin)/admin/purge-logs/page.tsx를 만든다: 접속 이력 화면(`login-histories/page.tsx`)과 같은 틀. 조건 기간(`PeriodFilter`, 처음 값은 접속 이력과 같게 `from: ""`, `to: ""`)과 대상 종류(`ChoiceFilter`: 전체·접속 이력·알림·리프레시 토큰·본문 이미지), 칸 일시·대상 종류·건수·방법·처리자, 늘 최신순. 머리 설명은 FR-010 "보관 기간이 지나 지운 개인정보의 종류와 건수를 1년 동안 보여요. 지운 내용은 남기지 않아요. 로그인 기록은 <link>접속 이력</link>에서 봐요." (링크 `/admin/login-histories`)

**Checkpoint**: 관리자가 파기 이력을 찾아본다

---

## Phase 5: User Story 3 - 1년이 지난 파기 이력은 지워진다 (Priority: P3)

**Goal**: 파기 이력도 365일이 지나면 매일 지워지고, 이 정리는 기록을 남기지 않는다

**Independent Test**: 366일 전·364일 전 기록을 넣고 정리기를 부른다 (Phase 7 T031)

- [X] T023 [US3] …/moa/common/purgelog/PurgeLogCleaner.java를 만든다: `@Component`, `@Value("${app.purge-log.retention-days:365}")`, `@Transactional @Scheduled(cron = "${app.purge-log.cleanup-cron:0 10 5 * * *}") deleteExpired()`가 `deleteCreatedBefore(now − retentionDays)`를 부르고 지운 수를 로그에 남긴다. 이벤트를 내지 않는다(FR-006). 클래스 주석에 "여러 서버에서 같이 돌아도 결과가 같다"와 기록을 남기지 않는 이유

**Checkpoint**: 파기 이력이 1년 뒤 사라진다

---

## Phase 6: Polish

**Purpose**: 문서 정합성

- [X] T024 docs/privacy.md "파기 이력"의 "어디에 어떻게 남길지는 구현하는 스펙이 정한다 (구현 전. 그때까지는 정리기 로그가 30일 동안 남는다)"를 "`specs/013-purge-log`가 `tb_purge_log`에 남기고 관리 메뉴 '파기 이력'에서 본다"로 고친다. "개인정보 보호책임자는 파기 결과(정리기 로그, 개별 파기 이력)를 확인한다"를 "파기 이력 화면에서 확인한다"로 고친다
- [X] T025 specs/README.md 기능 지도에서 `013-purge-log`의 상태를 `기획 중`에서 `구현됨`으로 바꾼다
- [X] T026 구현 PR 설명에 키트로 옮길 것을 적는다: `PersonalDataPurgedEvent`와 키트 정리기 3개(login-history, rich-text, 골격 토큰)의 이벤트 한 줄 (research.md R10)

---

## Phase 7: Tests (맨 끝, `CLAUDE.md`)

**Purpose**: AC마다 테스트 하나 (원칙 II). `dbTest`는 PR CI가 돌린다

- [X] T027 [US1] backend/src/test/java/com/moa/api/PurgeLogAcceptanceTest.java(`@Tag("db")`, `AcceptanceTest` 상속)를 만들고 US1 테스트를 더한다: `@DisplayName("US1-AC1 …")`부터 `"US1-AC6 …"`까지 하나씩. AC1·AC5는 `JdbcTemplate`으로 366일 전 접속 이력을 넣고(또는 넣지 않고) `LoginHistoryCleaner.deleteExpired()`, AC2는 알림 91일 전 + `NotificationCleaner`, AC3은 2일 전 만료 토큰 + `RefreshTokenCleaner`, AC4는 보관 시간이 지난 TEMP 이미지(파일 포함) + `RichTextImageCleaner`, AC6은 AC1 기록의 열이 data-model.md 열뿐이고 지운 아이디·IP 값이 어느 칸에도 없는지
- [X] T028 [US2] 같은 파일에 `"US2-AC1 …"`: 날짜·종류가 다른 기록 셋을 넣고 관리자 토큰으로 `GET /api/v1/admin/purge-logs?from=&to=&target=`이 그 기록만 최신순으로 돌려주는지
- [X] T029 [US2] 같은 파일에 `"US2-AC2 …"`: `PURGE_LOG_READ`만 묶은 역할의 운영자가 관리 메뉴에서 운영 아래 "파기 이력"만 보고 목록을 연다 (`AdminRolesAcceptanceTest`의 "보안 담당" 준비 방식)
- [X] T030 [US2] 같은 파일에 `"US2-AC3 …"`: 권한 없는 운영자의 목록 요청이 403
- [X] T031 [US3] 같은 파일에 `"US3-AC1 …"`: 366일 전·364일 전 기록 + `PurgeLogCleaner.deleteExpired()` → 366일 전만 지워지고 `tb_purge_log` 행 수가 하나 줄 뿐 새 행이 없다
- [X] T032 [P] backend/src/test/java/com/moa/common/purgelog/PurgeLogRecorderTest.java 단위 테스트: 건수 0이면 저장을 부르지 않는다(FR-003), 저장이 예외를 던지면 기록기도 던진다(FR-004)
- [X] T033 [P] backend/src/test/java/com/moa/api/AdminMenusAcceptanceTest.java와 AdminRolesAcceptanceTest.java의 관리 메뉴·연결 화면 기대값에 "파기 이력"(`ADMIN_PURGE_LOGS`)을 접속 이력 다음에 더한다. `001-admin-menus` US1-AC1을 먼저 고쳤으므로 원칙 V에 어긋나지 않는다. frontend/e2e/menu.spec.ts가 운영 메뉴 목록을 확인하면 함께 맞춘다
- [ ] T034 backend/에서 `./gradlew spotlessApply`, `./gradlew build`, frontend/에서 `npm run format`, `npm run check`를 돌린다. dev를 병합해 T008 마이그레이션 번호를 정하고 `bootRun`으로 한 번 띄워 Flyway·`ddl-auto: validate` 통과를 본다(`docs/db.md`)
- [ ] T035 quickstart.md "화면 확인"을 사람이 직접 한다. 시험 계정 비밀번호가 필요하므로 에이전트는 하지 않는다 (원칙 VI)

---

## Dependencies & Execution Order

- Phase 1 → Phase 2 → (US1, US2, US3) → Phase 6 → Phase 7
- US1·US2·US3은 Phase 2에만 기댄다. 서로 기대지 않는다
- US2 안: T014 → T015, T008 → T016, T017 → T018 → T019 → T020, T021 → T022
- Phase 7 테스트는 기능 코드가 다 끝난 뒤에 쓴다 (`CLAUDE.md`)

## Parallel Example

```text
Phase 2: T002, T003, T004 (enum 셋)
US1:    T010, T011, T012, T013 (정리 4곳, 서로 다른 파일. T009 뒤)
US2:    T014, T017, T021 (권한, Reader·DTO, 프론트 타입·API)
Phase 7: T032, T033
```

## Implementation Strategy

1. Phase 1~2, US1(T009~T013)로 파기 이력이 남게 한다 (MVP: 법이 요구하는 기록·관리)
2. US2로 관리 화면을 더한다 (보호책임자 확인)
3. US3으로 1년 정리를 더한다
4. Phase 6 문서, Phase 7 테스트와 검사 뒤 구현 PR을 연다
