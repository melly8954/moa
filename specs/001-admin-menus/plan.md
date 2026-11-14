# Implementation Plan: 관리 메뉴와 접근 (admin-menus)

**Branch**: `feat/001-admin-menus` | **Date**: 2026-11-09 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/001-admin-menus/spec.md`

## Summary

관리 메뉴 5개의 처음 구성과 권한 없는 접근의 차단은 키트(`springboot/v0.3.1` admin 프리셋, `web-next/v0.3.0` admin 프리셋)에 이미 있다.
moa가 할 일은 키트 시드의 "관리" 그룹을 없애 메뉴를 최상위로 옮기고, 그 구성이 스펙과 같음을 AC 테스트로 고정하는 것이다.
운영 코드는 바꾸지 않고 마이그레이션 하나를 더한다 ([research.md](./research.md) R1).

## Technical Context

**Language/Version**: Java 21 (Spring Boot 4.1.2), TypeScript 5 (Next 16.3)

**Primary Dependencies**: psw-dev-kit menu·user-admin·code·login-history·rbac 헬퍼 (백엔드·프론트 짝)

**Storage**: MariaDB. 메뉴는 `menus` 테이블, 처음 구성은 Flyway 시드 (`V1_7`, `V1_7_1`)와 moa의 위치 변경 (`V2`)

**Testing**: 인수 테스트 `AcceptanceTest` (MockMvc + Testcontainers MariaDB, `./gradlew dbTest`). 화면은 키트 e2e(Playwright)

**Target Platform**: 웹 백오피스

**Project Type**: 웹 서비스 (모노레포 `backend/`, `frontend/`)

**Performance Goals**: 해당 없음. 새 요청 경로가 없다

**Constraints**: 키트 동작을 다시 적지 않는다 (원칙 III). 키트 코드는 고치지 않는다

**Scale/Scope**: 인수 테스트 클래스 1개, AC 5개

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| 원칙 | 판정 | 근거 |
|---|---|---|
| I. 스펙이 먼저다 | 통과 | 새 동작을 만들지 않는다. spec.md에 `[NEEDS CLARIFICATION]`이 없다 |
| II. AC마다 테스트 하나 | 통과 | US1-AC1~US2-AC3에 API 통합 테스트를 하나씩 둔다. 이름에 AC 번호를 적는다 |
| III. 키트를 따른다 | 통과 | 키트 헬퍼와 시드를 그대로 쓰고, 메뉴 위치만 moa 마이그레이션으로 바꾼다 (원칙 III의 "메뉴 구성"). 스펙의 머리에 출처가 있다 |
| IV. 기존 기능은 제자리에서 고친다 | 통과 | 새 기능 폴더를 만들지 않는다 |
| V. 테스트를 고쳐서 통과시키지 않는다 | 통과 | 키트 테스트를 고치지 않는다. moa AC 테스트는 스펙(FR-001)이 바뀐 만큼만 고친다 |
| VI. 시크릿을 다루지 않는다 | 통과 | 인수 테스트는 토큰을 직접 만든다. 로컬 계정 비밀번호를 쓰지 않는다 |

Phase 1 설계 뒤 다시 확인했다. 위반이 없다.

## Project Structure

### Documentation (this feature)

```text
specs/001-admin-menus/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/admin-access.md
└── tasks.md             # /speckit-tasks가 만든다
```

### Source Code (repository root)

```text
backend/src/main/resources/db/migration/
└── V2__flat_admin_menus.sql      # 관리 메뉴를 최상위로 옮기고 "관리" 그룹을 지운다
backend/src/test/java/com/moa/
├── api/AdminMenusAcceptanceTest.java   # 이 기능의 AC 테스트
├── api/AdminRolesAcceptanceTest.java   # 003. 메뉴 모양이 바뀐 만큼 US1-AC1을 고친다
└── support/MenuSeed.java              # 처음 구성 되돌리기에 V2를 더한다
```

**Structure Decision**: 인수 테스트는 키트와 같은 `com.moa.api` 패키지에 둔다. 운영 코드와 프론트는 바꾸지 않는다. 마이그레이션은 키트 것을 고치지 않고 moa 것을 하나 더한다.

## Complexity Tracking

위반이 없어 비워 둔다.
