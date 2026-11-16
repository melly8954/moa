# Data Model: 구글 소셜 로그인 (010)

회원 정보 모델은 **잠정 B**다 (spec FR-004, Clarifications Q1): 회원 계정(키트 `users`)은 그대로 두고, 외부 계정 연결을 따로 둔다.
팀 확인 필요 (담당: 팀 회의). 키트 oauth 헬퍼가 다른 구조를 쓰면 이 문서를 헬퍼에 맞춘다 ([research.md](./research.md) R2).

## 회원 — `users` (키트, 구조 변경 없음)

| 칸 | 이 기능에서의 쓰임 |
|---|---|
| `login_id` (not null, 50) | 구글 가입 회원은 로그인 아이디로 로그인하지 않지만 칸이 필수다. 잠정: `g_` + 무작위 영숫자 12자 (유일). 화면에 보이지 않는다 (`008-party` FR-027). **FR-004 (c) 확정 뒤 바뀔 수 있음** |
| `name` | 구글 가입 회원은 닉네임을 넣는다 (FR-005). 지금 다른 기능이 `name`을 표시 이름으로 쓰므로 별도 닉네임 칸을 두지 않는다. **FR-004 (c) 확정 뒤 바뀔 수 있음** |
| `password` (nullable) | 구글 가입 회원은 비어 있다 → `hasPassword()` 거짓 (FR-010) |
| `role` | 구글 가입 회원은 `USER` (FR-002) |
| `locked_at` | 잠긴 회원은 구글 로그인도 막는다 (FR-003) |

- 구글 이름은 표시 이름으로 쓰지 않는다 (FR-005). 구글 이름·이메일은 아래 연결 행에만 남긴다

## 외부 계정 연결 — `user_social_accounts` (새 테이블, 잠정)

회원과 소셜 계정을 잇는다. 한 회원이 여러 개를 가질 수 있고(공급자마다 하나), 이번에는 구글만 쓴다.

| 칸 | 형식 | 규칙 |
|---|---|---|
| `id` | BIGINT, PK | |
| `user_id` | BIGINT, not null | `users.id` |
| `provider` | VARCHAR(20), not null | `GOOGLE` (enum `SocialProvider`) |
| `subject` | VARCHAR(255), not null | 공급자가 주는 바뀌지 않는 계정 식별자(구글 `sub`) |
| `email` | VARCHAR(255), null | 연결 때 구글 이메일. 구글에서 바뀌어도 고치지 않는다 (Edge Cases, 2026-11-17) |
| `email_verified` | BOOLEAN, not null | 연결 때 값. 기록용이며 연결 조건으로 쓰지 않는다 (Clarifications 2-1) |
| `display_name` | VARCHAR(100), null | 연결 때 구글 이름. 표시 이름으로 쓰지 않는다 |
| `created_at` | DATETIME(6), not null | 연결 시각 |

- 유일: `(provider, subject)` — 같은 구글 계정은 한 회원에만 이어진다 (FR-002, FR-006, 동시 가입은 이 제약으로 하나만 성공)
- 유일: `(user_id, provider)` — 한 회원에 구글 하나
- 색인: `user_id`
- 마이그레이션: `V2_7__user_social_accounts.sql` (구현 때 dev의 최신 번호를 다시 확인한다)
- 회원 삭제(soft delete) 때: 연결 행을 남긴다 → 같은 구글 계정으로 재가입이 막힌다. spec Edge Cases "삭제 회원 재가입" [NEEDS CLARIFICATION]이 정해지면 바꾼다

## 저장하지 않는 상태 (서명 쿠키)

DB에 두지 않고 서명한 짧은 쿠키로 둔다 ([research.md](./research.md) R3·R5).

| 이름 | 내용 | 수명·범위 |
|---|---|---|
| `oauth_state` | state, nonce, PKCE verifier, 모드(`LOGIN`/`LINK`), `next`, (연결이면) 회원 id | 10분, Path `/api/v1/auth/oauth`, HttpOnly, SameSite `Lax`. 콜백에서 지움 |
| `oauth_signup` | 공급자, subject, 이메일, email_verified, 구글 이름, `next` | 10분, Path `/api/v1/auth/oauth`, HttpOnly, SameSite `Strict`. 가입 완료 때 지움 |

## 규칙과 상태 변화

```text
[연결 없음 구글 계정] --콜백(LOGIN)--> [가입 대기 쿠키] --닉네임 제출--> [회원 + 연결] (USER, 비밀번호 없음)
                                         └-- 떠남/10분 지남 --> (아무것도 안 남음)  (FR-005, US1-AC5)
[비밀번호 있는 회원] --연결(LINK)--> [회원 + 연결] --해제--> [비밀번호 있는 회원]  (FR-006, US5)
[비밀번호 없는 회원 + 연결] --해제--> 막힘 (유일한 로그인 수단)                     (US5-AC6)
```

| 규칙 | 근거 |
|---|---|
| 같은 `(provider, subject)`가 다른 회원에 있으면 연결 막음 | FR-006, US5-AC3 |
| 관리자·운영자 계정(`ADMIN` 또는 업무 역할 보유)은 연결 막음 | FR-007, US5-AC7 |
| 연결이 하나라도 있는 회원에게 `ADMIN`·업무 역할 주기 막음 | FR-007, US1-AC6 |
| 비밀번호 없는 회원은 마지막 연결 해제 막음 | FR-006, US5-AC6 |
| 잠긴 회원은 구글 로그인 막음 | FR-003, US2-AC2 |
| 닉네임: 자리표시 2~20자, 앞뒤 공백 제거. 중복·글자 규칙은 spec FR-005 [NEEDS CLARIFICATION] 확정 뒤 | FR-005 |
