---

description: "구글 소셜 로그인 (google-login) 작업 목록"
---

# Tasks: 구글 소셜 로그인 (google-login)

**Input**: `specs/010-social-login/`의 설계 문서

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/auth-oauth.md, contracts/users-me-and-screens.md, quickstart.md

**Tests**: 원칙 II에 따라 AC마다 테스트를 하나 둔다. CLAUDE.md에 따라 기능 코드(백엔드·프론트)를 먼저 끝내고 테스트 작업은 맨 끝(Phase 10)에 둔다. 로컬에서는 빠른 검사(`.\gradlew.bat build`, `npm run check`)만 돌리고, DB 테스트와 e2e는 PR CI로 본다.

## Format: `[ID] [P?] [Story?] [공용?] Description`

- **[P]**: 병렬 가능 (다른 파일, 의존 없음)
- **[Story]**: 작업이 속한 사용자 스토리 (US1~US5)
- **[공용]**: 다른 기능·키트·여러 기능이 함께 쓰는 파일을 고치는 작업. **구현 전에 바꿀 내용을 보고하고 승인받는다**
- **(잠정)**: 스펙 [NEEDS CLARIFICATION]이나 팀 확인(키트 oauth 헬퍼, FR-004 잠정 B)에 기댄 부분. 확정되면 그 작업을 고친다
- 경로는 저장소 루트 기준이다. 백엔드 패키지 `backend/src/main/java/com/moa/`는 `B/`로 줄여 쓴다

---

## Phase 1: Setup (번호 확인, 헬퍼 확인, 설정)

**Purpose**: 구현 직전에 겹칠 수 있는 번호를 다시 확인하고, 키트 헬퍼 유무에 따라 Phase 2의 공급자 클라이언트 작업을 정한다

- [ ] T001 구현 직전 dev 최신을 다시 확인한다: `git fetch` 뒤 `origin/dev`의 backend/src/main/resources/db/migration 최신 버전(계획 때 `V2_6` → 새 파일 `V2_7`)과 B/common/exception/ErrorCode.java의 최신 A 코드(계획 때 `A007` → 새 코드 `A008`~`A013`)를 확인한다. 다르면 이 tasks.md, data-model.md, contracts/auth-oauth.md의 번호를 먼저 고친다
- [ ] T002 키트 oauth 헬퍼 상태를 팀(키트 담당/팀 리더)에게 확인한다 (사람이 한다, research R2). 있으면: 헬퍼를 얹는 작업으로 T011~T013을 바꾸고, 헬퍼 구조가 data-model.md와 다르면 data-model.md·T005~T008을 헬퍼에 맞춘다. 없으면: 아래 작업대로 moa가 직접 만든다
- [ ] T003 [P] [공용] backend/.env.example에 `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `GOOGLE_REDIRECT_URI` 이름과 설명 주석만 더한다(값 없음, 원칙 VI). 구현 전에 바꿀 내용을 보고하고 승인받는다
- [ ] T004 [P] [공용] backend/src/main/resources/application.yaml에 `# moa: 구글 로그인 (specs/010-social-login)` 문서로 `app.oauth.google.client-id: ${GOOGLE_CLIENT_ID:}`, `client-secret: ${GOOGLE_CLIENT_SECRET:}`, `redirect-uri: ${GOOGLE_REDIRECT_URI:}`을 더한다 (research R10). 구현 전에 바꿀 내용을 보고하고 승인받는다

**Checkpoint**: 번호와 헬퍼 방향이 정해졌다

---

## Phase 2: Foundational (데이터, 오류 코드, 공급자 경계, 공개 경로)

**Purpose**: 모든 스토리가 쓰는 테이블·엔터티·오류 코드·구글 연동 경계·서명 쿠키·공개 경로

**⚠️ CRITICAL**: US1~US5 모두 이 단계에 기댄다

- [ ] T005 [공용] backend/src/main/resources/db/migration/V2_7__user_social_accounts.sql을 만든다 (data-model.md, 잠정 B): `user_social_accounts(id BIGINT PK AUTO_INCREMENT, user_id BIGINT NOT NULL, provider VARCHAR(20) NOT NULL, subject VARCHAR(255) NOT NULL, email VARCHAR(255) NULL, email_verified BOOLEAN NOT NULL, display_name VARCHAR(100) NULL, created_at DATETIME(6) NOT NULL)`, `UNIQUE (provider, subject)`, `UNIQUE (user_id, provider)`, `KEY (user_id)`, 머리 주석 `-- moa: 구글 로그인. 스펙 specs/010-social-login (data-model.md)`. 번호는 T001 결과를 따른다. 구현 전에 바꿀 내용을 보고하고 승인받는다
- [ ] T006 [P] B/api/entity/user/SocialProvider.java(enum `GOOGLE`)와 B/api/entity/user/UserSocialAccount.java(엔터티, 위 칸 그대로, 생성 뒤 email·display_name을 바꾸는 메서드를 두지 않는다 — "구글에서 바뀌어도 고치지 않는다")를 만든다
- [ ] T007 [P] B/api/repository/jpa/user/UserSocialAccountRepository.java, B/api/repository/reader/user/UserSocialAccountReader.java(`findByProviderAndSubject`, `findAllByUserId`, `existsByUserId`), B/api/repository/writer/user/UserSocialAccountWriter.java(저장, 회원·공급자로 지우기)를 기존 user reader/writer 모양대로 만든다
- [ ] T008 [P] [공용] B/common/exception/ErrorCode.java에 contracts/auth-oauth.md 표의 코드 6개를 더한다: `OAUTH_PROVIDER_DISABLED`(A008, 404), `OAUTH_FAILED`(A009, 400), `SOCIAL_LINK_NOT_ALLOWED`(A010, 403), `SOCIAL_MEMBER_ROLE_NOT_ALLOWED`(A011, 409), `SOCIAL_ACCOUNT_IN_USE`(A012, 409), `SOCIAL_UNLINK_LAST_METHOD`(A013, 409). 번호는 T001 결과를 따른다. 구현 전에 바꿀 내용을 보고하고 승인받는다
- [ ] T009 [P] B/common/security/oauth/GoogleOAuthProperties.java(`@ConfigurationProperties("app.oauth.google")` record: clientId, clientSecret, redirectUri, `enabled()` = clientId가 비어 있지 않음)를 만든다 (research R10)
- [ ] T010 [P] B/common/security/oauth/OAuthIdentity.java(record: provider, subject, email, emailVerified, name)와 공급자 경계 인터페이스 B/common/security/oauth/OAuthProviderClient.java(`authorizationUrl(state, nonce, codeChallenge)`, `exchange(code, codeVerifier, nonce) → OAuthIdentity`)를 만든다 (research R2 경계, (잠정) 헬퍼가 오면 이 구현을 바꾼다)
- [ ] T011 B/common/security/oauth/GoogleOAuthClient.java: T010 구현. 인가 URL(`https://accounts.google.com/o/oauth2/v2/auth`, scope `openid email profile`, `code_challenge_method=S256`, nonce), `RestClient`로 토큰 엔드포인트 호출(client secret, PKCE verifier), 받은 ID 토큰의 `iss`·`aud`·`exp`·`nonce` 확인 (research R2 대안, OIDC 3.1.3.7). 실패는 `ServiceException(OAUTH_FAILED)`. (잠정) T002에서 헬퍼가 확인되면 헬퍼로 바꾼다
- [ ] T012 B/common/security/oauth/OAuthCookies.java: `oauth_state`(state, nonce, PKCE verifier, mode `LOGIN`/`LINK`, next, userId?) — 10분, Path `/api/v1/auth/oauth`, HttpOnly, SameSite `Lax` / `oauth_signup`(provider, subject, email, emailVerified, name, next) — 10분, 같은 Path, HttpOnly, SameSite `Strict`를 jjwt HMAC으로 서명해 쓰고 읽고 지운다. claim `typ`로 용도를 나누고 Secure는 `AuthProperties`의 쿠키 설정을 따른다 (research R3·R5, data-model "저장하지 않는 상태")
- [ ] T013 B/common/security/oauth/OAuthPublicPathsConfig.java: `PublicPaths` 빈으로 `/api/v1/auth/oauth/**`를 연다. 공용 `SecurityConfig`는 고치지 않는다 (research R1, `WebSocketConfig.webSocketPublicPaths` 모양). `next` 검사 유틸(같은 사이트 경로만, `/sign-in` 제외 — 프론트 `safeNextPath`와 같은 규칙)을 같은 패키지에 둔다
- [ ] T014 B/api/service/user/SocialAccountService.java 틀: `findLinkedUser(identity)`, `isManagerAccount(userId)`(시스템 역할 `ADMIN`이거나 업무 역할 보유 — `RoleUserService`/rbac reader로 판단, research R7), `isSocialMember(userId)`(연결 하나 이상)
- [ ] T015 [P] B/api/controller/v1/auth/ApiOAuthController.java: `GET /api/v1/auth/oauth/providers` → `{ providers: ["google"] | [] }` (contracts, US3-AC3·R9). 이후 스토리의 엔드포인트도 이 컨트롤러에 더한다
- [ ] T016 [P] frontend/src/api/index.ts에 `api.v1.auth.oauthProviders()`를 더한다 (타입은 T048의 `npm run gen:api` 뒤 정리)

**Checkpoint**: 테이블·경계·쿠키·공개 경로가 준비됐다. 구글 단추 노출 여부를 서버가 알려 준다

---

## Phase 3: User Story 1 - 처음 온 사람이 구글 계정으로 가입한다 (Priority: P1) 🎯 MVP

**Goal**: 연결된 회원이 없는 구글 계정이 닉네임을 넣어 회원(USER, 비밀번호 없음)이 되고 로그인한다. 닉네임 전에는 회원을 만들지 않는다. 구글 회원에게 관리 역할을 줄 수 없다

**Independent Test**: quickstart 2·3·6 — 처음 쓰는 구글 계정으로 닉네임 화면을 거쳐 `/`에 들어가고, 닉네임 화면에서 떠나면 회원이 없고, 관리자가 `ADMIN`을 줄 수 없다

- [ ] T017 [US1] ApiOAuthController에 `GET /api/v1/auth/oauth/google/authorize?mode=login&next=`: 설정 없으면 302 `/sign-in?oauthError=disabled`, 있으면 state·nonce·PKCE를 만들어 `oauth_state`(LOGIN) 쿠키를 심고 302로 구글 인가 화면 (contracts "authorize")
- [ ] T018 [US1] B/api/application/v1/auth/SocialLoginApplication.java: 콜백 처리 `handleCallback(code, state, error, request, response)` — `oauth_state` 확인·삭제, `exchange`, LOGIN이고 연결 없으면 회원을 만들지 않고 `oauth_signup` 쿠키를 심어 `/sign-up/nickname?next=…`로 보낼 결과를 돌려준다 (US1-AC1·AC3·AC5, US5-AC4, research R5). `@Transactional`을 두지 않는다(`AuthTokenApplication` 주석 규칙)
- [ ] T019 [US1] ApiOAuthController에 `GET /api/v1/auth/oauth/google/callback`을 더하고 T018 결과대로 302를 보낸다 (contracts 콜백 표)
- [ ] T020 [US1] SocialAccountService에 `signUp(identity, nickname)`: 한 트랜잭션으로 `users`(login_id `g_` + 무작위 영숫자 12자, 겹치면 다시 — (잠정) FR-004 (c); name = 닉네임; password null; role `USER`)와 `user_social_accounts`를 만든다. `(provider, subject)` 유일 위반은 `SOCIAL_ACCOUNT_IN_USE`로 바꾼다 (동시 가입 Edge Case). 사용자 생성은 기존 `UserWriter`/키트 생성 경로를 쓴다
- [ ] T021 [US1] 닉네임 검증 (잠정) "2~20자, 앞뒤 공백 제거"를 요청 record(B/api/controller/v1/auth/request/OAuthSignUpRequest.java)에 둔다. 중복·글자 규칙은 spec FR-005 [NEEDS CLARIFICATION] 확정 뒤 고친다
- [ ] T022 [US1] ApiOAuthController에 `POST /api/v1/auth/oauth/google/sign-up`: `oauth_signup` 쿠키 확인(없거나 만료 A009) → T020 → `AuthTokenApplication.issue(user.toPrincipal())` → `RefreshTokenTransport.write` → `AuthRefreshResponse` 응답, 쿠키 삭제 (contracts "sign-up", FR-012)
- [ ] T023 [US1] [공용] B/api/service/user/UserAdminService.java `changeRole`: 대상이 구글 회원(`SocialAccountService.isSocialMember`)이고 새 역할이 `ADMIN`이면 `SOCIAL_MEMBER_ROLE_NOT_ALLOWED` (FR-007, US1-AC6). 키트 user-admin 헬퍼 파일이다. 구현 전에 바꿀 내용을 보고하고 승인받는다
- [ ] T024 [US1] [공용] B/api/service/role/RoleUserService.java `requireNotAdmin`·`requireTargets`(업무 역할 부여 경로 `replaceRolesOfUser`, `addMembers`): 대상에 구글 회원이 있으면 `SOCIAL_MEMBER_ROLE_NOT_ALLOWED` (FR-007, US1-AC6). 키트 rbac 헬퍼 파일이다. 구현 전에 바꿀 내용을 보고하고 승인받는다
- [ ] T025 [P] [US1] frontend/src/app/(public)/sign-in/page.tsx: `oauthProviders()`에 google이 있으면 "구글로 계속하기" 단추를 두고 누르면 `/api/v1/auth/oauth/google/authorize?next=<currentNextPath>`로 `window.location` 이동. 기존 아이디·비밀번호 폼은 그대로 (FR-001, US3-AC3)
- [ ] T026 [P] [US1] frontend/src/app/(public)/sign-up/nickname/page.tsx(새): react-hook-form + zod(2~20자, 잠정)로 닉네임을 받아 `api.v1.auth.oauthSignUp`(frontend/src/api/index.ts에 더함) → 성공 시 기존 로그인 성공 처리(`queryClient.clear()`, `useAuthStore.signIn`, `next` 또는 `landingPath`) (FR-005, FR-009). 실패 문구(A009 → 로그인 화면 안내, A012 → 이미 가입됨)
- [ ] T027 [P] [US1] frontend/src/app/(public)/auth/google/done/page.tsx(새): `POST /api/v1/auth/token/refresh`로 액세스 토큰을 받아 기존 로그인 성공 처리, 실패하면 `/sign-in?oauthError=failed` (research R4). 화면은 진행 표시만
- [ ] T028 [US1] [공용] frontend/messages/ko.json: `signIn.google`(단추), 새 `signUp` 묶음(닉네임 화면 제목·설명·칸·단추·오류)을 더한다. 문구 검사(`npm run check`) 규칙을 따른다. 구현 전에 바꿀 내용을 보고하고 승인받는다
- [ ] T029 [US1] 사용자 관리 드로어·역할 관리에서 A011 응답이 기존 오류 표시로 안내되는지 확인하고, 안 되면 문구만 더한다 (frontend `user-drawer.tsx`·역할 관리 화면, [공용] 해당 시 T028과 함께 보고)

**Checkpoint**: 새 구글 계정으로 가입·로그인 된다 (MVP)

---

## Phase 4: User Story 2 - 구글 회원이 다시 로그인한다 (Priority: P1)

**Goal**: 연결된 구글 계정은 같은 회원으로 로그인하고, 잠긴 회원은 막히며, 비밀번호 없는 계정에는 "비밀번호 변경"이 보이지 않는다

**Independent Test**: quickstart 4·5 — 다시 로그인해도 회원 수가 그대로, 계정 메뉴에 비밀번호 변경이 없다

- [ ] T030 [US2] SocialLoginApplication 콜백 LOGIN 분기: 연결된 회원이 있으면 잠김(`isLocked`)은 `/sign-in?oauthError=locked`(A007), 아니면 `AuthTokenApplication.issue` → `RefreshTokenTransport.write` → `/auth/google/done?next=…` (FR-003, US2-AC1·AC2, US5-AC2)
- [ ] T031 [US2] [공용] `GET /api/v1/users/me` 응답(B/.../UserGetResponse.java)에 `hasPassword`(boolean)를 더한다 (contracts/users-me-and-screens.md). 구현 전에 바꿀 내용을 보고하고 승인받는다
- [ ] T032 [P] [US2] frontend/src/types/user.ts `Me`에 `hasPassword`를 더한다
- [ ] T033 [US2] [공용] frontend/src/components/shell/account-menu.tsx: `useMe()`의 `hasPassword`가 거짓이면 "비밀번호 변경" 항목을 숨긴다 (US2-AC3). 두 셸이 함께 쓰는 키트 셸 파일이다. 구현 전에 바꿀 내용을 보고하고 승인받는다

**Checkpoint**: 가입한 구글 회원이 다시 들어온다

---

## Phase 5: User Story 3 - 구글 로그인이 안 될 때 안내를 받는다 (Priority: P2)

**Goal**: 취소·확인 실패·설정 없음에서 로그인 화면에 머물고 알맞은 안내를 받는다

**Independent Test**: quickstart 1·11 — 설정이 없으면 단추가 없고, 취소하면 안내 없이 로그인 화면이다

- [ ] T034 [US3] SocialLoginApplication: `error=access_denied`는 아무것도 하지 않고 LOGIN `/sign-in`·LINK `/settings`로, state 불일치·만료·교환·검증 실패는 LOGIN `/sign-in?oauthError=failed`·LINK `/settings?googleLink=failed`로 보낸다. 쿠키가 없을 때 모드는 LOGIN으로 본다 (US3-AC1·AC2, contracts 콜백 표)
- [ ] T035 [US3] 설정 없음: authorize·callback·sign-up이 `GoogleOAuthProperties.enabled()`가 거짓이면 A008 또는 `/sign-in?oauthError=disabled` (US3-AC3)
- [ ] T036 [US3] frontend/src/app/(public)/sign-in/page.tsx: `oauthError`(`failed`·`locked`·`disabled`)면 폼 위에 문구를 보이고, 쿼리가 없으면 아무것도 보이지 않는다. 문구는 T028의 ko.json 변경에 함께 넣는다 ([공용] 보고에 포함)

**Checkpoint**: 실패해도 원인을 알 수 있다

---

## Phase 6: User Story 5 - 내 계정에 구글 계정을 연결하고 해제한다 (Priority: P2)

**Goal**: 로그인한 회원이 계정 설정에서 구글을 연결·해제한다. 관리자·운영자는 연결이 없고, 비밀번호 없는 회원은 해제할 수 없다

**Independent Test**: quickstart 7~10

- [ ] T037 [US5] SocialAccountService에 `prepareLink(userId)`(관리자·운영자면 `SOCIAL_LINK_NOT_ALLOWED`, 이미 구글 연결이면 `INVALID_STATE`), `link(userId, identity)`(다른 회원에 있으면 `SOCIAL_ACCOUNT_IN_USE`, 관리자·운영자면 `SOCIAL_LINK_NOT_ALLOWED`), `unlink(userId)`(비밀번호 없으면 `SOCIAL_UNLINK_LAST_METHOD`, 연결 없으면 그대로)를 더한다 (FR-006·FR-007, research R6·R7)
- [ ] T038 [US5] B/api/controller/v1/user/ 아래 내 외부 계정 컨트롤러: `POST /api/v1/users/me/social-accounts/google/link` → `prepareLink` → `oauth_state`(LINK, userId) 쿠키 → `{ authorizeUrl }`, `DELETE /api/v1/users/me/social-accounts/google` → `unlink` → 204 (contracts)
- [ ] T039 [US5] ApiOAuthController authorize `mode=link`: LINK `oauth_state`가 없거나 만료면 302 `/settings?googleLink=failed`, 있으면 구글 인가 화면으로 (contracts "authorize")
- [ ] T040 [US5] SocialLoginApplication 콜백 LINK 분기: `link` 결과대로 `/settings?googleLink=linked|inUse|notAllowed|failed` (US5-AC1·AC3·AC7)
- [ ] T041 [US5] [공용] `UserGetResponse`에 `socialAccounts`(`{ provider, email, linkedAt }[]`)와 `socialLinkAllowed`(관리자·운영자면 거짓)를 더한다. T031과 같은 파일이므로 보고를 함께 한다. 구현 전에 바꿀 내용을 보고하고 승인받는다
- [ ] T042 [P] [US5] frontend/src/types/user.ts `Me`에 `socialAccounts`, `socialLinkAllowed`를 더하고 frontend/src/api/index.ts에 `api.v1.user.linkGoogle()`, `api.v1.user.unlinkGoogle()`을 더한다
- [ ] T043 [US5] [공용] frontend/src/app/(account)/settings/page.tsx: `socialLinkAllowed`일 때만 "구글 계정" 줄 — 연결 안 됨이면 연결 단추(`linkGoogle()` 뒤 `authorizeUrl`로 이동), 연결됨이면 이메일과 해제 단추(`hasPassword` 거짓이면 해제 대신 "유일한 로그인 수단" 안내), `googleLink` 쿼리로 결과 문구. 기존 푸시 설정 화면 파일이다. 구현 전에 바꿀 내용을 보고하고 승인받는다
- [ ] T044 [US5] [공용] frontend/messages/ko.json `settings` 묶음에 구글 연결 문구(줄 제목, 연결·해제 단추, 결과 문구 linked·inUse·notAllowed·failed, 해제 불가 안내)를 더한다. 구현 전에 바꿀 내용을 보고하고 승인받는다

**Checkpoint**: 기존 계정과 구글을 잇고 끊는다

---

## Phase 7: User Story 4 - 관리자가 구글 로그인 기록을 본다 (Priority: P3)

**Goal**: 구글 로그인 성공·실패가 접속 이력에 남는다

**Independent Test**: quickstart 12

- [ ] T045 [US4] SocialLoginApplication·가입(T022): 성공(로그인·가입)과 실패(잠김, state·교환·검증 실패)에 기존 `LoginAttemptedEvent.succeeded/failed`를 발행한다. `loginId` 칸 값은 (잠정) "회원이 있으면 그 회원의 loginId, 없으면 `google:` + subject 앞 8자" — spec US4-AC1 [NEEDS CLARIFICATION] 확정 뒤 고친다 (research R8). `LoginHistoryRecorder`(키트)는 고치지 않는다
- [ ] T046 [US4] 취소(`access_denied`)는 이력을 남기지 않는다(로그인 시도 전). 이 판단을 SocialLoginApplication 주석에 적는다

**Checkpoint**: 접속 이력에 구글 로그인이 보인다

---

## Phase 8: Polish & Cross-Cutting

- [ ] T047 backend/에서 `.\gradlew.bat spotlessApply`, `.\gradlew.bat build` (DB 테스트 제외)
- [ ] T048 frontend/에서 `npm run gen:api`(local 백엔드 실행 중)로 `src/types/schema.d.ts`를 다시 만들고, T016·T026·T042의 손으로 쓴 타입을 생성 타입으로 바꾼 뒤 `npm run format`, `npm run check`
- [ ] T049 specs/010-social-login/quickstart.md "수동 확인" 1~12를 사람이 직접 수행한다. 구글 OAuth 클라이언트 만들기와 `.env.local` 값 넣기도 사람이 한다 (원칙 VI)
- [ ] T050 spec "나중에 고칠 곳" 목록(002 FR-004·FR-007·가정, 003 업무 역할, 008 가정·FR-027 — 팀 공유 필요(담당: 008-party 담당), 005, README 경계)을 PR 설명에 적어 각 담당에게 알린다 (사람이 한다. 이 기능에서 그 스펙들을 고치지 않는다)
- [ ] T051 키트로 옮길 것을 PR 설명에 적는다: user-admin·rbac 서비스의 구글 회원 막기(T023·T024), 셸 계정 메뉴 숨김(T033), 공급자 클라이언트(헬퍼가 없을 때, T010~T013) (원칙 III)

---

## Phase 9: (예약) 헬퍼 확정 반영

- [ ] T052 T002에서 키트 oauth 헬퍼가 확인되면: 헬퍼를 얹고 T010~T013 구현을 헬퍼로 바꾼 뒤 spec 출처의 [NEEDS CLARIFICATION]을 풀고 research R2를 결정으로 고친다. 헬퍼가 없기로 정해지면 출처를 "moa"로 확정한다

---

## Phase 10: Tests (맨 끝, CLAUDE.md)

**Purpose**: AC마다 테스트 하나 (원칙 II), 이름에 AC 번호. 구글은 부르지 않는다

- [ ] T053 backend/src/test/java/com/moa/support/FakeOAuthProviderClient.java: 테스트에서 `OAuthProviderClient`를 바꾸는 가짜(정해 둔 code → 정해 둔 `OAuthIdentity`, 실패 code → `OAUTH_FAILED`)와 `@TestConfiguration`. 헬퍼로 바뀌면 그 경계에 맞춘다
- [ ] T054 backend/src/test/java/com/moa/api/GoogleLoginAcceptanceTest.java(`AcceptanceTest` 상속): authorize → callback(쿠키 이어 붙이기) → sign-up 흐름으로 US1-AC1~AC6, US2-AC1~AC2, US3-AC1~AC3(설정 끈 프로필·속성 덮어쓰기), US4-AC1(접속 이력 성공·실패 행), US5-AC1~AC7(link·unlink·me 칸)을 `@DisplayName("US1-AC1 …")`으로 하나씩 둔다. 동시 가입 Edge Case(유일 제약 → A012)도 하나 둔다. US2-AC3·US5-AC7의 화면 부분은 `/users/me`의 `hasPassword`·`socialLinkAllowed` 값으로 확인한다
- [ ] T055 [P] 기존 수용 테스트가 깨지지 않는지 본다: `AdminUsersAcceptanceTest`(역할 변경), 역할 관리 수용 테스트, `UserAccountAcceptanceTest`(`/users/me` 칸 추가). 깨지면 원칙 V대로 스펙부터 확인한다
- [ ] T056 [P] frontend 단위 테스트: account-menu의 비밀번호 변경 숨김(`hasPassword` 거짓), sign-in의 단추 표시(`providers` 빈/있음)와 `oauthError` 문구, 닉네임 검증(2~20자, 잠정)
- [ ] T057 [P] frontend/e2e/google-login.spec.ts: 핵심 흐름 US1(단추 → 닉네임 → 첫 화면)만. 백엔드 콜백 이동은 Playwright 경로 가로채기로 흉내 낸다 (원칙 II "e2e는 핵심 흐름만")
- [ ] T058 로컬에서는 `.\gradlew.bat build`, `npm run check`만 돌린다. `dbTest`와 e2e는 PR CI로 확인하고, 사용자가 요청할 때만 로컬에서 `.\gradlew.bat dbTest --tests "*GoogleLoginAcceptanceTest"`를 돌린다

---

## Dependencies & Execution Order

- Phase 1 → Phase 2 → 스토리. T001은 모든 작업보다 먼저, T002 결과가 T010~T013을 정한다
- US1(Phase 3)이 MVP. US2(Phase 4)는 T018·T019 콜백 위에 분기를 더하므로 US1 뒤
- US3(Phase 5)는 콜백·로그인 화면(T019·T025) 뒤
- US5(Phase 6)는 콜백(T019)과 `UserGetResponse`(T031) 뒤
- US4(Phase 7)는 콜백·가입(T018·T022) 뒤
- Phase 8 → Phase 10(테스트) 순서. Phase 9는 헬퍼 확인이 나오는 대로
- [공용] 작업(T003·T004·T005·T008·T023·T024·T028·T031·T033·T041·T043·T044, T029·T036 해당 시)은 착수 전 보고·승인이 끝나야 시작한다. 보고는 묶어서 한 번에 해도 된다

## Parallel Example

```text
Phase 2 안: T006, T007, T008, T009, T010, T015, T016 (서로 다른 파일)
US1 안: T025, T026, T027 (프론트 화면 셋) — 백엔드 T017~T024와 동시에
US5 안: T042 (프론트 타입·API)와 T037~T040 (백엔드)
Phase 10 안: T055, T056, T057
```

## Implementation Strategy

1. **MVP**: Phase 1 → 2 → 3(US1). 새 구글 계정 가입·로그인만으로 배포 검토 가능
2. US2(다시 로그인, 비밀번호 변경 숨김) → US3(실패 안내) → US5(연결·해제) → US4(접속 이력)
3. Polish → 테스트(맨 끝) → PR. 잠정 작업(헬퍼, FR-004, 닉네임 규칙, 접속 이력 표시, 삭제 회원 재가입)은 확정되는 대로 해당 작업을 고친다
