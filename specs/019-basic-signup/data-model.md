# Data Model: 기본 회원가입·로그인 (019-basic-signup)

마이그레이션은 한 파일 `V<다음 번호>__basic_signup.sql`이다. 번호는 적지 않고 PR 직전에 dev를 병합해 마지막 번호 + 1로 정한다 (`docs/db.md` "번호 겹침"). 순서 조건: 018 `V13__member_withdrawal.sql`, 017 `V14__customer_center.sql` 다음.
머리 주석: `-- moa: 회원 이메일, 이메일 점유(소셜 이메일 백필), 이메일 인증 코드, 로그인 실패 잠금. 스펙 specs/019-basic-signup (FR-005~FR-008, FR-015, FR-021, research.md R1·R2·R3·R6)`.
엔진·문자셋·정렬은 `V1__schema.sql`의 다른 테이블과 같다. `DEFAULT`와 외래 키는 두지 않는다.

## 회원 — `tb_user` (키트, 바꾸지 않음)

- 이 스펙으로 가입한 회원: `login_id`(소문자, FR-003), `password`(bcrypt), `name`(닉네임), `role` = `USER`, `password_change_required` = false
- 이미 쓰는 아이디는 `uk_user_login_id_active_key`가 막는다. 탈퇴 유예 중에도 `active_key`=1이라 붙잡힌다 (018 research R1)

## 회원 이메일 — `tb_user_email` (새 테이블, research R1)

엔터티 `UserEmail`은 `BaseTimeEntity` + `@HardDelete`다. 가입할 때 만들고, 이번 범위에서는 바꾸지 않는다. 018 파기 때 행을 지운다.

| 열 | 타입 | 뜻 | COMMENT |
|---|---|---|---|
| `user_id` | BIGINT NOT NULL, PK | 회원 ID | `'회원 ID'` |
| `email` | VARCHAR(255) NOT NULL `COLLATE utf8mb4_bin` | 정규화한 이메일 (010 `normalizeEmail`: `strip()`·`toLowerCase(Locale.ROOT)`, NFC 없음, FR-005) | `'인증한 회원 이메일 (공백 제거·소문자; utf8mb4_bin: 점유 해시와 같은 바이트 기준)'` |
| `verified_at` | DATETIME(6) NOT NULL | 인증한 시각 | `'이메일 인증 시각'` |
| `created_at`, `updated_at` | DATETIME(6) NOT NULL | 감사 | — |

- 유니크 `uk_user_email_email (email)`. 탈퇴 유예 중이거나 관리자가 지운 회원의 행도 파기 때까지 남아 이메일을 붙잡는다 (FR-023)
- 테이블 COMMENT `'회원 이메일 (PK: 1:1 테이블; 가입 인증·비밀번호 재설정에만 씀)'`
- 소셜 계정 연결의 `tb_user_social_account.email`과의 겹침은 아래 이메일 점유 표가 막는다 (research R2)

## 이메일 점유 — `tb_email_claim` (새 테이블, research R2, plan D5)

엔터티 `EmailClaim`은 `@HardDelete`, 부모 없음. 이메일 하나에 한 행이고 그 이메일을 쓰는 회원을 가리킨다.

| 열 | 타입 | 뜻 | COMMENT |
|---|---|---|---|
| `id` | BIGINT AUTO_INCREMENT PK | | — |
| `email_hash` | CHAR(64) NOT NULL | 정규화 이메일(010 `normalizeEmail`, NFC 없음)의 UTF-8 바이트 SHA-256, 소문자 16진. 키 없음: 백필을 SQL `SHA2`로 하려고 | `'이메일 해시 (SHA-256 소문자 16진; 공백 제거·소문자 이메일)'` |
| `user_id` | BIGINT NOT NULL | 점유한 회원 | `'점유한 회원 ID'` |
| `created_at` | DATETIME(6) NOT NULL | 점유한 시각 | `'점유한 시각'` |

- 유니크 `uk_email_claim_email_hash (email_hash)`, 인덱스 `idx_email_claim_user_id (user_id)`
- 테이블 COMMENT `'이메일 점유 (감사 열 없음: 가입·연결 때 넣고 파기·연결 해제 때 삭제)'`
- 넣는 때: 이메일 가입(019)의 계정 만들기, 소셜 가입(010 `signUp`), 소셜 연결(010 US5). 같은 해시의 행이 이미 그 회원 것이면 넣지 않는다. 남의 것이면 중복 키로 막힌다
- 지우는 때: 회원 파기(018), 소셜 연결 해제(010 US5)에서 그 회원의 회원 이메일·다른 소셜 연결이 그 이메일을 더 쓰지 않을 때
- 유예 중·관리자가 지운 회원의 행은 파기 때까지 남아 이메일을 붙잡는다 (FR-023)
- 백필 (같은 마이그레이션): `INSERT INTO tb_email_claim (email_hash, user_id, created_at) SELECT SHA2(email, 256), MIN(user_id), MIN(created_at) FROM tb_user_social_account GROUP BY email COLLATE utf8mb4_bin`. 한 이메일을 여러 회원이 쓰는 행이 있으면 가장 작은 회원 ID가 점유한다
  - 저장된 `email`은 모두 010 `normalizeEmail`을 거친 값이라(010 코드로 확인) 그대로 해시하면 앱 해시와 같다. `LOWER(TRIM())`으로 감싸지 않는다. 묶음은 바이트(`utf8mb4_bin`)로 가른다: 열의 `utf8mb4_unicode_ci`는 악센트도 무시한다 (research R2 "백필 해시와 앱 해시가 같은 값인가")
  - 마이그레이션 전에 겹침·정규화 안 된 값·ASCII 밖 값을 센다 (tasks 첫 작업, quickstart 19)

## 이메일 인증 코드 — `tb_email_verification` (새 테이블, research R3)

엔터티 `EmailVerification`은 `@HardDelete`, 부모 없음(필요한 시각 열만). 요청마다 한 행.

| 열 | 타입 | 뜻 | COMMENT |
|---|---|---|---|
| `id` | BIGINT AUTO_INCREMENT PK | | — |
| `email_hash` | CHAR(64) NOT NULL | 정규화한 이메일의 HMAC-SHA256 (16진) | `'이메일 해시 (HMAC-SHA256; 원문 저장 안 함)'` |
| `purpose` | VARCHAR(20) NOT NULL | 쓰임 | `'쓰임 (SIGNUP, PASSWORD_RESET)'` |
| `mail_kind` | VARCHAR(20) NOT NULL | 보낸 메일 | `'보낸 메일 (CODE, ALREADY_REGISTERED, NONE)'` |
| `user_id` | BIGINT NULL | 재설정 대상 회원 | `'재설정 대상 회원 ID (NULL: 가입이거나 대상 없음)'` |
| `code_hash` | CHAR(64) NOT NULL | 코드의 HMAC-SHA256. 코드를 보내지 않은 행은 무작위 코드의 해시 | `'인증 코드 해시 (HMAC-SHA256)'` |
| `status` | VARCHAR(20) NOT NULL | 상태 | `'상태 (PENDING, VERIFIED, CONSUMED, REVOKED, LOCKED, SEND_FAILED)'` |
| `failed_attempts` | TINYINT NOT NULL | 틀린 횟수 | `'틀린 횟수'` |
| `expires_at` | DATETIME(6) NOT NULL | 코드 만료 시각 (만든 뒤 10분) | `'코드 만료 시각'` |
| `verified_at` | DATETIME(6) NULL | 코드를 맞힌 시각 | `'코드를 맞힌 시각 (NULL: 안 맞힘)'` |
| `consumed_at` | DATETIME(6) NULL | 가입·재설정에 쓴 시각 | `'가입·재설정에 쓴 시각 (NULL: 안 씀)'` |
| `created_at` | DATETIME(6) NOT NULL | 보낸(요청한) 시각 | `'요청 시각'` |

- 인덱스 `idx_email_verification_email_hash_created_at (email_hash, created_at)`: 발송 제한 세기, 마지막 행 찾기
- 인덱스 `idx_email_verification_user_id (user_id)`: 회원 파기 때 지우기
- 인덱스 `idx_email_verification_expires_at (expires_at)`: 정리기
- 테이블 COMMENT `'이메일 인증 코드 (감사 열 없음: 요청마다 한 행, 1일 뒤 삭제)'`

### 상태

```text
PENDING ──맞힘──▶ VERIFIED ──가입·재설정──▶ CONSUMED
   │  └─5번 틀림─▶ LOCKED
   ├─새 코드 요청──▶ REVOKED   (같은 email_hash·purpose의 앞 행)
   └─발송 실패────▶ SEND_FAILED (발송 제한에 세지 않음)
VERIFIED도 새 코드 요청이면 REVOKED. 맞힌 뒤 30분이 지나면 쓸 수 없다(상태는 VERIFIED로 남고 정리기가 지운다)
```

- 확인할 수 있는 행: 그 `email_hash`·`purpose`의 가장 최근 행이 `PENDING`이고 `expires_at > now`
- 만료와 잠김은 행 상태로 구별해 다른 안내를 준다(US2-AC1·AC5). 메일을 보내지 않은 행도 같은 규칙이라 계정 유무가 드러나지 않는다 (FR-014)

## 로그인 실패 기록 — `tb_login_failure` (새 테이블, research R6)

엔터티 `LoginFailure`는 `@HardDelete`, 부모 없음. 회원과 잇지 않는다(없는 아이디도 센다).

| 열 | 타입 | 뜻 | COMMENT |
|---|---|---|---|
| `id` | BIGINT AUTO_INCREMENT PK | | — |
| `login_key` | VARCHAR(50) NOT NULL | 입력한 아이디 (공백 제거·소문자, 50자에서 자름) | `'입력한 로그인 아이디 (공백 제거·소문자)'` |
| `failed_count` | TINYINT NOT NULL | 연속 시도 횟수 (비밀번호를 보기 전에 센다) | `'연속 로그인 시도 횟수'` |
| `last_failed_at` | DATETIME(6) NOT NULL | 마지막 시도 시각 | `'마지막 시도 시각'` |
| `locked_until` | DATETIME(6) NULL | 임시 잠금이 풀리는 시각 | `'임시 잠금 풀리는 시각 (NULL: 안 잠김)'` |

- 유니크 `uk_login_failure_login_key (login_key)`
- 인덱스 `idx_login_failure_last_failed_at (last_failed_at)`: 정리기
- 테이블 COMMENT `'로그인 실패 기록 (감사 열 없음: 성공하면 삭제, 1일 뒤 삭제)'`

## 파기 이력 대상 — `tb_purge_log.target` (COMMENT만)

`MODIFY COLUMN target VARCHAR(30) NOT NULL COMMENT '파기 대상 종류 (LOGIN_HISTORY, NOTIFICATION, REFRESH_TOKEN, RICH_TEXT_IMAGE, MEMBER, INQUIRY, EMAIL_VERIFICATION, LOGIN_FAILURE)'`. 구현 직전 dev의 COMMENT를 보고 그 뒤에 더한다.

## 저장하지 않는 상태

| 상태 | 둘 곳 | 근거 |
|---|---|---|
| 인증된 가입(30분, 한 번) | 서명 쿠키 `email_signup` (`verificationId`, `email`) + 행 `VERIFIED`→`CONSUMED` | research R4 |
| 재설정 허락(30분, 한 번) | 서명 쿠키 `password_reset` (`verificationId`, `userId`) + 같은 행 상태 | research R4 |
| 보낼 메일 주소·코드 원문 | 커밋 뒤 비동기 이벤트의 메모리에만 | research R3·R5 |

## 보관과 파기 (`docs/privacy.md`)

| 정보 | 기간 | 끝나면 | 누가 |
|---|---|---|---|
| 회원 이메일 (`tb_user_email`) | 탈퇴 뒤 30일 (관리자 삭제 뒤 30일) | 행 삭제 | 018 `MemberPurgeService`에 더한다 |
| 이메일 점유 (`tb_email_claim`) | 쓰는 동안. 탈퇴 뒤 30일, 소셜 연결 해제 | 행 삭제 | 018 파기, 010 US5 연결 해제 |
| 이메일 인증 코드 (`tb_email_verification`) | 쓰거나 만료된 뒤 1일. 회원 파기 때 그 회원 것 | 행 삭제 | `EmailVerificationCleaner`, 018 파기 |
| 로그인 실패 기록 (`tb_login_failure`) | 로그인에 성공할 때, 마지막 실패 뒤 1일 | 행 삭제 | 로그인, `LoginFailureCleaner` |

`docs/privacy.md` 보관 기간 표의 019 네 줄이 이 표와 같다 (2026-11-28 plan D1·D5).

## 검증 규칙 요약

| 규칙 | 근거 |
|---|---|
| 아이디 `^[a-z][a-z0-9_]{3,19}$`(소문자로 바꾼 뒤), 예약 접두어 막기, 이미 쓰면 B508 | FR-003, US1-AC7·AC8 |
| 비밀번호 8~100자, 영문·숫자 각 1개 이상, 아이디와 다름, 확인 칸 일치(화면) | FR-004 |
| 이메일 정규화, 254자 이하, 형식(`@Email`) | FR-005 |
| 코드 6자리 숫자 | FR-007 |
| 닉네임은 009 `NicknamePolicy`, 약관은 010 현재 버전 | FR-009 |
