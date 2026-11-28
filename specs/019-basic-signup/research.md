# Research: 기본 회원가입·로그인 (019-basic-signup)

2026-11-28 dev(7fc348f)의 코드를 읽고 정했다. "(잠정)"은 구현 전에 바꿀 수 있는 값이다.
번호가 붙은 결정 중 사용자 확인이 필요한 것은 plan.md "결정이 필요한 것"에 모았다.

## R1. 회원 이메일을 둘 곳 (FR-002, FR-005, FR-022, FR-023)

- **결정 (권장)**: 1:1 새 테이블 `tb_user_email` (PK `user_id`, `email` 유니크, `verified_at`). 키트 `tb_user`는 고치지 않는다
- **비교**

  | 기준 | A. `tb_user.email` 칸 추가 ([공용] 키트 테이블) | B. 1:1 `tb_user_email` (권장) |
  |---|---|---|
  | `docs/db.md` | 칸 추가는 된다. 기존 행이 있어 `NULL` 허용. 유니크는 `tb_user`의 `active_key`로 `uk_user_email_active_key (email, active_key)` | 1:1 테이블 PK 규칙(`user_id`, COMMENT `PK: 1:1 테이블`)에 맞는다. 유니크는 `uk_user_email_email` 하나 |
  | 유예 중 붙잡기 (FR-023) | 유예 중에는 `deleted_at`이 비어 `active_key`=1이라 붙잡힌다 (018 research R1과 같은 원리) | 행이 파기 때까지 남아 붙잡힌다 |
  | 관리자가 지운 회원 | `deleted_at`이 차서 `active_key`가 NULL → 이메일이 **바로 풀린다**. 행에는 개인정보가 30일 남는데 같은 이메일로 다른 사람이 가입할 수 있다 | 파기(삭제 뒤 30일, 018 FR-019) 때 행을 지울 때까지 붙잡힌다. 로그인 아이디와 달리 풀리지 않는다 |
  | 018 파기 | `MemberPurgeRepository.erase`의 UPDATE에 `email = null` 한 줄 | `deleteUserEmail` 한 줄 (게임 정보·프로필과 같은 "행 삭제") |
  | 조회 숨김 | `User`의 `@SQLRestriction`(탈퇴·지움 숨김)을 타므로 중복 확인은 native로 따로 써야 한다 | 엔터티 `UserEmail`에 숨김이 없어 그대로 찾는다. 탈퇴 여부는 필요할 때 `User` 기본 조회로 본다 |
  | 키트 영향 | 키트 엔터티 `User`와 user-admin 응답이 칸을 알게 된다 (원칙 III, [공용]) | 키트 파일을 고치지 않는다 |
  | 쓰임 분리 (FR-002) | 회원 정보와 섞여 다른 기능이 쓰기 쉽다 | 가입 인증·재설정 전용이 테이블로 드러난다. 010이 소셜 이메일을 `tb_user_social_account`에 따로 둔 것과 같은 모양 |
- **이유**: 관리자 삭제 회원의 이메일이 바로 풀리지 않고, 키트 테이블을 건드리지 않으며, 파기가 "행 삭제"로 단순하다. 009(`tb_user_profile`)·010(`tb_user_social_account`)이 회원 부가 정보를 1:1·1:N 테이블로 둔 선례와 맞다
- **영향**: spec FR-021·FR-022·Key Entities와 `docs/privacy.md`의 019 줄을 "행 삭제"로 고쳤다 (2026-11-28 사용자 결정, plan D1)

## R2. 소셜 이메일과의 양방향 중복과 동시 가입 (FR-005, US5-AC5, SC-002)

회원 이메일(`tb_user_email`)과 소셜 이메일(`tb_user_social_account.email`)은 테이블이 달라 유니크 하나로 막을 수 없다. 소셜 이메일은 같은 회원의 두 제공자가 같은 이메일을 쓸 수 있어 유니크도 없다(010). 확인만 하고 만들면 아주 짧은 틈에 한 이메일로 회원이 둘 생길 수 있다(SC-002). 두 안을 비교했다 (2026-11-28 사용자 요청, plan D5).

- **결정 (권장)**: B. 이메일 점유 표 `tb_email_claim`
- **비교**

  | 기준 | A. MariaDB `GET_LOCK` | B. 이메일 점유 표 (권장) |
  |---|---|---|
  | 막는 방법 | 같은 이메일의 두 가입이 이름 잠금 `moa:email:<해시>`를 차례로 잡고, 잠금 안에서 두 테이블을 확인한 뒤 만든다 | 회원 이메일·소셜 연결을 만들기 전에 같은 트랜잭션에서 점유 행을 먼저 넣는다. `uk_email_claim_email_hash`가 둘째를 막는다(중복 키 → B506·B507, 010은 `emailInUse`) |
  | 커넥션 풀 위험 | 이름 잠금은 트랜잭션이 아니라 **커넥션**에 붙는다. 풀로 돌아간 커넥션에 잠금이 남으면 그 커넥션을 다시 쓰는 요청까지 붙잡고, 같은 이메일 가입은 커넥션이 닫힐 때까지 막힌다. 롤백·예외·타임아웃 길마다 `RELEASE_LOCK`을 빠짐없이 불러야 한다 | 커넥션 상태가 없다. 트랜잭션이 롤백되면 점유도 사라진다 |
  | 서버 여러 대·DB 재시작 | 같은 DB면 된다. 읽기 복제본으로 가면 깨진다 | 유니크 제약이라 늘 된다 |
  | "이미 쓰는 이메일" 찾기 | 두 테이블을 본다 | 점유 표 하나를 본다(해시 유니크 조회). 회원 이메일인지(B506) 소셜인지(B507)는 그 회원의 `tb_user_email`을 보고 가른다 |
  | 018 유예 중 붙잡기 | 회원 이메일·소셜 연결 행이 남아 붙잡힌다 | 점유 행도 파기 때까지 남아 붙잡힌다. 관리자가 지운 회원도 파기 때까지 붙잡힌다 |
  | 018 파기 | 바꿀 것 없음 | `MemberPurgeRepository`에 `deleteEmailClaims(userId)` 한 줄 |
  | 010 변경 | `signUp`과 연결에 잠금을 잡는 줄 | `checkSignUp`이 점유 표를 본다. `signUp`이 소셜 연결 저장 전에 점유를 넣는다. US5(연결·해제, 아직 구현 전)는 연결 전에 점유를 넣거나 내 것인지 보고, 해제 때 그 회원이 그 이메일을 더 쓰지 않으면 점유를 지운다 |
  | 기존 데이터 | 없음 | 백필이 필요하다 (아래) |
  | 새 개인정보 | 없음 | 이메일 해시 행 (보관 기간 표에 한 줄) |

- **이유**: 커넥션 풀에서 이름 잠금이 엉뚱한 연결에 남는 위험(사용자 지적)을 없애고, 유일성을 DB 제약으로 지킨다. 찾기도 표 하나로 단순해진다. 대신 010에 한 줄씩 더하고 백필 한 번이 든다
- **`tb_email_claim`**: `email_hash`(SHA-256, 정규화 이메일, 유니크), `user_id`, `created_at`. 이메일 하나에 한 행이고, 한 회원이 같은 이메일을 회원 이메일·구글·디스코드에 함께 써도 한 행이다(넣기 전에 그 해시의 행이 내 것이면 넣지 않는다). 키 없는 SHA-256을 쓰는 이유는 마이그레이션 SQL(`SHA2(email, 256)`)로 백필해야 해서다. 같은 이메일 원문이 `tb_user_social_account`에 이미 있으므로 키 없는 해시가 더 드러내는 것은 없다. 인증 코드 표(R3)는 원문이 없는 표라 키 있는 HMAC을 그대로 쓴다
- **백필** (019 마이그레이션 안)
  - 소셜 이메일: `INSERT INTO tb_email_claim (email_hash, user_id, created_at) SELECT SHA2(email, 256), MIN(user_id), MIN(created_at) FROM tb_user_social_account GROUP BY email COLLATE utf8mb4_bin`. 010 FR-014가 겹침을 막아 왔으므로 이메일마다 회원은 하나여야 한다. 여럿이면 가장 먼저 가입한 회원이 점유하고, 나머지는 그대로 로그인되지만 그 이메일을 다시 점유할 수 없다. 겹침이 있는지는 마이그레이션 전에 세어 PR에 적는다 (tasks 첫 작업, quickstart 19)
  - `COLLATE utf8mb4_bin`인 이유: 열의 정렬 규칙 `utf8mb4_unicode_ci`는 대소문자뿐 아니라 악센트도 무시해 `café@x`와 `cafe@x`를 한 묶음으로 센다. 그러면 한 이메일의 점유가 빠진다. 해시는 바이트로 가르므로 묶음도 바이트로 가른다

### 백필 해시와 앱 해시가 같은 값인가 (2026-11-28 사용자 확인 요청)

- **010 코드 확인 (dev 7fc348f)**: `tb_user_social_account.email`을 쓰는 곳은 `SocialAccountService.signUp`의 `new UserSocialAccount(…, signup.email(), …)` 하나다. 그 값은 `checkSignUp`이 `normalizeEmail(identity.email())`로 만든 것을 `oauth_signup` 쿠키로 넘긴 것이다(`SocialLoginApplication` `ALLOWED` 가지). `normalizeEmail`은 `strip()` 뒤 `toLowerCase(Locale.ROOT)`이고 비면 NULL이다. **NFC는 하지 않는다**(NFC는 닉네임 `NicknamePolicy`만 한다). 마이그레이션·시드·로컬 데이터는 이 표에 행을 넣지 않는다. 연결(010 US5)은 아직 없다. 그러므로 저장된 값은 모두 이미 010 정규화를 거친 값이다
- **결정**: 019의 이메일 정규화는 010의 `normalizeEmail`을 **그대로** 쓴다(공용 `EmailNormalizer`로 옮기고 010도 그것을 부른다). NFC를 더하지 않는다. 백필 SQL은 저장된 값을 그대로 `SHA2(email, 256)`에 넣고 `LOWER(TRIM())`으로 감싸지 않는다
  - 감싸지 않는 이유: 저장 값은 이미 정규화돼 있어 감싸도 같아야 한다. 오히려 SQL `TRIM`은 공백만, Java `strip()`은 유니코드 공백 모두를 지우고, `LOWER`는 정렬 규칙의 대소문자 표를 써서 ASCII 밖에서 Java `Locale.ROOT`와 다를 수 있다. 같은 함수로 이미 만든 값을 다른 함수로 다시 바꾸면 어긋날 길만 생긴다
  - 앱 해시: `SHA-256(UTF-8 바이트(EmailNormalizer.normalize(입력)))`의 소문자 16진. MariaDB `SHA2(str, 256)`는 그 열의 문자셋(utf8mb4 = UTF-8) 바이트를 해시하고 소문자 16진을 돌려주므로 같은 값이다. 이것을 인수 테스트로 확인한다: 같은 문자열(ASCII, 대문자 없는 한글·악센트 포함)을 `SELECT SHA2(?, 256)`과 앱 함수로 만들어 같은지 본다 (tasks)
  - NFC 한계: 정규화에 NFC가 없어 분해형(NFD)과 완성형(NFC)으로 쓴 같은 모양의 이메일은 다른 이메일로 본다. 구글·디스코드가 주는 이메일과 회원이 쓰는 이메일은 사실상 ASCII라 영향은 거의 없다. 나중에 NFC를 더하면 그때 저장 값·점유 해시를 다시 만드는 마이그레이션이 필요하다(앱 쪽 다시 쓰기). 그래서 지금 더하지 않고 두 경로가 같은 함수를 쓰게 묶는 것을 우선한다
- **마이그레이션 전 확인 (tasks 첫 작업)**: 로컬과 데이터가 있는 환경에서 세어 PR에 적는다
  - 겹침: `SELECT email, COUNT(DISTINCT user_id) FROM tb_user_social_account GROUP BY email COLLATE utf8mb4_bin HAVING COUNT(DISTINCT user_id) > 1` → 0이어야 한다
  - 정규화 안 된 값: `SELECT COUNT(*) FROM tb_user_social_account WHERE email COLLATE utf8mb4_bin <> LOWER(TRIM(email)) COLLATE utf8mb4_bin` → 0이어야 한다 (ASCII 범위의 확인. 0이 아니면 백필 전에 멈추고 보고한다)
  - ASCII 밖 이메일: `SELECT COUNT(*) FROM tb_user_social_account WHERE email REGEXP '[^ -~]'` → 0이 아니면 그 행들은 NFC·대소문자 차이를 사람이 본다
- **비교한 안: 앱 쪽 백필** (마이그레이션 뒤 시작 작업이 Java로 해시를 만들어 넣기)
  - 좋은 점: 앱과 백필이 반드시 같은 함수다. 나중에 정규화를 바꿔도 다시 돌리기 쉽다
  - 나쁜 점: 웹 서버가 이미 요청을 받는 동안 돌아 그사이 가입이 점유를 건너뛸 수 있다. 서버마다 시작 때마다 돌아 멱등·동시 실행을 따로 다뤄야 한다. 데이터 변경이 Flyway 기록 밖에 있다. 테스트(`DatabaseCleaner`)·시드 되돌리기와 엇갈린다
  - **고르지 않았다**: 저장 값이 이미 같은 함수로 정규화된 것을 010 코드로 확인했으므로 SQL 백필도 같은 값을 만든다. 위 확인 쿼리와 해시 일치 테스트가 그것을 지킨다
- 회원 이메일 열 `tb_user_email.email`도 같은 이유로 `COLLATE utf8mb4_bin`이다(COMMENT에 이유). 유일성의 기준은 점유 표의 해시(바이트)이고, 두 표가 같은 기준을 써야 한다
  - 회원 이메일: 019 전에는 없으므로 백필이 없다
  - 파기된 회원의 소셜 연결은 018이 이미 지웠으므로 들어가지 않는다. 유예 중 회원의 연결은 들어가 붙잡힌다
- **A를 고른다면**: 잠금을 잡는 코드와 푸는 코드를 같은 `@Transactional` 메서드 안의 `try/finally`에 두고 `JdbcTemplate`(트랜잭션에 묶인 같은 커넥션)으로 부른다. `TransactionSynchronization.afterCompletion`은 커넥션을 돌려준 뒤일 수 있어 쓰지 않는다. 그래도 커넥션이 끊긴 뒤의 재시도·드라이버 재연결 같은 길은 남는다
- **잠금이 더는 필요 없다**: 코드 받기의 동시 요청(R3)도 점유 표가 아니라 이메일 해시의 마지막 인증 행을 `SELECT … FOR UPDATE`로 잡는다. 행이 없을 때의 첫 요청끼리는 둘 다 들어갈 수 있지만, 발송 제한이 하나 더 세어질 뿐 회원이 생기지는 않는다

## R3. 인증 코드·재설정 코드 저장 (FR-007, FR-008, FR-014)

- **결정**: 테이블 `tb_email_verification` 하나에 가입·재설정을 함께 둔다 (`purpose`)
  - 이메일은 원문 대신 `email_hash`(HMAC-SHA256, 키는 `JWT_SECRET`에서 용도 문자열로 나눈 값)로 둔다. 보낼 주소는 메모리의 이벤트로만 넘긴다. 코드 확인 요청은 이메일을 다시 받아 해시로 찾는다
  - 코드는 `code_hash`(HMAC-SHA256, 같은 키 + 행 id)로 둔다. 6자리는 경우의 수가 100만이라 키 없는 해시는 DB가 새면 바로 풀린다. bcrypt는 필요 없다(시도 5번 제한이 막는다)
  - 요청마다 행을 하나 넣는다. 그 이메일·쓰임의 앞 행은 `REVOKED`로 바꾼다 (US2-AC4)
  - 메일을 보내지 않는 요청도 행을 넣는다: 재설정의 없는 계정, 가입의 이미 쓰는 이메일(안내 메일). 이때 `code_hash`는 아무도 모르는 무작위 코드의 해시다. 그래서 만료·5번 잠김·발송 제한이 계정 유무와 상관없이 똑같이 동작한다 (FR-014, SC-004)
  - 행 열: `email_hash`, `purpose`(SIGNUP, PASSWORD_RESET), `mail_kind`(CODE, ALREADY_REGISTERED, NONE), `user_id`(재설정 대상, NULL 가능), `code_hash`, `status`(PENDING, VERIFIED, CONSUMED, REVOKED, LOCKED, SEND_FAILED), `failed_attempts`, `expires_at`, `verified_at`, `consumed_at`, `created_at`
  - 틀린 시도: `failed_attempts + 1`을 원자적 UPDATE로 하고 5가 되면 `LOCKED` (US2-AC5)
  - 발송 제한: 같은 `email_hash`의 행 중 `SEND_FAILED`가 아닌 것을 센다. 마지막 행이 60초 안이면 B504, 1시간 안에 5개면 B505. 가입·재설정을 합친다 (US2-AC3)
  - 같은 이메일로 동시에 두 요청: 그 `email_hash`의 마지막 행을 `SELECT … FOR UPDATE`로 잡고 센다. 행이 하나도 없을 때의 첫 요청끼리는 둘 다 들어갈 수 있으나 발송이 한 번 더 될 뿐이다 (R2 끝)
- **정리**: 쓰거나(CONSUMED) 만료된 뒤 1일 지난 행을 매일 새벽 정리기가 지운다. 파기 이력 대상 `EMAIL_VERIFICATION` (R11)
- **대안**: 원문 이메일 저장(개인정보가 1일 더 남음), 메모리·캐시 저장(서버 여러 대·재시작에 약하고 moa에 캐시가 없다)

## R4. 가입 진행 상태와 재설정 상태 (FR-006, FR-013, US1-AC10·AC11, US3-AC7)

- **결정**: 010의 `oauth_signup`과 같은 서명 HttpOnly 쿠키 + DB 한 번 쓰기
  - `email_signup` 쿠키: JWT 서명(키는 `JWT_SECRET`, `iss` = `<발급자>:email`, `typ` = `email_signup`), 내용은 `verificationId`, `email`(정규화), 30분, SameSite=Strict, Path=`/api/v1/auth/sign-up`
  - `password_reset` 쿠키: 같은 방식, 내용은 `verificationId`, `userId`, Path=`/api/v1/auth/password-reset`
  - 한 번 쓰기: 가입·재설정을 마칠 때 `UPDATE tb_email_verification SET status = 'CONSUMED' … WHERE id = ? AND status = 'VERIFIED' AND verified_at > now - 30분`이 1행일 때만 진행한다. 0행이면 A502·A503
  - 쿠키는 `OAuthCookies`를 본뜬 새 클래스 `EmailFlowCookies`로 둔다(010 파일은 고치지 않는다)
- **이유**: 010과 같은 방식(서버 세션 없음, 서명으로 위조 막기)이고, 010에는 없던 "한 번만"은 DB 상태로 지킨다
- **대안**: 응답 본문의 토큰을 프론트가 들고 다니기(XSS에 드러남), 서버 세션(moa는 세션이 없다)

## R5. 메일 발송 구조 (FR-018~FR-020)

- **결정**
  - 인터페이스 `AuthMailSender` (`sendSignupCode(to, code)`, `sendAlreadyRegistered(to)`, `sendPasswordResetCode(to, code)`). 메일 문안은 구현 밖의 `AuthMailTemplates`에 둔다
  - `SmtpAuthMailSender`: `spring-boot-starter-mail`의 `JavaMailSender`로 Gmail SMTP(`smtp.gmail.com:587`, STARTTLS, 앱 비밀번호)
  - `FakeAuthMailSender`: 보내지 않고 메모리 우편함에 쌓는다(최근 50통). 자동 테스트가 읽는다. local 프로필은 `GET /api/local/mail-outbox`로 개발자가 코드를 본다. 로그에는 쓰지 않는다 (FR-019·FR-020)
    - 우편함 컨트롤러는 `@Profile("local")`로만 등록한다(꼭 지킬 것, 2026-11-28 사용자). `@ConditionalOnBean(FakeAuthMailSender)`를 함께 걸어 local이라도 smtp로 띄우면 없다. 경로 `/api/local/**`는 보안 설정에 따로 열지 않고 컨트롤러의 `@PermitAll`로만 연다(등록되지 않은 프로필에서는 열 것이 없다)
    - 확인 테스트: (1) test 프로필 인수 테스트가 `GET /api/local/mail-outbox`에 404(또는 401, 등록 안 됨)를 받는다, (2) `ApplicationContextRunner`로 `dev`·`prod` 프로필 문맥에 우편함 컨트롤러 빈이 없고, `local`에는 있음을 본다. dev·prod 문맥은 DB 없이 메일 설정 빈만 올려 본다
  - 고르기: `app.mail.sender` = `smtp` | `fake` | `off`
    - 기본값: `application-local.yaml`·`application-test.yaml`은 `${MAIL_SENDER:fake}`, `application-dev.yaml`·`application-prod.yaml`은 `${MAIL_SENDER:off}`. **새 설정 없이 dev를 받아 local로 띄워도, 테스트를 돌려도 그대로 돈다**(꼭 지킬 것). `.env.local`에 아무것도 더하지 않아도 된다
    - `smtp`인데 `MAIL_FROM`·`MAIL_USERNAME`·`MAIL_PASSWORD` 중 하나라도 비면 시작을 막지 않고 `off`처럼 동작하며 경고 한 줄(값은 쓰지 않는다). FR-018 "설정 없으면 기능을 켜지 않는다"
    - `off`면 `AuthMailSender` 빈이 아무것도 보내지 않는 구현이고, `availability.emailSignupEnabled = false`, 코드 받기 API는 A504
  - 설정값 이름 (`application.yaml`에서만 환경 변수를 읽는다)

    | 설정 | 환경 변수 | 기본값 |
    |---|---|---|
    | `app.mail.sender` | `MAIL_SENDER` | local·test `fake`, dev·prod `off` |
    | `app.mail.from` | `MAIL_FROM` | (없음. smtp면 필수) |
    | `app.mail.from-name` | `MAIL_FROM_NAME` | `moa` |
    | `spring.mail.host` / `port` | `MAIL_HOST` / `MAIL_PORT` | `smtp.gmail.com` / `587` |
    | `spring.mail.username` / `password` | `MAIL_USERNAME` / `MAIL_PASSWORD` (Gmail 앱 비밀번호) | (없음) |
    | `app.email-verification.*` | `EMAIL_CODE_TTL`, `EMAIL_CODE_RESEND_INTERVAL`, `EMAIL_CODE_HOURLY_LIMIT`, `EMAIL_CODE_MAX_ATTEMPTS`, `EMAIL_VERIFIED_TTL` | `10m`, `60s`, `5`, `5`, `30m` |
    | `app.login-failure.*` | `LOGIN_FAILURE_MAX_ATTEMPTS`, `LOGIN_FAILURE_LOCK_DURATION` | `5`, `15m` |
  - `spring.mail.properties.mail.smtp.connectiontimeout`·`timeout`·`writetimeout` = 5초
- **응답 시간을 같게 하는 법**: 요청은 행을 넣고 커밋한 뒤 곧바로 같은 응답을 준다. 메일은 `@TransactionalEventListener(AFTER_COMMIT)` + `@Async`(007이 켠 `AsyncConfig`)에서 보낸다. 메일을 보내지 않는 경우(재설정의 없는 계정)는 이벤트가 없을 뿐 요청 안에서 하는 일(조회·행 넣기)은 같다. 실패하면 행을 `SEND_FAILED`로 바꿔 발송 제한에 세지 않고 건수만 로그에 남긴다
- **대안**: 요청 안에서 동기 발송(SMTP가 1~3초라 계정 유무가 응답 시간으로 드러남), 발송 대기 테이블 + 정리기(메일 한 종류에 과함). 키트 메일 헬퍼가 생기면 `AuthMailSender` 구현만 바꾼다 (spec 가정)

## R6. 로그인 실패 잠금 저장 (FR-015~FR-017, 관리자 포함)

- **결정**: 테이블 `tb_login_failure` (PK `id`, 유니크 `login_key`). 회원 행과 잇지 않아 없는 아이디도 센다
  - 열: `login_key`(입력 아이디를 `strip().toLowerCase()`, 50자), `failed_count`, `last_failed_at`, `locked_until`(NULL: 안 잠김)
  - 순서는 "시도 먼저 세기": 비밀번호를 보기 전에 짧은 새 트랜잭션(`REQUIRES_NEW`)에서 그 `login_key` 행을 잠그고(`INSERT … ON DUPLICATE KEY UPDATE` 뒤 `SELECT … FOR UPDATE`)
    1. `locked_until > now`면 A501 (비밀번호를 보지 않는다)
    2. `locked_until <= now`면 0으로 되돌린다
    3. `failed_count + 1`을 적고 커밋한다 (이 시도를 미리 센다)
  - 비밀번호가 맞으면 그 행을 지운다(새 트랜잭션). 틀렸고 센 값이 5면 `locked_until = now + 15분`을 적고 A501, 5 미만이면 A006
  - 미리 세는 이유: 동시에 여러 요청을 보내도 15분에 5번을 넘지 못한다 (SC-003). 바깥 트랜잭션이 예외로 롤백돼도 센 값이 남는다
  - 관리자·운영자 계정도 같은 길이다(spec FR-015 결정). 키트 `authenticate`는 역할을 가리지 않으므로 따로 할 것이 없다
- **정리**: 마지막 실패 뒤 1일 지난 행을 매일 새벽 지운다. 파기 이력 대상 `LOGIN_FAILURE` (R11)
- **한계 (꼭 적을 것, 2026-11-28 사용자)**: 아이디만 알면 누구나 일부러 5번 틀려 그 계정을 15분 잠글 수 있다. 관리자·운영자 아이디도 같다. 관리 계정은 회원 이메일이 없어 재설정으로 바로 풀 수도 없으므로 15분을 기다려야 하고, 계속 틀리면 계속 잠긴다. 소셜 로그인은 막히지 않지만 관리 계정은 소셜을 쓰지 않는다. 이번에는 받아들이고 후속으로 둔다: IP 기준 시도 제한(아이디 잠금과 별개로 한 IP의 실패를 센다), 관리 계정 로그인 경로·허용 IP 제한, 캡차, 2단계 인증 (plan "후속")
- **대안**: `tb_user`에 실패 칸(없는 아이디를 셀 수 없어 FR-015 위반), 접속 이력(`tb_login_history`)을 세기(365일 보관 표를 매 로그인 훑음, 성공 뒤 0으로 되돌리기가 어렵다)

## R7. 키트 `UserService.authenticate` 변경 범위와 018 순서 (FR-015, FR-016, 018 research R5)

- **결정**: `authenticate`에 실패 잠금 두 줄(앞의 "시도 세기", 뒤의 "성공 지우기·실패 잠그기")만 더한다. 018이 넣은 탈퇴 확인은 그대로 비밀번호 확인 뒤에 둔다

  | 순서 | 지금 (018 뒤) | 019 뒤 | 실패 코드 (응답 / 접속 이력) |
  |---|---|---|---|
  | 1 | — | 아이디 정규화, 시도 세기. 잠금 중이면 막음 | A501 / A501 |
  | 2 | 비밀번호 확인 (없는 아이디는 더미 해시) | 같음 | 틀리면 A006 / A006. 다섯째면 A501 / A006 |
  | 3 | — | 비밀번호가 맞으면 실패 기록을 지운다 | — |
  | 4 | 관리자 잠금 | 같음 | A007 / A007 |
  | 5 | 탈퇴 유예 중 / 파기 대기 | 같음 | A401 / A401, A402 / A402 |
  | 6 | 성공 | 같음 | — |

  - 3이 4보다 앞인 이유: 비밀번호를 맞혔으면 비밀번호 맞히기 공격이 아니다. 관리자 잠금·탈퇴 회원도 실패 횟수는 0이 된다
  - 018과의 관계: 탈퇴 여부는 비밀번호를 맞힌 사람에게만 알린다는 018 규칙이 그대로다. 탈퇴 회원도 틀리면 똑같이 세고 잠긴다 (spec Edge Cases)
  - 잠금 중에는 비밀번호를 보지 않으므로 관리자 잠금 안내(A007)를 보일 수 없다. spec Edge Cases의 "관리자 잠금 안내가 먼저"를 "임시 잠금 안내가 먼저, 비밀번호를 맞힌 뒤 관리자 잠금 안내"로 고친다 (plan D2)
  - 아이디 정규화: 로그인 조회는 지금처럼 `findByLoginId`(정렬 규칙 `utf8mb4_unicode_ci`라 대소문자를 가리지 않는다). 실패 키만 소문자로 바꾼다
- **[공용]**: `UserService`는 키트에서 왔고 018이 이미 moa 줄을 더했다. 같은 방식으로 "moa 019" 주석을 단다

## R8. 아이디·비밀번호 규칙 (FR-003, FR-004)

- **결정**: 가입 요청 DTO의 `@Pattern("^[a-z][a-z0-9_]{3,19}$")`(소문자로 바꾼 뒤 검사), 예약 접두어 목록은 `LoginIdPolicy` 상수. 비밀번호는 `PasswordPolicy`(8~100자, 영문·숫자 각 1개 이상, 아이디와 같지 않음)를 가입·재설정에서 쓴다
- 이미 쓰는 아이디: `uk_user_login_id_active_key`가 막고(정렬 규칙상 대소문자 무시), 서비스가 먼저 새 조회 `existsByLoginIdIncludingWithdrawn`(탈퇴 포함 native, `active_key`=1인 행)으로 B508을 준다. 탈퇴 유예 회원도 `active_key`=1이라 막힌다 (FR-023, 018 research R1)
- 010의 무작위 소셜 아이디(소문자·숫자 12자)와 겹칠 수 있으나 유니크가 막고 사람에게는 "이미 쓰는 아이디"로만 보인다
- 지금 비밀번호 변경(`/account/password`, 키트)의 규칙은 이번에 바꾸지 않는다 (plan D4)

## R9. 가입·약관·닉네임 재사용 (FR-009, FR-010)

- **결정**: `SocialAccountService`의 `requireCurrentTerms`·`saveProfile`(태그 다시 시도 포함)을 `MemberSignupSupport`로 옮겨 두 가입이 함께 쓴다. 010 동작은 그대로다(010 테스트가 기준)
- 가입 흐름: `EmailSignupApplication.signUp`이 `SocialLoginApplication.signUp`처럼 태그 충돌 때 새 트랜잭션으로 최대 3번(`NicknameTags.MAX_ATTEMPTS`) 다시 한다. 한 트랜잭션에서: 쿠키 확인 → 한 번 쓰기(R4) → 아이디 중복(B508) → `User`(USER, 비밀번호 해시) 저장 → 이메일 점유 `saveAndFlush`(중복 키면 B506·B507, 트랜잭션 전체 롤백, R2) → `UserEmail` → 프로필(닉네임·태그) → 약관 동의 기록. 롤백되면 한 번 쓰기도 되돌아가 다시 시도할 수 있다
- 성공하면 010과 같이 토큰을 주고 `LoginAttemptedEvent.succeeded`를 남긴다
- 약관 칸 모양은 `012-terms-privacy` FR-005의 공용 부품을 쓴다

## R10. 비밀번호 재설정 (FR-012~FR-014)

- **결정**: 코드 확인이 맞으면 `password_reset` 쿠키와 `loginId`를 준다. 저장 때: 쿠키 → 한 번 쓰기 → `PasswordPolicy` → `User.changePassword` → 비밀번호 변경 요구 해제 → `AuthTokenService.revokeAll(userId)` → 실패 기록 삭제(R6)
- 메일 보낼 대상: `tb_user_email`에 있고, `User` 기본 조회(탈퇴·지움 숨김)로 찾히고, 비밀번호가 있고, 관리자 잠금이 아닌 회원. 아니면 `mail_kind` = NONE 행만 넣는다 (US3-AC2·AC8)
- 지금 비밀번호 변경은 다른 기기 로그인을 끊지 않는다(키트 `changePassword` 주석). 재설정은 끊는다. spec FR-013의 "지금 비밀번호 변경과 같다"는 틀린 말이라 지운다 (plan D3)

## R11. 정리기와 파기 이력 (FR-021, `docs/privacy.md` "실행"·"파기 이력")

- **결정**: `EmailVerificationCleaner`, `LoginFailureCleaner`를 기존 정리기(`RefreshTokenCleaner` 등)와 같은 방식으로 둔다(매일 새벽, 여러 서버에서 같은 결과). 지운 건수를 013 `PurgeLogRecorder`로 남긴다. `PurgeTarget`에 `EMAIL_VERIFICATION`, `LOGIN_FAILURE`를 더하고 `tb_purge_log.target` COMMENT를 같은 마이그레이션에서 고친다 (017이 `INQUIRY`를 더한 방식)
- 회원 파기(018): `MemberPurgeRepository`에 `deleteUserEmail(userId)`, `deleteEmailClaims(userId)`, `deleteEmailVerifications(userId, emailHash)`를 더하고 `MemberPurgeWriter`·`MemberPurgeService`가 부른다 (FR-022)

## R12. 오류 코드 (contracts)

- 019는 `A501`~ `B501`~를 쓴다 (009 `2xx`, 014 `3xx`, 018 `4xx` 다음). 구현 직전에 `ErrorCode.java`에서 비었는지 다시 본다

## R13. 마이그레이션

- 파일 하나 `V<다음 번호>__basic_signup.sql`: `tb_user_email`, `tb_email_claim`과 소셜 이메일 백필(R2), `tb_email_verification`, `tb_login_failure`, `tb_purge_log.target` COMMENT. 번호는 PR 직전 dev 마지막 + 1 (`docs/db.md` "번호 겹침"). 순서 조건: 018 `V13__member_withdrawal.sql`(탈퇴 칸)과 017 `V14__customer_center.sql`(`INQUIRY` 대상) 다음
