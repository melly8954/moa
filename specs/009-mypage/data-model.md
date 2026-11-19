# Data Model: 마이페이지 (009)

마이그레이션: `V<번호>__user_profiles.sql`(아래 새 테이블 3개, 기존 회원 프로필 행 채우기), `V<번호>_1__play_time_codes.sql`(공통코드 시드). 번호는 정해 두지 않고 구현 때 dev 마지막 다음 번호로 한다. 순서 조건과 이유는 [research.md](./research.md) R12.

## 회원 — `users` (키트, 구조 변경 없음)

| 칸 | 이 기능에서의 쓰임 |
|---|---|
| `name` (not null, 50) | 닉네임 = 표시 이름 (spec FR-003, `010-social-login` FR-005). 바꿀 때 `User.rename`을 쓴다. 잠정 규칙(2~12자 등)은 바꿀 때만 검사한다. 규칙 이전의 기존 이름은 그대로 둔다 (spec Edge Cases) |
| `login_id` | 화면에 보이지 않는다 (계정 메뉴에서도 숨김, spec FR-014) |
| `role` + 업무 역할(rbac) | 관리자·운영자 판단 (spec FR-016, research R2) |

- 닉네임 중복 조회: `name`을 대소문자 무시로 비교(`utf8mb4_unicode_ci`), 자기 제외, 삭제된 회원 제외. 첫 검사다. DB 유일 제약은 `users`가 아니라 `user_profiles.nickname_key`에 둔다 (research R4)

## 프로필 — `user_profiles` (새 테이블)

회원 하나에 하나. 닉네임을 정할 때 만든다: 010 가입 트랜잭션, 기존 회원은 `V<번호>__user_profiles.sql`의 채우기, 행 없는 회원은 마이페이지에서 처음 저장할 때 (research R14). 행이 없으면 아래 "기본값"으로 응답한다.

| 칸 | 형식 | 규칙 | 기본값 |
|---|---|---|---|
| `user_id` | BIGINT, PK | `users.id` | |
| `nickname_key` | VARCHAR(50), null, 유일 `uk_user_profiles_nickname_key` | `users.name`의 앞뒤 공백 제거 + 소문자. 닉네임을 바꿀 때 `users.name`과 같은 트랜잭션에서 함께 바꾼다. 관리자 이름 바꾸기(002)도 함께 바꾼다 (research R4·R14) | 행을 만들 때의 `users.name`에서 |
| `avatar_key` | VARCHAR(30), null | `DefaultAvatar` 키 (research R10). 목록에서 빠진 키는 응답 때 `default` | `default` |
| `bio` | VARCHAR(500), null | 자기소개 500자 이내 (FR-006) | 빈 값 |
| `birth_year` | SMALLINT, null | 1900~올해. 응답에 `age` = 올해 − 출생 연도 (FR-007, research R6) | 빈 값 |
| `gender` | VARCHAR(20), null | `MALE`, `FEMALE`, `UNDISCLOSED` (enum `Gender`) | 빈 값 |
| `age_gender_public` | BOOLEAN, not null | 나이·성별 공개 여부 하나 (잠정, FR-007) | 거짓 |
| `discord_tag` | VARCHAR(32), null | 디스코드 새 사용자명 형식 (FR-008, research R7) | 빈 값 |
| `discord_visibility` | VARCHAR(20), not null | `PRIVATE`, `FRIENDS`(잠정), `MEMBERS` (enum `DiscordVisibility`) | `PRIVATE` |
| `nickname_changed_at` | DATETIME(6), null | 마지막 닉네임 변경 시각. 비어 있으면 가입 뒤 첫 변경 전 (FR-004, research R5) | 빈 값 |
| `created_at`, `updated_at` | DATETIME(6), not null | | |

- `V<번호>__user_profiles.sql` 채우기: 삭제되지 않은 모든 `users`에 행을 만든다. 정규화 값이 겹치는 기존 이름은 id가 가장 작은 하나만 `nickname_key`를 갖고 나머지는 null (research R14)
- 게임 정보 추가 때 이 행을 잠가(`SELECT … FOR UPDATE`) 최대 3개를 지킨다 (research R8)
- 회원 탈퇴 때 지우거나 `nickname_key`를 비우는 일은 회원 탈퇴 스펙이 정한다 (spec 가정)

## 플레이 가능 시간대 — `user_profile_play_times` (새 테이블)

| 칸 | 형식 | 규칙 |
|---|---|---|
| `user_id` | BIGINT, not null | `users.id` |
| `play_time_code` | VARCHAR(50), not null | 공통코드 `PLAY_TIME`의 코드 (FR-012) |

- PK `(user_id, play_time_code)`. 저장은 "보낸 목록으로 바꾸기"(지우고 다시 넣기)
- 새로 저장할 때는 켜진 코드만 받는다. 이미 저장된 코드가 꺼졌으면 응답에는 그대로 이름과 함께 보인다 (spec Edge Cases)

## 게임 정보 — `user_game_profiles` (새 테이블)

| 칸 | 형식 | 규칙 |
|---|---|---|
| `id` | BIGINT, PK | |
| `user_id` | BIGINT, not null | `users.id` |
| `game_code` | VARCHAR(50), not null | 공통코드 `GAME` (FR-010) |
| `tier_code` | VARCHAR(50), null | `<game_code>_TIER`. 랭크는 회원이 적는 값, 확인하지 않음 |
| `position_code` | VARCHAR(50), null | `<game_code>_POSITION`. 그룹이 없는 게임(`PUBG`)은 늘 null |
| `game_account_id` | VARCHAR(50), null | 게임 아이디. 적은 그대로 저장·표시 |
| `created_at`, `updated_at` | DATETIME(6), not null | 목록은 `created_at` 순 |

- 유일: `(user_id, game_code)` — 게임마다 하나 (US5-AC3)
- 색인: `user_id`
- 회원당 최대 3개는 서비스에서 지킨다 (위 잠금)
- `statsUrl`은 저장하지 않고 응답 때 `GameStatsLinks`가 만든다 (research R8)

## 저장하지 않는 값

| 값 | 어디에 | 근거 |
|---|---|---|
| 기본 아바타 목록 | 백엔드 enum `DefaultAvatar` + 프론트 `public/avatars/<키>.svg` | research R10 |
| 성별 목록 | enum `Gender` | spec FR-007 "정해 둔 목록" |
| 전적 사이트 주소 틀 | 백엔드 `GameStatsLinks` | research R8 |
| 닉네임 예약어 | `NicknamePolicy` 상수 | research R4 |
| 닉네임 변경 제한 기간 | 설정 `ProfileProperties.nicknameCooldown`(기본 30일) | research R5 |

## 공통코드 시드 (`V<번호>_1__play_time_codes.sql`)

새 그룹 `PLAY_TIME`. `code_groups`에 시스템 그룹(`system` = true, `004-admin-codes` FR-008)으로 넣고 `codes`에 코드를 넣는다. `code_groups` 표와 `system` 열은 004 Phase 6이 다시 만들므로 그것이 먼저다 (research R13):

| 코드 그룹 | 이름 | 코드 (순서대로) |
|---|---|---|
| `PLAY_TIME` | 플레이 가능 시간대 | `WEEKDAY_MORNING` 평일 오전, `WEEKDAY_EVENING` 평일 저녁, `WEEKEND_ALL_DAY` 주말 종일, `DAWN` 새벽 |

이 기능이 쓰지만 넣지 않는 시드 (`008-party`의 공통코드 시드 마이그레이션, 008 spec FR-002 표):

| 코드 그룹 | 코드 |
|---|---|
| `GAME` | `LOL`, `OVERWATCH`, `VALORANT`, `PUBG` |
| `LOL_TIER`, `LOL_POSITION` | 008 FR-002 표 |
| `OVERWATCH_TIER`, `OVERWATCH_POSITION` | 008 FR-002 표 |
| `VALORANT_TIER`, `VALORANT_POSITION` | 008 FR-002 표 |
| `PUBG_TIER` (포지션 없음) | 008 FR-002 표 |

## 규칙 요약

| 규칙 | 근거 |
|---|---|
| 관리자·운영자 계정은 마이페이지 API 전부 403 A201 | FR-016, US1-AC7, SC-006 |
| 닉네임: 공백 제거 뒤 2~12자, 한글·영문·숫자·`_`, 예약어 아님 (V001) | FR-003 잠정, US2-AC3 |
| 닉네임: 다른 회원과 대소문자 무시로 같으면 409 B201 (서비스 검사 + `nickname_key` 유일 제약) | FR-003 잠정, US2-AC5 |
| 닉네임: `nickname_changed_at` + 30일 전이면 409 B202 (첫 변경은 바로) | FR-004 잠정, US2-AC4·AC6 |
| 같은 닉네임으로 저장하면 아무것도 바뀌지 않음 | Edge Cases |
| 자기소개 500자, 출생 연도 1900~올해, 성별 목록, 디스코드 형식 (V001) | FR-006~FR-008, US3 |
| 아바타는 `DefaultAvatar` 키만 (V001) | FR-009, US4-AC2 |
| 게임·랭크·포지션·시간대는 켜진 공통코드만 (V001) | FR-010, FR-012, US5-AC5 |
| 게임 정보 회원당 3개(409 B203), 게임마다 하나(409 B204) | FR-010, US5-AC2·AC3 |
