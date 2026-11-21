# Research: 개인정보 파기 기록 (purge-log)

Technical Context에 미결은 없다. 아래는 코드를 확인해 정한 것이다 (2026-11-21).

## R1. 정리기가 파기 기록을 남기는 방법

- **Decision**: 정리기가 지운 뒤 같은 트랜잭션 안에서 이벤트 `PersonalDataPurgedEvent(대상 종류, 방법, 건수)`를 낸다. `common/purgelog`의 `PurgeLogRecorder`가 `@EventListener`(동기)로 받아 파기 기록을 저장한다. 건수가 0 이하면 저장하지 않는다(FR-003). 저장이 실패하면 예외를 그대로 던져 정리의 트랜잭션이 되돌려진다(FR-004)
- **Rationale**: 키트 정리기 3개(`common/helper/loginhistory`, `common/security/token`, `common/helper/richtext`)가 고칠 것은 이벤트를 내는 한 줄뿐이다. 키트 쪽은 받는 쪽이 없어도 동작하므로 키트로 옮기기 쉽다. 기록 규칙(0건 무시, 실패 전파)은 받는 쪽 한 곳에 모인다
- **Alternatives considered**: 정리기가 기록기를 직접 부르기 — 키트 코드가 moa 클래스에 묶인다. 정리기에 AOP 걸기 — 지운 건수를 얻을 수 없다(키트 정리기는 `void`). 로그 줄을 잡아 저장하기 — 로그 문구가 바뀌면 깨진다
- **접속 이력 기록기와 다른 점**: `LoginHistoryRecorder`는 저장 실패를 삼킨다(로그인을 막지 않으려고). 파기 기록은 반대로 실패를 던진다(기록 없는 삭제를 막으려고, FR-004)

## R2. 트랜잭션 경계

- **Decision**: 이벤트는 각 정리의 트랜잭션 안에서 낸다
  - 접속 이력·리프레시 토큰·본문 이미지: 키트 정리기의 `@Transactional deleteExpired()` 안, 삭제 바로 뒤
  - 알림: `NotificationCleaner`(api.scheduler)는 트랜잭션이 없고 `NotificationApplication.deleteExpired`가 `@Transactional`이다. 이벤트는 Application에서 낸다 (아키텍처 규칙: 트랜잭션 경계는 Application)
- **Rationale**: 동기 `@EventListener`는 내는 쪽의 트랜잭션에 함께 들어간다. 기록 저장 실패가 삭제를 되돌린다

## R3. 본문 이미지의 파일 순서 (FR-004)

- **Decision**: 키트 `RichTextImageService.deleteExpiredTemps()`를 고치지 않는다. 지금 순서(파일을 먼저 지우고, 지운 파일의 행만 지움)대로 두고, 정리기가 돌려받은 건수로 이벤트를 낸다
- **Rationale**: 파일 삭제가 실패하면 행이 남아 다음 날 다시 지운다(`Files.deleteIfExists`라 이미 없는 파일도 성공). 기록 저장이 실패하면 행 삭제만 되돌려지고, 다음 날 그 행을 지우며 기록한다. 개인정보(파일)가 남는 경우가 없다
- **Alternatives considered**: 행·기록을 확정한 뒤 파일 삭제(처음 A안) — 파일 삭제가 실패하면 가리키는 행이 없어 다시 찾지 못한다 (스펙 FR-004, 2026-11-21 결정)

## R4. 저장 위치와 계층

- **Decision**: 파기 기록 엔터티·저장소·기록기·정리기는 `common/purgelog`에 둔다. 관리 화면 조회는 접속 이력과 같은 계층으로 `api`에 둔다 (컨트롤러 → Application → Service → Reader → `common/purgelog`의 Repository)
- **Rationale**: 키트 정리기(`common`)가 `api`에 기대지 않게 한다. 접속 이력(`common/helper/loginhistory` + `api` 조회)과 같은 모양이라 읽기 쉽다. `ArchitectureTest`의 계층 규칙은 `api.*`만 보므로 `common`은 규칙 밖이다
- **Alternatives considered**: `api/entity`에 엔터티 — 키트 정리기가 `api`에 기대게 된다

## R5. 파기 기록 정리 (FR-006)

- **Decision**: `common/purgelog/PurgeLogCleaner`가 매일 `app.purge-log.cleanup-cron`(기본 `0 10 5 * * *`)에 `created_at`이 `app.purge-log.retention-days`(기본 365)일보다 오래된 기록을 지운다. 이 정리는 이벤트를 내지 않는다
- **Rationale**: 다른 정리(04:30~05:00)가 끝난 뒤에 돈다. 파기 기록에는 개인정보가 없어 기록 없이 지운다 (`docs/privacy.md`)

## R6. 목록 API (FR-008, FR-009)

- **Decision**: `GET /api/v1/admin/purge-logs` — 조건 `from`·`to`(날짜, 포함), `target`(대상 종류, 없으면 전체), `page`·`size`. 최신순. `from`이 `to`보다 늦으면 V001. 권한 `PURGE_LOG_READ` ([contracts/admin-purge-logs.md](./contracts/admin-purge-logs.md))
- **Rationale**: 접속 이력 API(`ApiAdminLoginHistoryController`)와 같은 조건·쪽 나눔·오류 규칙이다. 화면도 같은 검색 틀을 쓴다(FR-009: 처음 기간은 접속 이력과 같게)

## R7. 메뉴·연결 화면·권한 (FR-007, FR-008)

- **Decision**
  - 권한: `PurgeLogPermissions.READ = "PURGE_LOG_READ"`, 이름 "파기 기록 조회", 묶음 "파기 기록"
  - 연결 화면: `PurgeLogScreens` — `ADMIN_PURGE_LOGS`, "파기 기록", `/admin/purge-logs`, `@Order(45)` (접속 이력 40 다음)
  - 메뉴: 마이그레이션 `V<다음 번호>__purge_log.sql`이 `tb_purge_log`를 만들고, `tb_menu`에 운영(id 10) 아래 순서 40, 아이콘 `file`인 "파기 기록" 행을 더한다. 번호는 PR 직전에 정한다 (`docs/db.md`)
- **Rationale**: 접속 이력의 권한·연결 화면·시드와 같은 방식이다. 아이콘은 이미 있는 목록(`frontend/src/components/menu/icons.ts`)에서 고른다

## R8. 화면 (FR-009, FR-010)

- **Decision**: `frontend/src/app/(admin)/admin/purge-logs/page.tsx`를 접속 이력 화면과 같은 틀(`ListPage`, `SearchPanel`의 `PeriodFilter`·`ChoiceFilter`, `DataTable`, `useListSearch`)로 만든다. 문구는 `messages/ko.json`의 `purgeLog`에 둔다. 머리 설명은 FR-010 문구이고 "접속 이력"은 `/admin/login-histories` 링크다
- **Rationale**: 같은 모양이라 관리자가 따로 배우지 않는다. 문구 검사(`check:i18n`)를 지킨다

## R9. 테스트 (원칙 II)

- **Decision**: `backend/src/test/java/com/moa/api/PurgeLogAcceptanceTest.java`(`@Tag("db")`)에 US1-AC1~AC6, US2-AC1~AC3, US3-AC1을 하나씩 둔다. 정리기는 직접 부른다(접속 이력 FR-002 테스트와 같음)
  - 메뉴가 하나 늘어 기존 테스트가 바뀐다: `AdminMenusAcceptanceTest`(운영 메뉴 목록, 연결 화면 키 목록), `AdminRolesAcceptanceTest`(관리 메뉴 트리). 스펙 `001-admin-menus` US1-AC1을 먼저 고쳤으므로 원칙 V에 어긋나지 않는다
  - e2e는 더하지 않는다(핵심 흐름이 아니다). `frontend/e2e/menu.spec.ts`가 메뉴 목록을 보는지 확인하고, 보면 맞춘다
- **Alternatives considered**: FR-004(저장 실패 때 되돌림)를 자동 테스트로 — 저장을 일부러 실패시키는 장치가 필요하다. AC가 아니라 FR이므로 단위 테스트(기록기가 예외를 던지는지) 하나로 확인한다

## R10. 키트로 옮길 것

- `PersonalDataPurgedEvent`와 키트 정리기 3개의 이벤트 한 줄 (login-history, rich-text 헬퍼, 골격 토큰)
- 받는 쪽(`PurgeLogRecorder`, 엔터티, 화면)은 moa에 둔다. 키트에 넣을지는 키트로 옮길 때 정한다
- 구현 PR에서 키트로 옮길 목록 메모에 올린다
