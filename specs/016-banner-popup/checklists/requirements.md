# Specification Quality Checklist: 배너·팝업 관리

**Purpose**: Validate specification completeness and quality before proceeding to planning
**Created**: 2026-11-24
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

- 2026-11-24 답: 배너는 이미지 배너(FR-003), 노출 자리는 정하지 않고 관리 화면만(FR-011). 사용자 화면 노출은 자리가 정해질 때 이 스펙에 더한다
- 배너 이미지 크기 제한과 올리기 방법(키트 file 헬퍼 또는 rich-text 헬퍼)은 plan에서 정한다
- FR-001의 `001-admin-menus` FR-001 표 고치기는 같은 문서 PR에서 한다
- 2026-11-24 다시 검토 답: 등록 기본값은 켜짐·순서 0(FR-005), 목록은 상태와 이름(팝업은 제목)으로 찾는다(FR-008). 팝업 서식 걸러 저장(US2-AC7)과 팝업 순서(US2-AC8) AC를 더했다
- 일괄 처리(FR-008)는 켜기·끄기·삭제 (2026-11-24 답: 일괄 켜기·끄기 넣기). plan·research 메모: `POST /api/v1/admin/banners/bulk-delete`(ids), 팝업도 같은 모양. 켜기·끄기도 같은 모양으로 둔다(이름은 plan에서 메뉴·공통코드의 일괄 사용 처리와 맞춘다). 메뉴·역할의 bulk-delete(`MenuIdsRequest`·`RoleIdsRequest`)와 같게 한 트랜잭션, 204. 화면은 DataTable selection(rowLabel = 이름·제목) + `components/kit/bulk-toolbar.tsx`, 쪽·검색이 바뀌면 고른 것을 푼다(`roles/role-members.tsx`와 같다). 참고: `specs/002-admin-users/research.md` R18, `specs/006-notices/research.md` R13. 순서는 숫자 입력(FR-005)이라 끌기와 함께 쓰는 경우는 없다
