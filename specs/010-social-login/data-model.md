# Data Model: 소셜 로그인 (010)

회원 정보 모델은 **잠정 B**다 (spec FR-004, Clarifications Session 2026-11-16 Q1): 회원 계정(키트 `tb_user`)은 그대로 두고, 외부 계정 연결을 따로 둔다.
팀 확인 필요 (담당: 팀 회의). 키트 oauth 헬퍼가 다른 구조를 쓰면 이 문서를 헬퍼에 맞춘다 ([research.md](./research.md) R2).

마이그레이션은 하나다: `V<다음 번호>__social_login.sql` (아래 새 테이블과 기존 회원 프로필 행 채우기). 번호는 적지 않고 PR 직전에 dev를 병합해 마지막 번호 + 1로 정한다 (`docs/db.md` "번호 겹침"). 순서 조건: 009는 010의 `tb_user_profile` 다음이다.

## 회원 — `tb_user` (키트, 구조 변경 없음)

| 칸 | 이 기능에서의 쓰임 |
|---|---|
| `login_id` (not null, 50) | 소셜 가입 회원은 로그인 아이디로 로그인하지 않지만 칸이 필수다. 잠정: `s_` + 무작위 영숫자 12자 (유일). 화면에 보이지 않는다 (`008-party` FR-027). **FR-004 (c) 확정 뒤 바뀔 수 있음** |
| `name` | 소셜 가입 회원은 닉네임을 넣는다 (FR-005). 지금 다른 기능이 `name`을 표시 이름으로 쓰므로 별도 닉네임 칸을 두지 않는다. 다른 회원과 겹쳐도 된다. 구별은 `tb_user_profile`의 태그가 한다. **FR-004 (c) 확정 뒤 바뀔 수 있음** |
| `password` (nullable) | 소셜 가입 회원은 비어 있다 → `hasPassword()` 거짓 (FR-010) |
| `role` | 소셜 가입 회원은 `USER` (FR-002) |
| `locked_at` | 잠긴 회원은 소셜 로그인도 막는다 (FR-003) |

- 제공자 이름은 표시 이름으로 쓰지 않는다 (FR-005). 제공자 이름·이메일은 아래 연결 행에만 남긴다

## 프로필(닉네임 태그) — `tb_user_profile` (새 테이블, 최소형)

회원 하나에 하나. 010은 닉네임 구별에 필요한 칸만 만들고, `009-mypage`가 나머지 프로필 칸을 더한다 ([research.md](./research.md) R14, `009-mypage` research R4·R14). 규칙의 정본은 `009-mypage` research R4다.

| 칸 | 형식 | 규칙 |
|---|---|---|
| `user_id` | BIGINT, PK | `tb_user.id` |
| `nickname_key` | VARCHAR(50), not null | `tb_user.name`의 앞뒤 공백 제거 + 소문자 (`NicknamePolicy.key`) |
| `nickname_tag` | CHAR(4), not null | `0001`~`9999`. 서버가 정한다 (`NicknameTags`) |
| `created_at`, `updated_at` | DATETIME(6), not null | |

- 유일: `(nickname_key, nickname_tag)` — 같은 닉네임(대소문자 무시) 안에서 태그가 겹치지 않는다
- 가입 트랜잭션에서 회원·연결·약관 동의와 함께 만든다. 태그는 그 `nickname_key`에서 쓰이지 않은 번호 중 무작위. 다 찼으면 B201 (FR-005, US1-AC13·AC14)
- 기존 회원 채우기(같은 마이그레이션): 삭제되지 않은 회원(`tb_user`의 `role = 'USER'`, 시스템 계정 제외)에 행. 관리자 계정(`ADMIN`)은 회원 활동을 하지 않아 프로필을 두지 않는다 (2026-11-22). `nickname_key` = `LOWER(TRIM(name))`, `nickname_tag` = 같은 `nickname_key` 안에서 id 순서대로 `0001`부터
- 관리자 이름 바꾸기(`002-admin-users` FR-007)는 `nickname_key`를 바꾸고 태그를 맞춘다: 새 `nickname_key`에서 지금 태그가 비어 있으면 그대로, 아니면 새로
- 키트 경로로 만든 계정(첫 관리자, 관리자가 만든 계정)은 행이 없다. 행은 009 마이페이지에서 처음 저장할 때 생긴다

## 외부 계정 연결 — `tb_user_social_account` (새 테이블, 잠정)

회원과 소셜 계정을 잇는다. 한 회원이 여러 개를 가질 수 있다(제공자마다 하나).

| 칸 | 형식 | 규칙 |
|---|---|---|
| `id` | BIGINT, PK | |
| `user_id` | BIGINT, not null | `tb_user.id` |
| `provider` | VARCHAR(20), not null | `GOOGLE` 또는 `DISCORD` (enum `SocialProvider`) |
| `subject` | VARCHAR(255), not null | 제공자가 주는 바뀌지 않는 계정 식별자(구글 `sub`, 디스코드 `id`) |
| `email` | VARCHAR(255), not null | 연결 때 제공자가 인증한 이메일. 앞뒤 공백 제거·소문자 (research R12). 제공자에서 바뀌어도 고치지 않는다 (Edge Cases) |
| `display_name` | VARCHAR(100), null | 연결 때 제공자 이름(구글 `name`, 디스코드 `global_name` 또는 `username`). 표시 이름으로 쓰지 않는다 |
| `created_at` | DATETIME(6), not null | 연결 시각 |

- 유일: `(provider, subject)` — 같은 소셜 계정은 한 회원에만 이어진다 (FR-002, FR-006, 동시 가입은 이 제약으로 하나만 성공)
- 유일: `(user_id, provider)` — 한 회원에 제공자마다 하나
- 색인: `user_id`, `email` (이메일 중복 확인, FR-014). `email`은 유일이 아니다 — 한 회원의 구글·디스코드가 같은 이메일일 수 있다 (US5-AC8 본인 예외)
- 인증되지 않은 이메일은 저장하지 않는다 (FR-014). 그래서 예전 `email_verified` 칸을 두지 않는다
- 회원 삭제(soft delete) 때: 지금은 연결 행을 남긴다 → 같은 소셜 계정으로 다시 가입할 수 없고(유일 제약), 그 이메일도 중복으로 막힌다. 탈퇴 처리(연결 행 정리)는 회원 탈퇴 스펙이 정한다 (spec FR-015, research R13)

## 약관 동의 기록 — `tb_user_terms_agreement` (새 테이블)

| 칸 | 형식 | 규칙 |
|---|---|---|
| `id` | BIGINT, PK | |
| `user_id` | BIGINT, not null | `tb_user.id` |
| `terms_type` | VARCHAR(20), not null | `SERVICE`(이용약관) 또는 `PRIVACY`(개인정보 수집·이용) (enum `TermsType`) |
| `terms_version` | VARCHAR(20), not null | 동의한 버전 (`app.terms.*`, research R11) |
| `agreed_at` | DATETIME(6), not null | 동의 시각 = 가입 트랜잭션 시각 |

- 유일: `(user_id, terms_type, terms_version)`
- 가입이 끝날 때 회원·연결과 한 트랜잭션으로 두 행을 만든다. 가입이 끝나지 않으면 만들지 않는다 (FR-013)
- 연결(FR-006)·로그인에서는 만들지 않는다. 약관이 바뀐 뒤 다시 동의받는 일은 범위 밖 (spec 가정)

## 재가입 제한 기록 — `tb_social_rejoin_block` (새 테이블)

**테이블은 010 마이그레이션이 만들고, 행을 쓰는 일은 회원 탈퇴 스펙 몫이다. 010은 읽기만 한다** (spec FR-015).

| 칸 | 형식 | 규칙 |
|---|---|---|
| `id` | BIGINT, PK | |
| `provider` | VARCHAR(20), not null | `GOOGLE` 또는 `DISCORD` |
| `subject_hash` | CHAR(64), not null | `HMAC-SHA256(SOCIAL_REJOIN_HASH_SECRET, "<provider>:<subject>")` 16진수 (research R13). 원래 subject는 남기지 않는다 |
| `rejoin_available_at` | DATETIME(6), null | 다시 가입할 수 있는 시각 = "탈퇴 후 30일"과 "남은 정지 기간 끝" 중 늦은 때. **비어 있으면 영구** |
| `created_at` | DATETIME(6), not null | 기록 시각 |

- 유일: `subject_hash` (같은 계정을 다시 탈퇴시키면 탈퇴 스펙이 행을 고친다)
- 회원·연결이 완전 삭제돼도 남는다. `tb_user`를 가리키는 칸이 없다
- 막는 조건: 행이 있고 (`rejoin_available_at`이 비었거나 지금보다 뒤)

## 저장하지 않는 상태 (서명 쿠키)

DB에 두지 않고 서명한 짧은 쿠키로 둔다 ([research.md](./research.md) R3·R5).

| 이름 | 내용 | 수명·범위 |
|---|---|---|
| `oauth_state` | 제공자, state, nonce(구글), PKCE verifier, 모드(`LOGIN`/`LINK`), `next`, (연결이면) 회원 id | 10분, Path `/api/v1/auth/oauth`, HttpOnly, SameSite `Lax`. 콜백에서 지움 |
| `oauth_signup` | 제공자, subject, 이메일(정규화), 제공자 이름, `next` | 10분, Path `/api/v1/auth/oauth`, HttpOnly, SameSite `Strict`. 가입 완료 때 지움 |

## 규칙과 상태 변화

```text
[연결 없음 소셜 계정] --콜백(LOGIN), 가입 확인 통과--> [가입 대기 쿠키] --닉네임 + 약관 동의 제출, 다시 확인--> [회원 + 프로필(태그) + 연결 + 약관 동의 2행] (USER, 비밀번호 없음)
                                                  └-- 닉네임 태그가 다 참 --> 쿠키 그대로, 닉네임 화면에서 다른 닉네임  (US1-AC14)
                       └-- 가입 확인에서 막힘 --> 아무것도 안 남음, /sign-in?oauthError=…   (US1-AC9·AC10·AC11·AC12)
                                                  └-- 떠남/10분 지남 --> 아무것도 안 남음  (FR-005, US1-AC5)
[회원] --연결(LINK), 연결 확인 통과--> [회원 + 연결] --해제--> [회원]   (FR-006, US5)
[비밀번호 없는 회원 + 연결 1개] --해제--> 막힘 (유일한 로그인 수단)        (US5-AC6)
```

| 규칙 | 근거 |
|---|---|
| 이메일이 없거나 제공자가 인증하지 않았으면 가입·연결 막음 | FR-014, US1-AC12, US5-AC10 |
| 재가입 제한 기록에 걸리면 가입·연결 막음 | FR-015, US1-AC10·AC11, US5-AC9 |
| 같은 이메일의 연결 행이 다른 회원에 있으면 가입·연결 막음 (연결은 본인 행 제외) | FR-014, US1-AC9, US5-AC8 |
| 같은 `(provider, subject)`가 다른 회원에 있으면 연결 막음 | FR-006, US5-AC3 |
| 관리자·운영자 계정(`ADMIN` 또는 업무 역할 보유)은 연결 막음 | FR-007, US5-AC7 |
| 연결이 하나라도 있는 회원에게 `ADMIN`·업무 역할 주기 막음 | FR-007, US1-AC6 |
| 비밀번호 없는 회원은 마지막 연결 해제 막음 | FR-006, US5-AC6 |
| 잠긴 회원은 소셜 로그인 막음 | FR-003, US2-AC2 |
| 필수 약관 두 종류를 현재 버전으로 동의해야 가입 | FR-013, US1-AC7·AC8 |
| 닉네임 (잠정, `009-mypage` FR-003과 같은 규칙): 앞뒤 공백 제거 뒤 2~12자, 한글·영문·숫자·`_`만, 예약어(admin, 운영자, moa 등) 금지. spec FR-005 [NEEDS CLARIFICATION] 팀 확인 뒤 바꿀 수 있음 | FR-005 |
| 닉네임은 겹쳐도 됨. 가입 때 서버가 4자리 태그를 정함. 그 닉네임의 태그가 다 찼으면 가입 막음(B201) | FR-005, US1-AC13·AC14 |
