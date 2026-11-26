# Contract: 탈퇴·복구 API (018)

오류 응답 형식은 골격 `ErrorResponse`를 따른다. 근거는 [research.md](../research.md) R2·R4·R5, 상태는 [data-model.md](../data-model.md).

## 오류 코드 (`ErrorCode`에 더함, 번호는 구현 때 다시 확인)

| 코드 | 이름 | HTTP | 언제 |
|---|---|---|---|
| A401 | `ACCOUNT_WITHDRAWN` | 401 | 본인 확인에 성공했지만 유예 중 (FR-010). 복구 쿠키가 함께 온다 |
| A402 | `ACCOUNT_PURGE_PENDING` | 401 | 유예가 끝나고 파기 전 (FR-012). 복구할 수 없다 |
| A403 | `RECOVERY_NOT_AVAILABLE` | 400 | 복구 쿠키가 없거나, 만료·위조이거나, 지금 상태와 다름 (US2-AC10) |

이미 있는 것: A001, A002, A003, A006, A007, A201, V001.

## POST `/api/v1/users/me/withdrawal` — 탈퇴

- 로그인 필요. 요청 `{ "nickname": "string" }`
- 200 `{ "purgeAt": "2026-12-25T10:00:00+09:00" }`. 리프레시 쿠키를 지운다 (US1-AC5)
- 그 회원의 푸시 구독을 모두 지운다 (US1-AC11)
- 403 A201 관리자 계정 (US1-AC10)
- 400 V001(칸 `nickname`) 닉네임이 다름 (US1-AC4)
- 이미 탈퇴한 요청이 겹치면 처음 시각으로 200

## 탈퇴한 뒤의 모든 API

- 탈퇴 전에 받은 액세스 토큰: 로그인이 필요한 API는 401 A002. 로그인 없이 되는 API는 로그인하지 않은 것으로 답한다 (US1-AC6)
- `POST /api/v1/auth/token/refresh`: 401 A003
- 웹소켓 `CONNECT`: 거절. 열린 연결은 닫힌다

## POST `/api/v1/auth/login` — 로그인 (기존, 응답이 늘어남)

| 상황 | 응답 |
|---|---|
| 아이디·비밀번호가 틀림 (탈퇴했어도) | 401 A006, 쿠키 없음 (US2-AC3) |
| 맞음, 잠김 (탈퇴했어도) | 401 A007, 쿠키 없음 (US2-AC8) |
| 맞음, 유예 중 | 401 A401, `withdrawal_recovery` 쿠키 (US2-AC2) |
| 맞음, 파기 대기 | 401 A402 (US2-AC9) |

## GET `/api/v1/auth/oauth/{provider}/callback` — 소셜 콜백 (기존, 갈 곳이 늘어남)

| 상황 | 이동 |
|---|---|
| 연결된 회원, 잠김 | 지금처럼 `/sign-in?oauthError=locked` |
| 연결된 회원, 유예 중 | `withdrawal_recovery` 쿠키 + `/sign-in/recover` (`next`가 있으면 붙임) (US2-AC1) |
| 연결된 회원, 파기 대기 | `/sign-in?oauthError=purgePending` (US2-AC9) |

## GET `/api/v1/auth/recovery` — 복구 안내

- 로그인 없이 부른다. 복구 쿠키로 확인한다
- 200 `{ "purgeAt": "…" }`
- 400 A403 쿠키 없음·만료·위조, 이미 복구됨, 다시 탈퇴함
- 401 A402 유예가 지남 (POST와 같다)

## POST `/api/v1/auth/recovery` — 복구

- 로그인 없이 부른다. 본문 없음. 복구 쿠키로 확인한다
- 200 로그인과 같은 토큰 응답. 리프레시 쿠키를 쓰고 복구 쿠키를 지운다 (US2-AC4)
- 400 A403 쿠키 없음·만료·위조, 이미 복구됨, 다시 탈퇴함 (US2-AC10)
- 401 A007 잠김, 401 A402 유예가 지남

## 관리자 API (코드는 바뀌지 않음)

- 계정 만들기: 유예 중·파기 대기인 회원의 아이디는 지금처럼 409 R002 (US2-AC12)
- 사용자 관리 목록·하나 조회·바꾸기·지우기: 유예 중·파기 대기인 회원은 없는 사용자다(목록에 없음, 404 R001). 복구하면 다시 나온다 (US1-AC13)
- 파기 이력 목록: 대상 종류 `MEMBER`가 늘어난다. 스스로 탈퇴한 회원과 관리자가 지운 회원이 같은 종류다

## 개인 알림 (007 `NotificationApplication.notify`)

- 유예 중인 회원이면 저장하지 않고 null을 돌려준다. 실시간·푸시도 없다 (US1-AC12)

## 접속 이력

- A401·A402는 실패, 복구는 성공으로 남는다
