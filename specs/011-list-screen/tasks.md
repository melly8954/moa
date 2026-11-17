# Tasks: 업무 목록의 쪽 이동·빈 결과 (list-screen)

**Input**: Design documents from `specs/011-list-screen/`
**Prerequisites**: plan.md, spec.md, research.md, contracts/list-pager.md, quickstart.md

**Tests**: 기능 코드를 먼저 끝내고, 테스트 수정·추가는 맨 끝(Phase 6)에서 한다 (CLAUDE.md). 화면 검사는 로컬에서 돌리지 않고 PR CI로 본다.

**Format**: `[ID] [P?] [Story] Description`. 거의 모든 작업이 `frontend/src/components/kit/data-table.tsx` 한 파일이라 [P]가 적다.

## Phase 1: Setup

- [ ] T001 frontend/src/app/layout-tokens.css의 표 열 폭 묶음 아래에 줄 높이 토큰 `--table-row-height: 2.5rem`을 주석과 함께 더한다. 표 머리 `h-10`과 같은 높이다 (research.md R4)
- [ ] T002 [P] frontend/messages/ko.json `table`에 `first`("첫 쪽"), `last`("마지막 쪽"), `resetFilters`("조건 초기화")를 더하고 `total`을 `총 <count>{count}</count>건`으로 바꾼다 (contracts/list-pager.md "새 문구")

---

## Phase 2: Foundational

- [ ] T003 frontend/src/lib/list-search.ts의 `setPage`에 선택 인자 `{ replace?: boolean }`를 더한다. `navigate`가 이 값을 받으면 `router.push` 대신 `router.replace`(같은 `{ scroll: false }`)를 쓴다. `ListSearch` 타입과 머리 주석의 규칙 목록도 맞춘다 (research.md R5)

**Checkpoint**: 토큰·문구·replace가 준비됐다. 화면은 아직 그대로다.

---

## Phase 3: User Story 1 - 쪽 이동 줄에서 원하는 쪽으로 간다 (Priority: P1) 🎯 MVP

**Goal**: 아래 줄 세 칸(건수 | 쪽 이동 | 쪽 크기)과 처음·마지막 버튼

**Independent Test**: `/dev/list`에서 아래 줄 배치를 보고 «·»와 비활성, 좁은 화면 두 줄을 확인한다 (quickstart.md 1)

- [ ] T004 [US1] frontend/src/components/kit/data-table.tsx `Pager`의 바깥을 grid로 바꾼다. 넓은 화면은 `sm:grid-cols-[1fr_auto_1fr]`로 건수(왼쪽) | nav(`justify-self-center`) | 쪽 크기 `NativeSelect`(`justify-self-end`) 순서다. 좁은 화면은 `grid-cols-2`이고, nav가 첫 줄 두 칸을 차지해 가운데에, 건수와 쪽 크기가 둘째 줄 양쪽에 온다. 지금 왼쪽 묶음에 같이 있는 건수와 셀렉트를 두 칸으로 나눈다 (research.md R2)
- [ ] T005 [US1] 같은 파일에서 건수를 `t.rich("total", { count: … })`로 그리고 숫자만 `font-medium text-foreground`로 강조한다. `total`이 없으면(로딩·오류) 건수 칸은 비워 두되 grid 칸은 남긴다
- [ ] T006 [US1] 같은 파일 `Pager` nav의 이전 앞에 처음(`ChevronsLeft`, `aria-label` `t("first")`, `page <= 1`이면 비활성, `onPage(1)`), 다음 뒤에 마지막(`ChevronsRight`, `t("last")`, `page >= totalPages`이면 비활성, `onPage(totalPages)`) 버튼을 더한다. 모양은 이전·다음과 같은 `ghost icon-sm`이다 (research.md R3)

**Checkpoint**: US1을 손으로 확인할 수 있다 (quickstart.md 1의 1·2·마지막 항목).

---

## Phase 4: User Story 2 - 결과가 없어도 표 모양과 쪽 이동 자리가 그대로다 (Priority: P1)

**Goal**: 모든 상태에서 표 본문 최소 높이를 확보하고, 조건이 걸린 빈 결과에는 조건 초기화 버튼을 둔다

**Independent Test**: `/dev/list`에서 데이터, 9쪽(7건), 빈 결과, `error` 조회를 오가며 쪽 이동 줄 위치와 조건 초기화를 확인한다 (quickstart.md 1)

- [ ] T007 [US2] frontend/src/components/kit/data-table.tsx에서 `N = Math.min(search.size, 10)`를 한 번 계산한다. 가로 스크롤 상자(`role="region"`)에 `style={{ minHeight: "calc(var(--table-row-height) * (N + 1))" }}`를 주고, 데이터 행과 Skeleton 행의 높이를 `h-(--table-row-height)`로 맞춘다. Skeleton은 지금처럼 N줄이다 (research.md R4)
- [ ] T008 [US2] 같은 파일의 빈 결과 칸과 오류 칸(`TableCell colSpan`)에 높이 `calc(var(--table-row-height) * N)`를 주어 안내가 본문 가운데에 놓이게 한다
- [ ] T009 [US2] 같은 파일의 빈 결과 `Empty`에 `search.isFiltered`일 때만 `EmptyContent`와 `Button variant="outline" size="sm"`(`RotateCcw` 아이콘, `t("resetFilters")`, `onClick={search.reset}`)을 더한다. `EmptyContent`는 frontend/src/components/ui/empty.tsx에서 가져온다 (research.md R6)

**Checkpoint**: US1·US2를 함께 확인할 수 있다.

---

## Phase 5: User Story 3 - 처음 열면 10건씩, 없는 쪽 주소는 마지막 쪽으로 (Priority: P2)

**Goal**: 기본 쪽 크기 10, 넘친 쪽은 마지막 쪽으로 보낸다

**Independent Test**: 주소에 `size` 없이 열면 10건, `?page=99`로 열면 마지막 쪽이 열리고 뒤로 가기가 99로 돌아가지 않는다 (quickstart.md 1)

- [ ] T010 [P] [US3] frontend/src/lib/list-search.ts `useListSearch`의 기본값 `size: defaultSize = 20`을 `10`으로 바꾼다. `PAGE_SIZES`는 그대로다 (research.md R1)
- [ ] T011 [US3] frontend/src/components/kit/data-table.tsx에 `useEffect`를 더한다. `query.data`가 있고 `query.data.page === search.page - 1`(지금 쪽의 응답)이며 `search.page > Math.max(1, query.data.totalPages)`이면 `search.setPage(Math.max(1, query.data.totalPages), { replace: true })`를 부른다. 오류이면 `data`가 없으니 부르지 않는다 (research.md R5, T003 뒤)

**Checkpoint**: 기능 코드 끝. 화면 셋(사용자, 공지, 접속 이력)의 코드는 고치지 않는다.

---

## Phase 6: Polish & 테스트 (맨 끝)

- [ ] T012 frontend/에서 `npm run format`과 `npm run check`로 포맷·lint·타입·색·문구·단위 검사를 통과시킨다
- [ ] T013 quickstart.md의 "손으로 확인" 1~3을 브라우저에서 확인한다 (`/dev/list`, `/admin/login-histories`, `/admin/users`, `/admin/notices`)
- [ ] T014 frontend/e2e/list.spec.ts "쪽 이동과 쪽 크기" 테스트의 기본 20줄 기대를 10줄로 고친다. `size=50` 주소 기대는 그대로다 (원칙 V: FR-001이 먼저 바뀌었다)
- [ ] T015 frontend/e2e/list.spec.ts에 더한다: 처음·마지막 버튼과 양 끝 비활성(US1-AC2·AC3), `?page=99`를 열면 `?page=9`로 바뀌고 뒤로 가기가 99로 돌아가지 않음(US3-AC3), 결과 없는 조건에서 "조건 초기화"를 누르면 87건으로 돌아감(US2-AC5), 빈 결과와 데이터 상태에서 쪽 이동(`navigation` "쪽 이동")의 세로 위치가 같음(SC-001)
- [ ] T016 PR CI의 e2e 결과로 `list`·`login-history`·`user-admin`·`notices` spec을 확인한다. 실패하면 그때 고친다 (로컬에서 돌리지 않는다)
- [ ] T017 키트로 옮길 목록에 이 변경을 올린다: web-next 골격의 `data-table.tsx`·`list-search.ts`·`layout-tokens.css`·문구, `e2e/list.spec.ts` (원칙 III)

---

## Dependencies & Execution Order

- Phase 1 → Phase 2 → US1 → US2 → US3 → Phase 6
- US1·US2·US3는 모두 `data-table.tsx`를 고쳐 같은 파일에서 차례로 한다. T010만 다른 파일이라 언제든 할 수 있다
- T011은 T003(replace)이 필요하다
- 테스트(T014·T015)는 기능 코드가 다 끝난 뒤에 한다

## Parallel Example

```text
T002 (ko.json 문구)  ∥  T001 (토큰)
T010 (list-search 기본값)  ∥  US1·US2 작업 (data-table.tsx)
```

## Implementation Strategy

- MVP는 US1(아래 줄 세 칸 + 처음·마지막)이다. 다만 한 PR(`feat/011-list-screen`)에 US1~US3을 모두 넣는다. 같은 부품을 바꾸는 작은 묶음이라 나누면 오히려 화면이 두 번 바뀐다
- PR 설명에 영향 받는 화면 셋과 행 높이 변화(약 37px → 40px)를 적는다
