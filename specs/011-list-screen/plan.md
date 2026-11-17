# Implementation Plan: 업무 목록의 쪽 이동·빈 결과 (list-screen)

**Branch**: `feat/011-list-screen` | **Date**: 2026-11-17 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/011-list-screen/spec.md`

## Summary

키트 web-next 골격의 목록 부품 두 개만 고친다. 목록 표(`components/kit/data-table.tsx`)와 검색·쪽 상태(`lib/list-search.ts`)다.
기본 쪽 크기를 10으로 바꾸고, `Pager`를 세 칸 grid로 다시 배치하고 처음·마지막 버튼을 더한다.
표 본문은 줄 높이 토큰으로 최소 높이를 잡는다. 넘친 쪽은 조회 결과를 본 뒤 `router.replace`로 마지막 쪽에 보낸다. 조건이 걸린 빈 결과에는 조건 초기화 버튼을 둔다.
백엔드와 API는 바꾸지 않는다. 자세한 결정은 [research.md](research.md)에, 화면 약속은 [contracts/list-pager.md](contracts/list-pager.md)에 있다.

## Technical Context

**Language/Version**: TypeScript, Next (`frontend/`, 키트 `web-next/v0.5.0`)

**Primary Dependencies**: TanStack Query(조회 상태), next-intl(문구), shadcn ui(`Button`, `NativeSelect`, `Empty`, `Skeleton`), lucide 아이콘

**Storage**: 없음

**Testing**: `npm run check`(포맷·lint·타입·색·문구·단위), 화면 검사 Playwright `e2e/list.spec.ts`(PR CI)

**Target Platform**: 데스크톱·모바일 브라우저

**Project Type**: 웹 앱 (모노레포의 프론트만)

**Performance Goals**: 해당 없음. 조회 횟수는 지금과 같다 (넘친 쪽 보내기만 한 번 더 조회한다)

**Constraints**: 공용 부품이라 이 부품을 쓰는 화면 셋(사용자 관리, 공지 관리, 접속 이력)과 개발용 목록 예시(`/dev/list`)가 함께 바뀐다. 화면 쪽 코드는 고치지 않는다

**Scale/Scope**: 파일 두 개 + 토큰·문구. 화면 셋은 영향만 확인한다

## Constitution Check

| 원칙 | 확인 |
|---|---|
| I~II (스펙 먼저, 스펙과 코드 일치) | 통과. spec·plan·tasks를 먼저 병합하고 구현한다 |
| III 키트를 따른다 | 통과. 키트 골격 부품을 moa에서 먼저 고치고, 스펙에는 바꾸는 것만 적었다. 구현 뒤 키트로 옮길 목록에 올린다 |
| 목록 네 종류 | 통과. 대상은 업무 목록(서버 페이징, 쪽 번호, 조건·쪽은 주소에)이고 그 규칙은 그대로다 |
| V 테스트를 고쳐서 통과시키지 않는다 | 통과. `e2e/list.spec.ts`의 기본 20줄 기대는 스펙(FR-001)이 바뀌어서 고친다 |
| VI 시크릿 | 해당 없음 |

설계 뒤 다시 확인: 위반 없음.

## Project Structure

### Documentation (this feature)

```text
specs/011-list-screen/
├── spec.md
├── plan.md              # 이 파일
├── research.md          # 결정과 이유
├── contracts/
│   └── list-pager.md    # 아래 줄·표 본문의 화면 약속
├── quickstart.md        # 확인 방법
└── tasks.md             # /speckit-tasks
```

data-model.md는 두지 않는다. 데이터와 API가 바뀌지 않는다.

### Source Code (repository root)

```text
frontend/
├── src/
│   ├── lib/list-search.ts             # 기본 쪽 크기 10, 쪽 이동에 replace 선택
│   ├── components/kit/data-table.tsx  # Pager 세 칸·처음/마지막, 본문 최소 높이, 넘친 쪽 보내기, 조건 초기화
│   └── app/layout-tokens.css          # 줄 높이 토큰
├── messages/ko.json                   # table.first·last·resetFilters
└── e2e/list.spec.ts                   # 마지막에 고친다
```

**Structure Decision**: 프론트 공용 부품만 고친다. 화면(`app/(admin)/admin/*/page.tsx`)은 그대로 두고 확인만 한다.

## Complexity Tracking

없음.
