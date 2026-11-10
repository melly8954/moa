# Implementation Plan: 공통코드 (admin-codes)

**Branch**: `docs/004-admin-codes` (설계), `feat/004-admin-codes` (구현) | **Date**: 2026-11-10 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/004-admin-codes/spec.md`

## Summary

코드 그룹·코드 관리와 화면용 목록은 키트(`springboot/v0.3.1` code 헬퍼, `web-next/v0.3.0` code 헬퍼)에 이미 있다. 마이그레이션에 코드 그룹 시드가 없어 처음에는 비어 있고(FR-001), 캐시도 켜져 있지 않다(FR-003).
moa가 할 일은 "관리자가 코드 관리를 열면 FR-001 표의 그룹이 보인다"를 AC 테스트로 고정하는 것이다. 운영 코드와 마이그레이션은 바꾸지 않는다 ([research.md](./research.md) R1).

## Technical Context

**Language/Version**: Java 21 (Spring Boot 4.1.2), TypeScript 5 (Next 16.3)

**Primary Dependencies**: psw-dev-kit code 헬퍼 (`CodeApplication`, `ApiAdminCodeController`, `ApiCodeController`)

**Storage**: MariaDB `code_groups`, `codes` (`V1_4__codes.sql`). 새 테이블과 컬럼은 없다

**Testing**: 인수 테스트 `AcceptanceTest` (MockMvc + Testcontainers MariaDB, `./gradlew dbTest`)

**Target Platform**: 웹 백오피스

**Project Type**: 웹 서비스 (모노레포 `backend/`, `frontend/`)

**Performance Goals**: 해당 없음. 캐시 없이 조회한다 (FR-003)

**Constraints**: 키트 동작을 다시 적지 않는다 (원칙 III). 프리셋 밖 헬퍼(redis)를 넣지 않는다

**Scale/Scope**: 인수 테스트 클래스 1개, AC 1개

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| 원칙 | 판정 | 근거 |
|---|---|---|
| I. 스펙이 먼저다 | 통과 | 새 동작을 만들지 않는다. spec.md에 `[NEEDS CLARIFICATION]`이 없다 |
| II. AC마다 테스트 하나 | 통과 | US1-AC1에 API 통합 테스트를 하나 둔다. 이름에 AC 번호를 적는다 |
| III. 키트를 따른다 | 통과 | 키트 code 헬퍼를 그대로 쓴다. 스펙의 머리에 출처가 있다 |
| IV. 기존 기능은 제자리에서 고친다 | 통과 | 새 기능 폴더를 만들지 않는다 |
| V. 테스트를 고쳐서 통과시키지 않는다 | 통과 | 키트 테스트를 고치지 않는다. 새 테스트만 더한다 |
| VI. 시크릿을 다루지 않는다 | 통과 | 인수 테스트는 토큰을 직접 만든다 |

Phase 1 설계 뒤 다시 확인했다. 위반이 없다.

## Project Structure

### Documentation (this feature)

```text
specs/004-admin-codes/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/admin-codes.md
└── tasks.md             # /speckit-tasks가 만든다
```

### Source Code (repository root)

```text
backend/src/test/java/com/moa/api/
└── AdminCodesAcceptanceTest.java   # 새 파일. 이 기능의 AC 테스트
```

**Structure Decision**: 인수 테스트는 `com.moa.api`에 둔다. 운영 코드, 프론트, 마이그레이션은 바꾸지 않는다.

## Complexity Tracking

위반이 없어 비워 둔다.
