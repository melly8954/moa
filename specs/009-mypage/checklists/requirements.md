# Specification Quality Checklist: 마이페이지 (mypage)

**Purpose**: Validate specification completeness and quality before proceeding to planning
**Created**: 2026-11-17
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

- [NEEDS CLARIFICATION] 6곳이 남아 있다 (2026-11-17 팀 기획안 반영 뒤). 스킬 기본 제한(3개)보다 원칙 I("정해지지 않은 동작은 추측으로 정하지 않고 남긴 뒤 묻는다")과 사용자 지시를 따라 모두 남겼다 (010과 같은 처리). 모두 팀 확인 대상이다 (담당: 팀 회의)
  - FR-003 닉네임 규칙: 잠정 중복 불가(대소문자 무시)·2~12자·한글·영문·숫자·`_`·예약어 금지. `010-social-login` FR-005와 같은 답
  - FR-004 변경 제한: 잠정 가입 뒤 첫 변경 바로, 그 뒤 30일. `010-social-login` FR-005와 같은 답
  - FR-007 만 14세 미만 가입 처리(법정대리인 동의 또는 가입 제한). 010 가입과 함께 정함
  - FR-007 나이·성별 공개를 따로 둘지: 잠정 토글 하나(팀 기획안), 기획안 "공개범위 개별 관리" 항목
  - FR-013 친구·팔로워 정의: 친구·팔로우 기능(스펙 없음)이 정함. 그 전에도 "친구·팔로워만"을 고를 수 있게 할지
  - FR-013 다른 회원이 보는 범위: 잠정 로그인한 회원만, 나이·성별 공개 선택, 디스코드 태그 비공개/친구·팔로워만/전체 회원
- 정해진 것 (사용자 지시 2026-11-17): 기본 아바타만(업로드 보류), 게임 정보 최대 3개·공통코드(`GAME`, `<게임>_TIER`, `<게임>_POSITION`)·전적 사이트 링크·API 확인 범위 밖, 마이페이지 = 프로필(로그인 방식·소셜 연결은 010 계정 설정), 계정 메뉴에 닉네임, 회원 탈퇴·활동 기록 범위 밖, 나이는 출생 연도 저장·나이로 표시, 관리자·운영자는 마이페이지를 쓰지 않음(FR-016, 처음 가정을 정정)
- 팀 기획안 반영 (2026-11-17, 출처 줄): 플레이 가능 시간대 평일 오전·평일 저녁·주말 종일·새벽을 공통코드로(FR-012, 그룹·코드 이름은 잠정 `PLAY_TIME`), 디스코드 태그 공개 범위 비공개/친구·팔로워만/전체 회원(파티원 연결 지움), 나이·성별 공개 토글 하나(잠정), 게임정보 추가 모달(랭크 미확인 안내 US5-AC8, 개수 1/3 US5-AC9, 랭크는 `<게임>_TIER`에서 고름 — 사용자 확인), 편집 화면 필수·선택 표시(FR-017, US1-AC8)
- 기본값으로 정한 것 (가정): 자기소개 500자, 게임 아이디 50자, 성별 목록(남성·여성·밝히지 않음), 나이·성별·디스코드 태그 처음 값 비공개, 게임마다 하나, 디스코드 태그는 010 연결과 따로, 나이는 "올해 − 출생 연도"(연 나이)
- FR-003·FR-004·FR-007·FR-013은 팀 확인이 끝나야 수용 기준이 확정된다. 잠정 답으로 AC(US2-AC3~AC6, US3-AC2·AC3)는 붙였다 ("All functional requirements have clear acceptance criteria" 미통과의 이유)
- FR-008의 디스코드 사용자명 형식(2~32자, 소문자·숫자·`_`·`.`, 마침표 연속 금지)은 사용자에게 보이는 입력 규칙이라 스펙에 적었다
- "키트 `users`", "`GAME`·`<게임>_TIER`" 같은 이름은 기존 스펙과 경계를 잇기 위한 참조로 두었다
- 공용 파일: 계정 메뉴(`account-menu.tsx`, 키트 셸)는 FR-014에서 고치기 전 승인 대상으로 적었다. 이번에는 코드·공용 파일을 고치지 않았다
- Items marked incomplete require spec updates before `/speckit-clarify` or `/speckit-plan`
