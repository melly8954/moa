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

## Phase 6: 그룹 관리 (2026-11-18 FR-002·FR-004·FR-008, US1-AC1·AC6~AC10)

**Purpose**: 그룹을 DB에 두고 관리자가 화면에서 더하고 고치고 지운다. 기능이 쓰는 그룹은 시스템 그룹으로 잠근다 (research.md R9·R10). Phase 5의 "그룹 코드 등록"을 되돌린다. 구현 브랜치는 `feat/004-code-groups-on-screen`

**백엔드**

- [x] T013 backend/src/main/resources/db/migration/V2_8__code_groups_on_screen.sql로 `code_groups`를 다시 만든다. 열은 키트 `V1_4__codes.sql`의 `code_groups`에 `used_by VARCHAR(200) NULL`, `system BOOLEAN NOT NULL DEFAULT FALSE`를 더한다. 파일 첫 줄 주석에 `004-admin-codes` FR-004·FR-008을 적는다. 번호는 dev의 마지막 마이그레이션을 다시 보고 정한다 (data-model.md)
- [x] T014 `CodeGroup` 엔티티·저장소를 두고 backend/src/main/java/com/moa/api/codegroup/(`CodeGroupDefinition`·`CodeGroupSource`·`CodeGroupRegistry`·`LocalCodeGroups`)를 지운다. 코드 서비스·트리·항목 API·화면용 목록이 `code_groups`를 그룹 기준으로 쓰게 한다(없거나 지운 그룹은 R001). 트리·목록 응답에 `system`, 그룹은 만든 순서 (R9)
- [x] T015 `POST /api/v1/admin/code-groups`(일반 그룹, 형식 V001, 겹침 R002), `PUT /api/v1/admin/code-groups/{groupCode}`(이름·설명·쓰는 곳), `DELETE /api/v1/admin/code-groups/{groupCode}`(항목과 함께 소프트 삭제, 시스템 그룹 B001)를 둔다 (R9)
- [x] T016 backend/src/main/resources/db/localdata/에 local 시험 그룹 SQL(`INSERT IGNORE`)을 둔다: 시스템 그룹 `LOCAL_GAME_GENRE`, 일반 그룹 `LOCAL_REPORT_REASON` (R9)

**프론트**

- [x] T017 frontend/src/app/(admin)/admin/codes/: 머리 "그룹 추가", 그룹 이름을 누르면 그룹 드로어(그룹 코드는 만들 때만, 이름, 설명, 쓰는 곳), 일반 그룹 드로어의 "그룹 삭제"(확인 창에 항목 수), 시스템 그룹 행 잠금 표시와 드로어 안내, 빈 안내·머리 안내 문구(spec.md US1-AC6·FR-007). frontend/src/api·types의 code를 새 API에 맞추고 문구는 ko.json에 둔다 (R9)

**테스트 (마지막)**

- [x] T018 backend/src/test/java/com/moa/support/TestCodeGroups.java와 `CodeGroupRegistryTest`를 지우고, backend/src/test/java/com/moa/api/AdminCodesAcceptanceTest.java를 R10대로 고친다(US1-AC1·AC6 바뀜, US1-AC7~AC10 더함). 시스템 그룹은 JDBC로 넣는다 (원칙 V: 스펙이 바뀌어 고친다)
- [x] T019 frontend/e2e/code.spec.ts를 R10대로 고친다
- [ ] T020 backend/에서 `./gradlew spotlessApply`, `./gradlew build`, frontend/에서 `npm run format`, `npm run check`. 인수 테스트와 e2e는 PR CI로 본다. 그 뒤 quickstart.md "화면 확인"을 사람이 확인한다

---

## Phase 7: 흔한 방식과 두 칸 화면 (2026-11-20 FR-002·FR-004·FR-005·FR-007~FR-009, US1)

**Purpose**: 시스템 그룹과 삭제를 없애고 그룹·항목을 사용 여부로만 끈다. 쓰는 곳은 설명으로 합친다. 화면을 좌 트리 · 우 상세 두 칸으로 바꾼다 (research.md R11~R13). 구현 브랜치는 `feat/004-code-standard`. 화면 미리보기는 `design/code-tree-align` 브랜치의 커밋 `a947aab`(정렬)과 stash "preview: 004 공통코드 두 칸 화면"에 있다

**백엔드**

- [x] T021 backend/src/main/resources/db/migration/V2_9__code_groups_enabled.sql: `code_groups`에서 `used_by`·`system`을 지우고 `enabled BOOLEAN NOT NULL DEFAULT TRUE`를 더한다. 첫 줄 주석에 `004-admin-codes` FR-008을 적는다. 번호는 dev의 마지막 마이그레이션을 다시 보고 정한다 (data-model.md)
- [x] T022 `CodeGroup`·`CodeGroupDto`·요청·응답에서 `usedBy`·`system`을 빼고 `enabled`를 더한다. `PUT /api/v1/admin/code-groups/{groupCode}`가 `enabled`를 받는다. 그룹 지우기·항목 지우기·항목 일괄 지우기 API와 서비스 메서드를 없앤다. 화면용 목록은 꺼진 그룹이면 빈 목록(`includeDisabled=true`면 모든 항목) (R11)
- [x] T023 backend/src/main/resources/db/localdata/R__local_code_groups.sql을 새 열에 맞춘다 (시험 그룹 둘, 둘 다 사용 중)

**프론트**

- [x] T024 frontend/src/app/(admin)/admin/codes/: 두 칸 화면, 상세 패널(`DetailPanel`)과 좁은 폭 드로어, 저장하지 않은 변경 묻기, 일괄 처리 줄 고정(사용·사용 안 함), 그룹 체크(모두·일부, 항목 없으면 없음), 트리 안 그룹 추가, 그룹 패널(그룹 코드·이름·설명·사용, 삭제 없음), 꺼진 그룹 흐림, 코드 입력 거르기, 머리 안내(FR-007). 미리보기 stash를 바탕으로 한다 (R12)
- [x] T025 frontend/src/app/theme.css·globals.css에 `success` 색, src/components/kit/status-text.tsx positive를 초록으로, src/components/kit/bulk-toolbar.tsx 높이 고정. frontend/src/api·types의 code에서 그룹·항목 지우기와 `usedBy`·`system`을 빼고 `enabled`를 더한다

**테스트 (마지막)**

- [x] T026 backend/src/test/java/com/moa/api/AdminCodesAcceptanceTest.java를 R13대로 고친다(US1-AC1·AC4·AC6~AC9). backend/src/test/java/com/moa/api/CodeAcceptanceTest.java의 코드 지우기 부분을 지운다 (원칙 V: 스펙이 바뀌어 고친다)
- [x] T027 frontend/e2e/code.spec.ts를 R13대로 고친다(US1-AC1~AC10, 좁은 폭 드로어)
- [ ] T028 backend/에서 `./gradlew spotlessApply`, `./gradlew build`, frontend/에서 `npm run format`, `npm run check`. 인수 테스트와 e2e는 PR CI로 본다. 그 뒤 quickstart.md "화면 확인"을 사람이 확인한다 (T020의 화면 확인을 갈음한다)

---

## Dependencies & Execution Order

- Phase 5: `001-admin-menus` T071·T073 → T006 → T007 → T008 → T009 → T010 → T011 → T012
- Phase 6: T013 → T014 → T015 → T016 → T017 → T018 → T019 → T020. T012는 T020으로 갈음한다
- Phase 7: T021 → T022 → T023 → T024 → T025 → T026 → T027 → T028. T020의 화면 확인은 T028로 갈음한다
- T001 → T002 → T003 → T004, T005

## Parallel Example

없음. 테스트가 `AdminCodesAcceptanceTest.java` 한 파일에 있다.

## Implementation Strategy

1. T001~T002로 US1을 검증한다 (MVP이자 이 기능의 전부)
2. T003~T005로 마무리하고 구현 PR을 연다
