# Feature Specification: 공통코드 (admin-codes)

**Feature Branch**: `feat/004-admin-codes`

**Created**: 2026-11-07

**Status**: Draft

**Input**: User description: "관리 화면에서 바꾸는 값(분류, 상태 이름 등)을 공통코드로 두고 관리자가 코드 관리에서 바꾼다. moa가 쓰는 코드 그룹을 이 스펙에 모은다."

**출처**: psw-dev-kit springboot code 헬퍼. 태그는 키트를 적용할 때 적는다.
키트 동작(코드 그룹·코드 만들기·바꾸기·지우기, 화면용 목록, 코드 형식)은 키트 문서와 키트 수용 테스트가 기준이다 (원칙 III).

## User Scenarios & Testing *(mandatory)*

### User Story 1 - 관리자가 공통코드를 관리한다 (Priority: P3)

관리자가 코드 관리에서 moa가 쓰는 코드 그룹과 값을 보고 바꾼다.

**Why this priority**: 지금은 moa가 쓰는 코드 그룹이 없다. 다른 기능이 코드 그룹을 쓰기 시작하면 중요해진다.

**Independent Test**: 관리자로 코드 관리를 열어 확인한다.

**Acceptance Scenarios**:

1. **US1-AC1** **Given** 관리자가 로그인했다, **When** 코드 관리를 연다, **Then** FR-001 표의 코드 그룹이 보인다 (처음에는 비어 있다)

---

### Edge Cases

- 다른 기능이 쓰는 코드를 관리자가 사용 안 함으로 바꾸면: 그 기능의 화면 선택 목록에서 빠진다 (키트 동작). 이미 저장된 값의 처리는 그 기능의 스펙이 정한다

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: moa가 쓰는 코드 그룹은 아래 표에 모은다(MUST). 처음 배포할 때의 초기 코드는 그 그룹을 쓰는 기능의 스펙이 정한다

  | 코드 그룹 | 쓰는 기능 | 초기 코드 |
  |---|---|---|
  | (아직 없음) | | |

- **FR-002**: 기능이 코드 그룹을 새로 쓰면 그 기능의 스펙에 그룹과 초기 코드를 적고 FR-001 표에 한 줄 더한다(MUST)
- **FR-003**: 공통코드는 캐시 없이 쓴다. 백오피스 기반은 프리셋 밖 헬퍼(redis)를 쓰지 않기 때문이다

### Key Entities *(include if feature involves data)*

- **코드 그룹**: 같은 종류의 코드를 묶은 것 (예: 신고 사유). 영문 대문자·숫자·밑줄 이름을 가진다
- **코드**: 그룹 안의 값 하나. 이름, 순서, 사용 여부를 가진다

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 관리자가 코드 값 하나를 바꾸면 개발자의 배포 없이 화면에 반영된다
- **SC-002**: 수용 시나리오(US1) 전부가 자동 테스트로 확인된다 (원칙 II)

## Assumptions

- 관리자 화면(프론트)은 프론트 키트(web-next)가 만드는 admin 화면을 쓴다. 아직 태그가 없어 키트를 적용할 때 확인한다
- 코드 로직이 값에 따라 갈리는 상태값은 공통코드가 아니라 각 기능 안에 둔다 (키트 안내: 언제 공통코드, 언제 enum)
