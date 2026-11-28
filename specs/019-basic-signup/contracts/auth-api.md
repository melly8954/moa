# API 계약: 기본 회원가입·로그인 (019)

모두 `/api/v1/auth/` 아래이고 로그인 없이 연다(`@PermitAll`). 로그인한 사람을 막는 것은 화면이 한다(FR-011). 서버는 로그인 여부를 보지 않는다. 로그인한 채 불러도 새 회원이 생길 뿐 남의 정보는 드러나지 않는다.
응답 본문·오류 모양은 기존 API와 같다(`ErrorResponse`). 오류 번호는 구현 직전에 `ErrorCode.java`에서 다시 확인한다 (research R12).

## 가입

### `GET /api/v1/auth/sign-up/availability`

- 200 `{ "emailSignupEnabled": true }`. 메일이 꺼진 환경(`app.mail.sender = off`)이면 `false`이고 화면은 "지금은 이메일 가입을 쓸 수 없어요"를 보인다 (FR-018)

### `POST /api/v1/auth/sign-up/email-codes` — 코드 받기

- 요청 `{ "email": "a@b.com" }`
- 202 `{ "expiresInSeconds": 600, "resendAfterSeconds": 60 }`. 이메일이 새 것이든 이미 쓰이든(회원·소셜·탈퇴 유예) **같은 응답**이다. 새 이메일이면 코드 메일, 이미 쓰는 이메일이면 안내 메일을 커밋 뒤 보낸다 (FR-005, US5-AC1·AC2·AC6)
- 오류: 400 `V001`(형식), 429 `B504` `{ retryAfterSeconds }`(60초 안), 429 `B505`(1시간 5번), 404 `A504`(메일 꺼짐)

### `POST /api/v1/auth/sign-up/email-codes/verify` — 코드 확인

- 요청 `{ "email": "a@b.com", "code": "123456" }`
- 200 `{ "email": "a@b.com" }` + `Set-Cookie: email_signup=…; HttpOnly; SameSite=Strict; Path=/api/v1/auth/sign-up; Max-Age=1800`
- 오류: 400 `B501`(코드가 맞지 않음, 남은 횟수 `remainingAttempts`), 400 `B502`(만료), 400 `B503`(5번 틀려 못 씀), 409 `B506`(이미 가입된 이메일)·409 `B507`(다른 방식으로 가입된 이메일) — 코드를 보낸 뒤 다른 사람이 먼저 가입한 경우만 (US5-AC5)

### `POST /api/v1/auth/sign-up` — 계정 만들기

- 쿠키 `email_signup` 필요. 요청 `{ "loginId": "moa_user", "password": "…", "nickname": "모아", "agreements": [{ "type": "TERMS", "version": "…" }, { "type": "PRIVACY", "version": "…" }] }` (약관 모양은 010 `sign-up`과 같다)
- 201: 010 `POST /api/v1/auth/oauth/sign-up`과 같은 로그인 응답(액세스 토큰 본문 + 리프레시 쿠키). `email_signup` 쿠키를 지운다
- 오류: 401 `A502`(쿠키 없음·만료·이미 씀, US1-AC10·AC11), 400 `V001`(칸 오류, `fieldErrors`), 409 `B508`(이미 쓰는 아이디), 409 `B506`·`B507`(그사이 이메일이 쓰임), 409 `B201`(닉네임 태그가 다 참, 010과 같다), 400 `V001`(약관 버전이 현재와 다름, 010과 같다)

## 비밀번호 재설정

### `POST /api/v1/auth/password-reset/email-codes`

- 요청 `{ "email": "a@b.com" }`
- 202 `{ "expiresInSeconds": 600, "resendAfterSeconds": 60 }`. 계정이 있든 없든 **같은 응답**이다 (FR-014). 메일은 정상 비밀번호 회원일 때만 커밋 뒤 보낸다
- 오류: `V001`, `B504`, `B505`, `A504` (가입과 같다. 발송 제한은 계정 유무와 상관없이 센다)

### `POST /api/v1/auth/password-reset/email-codes/verify`

- 요청 `{ "email": "a@b.com", "code": "123456" }`
- 200 `{ "loginId": "moa_user" }` + `Set-Cookie: password_reset=…; HttpOnly; SameSite=Strict; Path=/api/v1/auth/password-reset; Max-Age=1800`
- 오류: `B501`, `B502`, `B503`. 메일을 받지 않은 이메일도 같은 규칙(행이 있으므로 `B501`→5번이면 `B503`, 10분 뒤 `B502`)이다

### `POST /api/v1/auth/password-reset`

- 쿠키 `password_reset` 필요. 요청 `{ "newPassword": "…" }`
- 204. 비밀번호를 바꾸고, 리프레시 토큰을 모두 끊고, 비밀번호 변경 요구와 임시 잠금을 푼다. 쿠키를 지운다 (FR-013, US3-AC4·AC5)
- 오류: 401 `A503`(쿠키 없음·만료·이미 씀, US3-AC7), 400 `V001`(규칙)

## 로그인 (기존 `POST /api/v1/auth/login`, 키트)

- 요청·성공 응답은 그대로다
- 새 오류 401 `A501` `{ "lockedUntil": "2026-11-28T10:15:00" }`: 다섯째로 틀렸을 때와 잠금 중의 모든 시도 (US4-AC2·AC3·AC6). 없는 아이디도 같다
- 순서와 접속 이력의 실패 코드는 research R7 표

## 로컬 전용

### `GET /api/local/mail-outbox`

- local 프로필에서만 등록된다. 200 `[{ "to": "a@b.com", "kind": "SIGNUP_CODE", "code": "123456", "sentAt": "…" }]`(최근 50통). 가짜 발송기의 우편함을 보인다. 로그에는 쓰지 않는다 (FR-019·FR-020)

## 오류 코드 (잠정)

| 코드 | HTTP | 이름 | 뜻 |
|---|---|---|---|
| A501 | 401 | `LOGIN_TEMPORARILY_LOCKED` | 로그인 실패 5번으로 15분 잠김 |
| A502 | 401 | `EMAIL_SIGNUP_EXPIRED` | 인증된 가입 상태가 없거나 끝남 |
| A503 | 401 | `PASSWORD_RESET_EXPIRED` | 재설정 허락이 없거나 끝남 |
| A504 | 404 | `EMAIL_DISABLED` | 이 환경은 메일을 쓰지 않는다 |
| B501 | 400 | `VERIFICATION_CODE_MISMATCH` | 코드가 맞지 않음 |
| B502 | 400 | `VERIFICATION_CODE_EXPIRED` | 코드가 만료됨 |
| B503 | 400 | `VERIFICATION_CODE_LOCKED` | 5번 틀려 새 코드가 필요함 |
| B504 | 429 | `VERIFICATION_RESEND_TOO_SOON` | 60초 안에 다시 받음 |
| B505 | 429 | `VERIFICATION_SEND_LIMIT` | 1시간 5번을 넘음 |
| B506 | 409 | `EMAIL_ALREADY_REGISTERED` | 회원 이메일로 쓰임 (코드를 맞힌 사람에게만) |
| B507 | 409 | `EMAIL_USED_BY_SOCIAL` | 소셜 계정 연결의 이메일 (코드를 맞힌 사람에게만) |
| B508 | 409 | `LOGIN_ID_TAKEN` | 이미 쓰는 아이디 |

- 프론트 `messages/ko.json`의 `errors`에 같은 키로 문구를 둔다. 접속 이력 화면의 실패 사유 `loginHistory.failure.A501` = "임시 잠금" (005 화면, plan "다른 담당 코드")
