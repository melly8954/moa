# Contract: 구글 로그인·가입·연결 API (010)

모든 경로는 프론트 출처에서 `src/proxy.ts`를 거쳐 백엔드로 간다 ([research.md](../research.md) R4).
"공개"는 `PublicPaths` 빈으로 연 경로다 (공용 `SecurityConfig`는 고치지 않는다). 공급자 쪽 처리(인가 URL, 코드 교환, ID 토큰 검증)는 키트 oauth 헬퍼로 바뀔 수 있다 (R2, 팀 확인 후 바뀔 수 있음). 아래 외부 동작은 헬퍼와 상관없이 같다.

## 오류 코드 (`ErrorCode`에 더함, 번호는 구현 때 다시 확인)

| 코드 | 이름 | HTTP | 언제 |
|---|---|---|---|
| A008 | `OAUTH_PROVIDER_DISABLED` | 404 | 구글 설정이 없는데 시작·콜백·가입을 부름 (US3-AC3) |
| A009 | `OAUTH_FAILED` | 400 | state 불일치·만료, 코드 교환 실패, ID 토큰 검증 실패 (US3-AC2) |
| A010 | `SOCIAL_LINK_NOT_ALLOWED` | 403 | 관리자·운영자 계정이 연결하려 함 (FR-007, US5-AC7) |
| A011 | `SOCIAL_MEMBER_ROLE_NOT_ALLOWED` | 409 | 구글 회원에게 `ADMIN`·업무 역할을 주려 함 (FR-007, US1-AC6) |
| A012 | `SOCIAL_ACCOUNT_IN_USE` | 409 | 그 구글 계정이 이미 다른 회원에 연결됨, 또는 동시 가입에서 진 쪽 (US5-AC3) |
| A013 | `SOCIAL_UNLINK_LAST_METHOD` | 409 | 비밀번호 없는 회원이 구글 연결을 해제하려 함 (US5-AC6) |

기존 A007 `ACCOUNT_LOCKED`는 잠긴 회원의 구글 로그인에도 쓴다 (US2-AC2).

## GET `/api/v1/auth/oauth/providers` — 공개

설정된 공급자 목록. 로그인 화면이 단추를 보일지 정한다 (US3-AC3, R9).

- 200 `{ "providers": ["google"] }` / 설정 없으면 `{ "providers": [] }`

## GET `/api/v1/auth/oauth/google/authorize` — 공개, 브라우저 이동

| 쿼리 | 뜻 |
|---|---|
| `mode` | `login`(기본) 또는 `link` |
| `next` | 로그인 뒤 돌아갈 같은 사이트 경로 (`safeNextPath` 규칙과 같게 서버도 검사) |

- `login`: 새 `oauth_state` 쿠키를 심고 302로 구글 인가 화면(scope `openid email profile`, PKCE S256, nonce)
- `link`: `POST …/link`(아래)가 심은 `LINK` 모드 `oauth_state`가 있어야 한다. 없거나 만료면 302 `/settings?googleLink=failed`
- 설정 없음: 302 `/sign-in?oauthError=disabled`

## GET `/api/v1/auth/oauth/google/callback` — 공개, 구글이 돌려보냄

쿼리 `code`, `state` 또는 `error`. 결과는 모두 302 이동이다 (JSON을 돌려주지 않는다).

| 경우 | 처리 | 이동 |
|---|---|---|
| `error=access_denied` (취소) | 아무것도 안 함 | LOGIN: `/sign-in` / LINK: `/settings` (US3-AC1) |
| state 불일치·만료, 교환·검증 실패 | 실패 이력 (R8) | LOGIN: `/sign-in?oauthError=failed` / LINK: `/settings?googleLink=failed` (US3-AC2) |
| LOGIN, 연결된 회원 있음, 잠김 | 실패 이력 (A007) | `/sign-in?oauthError=locked` (US2-AC2) |
| LOGIN, 연결된 회원 있음 | 성공 이력, `AuthTokenService.issue` → 리프레시 쿠키 심기 | `/auth/google/done?next=…` (US2-AC1, US5-AC2) |
| LOGIN, 연결된 회원 없음 | 회원을 만들지 않음, `oauth_signup` 쿠키 | `/sign-up/nickname?next=…` (US1-AC1, US1-AC3, US5-AC4) |
| LINK, 회원이 관리자·운영자 | 막음 (A010) | `/settings?googleLink=notAllowed` |
| LINK, 구글 계정이 다른 회원에 있음 | 막음 (A012) | `/settings?googleLink=inUse` (US5-AC3) |
| LINK, 성공 | 연결 행 만들기 | `/settings?googleLink=linked` (US5-AC1) |

- 콜백은 `oauth_state` 쿠키를 늘 지운다

## POST `/api/v1/auth/oauth/google/sign-up` — 공개 (가입 대기 쿠키 필요)

닉네임을 받아 회원과 연결을 만들고 로그인시킨다 (FR-005, US1).

- 요청 `{ "nickname": "string" }` — 자리표시 검증 2~20자 (data-model)
- 200 기존 `POST /api/v1/auth/login`과 같은 응답 모양(`AuthRefreshResponse`) + 리프레시 쿠키. `oauth_signup` 쿠키 지움. 성공 이력
- 400 V00x 닉네임 검증 실패 / A009 가입 대기 쿠키 없음·만료 / A012 동시 가입에서 이미 가입됨

## POST `/api/v1/users/me/social-accounts/google/link` — 로그인 필요

연결 의도를 만든다 (R6). 관리자·운영자면 403 A010, 이미 구글이 연결돼 있으면 409 B001.

- 200 `{ "authorizeUrl": "/api/v1/auth/oauth/google/authorize?mode=link" }` + `LINK` 모드 `oauth_state` 쿠키. 프론트는 이 주소로 이동한다

## DELETE `/api/v1/users/me/social-accounts/google` — 로그인 필요

연결 해제 (FR-006, US5-AC5). 비밀번호 없는 회원이면 409 A013 (US5-AC6). 연결이 없으면 204 그대로.

- 204
