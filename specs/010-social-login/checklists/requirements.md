# Specification Quality Checklist: 소셜 로그인 (social-login)

**Purpose**: Validate specification completeness and quality before proceeding to planning
**Created**: 2026-11-16
**Feature**: [spec.md](../spec.md)

## Content Quality

- [x] No implementation details (languages, frameworks, APIs)
- [x] Focused on user value and business needs
- [x] Written for non-technical stakeholders
- [x] All mandatory sections completed

## Requirement Completeness

- [ ] No [NEEDS CLARIFICATION] markers remain
- [x] Requirements are testable and unambiguous
- [x] Success criteria are measurable
- [x] Success criteria are technology-agnostic (no implementation details)
- [x] All acceptance scenarios are defined
- [x] Edge cases are identified
- [x] Scope is clearly bounded
- [x] Dependencies and assumptions identified

## Feature Readiness

- [ ] All functional requirements have clear acceptance criteria
- [x] User scenarios cover primary flows
- [x] Feature meets measurable outcomes defined in Success Criteria
- [x] No implementation details leak into specification

## Notes

- [NEEDS CLARIFICATION] 8곳이 남아 있다 (2026-11-17 닉네임 규칙을 `009-mypage`와 맞추고 질문 2개를 더한 뒤). 스킬 기본 제한(3개)보다 원칙 I("정해지지 않은 동작은 추측으로 정하지 않고 남긴 뒤 묻는다")과 사용자 지시를 따라 모두 남겼다
  - 팀 확인 필요: 출처(키트 oauth 헬퍼 유무, 구글·디스코드와 FR-006 연결·해제 지원 — 담당: 키트 담당/팀 리더), FR-004 회원 정보 모델(잠정 B, 담당: 팀 회의), FR-015 영구 정지 단계(담당: 팀 회의)
  - 팀 확인 필요 (잠정 답 있음): FR-005 닉네임 규칙(2~12자, 한글·영문·숫자·`_`, 예약어 금지, 대소문자 무시 중복 불가 — `009-mypage` FR-003과 같음)
  - 팀 확인 필요 (`009-mypage` FR-007과 함께): FR-005 만 14세 미만 가입 처리, FR-005 가입 때 출생 연도
  - 미정: FR-005 닉네임 없는 기존 회원의 표시 이름, US4-AC1 접속 이력 표시
- 정해진 것: FR-006 연결·해제(C), FR-005 닉네임 입력과 입력 전 이탈 처리, 웹만(앱 나중), FR-007 관리자·운영자 소셜 불가·소셜 회원 역할 금지, US2-AC3 비밀번호 변경 숨김, US3-AC3 설정 없는 제공자 단추 숨김, 소셜 이름·이메일 변경 시 가입 때 값 유지
- 2026-11-17 정해진 것: 제공자 구글+디스코드(카카오 나중), FR-013 약관·개인정보 필수 동의(US1-AC3 질문 풀림), FR-014 이메일 중복이면 가입·연결 막음(연결하는 본인 이메일은 예외), 이메일 없음·미인증이면 가입·연결 막음(Session 2026-11-16 "연결에 인증 조건 없음"을 정정), FR-015 재가입 제한(삭제 회원 재가입 질문 풀림, 연결에도 적용)
- FR-004는 팀 확인이 끝나야 수용 기준을 붙일 수 있다 ("All functional requirements have clear acceptance criteria" 미통과의 이유)
- "키트 `users`", "`/sign-in`" 같은 이름은 기존 스펙과 경계를 잇기 위한 참조로 두었다
- Items marked incomplete require spec updates before `/speckit-clarify` or `/speckit-plan`
