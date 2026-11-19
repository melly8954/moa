# Research: 소셜 로그인 (010)

스펙: [spec.md](./spec.md). 각 항목은 결정 / 이유 / 검토한 대안 순서다.
**"팀 확인 후 바뀔 수 있음"** 표시는 키트 oauth 헬퍼나 회원 정보 모델(FR-004) 같은 잠정 결정에 기댄 부분이다.
**마이그레이션 번호**: `V<번호>__social_login.sql`. 번호는 정해 두지 않고 구현 때 dev 마지막 다음 번호로 한다. `V2_7`은 004(`V2_7__code_groups_from_code.sql`)가 썼고, 그 뒤 번호는 004 Phase 6(`code_groups` 재생성)·008·009도 구현 때 차례로 받는다. 010은 공통코드를 쓰지 않아 004·008과는 병합 순서가 자유롭다. 009는 010이 만든 `user_profiles`에 칸을 더하므로 010 다음이다 (R14). 다만 Flyway `outOfOrder`가 꺼져 있어 dev에 들어간 번호보다 작은 번호는 나중에 넣을 수 없으므로, 구현 PR 병합 직전에 dev 마지막 번호를 다시 보고 그보다 크게 맞춘다.

## R1. 서버 경유 방식 (OAuth 2.0 인가 코드 + PKCE) — 2026-11-17 팀 결정

- **결정**: 프론트는 백엔드 주소로 이동만 한다. 백엔드가 제공자(구글, 디스코드) 인가 화면으로 보내고, 제공자 콜백을 받아 인가 코드를 토큰으로 바꾼다. 제공자 토큰과 client secret은 백엔드에만 둔다. 콜백에서 회원을 찾거나 가입 대기로 보내고, 아이디·비밀번호 로그인과 같은 moa JWT·리프레시 쿠키를 발급한 뒤 프론트로 돌려보낸다. 계정 설정의 연결(FR-006)도 같은 흐름을 쓴다. 경로는 제공자를 경로 변수로 받는다(`/api/v1/auth/oauth/{provider}/…`). 콜백 경로는 공용 `SecurityConfig`를 고치지 않고 `PublicPaths` 빈으로 연다
- **구현 때 바꿈 (2026-11-19)**: `PublicPaths` 빈 대신 OAuth·약관 컨트롤러 클래스에 `@PermitAll`을 붙여 연다. 이유: 콜백·가입도 컨트롤러 메서드이고 키트 `PublicPaths` 주석이 "컨트롤러 API는 `@PermitAll`"로 정한다. `@PermitAll`이면 감사 행위자도 시스템 계정이 된다(가입 때 `created_by`). `SecurityConfig`는 여전히 고치지 않는다
- **이유**
  - 제공자 토큰·client secret이 브라우저에 나오지 않는다 (원칙 VI)
  - 로그인 결과가 기존 로그인과 같은 토큰 체계(`AuthTokenService.issue`, `RefreshTokenTransport`)로 끝나 로그아웃·갱신·접속 이력을 따로 만들지 않는다 (FR-012). `AuthTokenApplication` 주석도 "소셜 로그인은 인증 뒤 `issue`를 부른다"고 둔다
  - 로그인과 연결, 두 제공자가 같은 콜백 흐름을 써서 구현·테스트가 하나로 모인다. 카카오를 더할 때도 클라이언트 하나만 더한다
  - `PublicPaths`는 이미 이 용도(OAuth 콜백)를 주석에 적어 둔 확장점이다
- **대안**
  - 프론트 SDK(Google Identity Services)로 ID 토큰을 받아 백엔드로 보내기: 예전 `feat/008-google-login`이 이 방식(tokeninfo 검증)이었다. 디스코드에는 같은 방식이 없고, 팀 결정에 따라 쓰지 않는다
  - Spring Security `oauth2Login`: 기본이 세션 저장이라 상태 없는 JWT 구조와 맞추려면 쿠키 저장소·성공 처리기를 다시 짜야 하고 `HttpSecurityCustomizer`로 보안 필터 체인을 바꾸게 된다

## R2. 키트 oauth 헬퍼와 제공자 경계 — 팀 확인 후 바뀔 수 있음

- **확인한 사실**: 적용 기록 `backend/.psw-dev-kit`(`springboot/v0.7.0`)과 `frontend/.psw-dev-kit`(`web-next/v0.7.0`)에 oauth 헬퍼가 없다. 키트 저장소에 헬퍼가 있는지는 확인하지 못했다
- **결정 (잠정, 스펙 출처)**: 키트 oauth 헬퍼를 기본 방향으로 두고, "제공자와 주고받는 부분"과 moa가 정하는 "회원 처리"를 나눠 설계한다
  - 헬퍼에 기댈 부분 (팀 확인 후 바뀔 수 있음): 인가 URL 만들기, state·PKCE·nonce 보관, 콜백에서 코드 교환, 신원 확인, 제공자 설정(`app.oauth.*`), 콜백 경로 열기
  - 헬퍼와 상관없이 moa가 만드는 부분: 외부 계정 연결·약관 동의·재가입 제한(data-model), 닉네임 가입 대기, 가입·연결 확인(R12·R13), 회원 찾기·만들기·연결·해제, 관리자·운영자 제한(FR-007), 토큰 발급 연결, 프론트 화면
  - 경계는 백엔드 안의 "제공자 클라이언트"(인가 URL → 코드 교환 → 확인된 신원 `provider, subject, email, emailVerified, name`)로 두고 제공자마다 하나 만든다. 설정된 클라이언트만 모은 목록(레지스트리)이 경로 변수 `{provider}`를 찾는다
- **구글**: OIDC. scope `openid email profile`, PKCE S256, nonce. 토큰 응답의 ID 토큰은 백엔드가 구글과 TLS로 직접 받은 것이므로 서명 검증 대신 `iss`·`aud`·`exp`·`nonce`만 확인한다 (OIDC Core 3.1.3.7). 이메일 인증은 `email_verified` 클레임
- **디스코드**: OIDC가 아니다(ID 토큰 없음, nonce 없음). scope `identify email`, 인가 `https://discord.com/oauth2/authorize`, 토큰 `https://discord.com/api/oauth2/token`(client secret + PKCE verifier), 받은 액세스 토큰으로 `GET https://discord.com/api/users/@me`를 불러 `id`(subject), `email`, `verified`(이메일 인증), `global_name`(없으면 `username`)을 얻는다. 액세스 토큰은 이 호출에만 쓰고 저장하지 않는다
  - **확인 필요**: 디스코드의 PKCE 지원 여부를 구현 전에 확인한다 (tasks T002). 지원하지 않으면 PKCE 없이 state만 쓰는 것을 팀에 다시 묻는다 (팀 결정 "둘 다 PKCE"와 다르므로 임의로 바꾸지 않는다)
- **헬퍼가 없을 때 (moa 직접)**: 같은 경계를 moa가 만든다. Spring `RestClient`, Jackson으로 새 의존성 없음. 나중에 헬퍼가 생기면 클라이언트를 바꾸고 키트로 옮긴다 (원칙 III)
- **구현 때 바꿈 (2026-11-19)**: `SocialProvider`·`TermsType` enum은 data-model의 `api/entity/user`가 아니라 `common/security/oauth`·`common/terms`에 둔다. 이유: 아키텍처 규칙(`ArchitectureTest`)상 컨트롤러가 `api.entity`에 의존할 수 없고, 제공자 경계(`common`)의 `OAuthIdentity`·쿠키도 제공자 값을 써야 한다. 엔터티는 `common`의 enum을 쓴다

## R3. state·PKCE·nonce 보관 — 서명한 짧은 쿠키 (팀 확인 후 바뀔 수 있음)

- **결정**: 인가 시작 때 제공자, state, nonce(구글만), PKCE verifier, 모드(`LOGIN`/`LINK`), 돌아갈 주소(`next`), 연결이면 회원 id를 담은 값을 서명해 HttpOnly 쿠키(`oauth_state`, Path `/api/v1/auth/oauth`, SameSite `Lax`, 10분)로 둔다. 콜백에서 쿼리의 `state`와 쿠키 값을, 경로의 제공자와 쿠키의 제공자를 비교하고 쿠키를 지운다
- **이유**: 서버가 상태 없는 JWT 구조라 세션을 새로 두지 않는다. 제공자에서 돌아오는 콜백은 다른 사이트에서 온 최상위 GET 이동이라 `Strict`이면 쿠키가 안 붙고 `Lax`는 붙는다. 서명 키는 기존 JWT 서명 키와 용도 구분(claim `typ=oauth_state`)으로 쓴다
- **대안**: DB·캐시에 state 저장 — 테이블과 정리 작업이 더 든다. 헬퍼가 다른 저장 방식을 쓰면 헬퍼를 따른다

## R4. 콜백 뒤 프론트로 돌아가기와 토큰 전달

- **결정**: 제공자 `redirect_uri`는 프론트 주소의 `/api/v1/auth/oauth/{provider}/callback`이다. 프론트 `src/proxy.ts`가 `/api/*`를 백엔드로 넘기므로 브라우저 입장에서는 프론트와 같은 출처다. 콜백은 리프레시 쿠키(`refresh_token`, Path `/api/v1/auth`)만 심고 프론트 화면 `/auth/oauth/done?next=…`으로 보낸다. 그 화면이 기존 `POST /api/v1/auth/token/refresh`로 액세스 토큰을 받아 `useAuthStore.signIn`을 부르고, 기존 첫 화면 규칙(`landingPath`, `next`)대로 이동한다 (FR-009, `001-admin-menus` FR-006)
- **이유**: 액세스 토큰을 주소에 싣지 않는다. 토큰 저장·첫 화면 결정은 기존 로그인 코드를 그대로 쓴다. 제공자마다 화면을 따로 두지 않는다
- **대안**: 콜백이 액세스 토큰을 주소 조각(`#token=`)으로 넘기기 — 기록·로그에 남을 위험이 있어 쓰지 않는다
- **실패·막힘**: 콜백은 `/sign-in?oauthError=<코드>`(로그인) 또는 `/settings?socialLink=<결과>&provider=<제공자>`(연결)로 보내고, 화면이 문구를 보인다 (US3, US1-AC9·AC10·AC12, US5-AC3·AC8~AC10). 취소(`error=access_denied`)는 안내 없이 로그인 화면(또는 설정 화면)으로 돌아간다 (US3-AC1)

## R5. 닉네임·약관 전에는 회원을 만들지 않기 (FR-005, FR-013)

- **결정**: 연결된 회원이 없는 소셜 계정이 콜백에 오면 가입 확인(R12·R13)을 먼저 하고, 통과하면 회원을 만들지 않은 채 확인된 신원(제공자, subject, 정규화한 이메일, 제공자 이름)과 `next`를 서명한 가입 대기 쿠키(`oauth_signup`, Path `/api/v1/auth/oauth`, SameSite `Strict`, 10분)에 담아 `/sign-up/nickname`으로 보낸다. 닉네임 화면이 `POST /api/v1/auth/oauth/sign-up`에 닉네임과 동의한 약관 버전을 보내면, 가입 확인을 한 번 더 한 뒤 회원·외부 계정 연결·약관 동의 기록을 한 트랜잭션으로 만들고 로그인 토큰을 발급한다
- **이유**: 떠나면 쿠키만 사라져 회원도 동의 기록도 생기지 않는다(US1-AC5). 막힐 사람은 닉네임 화면까지 가지 않는다. 쿠키가 살아 있는 10분 사이에 상황이 바뀔 수 있어 제출 때 다시 확인한다
- **동시 가입**: 외부 계정 연결의 `(provider, subject)` 유일 제약으로 두 번째 요청은 실패시키고 "이미 가입된 소셜 계정" 안내 후 로그인 화면으로 보낸다 (Edge Cases)
- **009 경계**: 가입 트랜잭션에서 `user_profiles` 행(`nickname_key`, `nickname_tag`)을 만든다. 010이 먼저 구현한다 (R14, `009-mypage` research R14, 2026-11-19)
- **대안**: 회원을 먼저 만들고 닉네임 없음 상태를 두기 — Session 2026-11-16에서 쓰지 않기로 했다

## R6. 연결(FR-006) 시작 — 로그인한 회원 확인

- **결정**: 계정 설정 화면이 액세스 토큰을 붙여 `POST /api/v1/users/me/social-accounts/{provider}/link`를 부르면, 백엔드가 회원을 확인(관리자·운영자 금지, 그 제공자가 이미 연결됨 금지)하고 `LINK` 모드 `oauth_state` 쿠키를 심은 뒤 이동할 주소(`/api/v1/auth/oauth/{provider}/authorize?mode=link`)를 돌려준다. 프론트는 그 주소로 이동만 한다. 콜백은 연결 확인(R12·R13, 본인 이메일 예외)을 한 뒤 쿠키 안의 회원 id에 연결한다
- **이유**: 최상위 이동에는 메모리에 있는 액세스 토큰이 붙지 않는다. 연결 의도를 인증된 요청에서 만들어 서명 쿠키에 묶으면 주소에 일회용 값을 싣지 않아도 된다
- **대안**: 리프레시 쿠키로 회원 확인 — 갱신 토큰을 다른 용도로 쓰게 돼 쓰지 않는다. 주소에 일회용 티켓 — 저장소가 더 든다

## R7. 관리자·운영자 제한 (FR-007)

- **결정**: "관리자·운영자 계정"은 시스템 역할 `ADMIN`이거나 업무 역할(rbac)을 하나라도 가진 계정이다
  - 연결 시작(R6)과 콜백에서 이 계정이면 막는다 (A010)
  - 소셜 회원(외부 계정 연결이 하나라도 있는 회원, 제공자 상관없음)에게 `ADMIN`을 주는 `UserAdminService.changeRole`, 업무 역할을 주는 `RoleUserService`(`requireNotAdmin`·`requireTargets`)에서 막는다 (A011)
- **이유**: 막는 지점을 기존 역할 변경 서비스 안에 두면 화면·API 어디서 오든 같은 규칙이다
- **구현 때 바꿈 (2026-11-19)**: 관리자·운영자 판단(`isManagerAccount`)은 US5(연결)를 만들 때 둔다. 이유: US1~US3에서 쓰는 곳이 없고, 업무 역할은 role 도메인이라 아키텍처 규칙상 `service/user`의 `SocialAccountService`가 읽을 수 없다. US5 때 Application이 user·role 서비스를 조합하는 자리로 정한다. 소셜 회원에게 역할 주기 막기(US1-AC6)는 US1의 T026·T027에서 한다
- **영향**: `002-admin-users` FR-007·`003-admin-roles`에 막는 규칙이 더해진다. 스펙 "나중에 고칠 곳"에 적어 두었고 이번에는 그 스펙을 고치지 않는다

## R8. 접속 이력 (FR-008)

- **결정**: 소셜 로그인 성공·실패도 기존 `LoginAttemptedEvent`를 발행해 `LoginHistoryRecorder`가 남기게 한다. 실패에는 잠김, state·교환·확인 실패, 가입 확인에서 막힘(이메일 미인증·재가입 제한·이메일 중복)을 넣는다. 취소는 남기지 않는다. 새 실패 코드는 `ErrorCode`에 더한다
- **미정 (스펙 US4-AC1 [NEEDS CLARIFICATION])**: 이벤트의 `loginId` 칸에 무엇을 넣을지, 로그인 방식을 따로 보일지. 정해질 때까지 "회원이 있으면 그 회원의 loginId, 없으면 `google:`·`discord:` 접두와 subject 앞 8자"를 자리표시로 둔다

## R9. 로그인 화면 단추와 비밀번호 변경 숨김 (US3-AC3, US2-AC3)

- **결정**
  - `GET /api/v1/auth/oauth/providers`(공개)가 설정된 제공자 목록(`["google","discord"]`, 일부, 또는 `[]`)을 돌려주고, 로그인 화면은 목록에 있는 제공자 단추만 보인다
  - `GET /api/v1/users/me`(`UserGetResponse`, 프론트 `Me`)에 `hasPassword`, `socialAccounts`(연결된 제공자·이메일·연결 시각), `socialLinkAllowed`를 더한다. `AccountMenu`는 `hasPassword`가 거짓이면 "비밀번호 변경"을 숨기고, 계정 설정은 `socialLinkAllowed`일 때만 연결 줄을 보인다
- **이유**: 설정 유무와 관리자·운영자 판단을 서버 한 곳에서 한다. 프론트는 보이기만 정한다

## R10. 설정 값

- **결정**
  - `app.oauth.google.client-id`·`client-secret`·`redirect-uri` ← `GOOGLE_CLIENT_ID`·`GOOGLE_CLIENT_SECRET`·`GOOGLE_REDIRECT_URI`
  - `app.oauth.discord.client-id`·`client-secret`·`redirect-uri` ← `DISCORD_CLIENT_ID`·`DISCORD_CLIENT_SECRET`·`DISCORD_REDIRECT_URI`
  - `app.oauth.rejoin-hash-secret` ← `SOCIAL_REJOIN_HASH_SECRET` (R13)
  - `app.terms.service-version`, `app.terms.privacy-version` (R11, 비밀 값 아님)
  - 제공자의 client-id가 비어 있으면 그 제공자를 끈다(R9 목록에서 빠짐, 시작 요청은 A008). 제공자가 하나라도 켜져 있는데 `rejoin-hash-secret`이 비어 있으면 시작할 때 실패한다(재가입 제한을 확인할 수 없는 채로 가입을 받지 않는다)
- **주의**: 값은 `.env.<프로필>`이나 환경 변수로 주고 레포에는 이름만 `backend/.env.example`에 더한다 (원칙 VI). 헬퍼가 오면 헬퍼 설정 이름을 따른다 — 팀 확인 후 바뀔 수 있음

## R11. 약관 동의 (FR-013)

- **결정**
  - 현재 약관 버전은 서버 설정(`app.terms.service-version`, `app.terms.privacy-version`)이 정하고, 공개 `GET /api/v1/terms`가 돌려준다
  - 약관 문안(요약·전문)은 프론트의 정적 파일(`frontend/src/content/terms/`, 버전마다 한 파일)로 둔다. 닉네임 화면은 서버가 준 버전의 문안을 보인다. 요약만 보이고 펼치면 전문
  - 가입 요청은 동의한 약관의 종류·버전을 함께 보낸다. 서버는 필수 두 종류(`SERVICE`, `PRIVACY`)가 모두 있고 버전이 현재 버전과 같을 때만 가입시킨다. 아니면 A017. 동의 시각은 서버 시각(가입 트랜잭션 시각)이다
- **이유**: 저장하는 버전을 서버가 정해야 기록을 믿을 수 있다. 문안은 길고 법무 문서라 문구 파일(`ko.json`)에 넣지 않는다. 화면을 연 사이에 버전이 바뀌면 다시 동의받는다
- **잠정**: 문안과 버전 값은 팀이 준비한다(spec 가정). 준비되기 전에는 자리표시 문안과 버전 `draft`로 구현하고 배포 전에 바꾼다
- **대안**: 약관을 DB에 두고 관리 화면으로 고치기 — 관리 화면이 이 스펙 범위 밖이다. 버전을 프론트만 알기 — 서버가 받은 값을 믿을 근거가 없다

## R12. 가입·연결 확인: 이메일 미인증과 이메일 중복 (FR-014)

- **결정**
  - 확인 순서: 이메일 없음·미인증(A014) → 재가입 제한(R13, A016) → 이메일 중복(A015). 가입은 콜백과 가입 제출 때, 연결은 콜백 때 확인한다
  - 이메일 인증 여부는 제공자가 준 값을 쓴다: 구글 `email_verified`, 디스코드 `verified`. 이메일이 없거나 값이 거짓이면 막는다
  - 이메일은 앞뒤 공백을 지우고 소문자로 바꿔 저장·비교한다. 그 밖의 정규화(구글의 점 무시 등)는 하지 않는다
  - 중복 비교 대상은 `user_social_accounts.email`이다(spec 가정: 키트 `users`에는 이메일 칸이 없다). 연결이면 같은 회원의 행은 빼고 비교한다(본인 예외). 삭제(soft delete)된 회원의 행도 비교 대상에 들어간다 — 탈퇴 처리가 정해지기 전까지 기존 행을 그대로 둔다(R13)
- **이유**: 미인증 이메일이 저장되면 그 이메일의 진짜 주인이 나중에 막힌다(Session 2026-11-17 정정). 제공자가 이미 확인한 값을 믿고 moa가 메일을 보내 확인하지 않는다
- **동시성**: 서로 다른 제공자로 같은 이메일이 동시에 가입하면 둘 다 확인을 통과할 수 있다. `email`은 한 회원 안에서 겹칠 수 있어(본인 예외) 유일 제약을 걸 수 없다. 가입 트랜잭션 안에서 다시 확인해 틈을 줄이고, 남는 틈(같은 순간 두 제공자로 가입)은 받아들인다. 운영에서 문제가 되면 이메일 잠금 테이블을 더한다
- **대안**: 이메일에 유일 제약 — 본인 예외(한 회원의 구글·디스코드가 같은 이메일)와 맞지 않는다

## R13. 재가입 제한 (FR-015)

- **결정**
  - 기록은 새 테이블 `social_rejoin_blocks`(data-model)에 둔다. 테이블은 010 마이그레이션이 만들고, 행을 쓰는 일은 회원 탈퇴 스펙이 맡는다. 010은 읽기만 한다
  - 계정 식별은 `HMAC-SHA256(SOCIAL_REJOIN_HASH_SECRET, "<provider>:<subject>")`의 16진수 64자다. 원래 subject와 개인정보는 남기지 않는다
  - `rejoin_available_at`이 비어 있으면 영구, 값이 있으면 그 시각 전까지 막는다(A016). 영구 정지 단계가 정해지기 전에도 이 칸 규칙은 그대로 둔다 (spec FR-015 [NEEDS CLARIFICATION]은 그런 기록이 생기는지의 문제다)
  - 가입(콜백·제출)과 연결(콜백) 때 확인한다
  - 지금의 회원 삭제(`002-admin-users` soft delete)는 외부 계정 연결 행을 지우지 않는다. 그래서 삭제된 회원의 소셜 계정으로는 `(provider, subject)` 유일 제약 때문에 다시 가입할 수 없다(A012). 탈퇴 처리(연결 행 정리, 재가입 제한 행 쓰기)는 회원 탈퇴 스펙이 정한다
- **비밀 값 운영**: 운영 중에 비밀 값을 바꾸면 기존 차단 기록과 대조할 수 없으므로 바꾸지 않는다. 값은 레포에 넣지 않고 환경별로 보관한다 (원칙 VI)
- **이유**: 디스코드 id 같은 subject는 추측해 볼 수 있는 값이라 그냥 SHA-256이면 대입으로 되돌릴 수 있다. 비밀 값을 섞으면 DB만 새어도 계정을 알아낼 수 없다
- **대안**: 그냥 SHA-256 — 위 이유로 쓰지 않는다. 회원 행을 지우지 않고 남기기 — 완전 삭제 정책과 맞지 않는다

## R14. 닉네임 태그와 `user_profiles` (FR-005, 2026-11-19)

규칙과 표시 형식의 정본은 `009-mypage` research R4다. 여기에는 010이 맡는 일만 적는다.

- **결정**
  - 010 마이그레이션이 `user_profiles`를 최소형으로 만든다: `user_id` BIGINT PK, `nickname_key` VARCHAR(50) NOT NULL, `nickname_tag` CHAR(4) NOT NULL, `created_at`·`updated_at`, 유일 `uk_user_profiles_nickname_tag (nickname_key, nickname_tag)`. 009가 나머지 칸을 더한다 (data-model)
  - 같은 마이그레이션이 기존 회원을 채운다: 삭제되지 않은 모든 `users`에 행, `nickname_key` = `LOWER(TRIM(name))`, `nickname_tag` = `LPAD(ROW_NUMBER() OVER (PARTITION BY LOWER(TRIM(name)) ORDER BY id), 4, '0')`
  - 010 구현이 `NicknamePolicy`(규칙)와 `NicknameTags`(`assign(key)`, `keepOrAssign(newKey, currentTag)`)를 `api/service/user/`에 만든다. 009는 그대로 쓴다
  - `NicknamePolicy`의 정규화 순서는 NFC 변환 → 앞뒤 공백 제거 → (키만) `toLowerCase(Locale.ROOT)`다. 분해형 한글(macOS 등에서 자모가 나뉘어 들어온 입력)이 완성형과 다른 키·다른 글자로 취급되어 같은 닉네임이 다른 태그 공간에 들어가거나 `^[가-힣…]` 검사에 걸리는 것을 막는다 (2026-11-19)
  - 가입(`signUp`)은 회원·외부 계정 연결·약관 동의와 같은 트랜잭션에서 `user_profiles` 행을 만든다. 태그는 `NicknameTags.assign`
  - 동시에 같은 태그를 고르면 `uk_user_profiles_nickname_tag` 위반이다. 가입 트랜잭션을 처음부터 다시 한다(최대 3번, 태그만 새로). 그래도면 B201. `(provider, subject)` 위반(같은 소셜 계정 동시 가입)은 다시 하지 않고 A012
  - 태그가 다 찬 닉네임은 409 `NICKNAME_TAGS_EXHAUSTED`(B201). 가입 대기 쿠키는 지우지 않아 닉네임 화면에서 다른 닉네임으로 다시 낸다 (US1-AC14)
  - 관리자 이름 바꾸기(`UserAdminService.rename`, 키트 user-admin, `002-admin-users` FR-007)가 `nickname_key`를 바꾸고 태그를 `keepOrAssign`으로 맞춘다. 010이 테이블을 만들고 같은 파일(소셜 회원 역할 막기, FR-007)을 이미 고치므로 010이 한다. 행이 없는 회원(키트 경로로 만든 계정)은 그대로 둔다
  - 가입 응답·`/users/me`에 태그를 더하지 않는다. 태그를 보이는 화면(마이페이지 머리)은 009가 만든다
- **이유**: 가입 때 닉네임이 처음 정해지므로 태그도 그때 정해야 `닉네임#태그`가 곧바로 고유하다. 010이 009보다 먼저 구현되므로 테이블과 규칙을 010이 만들고, 이름이 바뀌는 다른 길(관리자 이름 바꾸기)도 같은 때 맞춰야 키·태그가 어긋나지 않는다
- **대안**: 009가 테이블을 만들고 010은 기다리기 — 010이 먼저 구현 중이라 가입 때 태그를 줄 수 없다. 키트 `users`에 태그 칸 — 키트 테이블을 바꾼다 (원칙 III)

## 남은 스펙 미정과 계획의 처리

| 스펙 미정 | 계획에서의 처리 |
|---|---|
| 출처: 헬퍼 유무·연결 지원 (구글·디스코드) | R2 경계로 분리. 없으면 moa가 직접 |
| FR-004 회원 정보 모델 (잠정 B) | data-model을 잠정 B로 씀. users의 아이디·이름 칸 채우기도 잠정. 이메일 저장은 확정(FR-014) |
| FR-005 닉네임 규칙 | 잠정 규칙(2~12자, 한글·영문·숫자·`_`, 예약어 금지 — `009-mypage` FR-003과 같음)으로 검증하고 팀 확인 뒤 바뀌면 고친다. 중복은 허용하고 태그로 구별한다(R14). 가입 뒤 바꾸기와 변경 제한은 009가 맡는다 |
| FR-005 태그 직접 바꾸기, 표시 형식(팀 기획안 `@my_handle`과 다름) | `009-mypage` research R4 잠정안을 따른다. 핸들 방식으로 가면 R14·data-model `user_profiles`·닉네임 화면을 다시 정한다 |
| FR-005 만 14세 미만, 가입 때 출생 연도 | 이번 계획은 가입 화면에 출생 연도를 받지 않는다(현재 동작 유지). 팀 확인 뒤 정해지면 닉네임 화면과 가입 요청에 더한다 |
| FR-005 기존 회원 표시 이름 | 이번 구현은 기존 회원의 `name`을 그대로 둔다(변경 없음). 확정되면 따른다 |
| US4-AC1 접속 이력 표시 | R8 자리표시 |
| FR-015 영구 정지 단계 | R13: `rejoin_available_at`이 비면 영구로 본다. 그런 행을 쓸지는 회원 탈퇴·제재 스펙이 정한다 |
