# Implementation Plan: 이용약관·개인정보처리방침 공개 페이지 (terms-privacy)

**Branch**: `feat/012-terms-privacy` | **Date**: 2026-11-28 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/012-terms-privacy/spec.md`

## Summary

누구나 여는 이용약관·개인정보처리방침 화면(지금 버전, 예전 버전, 시행 예정 안내)을 프론트에 만든다. 백엔드 변경은 없다.
문안은 프론트 코드에 문서·버전마다 TS 파일로 두고(장마다 HTML 조각), 010 가입 화면의 이용약관도 같은 원문을 쓰게 옮긴다 ([research.md](./research.md) R1·R2).
화면은 셸 없는 새 경로 묶음 `(legal)`에 두어 관리자 계정도 열고(R4), 요청마다 그려 시행일이 되면 배포 없이 바뀐다(R5).
링크는 로그인·가입 화면, 사용자 셸 본문, 문서 화면 맨 아래의 푸터 줄에 둔다. 가입 화면의 약관 칸은 전문을 상자에 바로 보이고 "더보기"로 문서 화면을 새 탭에서 연다(R2·R7, 2026-11-28).

## Technical Context

**Language/Version**: TypeScript 5 (Next 16.3). 백엔드(Java 21, Spring Boot 4.1.2) 변경 없음

**Primary Dependencies**: 기존 프론트 의존성(DOMPurify, next-intl, base-ui). 새 의존성 없음

**Storage**: 없음. 문안은 `frontend/src/content/legal/`의 TS 파일

**Testing**: `npm run check`(단위 `tests/unit/legal.test.ts` 포함), PR CI의 e2e `e2e/legal.spec.ts`(3폭, 가로 넘침·접근성). 서버 API가 없어 인수 테스트(dbTest)는 없다

**Target Platform**: 웹 (사용자 화면, 공개)

**Project Type**: 웹 서비스 (모노레포 `frontend/`)

**Performance Goals**: 문서 화면은 서버에서 한 번에 그린다. 버전 고르기는 버전 수만큼의 비교 한 번

**Constraints**: 문안은 `messages/ko.json`에 넣지 않는다(spec 배경). 키트 `RichTextView`(표 없음)를 고치지 않는다(R3). 관리자 계정을 막는 사용자 셸에 두지 않는다(R4). 로컬에서는 `npm run check`만 돌린다

**Scale/Scope**: 화면 4개(문서 2 × 지금·버전), 푸터 줄 자리 5곳(로그인 2, 가입 1, 사용자 셸, 문서 화면), 문안 2개(각 첫 버전), AC 15개(US1 6, US2 5, US3 4)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| 원칙 | 판정 | 근거 |
|---|---|---|
| I. 스펙이 먼저다 | 통과 | spec.md에 `[NEEDS CLARIFICATION]`이 없다. 링크 자리·예전 버전·이용약관 함께는 2026-11-27 사용자 답 |
| II. AC마다 테스트 하나 | 통과 | 서버 API가 없어 AC 15개를 단위 테스트(날짜 규칙, 배포 전 검사)와 e2e(화면)로 본다 (R8). 원칙의 "API 통합 테스트 기본"은 API가 있는 기능에 해당한다 |
| III. 키트를 따른다 | 통과 | 키트 `RichTextView`는 고치지 않고 문서 보기 부품을 따로 둔다(R3). `sidebar-shell.tsx` 본문 영역에 푸터 자리 하나를 더하는 것은 moa가 이미 바꿔 온 셸 파일이고, 키트로 옮길 목록에 더한다(R7) |
| IV. 기존 기능은 제자리에서 고친다 | 통과 | 사용자 셸 푸터 줄은 `001-admin-menus` FR-005, 가입 화면의 약관 칸·원문은 `010-social-login` FR-013에 012를 가리키는 줄을 더했다(spec 커밋) |
| V. 테스트를 고쳐서 통과시키지 않는다 | 통과 | 010 가입 화면 e2e(`social-login.spec.ts`)가 약관 요약·펼치기를 본다면, 010 FR-013·US1-AC7을 먼저 고친 만큼(전문 상자, 더보기 새 탭)만 고친다 |
| VI. 시크릿을 다루지 않는다 | 통과 | 다룰 시크릿이 없다 |
| 기술 제약: DB·목록 | 해당 없음 | 테이블도 업무 목록도 없다. 버전 목록은 한 문서의 버전 수만큼이라 구성 목록에 가깝고, 상한은 두지 않는다(해마다 몇 개) |

Phase 1 설계 뒤 다시 확인했다. 위반이 없다.

## Project Structure

### Documentation (this feature)

```text
specs/012-terms-privacy/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── screens.md        # 화면 경로·구성·링크 자리
└── tasks.md              # /speckit-tasks가 만든다
```

### Source Code (repository root)

```text
frontend/
├── src/content/legal/
│   ├── index.ts                    # 문서별 버전 목록, LEGAL_CONTENT=fixture면 fixture 목록 (production 무시)
│   ├── types.ts                    # LegalDocumentKey, LegalVersion
│   ├── terms/draft.ts              # 이용약관 첫 버전 (010 자리표시 옮김)
│   ├── privacy/draft.ts            # 처리방침 첫 버전 (11-21 초안 + 018)
│   └── fixture.ts                  # e2e용 (시행 둘 + 예정 하나)
├── src/content/terms/index.ts      # 010 termsText: SERVICE는 legal 목록에서 (R2)
├── src/lib/legal.ts                # currentVersion, upcomingVersion, versionState, 오늘(한국 날짜)
├── src/components/legal/
│   ├── legal-document.tsx          # 문서 보기 (목차, 장, 표 상자, 작은 모양)
│   └── legal-footer.tsx            # 푸터 줄 "이용약관 · 개인정보처리방침" (newTab)
├── src/app/(legal)/
│   ├── layout.tsx                  # 머리 줄 (moa, 두 문서 링크)
│   ├── terms/page.tsx  terms/versions/[version]/page.tsx
│   └── privacy/page.tsx  privacy/versions/[version]/page.tsx
├── src/app/(public)/sign-in/page.tsx  sign-in/recover/page.tsx  sign-up/nickname/page.tsx   # 푸터 줄, 가입 화면 약관 칸(전문 상자·더보기)
├── src/components/shell/{sidebar-shell.tsx,user-shell.tsx}   # 본문 맨 아래 푸터 자리
├── messages/ko.json                # legal
├── tests/unit/legal.test.ts
├── e2e/legal.spec.ts
└── playwright.config.ts            # e2e 서버에 LEGAL_CONTENT=fixture
```

**Structure Decision**: 이 기능은 프론트만 바꾼다. 문서 화면은 셸 없는 `(legal)` 경로 묶음에 둔다(공지가 처음 `(site)`를 둔 것과 같은 이유, 006 R7). 문안 고르기는 순수 함수로 나눠 단위 테스트로 본다.

## Complexity Tracking

위반이 없어 비워 둔다.
