# Data Model: 개인정보 파기 이력 (purge-log)

## 파기 이력 (`tb_purge_log`)

엔터티 `PurgeLog` (`common/purgelog`). `BaseTimeEntity`(로그·토큰: `created_at`, `updated_at`)를 상속하고 `@HardDelete(reason = "보관 기간(1년)이 지난 파기 이력은 정리기가 지운다. 개인정보가 없어 기록 없이 지운다")`를 붙인다. 이름·COMMENT는 `docs/db.md`를 따른다.

| 열 | 타입 | NULL | 뜻 (COMMENT) |
|---|---|---|---|
| `id` | `BIGINT AUTO_INCREMENT` | 아님 | PK |
| `target` | `VARCHAR(30)` | 아님 | `'파기 대상 종류 (LOGIN_HISTORY, NOTIFICATION, REFRESH_TOKEN, RICH_TEXT_IMAGE, MEMBER)'` |
| `purge_method` | `VARCHAR(20)` | 아님 | `'파기 방법 (ROW_DELETE, COLUMN_ERASE)'` |
| `purged_count` | `INT` | 아님 | `'파기 건수 (1 이상)'` |
| `actor_type` | `VARCHAR(20)` | 아님 | `'처리자 종류 (SCHEDULER, PERSON)'` |
| `actor_user_id` | `BIGINT` | 예 | `'처리한 관리자 ID (NULL: 정리기)'` |
| `target_user_id` | `BIGINT` | 예 | `'파기 대상 회원 ID (NULL: 개별 파기 아님)'` |
| `created_at` | `DATETIME(6)` | 아님 | 파기 일시 (감사 열이라 COMMENT 없음) |
| `updated_at` | `DATETIME(6)` | 아님 | 감사 열 |

- 테이블 COMMENT: `'개인정보 파기 이력 (개인정보 없음)'`
- 인덱스: `idx_purge_log_created_at (created_at)` — 정리(FR-006)와 최신순 목록. `idx_purge_log_target_created_at (target, created_at)` — 종류 조건 목록
- `actor_user_id`·`target_user_id`는 다른 행을 가리키지만 외래 키를 걸지 않는다(`docs/db.md`). 이번에는 쓰는 곳이 없어 따로 인덱스를 두지 않는다. 회원 탈퇴 스펙이 이 열로 찾게 되면 그때 더한다

### 값 (enum, `common/purgelog`)

| enum | 값 | 화면 이름 |
|---|---|---|
| `PurgeTarget` | `LOGIN_HISTORY`, `NOTIFICATION`, `REFRESH_TOKEN`, `RICH_TEXT_IMAGE`, `MEMBER` | 접속 이력, 알림, 리프레시 토큰, 본문 이미지, 회원(`018-member-withdrawal`) |
| `PurgeMethod` | `ROW_DELETE`, `COLUMN_ERASE` | 행 삭제, 개인정보 열 삭제 |
| `PurgeActor` | `SCHEDULER`, `PERSON` | 정리기, 사람 |

- 이번 정리기 4개는 모두 `ROW_DELETE`, `SCHEDULER`다. `COLUMN_ERASE`·`PERSON`은 회원 탈퇴 스펙이 쓴다(FR-005)

### 규칙

- `purged_count`가 0 이하인 이벤트는 저장하지 않는다 (FR-003)
- `actor_type`이 `SCHEDULER`면 `actor_user_id`는 NULL이다. `target_user_id`는 회원 파기(`018-member-withdrawal` FR-014)만 값이 있고 나머지 정리는 NULL이다
- 저장 뒤 고치지 않는다 (목록은 보기만, FR-009)
- `created_at`이 지금 − 365일보다 이전이면 정리 때 지운다 (FR-006)

## 이벤트

`PersonalDataPurgedEvent` (`common/purgelog`, record)

| 필드 | 뜻 |
|---|---|
| `target` | `PurgeTarget` |
| `method` | `PurgeMethod` |
| `count` | 지운 건수 |
| `actorUserId` | 처리한 관리자 ID (정리기는 null) |
| `targetUserId` | 대상 회원 ID (개별 파기가 아니면 null) |

- 편의 생성: `PersonalDataPurgedEvent.byScheduler(target, count)` (`ROW_DELETE`, 처리자 정리기)

## 권한·연결 화면·메뉴

| 무엇 | 값 |
|---|---|
| 권한 | `PURGE_LOG_READ` "파기 이력 조회" (묶음 "파기 이력") |
| 연결 화면 | `ADMIN_PURGE_LOGS` "파기 이력" `/admin/purge-logs` |
| 메뉴 (`tb_menu`) | 상위 운영(id 10), 이름 "파기 이력"(V6은 "파기 기록"으로 넣었고 2026-11-22 마이그레이션이 바꾼다), `screen_key` `ADMIN_PURGE_LOGS`, 아이콘 `file`, 순서 40, 대상 ADMIN |

## 설정

| 키 | 기본값 | 뜻 |
|---|---|---|
| `app.purge-log.retention-days` | 365 | 파기 이력 보관 일수 (FR-006) |
| `app.purge-log.cleanup-cron` | `0 10 5 * * *` | 파기 이력 정리 시각 |
