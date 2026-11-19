# Contract: 내 정보 응답 추가 칸과 화면 (010)

## GET `/api/v1/users/me` — 칸 추가 (`UserGetResponse`, 프론트 `Me`)

| 칸 | 형식 | 쓰는 곳 |
|---|---|---|
| `hasPassword` | boolean | 계정 메뉴의 "비밀번호 변경" 숨김 (US2-AC3), 해제 단추 안내 (US5-AC6) |
| `socialAccounts` | `{ provider: "GOOGLE" \| "DISCORD", email: string, linkedAt: string }[]` | 계정 설정의 제공자별 연결 상태 (US5-AC1·AC5) |
| `socialLinkAllowed` | boolean | 관리자·운영자 계정이면 거짓 → 계정 설정에 소셜 연결을 보이지 않음 (FR-007, US5-AC7) |

기존 칸(`id, loginId, name, role, passwordChangeRequired`)은 그대로다. 추가 뒤 `npm run gen:api`로 타입을 다시 만든다.

## 사용자 역할 변경 API — 막는 규칙 추가 (응답 모양 변경 없음)

- 시스템 역할 변경(`UserAdminApplication.changeRole`): 대상이 소셜 회원(제공자 상관없음)이고 새 역할이 `ADMIN`이면 409 A011
- 업무 역할 부여(`AccessRoleApplication.replaceRolesOfUser`, `addMembers`): 대상에 소셜 회원이 있으면 409 A011
- `002-admin-users` FR-007, `003-admin-roles` 스펙 갱신은 spec "나중에 고칠 곳"에 있다 (이번에는 그 스펙을 고치지 않음)

## 화면 (프론트)

모두 메뉴로 열지 않는 화면이라 프로그램으로 등록하지 않는다 (`001-admin-menus` FR-008, spec FR-009).

| 경로 | 셸 | 내용 |
|---|---|---|
| `/sign-in` (기존, 키트 화면) | public | `providers`에 있는 제공자마다 "구글로 계속하기"·"디스코드로 계속하기" 단추 → `/api/v1/auth/oauth/{provider}/authorize?next=…`로 이동. `oauthError`면 폼 위 문구: `failed`·`locked`·`disabled`·`emailUnverified`("제공자에서 이메일 인증 후 다시 시도")·`emailInUse`("다른 방식으로 가입된 이메일")·`rejoinBlocked`(재가입이 제한된 계정) (US3, US1-AC9·AC10·AC12) |
| `/sign-up/nickname` (새) | public | 닉네임 칸과 필수 약관 두 개(이용약관, 개인정보 수집·이용). 약관마다 체크 상자와 요약, 펼치면 전문. 둘 다 체크해야 제출 단추가 켜진다. `GET /api/v1/terms`의 버전으로 문안(`frontend/src/content/terms/`)을 고르고, `POST …/sign-up`에 닉네임과 동의한 버전을 보낸다 → 성공하면 기존 로그인 성공 처리(`signIn`, `landingPath`/`next`). 실패 문구(A017 → 약관을 다시 확인, A009 → 로그인 화면 안내, A012 → 이미 가입됨, A014~A016 → 로그인 화면의 같은 문구) (FR-005, FR-013, US1-AC3·AC7) |
| `/auth/oauth/done` (새) | public | 화면 없이 `POST /api/v1/auth/token/refresh`로 액세스 토큰을 받아 기존 로그인 성공 처리. 실패하면 `/sign-in?oauthError=failed` |
| `/settings` (기존) | account | `socialLinkAllowed`일 때 제공자마다 한 줄(구글, 디스코드): 연결 안 됨 → 연결 단추(`POST …/{provider}/link` 뒤 `authorizeUrl`로 이동), 연결됨 → 이메일·해제 단추. 비밀번호가 없고 연결이 하나뿐이면 해제 대신 "유일한 로그인 수단" 안내. `socialLink`·`provider` 쿼리로 결과 문구(`linked`·`inUse`·`notAllowed`·`failed`·`emailUnverified`·`emailInUse`·`rejoinBlocked`) (US5) |
| 계정 메뉴 (`components/shell/account-menu.tsx`) | 두 셸 | `hasPassword` 거짓이면 "비밀번호 변경" 숨김 |

- 짧은 문구는 `frontend/messages/ko.json`(`signIn`, `settings`, 새 `signUp` 묶음)에 둔다. 약관 요약·전문은 길어서 `ko.json`에 넣지 않고 `frontend/src/content/terms/`의 버전별 파일에 둔다 (research R11)
