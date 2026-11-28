# Specification Quality Checklist: UI 기반 (ui-foundation)

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

- [NEEDS CLARIFICATION] 2개를 사용자 답으로 닫았다 (Clarifications Session 2026-11-28): 기본 화면 모드는 기기 설정을 따른다. 관리 셸은 아직 건드리지 않는다(모양·배치·테마 그대로, 관리 셸 적용은 범위 밖·나중). 그래서 FR-022(적용 범위), US1-AC4, SC-008을 더했다
- 구체 값(색 코드, 글꼴, 크기, 배치 상수, 와이어프레임, 컴포넌트 규칙)은 `design-system.md`에 두고 spec.md는 동작과 기준만 적었다. spec.md의 FR에 남은 수치(폭 기준, 40px 단추, 3:4, 2px 간격, 대비 비율)는 사용자가 보는 결과라 시험 기준으로 남겼다
- FR-020(검사 확장)과 FR-021(견본 파일)은 팀의 작업 방식에 관한 요구다. FR-020은 US1-AC2·SC-007로 확인하고, FR-021은 AC를 두지 않는다(견본은 다음 단계 산출물)
- 대비 값은 WCAG 상대 휘도 식으로 계산했다. 계산 도구로 다시 재는 것은 US3 Independent Test에 적었다
- 제공자 로고 크기·여백은 각 제공자 지침 원문을 plan에서 확인한다 (`design-system.md` 6.4)
- 한글 글꼴의 `next/font` 옵션(subsets, preload)은 plan에서 Next 문서로 확인한다 (`design-system.md` 2절)
- 다른 스펙에 반영할 곳(001 FR-005 셸 모양, 010 로그인 화면, 019 예약)은 spec.md 끝에 적었다. 이 브랜치에서는 그 스펙들을 고치지 않았다
- Items marked incomplete require spec updates before `/speckit-clarify` or `/speckit-plan`
