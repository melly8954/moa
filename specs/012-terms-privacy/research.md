# Research: 이용약관·개인정보처리방침 공개 페이지 (terms-privacy)

Technical Context에 미결은 없다. 아래는 moa 코드(010 약관 API·설정, 018 정리 시간대, 공개 화면 틀, 셸)를 확인해 정한 것이다.

2026-11-28: 처음 plan은 문안을 프론트 코드에 두고 Next 서버에서 그렸다. 사용자가 "다른 화면처럼" 하자고 해서, 문안은 백엔드가 공개 API로 내려 주고 화면은 그 API를 받아 브라우저에서 그리는 구조로 바꿨다 (spec Assumptions 고침).

## R1. 문안을 두는 곳과 모양

- **Decision**: 백엔드 리소스 `src/main/resources/legal/<문서>/<버전>.json`에 버전마다 파일 하나를 둔다. DB에 넣지 않는다
  - 파일 칸: `version`(버전 이름), `effectiveDate`(시행일 `YYYY-MM-DD`, 한국 날짜), `title`, `sections`(장마다 `{ "id", "heading", "html" }`)
  - 본문은 장마다 HTML 조각이다(문단, `h3` 조 제목, 목록, 표, 링크). 목차는 `sections`의 `heading`·`id`로 만든다
  - 문서 키는 `terms`(이용약관), `privacy`(개인정보처리방침). 백엔드 enum `LegalDocument`(`TERMS`, `PRIVACY`), 주소에는 소문자
  - 리소스 위치는 설정 `app.legal.location`(기본 `classpath:legal/`)이다. 인수 테스트는 테스트용 위치를 준다(R8)
- **Rationale**:
  - spec FR-007(버전마다 따로, 배포로 더함, 지우지 않음)과 FR-003(장·조, 목록, 표, 목차)
  - 다른 화면처럼 백엔드 API에서 받는다(2026-11-28). 010의 약관 종류·지금 버전이 이미 백엔드(`TermsType`, `TermsProperties`)에 있어 원문도 같은 쪽에 둔다
  - 법무 문안을 PR로만 바꾸니 DB와 관리 화면이 필요 없다. 나중에 관리 화면이 필요해지면 이 파일을 DB로 옮긴다
- **Alternatives considered**:
  - 프론트 코드에 두고 Next 서버에서 그리기(처음 plan): 다른 화면과 구조가 달라진다
  - DB 테이블: 관리 화면이 없는데 테이블·마이그레이션·시드가 늘어난다
  - 마크다운 파일: 렌더러 의존성이 늘고 표·목차 처리가 따로 필요하다

## R2. 010 가입 화면과 원문 하나로 쓰기 (FR-010)

- **Decision**:
  - 이용약관(`SERVICE`)의 원문은 R1의 `legal/terms/`로 옮긴다. 010의 프론트 문안 `src/content/terms/draft.json`의 `SERVICE`는 지운다
  - 010 가입 화면은 이용약관 칸의 전문을 `GET /api/v1/legal/terms/versions/{가입 동의 버전}`으로 받는다. 가입 동의 버전은 지금처럼 `GET /api/v1/terms`(`app.terms.service-version`)가 준다
  - `PRIVACY`(개인정보 수집·이용 동의)는 처리방침과 다른 문서라 지금처럼 프론트 `src/content/terms/draft.json`에 둔다
  - 백엔드는 시작할 때 `app.terms.service-version`이 `legal/terms/`에 있는 버전인지 본다. 없으면 시작하지 않는다
  - 이용약관 첫 버전은 010 자리표시 문안을 옮기고 버전 이름 `draft`, 시행일은 개발용 `2026-11-01`로 둔다. 실서비스 전에 팀 문안과 실제 시행일로 바꾼다 (010 tasks T056)
- **가입 화면 약관 칸** (spec FR-005, 2026-11-28): 요약·펼치기를 없애고 전문을 높이가 정해진 상자에 바로 보인다. 이용약관은 R3의 문서 보기 부품(작은 모양, 목차 없음), 개인정보 수집·이용 동의는 지금의 평문(`whitespace-pre-line`). 상자 아래 "더보기"(`/terms`)와 "개인정보처리방침 보기"(`/privacy`)는 새 탭(`target="_blank" rel="noopener"`)
- **Rationale**: FR-010(같은 원본), 010 FR-013 줄. 동의 버전과 원문이 같은 백엔드에 있어 시작 때 맞는지 볼 수 있다
- **Alternatives considered**: 010 동의서(`PRIVACY`)도 백엔드로 옮기기. 012가 다루는 공개 문서가 아니고 010 담당의 문안이라 이번에는 건드리지 않는다

## R3. 문서 보기 부품

- **Decision**: 프론트 `src/components/legal/legal-document.tsx`(클라이언트)가 버전 하나를 그린다: 제목, 버전·시행일 줄, 목차(장 제목 링크), 장마다 `<section id>` + `<h2>` + 본문. 본문 HTML은 DOMPurify로 표 태그를 허용해 걸러 `dangerouslySetInnerHTML`로 넣는다(키트 `rich-text-view.tsx`와 같은 방식, 허용 목록만 다르다). 표는 `overflow-x-auto` 상자로 감싸 표 안에서만 가로로 스크롤한다. 인쇄할 때는 화면 틀(머리 줄, 안내 띠, 푸터 줄)을 `print:hidden`으로 감춘다
  - 가입 화면은 같은 부품의 작은 모양(목차 없음, 높이 제한 스크롤)을 쓴다
- **Rationale**: US1-AC4·AC5, Edge Cases(인쇄). 화면을 브라우저에서 그리므로 DOMPurify를 쓸 수 있다. 문안은 고정 파일이지만 API를 거쳐 오므로 한 번 거른다. 키트 `RichTextView`는 rich-text 허용 태그(표 없음)라 그대로 쓰지 못한다
- **Alternatives considered**: `RichTextView`에 표를 허용하기. 키트 코드를 고치고, 회원이 쓰는 글(공지·팝업)에도 표가 열린다

## R4. 공개 API (백엔드)

- **Decision**: `ApiLegalController`(`/api/v1/legal`, `@PermitAll`, `@SecurityRequirements`)
  - `GET /api/v1/legal/{document}`: 지금 시행 중인 버전 전문 + 예정 버전(있으면 버전·시행일) + 버전 목록(버전·시행일·상태)
  - `GET /api/v1/legal/{document}/versions/{version}`: 그 버전 전문과 상태(`CURRENT`·`PAST`·`UPCOMING`), 지금 버전 이름
  - `{document}`가 `terms`·`privacy`가 아니거나 없는 버전이면 404 `R001`
  - 계층: 컨트롤러 → `LegalApplication`(트랜잭션 없음, 오늘 날짜를 구해 넘김) → `LegalService`(버전 고르기, 없으면 R001) → `LegalReader`(R1의 목록을 읽음). 목록 읽기는 `common/legal/LegalCatalog`(리소스를 시작 때 한 번 읽어 들고 있음)
- **Rationale**: spec FR-001(누구나), FR-002·FR-006. 레이어 규칙(`ArchitectureTest`)을 따른다. 리소스는 시작 때 한 번 읽어 요청마다 파일을 읽지 않는다
- **Alternatives considered**: 010의 `/api/v1/terms`에 붙이기. 그 API는 "가입 때 동의받을 버전"이라 뜻이 다르다

## R5. 지금 버전 고르기와 시간대 (FR-002, US3-AC4)

- **Decision**: 오늘은 `Asia/Seoul` 날짜다(`app.legal.zone`, 기본 `Asia/Seoul`). 018 정리기가 `cleanup-zone`으로 서버 시간대와 상관없이 한국 시각을 쓰는 것과 같은 방식이다
  - 지금 버전: `effectiveDate ≤ 오늘` 중 가장 늦은 것. 예정: `effectiveDate > 오늘` 중 가장 이른 것. 그 밖의 시행된 버전은 예전
  - 요청마다 오늘을 구하므로 시행일이 되면 배포 없이 바뀐다
  - 고르기는 `LegalService`의 순수 로직이고 오늘 날짜를 인자로 받아 단위 테스트한다
- **Rationale**: spec Edge Cases(시행일 0시 한국 시간). 서버 시간대에 기대지 않는다
- **Alternatives considered**: 브라우저 날짜로 고르기. 사용자 컴퓨터 시계·시간대에 따라 달라진다

## R6. 배포 전 검사 (FR-008)

- **Decision**: `LegalCatalog`가 시작할 때 리소스를 읽으며 검사하고, 어기면 애플리케이션이 뜨지 않는다. 같은 검사를 단위 테스트(`LegalCatalogTest`, `./gradlew build`)로도 본다
  - 문서마다 시행일이 오늘 이전인 버전이 적어도 하나
  - 한 문서 안에서 시행일과 버전 이름이 겹치지 않음
  - 파일 이름과 `version`이 같음, `version`은 영문 소문자·숫자·`-`·`.`만
  - `effectiveDate`가 `YYYY-MM-DD` 꼴, `sections`가 비지 않고 `id`가 겹치지 않음
  - 본문 HTML에 `<script`, `<iframe`, `<style`, `on…=` 이벤트 속성, `javascript:` 주소가 없음
  - `app.terms.service-version`이 이용약관 버전에 있음 (R2)
- **Rationale**: 문안이 배포 파일이라 잘못되면 시작 때 막는 것이 가장 가깝다. 단위 테스트는 PR CI `build`에서 먼저 잡는다
- **처리방침과 `docs/privacy.md` 맞추기(FR-009, SC-003)**: 자동으로 보지 않는다. 처리방침 문안이나 `docs/privacy.md`를 고치는 PR에서 사람이 대조한다(quickstart "문안 대조")

## R7. 화면 경로·화면 틀·푸터 줄 (FR-001, FR-004, FR-011)

- **Decision**:
  - 새 경로 묶음 `src/app/(legal)/`: `/terms`, `/privacy`, `/terms/versions/[version]`, `/privacy/versions/[version]`. 화면은 다른 화면처럼 클라이언트 컴포넌트이고 react-query로 R4의 API를 부른다. 레이아웃은 맨 위 머리 줄(moa 이름 `/` 링크, 두 문서 링크)과 맨 아래 푸터 줄, 본문은 읽기 좋은 폭(`max-w-3xl`). 사용자 셸·관리 셸을 쓰지 않아 관리자 계정도 막히지 않는다. 로그인 확인을 하지 않는다
  - 푸터 줄 `src/components/legal/legal-footer.tsx`: "이용약관 · 개인정보처리방침"(처리방침 굵게), 작은 글자, 가운데. 속성 `newTab`이면 새 탭
  - 로그인 화면(`/sign-in`, `/sign-in/recover`)·가입 화면(`/sign-up/nickname`) 맨 아래에 둔다. 가입 화면만 `newTab`
  - 사용자 셸: `sidebar-shell.tsx`의 본문 영역 맨 아래에 자리(`footer` 속성)를 더하고 `user-shell.tsx`만 푸터 줄을 넘긴다. 본문 영역을 세로 flex로 두어 본문이 짧으면 화면 맨 아래에 붙는다. 관리 셸은 넘기지 않는다
- **Rationale**: spec US1·US2, `001-admin-menus` FR-005(2026-11-28 푸터 줄). `(public)` 레이아웃은 로그인 상자용 가운데 정렬이라 긴 문서에 맞지 않는다(006 R7과 같은 이유). 사용자 셸에 두면 관리자 계정이 막힌다
- **키트**: `sidebar-shell.tsx`의 푸터 자리 더하기는 키트로 옮길 목록에 더한다 (`moa-to-kit-backlog`)

## R8. 테스트

- **Decision**:
  - 인수 테스트 `LegalAcceptanceTest`(`AcceptanceTest` 상속, PR CI `dbTest`): 테스트용 리소스 위치(`app.legal.location=classpath:legal-test/`)에 문서마다 예전(`old`, `2000-01-01`)·지금(`draft`, `2020-01-01`)·예정(`next`, `2099-01-01`) 세 버전을 두고(지금 버전 이름이 `draft`라 R6의 `app.terms.service-version` 검사를 통과한다) API로 US1-AC1·AC2·AC3(로그인 없이·회원·관리자 토큰), US3-AC1·AC2·AC3, 없는 문서·버전 404를 본다
  - 단위 테스트(`./gradlew build`): `LegalServiceTest`(오늘을 넣어 지금·예정·예전 고르기, US3-AC4의 시행일 당일 전환), `LegalCatalogTest`(R6 검사, 실제 `legal/` 리소스가 통과)
  - e2e `e2e/legal.spec.ts`: 다른 화면처럼 `page.route`로 `/api/v1/legal/**`를 가로챈다. 표·가로 넘침·접근성·목차(US1-AC4·AC5), 머리 줄(US1-AC6), 로그인·가입·사용자 셸 푸터 줄과 관리 셸에 없음(US2-AC1~AC3·AC5), 가입 화면 전문 상자와 새 탭(US2-AC4), 안내 띠·버전 목록·예전 버전 화면(US3 화면 쪽)
  - 010 가입 e2e(`social-login.spec.ts`)는 이용약관 전문을 새 API에서 받으므로 그 경로도 가로챈다
- **Rationale**: 원칙 II(API 통합 테스트 기본, e2e는 핵심 흐름). 테스트용 문안은 테스트 리소스에만 있어 운영 빌드에 섞이지 않는다
- **Alternatives considered**: e2e 서버에 테스트용 문안을 켜는 환경 변수(처음 plan). API를 가로채면 필요 없다
