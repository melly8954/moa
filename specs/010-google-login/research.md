# Research: 구글 소셜 로그인 (010)

스펙: [spec.md](./spec.md). 각 항목은 결정 / 이유 / 검토한 대안 순서다.
**"팀 확인 후 바뀔 수 있음"** 표시는 키트 oauth 헬퍼나 회원 정보 모델(FR-004) 같은 잠정 결정에 기댄 부분이다.

## R1. 서버 경유 방식 (OAuth 2.0 인가 코드 + PKCE) — 2026-11-17 팀 결정

- **결정**: 프론트는 백엔드 주소로 이동만 한다. 백엔드가 구글 인가 화면으로 보내고, 구글 콜백을 받아 인가 코드를 토큰으로 바꾼다. 구글 토큰과 client secret은 백엔드에만 둔다. 콜백에서 회원을 찾거나 만들고, 아이디·비밀번호 로그인과 같은 moa JWT·리프레시 쿠키를 발급한 뒤 프론트로 돌려보낸다. 계정 설정의 구글 연결(FR-006)도 같은 흐름을 쓴다. 콜백 경로는 공용 `SecurityConfig`를 고치지 않고 `PublicPaths` 빈으로 연다
- **이유**
  - 구글 토큰·client secret이 브라우저에 나오지 않는다 (원칙 VI, 프론트 번들·네트워크 탭에 비밀 값 없음)
  - 로그인 결과가 기존 로그인과 같은 토큰 체계(`AuthTokenService.issue`, `RefreshTokenTransport`)로 끝나 로그아웃·갱신·접속 이력을 따로 만들지 않는다 (FR-012). `AuthTokenApplication` 주석도 "소셜 로그인은 인증 뒤 `issue`를 부른다"고 둔다
  - 로그인과 연결이 같은 콜백을 써서 구현·테스트가 하나로 모인다
  - `PublicPaths`는 이미 이 용도(OAuth 콜백)를 주석에 적어 둔 확장점이라 공용 보안 설정을 고치지 않는다
- **대안**
  - 프론트 SDK(Google Identity Services)로 ID 토큰을 받아 백엔드로 보내기: 예전 `feat/008-google-login`이 이 방식(tokeninfo 검증)이었다. 구현은 작지만 팀 결정에 따라 쓰지 않는다
  - Spring Security `oauth2Login`: 기본이 세션 저장이라 상태 없는 JWT 구조와 맞추려면 쿠키 저장소·성공 처리기를 다시 짜야 하고 `HttpSecurityCustomizer`로 보안 필터 체인을 바꾸게 된다. 콜백을 컨트롤러로 두고 `PublicPaths`로 여는 쪽이 팀 결정과 맞는다

## R2. 키트 oauth 헬퍼 — 팀 확인 후 바뀔 수 있음

- **확인한 사실**: 적용 기록 `backend/.psw-dev-kit`(`springboot/v0.7.0`, helpers=code,login-history,menu,rbac,rich-text,user-admin,websocket)과 `frontend/.psw-dev-kit`에 oauth 헬퍼가 없다. 키트 저장소에 헬퍼가 있는지는 확인하지 못했다
- **결정 (잠정, 스펙 출처)**: 키트 oauth 헬퍼를 기본 방향으로 두고, 헬퍼가 맡을 것으로 보이는 "구글과 주고받는 부분"과 moa가 정하는 "회원 처리"를 나눠 설계한다
  - 헬퍼에 기댈 부분 (팀 확인 후 바뀔 수 있음): 인가 URL 만들기, state·PKCE·nonce 보관, 콜백에서 코드 교환, ID 토큰 검증, 공급자 설정(`app.oauth.*`), 콜백 경로 열기
  - 헬퍼와 상관없이 moa가 만드는 부분: 외부 계정 연결(data-model), 닉네임 가입 대기, 회원 찾기·만들기·연결·해제, 관리자·운영자 제한(FR-007), 토큰 발급 연결, 프론트 화면
  - 경계는 백엔드 안의 "공급자 클라이언트" 하나(인가 URL → 코드 교환 → 검증된 신원 `provider, subject, email, emailVerified, name`)로 둔다. 헬퍼가 오면 이 클라이언트를 헬퍼로 바꾸고, 헬퍼 구조가 data-model과 다르면 data-model을 헬퍼에 맞춘다
- **헬퍼가 없을 때 대안 (moa 직접)**: 같은 경계를 moa가 직접 만든다. Spring `RestClient`로 구글 토큰 엔드포인트를 부르고, 받은 ID 토큰은 백엔드가 구글과 TLS로 직접 받은 것이므로 서명 검증 대신 `iss`·`aud`·`exp`·`nonce`만 확인한다 (OIDC Core 3.1.3.7). 새 의존성은 없다(`RestClient`는 spring-web, JSON은 Jackson). 나중에 헬퍼가 생기면 이 클라이언트를 바꾸고 키트로 옮긴다 (원칙 III)

## R3. state·PKCE·nonce 보관 — 서명한 짧은 쿠키 (팀 확인 후 바뀔 수 있음)

- **결정**: 인가 시작 때 state, nonce, PKCE verifier, 모드(`LOGIN`/`LINK`), 돌아갈 주소(`next`), 연결이면 회원 id를 담은 값을 서명해 HttpOnly 쿠키(`oauth_state`, Path `/api/v1/auth/oauth`, SameSite `Lax`, 10분)로 둔다. 콜백에서 쿼리의 `state`와 쿠키 값을 비교하고 쿠키를 지운다
- **이유**: 서버가 상태 없는 JWT 구조라 세션을 새로 두지 않는다. 구글에서 돌아오는 콜백은 다른 사이트에서 온 최상위 GET 이동이라 `Strict`이면 쿠키가 안 붙고 `Lax`는 붙는다. 서명 키는 기존 JWT 서명 키와 다른 용도 구분(claim `typ=oauth_state`)으로 쓴다
- **대안**: DB·캐시에 state 저장 — 테이블과 정리 작업이 더 든다. 헬퍼가 다른 저장 방식을 쓰면 헬퍼를 따른다

## R4. 콜백 뒤 프론트로 돌아가기와 토큰 전달

- **결정**: 구글 `redirect_uri`는 프론트 주소의 `/api/v1/auth/oauth/google/callback`이다. 프론트 `src/proxy.ts`가 `/api/*`를 백엔드로 넘기므로 브라우저 입장에서는 프론트와 같은 출처다. 콜백은 리프레시 쿠키(`refresh_token`, Path `/api/v1/auth`)만 심고 상대 경로로 프론트 화면 `/auth/google/done?next=…`에 보낸다. 그 화면이 기존 `POST /api/v1/auth/token/refresh`로 액세스 토큰을 받아 `useAuthStore.signIn`을 부르고, 기존 첫 화면 규칙(`landingPath`, `next`)대로 이동한다 (FR-009, `001-admin-menus` FR-006)
- **이유**: 액세스 토큰을 주소에 싣지 않는다. 토큰 저장·첫 화면 결정은 기존 로그인 코드를 그대로 쓴다. 리프레시 쿠키는 `SameSite=Strict`지만 콜백 응답에서 심은 쿠키는 저장되고, 이후 프론트에서 부르는 갱신 요청은 같은 사이트 요청이라 붙는다
- **대안**: 콜백이 액세스 토큰을 주소 조각(`#token=`)으로 넘기기 — 기록·로그에 남을 위험이 있어 쓰지 않는다
- **실패·취소**: 콜백은 `/sign-in?oauthError=<코드>`(로그인) 또는 `/settings?googleLink=<결과>`(연결)로 보내고, 화면이 문구를 보인다 (US3, US5-AC3). 취소(`error=access_denied`)는 안내 없이 로그인 화면으로 돌아간다 (US3-AC1)

## R5. 닉네임 입력 전에는 회원을 만들지 않기 (FR-005, Q5 A)

- **결정**: 연결된 회원이 없는 구글 계정이 콜백에 오면 회원을 만들지 않고, 검증된 신원(공급자, subject, 이메일, 구글 이름)과 `next`를 서명한 가입 대기 쿠키(`oauth_signup`, Path `/api/v1/auth/oauth`, SameSite `Strict`, 10분)에 담아 `/sign-up/nickname`으로 보낸다. 닉네임 화면이 `POST /api/v1/auth/oauth/google/sign-up`에 닉네임을 보내면 그때 회원과 외부 계정 연결을 한 트랜잭션으로 만들고 로그인 토큰을 발급한다
- **이유**: 떠나면 쿠키만 사라져 회원이 생기지 않는다(US1-AC5). 다음 구글 로그인 때 다시 닉네임 화면부터 시작한다
- **동시 가입**: 외부 계정 연결의 `(provider, subject)` 유일 제약으로 두 번째 요청은 실패시키고 "이미 가입된 구글 계정" 안내 후 로그인 화면으로 보낸다 (Edge Cases)
- **대안**: 회원을 먼저 만들고 닉네임 없음 상태를 두기 — Q5에서 쓰지 않기로 했다

## R6. 연결(FR-006) 시작 — 로그인한 회원 확인

- **결정**: 계정 설정 화면이 액세스 토큰을 붙여 `POST /api/v1/users/me/social-accounts/google/link`를 부르면, 백엔드가 회원을 확인(관리자·운영자 금지, 이미 연결됨 금지)하고 `LINK` 모드 `oauth_state` 쿠키를 심은 뒤 이동할 주소(`/api/v1/auth/oauth/google/authorize?mode=link`)를 돌려준다. 프론트는 그 주소로 이동만 한다. 콜백은 쿠키 안의 회원 id에 연결한다
- **이유**: 최상위 이동에는 메모리에 있는 액세스 토큰이 붙지 않는다. 연결 의도를 인증된 요청에서 만들어 서명 쿠키에 묶으면 주소에 일회용 값을 싣지 않아도 된다
- **대안**: 리프레시 쿠키로 회원 확인 — 갱신 토큰을 다른 용도로 쓰게 돼 쓰지 않는다. 주소에 일회용 티켓 — 저장소가 더 든다

## R7. 관리자·운영자 제한 (FR-007)

- **결정**: "관리자·운영자 계정"은 시스템 역할 `ADMIN`이거나 업무 역할(rbac)을 하나라도 가진 계정이다
  - 연결 시작(R6)과 콜백에서 이 계정이면 막는다 (A010)
  - 구글 회원(외부 계정 연결이 하나라도 있는 회원)에게 `ADMIN`을 주는 `UserAdminService.changeRole`, 업무 역할을 주는 `RoleUserService`(`requireNotAdmin`·`requireTargets`)에서 막는다 (A011)
- **이유**: 막는 지점을 기존 역할 변경 서비스 안에 두면 화면·API 어디서 오든 같은 규칙이다
- **영향**: `002-admin-users` FR-007·`003-admin-roles`에 막는 규칙이 더해진다. 스펙 "나중에 고칠 곳"에 적어 두었고 이번에는 그 스펙을 고치지 않는다

## R8. 접속 이력 (FR-008)

- **결정**: 구글 로그인 성공·실패도 기존 `LoginAttemptedEvent`를 발행해 `LoginHistoryRecorder`가 남기게 한다. 새 실패 코드는 `ErrorCode`에 더한다
- **미정 (스펙 US4-AC1 [NEEDS CLARIFICATION])**: 이벤트의 `loginId` 칸에 무엇을 넣을지, 로그인 방식을 따로 보일지. 정해질 때까지 계획은 "회원이 있으면 그 회원의 loginId, 없으면 `google:` 접두와 구글 subject 일부"를 자리표시로 두고 tasks 단계에서 확정한다

## R9. 로그인 화면 단추와 비밀번호 변경 숨김 (US3-AC3, US2-AC3)

- **결정**
  - `GET /api/v1/auth/oauth/providers`(공개)가 설정된 공급자 목록(`["google"]` 또는 `[]`)을 돌려주고, 로그인 화면은 목록에 있을 때만 단추를 보인다
  - `GET /api/v1/users/me`(`UserGetResponse`, 프론트 `Me`)에 `hasPassword`, `socialAccounts`(연결된 공급자·이메일·연결 시각), `socialLinkAllowed`를 더한다. `AccountMenu`는 `hasPassword`가 거짓이면 "비밀번호 변경"을 숨기고, 계정 설정은 `socialLinkAllowed`일 때만 구글 연결을 보인다
- **이유**: 설정 유무와 관리자·운영자 판단을 서버 한 곳에서 한다. 프론트는 보이기만 정한다

## R10. 설정 값

- **결정**: `app.oauth.google.client-id`, `client-secret`, `redirect-uri`를 `GOOGLE_CLIENT_ID`·`GOOGLE_CLIENT_SECRET`·`GOOGLE_REDIRECT_URI` 환경 변수로 받는다. client-id가 비어 있으면 구글 로그인을 끈다(R9의 목록이 빈다, 시작 요청은 A008). 값은 `.env.<프로필>`이나 환경 변수로 주고 레포에는 이름만 `backend/.env.example`에 더한다 (헬퍼가 오면 헬퍼 설정 이름을 따른다 — 팀 확인 후 바뀔 수 있음)
- **주의**: `backend/.env.example`은 레포 파일이라 구현 단계에서 이름만 더한다. 실제 값은 다루지 않는다 (원칙 VI)

## 남은 스펙 미정과 계획의 처리

| 스펙 미정 | 계획에서의 처리 |
|---|---|
| 출처: 헬퍼 유무·연결 지원 | R2 경계로 분리. 없으면 대안대로 직접 |
| FR-004 회원 정보 모델 (잠정 B) | data-model을 잠정 B로 씀. users의 아이디·이름 칸 채우기도 잠정 |
| FR-005 닉네임 규칙 | 자리표시 규칙(2~20자, 중복 허용 여부 미정)으로 검증 지점만 두고 확정 뒤 숫자를 바꾼다 |
| FR-005 기존 회원 표시 이름 | 이번 구현은 기존 회원의 `name`을 그대로 둔다(변경 없음). 확정되면 따른다 |
| US1-AC3 약관·개인정보 동의 | 닉네임 화면에 칸을 두지 않는다. 정해지면 같은 화면에 더한다 |
| US4-AC1 접속 이력 표시 | R8 자리표시 |
| 삭제 회원 재가입 | 회원 삭제(soft delete) 때 외부 계정 연결을 어떻게 할지 정해질 때까지 연결 행을 남긴다(재가입 막힘). 확정 뒤 바꾼다 |
