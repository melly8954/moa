# Implementation Plan: 운영자 역할 (admin-roles)

**Branch**: `docs/003-admin-roles` (설계), `feat/003-admin-roles` (구현) | **Date**: 2026-11-10 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/003-admin-roles/spec.md`

## Summary

역할 만들기, 사용자에게 역할 주기, 권한 합집합 판정은 키트(`springboot/v0.3.1` rbac 헬퍼, `web-next/v0.3.0` rbac 헬퍼)에 이미 있다. 처음 배포할 때 역할 시드도 없다 (FR-003).
moa가 할 일은 "접속 이력만 보는 운영자" 흐름을 AC 테스트로 고정하는 것이다. 운영 코드와 마이그레이션은 바꾸지 않는다 ([research.md](./research.md) R1).

**메뉴 트리와 사용자 화면 (2026-11-15, FR-004~FR-008)**: 역할 창의 권한을 관리 메뉴 트리로 고르게 하고, 역할 관리에 사용자별 역할 탭과 역할별 구성원을 더한다. 역할 저장(권한 키 목록)과 권한 판정은 그대로다. 백엔드 API 다섯 개(트리, 사용자별 목록, 구성원 조회·더하기·빼기)와 화면을 더하고, 사용자 관리의 역할 주기를 뺀다. 테이블은 바뀌지 않는다. `001-admin-menus`의 프로그램(그 research R13~R15)에 기댄다. 키트 rbac 헬퍼를 moa에서 먼저 고치고 키트로 옮긴다 ([research.md](./research.md) R6~R11).

## Technical Context

**Language/Version**: Java 21 (Spring Boot 4.1.2), TypeScript 5 (Next 16.3)

**Primary Dependencies**: psw-dev-kit rbac 헬퍼 (`AccessRolePermissionChecker`), menu 헬퍼 (내 메뉴 거르기)

**Storage**: MariaDB `access_roles`, `user_access_roles` (`V1_8__access_roles.sql`). 새 테이블과 컬럼은 없다

**Testing**: 인수 테스트 `AcceptanceTest` (MockMvc + Testcontainers MariaDB, `./gradlew dbTest`). 화면은 키트 e2e(Playwright)

**Target Platform**: 웹 백오피스

**Project Type**: 웹 서비스 (모노레포 `backend/`, `frontend/`)

**Performance Goals**: 해당 없음. 권한은 요청마다 쿼리 1개로 조회한다 (키트)

**Constraints**: 키트 동작을 다시 적지 않는다 (원칙 III). 역할 등급 규칙은 두지 않는다 (스펙 Assumptions)

**Scale/Scope**: 인수 테스트 클래스 1개(AC 3개), 테스트 지원 클래스 1개

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| 원칙 | 판정 | 근거 |
|---|---|---|
| I. 스펙이 먼저다 | 통과 | 새 동작을 만들지 않는다. 메뉴 트리와 사용자 화면(2026-11-15)은 spec.md FR-004~FR-008을 먼저 고친 뒤 한다. spec.md에 `[NEEDS CLARIFICATION]`이 없다 |
| II. AC마다 테스트 하나 | 통과 | US1-AC1~AC3에 API 통합 테스트를 하나씩 둔다. 이름에 AC 번호를 적는다 |
| III. 키트를 따른다 | 통과 | 키트 rbac 헬퍼를 그대로 쓴다. 스펙의 머리에 출처가 있다 |
| IV. 기존 기능은 제자리에서 고친다 | 통과 | 새 기능 폴더를 만들지 않는다 |
| V. 테스트를 고쳐서 통과시키지 않는다 | 통과 | 001 테스트는 시드 복원 코드를 지원 클래스로 옮기기만 하고, 확인 내용은 바꾸지 않는다 (R3) |
| VI. 시크릿을 다루지 않는다 | 통과 | 인수 테스트는 토큰을 직접 만든다 |

Phase 1 설계 뒤 다시 확인했다. 위반이 없다.

## Project Structure

### Documentation (this feature)

```text
specs/003-admin-roles/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/admin-roles.md
└── tasks.md             # /speckit-tasks가 만든다
```

### Source Code (repository root)

```text
backend/src/test/java/com/moa/
├── support/MenuSeed.java                 # 새 파일. 메뉴 처음 구성 복원 (001과 공유)
└── api/
    ├── AdminRolesAcceptanceTest.java     # 새 파일. 이 기능의 AC 테스트
    └── AdminMenusAcceptanceTest.java     # 시드 복원을 MenuSeed로 바꾼다
```

메뉴 트리와 사용자 화면 (2026-11-15):

```text
backend/src/main/java/com/moa/api/
├── controller/v1/admin/role/ApiAdminRoleController.java        # permission-tree, roles/users, members (R6~R8)
├── controller/v1/admin/role/{request,response}/**              # 트리·사용자·구성원 응답, 구성원 더하기 요청
├── application/v1/role/AccessRoleApplication.java              # 메뉴 트리와 프로그램을 읽어 권한 트리를 만든다 (R6)
├── service/role/AccessRoleService.java                         # 구성원 더하기·빼기, ADMIN 대상 B001 (R7, R8)
└── repository/reader/role/AccessRoleReader.java, reader/user/** # 역할 있는 사용자·ADMIN, 검색, 구성원
backend/src/test/java/com/moa/api/AdminRolesAcceptanceTest.java # US2-AC1~US4-AC3 (R11)
frontend/src/
├── api/role.ts, types/role.ts                                  # 트리, 사용자별, 구성원
├── app/(admin)/admin/roles/page.tsx                            # 두 탭 (R10)
├── app/(admin)/admin/roles/{role-dialog, members-dialog, user-roles-tab}.tsx
├── app/(admin)/admin/users/user-extras.tsx                     # 역할 주기 빼기 (R9)
└── components/role/user-roles.tsx                              # 사용자별 역할 탭으로 옮긴 뒤 지운다
frontend/e2e/roles.spec.ts                                      # 트리 체크, 두 탭, 구성원
```

**Structure Decision**: 인수 테스트는 `com.moa.api`, 공유 지원 코드는 `com.moa.support`에 둔다. 처음 도입 때는 운영 코드, 프론트, 마이그레이션을 바꾸지 않았다. 메뉴 트리와 사용자 화면(2026-11-15)은 키트 rbac 헬퍼 파일 안에서 고치고, 키트로 옮길 목록에 올린다 (원칙 III).

## Complexity Tracking

위반이 없어 비워 둔다.
