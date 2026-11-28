# Specification Quality Checklist: 이용약관·개인정보처리방침 공개 페이지

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

- 2026-11-27 답: 링크는 로그인·가입 화면 맨 아래와 사용자 셸 사이드바 계정 줄 밑(관리 셸 제외), 예전 버전·시행 예정 안내 넣기, 이용약관 공개도 함께. 범위가 넓어져 폴더를 `012-privacy-policy`에서 `012-terms-privacy`로 바꿨다
- 2026-11-28 바꿈: 링크는 사이드바 계정 줄 밑 대신 화면 맨 아래 푸터 줄(로그인·가입, 사용자 셸 본문, 문서 화면)에 둔다. `001-admin-menus` FR-005의 규칙도 푸터 줄로 바꿨다. 가입 화면의 약관 칸은 요약·펼치기 대신 전문을 상자에 바로 보이고 "더보기"로 문서 화면을 새 탭에서 연다 (`010-social-login` FR-013·US1-AC7도 고침)
- 처리방침 문안의 법정 기재 항목은 팀의 법무 확인이 필요하다 (Assumptions)
- 이용약관 원문을 010 가입 화면과 한 곳에 두는 방법(FR-010)은 plan에서 정한다. 010 문안에는 시행일이 없어 버전마다 시행일을 더해야 한다
- 시행일이 되면 배포 없이 바뀌려면(US3-AC4) 요청 때 오늘 날짜로 버전을 골라야 한다 — plan research R5
- 2026-11-28: 문안을 백엔드가 공개 API로 내려 주고 화면은 다른 화면처럼 브라우저에서 그리게 바꿨다. spec Assumptions의 "서버 API를 두지 않는다"를 먼저 고쳤다
- 2026-11-27 검토: 문서 화면 맨 위의 moa 이름·다른 문서 링크(US1-AC6, FR-011)를 더했고, 010 FR-013에 012를 가리키는 줄을 더했다 (팀원에게 알릴 것). 브랜치는 `docs/012-terms-privacy`로 바꿨다
- 2026-11-28 plan 검토: 없는 문서 주소는 문자열로 받아 404(enum이면 400), 문안은 리소스 패턴으로 찾음(jar), 문안은 메타 JSON + 본문 HTML 둘로 나눔(jsoup으로 장 나누기)
