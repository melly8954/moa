# Specification Quality Checklist: 기본 회원가입·로그인 (basic-signup)

**Purpose**: Validate specification completeness and quality before proceeding to planning
**Created**: 2026-11-28
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

- 2026-11-28 사용자 답: FR-015 관리자·운영자 계정도 임시 잠금(A), FR-019 메일은 moa가 만든다(B, 키트에 있으면 바꾼다는 가정). 두 질문은 닫았다
- 2026-11-28 사용자 결정으로 가입 코드 받기도 이메일이 이미 쓰이는지 알리지 않는다(FR-005, SC-004). 이 결정에서 생긴 질문(FR-005 "이미 쓰는 이메일에 무엇을 보내는가")은 같은 날 A(가입 코드 대신 안내 메일)로 닫았다. 남은 [NEEDS CLARIFICATION]은 없다
- "구현 세부 없음"은 moa 스펙의 관례(다른 스펙처럼 키트 `tb_user`, Gmail SMTP, `.env.<프로필>`, 해시 저장을 적는다)에 따라 통과로 본다. Gmail SMTP·`.env`는 사용자가 정한 제약이고, 테이블·API 이름은 plan에서 정한다
- 아이디·비밀번호 규칙, 코드 값(10분, 60초, 1시간 5번, 시도 5번, 인증 뒤 30분), 예약 아이디 목록은 잠정 값으로 표시했다
- 남의 스펙은 고치지 않고 spec.md 끝 "다른 스펙·문서에 줄 영향"에 모았다
