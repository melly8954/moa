---

description: "기본 회원가입·로그인 (basic-signup) 작업 목록"
---

# Tasks: 기본 회원가입·로그인 (basic-signup)

**Input**: `specs/019-basic-signup/`의 설계 문서

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/auth-api.md, contracts/screens.md, quickstart.md

**Tests**: 원칙 II에 따라 AC마다 테스트를 하나 둔다(US1~US6 AC 48개, 이름에 AC 번호). CLAUDE.md에 따라 기능 코드를 먼저 끝내고 테스트 작업은 맨 끝(Phase 10)에 둔다. 마지막에 백엔드 전체(`build`, `dbTest`)와 e2e 전체를 로컬에서 돌린다: [공용] 로그인 화면과 키트 `authenticate`를 바꾸기 때문이다 (2026-11-28 사용자 지시, CLAUDE.md "로컬 검사"의 예외)

## Format: `[ID] [P?] [Story?] Description`

- **[P]**: 병렬 가능 (다른 파일, 의존 없음)
- **[Story]**: US1 가입, US2 코드 제한, US3 재설정, US4 실패 잠금, US5 이메일 양방향 중복, US6 탈퇴·파기
- **[공용]**: 키트·공용 파일. 2026-11-28 사용자 승인 범위(더하기만, plan "다른 담당 코드")를 넘으면 멈추고 묻는다
- **[haily, 승인됨]**: haily 코드. 2026-11-28 사용자 승인(더하기만). 승인한 범위를 넘으면 멈추고 묻는다
- 백엔드 `backend/src/main/java/com/moa/`는 `B/`, 테스트 `backend/src/test/java/com/moa/`는 `BT/`, 프론트 `frontend/src/`는 `F/`, 마이그레이션 `backend/src/main/resources/db/migration/`은 `MIG/`로 줄여 쓴다

---

## Phase 1: Setup (확인)

**Purpose**: 백필과 번호가 기대는 것을 먼저 확인한다

- [ ] T001 **백필 전 겹침 세기·정규화 확인** (research R2 "마이그레이션 전 확인", quickstart 19). 조회만 한다. 로컬 DB(`.\gradlew.bat bootRun`으로 띄운 local MariaDB)에서 세 쿼리를 돌려 수를 적는다: (1) 겹침 `SELECT email, COUNT(DISTINCT user_id) FROM tb_user_social_account GROUP BY email COLLATE utf8mb4_bin HAVING COUNT(DISTINCT user_id) > 1`, (2) 정규화 안 된 값 `SELECT COUNT(*) FROM tb_user_social_account WHERE email COLLATE utf8mb4_bin <> LOWER(TRIM(email)) COLLATE utf8mb4_bin`, (3) ASCII 밖 값 `SELECT COUNT(*) FROM tb_user_social_account WHERE email REGEXP '[^ -~]'`. 또 `git fetch` 뒤 `origin/dev`에서 `tb_user_social_account`에 행을 넣는 코드가 여전히 `SocialAccountService.signUp` 하나이고 그 값이 `normalizeEmail`(`strip()`·`toLowerCase(Locale.ROOT)`)을 거치는지 다시 본다(2026-11-28 확인됨). 셋 중 하나라도 0이 아니거나 넣는 길이 늘었으면 **멈추고 보고한다**. 데이터가 있는 다른 환경이 있으면 사람이 같은 쿼리를 돌린다. 결과를 PR 설명에 적는다
- [ ] T002 구현 브랜치 `feat/019-basic-signup`을 최신 `dev`에서 딴 뒤 확인한다: MIG/의 마지막 번호(계획 때 `V14`), B/common/exception/ErrorCode.java에 `A501`~`A504`, `B501`~`B508`이 비었는지(research R12), `tb_purge_log.target`의 지금 COMMENT 값 목록(data-model "파기 이력 대상"), `PurgeTarget` 값, 010 US5(연결·해제)가 그사이 구현됐는지. 다르면 data-model·contracts·이 파일의 번호·값을 먼저 고친다
- [ ] T003 haily에게 알린다(승인은 2026-11-28 사용자가 함): 005 `ko.json` 실패 사유 한 줄, 013 `PurgeTarget` 두 값·COMMENT·화면 문구, 001 로그인 화면 링크 두 개, 002는 코드 변경 없음(관리자 계정에도 임시 잠금). 013 contracts·data-model 값 목록은 haily가 고친다 (plan "다른 담당 코드")

**Checkpoint**: 세 쿼리가 0이고, 번호·값이 계획과 같다

---

## Phase 2: Foundational (스키마, 설정, 공용 부품)

**Purpose**: 모든 스토리가 기대는 것. 이 단계 끝에 `.\gradlew.bat build`가 통과해야 한다

- [ ] T004 MIG/V<다음 번호>__basic_signup.sql (번호는 T002에서 본 마지막 + 1, PR 직전에 다시 본다). 머리 주석 `-- moa: 회원 이메일, 이메일 점유(소셜 이메일 백필), 이메일 인증 코드, 로그인 실패 잠금. 스펙 specs/019-basic-signup (FR-005~FR-008, FR-015, FR-021, research.md R1·R2·R3·R6)`. data-model.md 그대로:
  - `tb_user_email`: `user_id BIGINT NOT NULL` PK, `email VARCHAR(255) NOT NULL COLLATE utf8mb4_bin COMMENT '인증한 회원 이메일 (공백 제거·소문자; utf8mb4_bin: 점유 해시와 같은 바이트 기준)'`, `verified_at DATETIME(6) NOT NULL COMMENT '이메일 인증 시각'`, `created_at`·`updated_at DATETIME(6) NOT NULL`, `UNIQUE KEY uk_user_email_email (email)`, 테이블 COMMENT `'회원 이메일 (PK: 1:1 테이블; 가입 인증·비밀번호 재설정에만 씀)'`
  - `tb_email_claim`: `id` PK, `email_hash CHAR(64) NOT NULL COMMENT '이메일 해시 (SHA-256 소문자 16진; 공백 제거·소문자 이메일)'`, `user_id BIGINT NOT NULL COMMENT '점유한 회원 ID'`, `created_at DATETIME(6) NOT NULL COMMENT '점유한 시각'`, `UNIQUE KEY uk_email_claim_email_hash (email_hash)`, `KEY idx_email_claim_user_id (user_id)`, 테이블 COMMENT `'이메일 점유 (감사 열 없음: 가입·연결 때 넣고 파기·연결 해제 때 삭제)'`
  - 백필: `INSERT INTO tb_email_claim (email_hash, user_id, created_at) SELECT SHA2(email, 256), MIN(user_id), MIN(created_at) FROM tb_user_social_account GROUP BY email COLLATE utf8mb4_bin;` (`LOWER(TRIM())`으로 감싸지 않는다, research R2)
  - `tb_email_verification`: data-model 표의 열·타입·COMMENT 그대로(`email_hash CHAR(64)`, `purpose VARCHAR(20)` `'쓰임 (SIGNUP, PASSWORD_RESET)'`, `mail_kind VARCHAR(20)` `'보낸 메일 (CODE, ALREADY_REGISTERED, NONE)'`, `user_id BIGINT NULL`, `code_hash CHAR(64) NOT NULL`, `status VARCHAR(20)` `'상태 (PENDING, VERIFIED, CONSUMED, REVOKED, LOCKED, SEND_FAILED)'`, `failed_attempts TINYINT NOT NULL`, `expires_at`·`created_at DATETIME(6) NOT NULL`, `verified_at`·`consumed_at DATETIME(6) NULL`), 인덱스 `idx_email_verification_email_hash_created_at`, `idx_email_verification_user_id`, `idx_email_verification_expires_at`, 테이블 COMMENT `'이메일 인증 코드 (감사 열 없음: 요청마다 한 행, 1일 뒤 삭제)'`
  - `tb_login_failure`: `id` PK, `login_key VARCHAR(50) NOT NULL`, `failed_count TINYINT NOT NULL`, `last_failed_at DATETIME(6) NOT NULL`, `locked_until DATETIME(6) NULL COMMENT '임시 잠금 풀리는 시각 (NULL: 안 잠김)'`, `UNIQUE KEY uk_login_failure_login_key (login_key)`, `KEY idx_login_failure_last_failed_at (last_failed_at)`, 테이블 COMMENT `'로그인 실패 기록 (감사 열 없음: 성공하면 삭제, 1일 뒤 삭제)'`
  - [haily, 승인됨] `ALTER TABLE tb_purge_log MODIFY COLUMN target VARCHAR(30) NOT NULL COMMENT '<T002에서 본 값 목록>, EMAIL_VERIFICATION, LOGIN_FAILURE)'`
  - `DEFAULT`·외래 키 없음. 엔진·문자셋·정렬은 V1의 다른 테이블과 같다. 다 쓰면 `bash scripts/check-migration-numbers.sh`
- [ ] T005 [P] [공용] backend/build.gradle에 `implementation 'org.springframework.boot:spring-boot-starter-mail'` 한 줄 (더하기만)
- [ ] T006 [P] [공용] 설정 (더하기만, research R5 표): backend/src/main/resources/application.yaml에 `app.mail`(`sender`, `from`, `from-name`), `spring.mail`(`host` `${MAIL_HOST:smtp.gmail.com}`, `port` `${MAIL_PORT:587}`, `username`, `password`, `properties.mail.smtp.auth: true`, `starttls.enable: true`, 시간 제한 5초), `app.email-verification`(`code-ttl 10m`, `resend-interval 60s`, `hourly-limit 5`, `max-attempts 5`, `verified-ttl 30m`), `app.login-failure`(`max-attempts 5`, `lock-duration 15m`). **`app.mail.sender` 기본값: application-local.yaml·backend/src/test/resources/application-test.yaml은 `${MAIL_SENDER:fake}`, application-dev.yaml·application-prod.yaml은 `${MAIL_SENDER:off}`** — 새 설정 없이 dev를 받아도 돌아야 한다(꼭 지킬 것). backend/.env.example에 `MAIL_SENDER`, `MAIL_FROM`, `MAIL_FROM_NAME`, `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`(Gmail 앱 비밀번호, 값 없음), `EMAIL_CODE_*`, `LOGIN_FAILURE_*` 이름과 한 줄 설명
- [ ] T007 [P] B/common/exception/ErrorCode.java에 contracts/auth-api.md "오류 코드" 표의 12개(`A501`~`A504`, `B501`~`B508`, HTTP 상태 그대로)
- [ ] T008 [P] B/api/service/user/EmailNormalizer.java: 010 `SocialAccountService.normalizeEmail`을 그대로 옮긴다(`strip()` 뒤 `toLowerCase(Locale.ROOT)`, 비면 null, **NFC 없음**). `SocialAccountService`는 이것을 부르게 바꾼다(동작 그대로, 010 테스트가 기준). 해시 `sha256Hex(normalized)` = UTF-8 바이트 SHA-256 소문자 16진도 여기 둔다(점유 표용, MariaDB `SHA2`와 같은 값)
- [ ] T009 [P] B/api/service/auth/EmailHasher.java: HMAC-SHA256(키는 `JwtProperties.secret()`에서 용도 문자열 `moa:email-verification`으로 나눈 값) — `emailHash(normalized)`, `codeHash(verificationId, code)`. 6자리 코드 만들기는 `SecureRandom` (research R3)
- [ ] T010 [P] 엔터티·리포지토리 (data-model.md, `docs/db.md` "엔터티" — `@Table`을 늘 적고 인덱스는 엔터티에 적지 않는다): B/api/entity/user/UserEmail.java(`BaseTimeEntity`, `@HardDelete`), B/api/entity/user/EmailClaim.java(`@HardDelete`), B/api/entity/auth/EmailVerification.java(+ `VerificationPurpose`, `MailKind`, `VerificationStatus` enum), B/api/entity/auth/LoginFailure.java, 각각 B/api/repository/{jpa,reader,writer}/ 아래 Reader·Writer. `EmailVerificationReader.findLatestForUpdate(emailHash, purpose)`, `countSince(emailHash, since)`(`SEND_FAILED` 제외), `EmailClaimReader.findByEmailHash`, `UserEmailReader.findByEmail`
- [ ] T011 [P] [haily, 승인됨] B/common/purgelog/PurgeTarget.java에 `EMAIL_VERIFICATION`(이메일 인증 코드), `LOGIN_FAILURE`(로그인 실패 기록) 두 값 (더하기만)
- [ ] T012 메일 부품 (research R5): B/common/mail/AuthMailSender.java(`sendSignupCode(to, code)`, `sendAlreadyRegistered(to)`, `sendPasswordResetCode(to, code)`), AuthMailTemplates.java(제목·본문 한국어, 보내는 이름 `moa`, US2-AC7의 코드·10분·쓰임·"요청하지 않았다면 무시" / US5-AC6 안내 문구), AuthMailProperties.java(`@ConfigurationProperties("app.mail")` + `@Validated`), SmtpAuthMailSender.java(`JavaMailSender`), FakeAuthMailSender.java(메모리 우편함 최근 50통, 읽기·비우기), OffAuthMailSender.java(보내지 않음). `sender` 값으로 하나만 빈이 되게 한다. `smtp`인데 `from`·`username`·`password` 중 비면 경고 한 줄(값은 쓰지 않는다)과 함께 off로 동작한다. 로그에 이메일 원문·코드를 쓰지 않는다 (FR-020)
- [ ] T013 B/common/mail/AuthMailListener.java: `AuthMailRequested` 이벤트(받는 주소, 종류, 코드, verificationId — 메모리에만)를 `@TransactionalEventListener(AFTER_COMMIT)` + `@Async`로 받아 보낸다. 실패하면 그 행을 새 트랜잭션에서 `SEND_FAILED`로 바꾸고 건수·종류만 로그에 남긴다 (research R5)
- [ ] T014 B/api/controller/local/LocalMailOutboxController.java: `GET /api/local/mail-outbox` → `FakeAuthMailSender`의 우편함(받는 주소, 종류, 코드, 시각). **`@Profile("local")` + `@ConditionalOnBean(FakeAuthMailSender.class)`**, `@PermitAll`. 보안 설정에 `/api/local/**`를 따로 열지 않는다 (꼭 지킬 것, research R5)
- [ ] T015 B/common/security/email/EmailFlowCookies.java: `OAuthCookies`를 본뜬 서명 쿠키 두 개 (research R4). `email_signup`(`verificationId`, `email`, Path `/api/v1/auth/sign-up`), `password_reset`(`verificationId`, `userId`, Path `/api/v1/auth/password-reset`), 둘 다 HttpOnly·SameSite=Strict·30분(`app.email-verification.verified-ttl`), `iss` = `<JWT 발급자>:email`, `typ`로 용도 구별, `secure`는 `AuthProperties.refreshCookie().secure()`. 010 파일은 고치지 않는다
- [ ] T016 B/api/service/auth/EmailClaimService.java (research R2): `claim(userId, normalizedEmail)` — 그 해시 행이 이미 그 회원 것이면 아무것도 안 하고, 아니면 `saveAndFlush`(남의 것이면 `DataIntegrityViolationException` → 호출한 쪽이 B506·B507로 바꾼다), `ownerOf(normalizedEmail)`, `isMemberEmail(userId, normalizedEmail)`(B506·B507 가르기), `releaseIfUnused(userId, normalizedEmail)`(010 US5 해제용), `deleteAllOf(userId)`(018 파기용)
- [ ] T017 B/api/service/auth/EmailVerificationService.java (research R3, data-model "상태"): `request(purpose, email)` — 정규화 → 해시 → 그 해시의 마지막 행 `FOR UPDATE` → 60초 안이면 B504(`retryAfterSeconds`), 1시간에 5개면 B505 → 앞 행 `REVOKED` → 메일 종류 정하기(가입: 점유 표에 있으면 `ALREADY_REGISTERED`, 아니면 `CODE` / 재설정: R10 조건의 회원이면 `CODE`+`user_id`, 아니면 `NONE`) → 행 넣기(코드를 안 보내도 무작위 코드의 해시) → `AuthMailRequested` 발행(`NONE`이면 발행 안 함). 응답은 계정 유무와 상관없이 같다. `verify(purpose, email, code)` — 마지막 행이 `PENDING`·만료 전이 아니면 B502(만료)·B503(잠김)·B501, 틀리면 원자적 `failed_attempts + 1`(5면 `LOCKED`, B503), 맞으면 `VERIFIED`. `consume(verificationId, purpose)` — `UPDATE … SET status='CONSUMED', consumed_at=now WHERE id=? AND status='VERIFIED' AND verified_at > now - 30분` 1행이 아니면 false. 메일이 꺼졌으면(`off`) A504
- [ ] T018 [P] B/api/service/auth/LoginIdPolicy.java(`^[a-z][a-z0-9_]{3,19}$`를 소문자로 바꾼 뒤 검사, 예약 접두어 `admin`, `administrator`, `root`, `system`, `moa`, `operator`, `support`, `test` 막기), B/api/service/auth/PasswordPolicy.java(8~100자, 영문·숫자 각 1개 이상, 아이디와 같지 않음). V001 칸 오류 모양은 기존과 같다 (FR-003·FR-004)
- [ ] T019 B/api/service/user/MemberSignupSupport.java (research R9): `SocialAccountService`의 `requireCurrentTerms`·`saveProfile`(태그 고르기·`lastAttempt`)과 약관 동의 기록 저장을 옮긴다. `SocialAccountService`는 이것을 부른다. 010 동작은 그대로(010 테스트가 기준)
- [ ] T020 [P] [공용] frontend/messages/ko.json: 새 `emailAuth` 묶음(contracts/screens.md의 모든 문구), `errors`에 `A501`~`A504`·`B501`~`B508`. [haily, 승인됨] `loginHistory.failure.A501` = "임시 잠금", 파기 이력 대상 이름 `EMAIL_VERIFICATION` "이메일 인증 코드"·`LOGIN_FAILURE` "로그인 실패 기록" (더하기만)
- [ ] T021 [P] F/api/index.ts에 `api.v1.auth.emailSignup.{availability, requestCode, verifyCode, signUp}`, `api.v1.auth.passwordReset.{requestCode, verifyCode, reset}`, F/types/email-auth.ts(손으로 쓴 임시 타입, T056에서 생성 타입으로 바꾼다)

**Checkpoint**: `.\gradlew.bat build`(validate 포함 기존 테스트 통과)와 `bootRun`으로 Flyway·`ddl-auto: validate` 통과

---

## Phase 3: User Story 1 - 이메일을 인증하고 아이디·비밀번호로 가입한다 (Priority: P1) 🎯 MVP

**Goal**: 새 이메일로 코드를 받아 인증하고 계정 정보를 넣어 `USER`로 가입·로그인한다

**Independent Test**: quickstart 1~4

- [ ] T022 [US1] B/api/application/v1/auth/EmailSignupApplication.java (research R9): `signUp(cookie, command)` — 태그 충돌이면 새 트랜잭션으로 최대 `NicknameTags.MAX_ATTEMPTS`번. 한 트랜잭션에서: 쿠키 읽기(없으면 A502) → `consume`(false면 A502) → `LoginIdPolicy`·`PasswordPolicy`·닉네임 → 아이디 중복(`existsByLoginIdIncludingWithdrawn`, B508) → `User`(USER, bcrypt, `password_change_required` false) 저장 → `EmailClaimService.claim`(중복이면 롤백, B506·B507) → `UserEmail`(verified_at = 행의 verified_at) → `MemberSignupSupport`로 프로필·약관 → 토큰 발급, `LoginAttemptedEvent.succeeded` (FR-001·FR-006·FR-009·FR-010)
- [ ] T023 [US1] B/api/repository/jpa/user/UserRepository(또는 Reader)에 `existsByLoginIdIncludingWithdrawn(loginId)` native(`active_key = 1`인 행, 탈퇴 유예 포함) (research R8)
- [ ] T024 [US1] B/api/controller/v1/auth/ApiEmailSignupController.java (contracts/auth-api.md "가입"): `GET /sign-up/availability`, `POST /sign-up/email-codes`(202), `POST /sign-up/email-codes/verify`(200 + `email_signup` 쿠키), `POST /sign-up`(201, 010 sign-up과 같은 로그인 응답, 쿠키 지움). 모두 `@PermitAll`. 요청 DTO에 `@Email @Size(max=254)`, `@Pattern("\\d{6}")`
- [ ] T025 [P] [US1] F/components/auth/email-code-step.tsx: 이메일 → 코드 받기 → 6칸 코드, 남은 시간 10:00, 다시 받기(60초 꺼짐·남은 초), 늘 보이는 스팸함 안내, B501(남은 횟수)·B502·B503·B504·B505 안내. 가입·찾기가 함께 쓴다 (contracts/screens.md)
- [ ] T026 [US1] F/app/(public)/sign-up/page.tsx: `email-code-step`으로 인증 → `/sign-up/account`. `availability.emailSignupEnabled = false`면 안내만. 로그인한 사람이면 첫 화면으로 (FR-011, US1-AC12)
- [ ] T027 [US1] F/app/(public)/sign-up/account/page.tsx: 인증한 이메일(읽기만), 아이디·비밀번호·확인·닉네임(010 닉네임 칸 부품)·약관(`012-terms-privacy` FR-005 부품), 칸 아래 오류, A502면 안내와 `/sign-up`으로. 성공하면 010 가입과 같은 로그인 처리와 이동 (US1-AC3~AC10)
- [ ] T028 [US1] [공용] [haily, 승인됨] F/app/(public)/sign-in/page.tsx: 아이디·비밀번호 폼 아래 "회원가입"(→ `/sign-up`), "아이디·비밀번호 찾기"(→ `/sign-in/find`) 링크 두 개 (더하기만, US1-AC1)

**Checkpoint**: quickstart 1~4가 된다

---

## Phase 4: User Story 2 - 인증 코드의 만료·재발송·시도 제한 (Priority: P1)

**Goal**: 만료·간격·횟수·시도 제한이 화면과 메일에 맞게 보인다 (규칙 자체는 T017)

**Independent Test**: quickstart 7·8

- [ ] T029 [US2] T017의 제한이 contracts 오류(`retryAfterSeconds`, `remainingAttempts`)를 응답 본문에 담는지 맞춘다. B/common/exception의 오류 응답에 추가 필드를 담는 기존 방식이 없으면 `ServiceException` 상세 맵으로 담는다 (US2-AC1~AC6)
- [ ] T030 [P] [US2] AuthMailTemplates의 코드 메일 본문을 US2-AC7대로 다듬는다: 6자리 코드, "10분 안에 입력", 쓰임(가입/비밀번호 재설정), "요청하지 않았다면 이 메일을 무시해 주세요"
- [ ] T031 [US2] F/components/auth/email-code-step.tsx에서 서버 `retryAfterSeconds`로 다시 받기 남은 초를 맞추고, B502면 다시 받기 단추를 바로 켠다 (US2-AC1·AC2)

---

## Phase 5: User Story 3 - 이메일 코드로 비밀번호를 재설정한다 (Priority: P1)

**Goal**: 이메일만으로 코드를 받아 아이디를 보고 새 비밀번호를 정한다. 계정 유무가 드러나지 않는다

**Independent Test**: quickstart 9·10·12

- [ ] T032 [US3] B/api/application/v1/auth/PasswordResetApplication.java (research R10): 코드 확인이 맞으면 `password_reset` 쿠키 + `loginId`. `reset(cookie, newPassword)` — 쿠키(없으면 A503) → `consume`(false면 A503) → `PasswordPolicy` → `User.changePassword` → 비밀번호 변경 요구 해제 → `AuthTokenService.revokeAll(userId)` → `LoginFailureService.clear(loginId)` (T035 뒤에 잇는다). 메일 대상 판단(R10): `tb_user_email`에 있고, `User` 기본 조회로 찾히고(탈퇴·지움 아님), 비밀번호가 있고, 관리자 잠금이 아님 (FR-012~FR-014, US3-AC1~AC8)
- [ ] T033 [US3] B/api/controller/v1/auth/ApiPasswordResetController.java: `POST /password-reset/email-codes`(202, 계정 유무와 같은 응답), `POST /password-reset/email-codes/verify`(200 `{loginId}` + 쿠키), `POST /password-reset`(204, 쿠키 지움). `@PermitAll`
- [ ] T034 [US3] F/app/(public)/sign-in/find/page.tsx: `email-code-step`(안내 "가입된 이메일이면 코드를 보냈어요"), 늘 보이는 "소셜 계정으로 가입했다면 그 계정으로 로그인해 주세요", 2단계 "아이디는 {loginId}예요" + 새 비밀번호·확인 → `/sign-in?reset=done`. A503이면 처음부터. F/app/(public)/sign-in/page.tsx의 `reset=done` 안내 한 줄은 T028과 같은 파일에 더하기만 (US3)

---

## Phase 6: User Story 4 - 로그인 실패가 이어지면 잠시 잠긴다 (Priority: P1)

**Goal**: 같은 아이디로 연속 5번 틀리면 15분 잠긴다. 관리자·운영자도 같다. 없는 아이디도 같은 응답이다

**Independent Test**: quickstart 11·13·14·15

- [ ] T035 [US4] B/api/service/auth/LoginFailureService.java (research R6): 모두 `@Transactional(propagation = REQUIRES_NEW)`. `reserve(loginId)` — `login_key` = `strip().toLowerCase(Locale.ROOT)` 50자 자름, `INSERT … ON DUPLICATE KEY UPDATE` 뒤 `SELECT … FOR UPDATE`, `locked_until > now`면 A501(`lockedUntil`), `locked_until <= now`면 0으로, `failed_count + 1` 적고 커밋, 센 값 돌려줌. `onWrongPassword(loginKey, count)` — 5 이상이면 `locked_until = now + 15분` 적고 A501, 아니면 A006. `clear(loginId)` — 행 지움
- [ ] T036 [US4] [공용] B/api/service/user/UserService.java `authenticate` (research R7 표, "moa 019" 주석, 더하기만): 맨 앞에 `reserve` (A501이면 `LoginAttemptedEvent.failed(loginId, "A501")` 남기고 던짐), 비밀번호가 틀리면 `onWrongPassword`(접속 이력은 지금처럼 A006, 응답은 A501 또는 A006), 비밀번호가 맞으면 `clear` 뒤 지금의 관리자 잠금(A007) → 탈퇴(A401·A402) → 성공 순서를 그대로 둔다. 역할로 가르지 않는다(관리자 포함, FR-015)
- [ ] T037 [US4] contracts의 A501 응답에 `lockedUntil`을 담는다(T029와 같은 방식)
- [ ] T038 [US4] [공용] F/app/(public)/sign-in/page.tsx: A501이면 폼 위에 "로그인을 5번 실패해 {시각}까지 로그인할 수 없어요. 비밀번호를 잊었다면 찾기를 이용해 주세요" (더하기만, US4-AC2·AC3)

---

## Phase 7: User Story 5 - 이메일은 회원과 소셜 계정을 통틀어 하나만 쓴다 (Priority: P2)

**Goal**: 이메일 가입과 소셜 가입이 점유 표로 서로 막는다

**Independent Test**: quickstart 5·6·20·21

- [ ] T039 [US5] B/api/service/user/SocialAccountService.java (010, 본인): `checkSignUp`은 `tb_user_social_account.existsByEmail` 대신(또는 함께) `EmailClaimService.ownerOf`로 본다. `signUp`은 `saveSocialAccount` 전에 `EmailClaimService.claim(user.getId(), signup.email())`(중복 키면 지금의 `EMAIL_IN_USE` 길과 같은 안내). 010 FR-014 동작은 그대로(010 테스트가 기준). 010 US5(연결·해제)가 그사이 구현됐으면(T002) 연결 전 `claim`, 해제 뒤 `releaseIfUnused`를 더한다
- [ ] T040 [US5] T017·T022에서 B506·B507을 가른다: 점유 주인의 `tb_user_email.email`이 그 이메일이면 B506, 아니면 B507 (US5-AC5)
- [ ] T041 [P] [US5] F/app/(public)/sign-up/page.tsx: B506·B507이면 안내와 "아이디·비밀번호 찾기"·로그인 길 (US5-AC5)

---

## Phase 8: User Story 6 - 탈퇴와 파기에서 회원 이메일을 다룬다 (Priority: P2)

**Goal**: 유예 중에는 아이디·이메일이 붙잡히고, 파기하면 이메일·점유·코드가 지워진다

**Independent Test**: quickstart 16·17·18

- [ ] T042 [US6] B/api/repository/jpa/user/MemberPurgeRepository.java (018, 본인): `deleteUserEmail(userId)`(`delete from tb_user_email where user_id = :userId`), `deleteEmailClaims(userId)`, `deleteEmailVerifications(userId)`(`user_id`로) 세 native 쿼리. B/api/repository/writer/user/MemberPurgeWriter.java·B/api/service/user/MemberPurgeService.java가 지금 지우기 순서 안에서 부른다. 회원 이메일의 해시로 남은 가입 코드 행도 지운다(`email_hash`, 이메일 행을 지우기 전에 해시를 구함) (FR-022, US6-AC4)
- [ ] T043 [P] [US6] B/api/service/auth/EmailVerificationCleaner.java, B/api/service/auth/LoginFailureCleaner.java: `RefreshTokenCleaner`와 같은 방식(매일 새벽 `@Scheduled`, 여러 서버에서 같은 결과). 인증 코드는 `consumed_at` 또는 `expires_at`이 1일 지난 행, 실패 기록은 `last_failed_at`이 1일 지난 행을 지운다. 지운 건수를 `PurgeLogRecorder`로 `EMAIL_VERIFICATION`·`LOGIN_FAILURE`에 남긴다 (FR-021, research R11)
- [ ] T044 [US6] 로그 확인: 새로 쓴 로그 줄(메일 발송 실패, 정리기 건수, 설정 경고)에 이메일·코드·비밀번호가 들어가지 않는지 코드로 본다 (FR-020, US6-AC6)

---

## Phase 9: 문서

- [ ] T045 [P] specs/010-social-login/spec.md (본인): 배경 "첫 회원 가입 경로"를 "가입 경로는 소셜(010)과 아이디·비밀번호(019)"로, FR-014·가정 240줄에 "019 회원 이메일도 이메일 점유 표로 함께 막는다", FR-001에 "회원도 아이디·비밀번호 폼을 쓴다" (spec 019 영향 목록)
- [ ] T046 [P] specs/018-member-withdrawal/spec.md·data-model.md (본인): 파기 대상에 회원 이메일·이메일 점유·인증 코드를 더한다
- [ ] T047 [P] specs/README.md 019 상태(구현 중 → 구현됨), `docs/privacy.md` 019 네 줄의 상태 칸(구현됨, 정리기 이름)
- [ ] T048 PR 설명에 담당 알림 목록(spec 영향 목록: 002·005·012·013·001·008 haily·hyeonchang), T001 세 쿼리 결과, 백필 수(quickstart 20), 관리자 잠금 악용 한계와 후속(plan "한계와 후속")을 적는다

---

## Phase 10: 테스트 (PR 직전)

**Purpose**: 원칙 II. 모든 AC에 테스트 하나, 이름에 AC 번호. 기능 코드가 다 된 뒤에 쓴다

- [ ] T049 [P] BT/support/MailOutbox.java: 테스트에서 `FakeAuthMailSender` 우편함을 읽고 비우는 도우미. `AcceptanceTest` 사이에 비운다. 시각은 기존 `Clock` 바꾸기 도우미(018 테스트)를 쓴다
- [ ] T050 [P] BT/api/EmailSignupAcceptanceTest.java (`@Tag("db")`): US1-AC2~AC11, US2-AC1~AC7, US5-AC1·AC2·AC5·AC6 하나씩. `US2-AC3`는 가입·재설정 합산을 본다. 동시 가입(같은 이메일 두 요청, 이메일 가입 + 구글 가입)이 회원 하나만 만드는지 (SC-002)
- [ ] T051 [P] BT/api/PasswordResetAcceptanceTest.java (`@Tag("db")`): US3-AC1~AC8. 없는 이메일·소셜 이메일·관리자 잠금·탈퇴 유예 이메일의 응답이 있는 계정과 같은지(본문·상태), 메일이 가지 않는지
- [ ] T052 [P] BT/api/LoginLockAcceptanceTest.java (`@Tag("db")`): US4-AC1~AC9. 관리자 계정 5번 → 잠금(FR-015), 동시 10요청에서 비밀번호 확인이 5번을 넘지 않음(SC-003), 접속 이력 실패 코드 A006·A501, 임시 잠금 중 관리자 잠금 계정은 A501(D2), 018 탈퇴 회원 순서(비밀번호 맞힘 → A401)
- [ ] T053 [P] BT/api/SocialEmailClaimAcceptanceTest.java (`@Tag("db")`): US5-AC3·AC4(010 가입 길이 점유 표로 막힘, 본인이면 됨), 백필 해시 일치: 같은 문자열들(ASCII, 한글·악센트 포함)을 `SELECT SHA2(?, 256)`과 `EmailNormalizer.sha256Hex`로 만들어 같은지 (research R2)
- [ ] T054 [P] BT/api/MemberPurgeAcceptanceTest.java (018, 본인)에 US6-AC1~AC6을 더한다: 유예 중 아이디·이메일 막힘, 파기 뒤 `tb_user_email`·`tb_email_claim`·`tb_email_verification`에 그 회원 행 0, 다시 가입됨, 로그에 이메일·코드 없음(로그 잡기). 정리기 두 개의 1일 기준과 파기 이력 기록
- [ ] T055 [P] BT/common/mail/LocalMailOutboxProfileTest.java: `ApplicationContextRunner`로 `dev`·`prod`(`MAIL_SENDER` 기본 off) 문맥에 `LocalMailOutboxController` 빈이 없고, `local`(fake)에는 있음. 또 test 프로필 인수 테스트에서 `GET /api/local/mail-outbox`가 열리지 않음(꼭 지킬 것). 설정 없이 local·test 문맥이 뜨는지(`MAIL_SENDER` 기본 fake)
- [ ] T056 frontend/에서 `npm run gen:api`(사용자가 띄운 local 백엔드)로 F/types/schema.d.ts를 다시 만들고 T021의 손으로 쓴 타입을 바꾼다
- [ ] T057 [P] 프론트 단위 테스트(vitest): `email-code-step`(남은 시간, 다시 받기 60초, B501 남은 횟수, B502 단추 켜짐, 스팸함 안내), `/sign-up/account` 칸 오류·A502 이동, `/sign-in/find` 두 단계, 로그인 화면 링크 두 개·A501 안내·`reset=done`. 이름에 AC 번호(US1-AC1·AC12, US2-AC1·AC2, US3-AC3·AC7, US4-AC2·AC3)
- [ ] T058 [P] e2e(Playwright) 핵심 흐름 하나: 로그인 화면 → 회원가입 → (테스트 백엔드 우편함의 코드) → 계정 정보 → 로그인된 첫 화면 → 로그아웃 → 아이디·비밀번호 로그인 (원칙 II "핵심 흐름")
- [ ] T059 `.\gradlew.bat spotlessApply`, `.\gradlew.bat build`, frontend `npm run format`, `npm run check`
- [ ] T060 **백엔드 전체**: `.\gradlew.bat dbTest` 전체를 로컬에서 돌린다 (사용자 지시. 키트 `authenticate`를 바꿔 기존 로그인·010·018 테스트가 영향을 받는다). 실패하면 테스트를 고치지 않고 원인부터 본다 (원칙 V)
- [ ] T061 **e2e 전체**: push 전에 `npm run test:e2e` 전체를 로컬에서 돌린다 ([공용] 로그인 화면·`ko.json`을 바꿨기 때문, 메모리 "e2e before push")

---

## Phase 11: Polish

- [ ] T062 quickstart.md "수동 확인" 22개를 사람이 직접 한다. 실제 Gmail 발송은 `.env.local`에 `MAIL_SENDER=smtp`와 계정을 넣어 한 번 본다(값은 커밋하지 않는다)
- [ ] T063 PR 직전 dev를 병합하고 마이그레이션 번호를 마지막 + 1로 맞춘다(`bash scripts/check-migration-numbers.sh`). 바뀌었으면 T001 세 쿼리를 다시 돌린다

---

## Dependencies & Execution Order

- Phase 1 → Phase 2 → (Phase 3 US1 → Phase 4 US2) ∥ Phase 5 US3 ∥ Phase 6 US4 → Phase 7 US5 → Phase 8 US6 → Phase 9 → Phase 10 → Phase 11
- T001이 0이 아니면 멈춘다(백필 SQL이 앱 해시와 같다는 전제가 깨짐)
- T004(마이그레이션)가 모든 엔터티(T010)보다 먼저, T008(정규화) → T016·T017 → T022·T032
- T035 → T036(authenticate) → T032의 `clear` 연결
- T039(010)는 T016 뒤. T042(018)는 T010 뒤
- US2는 US1의 화면(T025)에 기댄다. US3·US4는 US1과 따로 만들 수 있다(Phase 2만 있으면 된다)

## Parallel Example

```text
Phase 2: T005, T006, T007, T008, T009, T010, T011, T018, T020, T021 함께
Phase 10: T049~T055, T057, T058 함께 (T056은 백엔드를 띄운 뒤)
```

## Implementation Strategy

- MVP: Phase 1~3 (US1 가입). 이것만으로 이메일 가입·로그인이 된다
- 다음: US4(실패 잠금)를 공개 가입과 같은 PR에 넣는다(spec US4 "공개 가입과 함께"). US2·US3·US5·US6도 같은 구현 PR이다
- 테스트는 맨 끝(Phase 10)에 모아 쓰고, 마지막에 백엔드 전체(dbTest)와 e2e 전체를 돌린다
