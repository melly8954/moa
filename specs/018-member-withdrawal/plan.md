# Implementation Plan: 회원 탈퇴 (member-withdrawal)

**Branch**: `feat/018-member-withdrawal` | **Date**: 2026-11-25 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/018-member-withdrawal/spec.md`

## Summary

회원이 마이페이지 "계정 관리" 탭에서 닉네임을 입력해 탈퇴하고, 30일 안에 다시 로그인해 복구하며, 30일이 지나면 매일 도는 정리가 `docs/privacy.md`대로 파기한다.
탈퇴 상태는 `tb_user.withdrawn_at`에 두고 **유예 중에는 `deleted_at`을 쓰지 않는다**. 그래서 로그인 아이디·이메일·소셜 계정이 지금의 유니크와 행으로 저절로 붙잡히고 관리자 코드를 고치지 않는다 ([research.md](./research.md) R1).
유예 중인 회원은 `User`의 기본 조회 조건에서 숨긴다. 로그인·복구·파기만 예외로 읽는다 (R8). `docs/privacy.md`의 "그 목적 말고는 쓰지 않는다"를 공통 지점 한 곳에서 지킨다.
"다음 요청부터 인증 거부"는 JWT 필터 뒤에 요청마다 PK 조회 한 번을 하는 필터로 한다. 키트 코드는 고치지 않는다 (R2).
복구는 본인 확인에 성공한 자리에서만 주는 10분짜리 서명 쿠키로 한다 (R5). 파기는 회원마다 한 트랜잭션으로 하고 `PersonalDataPurgedEvent`를 낸다. 관리자가 지운 지 30일이 지난 회원도 같은 정리가 파기한다 (R6, FR-019).

## Technical Context

**Language/Version**: Java 21 (Spring Boot 4.1.2), TypeScript 5 (Next 16.3)

**Primary Dependencies**: 기존 Spring Security·jjwt·Spring Data JPA·Flyway, 키트 websocket. 새 의존성 없음

**Storage**: MariaDB. `tb_user`에 칸 둘과 인덱스 둘, `tb_purge_log.target` COMMENT (`V<다음 번호>__member_withdrawal.sql`, 번호는 PR 직전). 새 테이블 없음

**Testing**: 인수 테스트 3개 클래스(`@Tag("db")`), 백엔드 단위, 프론트 단위, e2e 핵심 흐름 2개. `User` 조건을 바꾼 뒤 `build`와 `dbTest` 전체를 로컬에서 돌린다 (사용자 지시)

**Target Platform**: 웹 (사용자 셸, 공개 로그인 화면)

**Project Type**: 웹 서비스 (모노레포 `backend/`, `frontend/`)

**Performance Goals**: 인증된 요청마다 PK 조회 1회 추가. 파기 정리는 인덱스로 대상만 읽는다

**Constraints**: 키트 코드와 관리자 기능 코드를 고치지 않는다 (원칙 III, FR-019). Service는 자기 도메인만, 트랜잭션은 Application만 (`ArchitectureTest`). 로그에 이메일 원문·비밀번호·토큰·소셜 식별자 금지 (FR-020). 유예 뒤 5일 안 파기

**Scale/Scope**: API 3개 추가(탈퇴 1, 복구 2), 기존 API 2개의 응답이 늘어남(로그인, 소셜 콜백), 화면 2개 추가와 3개 변경, AC 48개

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| 원칙 | 판정 | 근거 |
|---|---|---|
| I. 스펙이 먼저다 | 통과 | spec에 미결 표시가 없다. plan이 정하는 것은 spec이 맡긴 방법뿐이다(인증 거부, 붙잡기, 파기 전 5일의 문구) |
| II. AC마다 테스트 하나 | 통과 | AC 48개의 대응을 quickstart와 tasks에 둔다. US1-AC7은 보이는 화면이 아직 없어 규칙의 단위 테스트다 |
| III. 키트를 따른다 | 통과 | `SecurityConfig`·JWT 필터·토큰·웹소켓 설정을 고치지 않고 키트의 확장점(`HttpSecurityCustomizer`, 추가 `WebSocketMessageBrokerConfigurer`)을 쓴다. 골격 회원 도메인(`User`, `UserRepository`, `UserService`)은 프로젝트가 고치는 곳이다 |
| IV. 기존 기능은 제자리에서 고친다 | 통과 | `013` FR-002·data-model, `010` contracts 콜백 표와 spec, `001` FR-005(계정 화면, 계정 줄), `docs/privacy.md`를 이 문서 PR에서 고친다. 아래 "함께 고칠 문서" |
| V. 테스트를 고쳐서 통과시키지 않는다 | 통과 | 기존 테스트를 바꿀 것은 지금 보이지 않는다. `User` 조건을 바꾼 뒤 전체를 돌려 깨지는 것이 나오면 멈추고 묻는다 |
| VI. 시크릿을 다루지 않는다 | 통과 | 복구 쿠키는 기존 JWT 키로 서명한다. 새 비밀 값이 없다 |
| `docs/privacy.md` | 통과 | 유예 보관은 복구 목적으로만(R8·R13), 30일 뒤 5일 안 파기(R6), 관리자가 지운 회원도 파기(FR-019), 해시를 모으지 않음(FR-015), 로그에 개인정보 없음(R9) |
| 기술 제약: 목록 | 해당 없음 | 새 목록이 없다 |
| 기술 제약: DB | 통과 | `docs/db.md`의 칸·인덱스 이름과 COMMENT를 따른다. 마이그레이션 번호는 `V<다음 번호>` |

Phase 1 설계 뒤 다시 확인했다. 위반이 없다.

## Project Structure

### Documentation (this feature)

```text
specs/018-member-withdrawal/
├── spec.md
├── plan.md
├── research.md            # R1 탈퇴 상태 ~ R12 테스트
├── data-model.md          # tb_user 칸 둘, 상태 변화, 파기할 때
├── quickstart.md
├── contracts/
│   ├── withdrawal-api.md
│   └── screens.md
├── checklists/requirements.md
└── tasks.md               # /speckit-tasks가 만든다
```

### Source Code (repository root)

```text
backend/
├── src/main/resources/db/migration/V<다음 번호>__member_withdrawal.sql
├── src/main/java/com/moa/common/
│   ├── security/access/       # AccountAccessChecker, AccountAccessFilter, 등록 빈(HttpSecurityCustomizer)
│   ├── security/recovery/     # 복구 쿠키, WithdrawnAccountException, 그 응답을 만드는 Advice
│   ├── security/token/RefreshTokenEraser.java
│   ├── websocket/ 밖의 moa 설정  # api/realtime: CONNECT 거절, 열린 세션 닫기
│   ├── purgelog/PurgeTarget.java            # MEMBER
│   └── exception/ErrorCode.java             # A401~A403 [공용]
├── src/main/java/com/moa/api/
│   ├── controller/v1/user/ApiMyWithdrawalController.java
│   ├── controller/v1/auth/ApiAccountRecoveryController.java
│   ├── application/v1/user/{MemberWithdrawalApplication,AccountRecoveryApplication,MemberPurgeApplication}.java
│   ├── application/v1/auth/SocialLoginApplication.java   # 탈퇴한 회원의 갈 곳
│   ├── service/user/          # MemberWithdrawalService, MemberPurgeService, MemberStatusService, WithdrawnAccountChecker,
│   │                          # MemberDisplay, UserService 변경
│   ├── service/{notification,notice}/        # 그 회원의 행 지우기
│   ├── application/v1/notification/NotificationApplication.java   # notify 한 줄 (haily, 승인됨)
│   ├── repository/{jpa,reader,writer}/user/   # 유예 회원을 보는 조회, 파기
│   ├── dto/user/              # 이벤트 record 셋, 상태 DTO
│   ├── entity/user/User.java  # withdrawn_at, purged_at, 기본 조회 조건
│   └── scheduler/MemberPurgeCleaner.java
└── src/test/java/com/moa/api/{UserRestrictionAcceptanceTest,MemberWithdrawalAcceptanceTest,MemberPurgeAcceptanceTest}.java

frontend/
├── src/app/(account)/account/profile/{page.tsx,manage/page.tsx}
├── src/app/(public)/sign-in/{page.tsx,recover/page.tsx}
├── src/components/profile/mypage-frame.tsx
├── src/components/account-management/       # 항목 목록, 탈퇴 영역, 확인 창
├── src/components/auth/                     # 탈퇴 안내, oauth-error-alert(purgePending)
├── src/api/{withdrawal,index}.ts  src/types/  src/lib/next-path.ts  messages/ko.json
└── e2e/withdrawal.spec.ts
```

**Structure Decision**: 백엔드는 009와 같은 골격 계층이다. 보안·쿠키처럼 컨트롤러 밖에서 도는 것은 `common`에 둔다(`ArchitectureTest`의 계층 규칙은 `api`만 본다). 프론트는 009 마이페이지와 010 로그인 화면 위에 더한다.

## 공용 파일 (구현 전에 바꿀 내용을 보고하고 승인받는다)

| 파일 | 바꿀 것 |
|---|---|
| `ErrorCode.java` | A401~A403 |
| `User.java`, `UserRepository.java`, `UserReader.java`, `UserService.java` | 칸 둘, 기본 조회 조건, 예외 조회, 로그인 확인 |
| `application.yaml` | `app.withdrawal.*` |
| `SocialLoginApplication.java`, `SocialAccountService.java`, `SocialLoginUserDto` (010) | 탈퇴한 회원의 갈 곳 |
| `sign-in/page.tsx`, `oauth-error-alert.tsx`, `next-path.ts`, `api/index.ts`, `ko.json` | 복구 안내, 경로, 문구 |

**haily(melly8954) 코드** (2026-11-25 사용자 승인. 기존 동작은 바꾸지 않고 더하기만 한다)

| 파일 | 바꿀 것 | 승인 |
|---|---|---|
| `NotificationApplication.notify` (007) | 유예 중 회원이면 저장·발송하지 않는 한 줄 (FR-022) | 2026-11-25 승인 |
| `NotificationService`와 그 reader·writer·repository (007) | 그 회원의 푸시 구독 지우기(FR-021), 알림·구독 지우기(파기). 메서드 더하기만 | 2026-11-25 승인 |
| `NoticeService`와 그 writer (006) | 그 회원의 공지 확인 지우기(파기). 메서드 더하기만 | 2026-11-25 승인 |
| `PurgeTarget`, `admin/purge-logs/page.tsx`, `types/purge-log.ts`, `ko.json` `purgeLog` (013) | 대상 종류 "회원" | 2026-11-25 승인 |
| `tb_purge_log.target` COMMENT (013 테이블, 우리 마이그레이션) | 값 목록에 `MEMBER` | 2026-11-25 승인 |

- 사용자 관리(002)·권한 관리(003) 코드는 고치지 않는다

## 담당자 알림 목록

`User`의 기본 조회 조건(R8)으로 막히지 않는 넷이다. 이 PR을 병합할 때 사람이 알린다.

| # | 무엇 | 담당 | 알릴 내용 |
|---|---|---|---|
| 1 | 프로필 표 직접 조회 | hyeonchang (`014` 프로필·검색, `008`) | `tb_user_profile`을 `User` 없이 읽으면 유예 중 회원의 아바타·태그·소개가 나온다. 늘 `User`와 함께 읽는다. 회원 검색·멘션도 같다 |
| 2 | 숏폼 | hyeonchang (`015`) | 작성자 이름은 막히지만 숏폼 자체는 남는다. `015` FR-015대로 유예 중에는 숨기고 "파기됨"에서 지운다. 상태는 `MemberStatusService`로 본다 |
| 3 | 개인 알림 | haily (`007`) | `notify`에 유예 중 회원을 거르는 한 줄을 이 기능이 넣는다(승인됨). 알림을 보내는 기능은 null을 실패로 보지 않는다 |
| 4 | 문의 답변 | haily (`017`) | 작성자는 조회에 없어 "탈퇴한 회원"이다. 답변 막기와 1년 보관은 `MemberStatusService`의 탈퇴 시각으로 한다. "탈퇴 뒤에는 다툼 대응 말고는 쓰지 않는다"(`docs/privacy.md`)는 017이 지킨다 |

함께 알릴 것

- `008`(hyeonchang): 멤버·신청·채팅의 이름은 왼쪽 조인이나 따로 읽기로 하고, 없으면 `MemberDisplay`의 "탈퇴한 회원"을 쓴다. 안쪽 조인이면 멤버 행이 빠질 수 있다
- `002`(haily): 사용자 관리 목록에서 유예 중 회원이 사라진다. 지운 회원은 30일 뒤 되돌릴 수 없게 파기되고, 처음 돌 때 이미 지운 회원이 한꺼번에 파기된다. 지운 소셜 회원은 파기 뒤 같은 계정으로 다시 가입할 수 있다
- `012`(haily): 유예 30일, 탈퇴해도 남는 것을 처리방침에 알린다

## 알려진 한계 (2026-11-25 사용자 확인, 그대로 둔다)

- 탈퇴하고 15분 안에 복구하면 탈퇴 전 액세스 토큰이 남은 수명 동안 다시 통과한다 ([research.md](./research.md) R2)
- 서버가 여럿이면 다른 서버에 열린 실시간 연결은 닫지 못한다. 다시 연결하면 막힌다 (R3)
- 쿠키를 쓰지 않는 앱 클라이언트는 복구할 수 없다. 소셜 로그인도 지금 같다 (R5)

## 함께 고칠 문서 (원칙 IV)

| 문서 | 고칠 것 |
|---|---|
| `013-purge-log` FR-002, data-model 규칙 | 대상 종류 "회원", 탈퇴 파기의 대상 회원 ID (이 문서 PR) |
| `013-purge-log` contracts·data-model의 값 목록 | `MEMBER` (구현 PR) |
| `010-social-login` contracts/auth-oauth 콜백 표, spec 158·244줄 | 유예 중 → `/sign-in/recover`, 파기 대기 → `purgePending`, 관리자 삭제의 파기 (이 문서 PR) |
| `001-admin-menus` FR-005 | 계정 화면에 마이페이지(프로필·계정 관리), 009가 바꾼 계정 줄·톱니 메뉴 (이 문서 PR) |
| `docs/privacy.md` 푸시 구독 줄 | "탈퇴할 때"를 더함 (이 문서 PR) |
| `docs/privacy.md` 보관 기간 표의 상태 칸 | "회원 탈퇴 스펙"이 `018`을 가리키게 (구현 PR) |
| `specs/README.md` | 018 상태 (구현 PR) |

## Complexity Tracking

위반이 없어 비워 둔다.
