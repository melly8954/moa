# Research: 업무 목록의 쪽 이동·빈 결과

화면 쪽 결정은 spec "배경과 결정"에 있다. 여기에는 구현 방법의 결정만 둔다.

## R1. 기본 쪽 크기 (FR-001)

- **Decision**: `useListSearch`의 기본값 `size = 20`을 `10`으로 바꾼다. `PAGE_SIZES = [10, 20, 50]`은 그대로 둔다.
- **Rationale**: 키트 목록 표를 쓰는 화면은 모두 쪽 크기를 따로 넘기지 않는다. 그래서 한 곳만 바꾸면 모든 화면에 적용된다. 주소에는 기본값과 다를 때만 `size`를 적으므로, 이제 `size=20`도 주소에 남는다.
- **Alternatives considered**: 화면마다 `size: 10`을 넘기기. 빠뜨리기 쉽고 FR-009(화면마다 따로 고치지 않음)에 어긋난다.
- **영향 없음**: 알림함은 `useListSearch`에 `size: 20`을 직접 넘긴다. 사용자 공지 목록과 역할 구성원 창은 이 훅을 쓰지 않는다.

## R2. 아래 줄 세 칸 (FR-002~FR-004)

- **Decision**: `Pager` 바깥을 grid로 바꾼다. 넓은 화면(`sm` 이상)은 `grid-cols-[1fr_auto_1fr]`이고 건수 | 쪽 이동 | 쪽 크기 순서다. 좁은 화면은 두 열이다. 첫 줄에 쪽 이동(두 칸 차지, 가운데), 둘째 줄에 건수(왼쪽)와 쪽 크기(오른쪽)를 둔다.
- **Rationale**: 양쪽 칸이 같은 비율(1fr)이라 쪽 이동이 정확히 가운데 온다. flex `justify-between`은 양쪽 내용 폭이 다르면 가운데가 밀린다.
- **숫자 강조**: `table.total` 문구를 rich text로 바꿔 숫자만 `font-medium text-foreground`로 보인다. 줄 전체는 지금처럼 `text-muted-foreground`다.

## R3. 처음·마지막 버튼 (FR-005)

- **Decision**: lucide `ChevronsLeft`·`ChevronsRight` 아이콘의 `ghost icon-sm` 버튼을 이전·다음 바깥에 둔다. 이름은 `table.first`("첫 쪽")와 `table.last`("마지막 쪽")이다. 비활성 조건은 각각 이전·다음과 같다.
- **Alternatives considered**: 양 끝 번호를 고정하고 말줄임(`1 … 5 6 7 … 40`)을 두는 안. 너비가 쪽 수에 따라 변하고 변경이 크다 (사용자 결정).

## R4. 표 본문 최소 높이 (FR-006)

- **Decision**: 줄 높이 토큰 `--table-row-height: 2.5rem`을 `layout-tokens.css`에 둔다. 표 머리(`h-10`)와 같은 높이다.
  - 데이터 행: 행 높이를 토큰으로 맞춘다. 지금은 셀 여백으로 약 37px이고, 40px로 조금 커진다.
  - 표 영역(가로 스크롤 상자): 최소 높이를 `토큰 × (N + 1)`로 둔다. N은 `min(쪽 크기, 10)`이고, +1은 표 머리다. 마지막 쪽에 몇 건만 있으면 행 아래가 상자 안에서 비어 보인다.
  - 빈 결과·오류 칸: 높이를 `토큰 × N`으로 둬서 안내가 본문 가운데에 놓인다.
  - 로딩: 지금처럼 Skeleton을 N줄 그리고, 각 줄은 토큰 높이다.
- **Rationale**: 행 높이가 토큰 하나로 정해져야 데이터 상태와 빈 상태의 높이가 정확히 같다. 키트가 디자인을 정할 때 이 값 하나로 바꾼다 (`layout-tokens.css` 머리 주석의 규칙).
- **Alternatives considered**: `tbody`에 min-height 주기. 표 요소에는 min-height가 듣지 않는다. 빈 행을 채워 넣는 안은 화면 낭독기가 빈 행을 읽는다.

## R5. 넘친 쪽 보내기 (FR-008)

- **Decision**: `DataTable`이 조회 결과를 받은 뒤 쪽 수를 확인한다. 받은 응답이 지금 쪽의 것이고(`data.page === search.page - 1`) `search.page > max(1, data.totalPages)`이면 `search.setPage(max(1, totalPages), { replace: true })`를 부른다. `useListSearch`의 쪽 이동에 `replace` 선택을 더하고, 이때 `router.push` 대신 `router.replace`를 쓴다.
- **Rationale**: 전체 쪽 수는 조회 결과로만 안다. `keepPreviousData` 때문에 새 쪽 응답이 오기 전에는 이전 응답이 남아 있으므로, 응답의 쪽 번호로 지금 쪽의 결과인지 확인해야 잘못 보내지 않는다. replace로 보내야 뒤로 가기가 넘친 주소로 돌아가지 않는다 (US3-AC3).
- **오류**: 조회가 실패하면 `data`가 없어 보내지 않는다.
- **Alternatives considered**: 1쪽으로 보내기. 마지막 쪽이 원래 보려던 위치에 더 가깝다 (사용자 결정). 서버가 고쳐서 돌려주기. API를 바꿔야 한다.

## R6. 빈 결과의 조건 초기화 (FR-007)

- **Decision**: 빈 결과 안내(`Empty`)에 `EmptyContent`를 더하고, 그 안에 `Button variant="outline" size="sm"`(`RotateCcw` 아이콘, `table.resetFilters` "조건 초기화")을 둔다. `search.isFiltered`일 때만 보인다. 누르면 `search.reset()`을 부르는데, 검색 영역의 초기화와 같은 함수다.
- **Rationale**: 같은 함수를 써야 동작이 어긋나지 않는다. `isFiltered`는 이미 빈 결과 문구("조건에 맞는 결과가 없어요" / "아직 등록된 내용이 없어요")를 고르는 데 쓰고 있다.

## R7. 테스트

- **Decision**: 테스트는 구현 뒤 마지막에 고친다 (CLAUDE.md). `e2e/list.spec.ts`(`/dev/list` 예시, 87건)에서 아래를 고치거나 더한다. 로컬에서는 돌리지 않고 PR CI로 본다.
  - 고침: 기본 20줄 → 10줄, `size=50` 주소는 그대로
  - 더함: 처음·마지막 버튼과 비활성, 넘친 쪽(`?page=99` → 마지막 쪽, 뒤로 가기), 조건이 걸린 빈 결과의 조건 초기화 버튼, 빈 결과와 데이터 상태에서 쪽 이동 줄의 세로 위치가 같음
- 다른 화면 e2e(접속 이력, 사용자, 공지)는 응답을 가짜로 주고 행 수를 기본값에 기대지 않아서 바뀌지 않을 것으로 본다. PR CI에서 확인한다.
