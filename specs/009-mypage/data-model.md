# Data Model: 마이페이지 (009)

마이그레이션은 한 파일이다: `V<다음 번호>__mypage.sql` (`tb_user_profile` 칸 더하기, 새 테이블 2개, 공통코드 `PLAY_TIME` 시드). 번호는 적지 않고 PR 직전에 dev를 병합해 마지막 번호 + 1로 정한다 (`docs/db.md` "번호 겹침"). 순서 조건: 010의 `tb_user_profile` 다음이다(010은 dev에 들어갔다, 2026-11-22). 이유는 [research.md](./research.md) R12.
테이블·열·인덱스·COMMENT는 `docs/db.md`를 따른다.

## 회원 — `tb_user` (구조 변경 없음)

| 칸 | 이 기능에서의 쓰임 |
|---|---|
| `name` (not null, 50) | 닉네임 = 표시 이름 (spec FR-003, `010-social-login` FR-005). 다른 회원과 겹쳐도 된다. 바꿀 때 `User.rename`을 쓴다. 잠정 규칙(2~12자 등)은 바꿀 때만 검사한다. 규칙 이전의 기존 이름은 그대로 둔다 (spec Edge Cases) |
| `login_id` | 회원에게는 화면에 보이지 않는다 (계정 바에서도 숨김, spec FR-014) |
| `role` | 관리자·운영자 판단: `ADMIN` 계정 (spec FR-016, research R2) |

- 닉네임 중복 조회는 두지 않는다. 회원 구별은 `tb_user_profile`의 `(nickname_key, nickname_tag)`가 한다 (research R4, 2026-11-19)

## 프로필 — `tb_user_profile` (010이 만든 테이블에 칸을 더함)

회원 하나에 하나. 닉네임을 정할 때 만든다: 010 가입 트랜잭션, 기존 회원(`USER`)은 010 마이그레이션의 채우기, 행 없는 회원은 마이페이지에서 처음 저장할 때 (research R14). 행이 없으면 아래 "기본값"으로 응답한다.
`user_id`·`nickname_key`·`nickname_tag`·`created_at`·`updated_at`, `uk_user_profile_nickname_key_nickname_tag`, 테이블 COMMENT `'회원 프로필 (PK: 1:1 테이블)'`은 010 마이그레이션(`*__social_login.sql`)이 만들었다. 엔터티 `UserProfile`은 `BaseTimeEntity`다. 009는 `ALTER TABLE tb_user_profile ADD COLUMN`으로 그 밖의 칸을 더한다.

| 칸 | 형식 | 규칙 | 기본값 (응답) | 열 COMMENT |
|---|---|---|---|---|
| `user_id` | BIGINT, PK (010) | `tb_user.id` | | (010) |
| `nickname_key` | VARCHAR(50), not null (010) | `tb_user.name`의 앞뒤 공백 제거 + 소문자(`NicknamePolicy.key`). 닉네임을 바꿀 때 `tb_user.name`과 같은 트랜잭션에서 함께 바꾼다. 관리자 이름 바꾸기(002)는 010이 이미 맞춘다 (research R4·R14) | 행을 만들 때의 `tb_user.name`에서 | (010) |
| `nickname_tag` | CHAR(4), not null (010) | `0001`~`9999`. 서버가 정한다(`NicknameTags`). 닉네임을 바꾸면 새 `nickname_key`에서 비어 있으면 그대로, 아니면 새로 (research R4) | 행을 만들 때 `NicknameTags.assign` | (010) |
| `avatar_key` | VARCHAR(30), not null, `DEFAULT 'default'` | `DefaultAvatar` 키 (research R10). 목록에서 빠진 키는 응답 때 `default`. 기존 행은 `DEFAULT`로 `default`가 된다 | `default` | `'기본 아바타 키 (DefaultAvatar)'` |
| `bio` | VARCHAR(500), null | 자기소개 500자 이내 (FR-006) | 빈 값 | `'자기소개'` |
| `birth_year` | SMALLINT, null | 1900~올해. 응답에 `age` = 올해 − 출생 연도 (FR-007, research R6) | 빈 값 | `'출생 연도'` |
| `gender` | VARCHAR(20), null | enum `Gender` | 빈 값 | `'성별 (MALE, FEMALE, UNDISCLOSED)'` |
| `age_gender_public` | BOOLEAN, not null, `DEFAULT FALSE` | 나이·성별 공개 여부 하나 (잠정, FR-007) | 거짓 | `'나이·성별 공개 여부'` |
| `discord_tag` | VARCHAR(32), null | 디스코드 새 사용자명 형식 (FR-008, research R7) | 빈 값 | `'디스코드 사용자명'` |
| `discord_visibility` | VARCHAR(20), not null, `DEFAULT 'PRIVATE'` | enum `DiscordVisibility` | `PRIVATE` | `'디스코드 공개 범위 (PRIVATE, FRIENDS, MEMBERS)'` |
| `nickname_changed_at` | DATETIME(6), null | 마지막 닉네임 변경 시각 (FR-004, research R5) | 빈 값 | `'닉네임 바꾼 시각 (NULL: 가입 뒤 안 바꿈)'` |
| `created_at`, `updated_at` | DATETIME(6), not null (010) | | | |

- `DEFAULT`는 기존 행이 있는 테이블에 `NOT NULL` 칸을 더하는 `avatar_key`·`age_gender_public`·`discord_visibility`에만 둔다(기존 행을 채운다, `docs/db.md` "열"). NULL 칸에는 두지 않는다. 행을 넣는 코드는 늘 값을 준다. 010 가입처럼 009 칸을 모르는 코드가 행을 만들어도 값이 비지 않도록, 엔터티 `UserProfile`의 세 필드에 같은 기본값(`default`, 거짓, `PRIVATE`)을 둔다
- 기존 회원 채우기는 010 마이그레이션이 했다(삭제되지 않은 `USER`, 시스템 계정 제외). 009는 행을 넣지 않는다 (research R14)
- 게임 정보 추가 때 이 행을 잠가(`SELECT … FOR UPDATE`) 최대 3개를 지킨다 (research R8)
- 탈퇴 회원의 행은 `docs/privacy.md` "탈퇴 회원 파기"대로 회원 탈퇴 스펙이 지운다 (spec 가정)

## 플레이 가능 시간대 — `tb_user_profile_play_time` (새 컬렉션 테이블)

`UserProfile`의 `@ElementCollection`(`@CollectionTable(name = "tb_user_profile_play_time", joinColumns = @JoinColumn(name = "user_id"))`)이다. 저장은 "보낸 목록으로 바꾸기"다.

| 칸 | 형식 | 규칙 | 열 COMMENT |
|---|---|---|---|
| `user_id` | BIGINT, not null | `tb_user_profile.user_id` | `'회원 ID'` |
| `play_time_code` | VARCHAR(50), not null | 공통코드 `PLAY_TIME`의 항목 코드 (FR-012). `tb_code.code`와 같은 길이 | `'플레이 가능 시간대 코드 (PLAY_TIME 그룹)'` |

- PK `(user_id, play_time_code)` (`docs/db.md` "키": 컬렉션 테이블). `user_id`는 PK 앞 열이라 인덱스를 따로 두지 않는다
- 감사 열 없음 (`docs/db.md` "감사·삭제 열": 컬렉션 테이블은 주인 행과 함께 저장된다)
- 테이블 COMMENT `'플레이 가능 시간대 (PK: 컬렉션 테이블; 감사 열 없음: 프로필과 함께 저장)'`
- 새로 저장할 때는 켜진 코드만 받는다. 이미 저장된 코드가 꺼졌으면 응답에는 그대로 이름과 함께 보인다 (spec Edge Cases)

## 게임 정보 — `tb_user_game_profile` (새 테이블)

엔터티 `UserGameProfile`은 `BaseEntity` + `@HardDelete`다. 회원이 지우면 행을 지운다(개인정보라 소프트 삭제로 남기지 않는다, `docs/privacy.md` "원칙").

| 칸 | 형식 | 규칙 | 열 COMMENT |
|---|---|---|---|
| `id` | BIGINT, PK AUTO_INCREMENT | | |
| `user_id` | BIGINT, not null | `tb_user.id` | `'회원 ID'` |
| `game_code` | VARCHAR(50), not null | 공통코드 `GAME` (FR-010) | `'게임 코드 (GAME 그룹)'` |
| `tier_code` | VARCHAR(50), null | `<game_code>_TIER`. 랭크는 회원이 적는 값, 확인하지 않음 | `'랭크 코드 (<게임>_TIER 그룹; 회원 입력, 확인 안 함)'` |
| `position_code` | VARCHAR(50), null | `<game_code>_POSITION`. 그룹이 없는 게임(`PUBG`)은 늘 null | `'포지션 코드 (<게임>_POSITION 그룹; NULL: 없음)'` |
| `game_account_id` | VARCHAR(50), null | 게임 아이디. 적은 그대로 저장·표시 | `'게임 아이디'` |
| `created_at`, `updated_at`, `created_by`, `updated_by` | `BaseEntity` 감사 열 | 목록은 `created_at` 순 | |

- 유일 `uk_user_game_profile_user_id_game_code (user_id, game_code)` — 게임마다 하나 (US5-AC3)
- `user_id` 인덱스는 따로 두지 않는다: 유일 제약의 앞 열이다(`docs/db.md` "인덱스·제약"). 목록 정렬(`created_at`)은 회원당 3행 이하라 인덱스를 두지 않는다
- 테이블 COMMENT `'회원 게임 정보'`
- 회원당 최대 3개는 서비스에서 지킨다 (위 잠금)
- `statsUrl`은 저장하지 않고 응답 때 `GameStatsLinks`가 만든다 (research R8)

## 저장하지 않는 값

| 값 | 어디에 | 근거 |
|---|---|---|
| 기본 아바타 목록 | 백엔드 enum `DefaultAvatar` + 프론트 `public/avatars/<키>.svg` | research R10 |
| 성별 목록 | enum `Gender` | spec FR-007 "정해 둔 목록" |
| 전적 사이트 주소 틀 | 백엔드 `GameStatsLinks` | research R8 |
| 닉네임 예약어 | `NicknamePolicy` 상수 (010) | research R4 |
| 닉네임 변경 제한 기간 | 설정 `ProfileProperties.nicknameCooldown`(기본 30일) | research R5 |

## 공통코드 시드 (같은 파일 `V<다음 번호>__mypage.sql`)

새 그룹 `PLAY_TIME`을 지금 dev의 공통코드(`tb_code_group`, `tb_code`, 관리자가 코드 관리 화면에서 고침, `004-admin-codes` FR-002·FR-004·FR-008)에 넣는다. 시스템 그룹 구분(`system` 열)은 2026-11-20에 없어졌다 (research R13):

| 코드 그룹 | 이름 | 설명 | 항목 (순서대로) |
|---|---|---|---|
| `PLAY_TIME` | 플레이 가능 시간대 | 마이페이지 플레이 가능 시간대 선택지 (009-mypage) | `WEEKDAY_MORNING` 평일 오전, `WEEKDAY_EVENING` 평일 저녁, `WEEKEND_ALL_DAY` 주말 종일, `DAWN` 새벽 |

- 그룹: `group_code = 'PLAY_TIME'`인 활성 행이 없을 때만 넣는다. `enabled` 참, `sort_order`는 지금 그룹의 가장 큰 값 + 1. 관리자가 이미 같은 그룹을 만들었으면 넣지 않는다 (`004-admin-codes` FR-002)
- 항목: `(group_code, code)` 활성 행이 없을 때만 넣는다. `sort_order` 0~3, `enabled` 참
- 행은 고정 id가 아니라 `group_code`·`code`로 찾고, 있는 행은 고치지 않는다(관리자 변경을 덮어쓰지 않음, `docs/db.md` "마이그레이션"). 감사 열 `created_by`·`updated_by`는 시스템 계정(1)
- 쓰는 곳은 그룹 설명에 적는다 (`004-admin-codes` 2026-11-20 "쓰는 곳은 설명으로 합친다")

이 기능이 쓰지만 넣지 않는 시드 (`008-party`의 공통코드 시드, 008 spec FR-002 표). **2026-11-22 dev에 없다** — 게임 정보(US5)는 008 시드가 dev에 들어간 뒤에 구현한다 (research R13, tasks T002):

| 코드 그룹 | 항목 |
|---|---|
| `GAME` | `LOL`, `OVERWATCH`, `VALORANT`, `PUBG` |
| `LOL_TIER`, `LOL_POSITION` | 008 FR-002 표 |
| `OVERWATCH_TIER`, `OVERWATCH_POSITION` | 008 FR-002 표 |
| `VALORANT_TIER`, `VALORANT_POSITION` | 008 FR-002 표 |
| `PUBG_TIER` (포지션 없음) | 008 FR-002 표 |

## 규칙 요약

| 규칙 | 근거 |
|---|---|
| 관리자·운영자 계정(`ADMIN`)은 마이페이지 API 전부 403 A201 | FR-016, US1-AC7, SC-006 |
| 닉네임: 공백 제거 뒤 2~12자, 한글·영문·숫자·`_`, 예약어 아님 (V001) | FR-003 잠정, US2-AC3 |
| 닉네임: 다른 회원과 겹쳐도 됨. `(nickname_key, nickname_tag)` 유일. 바꾸면 태그는 비어 있으면 그대로, 아니면 새로 | FR-003, US2-AC5·AC7 |
| 닉네임: 그 `nickname_key`의 태그 9999개가 다 쓰이면 409 B201 | FR-003, US2-AC8 |
| 닉네임: `nickname_changed_at` + 30일 전이면 409 B202 (첫 변경은 바로) | FR-004 잠정, US2-AC4·AC6 |
| 같은 닉네임으로 저장하면 아무것도 바뀌지 않음 | Edge Cases |
| 자기소개 500자, 출생 연도 1900~올해, 성별 목록, 디스코드 형식 (V001) | FR-006~FR-008, US3 |
| 아바타는 `DefaultAvatar` 키만 (V001) | FR-009, US4-AC2 |
| 게임·랭크·포지션·시간대는 켜진 공통코드만 (V001) | FR-010, FR-012, US5-AC5 |
| 게임 정보 회원당 3개(409 B203), 게임마다 하나(409 B204) | FR-010, US5-AC2·AC3 |
