# Research: 이용약관·개인정보처리방침 공개 페이지 (terms-privacy)

Technical Context에 미결은 없다. 아래는 moa 프론트 코드(010 약관 문안, 공개 화면 틀, 셸)를 확인해 정한 것이다.

## R1. 문안을 두는 곳과 모양

- **Decision**: 프론트 `src/content/legal/`에 문서(`terms`, `privacy`)마다 폴더를 두고, 버전마다 TS 파일 하나(`<버전>.ts`)를 둔다. 파일은 `LegalVersion`을 낸다: `version`(버전 이름), `effectiveDate`(시행일 `YYYY-MM-DD`, 한국 날짜), `title`, `summary`(가입 화면의 요약, 이용약관만), `sections`(장마다 `{ id, heading, html }`). `src/content/legal/index.ts`가 문서별 버전 목록을 모은다
- 본문은 장마다 HTML 조각으로 쓴다(문단, 목록, 표, 링크, 조 제목 `h3`). 장 제목(`h2`)과 목차는 `sections`의 `heading`·`id`로 만든다
- **Rationale**:
  - FR-003(장·조 제목, 목록, 표, 목차). 010의 문안은 `summary`·`body` 평문(`whitespace-pre-line`)이라 표와 목차를 만들 수 없다
  - 새 의존성 없이 쓸 수 있다. 마크다운을 쓰려면 렌더러(`react-markdown`, `remark-gfm`)를 더해야 한다
  - 장을 나눠 두면 목차·바로 가기(`#id`)를 문안에서 다시 찾지 않아도 된다
  - 문안은 우리가 쓴 고정 파일이지만, 그리는 곳에서 DOMPurify 기본 설정(표 포함)으로 한 번 거른다(R3)
- **Alternatives considered**:
  - 마크다운 파일 + `react-markdown`: 쓰기는 가장 편하지만 의존성 둘이 늘고, 키트 rich-text와 다른 렌더러가 생긴다
  - 장마다 JSX 컴포넌트: 법무 문안을 JSX로 쓰기 불편하고, 팀이 문안을 바꿀 때 문턱이 높다
  - 서버에 저장하고 관리 화면에서 고치기: spec 배경(배포로만 바꾼다)

## R2. 010 가입 화면과 원문 하나로 쓰기 (FR-010)

- **Decision**: 이용약관(`SERVICE`)의 원문은 `src/content/legal/terms/`로 옮기고, 010의 `src/content/terms/index.ts`의 `termsText(type, version)`은 `SERVICE`면 legal 목록에서 그 버전 이름의 `summary`와 본문을 찾는다. `PRIVACY`(개인정보 수집·이용 동의)는 처리방침과 다른 문서라 지금처럼 `src/content/terms/draft.json`에 둔다
  - 가입 화면의 펼친 전문은 평문 대신 R3의 문서 보기 부품(작은 모양)으로 그린다
  - 가입 화면이 동의받는 버전은 지금처럼 서버 설정(`app.terms.service-version`, 010 R11)이 정한다. 공개 화면의 "지금 시행 중"은 시행일로 정한다. 둘이 다를 수 있다(spec Edge Cases)
  - 이용약관 첫 버전은 010의 자리표시 문안을 옮기고 버전 이름 `draft`, 시행일은 개발용으로 `2026-11-01`로 둔다. 실서비스 전에 팀 문안과 실제 시행일로 바꾼다 (010 tasks T056)
- **Rationale**: FR-010(같은 원본), 010 FR-013 줄(2026-11-27)
- **Alternatives considered**: 010의 `draft.json`에 시행일만 더하기. 평문이라 표·목차를 못 만든다(R1)

## R3. 문서 보기 부품

- **Decision**: `src/components/legal/legal-document.tsx`가 버전 하나를 그린다: 제목, 버전·시행일 줄, 목차(장 제목 링크), 장마다 `<section id>` + `<h2>` + 본문. 본문은 DOMPurify 기본 설정(표 허용)으로 거른 HTML을 `dangerouslySetInnerHTML`로 넣는다(키트 `rich-text-view.tsx`와 같은 방식). 표는 `overflow-x-auto` 상자로 감싸 표 안에서만 가로로 스크롤한다. 인쇄할 때는 화면 틀(머리 줄, 버전 안내 띠)을 `print:hidden`으로 감춘다
  - 가입 화면은 같은 부품의 작은 모양(목차 없음, 높이 제한 스크롤)을 쓴다
- **Rationale**: US1-AC4·AC5, Edge Cases(인쇄). 키트 `RichTextView`는 rich-text 허용 태그(표 없음)로 걸러 쓰지 못한다
- **Alternatives considered**: `RichTextView`에 표를 허용하기. 키트 코드를 고치고, 회원이 쓰는 글(공지·팝업)에도 표가 열린다

## R4. 화면 경로와 화면 틀 (FR-001, FR-011)

- **Decision**: 새 경로 묶음 `src/app/(legal)/`에 레이아웃 하나와 화면 넷을 둔다
  - `/terms`, `/privacy`: 지금 시행 중인 버전
  - `/terms/versions/[version]`, `/privacy/versions/[version]`: 버전 하나(예전·예정)
  - 레이아웃: 맨 위 머리 줄에 moa 이름(`/`로 가는 링크)과 다른 문서 링크, 본문은 읽기 좋은 폭(`max-w-3xl`). 사용자 셸·관리 셸을 쓰지 않아 관리자 계정도 막히지 않는다. 로그인 확인을 하지 않는다
- **Rationale**: FR-001(누구나, 관리자 계정 포함), FR-011(돌아갈 길). `(public)` 레이아웃은 로그인 상자용으로 가운데 정렬만 해 긴 문서에 맞지 않는다(006 R7과 같은 이유)
- **Alternatives considered**: 사용자 셸 안에 두기. 관리자 계정이 막힌다(`001-admin-menus` FR-005)

## R5. 시행일이 되면 배포 없이 바뀌기 (FR-002, US3-AC4)

- **Decision**: 네 화면은 서버 컴포넌트이고 `export const dynamic = "force-dynamic"`으로 요청마다 그린다. 요청 때 오늘 날짜를 `SITE.timeZone`(Asia/Seoul)으로 구해 버전을 고른다. 고르는 규칙은 `src/lib/legal.ts`의 순수 함수(`currentVersion`, `upcomingVersion`, `versionState`)에 두고 단위 테스트로 본다
  - 지금 버전: `effectiveDate ≤ 오늘` 중 가장 늦은 것. 예정: `effectiveDate > 오늘` 중 가장 이른 것
- **Rationale**: 빌드 때 그리는 정적 화면은 날짜가 지나도 바뀌지 않는다(spec 검토 메모). 이 프로젝트는 요청마다 그리는 화면에 이미 `force-dynamic`을 쓴다(`(dev)` 화면들)
- **Alternatives considered**: 클라이언트에서 날짜로 고르기. 처음 그림과 바뀐 그림이 달라 깜박이고, 검색 엔진에 빈 화면이 보일 수 있다

## R6. 배포 전 검사 (FR-008)

- **Decision**: 단위 테스트(`tests/unit/legal.test.ts`)가 `src/content/legal/index.ts`를 읽어 문서마다 다음을 본다. `npm run check`(PR CI `check`)에서 돈다
  - 시행일이 오늘 이전인 버전이 적어도 하나
  - 한 문서 안에서 시행일과 버전 이름이 겹치지 않음
  - `effectiveDate`가 `YYYY-MM-DD` 꼴, `sections`가 비지 않고 `id`가 겹치지 않음
- **Rationale**: 문안이 코드 안에 있어 테스트로 막는 것이 가장 가깝다. 따로 스크립트를 두지 않는다
- **처리방침과 `docs/privacy.md` 맞추기(FR-009, SC-003)**: 자동으로 보지 않는다. 처리방침 문안이나 `docs/privacy.md`를 고치는 PR에서 사람이 대조한다(quickstart "문안 대조"). 문안에 보관 기간 표가 하나 있어 대조가 쉽다

## R7. 링크 자리 (FR-004, FR-005)

- **Decision**:
  - 로그인 화면(`/sign-in`, `/sign-in/recover`)과 가입 화면(`/sign-up/nickname`) 맨 아래: `src/components/legal/legal-links.tsx`의 "이용약관 · 개인정보처리방침" 줄. 가입 화면의 약관 칸 옆 링크와 맨 아래 줄은 새 탭(`target="_blank" rel="noopener"`)으로 연다(FR-005, US2-AC4). 로그인 화면은 같은 탭
  - 사용자 셸: `sidebar-shell.tsx`의 계정 줄 밑에 자리(`footer` 속성)를 더하고 `user-shell.tsx`만 링크 줄을 넘긴다. 관리 셸은 넘기지 않는다. 접힌 사이드바에서는 정보 아이콘 단추(`Info`)와 두 링크가 든 메뉴(`001-admin-menus` FR-005 2026-11-27 줄)
- **Rationale**: spec US2, 001 FR-005
- **키트**: `sidebar-shell.tsx`의 자리 더하기는 키트로 옮길 목록에 더한다 (`moa-to-kit-backlog`)

## R8. 테스트

- **Decision**: 백엔드 변경이 없어 API 통합 테스트는 없다. AC는 단위 테스트와 e2e로 본다
  - 단위(`tests/unit/legal.test.ts`): 버전 고르기(US1-AC1·AC2, US3-AC1·AC4의 날짜 규칙), 배포 전 검사(R6)
  - e2e(`e2e/legal.spec.ts`): 로그인 없이·회원·관리자 계정으로 두 화면 열기(US1-AC1~AC3), 표·가로 넘침·접근성·목차(US1-AC4·AC5), 머리 줄 링크(US1-AC6), 로그인·가입·사이드바(펼침·접힘·서랍)·관리 셸 링크(US2-AC1~AC5), 버전 목록·예전 버전·예정 안내(US3-AC1~AC3). 날짜에 따라 바뀌는 것은 e2e에서 날짜를 바꾸지 않고 단위 테스트로 본다
  - e2e용 문안: 화면을 서버에서 그려 브라우저에서 가로챌 수 없고, 처음 배포 때 문서마다 버전이 하나뿐이라 예전·예정 화면을 실제 문안으로 볼 수 없다. 그래서 `src/content/legal/fixture.ts`(시행된 버전 둘 + 예정 버전 하나, 표 포함)를 두고, 환경 변수 `LEGAL_CONTENT=fixture`일 때만 그 목록을 쓴다. Playwright가 띄우는 서버에만 이 값을 준다. `NODE_ENV=production`에서는 이 값을 무시한다
  - 실제 문안 목록은 단위 테스트(R6)가 본다
- **Rationale**: 원칙 II(AC마다 테스트). 이 기능은 화면만 있다
- **Alternatives considered**: "오늘"을 환경 변수로 바꾸기. 실제 문안이 한 버전뿐이면 예전·예정 화면을 만들 수 없다
