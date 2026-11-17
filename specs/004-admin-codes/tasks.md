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

- [X] T001 `AcceptanceTest`를 상속한 `AdminCodesAcceptanceTest`를 backend/src/test/java/com/moa/api/AdminCodesAcceptanceTest.java에 만든다. 클래스 주석에 스펙 `004-admin-codes`를 적는다. ADMIN 요청은 `bearer(2L, "ADMIN")`으로 만든다. 키트 `CodeAcceptanceTest`는 고치지 않는다

---

## Phase 2: Foundational

공통 선행 작업은 없다. 마이그레이션에 코드 그룹 시드가 없어 복원할 것이 없다 (research.md R3).

---

## Phase 3: User Story 1 - 관리자가 공통코드를 관리한다 (Priority: P3) 🎯 MVP

**Goal**: 관리자가 코드 관리를 열면 spec.md FR-001 표의 코드 그룹이 보인다는 것을 고정한다

**Independent Test**: `./gradlew dbTest --tests "*AdminCodesAcceptanceTest"`에서 US1-AC1이 통과한다

- [X] T002 [US1] `@DisplayName("US1-AC1 ...")` 테스트를 backend/src/test/java/com/moa/api/AdminCodesAcceptanceTest.java에 더한다. 관리자로 `GET /api/v1/admin/code-groups`를 불러 200이고, 그룹 코드 목록(`$[*].groupCode`)이 spec.md FR-001 표와 같음을 확인한다. 지금 표는 "(아직 없음)"이므로 빈 목록(`$.length()` = 0)이다. 기대 목록은 테스트 안에 상수로 두고, FR-001 표에 그룹이 더해지면 함께 고친다고 주석으로 적는다

**Checkpoint**: US1이 검증된다

---

## Phase 4: Polish

**Purpose**: 전체 검사와 문서 정합성

- [X] T003 backend/에서 `./gradlew spotlessApply`, `./gradlew build dbTest`를 돌려 통과를 확인한다 (Docker 필요). 테스트가 실패하면 테스트를 고치지 말고 spec.md의 AC부터 확인한다 (원칙 V)
- [X] T004 specs/README.md 기능 지도에서 `004-admin-codes`의 상태를 `기획 중`에서 `구현됨`으로 바꾼다
- [ ] T005 specs/004-admin-codes/quickstart.md의 "화면 확인" 절차를 사람이 직접 수행한다. 시험 계정 비밀번호가 필요하므로 에이전트는 하지 않는다 (원칙 VI)

---

## Phase 5: 관리 화면 정리 (2026-11-15 FR-004~FR-007, US1 변경)

**Purpose**: 그룹 코드 등록, 항목 코드 자동 순번, 트리 표·일괄 처리 (research.md R5~R8). `001-admin-menus` T071·T073 뒤에 한다

**백엔드**

- [x] T006 backend/src/main/java/com/moa/api/codegroup/(또는 code 헬퍼 패키지)에 `CodeGroupDefinition`·`CodeGroupSource`·`CodeGroupRegistry`(시작 검사)를 만들고, backend/src/main/resources/db/migration/V2_7__code_groups_from_code.sql로 `code_groups`를 지운다. 코드 서비스·화면용 목록이 등록을 그룹 기준으로 쓰게 하고 그룹 만들기 API를 없앤다 (R5)
- [x] T007 항목 만들기에서 코드를 비우면 자동 순번(R6), `GET /admin/code-groups/tree`, `PUT .../codes/order`, `PUT .../codes/enabled`, `POST .../codes/bulk-delete`를 둔다 (R7)
- [x] T008 backend/src/test/java/com/moa/support/TestCodeGroups.java(테스트 전용 그룹 둘)와 backend/src/test/java/com/moa/api/AdminCodesAcceptanceTest.java의 US1-AC1~AC6을 R8대로 쓰고, 키트 CodeAcceptanceTest의 그룹 만들기 테스트를 지운다 (원칙 V)

**프론트**

- [x] T009 frontend/src/app/(admin)/admin/codes/: 트리 표(그룹 행 접기·펴기, 체크 칸 없음, 항목 체크박스·손잡이·이름·코드·사용), 그룹마다 "항목 추가", 항목 드로어(이름, 사용, 만들 때만 코드), 일괄 줄(한 그룹 안에서만), 그룹 안 끌기, 빈 안내, 머리 안내(FR-007 문구). 메뉴 관리 부품을 같이 쓰게 나눈다. frontend/src/api·types의 code를 새 API에 맞춘다 (R7)
- [x] T010 frontend/e2e/code.spec.ts를 R8대로 고친다 (원칙 V)

**마무리**

- [x] T011 backend/에서 `./gradlew spotlessApply`, `./gradlew build`, `./gradlew dbTest --tests "*Code*AcceptanceTest"`, frontend/에서 `npm run format`, `npm run check`. e2e와 dbTest 전체는 PR CI로 본다. (2026-11-17: 빠른 검사만 로컬에서 하고, 인수 테스트도 CLAUDE.md대로 PR CI로 본다)
- [ ] T012 specs/004-admin-codes/quickstart.md "화면 확인"을 새 화면에 맞추고 사람이 확인한다 (시험 그룹은 local 프로필에서만 등록한다)

---

## Dependencies & Execution Order

- Phase 5: `001-admin-menus` T071·T073 → T006 → T007 → T008 → T009 → T010 → T011 → T012
- T001 → T002 → T003 → T004, T005

## Parallel Example

없음. 테스트가 `AdminCodesAcceptanceTest.java` 한 파일에 있다.

## Implementation Strategy

1. T001~T002로 US1을 검증한다 (MVP이자 이 기능의 전부)
2. T003~T005로 마무리하고 구현 PR을 연다
