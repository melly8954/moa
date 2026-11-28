---

description: "이용약관·개인정보처리방침 공개 페이지 (terms-privacy) 작업 목록"
---

# Tasks: 이용약관·개인정보처리방침 공개 페이지 (terms-privacy)

**Input**: `specs/012-terms-privacy/`의 설계 문서

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/api.md, contracts/screens.md, quickstart.md

**Tests**: 원칙 II에 따라 AC마다 테스트를 하나 둔다. CLAUDE.md에 따라 기능 코드(백엔드·프론트)를 먼저 끝내고 테스트 작업은 맨 끝(Phase 7)에 둔다. 로컬에서는 빠른 검사(`.\gradlew.bat build`, `npm run check`)만 돌리고, DB 테스트와 e2e는 PR CI로 본다.

## Format: `[ID] [P?] [Story?] Description`

- **[P]**: 병렬 가능 (다른 파일, 의존 없음)
- **[Story]**: 작업이 속한 사용자 스토리 (US1 문서 읽기, US2 푸터 줄·가입 화면, US3 버전)
- 경로는 저장소 루트 기준이다. 백엔드 패키지 `backend/src/main/java/com/moa/`는 `B/`, 프론트 `frontend/src/`는 `F/`로 줄여 쓴다

---

## Phase 1: Setup (확인)

- [X] T001 구현 브랜치 `feat/012-terms-privacy`를 최신 `dev`에서 딴 뒤 확인한다: backend/src/main/resources/application.yaml의 `app.terms.service-version`이 `draft`인지, backend/gradle/helpers/rich-text.gradle에 jsoup(`org.jsoup:jsoup`)이 있는지, F/app 아래 `/terms`·`/privacy` 경로가 없는지, F/components/shell/sidebar-shell.tsx 본문 영역(`SidebarInset` 안 `div`)이 계획 때와 같은지. 다르면 plan·research를 먼저 고친다

---

## Phase 2: Foundational (문안·공개 API·문서 보기 부품·화면 틀)

**Purpose**: 세 스토리가 함께 쓰는 백엔드 문안 읽기와 API, 프론트 API·부품·레이아웃

### 백엔드

- [X] T002 [P] B/common/legal/: `LegalDocument`(`TERMS`→키 `terms`, `PRIVACY`→키 `privacy`, `static Optional<LegalDocument> fromKey(String)`), `LegalProperties`(`@ConfigurationProperties("app.legal")`, `location` 기본 `classpath:legal/`, `zone` 기본 `Asia/Seoul`, `@Validated`), `LegalSection`(record `id`, `heading`, `html`), `LegalVersion`(record `document`, `version`, `effectiveDate` `LocalDate`, `title`, `List<LegalSection> sections`)
- [X] T003 B/common/legal/LegalCatalog.java: `@Component`. 생성할 때 `PathMatchingResourcePatternResolver`로 `classpath*:` + 위치 + `*/*.json`을 찾아(research R1, 배포 jar에서도 읽히게) 문서 폴더 이름으로 `LegalDocument`를 정하고, Jackson으로 메타(`version`, `effectiveDate`, `title`)를 읽고 같은 이름의 `.html`을 jsoup으로 읽어 맨 위 `<section id>`마다 `LegalSection`(`id`, `h2` 글자, `h2`를 뺀 안쪽 HTML)을 만든다. 검사(data-model, research R6)를 어기면 `IllegalStateException`으로 시작을 막는다: 문서마다 시행일이 오늘(`zone`) 이전인 버전이 적어도 하나, 한 문서 안에서 시행일·버전 이름이 겹치지 않음, 파일 이름 = `version`이고 `version`은 `^[a-z0-9][a-z0-9.-]*$`, `effectiveDate`가 `YYYY-MM-DD`, `.json`마다 짝 `.html`, 본문 맨 위가 `section`+`h2`로만 이루어지고 장이 1개 이상이며 `id`가 겹치지 않음, 본문에 `<script`·`<iframe`·`<style`·`on…=` 속성·`javascript:`가 없음, `TermsProperties.serviceVersion`이 이용약관 버전에 있음. `List<LegalVersion> versions(LegalDocument)`는 시행일 최근순. 테스트가 위치와 오늘을 바꿀 수 있게 정적 팩토리(`load(ResourcePatternResolver, String location, LocalDate today, String serviceVersion)`)를 둔다
- [X] T004 [P] backend/src/main/resources/application.yaml: `app.legal.location: ${LEGAL_LOCATION:classpath:legal/}`, `app.legal.zone: ${LEGAL_ZONE:Asia/Seoul}`와 `# moa: 공개 문서 문안 위치·오늘을 정하는 시간대 (specs/012-terms-privacy research R1·R5)` 주석. `app.terms` 주석의 "문안은 frontend/src/content/terms/에" 중 이용약관은 `legal/terms/`로 고친다
- [X] T005 [P] backend/src/main/resources/legal/terms/draft.json(`{"version":"draft","effectiveDate":"2026-11-01","title":"moa 이용약관"}`)과 draft.html: frontend/src/content/terms/draft.json의 `SERVICE.body`(자리표시)를 장 구조(`<section id="purpose"><h2>제1조 (목적)</h2><p>…</p></section>` 등)로 옮긴다
- [X] T006 [P] backend/src/main/resources/legal/privacy/draft.json(`{"version":"draft","effectiveDate":"2026-11-01","title":"moa 개인정보처리방침"}`)과 draft.html: 장은 처리 목적·항목, 보유 기간(docs/privacy.md "보관 기간 표"와 같은 표), 파기 절차·방법(탈퇴 뒤 30일 안 다시 로그인하면 복구, 지나면 파기 — `018-member-withdrawal`, 글·댓글·파티는 남고 작성자만 "탈퇴한 회원"), 제3자 제공·처리 위탁·국외 이전(푸시 서비스), 정보주체의 권리·행사 방법, 안전성 확보 조치, 자동 수집 장치(쿠키 등), 개인정보 보호책임자, 처리방침 변경. 운영 주체·보호책임자·호스팅 업체는 `[운영 주체]`·`[보호책임자 직책]`·`[공용 이메일]`·`[호스팅 업체]` 빈칸 (spec Assumptions, 2026-11-21 초안)
- [X] T007 [P] B/api/dto/legal/: `VersionState`(`CURRENT`, `PAST`, `UPCOMING`), `LegalVersionDto`(`LegalVersion` + `state`), `LegalDocumentDto`(`current`, `upcoming`(nullable), `versions`)
- [X] T008 B/api/repository/reader/legal/LegalReader.java: `LegalCatalog`의 `versions(LegalDocument)`를 읽는다
- [X] T009 B/api/service/legal/LegalService.java: `document(String key, LocalDate today)`와 `version(String key, String version, LocalDate today)`. 키가 `terms`·`privacy`가 아니거나 버전이 없으면 `ServiceException(ErrorCode.NOT_FOUND)`(R001, research R4: 문자열로 받아 400이 되지 않게). 지금 = `effectiveDate ≤ today` 중 가장 늦은 것, 예정 = `effectiveDate > today` 중 가장 이른 것, 그 밖 PAST (research R5)
- [X] T010 B/api/application/v1/legal/LegalApplication.java: `today = LocalDate.now(ZoneId.of(legalProperties.zone()))`를 구해 서비스에 넘긴다(트랜잭션 없음)
- [X] T011 B/api/controller/v1/legal/ApiLegalController.java(`/api/v1/legal`, 클래스에 `@PermitAll`·`@SecurityRequirements`, `@Tag(name = "공개 문서 API")`): `GET /{document}` → `LegalDocumentResponse`, `GET /{document}/versions/{version}` → `LegalVersionResponse`(`currentVersion` 포함). `{document}`는 `String`. 응답 날짜는 `LocalDate`. 모양은 contracts/api.md

### 프론트

- [X] T012 [P] F/types/legal.ts(`LegalDocumentKey` = `"terms" | "privacy"`, 응답 타입, `VersionState`), F/api/legal.ts(`legalApi.get(document)`, `legalApi.version(document, version)`, 둘 다 `skipAuth: true`), F/api/index.ts에 `legal: legalApi`
- [X] T013 [P] F/components/legal/legal-document.tsx(클라이언트): 제목 `h1`, "시행일 YYYY-MM-DD · 버전 {version}" 줄, 목차(장 `heading` → `#id` 링크, `nav` `aria-label`), 장마다 `<section id>` + `h2` + 본문. 본문은 DOMPurify로 거른다: 허용 태그 `p`, `h3`, `ul`, `ol`, `li`, `a`, `strong`, `em`, `br`, `table`, `thead`, `tbody`, `tr`, `th`, `td`, `caption`, 허용 속성 `href`·`colspan`·`rowspan`·`scope`. 표는 `[&_table]:block [&_table]:overflow-x-auto`로 표 안에서만 가로 스크롤 (research R3). `compact` 속성이면 목차·`h1` 없이 높이 제한 상자(`max-h-48 overflow-y-auto`). DOMPurify는 브라우저 DOM이 있어야 하므로 키트 `rich-text-view.tsx`처럼 `useSyncExternalStore`로 브라우저에서만 거르고 서버 렌더에서는 빈 칸을 그린다(데이터도 브라우저에서 API로 받는다)
- [X] T014 [P] F/components/legal/legal-footer.tsx: "이용약관 · 개인정보처리방침"(처리방침 `font-semibold`), 작은 글자(`text-xs text-muted-foreground`), 가운데, `print:hidden`. 속성 `newTab`이면 `target="_blank" rel="noopener"`
- [X] T015 [P] frontend/messages/ko.json `legal` 묶음: 문서 이름(이용약관, 개인정보처리방침), 시행일·버전 줄, 목차, 안내 띠(바뀝니다·미리 보기, 지금 버전 아님·지금 버전 보기, 시행 전), 다른 버전 보기, 지금 시행 중·예정 표시, 없는 버전, 불러오기 실패, 가입 화면의 "더보기"·"개인정보처리방침 보기"
- [X] T016 F/app/(legal)/layout.tsx: 맨 위 머리 줄(왼쪽 "moa" → `/`, 오른쪽 두 문서 링크, 지금 문서에 `aria-current`, `print:hidden`), 본문 `main`(`max-w-3xl` 가운데), 맨 아래 T014 푸터 줄. 셸·로그인 확인 없음 (FR-001, FR-011, research R7)

**Checkpoint**: 공개 API가 응답하고 문서 보기 부품·레이아웃이 준비됐다

---

## Phase 3: User Story 1 - 누구나 지금 시행 중인 약관과 처리방침을 읽는다 (Priority: P1) 🎯 MVP

**Goal**: 로그인 여부·계정 종류와 상관없이 `/terms`, `/privacy`에서 지금 버전 전문을 읽는다

**Independent Test**: 로그인하지 않은 채, 회원으로, 관리자 계정으로 두 화면을 열어 같은 전문·버전·시행일이 보이는지 본다

- [X] T017 [US1] F/app/(legal)/terms/page.tsx, F/app/(legal)/privacy/page.tsx(클라이언트): react-query로 `legalApi.get(document)`, 불러오는 동안 뼈대(`Skeleton`), 오류면 공통 오류 문구와 다시 시도, 성공이면 `current`를 T013으로 그린다. 두 화면은 문서 키만 다른 같은 부품(F/components/legal/legal-page.tsx)을 쓴다

**Checkpoint**: 두 문서를 누구나 읽는다 (MVP)

---

## Phase 4: User Story 2 - 화면 아래 링크로 약관과 처리방침을 연다 (Priority: P1)

**Goal**: 로그인·가입 화면과 사용자 셸 본문 맨 아래 푸터 줄, 가입 화면 약관 칸의 전문 상자와 새 탭 더보기

**Independent Test**: 링크가 있어야 하는 화면마다 두 링크를 눌러 문서 화면이 열리는지, 가입 화면은 새 탭인지, 관리 셸에는 없는지 본다

- [X] T018 [P] [US2] F/app/(public)/sign-in/page.tsx, F/app/(public)/sign-in/recover/page.tsx: 화면 맨 아래에 T014 푸터 줄(같은 탭)
- [X] T019 [US2] F/components/shell/sidebar-shell.tsx: `footer?: React.ReactNode` 속성을 더하고, 본문 영역(`SidebarInset` 안)을 세로 flex로 두어 본문 아래에 `footer`를 그린다. 본문이 짧으면 화면 맨 아래에 붙는다(`mt-auto`). F/components/shell/user-shell.tsx만 `footer={<LegalFooter />}`를 넘긴다. admin-shell은 넘기지 않는다 (`001-admin-menus` FR-005 2026-11-28)
- [X] T020 [US2] F/app/(public)/sign-up/nickname/page.tsx: 약관 칸에서 요약·`Collapsible`(`showFull`)을 없앤다. 이용약관 칸은 `GET /api/v1/terms`의 `SERVICE` 버전으로 `legalApi.version("terms", version)`을 불러 T013의 `compact` 모양으로 전문을 보이고 아래에 "더보기"(`/terms`, 새 탭). 개인정보 수집·이용 칸은 지금 문안(`termsText("PRIVACY", version)`)의 본문을 높이 제한 상자(`max-h-48 overflow-y-auto whitespace-pre-line`)에 보이고 아래에 "개인정보처리방침 보기"(`/privacy`, 새 탭). `termsReady`는 이용약관 전문 응답까지 받은 뒤 참. 화면 맨 아래에 T014 푸터 줄(`newTab`) (spec FR-005, `010-social-login` FR-013·US1-AC7)
- [X] T021 [US2] F/content/terms/draft.json에서 `SERVICE`를 지우고, F/content/terms/index.ts의 `termsText`는 `PRIVACY`만 다룬다(타입도 `PRIVACY`로 좁힘). 주석에 이용약관 원문은 백엔드 `legal/terms/`라고 적는다 (FR-010, research R2). `messages/ko.json`의 쓰지 않게 된 `showFull` 키를 지운다

**Checkpoint**: 링크와 가입 화면이 스펙대로다

---

## Phase 5: User Story 3 - 바뀐다는 것을 미리 알고 예전 버전을 본다 (Priority: P2)

**Goal**: 시행 예정 안내 띠, 버전 목록, 예전·예정 버전 화면

**Independent Test**: 예전·지금·예정 버전이 있을 때 안내 띠, 목록, 각 버전 화면을 본다

- [X] T022 [P] [US3] F/components/legal/legal-versions.tsx: "다른 버전 보기"를 누르면 펼치는 버전 목록(시행일 최근순, `UPCOMING`은 "예정", `CURRENT`는 "지금 시행 중", 줄마다 `/{document}/versions/{version}` 링크). F/components/legal/legal-notice.tsx: 안내 띠 세 가지(지금 화면 + 예정 있음: "YYYY년 M월 D일부터 바뀝니다" + 미리 보기 링크 / 예전 버전: "지금 시행 중인 버전이 아니에요" + 지금 버전 링크 / 예정 버전: "YYYY년 M월 D일부터 시행돼요. 아직 시행 전이에요" + 지금 버전 링크), `print:hidden`
- [X] T023 [US3] T017의 legal-page.tsx에 `upcoming`이 있으면 안내 띠, 제목 아래 T022 버전 목록을 더한다
- [X] T024 [US3] F/app/(legal)/terms/versions/[version]/page.tsx, F/app/(legal)/privacy/versions/[version]/page.tsx(클라이언트): 버전 이름은 공지 고치기 화면(`notices/[id]`)처럼 `useParams<{ version: string }>()`로 읽는다. `legalApi.version(document, version)`, `state`에 따라 안내 띠, 버전 목록. API가 404 `R001`이면 "없는 버전이에요" + 지금 버전 링크 (contracts/screens.md)

**Checkpoint**: 세 스토리가 모두 동작한다

---

## Phase 6: Polish & Cross-Cutting

- [X] T025 frontend/에서 local 백엔드를 띄운 채 `npm run gen:api`로 F/types/schema.d.ts를 다시 만든다. T012의 손으로 쓴 타입은 공지처럼 그대로 둔다
- [X] T026 backend/에서 `.\gradlew.bat spotlessApply`, `.\gradlew.bat build`, 한 번 `bootRun`으로 실제 문안이 시작 검사를 통과하고 `GET /api/v1/legal/privacy`가 200인지 본다. frontend/에서 `npm run format`, `npm run check`
- [ ] T027 quickstart.md "화면 확인" 1~6과 "문안 대조"를 사람이 한다. 시험 계정 비밀번호가 필요하므로 에이전트는 하지 않는다 (원칙 VI)
- [ ] T028 사람이 한다: 010 담당 팀원에게 `010-social-login` FR-013·US1-AC7 변경(가입 화면 약관 칸은 전문 상자 + 새 탭 더보기, 이용약관 원문은 백엔드 `legal/terms/`)과 `social-login.spec.ts` 수정을 알린다
- [X] T029 PR 직전에 specs/README.md의 012 줄 상태를 "구현됨"으로 바꾸고, 키트로 옮길 목록에 "sidebar-shell 본문 푸터 자리(`footer` 속성)"를 더한다

---

## Phase 7: Tests (맨 끝, CLAUDE.md)

**Purpose**: AC마다 테스트 하나 (원칙 II), 이름에 AC 번호. 어느 AC를 무엇으로 보는지는 research R8과 contracts/api.md "AC와 확인 방법"

- [X] T030 backend/src/test/resources/legal-test/{terms,privacy}/{old,draft,next}.{json,html}: 문서마다 `old` 2000-01-01, `draft` 2020-01-01, `next` 2099-01-01. 처리방침 `draft.html`에는 표 하나
- [X] T031 backend/src/test/java/com/moa/api/LegalAcceptanceTest.java(`AcceptanceTest` 상속, `@TestPropertySource(properties = "app.legal.location=classpath:legal-test/")`): US1-AC1·US1-AC2(토큰 없이 처리방침·이용약관), US1-AC3(회원 토큰, `superAdmin()` 토큰이 같은 본문), US3-AC1(목록 순서·상태), US3-AC2(`old` PAST, `currentVersion`=`draft`), US3-AC3(`upcoming`, `next` UPCOMING), 없는 문서(`cookies`)·버전(`none`) 404 `R001`을 `@DisplayName("US1-AC1 …")`으로
- [X] T032 [P] backend/src/test/java/com/moa/api/service/legal/LegalServiceTest.java(단위): 오늘을 바꿔 지금·예정·예전을 고른다. US3-AC4: 시행일 전날은 예정, 당일부터 지금 버전
- [X] T033 [P] backend/src/test/java/com/moa/common/legal/LegalCatalogTest.java(단위): 실제 `classpath*:legal/*/*.json`이 검사를 통과한다(배포 jar와 같은 리소스 패턴). 잘못된 문안 폴더(backend/src/test/resources/legal-invalid/<경우>/…: 스크립트, 짝 `.html` 없음, 시행일 겹침, 시행된 버전 없음, 장 밖의 글, `service-version` 없음)마다 `IllegalStateException`
- [X] T034 [P] frontend/e2e/legal.spec.ts: `page.route`로 `/api/v1/legal/**`를 가로챈다. US1-AC3(관리자 계정 `me.role=ADMIN`으로 `/privacy`를 열면 다른 곳으로 보내지지 않고 그대로 머문다), US1-AC4(표만 가로 스크롤, 3폭 가로 넘침, 접근성), US1-AC5(목차 링크 → 장으로), US1-AC6(머리 줄 moa·다른 문서 링크, 관리자 계정이 moa를 누르면 `/admin`으로 간다), US2-AC1(로그인 화면 푸터 줄 → 문서), US2-AC2(사용자 셸 푸터 줄, 사이드바 접기·좁은 폭에서도 맨 아래), US2-AC3(처리방침 굵게), US2-AC5(관리 셸에 푸터 줄 없음), US3 화면 쪽(안내 띠, 버전 목록, 예전 버전 안내, 없는 버전)
- [X] T035 [P] frontend/e2e/social-login.spec.ts: `/api/v1/legal/terms/versions/draft`를 가로채고, US1-AC7을 010 FR-013 변경만큼 고친다(전문 상자, 펼치기 없음). US2-AC4: 가입 화면의 "더보기"·"개인정보처리방침 보기"·푸터 줄이 `target="_blank"`인지 (원칙 V)
  - 같은 변경으로 단위 테스트 frontend/tests/unit/sign-up-nickname-page.dom.test.tsx의 US1-AC7(요약·펼치기)도 전문 상자·새 탭 링크로 고쳤다 (`npm run check`에서 깨져 구현 때 함께 고침)
- [X] T036 로컬에서는 `.\gradlew.bat build`, `npm run check`만 돌린다. `dbTest`와 e2e는 PR CI로 확인한다

---

## Dependencies & Execution Order

- Phase 1 → Phase 2 → 스토리. T001은 모든 작업보다 먼저
- Phase 2 안: T002 → T003 → T008 → T009 → T010 → T011 (백엔드 사슬). T004~T007은 서로 기대지 않는다(T003은 T004의 설정, T005·T006의 문안이 있어야 실제로 뜬다). 프론트 T012~T015는 서로 기대지 않고, T016은 T014·T015 뒤
- US1(Phase 3)이 MVP: T017은 T011·T012·T013·T015·T016 뒤
- US2(Phase 4): T018·T019는 T014 뒤, T020은 T011·T012·T013 뒤, T021은 T020과 함께
- US3(Phase 5): T022 → T023(T017 뒤), T024
- Phase 6(T025는 T011 뒤) → Phase 7(테스트) → PR. T029는 PR 바로 전

## Parallel Example

```text
Phase 2 안: T002, T004, T005, T006, T007 (백엔드)와 T012, T013, T014, T015 (프론트)
US2 안: T018 (로그인 화면)과 T019 (셸)
Phase 7 안: T032, T033, T034, T035
```

## Implementation Strategy

1. **MVP**: Phase 1 → 2 → 3(US1). 두 문서를 누구나 읽는다 (공개 의무를 지킨다)
2. US2(푸터 줄, 가입 화면) → US3(버전 이력, 시행 예정 안내)
3. Polish → 테스트(맨 끝) → PR. 실서비스 배포 전에 처리방침 빈칸(운영 주체·보호책임자·호스팅 업체)과 이용약관 실제 문안·시행일을 채운다
