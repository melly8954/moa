---

description: "접속 이력 (admin-login-history) 작업 목록"
---

# Tasks: 접속 이력 (admin-login-history)

**Input**: `specs/005-admin-login-history/`의 설계 문서

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/admin-login-history.md

**Tests**: 원칙 II에 따라 AC마다 테스트를 하나 둔다. 이 기능에서 실제로 바꾸는 보관 기간(FR-002)도 테스트로 확인하고, 설정을 바꾸기 전에 테스트가 먼저 실패하는지 본다. 키트 코드와 마이그레이션은 고치지 않는다.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: 병렬 가능 (다른 파일, 의존 없음)
- **[Story]**: 작업이 속한 사용자 스토리

## Phase 1: Setup

**Purpose**: 테스트 클래스 준비

- [X] T001 `AcceptanceTest`를 상속한 `AdminLoginHistoryAcceptanceTest`를 backend/src/test/java/com/moa/api/AdminLoginHistoryAcceptanceTest.java에 만든다. 클래스 주석에 스펙 `005-admin-login-history`를 적는다. `TestUsers`, `JdbcTemplate`, `LoginHistoryCleaner`를 주입한다. 키트 `LoginHistoryAcceptanceTest`는 고치지 않는다

---

## Phase 2: Foundational

공통 선행 작업은 없다.

---

## Phase 3: User Story 1 - 관리자가 접속 이력을 본다 (Priority: P2) 🎯 MVP

**Goal**: 로그인 성공·실패가 접속 IP, 기기와 함께 남고, 관리자가 아이디로 찾으면 최신순으로 보인다는 것을 고정한다

**Independent Test**: `./gradlew dbTest --tests "*AdminLoginHistoryAcceptanceTest"`에서 US1-AC1이 통과한다

- [X] T002 [US1] `@DisplayName("US1-AC1 ...")` 테스트를 backend/src/test/java/com/moa/api/AdminLoginHistoryAcceptanceTest.java에 더한다. contracts/admin-login-history.md "US1-AC1" 순서를 따른다: `testUsers.create`로 그 아이디(예: 10L `kim`)와 다른 아이디(예: 11L `lee`)를 만들고, `POST /api/v1/auth/login`에 `User-Agent` 헤더(Windows Chrome 문자열, 키트 `LoginHistoryAcceptanceTest`의 `CHROME_WINDOWS`와 같은 값)와 접속 주소(`RequestPostProcessor`로 `remoteAddr`를 `203.0.113.10` 같은 문서용 주소로)를 주어 `kim` 실패 → `kim` 성공 → `lee` 성공 순으로 로그인한다. 관리자(`bearer(2L, "ADMIN")`)로 `GET /api/v1/admin/login-histories?loginId=kim`을 불러 200, `totalElements` 2, `content[0].success` true, `content[1].success` false와 `failureCode` `A006`, 두 건 모두 `ip`가 준 주소이고 `deviceType`·`os`·`browser`가 비어 있지 않음을 확인한다

**Checkpoint**: US1이 검증된다

---

## Phase 4: 보관 기간 90일 (FR-002)

**Purpose**: 보관 기간 기본값을 키트 기본 365일에서 90일로 바꾼다 (research.md R2, R4)

- [X] T003 `@DisplayName("FR-002 ...")` 테스트를 backend/src/test/java/com/moa/api/AdminLoginHistoryAcceptanceTest.java에 더한다. `JdbcTemplate`으로 `login_histories`에 생성 시각이 89일 전인 이력과 91일 전인 이력을 하나씩 넣고(컬럼은 키트 `LoginHistoryAcceptanceTest.history()`와 같게), `loginHistoryCleaner.deleteExpired()`를 부른 뒤 91일 전 이력만 지워지고 89일 전 이력은 남았는지 확인한다
- [X] T004 backend/에서 `./gradlew dbTest --tests "*AdminLoginHistoryAcceptanceTest"`를 돌려 T003이 실패하는지 확인한다 (지금 기본값 365일이라 91일 전 이력이 남는다). 실패하지 않으면 T003이 보관 기간을 확인하지 못하는 것이므로 T003부터 다시 본다
- [X] T005 [P] backend/src/main/resources/application.yaml의 `retention-days: ${LOGIN_HISTORY_RETENTION_DAYS:365}`를 `${LOGIN_HISTORY_RETENTION_DAYS:90}`으로 바꾸고, 바로 위 주석에 스펙 `005-admin-login-history` FR-002를 적는다
- [X] T006 [P] backend/.env.example의 `LOGIN_HISTORY_RETENTION_DAYS=365`를 `LOGIN_HISTORY_RETENTION_DAYS=90`으로 바꾼다
- [X] T007 backend/에서 `./gradlew dbTest --tests "*AdminLoginHistoryAcceptanceTest"`를 다시 돌려 US1-AC1과 FR-002 테스트가 모두 통과하는지 확인한다

**Checkpoint**: 보관 기간이 90일로 바뀌고 테스트로 확인된다

---

## Phase 5: Polish

**Purpose**: 전체 검사와 문서 정합성

- [X] T008 backend/에서 `./gradlew spotlessApply`, `./gradlew build dbTest`를 돌려 통과를 확인한다 (Docker 필요). 테스트가 실패하면 테스트를 고치지 말고 spec.md부터 확인한다 (원칙 V)
- [X] T009 specs/README.md 기능 지도에서 `005-admin-login-history`의 상태를 `기획 중`에서 `구현됨`으로 바꾼다
- [ ] T010 specs/005-admin-login-history/quickstart.md의 "화면 확인" 절차를 사람이 직접 수행한다. 시험 계정 비밀번호가 필요하므로 에이전트는 하지 않는다 (원칙 VI)

---

## Dependencies & Execution Order

- T001 → T002 → T003 → T004 → T005·T006 → T007 → T008 → T009, T010
- US1(T002)과 FR-002(T003~T007)는 서로 기대지 않지만 같은 테스트 파일을 고치므로 차례로 한다
- T004에서 FR-002 테스트가 실패하는 것을 먼저 본 뒤에 설정을 바꾼다

## Parallel Example

```text
T004에서 실패를 확인한 뒤:
  T005 application.yaml 기본값 90
  T006 .env.example 값 90
```

## Implementation Strategy

1. T001~T002로 US1을 검증한다 (MVP)
2. T003~T007로 보관 기간을 바꾸고 테스트로 확인한다 (실패 → 설정 변경 → 통과)
3. T008~T010으로 마무리하고 구현 PR을 연다
