# Contract: 내 정보 응답 추가 칸과 화면 (010)

## GET `/api/v1/users/me` — 칸 추가 (`UserGetResponse`, 프론트 `Me`)

| 칸 | 형식 | 쓰는 곳 |
|---|---|---|
| `hasPassword` | boolean | 계정 메뉴의 "비밀번호 변경" 숨김 (US2-AC3), 해제 단추 안내 (US5-AC6) |
| `socialAccounts` | `{ provider: "GOOGLE", email: string \| null, linkedAt: string }[]` | 계정 설정의 연결 상태 (US5-AC1·AC5) |
| `socialLinkAllowed` | boolean | 관리자·운영자 계정이면 거짓 → 계정 설정에 구글 연결을 보이지 않음 (FR-007, US5-AC7) |

기존 칸(`id, loginId, name, role, passwordChangeRequired`)은 그대로다. 추가 뒤 `npm run gen:api`로 타입을 다시 만든다.

## 사용자 역할 변경 API — 막는 규칙 추가 (응답 모양 변경 없음)

- 시스템 역할 변경(`UserAdminApplication.changeRole`): 대상이 구글 회원이고 새 역할이 `ADMIN`이면 409 A011
- 업무 역할 부여(`AccessRoleApplication.replaceRolesOfUser`, `addMembers`): 대상에 구글 회원이 있으면 409 A011
- `002-admin-users` FR-007, `003-admin-roles` 스펙 갱신은 "나중에 고칠 곳"에 있다 (이번에는 그 스펙을 고치지 않음)

## 화면 (프론트)

모두 메뉴로 열지 않는 화면이라 프로그램으로 등록하지 않는다 (`001-admin-menus` FR-008, spec FR-009).

| 경로 | 셸 | 내용 |
|---|---|---|
| `/sign-in` (기존) | public | `providers`에 google이 있으면 "구글로 계속하기" 단추 → `/api/v1/auth/oauth/google/authorize?next=…`로 이동. `oauthError`(`failed`·`locked`·`disabled`)면 문구 (US3) |
| `/sign-up/nickname` (새) | public | 닉네임 입력 → `POST …/sign-up` → 성공하면 기존 로그인 성공 처리(`signIn`, `landingPath`/`next`). 실패 문구 |
| `/auth/google/done` (새) | public | 화면 없이 `POST /api/v1/auth/token/refresh`로 액세스 토큰을 받아 기존 로그인 성공 처리. 실패하면 `/sign-in?oauthError=failed` |
| `/settings` (기존) | account | `socialLinkAllowed`일 때 "구글 계정" 줄: 연결 안 됨 → 연결 단추(`POST …/link` 뒤 `authorizeUrl`로 이동), 연결됨 → 이메일·해제 단추(`hasPassword` 거짓이면 해제 대신 안내). `googleLink` 쿼리로 결과 문구 |
| 계정 메뉴 (`components/shell/account-menu.tsx`) | 두 셸 | `hasPassword` 거짓이면 "비밀번호 변경" 숨김 |

- 문구는 `frontend/messages/ko.json`(`signIn`, `settings`, 새 `signUp` 묶음)에 둔다
