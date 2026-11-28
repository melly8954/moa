# Implementation Plan: 기본 회원가입·로그인 (basic-signup)

**Branch**: `docs/019-basic-signup-plan` (구현은 `feat/019-basic-signup`) | **Date**: 2026-11-28 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/019-basic-signup/spec.md`

## Summary

로그인하지 않은 사람이 이메일 6자리 코드로 인증한 뒤 아이디·비밀번호·닉네임·약관으로 `USER` 회원이 된다. 비밀번호를 잊으면 이메일 코드로 재설정한다. 아이디·비밀번호 로그인은 연속 5번 틀리면 15분 잠긴다(관리자·운영자 포함).
회원 이메일은 1:1 새 테이블 `tb_user_email`에 두고, 회원 이메일과 소셜 이메일을 통틀어 한 이메일은 한 회원만 쓰도록 이메일 점유 표 `tb_email_claim`(유니크)에 먼저 한 행을 넣는다. 인증 코드는 이메일·코드 모두 HMAC 해시로 `tb_email_verification`에 요청마다 한 행으로 둔다. 인증 뒤 30분 상태는 010처럼 서명 쿠키에 두고, 한 번만 쓰이게 DB 상태로 지킨다. 메일은 `AuthMailSender` 인터페이스 뒤에 Gmail SMTP와 가짜 발송기를 두고, 커밋 뒤 비동기로 보내 응답 시간이 계정 유무를 드러내지 않게 한다. 실패 잠금은 회원과 잇지 않은 `tb_login_failure`에 아이디별로 비밀번호를 보기 전에 센다. 자세한 결정은 [research.md](./research.md)다.

## Technical Context

**Language/Version**: Java 21 (Spring Boot 4.1.2), TypeScript (Next 16.3.5, React 19.2.8)

**Primary Dependencies**: 기존 골격(Spring MVC, Spring Security, JPA + QueryDSL, Flyway, JWT(jjwt)), 새 `spring-boot-starter-mail`(R5). 007 `AsyncConfig`(`@EnableAsync`), 009 `NicknamePolicy`·`NicknameTags`, 010 `SocialAccountService`·약관(`TermsProperties`)·`OAuthCookies` 방식, 013 `PurgeLogRecorder`, 018 `MemberPurgeService`·`WithdrawalProperties`, 012 약관 칸 부품

**Storage**: MariaDB 11.4. 한 마이그레이션 `V<다음 번호>__basic_signup.sql`: 새 테이블 4개(`tb_user_email`, `tb_email_claim` + 소셜 이메일 백필, `tb_email_verification`, `tb_login_failure`), `tb_purge_log.target` COMMENT. 번호는 PR 직전 dev 마지막 + 1, 018·017 마이그레이션 다음 ([data-model.md](./data-model.md))

**Testing**: 백엔드 인수 테스트(`@Tag("db")`, `dbTest`)는 가짜 발송기 우편함으로 코드를 읽는다. 시각은 기존 `Clock` 바꾸기 방식(018 테스트)으로 만료·잠금 풀림을 본다. 프론트 단위 테스트(vitest)는 단계 화면·남은 시간·단추 상태. e2e는 가입 → 로그인 한 흐름만(원칙 II "핵심 흐름")

**Target Platform**: 웹(백엔드 서버 + Next). 메일은 Gmail SMTP

**Project Type**: 웹 서비스 (`backend/` + `frontend/`)

**Performance Goals**: 코드 받기·재설정 요청의 응답은 메일 발송을 기다리지 않는다(커밋 뒤 비동기). 메일 도착 1분 안 (SC-001, Gmail에 기댐)

**Constraints**: 시크릿(Gmail 앱 비밀번호)은 `.env.<프로필>`·환경 변수만 (원칙 VI). 로그·기록에 이메일 원문·코드·비밀번호 없음 (FR-020). 가입·재설정·로그인 응답으로 계정 유무가 드러나지 않음 (SC-004). Gmail 하루 발송 한도 (spec 가정). `MAIL_SENDER` 기본값은 local·test `fake`, dev·prod `off`라 새 설정 없이 dev를 받아도 돈다(꼭 지킬 것). `/api/local/mail-outbox`는 local 프로필에서만 등록하고 dev·prod 문맥에 없음을 테스트로 본다(꼭 지킬 것, research R5)

**Scale/Scope**: 새 API 7개 + 로컬 전용 1개, 기존 로그인 API 오류 1개 추가, 새 화면 3개(`/sign-up`, `/sign-up/account`, `/sign-in/find`) + 로그인 화면 수정, 새 테이블 4개, 정리기 2개

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| 원칙 | 확인 | 결과 |
|---|---|---|
| I. 스펙이 먼저다 | spec.md에 `[NEEDS CLARIFICATION]` 0개. plan에서 정한 값은 research에 적었다. D1~D3에 따라 spec을 먼저 고쳤다(2026-11-28) | 통과 |
| II. AC마다 테스트 하나 | US1~US6 AC 48개를 인수 테스트(API)와 프론트 단위 테스트로 나눠 이름에 AC 번호. 메일 내용(US2-AC7)·로그(US6-AC6)는 가짜 발송기·로그 잡기로 본다 | 통과 |
| III. 키트를 따른다 | 키트 파일은 `UserService.authenticate`(실패 잠금 두 줄), `build.gradle`(메일 의존성), `application.yaml`·`.env.example`(설정), `/sign-in` 화면(링크)만 고친다. 모두 [공용]으로 표시하고 "moa 019" 주석. `tb_user`·`User`는 고치지 않는다(R1) | 통과 |
| IV. 기존 기능은 제자리에서 고친다 | 010(소셜 가입의 회원 이메일 확인, 가입 공용 부분 옮기기), 018(파기 대상), 005(실패 사유 문구), 013(파기 대상 종류)은 그 기능의 코드를 고친다. 남의 스펙 문서는 고치지 않고 담당에게 알린다 (아래 "다른 담당 코드") | 통과 |
| V. 테스트를 고쳐서 통과시키지 않는다 | `AuthLoginAcceptanceTest`(키트)·010·018 테스트는 그대로 통과해야 한다. 실패 잠금 때문에 같은 아이디로 5번 넘게 틀리는 기존 테스트가 있으면 테스트 사이에 `tb_login_failure`를 비운다(`DatabaseCleaner`가 이미 모든 테이블을 비움). 기대값을 바꿔야 하면 spec부터 본다 | 통과 |
| VI. 시크릿을 다루지 않는다 | `MAIL_PASSWORD`는 이름만 `.env.example`에 둔다. 값은 읽지도 출력하지도 않는다 | 통과 |
| 기술 제약: 목록 종류 | 새 목록 없음 (로컬 우편함은 최근 50통의 개발용 응답) | 해당 없음 |
| 기술 제약: `docs/db.md` | 새 테이블 이름·PK(1:1은 `user_id`)·COMMENT·인덱스 이름·`DEFAULT` 없음을 data-model에 맞췄다 | 통과 |

Phase 1 뒤 다시 봄: 계약·데이터 모델이 위 판단을 바꾸지 않는다.

## Project Structure

### Documentation (this feature)

```text
specs/019-basic-signup/
├── spec.md
├── plan.md              # 이 파일
├── research.md          # R1~R13
├── data-model.md        # tb_user_email, tb_email_verification, tb_login_failure, 상태, 보관
├── quickstart.md        # 수동 확인 22개
├── contracts/
│   ├── auth-api.md      # 가입·재설정 API, 로그인 A501, 오류 코드
│   └── screens.md       # /sign-up, /sign-up/account, /sign-in/find, /sign-in 링크
├── checklists/requirements.md
└── tasks.md             # /speckit-tasks
```

### Source Code (repository root)

```text
backend/
├── build.gradle                                       # [공용] spring-boot-starter-mail
├── .env.example                                       # [공용] MAIL_*, EMAIL_CODE_*, LOGIN_FAILURE_*
└── src/main/
    ├── resources/
    │   ├── application.yaml                           # [공용] app.mail, app.email-verification, app.login-failure, spring.mail
    │   └── db/migration/V<다음 번호>__basic_signup.sql
    └── java/com/moa/
        ├── api/controller/v1/auth/
        │   ├── ApiEmailSignupController.java          # sign-up/availability, email-codes, verify, sign-up
        │   └── ApiPasswordResetController.java        # password-reset/email-codes, verify, password-reset
        ├── api/controller/local/LocalMailOutboxController.java  # local 프로필만
        ├── api/application/v1/auth/
        │   ├── EmailSignupApplication.java            # 태그 충돌 다시 시도, 토큰 발급 (R9)
        │   └── PasswordResetApplication.java
        ├── api/service/auth/
        │   ├── EmailVerificationService.java          # 코드 만들기·확인·발송 제한·한 번 쓰기 (R3)
        │   ├── EmailClaimService.java                 # 이메일 점유 넣기·찾기·지우기 (R2)
        │   ├── LoginFailureService.java               # REQUIRES_NEW 시도 세기·지우기 (R6)
        │   ├── LoginIdPolicy.java, PasswordPolicy.java
        │   └── EmailHasher.java                       # HMAC (R3)
        ├── api/service/user/
        │   ├── MemberSignupSupport.java               # 010에서 옮긴 약관·프로필 (R9)
        │   ├── EmailNormalizer.java                   # 010 normalizeEmail을 옮김 (strip·소문자, NFC 없음, R2)
        │   ├── SocialAccountService.java              # 010: 회원 이메일도 확인 (R2)
        │   ├── UserService.java                       # [공용] 키트 authenticate에 실패 잠금 (R7)
        │   └── MemberPurgeService.java                # 018: 이메일·코드 파기 (R11)
        ├── api/entity/auth/ EmailVerification, LoginFailure (+ enum)
        ├── api/entity/user/UserEmail.java, EmailClaim.java
        ├── api/repository/{jpa,reader,writer}/…       # 위 엔터티, MemberPurgeRepository에 두 줄
        ├── common/mail/
        │   ├── AuthMailSender.java, AuthMailTemplates.java, AuthMailProperties.java
        │   ├── SmtpAuthMailSender.java, FakeAuthMailSender.java
        │   └── AuthMailListener.java                  # AFTER_COMMIT + @Async (R5)
        ├── common/security/email/EmailFlowCookies.java # email_signup, password_reset (R4)
        ├── common/purgelog/PurgeTarget.java           # 013: EMAIL_VERIFICATION, LOGIN_FAILURE
        └── (api/service/auth/) EmailVerificationCleaner.java, LoginFailureCleaner.java  # RefreshTokenCleaner와 같은 방식
backend/src/test/java/com/moa/
├── api/EmailSignupAcceptanceTest.java, PasswordResetAcceptanceTest.java, LoginLockAcceptanceTest.java
├── common/mail/LocalMailOutboxProfileTest.java      # local에만 우편함 빈, dev·prod·test에는 없음 (R5)
└── support/MailOutbox.java                            # FakeAuthMailSender 읽기

frontend/
├── messages/ko.json                                   # [공용] emailAuth 묶음, errors A501~B508, loginHistory.failure.A501
└── src/
    ├── app/(public)/sign-in/page.tsx                  # [공용] 링크 두 개, A501 안내, reset=done
    ├── app/(public)/sign-in/find/page.tsx             # 새
    ├── app/(public)/sign-up/page.tsx                  # 새 (1단계)
    ├── app/(public)/sign-up/account/page.tsx          # 새 (2단계)
    ├── components/auth/email-code-step.tsx            # 가입·찾기가 함께 쓰는 이메일→코드 부품
    ├── api/index.ts                                   # [공용] api.v1.auth.emailSignup·passwordReset
    └── types/schema.d.ts                              # npm run gen:api
```

**Structure Decision**: 기존 `com.moa.api` 계층(controller → application → service → repository)을 따른다. 메일은 다른 기능도 쓸 수 있게 `common/mail`에 두되 이번에는 인증 메일만 만든다. 화면은 010의 `(public)` 그룹 옆에 둔다.

## 다른 담당 코드

남의 스펙 문서는 고치지 않는다 (spec "다른 스펙·문서에 줄 영향"). 아래 "승인"은 **더하기만** 하는 변경이다. 있는 줄을 바꾸거나 지우게 되면 다시 묻는다.

| 담당 | 무엇 | 파일 | 상태 |
|---|---|---|---|
| haily (005 접속 이력) | 실패 사유 문구 한 줄 `loginHistory.failure.A501` = "임시 잠금". 화면 코드는 고치지 않는다(문구 키로 보인다) | `frontend/messages/ko.json` ([공용]) | 승인 (2026-11-28 사용자 승인) |
| haily (013 파기 이력) | `PurgeTarget`에 `EMAIL_VERIFICATION`·`LOGIN_FAILURE` 두 값, `tb_purge_log.target` COMMENT(019 마이그레이션), 화면의 대상 이름 문구 | `common/purgelog/PurgeTarget.java`, 019 마이그레이션, `ko.json` | 승인 (2026-11-28 사용자 승인). 013 contracts·data-model의 값 목록은 haily에게 알린다 |
| haily (001 메뉴·셸) | 로그인 화면에 링크 두 개("회원가입", "아이디·비밀번호 찾기") | `frontend/src/app/(public)/sign-in/page.tsx` ([공용]) | 승인 (2026-11-28 사용자 승인) |
| 키트 (psw-dev-kit) | `UserService.authenticate`에 실패 잠금 두 줄(R7), `build.gradle`에 `spring-boot-starter-mail` | `UserService.java`, `build.gradle` | 승인 (2026-11-28 사용자 승인). 키트로 올릴지는 나중에 정한다 |
| 키트 (설정) | `application.yaml`·프로필 yaml의 `app.mail`·`app.email-verification`·`app.login-failure`·`spring.mail`, `.env.example`의 이름 | `application*.yaml`, `.env.example` ([공용]) | 승인 (2026-11-28 사용자 승인, 더하기만) |
| haily (002 사용자 관리) | 코드 변경 없음. 관리자 잠금과 임시 잠금이 따로이고(FR-016) 관리자 계정에도 임시 잠금이 걸린다는 것만 알린다 | — | 알림만 |
| jeonggi4007 (010, 본인) | `checkSignUp`이 점유 표를 본다, `signUp`이 소셜 연결 전에 점유를 넣는다, 가입 공용 부분을 `MemberSignupSupport`로 옮긴다. 010 US5(연결·해제)는 구현할 때 점유를 넣고 지운다 | `SocialAccountService.java` | 본인. 010 spec FR-014·배경 문구도 고친다 |
| jeonggi4007 (018, 본인) | 파기 때 회원 이메일·이메일 점유·인증 코드 삭제 | `MemberPurgeRepository`·`MemberPurgeWriter`·`MemberPurgeService` | 본인 |

## 정한 것 (2026-11-28 사용자 결정)

| # | 무엇 | 결정 | 고친 곳 |
|---|---|---|---|
| D1 | 회원 이메일 위치 (research R1) | 1:1 `tb_user_email` | spec FR-021·FR-022·Key Entities, `docs/privacy.md` 019 줄 |
| D2 | 임시 잠금과 관리자 잠금이 함께일 때 (research R7) | 임시 잠금 안내가 먼저. 비밀번호를 맞힌 뒤에 관리자 잠금 안내 | spec Edge Cases |
| D3 | 재설정의 다른 기기 로그인 끊기 | 끊는다. FR-013의 "지금 비밀번호 변경과 같다"만 지웠다 | spec FR-013 |
| D4 | 키트 비밀번호 변경 규칙을 019 규칙에 맞추는가 | 이번에는 맞추지 않는다 | 아래 "후속" |
| D5 | 회원 이메일·소셜 이메일 동시 가입 (research R2) | 막는다. `GET_LOCK` 대신 이메일 점유 표 `tb_email_claim`(B, 2026-11-28 확정, 비교는 research R2). 백필 해시는 010 정규화 그대로, NFC 없음 | spec FR-021·Key Entities, `docs/privacy.md` 점유 줄, data-model, research R2·R9·R13 |

## 한계와 후속

- **관리자 잠금 악용 (한계, 꼭 적을 것)**: 관리자·운영자 아이디를 알면 누구나 일부러 5번 틀려 그 계정을 15분 잠글 수 있고, 계속 틀리면 계속 잠긴다. 관리 계정은 회원 이메일이 없어 재설정으로 풀 수 없다. 이번에는 받아들인다 (research R6)
  - 후속: IP 기준 시도 제한(아이디 잠금과 별개로 한 IP의 실패를 센다), 관리 계정의 로그인 허용 IP·경로 제한, 캡차, 2단계 인증
- **키트 비밀번호 변경 규칙 (D4)**: `/account/password`(키트)는 지금 8~100자만 본다. 019 규칙(영문·숫자 각 1개, 아이디와 다름)에 맞출지는 후속으로 정한다. 맞추면 관리자 계정에도 걸린다
- **가입 이메일 바꾸기, 기존 비밀번호 회원·소셜 회원의 이메일 등록**: spec 가정대로 이번 범위 밖
- **키트 메일 헬퍼**: 팀에 물었다. 키트에 있으면 `AuthMailSender` 구현을 그쪽으로 바꾼다 (spec 가정)

## Complexity Tracking

| 늘어난 것 | 왜 필요한가 | 더 단순한 안을 버린 이유 |
|---|---|---|
| 메일을 보내지 않는 요청도 인증 코드 행을 넣는다 | 만료·5번 잠김·발송 제한이 계정 유무와 상관없이 같아야 한다 (FR-014, SC-004) | 행을 넣지 않으면 "코드가 맞지 않음"과 "만료"의 차이로 계정 유무가 드러난다 |
| 시도를 비밀번호 확인 전에 새 트랜잭션으로 센다 | 동시 요청으로 15분에 5번을 넘기지 못하게 하고, 로그인 예외의 롤백에도 남게 한다 (SC-003) | 틀린 뒤에 세면 동시 요청으로 더 시도할 수 있고, 같은 트랜잭션이면 예외로 롤백된다 |
| 이메일 점유 표와 소셜 이메일 백필 | 두 테이블에 걸친 이메일 유일성을 DB 제약으로 지킨다 (SC-002) | 확인만 하면 틈이 있고, `GET_LOCK`은 커넥션에 붙어 풀로 돌아간 연결에 남을 위험이 있다 (research R2) |
