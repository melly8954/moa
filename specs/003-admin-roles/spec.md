# Feature Specification: 운영자 역할 (admin-roles)

**Feature Branch**: `feat/003-admin-roles`

**Created**: 2026-11-07

**Status**: Draft

**Input**: User description: "관리자가 권한 일부를 묶은 역할을 만들어 운영자에게 주고, 운영자는 그 역할의 메뉴와 기능만 쓴다."

**출처**: psw-dev-kit springboot rbac 헬퍼. 태그 `springboot/v0.3.1`, 화면은 `web-next/v0.3.0`.
키트 동작(역할 만들기·바꾸기·지우기, 사용자에게 역할 주기, 권한 합집합, 권한 상승 방지)은 키트 문서와 키트 수용 테스트가 기준이다 (원칙 III).

**배경** (2026-11-07 결정): 예전 기획은 운영자가 한 명이라 관리자 등급을 나누지 않기로 했다. 운영자가 늘어날 때를 대비해 rbac를 넣어 역할로 권한을 나눈다.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - 운영자는 받은 역할의 메뉴와 기능만 쓴다 (Priority: P2)

관리자가 역할 관리에서 권한 일부를 묶은 역할(예: 접속 이력만 보는 "보안 담당")을 만들어 운영자에게 준다. 그 운영자는 그 권한의 메뉴와 기능만 쓴다.

**Why this priority**: 운영자가 늘어날 때를 위한 기능이다. 첫 출시에는 관리자 한 명으로도 운영할 수 있다.

**Independent Test**: 접속 이력 조회 권한만 묶은 역할을 운영자에게 주고, 그 운영자로 로그인해 확인한다.

**Acceptance Scenarios**:

1. **US1-AC1** **Given** 접속 이력 조회 권한만 가진 역할을 받은 운영자가 로그인했다, **When** 백오피스에 들어간다, **Then** 관리 메뉴에 접속 이력만 보인다
2. **US1-AC2** **Given** 같은 운영자다, **When** 접속 이력을 연다, **Then** 목록이 보인다
3. **US1-AC3** **Given** 같은 운영자다, **When** 사용자 관리 주소로 직접 들어간다, **Then** 권한이 없다는 안내와 함께 막힌다

---

### Edge Cases

- 역할 관리 권한만 받은 운영자: 자기가 가진 권한까지만 역할에 담을 수 있다 (키트 권한 상승 방지)
- 역할을 지우면: 그 역할을 받은 운영자에게서도 빠지고, 다음 요청부터 그 메뉴와 기능을 못 쓴다 (키트 동작)

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: 관리자는 관리 메뉴의 권한(`001-admin-menus` FR-001 표)을 골라 묶은 역할을 만들고 운영자에게 줄 수 있어야 한다(MUST)
- **FR-002**: 운영자는 받은 역할들의 권한을 합친 범위의 메뉴와 기능만 쓸 수 있어야 한다(MUST)
- **FR-003**: 처음 배포할 때는 역할을 하나도 두지 않는다. 필요한 역할은 관리자가 만든다

### Key Entities *(include if feature involves data)*

- **역할**: 권한 여러 개를 묶은 것. 관리자가 만들어 운영자에게 준다. 한 운영자가 여러 역할을 받을 수 있다

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 관리자가 새 역할을 만들어 운영자에게 주기까지 3분 안에 마칠 수 있다
- **SC-002**: 운영자가 받지 않은 권한의 관리 화면·기능 접근은 100% 막힌다
- **SC-003**: 수용 시나리오(US1) 전부가 자동 테스트로 확인된다 (원칙 II)

## Assumptions

- 관리자 화면(프론트)은 프론트 키트(web-next admin 프리셋)가 만드는 admin 화면을 쓴다
- 역할 등급(본사 > 지점 같은) 규칙은 두지 않는다. 키트 기본 권한 상승 방지만 쓴다
