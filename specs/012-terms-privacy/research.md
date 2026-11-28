# Research: 이용약관·개인정보처리방침 공개 페이지 (terms-privacy)

Technical Context에 미결은 없다. 아래는 moa 프론트 코드(010 약관 문안, 공개 화면 틀, 셸)를 확인해 정한 것이다.

## R1. 문안을 두는 곳과 모양

- **Decision**: 프론트 `src/content/legal/`에 문서(`terms`, `privacy`)마다 폴더를 두고, 버전마다 TS 파일 하나(`<버전>.ts`)를 둔다. 파일은 `LegalVersion`을 낸다: `version`(버전 이름), `effectiveDate`(시행일 `YYYY-MM-DD`, 한국 날짜), `title`, `sections`(장마다 `{ id, heading, html }`). `src/content/legal/index.ts`가 문서별 버전 목록을 모은다
- 본문은 장마다 HTML 조각으로 쓴다(문단, 목록, 표, 링크, 조 제목 `h3`). 장 제목(`h2`)과 목차는 `sections`의 `heading`·`id`로 만든다
- **Rationale**:
  - FR-003(장·조 제목, 목록, 표, 목차). 010의 문안은 `summary`·`body` 평문(`whitespace-pre-line`)이라 표와 목차를 만들 수 없다. 가입 화면은 2026-11-28부터 요약 없이 전문을 보이므로(spec FR-005) 요약 칸도 두지 않는다
  - 새 의존성 없이 쓸 수 있다. 마크다운을 쓰려면 렌더러(`react-markdown`, `remark-gfm`)를 더해야 한다
  - 장을 나눠 두면 목차·바로 가기(`#id`)를 문안에서 다시 찾지 않아도 된다
  - 문안은 저장소에 있는 고정 코드라 JSX와 같은 믿음 수준이다. 그리는 곳에서 거르지 않고, 위험한 내용이 없는지는 배포 전 검사(R6)가 본다(R3)
- **Alternatives considered**:
  - 마크다운 파일 + `react-markdown`: 쓰기는 가장 편하지만 의존성 둘이 늘고, 키트 rich-text와 다른 렌더러가 생긴다
  - 장마다 JSX 컴포넌트: 법무 문안을 JSX로 쓰기 불편하고, 팀이 문안을 바꿀 때 문턱이 높다
  - 서버에 저장하고 관리 화면에서 고치기: spec 배경(배포로만 바꾼다)

## R2. 010 가입 화면과 원문 하나로 쓰기 (FR-010)

- **Decision**: 이용약관(`SERVICE`)의 원문은 `src/content/legal/terms/`로 옮기고, 010의 `src/content/terms/index.ts`의 `termsText(type, version)`은 `SERVICE`면 legal 목록에서 그 버전 이름의 문안을 찾는다. `PRIVACY`(개인정보 수집·이용 동의)는 처리방침과 다른 문서라 지금처럼 `src/content/terms/draft.json`에 둔다
  - 가입 화면의 약관 칸(spec FR-005, 2026-11-28): 요약·펼치기(`Collapsible`)를 없애고 전문을 높이가 정해진 상자(`max-h-48 overflow-y-auto`)에 바로 보인다. 이용약관은 R3의 문서 보기 부품(작은 모양, 목차 없음), 개인정보 수집·이용 동의는 지금의 평문(`whitespace-pre-line`). 상자 아래 "더보기"(`/terms`)와 "개인정보처리방침 보기"(`/privacy`)는 새 탭(`target="_blank" rel="noopener"`)
  - 가입 화면이 동의받는 버전은 지금처럼 서버 설정(`app.terms.service-version`, 010 R11)이 정한다. 공개 화면의 "지금 시행 중"은 시행일로 정한다. 둘이 다를 수 있다(spec Edge Cases)
  - 이용약관 첫 버전은 010의 자리표시 문안을 옮기고 버전 이름 `draft`, 시행일은 개발용으로 `2026-11-01`로 둔다. 실서비스 전에 팀 문안과 실제 시행일로 바꾼다 (010 tasks T056)
- **Rationale**: FR-010(같은 원본), 010 FR-013 줄(2026-11-27)
- **Alternatives considered**: 010의 `draft.json`에 시행일만 더하기. 평문이라 표·목차를 못 만든다(R1)

## R3. 문서 보기 부품

- **Decision**: `src/components/legal/legal-document.tsx`가 버전 하나를 그린다: 제목, 버전·시행일 줄, 목차(장 제목 링크), 장마다 `<section id>` + `<h2>` + 본문. 본문 HTML은 거르지 않고 `dangerouslySetInnerHTML`로 넣는다. 서버 컴포넌트라 서버에서 완성된 화면이 나간다. 표는 `overflow-x-auto` 상자로 감싸 표 안에서만 가로로 스크롤한다. 인쇄할 때는 화면 틀(머리 줄, 버전 안내 띠)을 `print:hidden`으로 감춘다
  - 가입 화면은 같은 부품의 작은 모양(목차 없음, 높이 제한 스크롤)을 쓴다
- **Rationale**: US1-AC4·AC5, Edge Cases(인쇄). 키트 `RichTextView`는 rich-text 허용 태그(표 없음)로 거르고, 거르기에 브라우저 DOM이 필요해 서버 렌더에서는 빈 칸을 그린다. 문서 화면은 서버에서 그리므로(R5) DOMPurify를 쓰면 처음 그림에 본문이 없다
- **거르지 않는 이유**: 문안은 회원이 쓴 글이 아니라 PR로만 바뀌는 고정 코드다. 실수로 들어간 스크립트·이벤트 속성은 R6의 단위 테스트가 막는다 (2026-11-28 plan 검토)
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
  - 본문 HTML에 `<script`, `<iframe`, `<style`, `on…=` 이벤트 속성, `javascript:` 주소가 없음 (R3: 거르지 않고 그리므로)
- **Rationale**: 문안이 코드 안에 있어 테스트로 막는 것이 가장 가깝다. 따로 스크립트를 두지 않는다
- **처리방침과 `docs/privacy.md` 맞추기(FR-009, SC-003)**: 자동으로 보지 않는다. 처리방침 문안이나 `docs/privacy.md`를 고치는 PR에서 사람이 대조한다(quickstart "문안 대조"). 문안에 보관 기간 표가 하나 있어 대조가 쉽다

## R7. 링크 자리 (FR-004, FR-005)

- **Decision** (2026-11-28, 푸터 줄):
  - 푸터 줄 하나를 `src/components/legal/legal-footer.tsx`로 만든다: "이용약관 · 개인정보처리방침"(처리방침 굵게), 작은 글자, 가운데. 속성 `newTab`이면 새 탭으로 연다
  - 로그인 화면(`/sign-in`, `/sign-in/recover`)과 가입 화면(`/sign-up/nickname`) 맨 아래에 둔다. 가입 화면만 `newTab`(spec FR-005, US2-AC4)
  - 사용자 셸: `sidebar-shell.tsx`의 본문 영역 맨 아래에 자리(`footer` 속성)를 더하고 `user-shell.tsx`만 푸터 줄을 넘긴다. 본문 영역을 세로 flex로 두어 본문이 짧으면 화면 맨 아래에 붙는다. 사이드바를 접거나 서랍이 되어도 본문 영역이라 같다. 관리 셸은 넘기지 않는다
  - 문서 화면(`(legal)` 레이아웃) 맨 아래에도 같은 줄을 둔다(인쇄 때 숨김)
- **Rationale**: spec US2, 001 FR-005
- **키트**: `sidebar-shell.tsx`의 자리 더하기는 키트로 옮길 목록에 더한다 (`moa-to-kit-backlog`)

## R8. 테스트

- **Decision**: 백엔드 변경이 없어 API 통합 테스트는 없다. AC는 단위 테스트와 e2e로 본다
  - 단위(`tests/unit/legal.test.ts`): 버전 고르기(US1-AC1·AC2, US3-AC1·AC4의 날짜 규칙), 배포 전 검사(R6)
  - e2e(`e2e/legal.spec.ts`): 로그인 없이·회원·관리자 계정으로 두 화면 열기(US1-AC1~AC3), 표·가로 넘침·접근성·목차(US1-AC4·AC5), 머리 줄 링크(US1-AC6), 로그인·가입·사용자 셸(펼침·접힘·좁은 폭) 푸터 줄, 가입 화면의 전문 상자와 새 탭, 관리 셸에 푸터 없음(US2-AC1~AC5), 버전 목록·예전 버전·예정 안내(US3-AC1~AC3). 날짜에 따라 바뀌는 것은 e2e에서 날짜를 바꾸지 않고 단위 테스트로 본다
  - e2e용 문안: 화면을 서버에서 그려 브라우저에서 가로챌 수 없고, 처음 배포 때 문서마다 버전이 하나뿐이라 예전·예정 화면을 실제 문안으로 볼 수 없다. 그래서 `src/content/legal/fixture.ts`를 두고, 환경 변수 `LEGAL_FIXTURE=true`일 때만 쓴다 (2026-11-28 plan 검토로 고침)
    - 테스트용 목록은 실제 목록을 **바꾸지 않고 더하기만** 한다: 문서마다 예전 버전(시행일 `2000-01-01`, 표 포함) 하나와 예정 버전(시행일 `2099-01-01`) 하나. 실제 `draft`가 그대로 지금 버전으로 남아 010 가입 e2e(`social-login.spec.ts`, 버전 `draft`)가 깨지지 않는다
    - e2e 서버는 운영 모드(`npm run build && next start`)로 뜬다. 그래서 `NODE_ENV`로 막지 않고, 이미 있는 `SHOW_DEV_PAGES=true`처럼 그 환경 변수가 있을 때만 켠다. 값은 `playwright.config.ts`의 `webServer.env`에서만 준다. 실서비스 배포 설정에는 넣지 않는다
  - 실제 문안 목록은 단위 테스트(R6)가 본다
- **Rationale**: 원칙 II(AC마다 테스트). 이 기능은 화면만 있다
- **Alternatives considered**: "오늘"을 환경 변수로 바꾸기. 실제 문안이 한 버전뿐이면 예전·예정 화면을 만들 수 없다
