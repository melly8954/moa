# Implementation Plan: 관리자·운영자 계정 (admin-users)

**Branch**: `docs/002-admin-users` (설계), `feat/002-admin-users` (구현) | **Date**: 2026-11-10 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/002-admin-users/spec.md`

## Summary

첫 관리자 부트스트랩, 운영자 계정 만들기, 임시 비밀번호와 첫 로그인 변경 안내는 키트(`springboot/v0.3.1` user-admin 헬퍼와 골격, `web-next/v0.3.0` user-admin 헬퍼)에 이미 있다.
moa가 할 일은 스펙의 두 흐름을 AC 테스트로 고정하는 것이다. 운영 코드와 마이그레이션은 바꾸지 않는다 ([research.md](./research.md) R1).

**관리 화면 정리 (2026-11-15, FR-006~FR-008, US3)**: 사용자 관리 목록에 업무 역할 열을 더하고(응답에 `accessRoles`), 행의 "관리" 메뉴를 드로어로 옮기며 만들기도 드로어에서 한다. `001-admin-menus` Phase 11의 공용 드로어·머리 안내와 `003-admin-roles`의 업무 역할 조회를 쓴다 ([research.md](./research.md) R6~R8). 구현 브랜치는 `feat/002-user-console`

**관리자 계정과 드로어 역할 주기 (2026-11-21, FR-001·FR-003·FR-005~FR-008, US1~US4)**: `ADMIN`을 관리자 계정, `USER`를 회원으로 쓰고 화면에서 "계정 종류"로 부른다. 계정 종류 바꾸기와 관리자 계정 만들기를 사용자 관리 권한으로 열고, 최고 관리자가 아니면 내 권한 안쪽의 관리자 계정만 다루게 바꾼다(`UserAdminService`). 응답에 `superAdmin`·`protection`을 더하고, 드로어에 역할 체크 칸을 둔다(역할 저장은 `003`의 지금 API). 최고 관리자 판정과 역할 규칙은 `003-admin-roles` research R16·R17에 기대므로 같은 구현 브랜치 `feat/003-admin-accounts`에서 한다 ([research.md](./research.md) R9~R12)

**최고 관리자 계정 고정과 개발자 DB 절차 (2026-11-21·22, FR-007·FR-009, US1-AC2·AC3, US4-AC3, Clarifications)**: 최고 관리자 계정 행은 본인 이름 바꾸기만 되고 나머지는 누구든 B001로 막는다. 되돌리기는 서버 코드가 아니라 `backend/src/main/resources/db/ops/`의 SQL 파일 두 개(비밀번호 되돌리기, 최고 관리자 바꾸기)와 quickstart 안내로 둔다. 인수 테스트가 같은 파일을 실행해 확인한다. `003-admin-roles` Phase 8과 한 구현 브랜치 `feat/003-single-super-admin` ([research.md](./research.md) R13~R15)

**소셜 계정 표시와 비밀번호 초기화 이유 (2026-11-24, FR-004·FR-007, US3-AC5)**: 응답 `UserAdminResponse`에 연결된 제공자 `socialProviders`를 더해 목록의 아이디 뒤 배지와 드로어의 로그인 방식·비밀번호 줄 설명에 쓴다. 연결은 `010-social-login`의 `tb_user_social_account`에서 한 쪽의 계정을 한 번에 읽는다. 마이그레이션은 없다. 구현 브랜치 `feat/002-social-mark` ([research.md](./research.md) R17)

## Technical Context

**Language/Version**: Java 21 (Spring Boot 4.1.2), TypeScript 5 (Next 16.3)

**Primary Dependencies**: psw-dev-kit user-admin 헬퍼, 골격의 첫 관리자 부트스트랩 (`AdminBootstrapRunner`)

**Storage**: MariaDB `tb_user` 테이블. 새 테이블과 컬럼은 없다. 기존 운영자의 `USER` → `ADMIN` 옮기기는 `003-admin-roles`의 V7이 한다

**Testing**: 인수 테스트 `AcceptanceTest` (MockMvc + Testcontainers MariaDB, `./gradlew dbTest`). 화면은 키트 e2e(Playwright)

**Target Platform**: 웹 백오피스

**Project Type**: 웹 서비스 (모노레포 `backend/`, `frontend/`)

**Performance Goals**: 해당 없음. 새 요청 경로가 없다

**Constraints**: 키트 동작을 다시 적지 않는다 (원칙 III). 임시 비밀번호는 메일·문자로 보내지 않는다 (스펙 Assumptions)

**Scale/Scope**: 인수 테스트 클래스 1개, AC 2개

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| 원칙 | 판정 | 근거 |
|---|---|---|
| I. 스펙이 먼저다 | 통과 | 새 동작을 만들지 않는다. spec.md에 `[NEEDS CLARIFICATION]`이 없다 |
| II. AC마다 테스트 하나 | 통과 | US1-AC1, US2-AC1에 API 통합 테스트를 하나씩 둔다. 이름에 AC 번호를 적는다 |
| III. 키트를 따른다 | 통과 | 키트 헬퍼와 부트스트랩을 그대로 쓴다. 스펙의 머리에 출처가 있다 |
| IV. 기존 기능은 제자리에서 고친다 | 통과 | 새 기능 폴더를 만들지 않는다 |
| V. 테스트를 고쳐서 통과시키지 않는다 | 통과 | 키트 테스트를 고치지 않는다. 새 테스트만 더한다 |
| VI. 시크릿을 다루지 않는다 | 통과 | 테스트 비밀번호는 테스트 안에서만 쓰는 값이고, 운영 설정 값을 읽지 않는다 |

Phase 1 설계 뒤 다시 확인했다. 위반이 없다.

## Project Structure

### Documentation (this feature)

```text
specs/002-admin-users/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/admin-accounts.md
└── tasks.md             # /speckit-tasks가 만든다
```

### Source Code (repository root)

```text
backend/src/test/java/com/moa/api/
└── AdminUsersAcceptanceTest.java   # 새 파일. 이 기능의 AC 테스트
```

**Structure Decision**: 인수 테스트는 키트와 같은 `com.moa.api` 패키지에 둔다. 운영 코드, 프론트, 마이그레이션은 바꾸지 않는다.

## Complexity Tracking

위반이 없어 비워 둔다.
