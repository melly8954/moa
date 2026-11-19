# Contract: 소셜 로그인·가입·연결 API (010)

모든 경로는 프론트 출처에서 `src/proxy.ts`를 거쳐 백엔드로 간다 ([research.md](../research.md) R4).
"공개"는 `PublicPaths` 빈으로 연 경로다 (공용 `SecurityConfig`는 고치지 않는다). 제공자 쪽 처리(인가 URL, 코드 교환, 신원 확인)는 키트 oauth 헬퍼로 바뀔 수 있다 (R2, 팀 확인 후 바뀔 수 있음). 아래 외부 동작은 헬퍼와 상관없이 같다.

경로 변수 `{provider}`는 `google` 또는 `discord`다. 모르는 값이거나 설정이 꺼진 제공자면 A008.

## 오류 코드 (`ErrorCode`에 더함, 번호는 구현 때 다시 확인)

| 코드 | 이름 | HTTP | 언제 |
|---|---|---|---|
| A008 | `OAUTH_PROVIDER_DISABLED` | 404 | 모르는 제공자이거나 그 제공자 설정이 없는데 시작·콜백·가입·연결을 부름 (US3-AC3) |
| A009 | `OAUTH_FAILED` | 400 | state 불일치·만료, 코드 교환 실패, 신원 확인 실패 (US3-AC2) |
| A010 | `SOCIAL_LINK_NOT_ALLOWED` | 403 | 관리자·운영자 계정이 연결하려 함 (FR-007, US5-AC7) |
| A011 | `SOCIAL_MEMBER_ROLE_NOT_ALLOWED` | 409 | 소셜 회원에게 `ADMIN`·업무 역할을 주려 함 (FR-007, US1-AC6) |
| A012 | `SOCIAL_ACCOUNT_IN_USE` | 409 | 그 소셜 계정이 이미 다른 회원에 연결됨, 또는 동시 가입에서 진 쪽 (US5-AC3) |
| A013 | `SOCIAL_UNLINK_LAST_METHOD` | 409 | 비밀번호 없는 회원이 마지막 소셜 연결을 해제하려 함 (US5-AC6) |
| A014 | `SOCIAL_EMAIL_UNVERIFIED` | 400 | 제공자가 이메일을 주지 않거나 인증되지 않은 이메일 (FR-014, US1-AC12, US5-AC10) |
| A015 | `SOCIAL_EMAIL_IN_USE` | 409 | 다른 회원이 쓰는 이메일 (FR-014, US1-AC9, US5-AC8) |
| A016 | `SOCIAL_REJOIN_RESTRICTED` | 403 | 재가입 제한 기록에 걸림 (FR-015, US1-AC10·AC11, US5-AC9) |
| A017 | `TERMS_NOT_AGREED` | 400 | 필수 약관에 동의하지 않았거나 현재 버전이 아님 (FR-013, US1-AC7) |
| B201 | `NICKNAME_TAGS_EXHAUSTED` | 409 | 그 닉네임(대소문자 무시)의 태그 9999개가 모두 쓰임 (FR-005, US1-AC14). `009-mypage` contracts와 같은 코드다. 010이 먼저 더한다 (2026-11-19) |

기존 A007 `ACCOUNT_LOCKED`는 잠긴 회원의 소셜 로그인에도 쓴다 (US2-AC2).

## GET `/api/v1/auth/oauth/providers` — 공개

설정된 제공자 목록. 로그인 화면이 어떤 단추를 보일지 정한다 (US3-AC3, R9).

- 200 `{ "providers": ["google", "discord"] }` / 일부만 설정이면 그것만 / 없으면 `{ "providers": [] }`

## GET `/api/v1/terms` — 공개

가입 때 동의받을 현재 약관 버전 (FR-013, R11).

- 200 `{ "terms": [ { "type": "SERVICE", "version": "string", "required": true }, { "type": "PRIVACY", "version": "string", "required": true } ] }`
- 문안은 프론트가 `type`·`version`으로 정적 파일을 고른다

## GET `/api/v1/auth/oauth/{provider}/authorize` — 공개, 브라우저 이동

| 쿼리 | 뜻 |
|---|---|
| `mode` | `login`(기본) 또는 `link` |
| `next` | 로그인 뒤 돌아갈 같은 사이트 경로 (`safeNextPath` 규칙과 같게 서버도 검사) |

- `login`: 새 `oauth_state` 쿠키를 심고 302로 제공자 인가 화면 (구글: scope `openid email profile`, PKCE S256, nonce / 디스코드: scope `identify email`, PKCE S256)
- `link`: `POST …/link`(아래)가 심은 같은 제공자의 `LINK` 모드 `oauth_state`가 있어야 한다. 없거나 만료면 302 `/settings?socialLink=failed&provider={provider}`
- 설정 없음: 302 `/sign-in?oauthError=disabled`

## GET `/api/v1/auth/oauth/{provider}/callback` — 공개, 제공자가 돌려보냄

쿼리 `code`, `state` 또는 `error`. 결과는 모두 302 이동이다 (JSON을 돌려주지 않는다). LINK 이동에는 모두 `&provider={provider}`가 붙는다.

| 경우 | 처리 | 이동 |
|---|---|---|
| `error=access_denied` (취소) | 아무것도 안 함 | LOGIN: `/sign-in` / LINK: `/settings` (US3-AC1) |
| state 불일치·만료, 교환·확인 실패 | 실패 이력 (R8) | LOGIN: `/sign-in?oauthError=failed` / LINK: `/settings?socialLink=failed` (US3-AC2) |
| LOGIN, 연결된 회원 있음, 잠김 | 실패 이력 (A007) | `/sign-in?oauthError=locked` (US2-AC2) |
| LOGIN, 연결된 회원 있음 | 성공 이력, `AuthTokenService.issue` → 리프레시 쿠키 심기 | `/auth/oauth/done?next=…` (US2-AC1, US5-AC2) |
| LOGIN, 연결 없음, 이메일 없음·미인증 | 실패 이력 (A014), 회원 안 만듦 | `/sign-in?oauthError=emailUnverified` (US1-AC12) |
| LOGIN, 연결 없음, 재가입 제한 | 실패 이력 (A016), 회원 안 만듦 | `/sign-in?oauthError=rejoinBlocked` (US1-AC10·AC11) |
| LOGIN, 연결 없음, 이메일 중복 | 실패 이력 (A015), 회원 안 만듦 | `/sign-in?oauthError=emailInUse` (US1-AC9) |
| LOGIN, 연결 없음, 확인 통과 | 회원을 만들지 않음, `oauth_signup` 쿠키 | `/sign-up/nickname?next=…` (US1-AC1, US1-AC3, US5-AC4) |
| LINK, 회원이 관리자·운영자 | 막음 (A010) | `/settings?socialLink=notAllowed` (US5-AC7) |
| LINK, 이메일 없음·미인증 | 막음 (A014) | `/settings?socialLink=emailUnverified` (US5-AC10) |
| LINK, 재가입 제한 | 막음 (A016) | `/settings?socialLink=rejoinBlocked` (US5-AC9) |
| LINK, 그 소셜 계정이 다른 회원에 있음 | 막음 (A012) | `/settings?socialLink=inUse` (US5-AC3) |
| LINK, 이메일을 다른 회원이 씀 (본인 행 제외) | 막음 (A015) | `/settings?socialLink=emailInUse` (US5-AC8) |
| LINK, 성공 | 연결 행 만들기 | `/settings?socialLink=linked` (US5-AC1) |

- 콜백은 `oauth_state` 쿠키를 늘 지운다. 경로의 제공자와 쿠키의 제공자가 다르면 state 불일치로 본다
- 확인 순서는 research R12: 이메일 없음·미인증 → 재가입 제한 → 이메일 중복

## POST `/api/v1/auth/oauth/sign-up` — 공개 (가입 대기 쿠키 필요)

닉네임과 약관 동의를 받아 회원·연결·동의 기록을 만들고 로그인시킨다 (FR-005, FR-013, US1). 제공자는 가입 대기 쿠키에 있다.

- 요청
  ```json
  { "nickname": "string", "agreements": [ { "type": "SERVICE", "version": "string" }, { "type": "PRIVACY", "version": "string" } ] }
  ```
  - `nickname`: 잠정 규칙(data-model, `009-mypage` FR-003과 같음) — 앞뒤 공백 제거 뒤 2~12자, 한글·영문·숫자·`_`만, 예약어 금지는 V00x. 다른 회원과 겹쳐도 된다. 서버가 4자리 태그를 정한다(응답에 싣지 않음). 그 닉네임(대소문자 무시)의 태그가 다 찼으면 409 B201 `NICKNAME_TAGS_EXHAUSTED`(`009-mypage` contracts와 같은 코드, 010이 먼저 더함)
  - `agreements`: 필수 두 종류가 모두 있고 버전이 `GET /api/v1/terms`의 현재 버전과 같아야 한다
- 처리: 가입 확인(R12·R13)을 다시 한 뒤 한 트랜잭션으로 회원·프로필(`nickname_key`, 태그)·연결·약관 동의 2행 (research R14)
- 200 기존 `POST /api/v1/auth/login`과 같은 응답 모양(`AuthRefreshResponse`) + 리프레시 쿠키. `oauth_signup` 쿠키 지움. 성공 이력
- 400 V00x 닉네임 검증 실패 / A017 약관 미동의·버전 다름 / A009 가입 대기 쿠키 없음·만료 / A014·A015·A016 쿠키 발급 뒤 상황이 바뀌어 막힘 / 409 A012 동시 가입에서 이미 가입됨 / 409 B201 닉네임 태그가 다 참(가입 대기 쿠키는 그대로, US1-AC14)

## POST `/api/v1/users/me/social-accounts/{provider}/link` — 로그인 필요

연결 의도를 만든다 (R6). 관리자·운영자면 403 A010, 그 제공자가 이미 연결돼 있으면 409 B001.

- 200 `{ "authorizeUrl": "/api/v1/auth/oauth/{provider}/authorize?mode=link" }` + `LINK` 모드 `oauth_state` 쿠키. 프론트는 이 주소로 이동한다
- 이메일·재가입 확인은 제공자 신원을 받은 콜백에서 한다

## DELETE `/api/v1/users/me/social-accounts/{provider}` — 로그인 필요

연결 해제 (FR-006, US5-AC5). 비밀번호가 없고 남은 연결이 이것 하나면 409 A013 (US5-AC6). 연결이 없으면 204 그대로.

- 204
