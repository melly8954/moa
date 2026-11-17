---

description: "소셜 로그인 (social-login) 작업 목록"
---

# Tasks: 소셜 로그인 (social-login)

**Input**: `specs/010-social-login/`의 설계 문서

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/auth-oauth.md, contracts/users-me-and-screens.md, quickstart.md

**Tests**: 원칙 II에 따라 AC마다 테스트를 하나 둔다. CLAUDE.md에 따라 기능 코드(백엔드·프론트)를 먼저 끝내고 테스트 작업은 맨 끝(Phase 10)에 둔다. 로컬에서는 빠른 검사(`.\gradlew.bat build`, `npm run check`)만 돌리고, DB 테스트와 e2e는 PR CI로 본다.

## Format: `[ID] [P?] [Story?] [공용?] Description`

- **[P]**: 병렬 가능 (다른 파일, 의존 없음)
- **[Story]**: 작업이 속한 사용자 스토리 (US1~US5)
- **[공용]**: 다른 기능·키트·여러 기능이 함께 쓰는 파일을 고치는 작업. **구현 전에 바꿀 내용을 보고하고 승인받는다** (공용 작업 15개, 파일 11개 — plan.md "공용 파일")
- **(잠정)**: 스펙 [NEEDS CLARIFICATION]이나 팀 확인(키트 oauth 헬퍼, FR-004 잠정 B, 디스코드 PKCE, 약관 문안)에 기댄 부분. 확정되면 그 작업을 고친다
- 경로는 저장소 루트 기준이다. 백엔드 패키지 `backend/src/main/java/com/moa/`는 `B/`로 줄여 쓴다

---

## Phase 1: Setup (번호 확인, 헬퍼·제공자 확인, 설정)

**Purpose**: 구현 직전에 겹칠 수 있는 번호를 다시 확인하고, 키트 헬퍼 유무와 디스코드 PKCE 지원에 따라 Phase 2의 제공자 클라이언트 작업을 정한다

- [ ] T001 구현 직전 dev 최신을 다시 확인한다: `git fetch` 뒤 `origin/dev`의 backend/src/main/resources/db/migration 최신 버전(계획 때 `V2_6` → 새 파일 `V2_7`)과 B/common/exception/ErrorCode.java의 최신 A 코드(계획 때 `A007` → 새 코드 `A008`~`A017`)를 확인한다. 다르면 이 tasks.md, data-model.md, contracts/auth-oauth.md의 번호를 먼저 고친다
- [ ] T002 사람이 확인한다 (research R2): (1) 키트 oauth 헬퍼 상태를 팀(키트 담당/팀 리더)에게 묻는다. 있으면 헬퍼를 얹는 작업으로 T010~T014를 바꾸고, 헬퍼 구조가 data-model.md와 다르면 data-model.md·T005~T008을 헬퍼에 맞춘다. 없으면 아래 작업대로 moa가 직접 만든다. (2) 디스코드가 PKCE(`code_challenge`)를 받는지 확인한다. 받지 않으면 구현 전에 팀에 다시 묻는다 (팀 결정 "둘 다 PKCE")
- [ ] T003 [P] [공용] backend/.env.example에 `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `GOOGLE_REDIRECT_URI`, `DISCORD_CLIENT_ID`, `DISCORD_CLIENT_SECRET`, `DISCORD_REDIRECT_URI`, `SOCIAL_REJOIN_HASH_SECRET` 이름과 설명 주석만 더한다(값 없음, 원칙 VI). 재가입 해시 비밀 값은 "운영 중에 바꾸지 않는다"를 주석에 적는다 (research R13). 구현 전에 바꿀 내용을 보고하고 승인받는다
- [ ] T004 [P] [공용] backend/src/main/resources/application.yaml에 `# moa: 소셜 로그인 (specs/010-social-login)` 문서로 `app.oauth.google.client-id: ${GOOGLE_CLIENT_ID:}`·`client-secret`·`redirect-uri`, `app.oauth.discord.*`(같은 모양), `app.oauth.rejoin-hash-secret: ${SOCIAL_REJOIN_HASH_SECRET:}`, `app.terms.service-version`·`privacy-version`(잠정 `draft`)을 더한다 (research R10·R11). 구현 전에 바꿀 내용을 보고하고 승인받는다

**Checkpoint**: 번호와 헬퍼·PKCE 방향이 정해졌다

---

## Phase 2: Foundational (데이터, 오류 코드, 제공자 경계, 가입·연결 확인, 공개 경로)

**Purpose**: 모든 스토리가 쓰는 테이블·엔터티·오류 코드·제공자 연동 경계·서명 쿠키·가입 확인·공개 경로

**⚠️ CRITICAL**: US1~US5 모두 이 단계에 기댄다

- [ ] T005 [공용] backend/src/main/resources/db/migration/V2_7__social_login.sql을 만든다 (data-model.md): `user_social_accounts`(provider `GOOGLE`/`DISCORD`, email NOT NULL, `UNIQUE (provider, subject)`, `UNIQUE (user_id, provider)`, `KEY (user_id)`, `KEY (email)`), `user_terms_agreements`(`UNIQUE (user_id, terms_type, terms_version)`), `social_rejoin_blocks`(provider, `subject_hash CHAR(64)` UNIQUE, `rejoin_available_at` NULL = 영구, created_at). 머리 주석 `-- moa: 소셜 로그인. 스펙 specs/010-social-login (data-model.md). social_rejoin_blocks의 행은 회원 탈퇴 스펙이 쓴다`. 번호는 T001 결과를 따른다. 구현 전에 바꿀 내용을 보고하고 승인받는다
- [ ] T006 [P] B/api/entity/user/ 아래 엔터티: `SocialProvider`(enum `GOOGLE`, `DISCORD`), `UserSocialAccount`(생성 뒤 email·display_name을 바꾸는 메서드를 두지 않는다), `TermsType`(enum `SERVICE`, `PRIVACY`), `UserTermsAgreement`, `SocialRejoinBlock`(읽기 전용, `isBlocking(now)` = `rejoinAvailableAt`이 null이거나 now보다 뒤)
- [ ] T007 [P] B/api/repository/{jpa,reader,writer}/user/ 아래: `UserSocialAccountReader`(`findByProviderAndSubject`, `findAllByUserId`, `existsByUserId`, `existsByEmailAndUserIdNot`, `existsByEmail`), `UserSocialAccountWriter`(저장, 회원·제공자로 지우기), `UserTermsAgreementWriter`(저장), `SocialRejoinBlockReader`(`findBySubjectHash`). 쓰기 쪽은 재가입 제한 기록에 만들지 않는다(회원 탈퇴 스펙 몫). 기존 user reader/writer 모양대로
- [ ] T008 [P] [공용] B/common/exception/ErrorCode.java에 contracts/auth-oauth.md 표의 코드 10개를 더한다: `OAUTH_PROVIDER_DISABLED`(A008, 404), `OAUTH_FAILED`(A009, 400), `SOCIAL_LINK_NOT_ALLOWED`(A010, 403), `SOCIAL_MEMBER_ROLE_NOT_ALLOWED`(A011, 409), `SOCIAL_ACCOUNT_IN_USE`(A012, 409), `SOCIAL_UNLINK_LAST_METHOD`(A013, 409), `SOCIAL_EMAIL_UNVERIFIED`(A014, 400), `SOCIAL_EMAIL_IN_USE`(A015, 409), `SOCIAL_REJOIN_RESTRICTED`(A016, 403), `TERMS_NOT_AGREED`(A017, 400). 번호는 T001 결과를 따른다. 구현 전에 바꿀 내용을 보고하고 승인받는다
- [ ] T009 [P] B/common/security/oauth/OAuthProperties.java(`@ConfigurationProperties("app.oauth")`: google·discord 각각 clientId·clientSecret·redirectUri와 `enabled()`, `rejoinHashSecret`. 제공자가 하나라도 켜져 있는데 `rejoinHashSecret`이 비면 시작 실패)와 B/api/…/TermsProperties.java(`app.terms.service-version`, `privacy-version`)를 만든다 (research R10·R11)
- [ ] T010 [P] B/common/security/oauth/OAuthIdentity.java(record: provider, subject, email, emailVerified, name), 제공자 경계 인터페이스 OAuthProviderClient.java(`provider()`, `authorizationUrl(state, nonce, codeChallenge)`, `exchange(code, codeVerifier, nonce) → OAuthIdentity`), 설정된 클라이언트만 모은 OAuthProviderRegistry.java(`{provider}` 경로 값 → 클라이언트, 없거나 꺼졌으면 A008)를 만든다 (research R2 경계, (잠정) 헬퍼가 오면 바꾼다)
- [ ] T011 [P] B/common/security/oauth/GoogleOAuthClient.java: 인가 URL(`https://accounts.google.com/o/oauth2/v2/auth`, scope `openid email profile`, `code_challenge_method=S256`, nonce), `RestClient`로 토큰 엔드포인트 호출(client secret, PKCE verifier), 받은 ID 토큰의 `iss`·`aud`·`exp`·`nonce` 확인, `sub`·`email`·`email_verified`·`name`을 신원으로 (research R2, OIDC 3.1.3.7). 실패는 `ServiceException(OAUTH_FAILED)`. (잠정) 헬퍼가 확인되면 바꾼다
- [ ] T012 [P] B/common/security/oauth/DiscordOAuthClient.java: 인가 URL(`https://discord.com/oauth2/authorize`, scope `identify email`, `code_challenge_method=S256`), `RestClient`로 토큰 엔드포인트(`https://discord.com/api/oauth2/token`) 호출, 받은 액세스 토큰으로 `GET https://discord.com/api/users/@me` → `id`·`email`·`verified`·`global_name`(없으면 `username`)을 신원으로. 액세스 토큰은 저장하지 않는다. nonce는 쓰지 않는다 (research R2). 실패는 `OAUTH_FAILED`. (잠정) T002의 PKCE 확인 결과를 따른다
- [ ] T013 B/common/security/oauth/OAuthCookies.java: `oauth_state`(provider, state, nonce?, PKCE verifier, mode `LOGIN`/`LINK`, next, userId?) — 10분, Path `/api/v1/auth/oauth`, HttpOnly, SameSite `Lax` / `oauth_signup`(provider, subject, email(정규화), name, next) — 10분, 같은 Path, HttpOnly, SameSite `Strict`를 jjwt HMAC으로 서명해 쓰고 읽고 지운다. claim `typ`로 용도를 나누고 Secure는 `AuthProperties`의 쿠키 설정을 따른다 (research R3·R5, data-model "저장하지 않는 상태")
- [ ] T014 B/common/security/oauth/OAuthPublicPathsConfig.java: `PublicPaths` 빈으로 `/api/v1/auth/oauth/**`, `/api/v1/terms`를 연다. 공용 `SecurityConfig`는 고치지 않는다 (research R1, `WebSocketConfig.webSocketPublicPaths` 모양). `next` 검사 유틸(같은 사이트 경로만, `/sign-in` 제외 — 프론트 `safeNextPath`와 같은 규칙)을 같은 패키지에 둔다
- [ ] T015 B/common/security/oauth/SocialRejoinHasher.java: `hash(provider, subject)` = `HMAC-SHA256(rejoinHashSecret, "<provider>:<subject>")` 16진수 64자 (JDK `Mac`, research R13)
- [ ] T016 B/api/service/user/SocialAccountService.java 틀: `findLinkedUser(identity)`, `isManagerAccount(userId)`(시스템 역할 `ADMIN`이거나 업무 역할 보유 — `RoleUserService`/rbac reader로 판단, research R7), `isSocialMember(userId)`(연결 하나 이상), 이메일 정규화(앞뒤 공백 제거, 소문자, research R12)
- [ ] T017 SocialAccountService에 가입·연결 확인을 더한다 (research R12·R13): `checkSignUp(identity)`와 `checkLink(userId, identity)` — 순서대로 이메일 없음·미인증(`SOCIAL_EMAIL_UNVERIFIED`) → 재가입 제한(T015 해시로 `SocialRejoinBlockReader` 조회, `isBlocking(now)`이면 `SOCIAL_REJOIN_RESTRICTED`) → 이메일 중복(가입은 `existsByEmail`, 연결은 `existsByEmailAndUserIdNot(email, userId)` → `SOCIAL_EMAIL_IN_USE`)
- [ ] T018 [P] B/api/controller/v1/auth/ApiOAuthController.java에 `GET /api/v1/auth/oauth/providers` → `{ providers: [...] }`(설정된 제공자만, US3-AC3·R9), B/api/controller/v1/terms/ApiTermsController.java에 `GET /api/v1/terms` → 현재 버전 두 개 (contracts, R11). 이후 스토리의 OAuth 엔드포인트도 ApiOAuthController에 더한다
- [ ] T019 [P] frontend/src/api/index.ts에 `api.v1.auth.oauthProviders()`, `api.v1.terms.current()`를 더한다 (타입은 T052의 `npm run gen:api` 뒤 정리)

**Checkpoint**: 테이블·경계·쿠키·확인·공개 경로가 준비됐다. 단추 노출 여부와 약관 버전을 서버가 알려 준다

---

## Phase 3: User Story 1 - 처음 온 사람이 소셜 계정으로 가입한다 (Priority: P1) 🎯 MVP

**Goal**: 연결된 회원이 없는 소셜 계정(구글, 디스코드)이 가입 확인을 통과하고 닉네임과 필수 약관 동의를 넣어 회원(USER, 비밀번호 없음)이 되고 로그인한다. 그 전에는 회원을 만들지 않는다. 이메일 미인증·중복, 재가입 제한이면 막는다. 소셜 회원에게 관리 역할을 줄 수 없다

**Independent Test**: quickstart 2~11·14 — 처음 쓰는 구글·디스코드 계정으로 닉네임·약관 화면을 거쳐 `/`에 들어가고, 막힐 경우는 회원이 없고, 관리자가 `ADMIN`을 줄 수 없다

- [ ] T020 [US1] ApiOAuthController에 `GET /api/v1/auth/oauth/{provider}/authorize?mode=login&next=`: 설정 없으면 302 `/sign-in?oauthError=disabled`, 있으면 state·nonce(구글)·PKCE를 만들어 `oauth_state`(LOGIN) 쿠키를 심고 302로 제공자 인가 화면 (contracts "authorize")
- [ ] T021 [US1] B/api/application/v1/auth/SocialLoginApplication.java: 콜백 처리 `handleCallback(provider, code, state, error, request, response)` — `oauth_state` 확인(제공자 일치 포함)·삭제, `exchange`, LOGIN이고 연결 없으면 `checkSignUp`(T017) → 막히면 `/sign-in?oauthError=emailUnverified|rejoinBlocked|emailInUse`, 통과하면 회원을 만들지 않고 `oauth_signup` 쿠키를 심어 `/sign-up/nickname?next=…`로 보낼 결과를 돌려준다 (US1-AC1·AC3·AC5·AC9~AC12, US5-AC4, research R5). `@Transactional`을 두지 않는다(`AuthTokenApplication` 주석 규칙)
- [ ] T022 [US1] ApiOAuthController에 `GET /api/v1/auth/oauth/{provider}/callback`을 더하고 T021 결과대로 302를 보낸다 (contracts 콜백 표)
- [ ] T023 [US1] SocialAccountService에 `signUp(identity, nickname, agreements)`: `checkSignUp`을 다시 한 뒤 한 트랜잭션으로 `users`(login_id `s_` + 무작위 영숫자 12자, 겹치면 다시 — (잠정) FR-004 (c); name = 닉네임; password null; role `USER`), `user_social_accounts`, `user_terms_agreements` 2행(agreed_at = 트랜잭션 시각)을 만든다. `(provider, subject)` 유일 위반은 `SOCIAL_ACCOUNT_IN_USE`로 바꾼다 (동시 가입 Edge Case). 사용자 생성은 기존 `UserWriter`/키트 생성 경로를 쓴다
- [ ] T024 [US1] 가입 요청 검증: B/api/controller/v1/auth/request/OAuthSignUpRequest.java에 닉네임 (잠정, `009-mypage` FR-003과 같은 규칙) "앞뒤 공백 제거 뒤 2~12자, 한글·영문·숫자·`_`만"과 `agreements` 목록. 예약어(admin, 운영자, moa 등) 금지와 다른 회원과의 중복(대소문자 무시)은 서비스에서 확인한다(중복 오류 코드는 009와 같은 코드로). 필수 약관 두 종류가 모두 있고 버전이 `TermsProperties`의 현재 버전과 같은지는 서비스에서 확인해 아니면 `TERMS_NOT_AGREED` (FR-005, FR-013, US1-AC7, research R11). 닉네임 규칙은 spec FR-005 [NEEDS CLARIFICATION] 팀 확인 뒤 바뀌면 고친다
- [ ] T025 [US1] ApiOAuthController에 `POST /api/v1/auth/oauth/sign-up`: `oauth_signup` 쿠키 확인(없거나 만료 A009) → T023 → `AuthTokenApplication.issue(user.toPrincipal())` → `RefreshTokenTransport.write` → `AuthRefreshResponse` 응답, 쿠키 삭제 (contracts "sign-up", FR-012)
- [ ] T026 [US1] [공용] B/api/service/user/UserAdminService.java `changeRole`: 대상이 소셜 회원(`SocialAccountService.isSocialMember`)이고 새 역할이 `ADMIN`이면 `SOCIAL_MEMBER_ROLE_NOT_ALLOWED` (FR-007, US1-AC6). 키트 user-admin 헬퍼 파일이다. 구현 전에 바꿀 내용을 보고하고 승인받는다
- [ ] T027 [US1] [공용] B/api/service/role/RoleUserService.java `requireNotAdmin`·`requireTargets`(업무 역할 부여 경로 `replaceRolesOfUser`, `addMembers`): 대상에 소셜 회원이 있으면 `SOCIAL_MEMBER_ROLE_NOT_ALLOWED` (FR-007, US1-AC6). 키트 rbac 헬퍼 파일이다. 구현 전에 바꿀 내용을 보고하고 승인받는다
- [ ] T028 [US1] [공용] frontend/src/app/(public)/sign-in/page.tsx: `oauthProviders()`에 있는 제공자마다 "구글로 계속하기"·"디스코드로 계속하기" 단추를 두고 누르면 `/api/v1/auth/oauth/{provider}/authorize?next=<currentNextPath>`로 `window.location` 이동. 기존 아이디·비밀번호 폼은 그대로 (FR-001, US3-AC3). 키트 web-next 로그인 화면 파일이다. 구현 전에 바꿀 내용을 보고하고 승인받는다
- [ ] T029 [P] [US1] frontend/src/content/terms/(새): 약관 종류·버전마다 요약·전문을 내보내는 파일과 `type`·`version`으로 고르는 함수. (잠정) 문안은 팀이 준비하기 전까지 자리표시, 버전 `draft` (spec 가정, research R11)
- [ ] T030 [P] [US1] frontend/src/app/(public)/sign-up/nickname/page.tsx(새): react-hook-form + zod(닉네임 잠정 규칙의 길이 2~12자·한글·영문·숫자·`_`만, 약관 두 체크 필수. 예약어·중복은 서버 응답으로 칸 오류)로 닉네임과 약관 동의를 받는다. `api.v1.terms.current()`의 버전으로 T029 문안을 보이고 요약만 보이다 펼치면 전문. 둘 다 체크해야 제출 단추가 켜진다. `api.v1.auth.oauthSignUp`(frontend/src/api/index.ts에 더함)에 닉네임과 동의한 버전을 보내고 → 성공 시 기존 로그인 성공 처리(`queryClient.clear()`, `useAuthStore.signIn`, `next` 또는 `landingPath`) (FR-005, FR-009, FR-013). 실패 문구(A017 → 약관 다시 확인, A009 → 로그인 화면 안내, A012 → 이미 가입됨, A014~A016 → 로그인 화면과 같은 문구)
- [ ] T031 [P] [US1] frontend/src/app/(public)/auth/oauth/done/page.tsx(새): `POST /api/v1/auth/token/refresh`로 액세스 토큰을 받아 기존 로그인 성공 처리, 실패하면 `/sign-in?oauthError=failed` (research R4). 화면은 진행 표시만
- [ ] T032 [US1] [공용] frontend/messages/ko.json: `signIn.google`·`signIn.discord`(단추), 새 `signUp` 묶음(닉네임 화면 제목·설명·칸·약관 체크 라벨·펼치기·단추·오류)을 더한다. 약관 요약·전문은 넣지 않는다(T029). 문구 검사(`npm run check`) 규칙을 따른다. 구현 전에 바꿀 내용을 보고하고 승인받는다
- [ ] T033 [US1] 사용자 관리 드로어·역할 관리에서 A011 응답이 기존 오류 표시로 안내되는지 확인하고, 안 되면 문구만 더한다 (frontend `user-drawer.tsx`·역할 관리 화면, [공용] 해당 시 T032와 함께 보고)

**Checkpoint**: 새 소셜 계정으로 가입·로그인 된다 (MVP)

---

## Phase 4: User Story 2 - 소셜 회원이 다시 로그인한다 (Priority: P1)

**Goal**: 연결된 소셜 계정은 같은 회원으로 로그인하고, 잠긴 회원은 막히며, 비밀번호 없는 계정에는 "비밀번호 변경"이 보이지 않는다

**Independent Test**: quickstart 12·13 — 다시 로그인해도 회원 수가 그대로, 계정 메뉴에 비밀번호 변경이 없다

- [ ] T034 [US2] SocialLoginApplication 콜백 LOGIN 분기: 연결된 회원이 있으면 잠김(`isLocked`)은 `/sign-in?oauthError=locked`(A007), 아니면 `AuthTokenApplication.issue` → `RefreshTokenTransport.write` → `/auth/oauth/done?next=…` (FR-003, US2-AC1·AC2, US5-AC2). 이미 연결된 회원의 로그인에는 가입 확인(T017)을 하지 않는다
- [ ] T035 [US2] [공용] `GET /api/v1/users/me` 응답(B/.../UserGetResponse.java)에 `hasPassword`(boolean)를 더한다 (contracts/users-me-and-screens.md). 구현 전에 바꿀 내용을 보고하고 승인받는다
- [ ] T036 [P] [US2] frontend/src/types/user.ts `Me`에 `hasPassword`를 더한다
- [ ] T037 [US2] [공용] frontend/src/components/shell/account-menu.tsx: `useMe()`의 `hasPassword`가 거짓이면 "비밀번호 변경" 항목을 숨긴다 (US2-AC3). 두 셸이 함께 쓰는 키트 셸 파일이다. 구현 전에 바꿀 내용을 보고하고 승인받는다

**Checkpoint**: 가입한 소셜 회원이 다시 들어온다

---

## Phase 5: User Story 3 - 소셜 로그인이 안 될 때 안내를 받는다 (Priority: P2)

**Goal**: 취소·확인 실패·설정 없음에서 로그인 화면에 머물고 알맞은 안내를 받는다. 가입 확인에서 막힌 경우도 같은 자리에서 안내한다

**Independent Test**: quickstart 1·21 — 설정이 없는 제공자의 단추만 없고, 취소하면 안내 없이 로그인 화면이다

- [ ] T038 [US3] SocialLoginApplication: `error=access_denied`는 아무것도 하지 않고 LOGIN `/sign-in`·LINK `/settings`로, state 불일치·만료·제공자 불일치·교환·확인 실패는 LOGIN `/sign-in?oauthError=failed`·LINK `/settings?socialLink=failed&provider=…`로 보낸다. 쿠키가 없을 때 모드는 LOGIN으로 본다 (US3-AC1·AC2, contracts 콜백 표)
- [ ] T039 [US3] 설정 없음: 제공자마다 `OAuthProperties`의 `enabled()`가 거짓이면 authorize·callback·link가 A008 또는 `/sign-in?oauthError=disabled` (US3-AC3). 한 제공자가 꺼져도 다른 제공자는 그대로
- [ ] T040 [US3] [공용] frontend/src/app/(public)/sign-in/page.tsx: `oauthError`(`failed`·`locked`·`disabled`·`emailUnverified`·`emailInUse`·`rejoinBlocked`)면 폼 위에 문구를 보이고, 쿼리가 없으면 아무것도 보이지 않는다. T028과 같은 키트 파일이고 문구는 T032의 ko.json 변경에 함께 넣는다. 구현 전에 바꿀 내용을 보고하고 승인받는다

**Checkpoint**: 실패하거나 막혀도 원인을 알 수 있다

---

## Phase 6: User Story 5 - 내 계정에 소셜 계정을 연결하고 해제한다 (Priority: P2)

**Goal**: 로그인한 회원이 계정 설정에서 구글·디스코드를 제공자마다 연결·해제한다. 연결에도 이메일 미인증·중복(본인 예외)과 재가입 제한을 확인한다. 관리자·운영자는 연결이 없고, 비밀번호 없는 회원은 마지막 연결을 해제할 수 없다

**Independent Test**: quickstart 9·15~20

- [ ] T041 [US5] SocialAccountService에 `prepareLink(userId, provider)`(관리자·운영자면 `SOCIAL_LINK_NOT_ALLOWED`, 그 제공자가 이미 연결이면 `INVALID_STATE`), `link(userId, identity)`(관리자·운영자면 `SOCIAL_LINK_NOT_ALLOWED` → `checkLink`(T017, 본인 예외) → 다른 회원에 있으면 `SOCIAL_ACCOUNT_IN_USE`), `unlink(userId, provider)`(비밀번호가 없고 남은 연결이 이것 하나면 `SOCIAL_UNLINK_LAST_METHOD`, 연결 없으면 그대로)를 더한다 (FR-006·FR-007·FR-014·FR-015, research R6·R7·R12·R13)
- [ ] T042 [US5] B/api/controller/v1/user/ 아래 내 외부 계정 컨트롤러: `POST /api/v1/users/me/social-accounts/{provider}/link` → `prepareLink` → `oauth_state`(LINK, provider, userId) 쿠키 → `{ authorizeUrl }`, `DELETE /api/v1/users/me/social-accounts/{provider}` → `unlink` → 204 (contracts)
- [ ] T043 [US5] ApiOAuthController authorize `mode=link`: 같은 제공자의 LINK `oauth_state`가 없거나 만료면 302 `/settings?socialLink=failed&provider=…`, 있으면 제공자 인가 화면으로 (contracts "authorize")
- [ ] T044 [US5] SocialLoginApplication 콜백 LINK 분기: `link` 결과대로 `/settings?socialLink=linked|inUse|notAllowed|failed|emailUnverified|emailInUse|rejoinBlocked&provider=…` (US5-AC1·AC3·AC7~AC10)
- [ ] T045 [US5] [공용] `UserGetResponse`에 `socialAccounts`(`{ provider, email, linkedAt }[]`)와 `socialLinkAllowed`(관리자·운영자면 거짓)를 더한다. T035와 같은 파일이므로 보고를 함께 한다. 구현 전에 바꿀 내용을 보고하고 승인받는다
- [ ] T046 [P] [US5] frontend/src/types/user.ts `Me`에 `socialAccounts`, `socialLinkAllowed`를 더하고 frontend/src/api/index.ts에 `api.v1.user.linkSocial(provider)`, `api.v1.user.unlinkSocial(provider)`를 더한다
- [ ] T047 [US5] [공용] frontend/src/app/(account)/settings/page.tsx: `socialLinkAllowed`일 때만 제공자마다 한 줄(구글, 디스코드) — 연결 안 됨이면 연결 단추(`linkSocial(provider)` 뒤 `authorizeUrl`로 이동), 연결됨이면 이메일과 해제 단추(비밀번호가 없고 연결이 하나뿐이면 해제 대신 "유일한 로그인 수단" 안내), `socialLink`·`provider` 쿼리로 결과 문구. 기존 푸시 설정 화면 파일이다. 구현 전에 바꿀 내용을 보고하고 승인받는다
- [ ] T048 [US5] [공용] frontend/messages/ko.json `settings` 묶음에 소셜 연결 문구(제공자 이름, 줄 제목, 연결·해제 단추, 결과 문구 linked·inUse·notAllowed·failed·emailUnverified·emailInUse·rejoinBlocked, 해제 불가 안내)를 더한다. 구현 전에 바꿀 내용을 보고하고 승인받는다

**Checkpoint**: 기존 계정과 소셜 계정을 잇고 끊는다

---

## Phase 7: User Story 4 - 관리자가 소셜 로그인 기록을 본다 (Priority: P3)

**Goal**: 소셜 로그인 성공·실패가 접속 이력에 남는다

**Independent Test**: quickstart 22

- [ ] T049 [US4] SocialLoginApplication·가입(T025): 성공(로그인·가입)과 실패(잠김, state·교환·확인 실패, 가입 확인에서 막힘 A014~A016)에 기존 `LoginAttemptedEvent.succeeded/failed`를 발행한다. `loginId` 칸 값은 (잠정) "회원이 있으면 그 회원의 loginId, 없으면 `google:`·`discord:` + subject 앞 8자" — spec US4-AC1 [NEEDS CLARIFICATION] 확정 뒤 고친다 (research R8). `LoginHistoryRecorder`(키트)는 고치지 않는다
- [ ] T050 [US4] 취소(`access_denied`)는 이력을 남기지 않는다(로그인 시도 전). 이 판단을 SocialLoginApplication 주석에 적는다

**Checkpoint**: 접속 이력에 소셜 로그인이 보인다

---

## Phase 8: Polish & Cross-Cutting

- [ ] T051 backend/에서 `.\gradlew.bat spotlessApply`, `.\gradlew.bat build` (DB 테스트 제외)
- [ ] T052 frontend/에서 `npm run gen:api`(local 백엔드 실행 중)로 `src/types/schema.d.ts`를 다시 만들고, T019·T030·T046의 손으로 쓴 타입을 생성 타입으로 바꾼 뒤 `npm run format`, `npm run check`
- [ ] T053 specs/010-social-login/quickstart.md "수동 확인" 1~22를 사람이 직접 수행한다. 구글 OAuth 클라이언트·디스코드 애플리케이션 만들기와 `.env.local` 값 넣기도 사람이 한다 (원칙 VI)
- [ ] T054 spec "나중에 고칠 곳" 목록(002 FR-004·FR-007·가정, 003 업무 역할, 008 가정·FR-027 — 팀 공유 필요(담당: 008-party 담당), 005, README 경계 4줄, 회원 탈퇴 스펙, 회원 제재 기능)을 PR 설명에 적어 각 담당에게 알린다 (사람이 한다. 이 기능에서 그 스펙들을 고치지 않는다)
- [ ] T055 키트로 옮길 것을 PR 설명에 적는다: user-admin·rbac 서비스의 소셜 회원 막기(T026·T027), 로그인 화면 제공자 단추(T028·T040), 셸 계정 메뉴 숨김(T037), 제공자 클라이언트(헬퍼가 없을 때, T010~T014) (원칙 III)
- [ ] T056 배포 전에 약관 문안과 버전(`draft` → 실제 버전)을 팀이 준 값으로 바꾼다 (T004·T029, (잠정))

---

## Phase 9: (예약) 헬퍼 확정 반영

- [ ] T057 T002에서 키트 oauth 헬퍼가 확인되면: 헬퍼를 얹고 T010~T014 구현을 헬퍼로 바꾼 뒤 spec 출처의 [NEEDS CLARIFICATION]을 풀고 research R2를 결정으로 고친다. 헬퍼가 없기로 정해지면 출처를 "moa"로 확정한다

---

## Phase 10: Tests (맨 끝, CLAUDE.md)

**Purpose**: AC마다 테스트 하나 (원칙 II), 이름에 AC 번호. 제공자는 부르지 않는다

- [ ] T058 backend/src/test/java/com/moa/support/FakeOAuthProviderClients.java: 테스트에서 구글·디스코드 `OAuthProviderClient`를 바꾸는 가짜(정해 둔 code → 정해 둔 `OAuthIdentity`(이메일 인증·미인증, 이메일 없음 포함), 실패 code → `OAUTH_FAILED`)와 `@TestConfiguration`, 재가입 제한 행을 `SocialRejoinHasher`로 넣는 도우미. 헬퍼로 바뀌면 그 경계에 맞춘다
- [ ] T059 backend/src/test/java/com/moa/api/SocialLoginAcceptanceTest.java(`AcceptanceTest` 상속): authorize → callback(쿠키 이어 붙이기) → sign-up 흐름으로 US1-AC1~AC12(구글·디스코드 각각 가입 하나씩 포함), US2-AC1~AC2, US3-AC1~AC3(한 제공자 설정 끈 속성 덮어쓰기), US4-AC1(접속 이력 성공·실패 행), US5-AC1~AC10(link·unlink·me 칸)을 `@DisplayName("US1-AC1 …")`으로 하나씩 둔다. 동시 가입 Edge Case(유일 제약 → A012)도 하나 둔다. US2-AC3·US5-AC7의 화면 부분은 `/users/me`의 `hasPassword`·`socialLinkAllowed` 값으로 확인한다
- [ ] T060 [P] 기존 수용 테스트가 깨지지 않는지 본다: `AdminUsersAcceptanceTest`(역할 변경), 역할 관리 수용 테스트, `UserAccountAcceptanceTest`(`/users/me` 칸 추가). 깨지면 원칙 V대로 스펙부터 확인한다
- [ ] T061 [P] frontend 단위 테스트: account-menu의 비밀번호 변경 숨김(`hasPassword` 거짓), sign-in의 제공자별 단추 표시(`providers` 빈/일부/전부)와 `oauthError` 문구 6개, 닉네임 화면의 약관 체크 전 제출 막힘·펼치기, 닉네임 검증(잠정: 1자·13자·허용되지 않는 글자 막힘, 2~12자 한글·영문·숫자·`_` 통과)
- [ ] T062 [P] frontend/e2e/social-login.spec.ts: 핵심 흐름 US1(단추 → 닉네임·약관 → 첫 화면)만. 백엔드 콜백 이동은 Playwright 경로 가로채기로 흉내 낸다 (원칙 II "e2e는 핵심 흐름만")
- [ ] T063 로컬에서는 `.\gradlew.bat build`, `npm run check`만 돌린다. `dbTest`와 e2e는 PR CI로 확인하고, 사용자가 요청할 때만 로컬에서 `.\gradlew.bat dbTest --tests "*SocialLoginAcceptanceTest"`를 돌린다

---

## Dependencies & Execution Order

- Phase 1 → Phase 2 → 스토리. T001은 모든 작업보다 먼저, T002 결과가 T010~T014를 정한다
- Phase 2 안: T015 → T017 (해시 → 확인), T016 → T017
- US1(Phase 3)이 MVP. US2(Phase 4)는 T021·T022 콜백 위에 분기를 더하므로 US1 뒤
- US3(Phase 5)는 콜백·로그인 화면(T022·T028) 뒤
- US5(Phase 6)는 콜백(T022), 확인(T017), `UserGetResponse`(T035) 뒤
- US4(Phase 7)는 콜백·가입(T021·T025) 뒤
- Phase 8 → Phase 10(테스트) 순서. Phase 9는 헬퍼 확인이 나오는 대로
- [공용] 작업 15개(T003·T004·T005·T008·T026·T027·T028·T032·T035·T037·T040·T045·T047·T048, T033 해당 시)는 착수 전 보고·승인이 끝나야 시작한다. 보고는 묶어서 한 번에 해도 된다. 공용 파일 11개는 plan.md "공용 파일" 표

## Parallel Example

```text
Phase 2 안: T006, T007, T008, T009, T010, T011, T012, T018, T019 (서로 다른 파일)
US1 안: T029, T030, T031 (프론트 약관·화면) — 백엔드 T020~T027과 동시에
US5 안: T046 (프론트 타입·API)와 T041~T044 (백엔드)
Phase 10 안: T060, T061, T062
```

## Implementation Strategy

1. **MVP**: Phase 1 → 2 → 3(US1). 새 소셜 계정 가입·로그인만으로 배포 검토 가능 (약관 문안이 준비돼야 실제 배포)
2. US2(다시 로그인, 비밀번호 변경 숨김) → US3(실패 안내) → US5(연결·해제) → US4(접속 이력)
3. Polish → 테스트(맨 끝) → PR. 잠정 작업(헬퍼, 디스코드 PKCE, FR-004, 닉네임 규칙, 접속 이력 표시, 영구 정지, 약관 문안)은 확정되는 대로 해당 작업을 고친다
