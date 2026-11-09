# Feature Specification: 접속 이력 (admin-login-history)

**Feature Branch**: `feat/005-admin-login-history`

**Created**: 2026-11-07

**Status**: Draft

**Input**: User description: "로그인 성공·실패를 접속 IP와 기기와 함께 남기고 관리자가 목록으로 본다. 보관 기간은 moa가 정한다."

**출처**: psw-dev-kit springboot login-history 헬퍼. 태그 `springboot/v0.3.1`, 화면은 `web-next/v0.3.0`.
키트 동작(기록, 조회 조건, 기기 분석, 보관 기간이 지난 이력 정리)은 키트 문서와 키트 수용 테스트가 기준이다 (원칙 III).

## User Scenarios & Testing *(mandatory)*

### User Story 1 - 관리자가 접속 이력을 본다 (Priority: P2)

관리자가 접속 이력에서 로그인 성공과 실패를 보고, 특정 아이디의 실패 이력을 찾는다.

**Why this priority**: 계정 도용이나 무차별 로그인 시도를 확인하는 수단이다. 첫 출시에 있어야 하지만 운영 초기에는 자주 쓰지 않는다.

**Independent Test**: 로그인을 성공·실패로 몇 번 한 뒤 관리자로 접속 이력을 연다.

**Acceptance Scenarios**:

1. **US1-AC1** **Given** 어떤 아이디로 로그인 실패와 성공이 있었다, **When** 관리자가 접속 이력에서 그 아이디로 찾는다, **Then** 실패와 성공이 최신순으로 접속 IP, 기기와 함께 보인다

---

### Edge Cases

- 보관 기간이 지난 이력: 매일 지워져 목록에 보이지 않는다 (키트 동작)
- 프록시 뒤에서 운영할 때 실제 접속 IP가 남지 않을 수 있다: 배포 구성에서 맞춘다 (가정 참조)

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: 시스템은 로그인 성공과 실패를 접속 이력으로 남겨야 한다(MUST). 동작은 키트를 따른다
- **FR-002**: 접속 이력은 90일 동안 보관하고, 지나면 지운다(MUST). 키트 기본(365일)보다 짧게 둔다. IP와 기기는 개인정보라 필요한 만큼만 둔다 (2026-11-07 결정)

### Key Entities *(include if feature involves data)*

- **접속 이력**: 로그인 한 번의 기록. 입력한 아이디, 성공 여부, 접속 IP, 기기 종류, OS, 브라우저, 시각을 가진다

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 관리자가 특정 아이디의 최근 로그인 실패를 1분 안에 찾을 수 있다
- **SC-002**: 보관 기간이 지난 이력은 하루 안에 지워진다
- **SC-003**: 수용 시나리오(US1) 전부가 자동 테스트로 확인된다 (원칙 II)

## Assumptions

- 관리자 화면(프론트)은 프론트 키트(web-next admin 프리셋)가 만드는 admin 화면을 쓴다
- 운영 환경은 프록시(로드밸런서 등) 뒤에 있을 수 있다. 실제 접속 IP를 남기는 설정은 배포 구성 때 맞춘다
- 로그인 실패 횟수로 계정을 잠그는 기능은 두지 않는다 (예전 기획 결정: 로그인 실패 잠금 없음)
