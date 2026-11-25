# Specification Quality Checklist: 회원 탈퇴 (member-withdrawal)

**Purpose**: Validate specification completeness and quality before proceeding to planning
**Created**: 2026-11-25
**Feature**: [spec.md](../spec.md)

## Content Quality

- [x] No implementation details (languages, frameworks, APIs)
- [x] Focused on user value and business needs
- [x] Written for non-technical stakeholders
- [x] All mandatory sections completed

## Requirement Completeness

- [x] No [NEEDS CLARIFICATION] markers remain
- [x] Requirements are testable and unambiguous
- [x] Success criteria are measurable
- [x] Success criteria are technology-agnostic (no implementation details)
- [x] All acceptance scenarios are defined
- [x] Edge cases are identified
- [x] Scope is clearly bounded
- [x] Dependencies and assumptions identified

## Feature Readiness

- [x] All functional requirements have clear acceptance criteria
- [x] User scenarios cover primary flows
- [x] Feature meets measurable outcomes defined in Success Criteria
- [x] No implementation details leak into specification

## Notes

- 범위를 줄였다 (사용자 지시 2026-11-25): 팀원 답을 기다리지 않고 회원이 스스로 하는 탈퇴만 한다
  - FR-016 탭 구조와 빨간 "탈퇴하기" 단추만. 붙는 것은 없음
  - FR-017 탈퇴됨·복구됨·파기됨 이벤트를 내기만 함. 붙는 처리는 없음
  - FR-019 관리자가 지운 회원의 파기는 뺌. 후속: 관리자 기능 쪽에서 다룸
- 정해진 것 (사용자 지시 2026-11-25): 계정 관리 탭, 맨 아래 오른쪽 빨간 단추, 닉네임 입력 확인, 즉시 로그인 막기·숨기기, 30일 본인 로그인 복구만(관리자 대신 복구는 후속), 유예 중 로그인 아이디·이메일·소셜 계정 붙잡기(저장 방법은 plan), 5일 안 파기와 파기 이력, 재가입 제한 기록은 만들지 않음(정지 기능 스펙이 만듦), 관리자 계정 제외, 세션은 결과(다음 요청부터 인증 거부)만, 로그에 이메일 원문 금지
- 작성하며 정해 사용자가 그대로 두기로 한 것: FR-004 닉네임은 대소문자까지 같아야 함·태그 없음, FR-006 실시간 연결도 끊음, FR-008 남은 자리의 이름은 "탈퇴한 회원", FR-014 파기 이력은 회원마다 1건·"개인정보 열 삭제", 유예 뒤 파기 전 최대 5일의 동작(가정)
- 범위를 줄이며 정한 것 (확인 필요)
  - FR-017 실패 기록·다시 시도는 처리를 붙이는 기능의 몫으로 넘김 (지금 붙는 처리가 없음)
  - FR-020 로그 금지를 요구사항과 AC(US3-AC14)로 적음. 소셜 계정 식별자도 함께 금지
  - US3-AC11 관리자가 지운 회원은 이 기능의 정리가 건드리지 않음을 확인
- 파기 대상은 사용자가 적은 다섯 테이블에 `docs/privacy.md` "탈퇴 회원 파기"가 적은 알림·공지 확인·푸시 구독·리프레시 토큰을 더했다 (privacy.md가 정본)
- FR-018은 다른 기능이 읽는 값이라 따로 AC를 두지 않았다. 이 값을 처음 쓰는 기능(`017-customer-center` US5 등)의 AC로 확인한다
- 테이블 이름, `PersonalDataPurgedEvent`, `MemberActivityPolicy`는 기존 문서와 경계를 잇기 위한 참조로 두었다
- plan에서 볼 것: 지금 서버는 로그인 정보를 재발급할 때 회원 상태를 다시 읽는다. 요청마다 읽는지는 plan에서 확인한다. FR-006("다음 요청부터")을 지키려면 요청마다 탈퇴를 확인할 길이 필요할 수 있다
- Items marked incomplete require spec updates before `/speckit-clarify` or `/speckit-plan`
