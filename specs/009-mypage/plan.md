# Implementation Plan: 마이페이지 (mypage)

**Branch**: `feat/009-mypage` (문서는 `docs/009-mypage`) | **Date**: 2026-11-17 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/009-mypage/spec.md`

## Summary

로그인한 회원(관리자·운영자 제외)이 계정 화면 `/account/profile`에서 자기 프로필을 보고 고친다. 닉네임은 키트 `tb_user.name`을 그대로 표시 이름으로 쓰고(`010-social-login`과 같은 결정), 나머지 프로필(아바타, 자기소개, 출생 연도·성별·공개 여부, 디스코드 태그·공개 범위, 플레이 가능 시간대)은 새 테이블 `tb_user_profile`, 게임 정보는 `tb_user_game_profile`(회원당 최대 3개)에 둔다. 닉네임은 다른 회원과 겹쳐도 되고 회원은 서버가 정한 4자리 태그로 `닉네임#0421`처럼 구별한다(2026-11-19). `tb_user_profile`(`nickname_key`·`nickname_tag`, `uk_user_profile_nickname_key_nickname_tag`)와 규칙 `NicknamePolicy`·태그 `NicknameTags`, 가입 때 행 만들기, 기존 회원 채우기, 관리자 이름 바꾸기의 태그 맞추기는 010이 dev에 만들었고(2026-11-22, PR #67), 009는 칸을 더하고 닉네임을 바꿀 때 `tb_user.name`과 키·태그를 같은 트랜잭션에서 함께 바꾼다 (research R4·R14). **010은 dev에 들어갔다**. 게임·랭크·포지션은 `008-party`가 넣는 공통코드를, 플레이 가능 시간대는 이 기능이 새로 넣는 공통코드 그룹 `PLAY_TIME`을 쓴다. 008 시드는 아직 dev에 없어 게임 정보(US5)는 그 뒤에 구현한다(research R13). 전적 사이트 링크는 서버가 게임별 규칙으로 만들어 준다. 계정 바(`account-bar.tsx`)는 아바타를 보이고 회원의 로그인 아이디를 숨기며 톱니 메뉴에 "마이페이지" 항목을 더한다. 열린 질문 6개는 spec의 잠정 답으로 설계하고 확정되면 해당 자리만 바꾼다 ([research.md](./research.md) "열린 질문의 처리").

## Technical Context

**Language/Version**: 백엔드 Java 21, Spring Boot 4.1. 프론트 TypeScript, Next 16, React 19

**Primary Dependencies**: 기존 Spring Data JPA·Flyway·Bean Validation, 키트 code 헬퍼(공통코드 조회 `GET /api/v1/codes/{groupCode}`). 새 의존성 없음. 프론트는 기존 api 클라이언트, react-hook-form·zod, TanStack Query(`useMe`), next-intl 문구, 공통코드 훅(`lib/codes.ts`)

**Storage**: MariaDB. 한 마이그레이션 `V<다음 번호>__mypage.sql`: 010이 만든 `tb_user_profile`에 칸 더하기(`ALTER TABLE … ADD COLUMN`), 새 테이블 2개 `tb_user_profile_play_time`(컬렉션 테이블), `tb_user_game_profile`, 공통코드 `PLAY_TIME` 그룹·항목. 번호는 PR 직전 dev 마지막 + 1, 010의 `tb_user_profile` 다음 (research R12, `docs/db.md`). 키트 `tb_user`는 구조를 바꾸지 않는다

**Testing**: 인수 테스트 `AcceptanceTest` 상속(`@Tag("db")`, `dbTest`, 이름에 AC 번호). 프론트 단위 테스트(`npm run check`), e2e는 핵심 흐름(닉네임 바꾸기 → 계정 바 반영)만

**Target Platform**: 웹

**Project Type**: 웹 서비스 모노레포 (`backend/`, `frontend/`)

**Performance Goals**: SC-001 닉네임 변경 30초 안(화면 하나, 요청 하나). SC-002 저장 뒤 계정 바 즉시 반영(`me` 쿼리 무효화)

**Constraints**: 다른 스펙 파일·키트 코드 수정은 [공용]으로 표시하고 승인 뒤에만. 이미지 올리기 없음(FR-009). 게임 API 호출 없음(FR-010). 공용 `SecurityConfig` 수정 없음(새 API는 모두 로그인 필요라 기본 규칙으로 막힌다)

**Scale/Scope**: 새 API 9개, 새 화면 1개(`/account/profile`, 게임정보 추가 모달 포함) + 계정 바 수정, 새 테이블 2개와 `tb_user_profile` 칸 8개, 공통코드 그룹 1개

## Constitution Check

*GATE: Phase 0 전 확인, Phase 1 뒤 다시 확인.*

| 원칙 | 확인 | 결과 |
|---|---|---|
| I. 스펙이 먼저다 | 계획의 동작은 spec FR·AC에 있다. 열린 질문 6개는 추측으로 확정하지 않고 spec의 잠정 답으로 설계했다. 확정되면 바꿀 자리를 research "열린 질문의 처리"에 적었다: FR-003 닉네임 규칙(검증 한 곳 `NicknamePolicy`), FR-004 변경 제한(30일 값 하나), FR-007 만 14세 미만(이번에는 막지 않음, 출생 연도 범위만), FR-007 나이·성별 공개 개별 여부(칸 하나 → 둘로 바꾸기 쉽게), FR-013 친구 기능 전에도 "친구만"을 고를 수 있게 할지(공개 범위 값만 저장, 보는 API 없음), FR-013 보기 범위(보이는 규칙을 한 곳 `ProfileVisibility`에 둠). FR-013의 "다른 회원 프로필을 보는 화면·API"는 spec이 범위 밖으로 두어 만들지 않는다 | 통과 (잠정 6곳) |
| II. AC마다 테스트 하나 | US1~US6 AC 32개를 인수 테스트(API)와 프론트 단위 테스트로 나눠 하나씩, 이름에 AC 번호. 화면만의 AC(US1-AC8, US5-AC8·AC9의 표시)는 프론트 단위 테스트 | 통과 |
| III. 키트를 따른다 | 키트 `tb_user`(구조 변경 없음, 기존 `rename` 사용), code 헬퍼(그룹·코드 조회, 시드는 `tb_code_group`·`tb_code`에 넣기만, research R13), 셸 계정 바. 키트 파일을 고치는 곳은 아래 "공용 파일"로 모아 승인받는다 | 통과 (공용 승인 대기) |
| IV. 제자리에서 고친다 | 001·002·004·008·010 스펙 갱신은 spec "나중에 고칠 곳"에 목록만. 이번에 다른 스펙 파일을 고치지 않는다 | 통과 |
| V. 테스트를 고쳐서 통과시키지 않는다 | 해당 없음 (계획 단계). 기존 `UserAccountAcceptanceTest`의 `/users/me` 칸 추가는 스펙(010·009)에 근거한다 | 통과 |
| VI. 시크릿 | 다루는 시크릿 없음 | 해당 없음 |
| 목록 종류 | 새 목록 화면 없음 (게임 정보는 최대 3줄) | 해당 없음 |

Phase 1 뒤 재확인: data-model·contracts가 위 표와 어긋나지 않는다. 위반 없음 → Complexity Tracking 비움.

## Project Structure

### Documentation (this feature)

```text
specs/009-mypage/
├── spec.md
├── plan.md              # 이 파일
├── research.md          # R1 화면 경로 ~ R14 프로필 행을 만드는 때, 열린 질문의 처리
├── data-model.md        # tb_user(변경 없음) + tb_user_profile + tb_user_profile_play_time + tb_user_game_profile, 공통코드 시드
├── quickstart.md        # 수동·자동 확인 순서
├── contracts/
│   ├── profile-api.md           # 내 프로필·닉네임·게임 정보·아바타 API, 오류 코드
│   └── users-me-and-screens.md  # /users/me 추가 칸, 화면, 계정 바
├── checklists/requirements.md
└── tasks.md             # /speckit-tasks에서 만든다
```

### Source Code (repository root)

```text
backend/src/main/java/com/moa/
├── api/controller/v1/user/            # 내 프로필·닉네임·게임 정보 (ApiMyProfileController), 아바타 목록
├── api/application/v1/user/           # 마이페이지 흐름 (ProfileApplication)
├── api/entity/user/                   # UserProfile, UserGameProfile, Gender, DiscordVisibility, DefaultAvatar
├── api/repository/{jpa,reader,writer}/user/
├── api/service/user/                  # ProfileService, NicknameService, GameProfileService,
│                                      # ProfileVisibility(보이는 규칙), MemberActivityPolicy(관리자·운영자 판단)
│                                      # NicknamePolicy(규칙)·NicknameTags(태그)는 010이 만든 것을 쓴다
├── api/service/user/GameStatsLinks.java  # 게임별 전적 사이트 링크 규칙
└── common/exception/ErrorCode.java    # B202~B204, A201 (B201은 010)
backend/src/main/resources/db/migration/
└── V<다음 번호>__mypage.sql          # 번호는 PR 직전 dev 마지막 + 1, 010의 tb_user_profile 다음 (research R12)
backend/src/test/java/com/moa/api/MyPageAcceptanceTest.java

frontend/
├── public/avatars/                    # 기본 아바타 이미지 (새)
└── src/
    ├── app/(account)/account/profile/page.tsx   # 새: 마이페이지(프로필 편집)
    ├── components/profile/                      # 새: 편집 폼, 게임정보 추가 모달, 아바타 고르기, 게임 정보 목록
    ├── components/shell/account-bar.tsx         # 아바타, 회원 로그인 아이디 숨김, 톱니 메뉴의 마이페이지 항목
    ├── lib/next-path.ts                         # PROFILE_PATH
    ├── lib/avatars.ts                           # 새: 아바타 키 → 이미지
    ├── types/user.ts, types/profile.ts, api/index.ts
    └── messages/ko.json
```

**Structure Decision**: 기존 모노레포 구조와 백엔드 계층(controller → application → service → repository reader/writer)을 따른다. 마이페이지는 키트 계정 화면 묶음 `(account)` 안에 둔다(`/account/password`와 같은 묶음, `AuthGate`·`AccountShell`을 그대로 씀). 프로필 데이터는 키트 `tb_user`를 넓히지 않고 1:1 테이블로 둔다(research R3).

## 공용 파일 (구현 전에 바꿀 내용을 보고하고 승인받는다)

다른 기능·키트가 함께 쓰는 파일이다. tasks.md에서 `[공용]`으로 표시한다.

| 파일 | 출처 | 바꿀 것 |
|---|---|---|
| `backend/src/main/resources/db/migration/V<다음 번호>__mypage.sql` | 번호 공간을 함께 씀. PR 직전 dev 마지막 + 1 (010 뒤) | 010이 만든 `tb_user_profile`에 칸 더하기, 새 테이블 2개, `PLAY_TIME` 그룹·항목 4개 (research R12·R13·R14) |
| `backend/src/main/java/com/moa/common/exception/ErrorCode.java` | 골격 | B202~B204, A201 (B201은 010이 넣음) |
| `backend/src/main/java/com/moa/api/controller/v1/user/response/UserGetResponse.java` | 골격 (010이 `hasPassword`를 더함) | `avatar`, `mypageAllowed` |
| `frontend/src/types/user.ts` | 키트 web-next | `Me`에 `avatar`, `mypageAllowed` |
| `frontend/src/components/shell/account-bar.tsx` | moa 셸 (001 FR-005, 010이 아이디 줄·비밀번호 변경 숨김을 더함) | 아바타 표시, 회원의 `loginId` 숨김, 톱니 메뉴의 "마이페이지" 항목(관리자·운영자에게 숨김) |
| `frontend/src/lib/next-path.ts` | 키트 web-next | `PROFILE_PATH = "/account/profile"` |
| `frontend/messages/ko.json` | 여러 기능 | `profile` 묶음, `shell.mypage` |

- `SecurityConfig`, `AuthGate`, `(account)/layout.tsx`, 키트 `User` 엔터티는 고치지 않는다 (`User.rename`을 그대로 쓴다)
- 다른 스펙(001·002·004·008·010)과 `specs/README.md`는 spec "나중에 고칠 곳" 목록대로 나중에
- 010과 겹치는 파일(`UserGetResponse`, `account-bar.tsx`)은 dev에 들어간 010 변경을 지키고 더한다. 관리자·운영자 판단은 010이 만들지 않아 009가 만든다 (research R11)
- `UserReader` 닉네임 중복 조회와 `UserAdminService.rename` 고치기는 2026-11-19에 이 표에서 빠졌다: 중복 조회는 필요 없고, 이름 바꾸기의 태그 맞추기는 010이 한다 (research R4·R14)

## Complexity Tracking

위반 없음.
