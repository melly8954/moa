# Implementation Plan: 접속 이력 (admin-login-history)

**Branch**: `docs/005-admin-login-history` (설계), `feat/005-admin-login-history` (구현) | **Date**: 2026-11-10 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/005-admin-login-history/spec.md`

## Summary

로그인 성공·실패 기록, 관리자 목록 조회, 보관 기간이 지난 이력의 매일 정리는 키트(`springboot/v0.3.1` login-history 헬퍼, `web-next/v0.3.0` login-history 헬퍼)에 이미 있다.
moa가 바꿀 것은 보관 기간 하나다. 지금 기본값이 키트 기본 365일이라 FR-002의 90일로 고친다. 그리고 US1-AC1과 FR-002를 테스트로 고정한다 ([research.md](./research.md) R1, R2).

## Technical Context

**Language/Version**: Java 21 (Spring Boot 4.1.2), TypeScript 5 (Next 16.3)

**Primary Dependencies**: psw-dev-kit login-history 헬퍼 (`LoginHistoryCleaner`, `ApiAdminLoginHistoryController`)

**Storage**: MariaDB `login_histories`. 새 테이블과 컬럼은 없다

**Testing**: 인수 테스트 `AcceptanceTest` (MockMvc + Testcontainers MariaDB, `./gradlew dbTest`)

**Target Platform**: 웹 백오피스

**Project Type**: 웹 서비스 (모노레포 `backend/`, `frontend/`)

**Performance Goals**: 해당 없음. 정리는 매일 새벽 한 번 돈다 (키트 기본 04:40)

**Constraints**: 키트 동작을 다시 적지 않는다 (원칙 III). 키트 코드는 고치지 않고 설정만 바꾼다

**Scale/Scope**: 설정 파일 2개 수정, 인수 테스트 클래스 1개 (AC 1개 + FR-002 확인 1개)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| 원칙 | 판정 | 근거 |
|---|---|---|
| I. 스펙이 먼저다 | 통과 | 바꾸는 것은 FR-002가 정한 보관 기간뿐이다. spec.md에 `[NEEDS CLARIFICATION]`이 없다 |
| II. AC마다 테스트 하나 | 통과 | US1-AC1에 API 통합 테스트를 하나 둔다. 실제로 바꾸는 FR-002도 테스트로 확인한다 |
| III. 키트를 따른다 | 통과 | 키트가 열어 둔 설정(`app.login-history.retention-days`)으로 바꾼다 |
| IV. 기존 기능은 제자리에서 고친다 | 통과 | 새 기능 폴더를 만들지 않는다 |
| V. 테스트를 고쳐서 통과시키지 않는다 | 통과 | 키트 테스트를 고치지 않는다. 키트 테스트는 보관 기간에 기대지 않는다 |
| VI. 시크릿을 다루지 않는다 | 통과 | 보관 기간은 시크릿이 아니다. 인수 테스트는 토큰을 직접 만든다 |

Phase 1 설계 뒤 다시 확인했다. 위반이 없다.

## Project Structure

### Documentation (this feature)

```text
specs/005-admin-login-history/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/admin-login-history.md
└── tasks.md             # /speckit-tasks가 만든다
```

### Source Code (repository root)

```text
backend/
├── .env.example                                    # LOGIN_HISTORY_RETENTION_DAYS 365 → 90
├── src/main/resources/application.yaml             # retention-days 기본값 365 → 90
└── src/test/java/com/moa/api/
    └── AdminLoginHistoryAcceptanceTest.java        # 새 파일. AC와 FR-002 테스트
```

**Structure Decision**: 설정은 키트가 덧붙인 `application.yaml`의 login-history 문서에서 기본값만 바꾼다. 키트 코드(`LoginHistoryCleaner`)는 고치지 않는다.

## Complexity Tracking

위반이 없어 비워 둔다.
