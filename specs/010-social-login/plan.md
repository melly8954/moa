# Implementation Plan: 소셜 로그인 (social-login)

**Branch**: `feat/010-social-login` (문서는 `docs/010-google-login`) | **Date**: 2026-11-17 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/010-social-login/spec.md`

## Summary

moa의 첫 회원 가입 경로로 구글·디스코드 소셜 로그인을 붙인다. 팀 결정(2026-11-17)에 따라 두 제공자 모두 서버 경유 OAuth 인가 코드 방식(PKCE)을 쓴다. 프론트는 백엔드 주소로 이동만 하고, 백엔드가 제공자 콜백을 받아 회원을 찾거나(로그인) 닉네임 입력·약관 동의 뒤 만들고(가입), 기존 로그인과 같은 moa JWT·리프레시 쿠키를 발급한다. 가입과 연결 전에 이메일 미인증, 재가입 제한, 이메일 중복을 확인해 막는다. 로그인한 회원은 계정 설정에서 같은 흐름으로 제공자마다 연결·해제한다. 회원 정보는 잠정 B(키트 `users` 그대로 + `user_social_accounts`)로 두고, 약관 동의 기록과 재가입 제한 기록 테이블을 더한다. 제공자와 주고받는 부분은 키트 oauth 헬퍼로 바꿀 수 있게 경계를 나눈다 (팀 확인 후 바뀔 수 있음, [research.md](./research.md) R2).

## Technical Context

**Language/Version**: 백엔드 Java 21, Spring Boot 4.1. 프론트 TypeScript, Next 16, React 19

**Primary Dependencies**: 기존 Spring Security·jjwt·Spring Data JPA·Flyway. 제공자 호출은 Spring `RestClient`(새 의존성 없음). 재가입 제한 해시는 JDK `Mac`(HmacSHA256). 키트 oauth 헬퍼 — 적용 기록에 없음, 팀 확인 중 (R2). 프론트는 기존 api 클라이언트, react-hook-form·zod, next-intl 문구

**Storage**: MariaDB. 새 테이블 3개 `user_social_accounts`, `user_terms_agreements`, `social_rejoin_blocks` (한 마이그레이션 `V2_7__social_login.sql`). state·가입 대기는 서명 쿠키 (DB 아님)

**Testing**: 인수 테스트 `AcceptanceTest` 상속(`@Tag("db")`, `dbTest`, AC 번호 이름), 제공자 클라이언트는 제공자마다 테스트용 가짜. 재가입 제한 행은 테스트가 직접 넣는다. 프론트 단위 테스트(`npm run check`), e2e는 US1만

**Target Platform**: 웹 (앱 클라이언트는 나중, spec 가정)

**Project Type**: 웹 서비스 모노레포 (`backend/`, `frontend/`)

**Performance Goals**: SC-001 처음 가입 1분 안(닉네임·약관 포함), SC-002 다시 로그인은 단추 한 번 + 계정 고르기. 서버 쪽 추가 지연은 토큰 교환 한 번(디스코드는 사용자 조회 한 번 더)

**Constraints**: 제공자 토큰·client secret·재가입 해시 비밀 값은 백엔드 환경 값에만 (원칙 VI). 공용 `SecurityConfig` 수정 없음 — 콜백·약관 조회는 `PublicPaths` 빈. 다른 스펙 파일 수정 없음

**Scale/Scope**: 제공자 2개(구글, 디스코드), 새 API 7개, 새 화면 2개 + 기존 화면 3곳 수정, 새 테이블 3개

## Constitution Check

*GATE: Phase 0 전 확인, Phase 1 뒤 다시 확인.*

| 원칙 | 확인 | 결과 |
|---|---|---|
| I. 스펙이 먼저다 | 계획의 동작은 spec FR·AC에 있다. 스펙 미정 6곳은 추측으로 확정하지 않고 자리표시·현재 동작 유지로 두었다 (research "남은 스펙 미정") | 통과 (자리표시는 확정 뒤 바꿈) |
| II. AC마다 테스트 하나 | US1~US5 AC 전부(US1-AC1~AC12, US5-AC1~AC10 포함)를 인수 테스트로, 이름에 AC 번호. 제공자는 가짜 | 통과 |
| III. 키트를 따른다 | 기존 확장점(`PublicPaths`, `AuthTokenApplication.issue`, `LoginAttemptedEvent`)을 쓴다. 헬퍼가 확인되면 제공자 클라이언트를 헬퍼로 바꾸고 스펙 출처에 적는다. 키트 user-admin·rbac 서비스와 로그인 화면을 고치는 것은 moa에서 먼저 하고 키트로 옮길지 팀과 정한다 | 통과 (헬퍼 확인 대기) |
| IV. 제자리에서 고친다 | 002·003·008·README 경계 갱신은 spec "나중에 고칠 곳"에 목록만, 이번에 다른 스펙 파일을 고치지 않음 | 통과 |
| V. 테스트를 고쳐서 통과시키지 않는다 | 해당 없음 (계획 단계) | 통과 |
| VI. 시크릿 | client secret·제공자 토큰·`SOCIAL_REJOIN_HASH_SECRET`은 백엔드 환경 값에만. `.env.*` 값은 다루지 않고 `.env.example`에 이름만 | 통과 |
| 목록 종류 | 새 목록 화면 없음 (연결 상태는 제공자마다 한 줄) | 해당 없음 |

Phase 1 뒤 재확인: data-model·contracts가 위 표와 어긋나지 않는다. 위반 없음 → Complexity Tracking 비움.

## Project Structure

### Documentation (this feature)

```text
specs/010-social-login/
├── spec.md
├── plan.md              # 이 파일
├── research.md          # R1 서버 경유 결정 ~ R13 재가입 제한
├── data-model.md        # users(변경 없음) + user_social_accounts(잠정 B) + 약관 동의 + 재가입 제한, 서명 쿠키
├── quickstart.md        # 수동·자동 확인 순서
├── contracts/
│   ├── auth-oauth.md            # providers, terms, authorize, callback, sign-up, link, unlink, 오류 코드
│   └── users-me-and-screens.md  # /users/me 추가 칸, 역할 막기, 화면
├── checklists/requirements.md
└── tasks.md
```

### Source Code (repository root)

```text
backend/src/main/java/com/moa/
├── api/controller/v1/auth/          # OAuth 컨트롤러 (providers, authorize, callback, sign-up)
├── api/controller/v1/terms/         # 현재 약관 버전 조회
├── api/controller/v1/user/          # 내 외부 계정 link·unlink ({provider})
├── api/application/v1/auth/         # 소셜 로그인 흐름 → AuthTokenApplication.issue 재사용
├── api/entity/user/                 # UserSocialAccount, SocialProvider, UserTermsAgreement, TermsType, SocialRejoinBlock
├── api/repository/{jpa,reader,writer}/user/
├── api/service/user/                # 소셜 계정 서비스 (찾기·가입·연결·해제, 가입·연결 확인, 관리자 제한)
├── api/service/user/UserAdminService.java, api/service/role/RoleUserService.java  # 소셜 회원 역할 막기 추가
├── common/security/oauth/           # 제공자 클라이언트 경계(구글, 디스코드), state·가입 대기 서명 쿠키, 설정, 재가입 해시, PublicPaths 빈
│                                    #   ← 키트 oauth 헬퍼로 바뀔 수 있는 부분 (R2)
└── common/exception/ErrorCode.java  # A008~A017
backend/src/main/resources/
├── application.yaml                 # app.oauth.google.*, app.oauth.discord.*, app.oauth.rejoin-hash-secret, app.terms.*
└── db/migration/V2_7__social_login.sql
backend/src/test/java/com/moa/api/SocialLoginAcceptanceTest.java (+ 제공자별 가짜)

frontend/src/
├── app/(public)/sign-in/page.tsx            # 구글·디스코드 단추, oauthError 문구
├── app/(public)/sign-up/nickname/page.tsx   # 새: 닉네임 + 약관 동의
├── app/(public)/auth/oauth/done/page.tsx    # 새
├── app/(account)/settings/page.tsx          # 제공자마다 연결·해제 줄
├── components/shell/account-menu.tsx        # hasPassword로 비밀번호 변경 숨김
├── content/terms/                           # 새: 약관 요약·전문 (버전마다)
├── lib/me.ts, types/user.ts, api/index.ts   # 새 칸·API
└── messages/ko.json
```

**Structure Decision**: 기존 모노레포 구조와 백엔드 계층(controller → application → service → repository reader/writer)을 그대로 따른다. 제공자와 주고받는 코드는 `common/security/oauth/` 한 곳에 모아 키트 헬퍼로 바꾸기 쉽게 두고, 제공자마다 클라이언트 하나를 둔다.

## 공용 파일 (구현 전에 바꿀 내용을 보고하고 승인받는다)

다른 기능·키트가 함께 쓰는 파일이다. tasks.md의 `[공용]` 작업 15개(조건부 1개 포함)가 아래 11개 파일을 고친다.

| 파일 | 바꿀 것 |
|---|---|
| `backend/.env.example` | `GOOGLE_*`, `DISCORD_*`, `SOCIAL_REJOIN_HASH_SECRET` 이름만 |
| `backend/src/main/resources/application.yaml` | `app.oauth.google.*`, `app.oauth.discord.*`, `app.oauth.rejoin-hash-secret`, `app.terms.*` |
| `backend/src/main/resources/db/migration/V2_7__social_login.sql` | 새 테이블 3개 (번호 공간을 함께 씀) |
| `backend/src/main/java/com/moa/common/exception/ErrorCode.java` | A008~A017 |
| `backend/.../service/user/UserAdminService.java` (키트 user-admin) | 소셜 회원에게 `ADMIN` 막기 |
| `backend/.../service/role/RoleUserService.java` (키트 rbac) | 소셜 회원에게 업무 역할 막기 |
| `backend/.../UserGetResponse.java` | `hasPassword`, `socialAccounts`, `socialLinkAllowed` |
| `frontend/src/app/(public)/sign-in/page.tsx` (키트 web-next) | 제공자 단추, `oauthError` 문구 |
| `frontend/src/components/shell/account-menu.tsx` (키트 셸) | 비밀번호 변경 숨김 |
| `frontend/src/app/(account)/settings/page.tsx` | 제공자마다 연결·해제 줄 |
| `frontend/messages/ko.json` | `signIn`, `signUp`(약관 포함), `settings` 문구 |

- `SecurityConfig`(공용 보안 설정)는 고치지 않는다. 콜백·약관 조회 같은 공개 경로는 `PublicPaths` 빈으로 연다
- `specs/README.md`·다른 스펙: spec "나중에 고칠 곳" 목록대로 나중에

## Complexity Tracking

위반 없음.
