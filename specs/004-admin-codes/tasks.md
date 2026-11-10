---

description: "공통코드 (admin-codes) 작업 목록"
---

# Tasks: 공통코드 (admin-codes)

**Input**: `specs/004-admin-codes/`의 설계 문서

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/admin-codes.md

**Tests**: 원칙 II에 따라 AC마다 테스트를 하나 둔다. 동작은 키트에 이미 있으므로 이 기능의 작업은 테스트가 전부다 (research.md R1). 운영 코드, 프론트, 마이그레이션은 고치지 않는다.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: 병렬 가능 (다른 파일, 의존 없음)
- **[Story]**: 작업이 속한 사용자 스토리

모든 테스트는 한 파일에 들어가므로 [P] 작업은 없다.

## Phase 1: Setup

**Purpose**: 테스트 클래스 준비

- [ ] T001 `AcceptanceTest`를 상속한 `AdminCodesAcceptanceTest`를 backend/src/test/java/com/moa/api/AdminCodesAcceptanceTest.java에 만든다. 클래스 주석에 스펙 `004-admin-codes`를 적는다. ADMIN 요청은 `bearer(2L, "ADMIN")`으로 만든다. 키트 `CodeAcceptanceTest`는 고치지 않는다

---

## Phase 2: Foundational

공통 선행 작업은 없다. 마이그레이션에 코드 그룹 시드가 없어 복원할 것이 없다 (research.md R3).

---

## Phase 3: User Story 1 - 관리자가 공통코드를 관리한다 (Priority: P3) 🎯 MVP

**Goal**: 관리자가 코드 관리를 열면 spec.md FR-001 표의 코드 그룹이 보인다는 것을 고정한다

**Independent Test**: `./gradlew dbTest --tests "*AdminCodesAcceptanceTest"`에서 US1-AC1이 통과한다

- [ ] T002 [US1] `@DisplayName("US1-AC1 ...")` 테스트를 backend/src/test/java/com/moa/api/AdminCodesAcceptanceTest.java에 더한다. 관리자로 `GET /api/v1/admin/code-groups`를 불러 200이고, 그룹 코드 목록(`$[*].groupCode`)이 spec.md FR-001 표와 같음을 확인한다. 지금 표는 "(아직 없음)"이므로 빈 목록(`$.length()` = 0)이다. 기대 목록은 테스트 안에 상수로 두고, FR-001 표에 그룹이 더해지면 함께 고친다고 주석으로 적는다

**Checkpoint**: US1이 검증된다

---

## Phase 4: Polish

**Purpose**: 전체 검사와 문서 정합성

- [ ] T003 backend/에서 `./gradlew spotlessApply`, `./gradlew build dbTest`를 돌려 통과를 확인한다 (Docker 필요). 테스트가 실패하면 테스트를 고치지 말고 spec.md의 AC부터 확인한다 (원칙 V)
- [ ] T004 specs/README.md 기능 지도에서 `004-admin-codes`의 상태를 `기획 중`에서 `구현됨`으로 바꾼다
- [ ] T005 specs/004-admin-codes/quickstart.md의 "화면 확인" 절차를 사람이 직접 수행한다. 시험 계정 비밀번호가 필요하므로 에이전트는 하지 않는다 (원칙 VI)

---

## Dependencies & Execution Order

- T001 → T002 → T003 → T004, T005

## Parallel Example

없음. 테스트가 `AdminCodesAcceptanceTest.java` 한 파일에 있다.

## Implementation Strategy

1. T001~T002로 US1을 검증한다 (MVP이자 이 기능의 전부)
2. T003~T005로 마무리하고 구현 PR을 연다
