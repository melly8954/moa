# Implementation Plan: 개인정보 파기 기록 (purge-log)

**Branch**: `feat/013-purge-log` | **Date**: 2026-11-21 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/013-purge-log/spec.md`

## Summary

개인정보를 지우는 정리 4개(접속 이력, 알림, 리프레시 토큰, 본문 이미지)가 지운 뒤 같은 트랜잭션에서 이벤트를 내고, 받는 쪽이 1건 이상일 때 `tb_purge_log`에 파기 기록을 남긴다. 기록이 실패하면 삭제도 되돌려진다. 파기 기록은 1년 뒤 기록 없이 지운다. 관리 메뉴 "운영 → 파기 기록"에 접속 이력과 같은 틀의 업무 목록을 두고 `PURGE_LOG_READ` 권한으로 막는다 ([research.md](./research.md) R1~R8).

## Technical Context

**Language/Version**: Java 21 (Spring Boot 4.1.2), TypeScript 5 (Next 16.3)

**Primary Dependencies**: psw-dev-kit 정리기(`LoginHistoryCleaner`, `RefreshTokenCleaner`, `RichTextImageCleaner`), moa `NotificationApplication`, 키트 rbac 권한(`PermissionSource`), 연결 화면(`ScreenSource`), 프론트 키트 관리 화면 부품(`ListPage`, `SearchPanel`, `DataTable`)

**Storage**: MariaDB 새 테이블 `tb_purge_log`, `tb_menu`에 행 하나 ([data-model.md](./data-model.md))

**Testing**: 인수 테스트 `AcceptanceTest` (MockMvc + Testcontainers MariaDB, `./gradlew dbTest`, PR CI). 기록기 실패 전파는 단위 테스트

**Target Platform**: 웹 백오피스

**Project Type**: 웹 서비스 (모노레포 `backend/`, `frontend/`)

**Performance Goals**: 해당 없음. 정리는 매일 새벽 한 번이고 기록은 정리 하나에 한 행이다. 1년에 많아야 1,500행 정도다

**Constraints**: 키트 정리기는 이벤트 한 줄만 더한다 (원칙 III, 키트로 옮길 목록). 본문 이미지의 파일 순서는 키트 그대로다 (FR-004). 파기 기록에 개인정보를 넣지 않는다 (FR-002)

**Scale/Scope**: 백엔드 `common/purgelog` 새 패키지(엔터티, enum 3개, 이벤트, 기록기, 정리기, 저장소), `api`의 조회 계층 한 벌(컨트롤러, Application, Service, Reader, DTO), 권한·연결 화면 각 1개, 마이그레이션 1개, 정리 4곳 수정. 프론트 화면 1개, API·타입, 문구. 테스트 인수 1개 클래스 + 기존 2개 수정 + 단위 1개

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| 원칙 | 판정 | 근거 |
|---|---|---|
| I. 스펙이 먼저다 | 통과 | spec.md에 `[NEEDS CLARIFICATION]`이 없다. 메뉴를 더하므로 `001-admin-menus` FR-001·FR-008 표와 US1-AC1을 먼저 고쳤다 |
| II. AC마다 테스트 하나 | 통과 | US1-AC1~AC6, US2-AC1~AC3, US3-AC1마다 인수 테스트 하나 (R9) |
| III. 키트를 따른다 | 통과 | 키트 정리기의 삭제 동작은 바꾸지 않고 이벤트 한 줄만 더한다. 고친 내용은 키트로 옮길 목록에 올린다 (R10) |
| IV. 기존 기능은 제자리에서 고친다 | 통과 | 새 기능이라 새 폴더다. 메뉴 변경은 `001-admin-menus` spec.md를 제자리에서 고쳤다 |
| V. 테스트를 고쳐서 통과시키지 않는다 | 통과 | `AdminMenusAcceptanceTest`·`AdminRolesAcceptanceTest`는 스펙(001 US1-AC1)이 먼저 바뀌어 맞춘다. 키트 수용 테스트는 고치지 않는다 |
| VI. 시크릿을 다루지 않는다 | 통과 | 시크릿을 쓰지 않는다. 파기 기록에도 토큰 값을 넣지 않는다 |
| 기술 제약: `docs/db.md` | 통과 | `tb_purge_log` 단수, `BaseTimeEntity`, `@HardDelete(reason)`, 이름 붙인 인덱스, COMMENT, 마이그레이션 번호는 PR 직전 |
| 기술 제약: 목록 | 통과 | 업무 목록 (FR-009) |

Phase 1 설계 뒤 다시 확인했다. 위반이 없다.

## Project Structure

### Documentation (this feature)

```text
specs/013-purge-log/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/admin-purge-logs.md
├── checklists/requirements.md
└── tasks.md             # /speckit-tasks가 만든다
```

### Source Code (repository root)

```text
backend/src/main/java/com/moa/
├── common/purgelog/                                   # 새 패키지 (R1, R4, R5)
│   ├── PurgeLog.java                                  # 엔터티 (BaseTimeEntity, @HardDelete)
│   ├── PurgeTarget.java · PurgeMethod.java · PurgeActor.java
│   ├── PersonalDataPurgedEvent.java                   # 정리기가 내는 이벤트
│   ├── PurgeLogRecorder.java                          # @EventListener, 0건 무시, 실패 전파
│   ├── PurgeLogCleaner.java                           # 1년 지난 기록 정리 (이벤트 없음)
│   └── PurgeLogRepository.java
├── common/helper/loginhistory/LoginHistoryCleaner.java  # 키트. 이벤트 한 줄
├── common/security/token/RefreshTokenCleaner.java       # 키트. 이벤트 한 줄
├── common/helper/richtext/RichTextImageCleaner.java     # 키트. 이벤트 한 줄 (파일 순서 그대로)
├── api/application/v1/notification/NotificationApplication.java  # deleteExpired에서 이벤트
├── api/permission/PurgeLogPermissions.java            # PURGE_LOG_READ
├── api/screen/PurgeLogScreens.java                    # ADMIN_PURGE_LOGS, @Order(45)
├── api/controller/v1/admin/purgelog/                  # GET /api/v1/admin/purge-logs (+ request, response)
├── api/application/v1/purgelog/                       # PurgeLogApplication (+ query)
├── api/service/purgelog/PurgeLogService.java
├── api/repository/reader/purgelog/PurgeLogReader.java
└── api/dto/purgelog/PurgeLogDto.java

backend/src/main/resources/
├── application.yaml                                   # app.purge-log.retention-days·cleanup-cron
└── db/migration/V<다음 번호>__purge_log.sql            # tb_purge_log, 메뉴 행

backend/src/test/java/com/moa/
├── api/PurgeLogAcceptanceTest.java                    # 새 파일. US1~US3
├── api/AdminMenusAcceptanceTest.java                  # 운영 메뉴·연결 화면 목록에 파기 기록
├── api/AdminRolesAcceptanceTest.java                  # 관리 메뉴 트리에 파기 기록
└── common/purgelog/PurgeLogRecorderTest.java          # 0건 무시, 저장 실패 전파 (FR-003, FR-004)

frontend/
├── src/app/(admin)/admin/purge-logs/page.tsx          # 목록 화면 (R8)
├── src/api/purge-log.ts · src/api/index.ts            # API
├── src/types/purge-log.ts                             # 타입
└── messages/ko.json                                   # purgeLog 문구, 머리 설명 (FR-010)

docs/privacy.md                                        # "파기 기록"의 "구현 전" 줄을 013으로
```

**Structure Decision**: 기록·정리는 키트 정리기와 같은 `common`에 두어 키트 코드가 `api`에 기대지 않게 하고, 관리 화면 조회는 접속 이력과 같은 `api` 계층으로 둔다 (R4).

## Complexity Tracking

위반이 없어 비워 둔다.
