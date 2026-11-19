# Research: 마이페이지 (009)

스펙: [spec.md](./spec.md). 각 항목은 결정 / 이유 / 검토한 대안 순서다.
**"잠정"** 표시는 spec의 열린 질문(팀 확인 대기)에 기댄 부분이다. 맨 아래 "열린 질문의 처리"에 확정 뒤 바꿀 자리를 모았다.
`010-social-login`(PR #54, 병합 전)은 `origin/docs/010-google-login`의 문서를 읽기만 해서 경계를 맞췄다.

## R1. 화면 경로와 묶음 (FR-001, FR-015)

- **결정**: 마이페이지는 `/account/profile`이다. 키트 계정 화면 묶음 `frontend/src/app/(account)/`에 둔다(`/account/password`와 같은 묶음). 이 묶음의 `layout.tsx`가 `AuthGate`(로그인 확인, 로그인 뒤 돌아오기)와 `AccountShell`(사용자에 맞는 셸)을 이미 하므로 US1-AC2를 따로 만들지 않는다. 계정 메뉴로 여는 계정 화면이라 프로그램으로 등록하지 않는다(`001-admin-menus` FR-008)
- **이유**: 기존 계정 화면과 같은 틀·같은 보호를 그대로 쓴다. 경로 상수는 `lib/next-path.ts`의 `PASSWORD_PATH`·`SETTINGS_PATH` 옆에 `PROFILE_PATH`로 둔다
- **대안**: `/mypage` — 계정 화면 묶음 밖이 되어 `AuthGate`·셸 처리를 다시 해야 한다. `/settings` 안에 넣기 — spec FR-002가 프로필과 로그인 방식(010의 계정 설정)을 나눈다

## R2. 관리자·운영자 막기 (FR-016)

- **결정**
  - "관리자·운영자 계정" = 시스템 역할 `ADMIN`이거나 업무 역할(rbac)을 하나라도 가진 계정. `010-social-login` FR-007·research R7과 같은 정의다
  - 판단은 서비스 한 곳 `MemberActivityPolicy.isManagerAccount(userId)`에 둔다. 010이 같은 판단(`SocialAccountService.isManagerAccount`)을 계획했으므로 먼저 구현하는 쪽이 이 클래스로 만들고 다른 쪽이 가져다 쓴다 (R11)
  - 서버: 마이페이지 API 전부(프로필·닉네임·게임 정보)가 관리자·운영자면 403 `MYPAGE_NOT_ALLOWED`(A201)
  - 프론트: `GET /api/v1/users/me`에 `mypageAllowed`(관리자·운영자면 거짓)를 더한다. 계정 메뉴는 거짓이면 "마이페이지"를 숨기고(US1-AC6), `/account/profile`은 거짓이면 기존 권한 없음 화면을 보인다(US1-AC7, `001-admin-menus` FR-003)
- **이유**: 서버에서 막아야 주소·API 직접 호출도 막힌다(SC-006). 화면은 서버가 준 값으로 보이기만 정한다(010 R9과 같은 방식)
- **대안**: 프론트가 `role`과 `/users/me/permissions`로 판단 — 업무 역할 판단 규칙이 프론트에 생겨 서버와 어긋날 수 있다

## R3. 프로필 저장 위치 (Key Entities)

- **결정**: 키트 `users`는 구조를 바꾸지 않는다. 닉네임은 `users.name` 그대로(`User.rename` 사용). 나머지 프로필은 1:1 새 테이블 `user_profiles`, 플레이 가능 시간대는 `user_profile_play_times`, 게임 정보는 `user_game_profiles`
  - `user_profiles` 행을 만드는 때는 R14. 행이 없으면 기본값(기본 아바타, 빈 값, 비공개)으로 응답한다
  - 닉네임 변경 시각(`nickname_changed_at`)과 닉네임 정규화 값(`nickname_key`)·태그(`nickname_tag`, R4)도 `user_profiles`에 둔다. `user_profiles`는 010 마이그레이션이 `user_id`·`nickname_key`·`nickname_tag`·시각 칸만으로 먼저 만들고, 009가 나머지 칸을 더한다 (R12, R14)
- **이유**: 키트 골격 테이블(`users`)을 넓히면 키트를 다시 적용할 때 충돌하고 공용 파일이 늘어난다. 010도 `users`를 바꾸지 않고 별도 테이블을 둔다(010 data-model). 닉네임은 이미 여러 기능이 `name`으로 보이고 있어(`008-party` FR-027) 칸을 옮기지 않는다
- **대안**: `users`에 칸 추가 — 위 이유. 프로필을 JSON 한 칸에 — 공통코드 검증·조회가 어렵다

## R4. 닉네임 규칙과 고유 태그 (FR-003, 잠정) — 2026-11-19 "중복 불가"에서 바뀜

- **결정**
  - 닉네임은 다른 회원과 겹쳐도 된다. 회원은 `닉네임#태그`로 구별한다. 태그는 숫자 4자리 문자열 `0001`~`9999`
  - 규칙은 한 곳 `NicknamePolicy`에 둔다: 앞뒤 공백 제거 → 2~12자 → 한글(완성형)·영문·숫자·`_`만(`^[가-힣A-Za-z0-9_]{2,12}$`) → 예약어 아님. `key(nickname)` = `toLowerCase(Locale.ROOT)`. **010 구현이 만들고 009가 그대로 쓴다**
  - 예약어(대소문자 무시, 같으면 막음): `admin`, `administrator`, `root`, `system`, `moa`, `운영자`, `관리자`, `운영팀`, `관리팀`. 목록은 `NicknamePolicy` 안의 상수다 (잠정, 팀 확인 때 함께 본다)
  - 저장: `user_profiles.nickname_key`(VARCHAR(50), 위 `key`)와 `nickname_tag`(CHAR(4)), 유일 제약 `uk_user_profiles_nickname_tag (nickname_key, nickname_tag)`. 표시 닉네임은 그대로 `users.name`이다. 키트 `users`는 고치지 않는다. 정규화는 앱이 해서 DB 정렬 규칙에 기대지 않는다
  - 태그 정하기: `NicknameTags.assign(key)` — 그 `key`에서 쓰이지 않은 번호 중 무작위. 회원이 고르지 않는다. 가입(010), 프로필 행이 처음 생길 때(R14), 닉네임을 바꿀 때 부른다. **010 구현이 만들고 009가 그대로 쓴다**
  - 닉네임 바꾸기: `NicknameTags.keepOrAssign(newKey, currentTag)` — 새 `key`에서 지금 태그가 비어 있으면 그대로, 아니면 `assign`. 대소문자만 바꾸면 `key`가 같으므로 그대로. `users.name`(`User.rename`)과 `nickname_key`·`nickname_tag`를 같은 트랜잭션에서 함께 바꾼다
  - 동시에 같은 번호를 고른 경우: 유일 제약 위반(`DataIntegrityViolationException`)이면 그 작업을 새 트랜잭션으로 처음부터 다시 한다(최대 3번). 그래도 실패하면 B201
  - 다 참: 그 `key`의 9999개가 모두 쓰이면 409 `NICKNAME_TAGS_EXHAUSTED`(B201)
  - 길이·글자 오류와 예약어는 V001(칸 오류)
  - 회원이 태그를 직접 고르거나 바꾸는 기능은 두지 않는다 (spec FR-003 [NEEDS CLARIFICATION], 잠정)
- **표시·검색·멘션 형식 (잠정, 팀 확인 — 팀 기획안 `@my_handle`과 다름)**
  - 표시: 기본은 닉네임만. 사람을 골라야 하는 자리(마이페이지·프로필 머리, 회원 검색 결과, 멘션 후보, 사용자 관리 목록·드로어)는 `닉네임#0421`(태그는 흐린 글씨). 이번 009에서 태그를 보이는 곳은 마이페이지 머리뿐이다(US1-AC1). 사용자 관리는 002 "나중에 고칠 곳"
  - 응답: 닉네임을 주는 API는 `nickname`과 `nicknameTag`(없으면 `null`)를 따로 준다. 합친 문자열은 화면이 만든다
  - 검색: 닉네임 일부(대소문자 무시)로 찾고 결과마다 태그를 함께 보인다. `닉네임#0421`을 넣으면 `key`·태그가 같은 회원 하나를 찾는다
  - 멘션: `@닉네임#0421`. 후보에서 고르면 그 형식으로 넣는다. 저장 방식(회원 id로 저장 등)은 멘션 기능 스펙이 정한다
  - 검색·멘션 기능은 이번에 만들지 않는다. 형식만 정한다
- **이유**: 닉네임 겹침을 허용하면 원하는 닉네임을 쓸 수 있고 "이미 쓰는 닉네임" 막힘이 거의 없어진다(가입 화면에서 막히지 않음). 태그를 1:1 프로필 쪽에 두면 키트 `users`를 고치지 않고 DB에서 고유함을 지킨다. 4자리 무작위는 순차보다 같은 닉네임의 가입 순서가 덜 드러난다. 규칙(`NicknamePolicy`)과 태그(`NicknameTags`)를 한 곳에 두어 010 가입·009 변경·002 이름 바꾸기가 같은 판단을 쓴다
- **대안**
  - 닉네임 유일(2026-11-17 잠정안, 이전 R4): 인기 닉네임 경쟁, 대소문자만 다른 이름도 막힘
  - 서버가 주는 공개 번호(무작위 영숫자 8자): 사람이 외우거나 말로 전하기 어렵다
  - 회원이 정하는 핸들 `@my_handle`(팀 기획안 화면): 가입 칸이 하나 늘고 핸들 중복 막힘이 다시 생긴다. 팀 확인 결과에 따라 바뀔 수 있다
  - 키트 `users`에 태그 칸: 키트 테이블을 바꾼다(원칙 III)
  - 순차 번호(`0001`, `0002` …): 닉네임마다 가입 순서와 인기가 드러난다. 다만 기존 회원 채우기(010 마이그레이션)는 SQL로 무작위를 고르기 어려워 id 순서대로 `0001`부터 준다

## R5. 닉네임 변경 제한 (FR-004, 잠정)

- **결정**: `user_profiles.nickname_changed_at`이 비어 있으면 가입 뒤 첫 변경이라 바로 바꾼다(US2-AC6). 값이 있으면 그 시각 + 30일 전에는 409 `NICKNAME_CHANGE_TOO_SOON`(B202)과 다시 바꿀 수 있는 시각(`availableAt`)을 돌려준다(US2-AC4). 바꾸면 지금 시각을 적는다. 같은 값으로 저장하면 아무것도 바꾸지 않고 시각도 그대로다(Edge Cases)
  - 30일은 `ProfileProperties.nicknameCooldown`(기본 `30d`) 설정 값 하나다
- **이유**: 가입 때 닉네임을 정하는 010은 이 칸을 건드리지 않으므로 "가입 뒤 첫 변경"을 따로 기록하지 않아도 된다. 관리자의 드로어 이름 바꾸기(`002-admin-users` FR-007)는 이 제한을 받지 않는다(spec "나중에 고칠 곳" 002)
- **대안**: 변경 이력 테이블 — 지금 필요한 것은 마지막 시각 하나다

## R6. 나이·성별 (FR-007)

- **결정**
  - 출생 연도 `birth_year`(SMALLINT, 1900~올해). 미래 연도는 V001. 응답에는 `birthYear`와 계산한 `age`(올해 − 출생 연도, spec 가정)를 함께 준다
  - 성별은 enum `Gender`: `MALE`, `FEMALE`, `UNDISCLOSED`(밝히지 않음). spec이 "정해 둔 목록"이라 공통코드로 두지 않는다
  - 공개 여부는 칸 하나 `age_gender_public`(잠정, 팀 기획안 토글 하나). 기본 거짓
  - 만 14세 미만은 이번에 막지 않는다(잠정 답 없음, 010과 함께 정함). 출생 연도 범위만 검사한다
- **이유**: 나이를 저장하면 해마다 틀린다(spec Clarifications). 개별 공개로 정해지면 칸 하나를 둘로 나누면 된다(마이그레이션 하나)
- **대안**: 생년월일 — spec은 연도만 받는다

## R7. 디스코드 태그 (FR-008)

- **결정**: `discord_tag` VARCHAR(32), 검증 `^(?!.*\.\.)[a-z0-9_.]{2,32}$`. 저장 전에 앞뒤 공백만 지우고 대소문자는 바꾸지 않는다(대문자는 형식 오류, US3-AC4). 공개 범위 enum `DiscordVisibility`: `PRIVATE`(기본), `FRIENDS`(친구·팔로워만, 잠정), `MEMBERS`(전체 회원)
- **이유**: spec이 새 사용자명 형식을 입력 규칙으로 정했다. 대문자를 조용히 소문자로 바꾸면 회원이 잘못 적은 것을 모른다
- **친구·팔로워(잠정)**: 정의는 친구·팔로우 기능이 정한다. 이번에는 값만 저장하고, 다른 회원에게 보이는 API를 만들지 않으므로(R9) 판단 코드도 두지 않는다

## R8. 게임 정보 (FR-010, FR-011)

- **결정**
  - `user_game_profiles`: 회원·게임마다 한 행(유일 `(user_id, game_code)`), 랭크 코드·포지션 코드·게임 아이디(50자) 선택
  - 검증: 게임은 `GAME` 그룹의 켜진 코드, 랭크는 `<게임>_TIER`, 포지션은 `<게임>_POSITION`의 켜진 코드. 포지션 그룹이 없는 게임(`PUBG`)에 포지션을 보내면 V001. 공통코드 조회는 키트 code 헬퍼의 서비스(`CodeService`)를 쓴다
  - 쓰지 않게 된 코드(Edge Cases): 이미 저장된 값은 그대로 응답한다(이름은 `includeDisabled`로 찾음). 새로 저장하거나 고칠 때는 켜진 코드만 받는다
  - 최대 3개: 추가 때 같은 트랜잭션에서 회원의 `user_profiles` 행을 잠그고(`SELECT … FOR UPDATE`, 없으면 만든 뒤) 개수를 센다. 3개면 409 `GAME_PROFILE_LIMIT`(B203). 같은 게임이 있으면 409 `GAME_PROFILE_DUPLICATED`(B204)
  - 응답에 `count`·`max`(3)를 함께 준다(US5-AC9의 "1/3")
- **전적 사이트 링크**: 서버의 `GameStatsLinks`가 게임 코드별 주소 틀로 `statsUrl`을 만든다. 게임 아이디는 URL 인코딩한다(Edge Cases). 처음 틀(잠정, 구현 때 주소를 다시 확인한다):
  | 게임 | 틀 |
  |---|---|
  | `LOL` | `https://www.op.gg/summoners/kr/{id}` (Riot ID `이름#태그`는 `#`을 `-`로 바꿔 넣는다) |
  | `VALORANT` | `https://tracker.gg/valorant/profile/riot/{id}/overview` (`#`은 인코딩) |
  | `OVERWATCH` | `https://www.overbuff.com/players/{id}` (`#`은 `-`) |
  | `PUBG` | `https://pubg.op.gg/user/{id}` |
  틀이 없는 게임은 `statsUrl`이 `null`이다(US5-AC6)
- **이유**: 링크 규칙을 서버 한 곳에 두면 다른 회원 프로필 화면(나중)과 파티 화면도 같은 링크를 쓴다. 회원이 바꾸지 않는 값이라 공통코드가 아니다(spec FR-011)
- **대안**: 링크 틀을 공통코드 설명 칸에 — 관리자가 바꿀 수 있게 되는데 spec은 moa가 정한다

## R9. 다른 회원이 보는 범위 (FR-013, 잠정)

- **결정**: 이번에 다른 회원 프로필을 보는 API·화면을 만들지 않는다(spec FR-013 "보이는 규칙만 정함"). 규칙은 `ProfileVisibility.view(profile, viewer)` 한 곳에 두어 나중 기능이 그대로 쓴다: 로그인하지 않으면 없음, 나이·성별은 `age_gender_public`일 때만, 디스코드 태그는 `MEMBERS`일 때만(`FRIENDS`는 친구 기능 전까지 아무에게도 안 보임, spec Edge Cases), 나머지는 모두. 단위 테스트로 규칙만 확인한다
- **이유**: spec에 다른 회원 프로필 화면의 AC가 없다(원칙 I). 그러나 공개 여부를 저장하는 이 기능이 규칙의 주인이라 규칙 자체는 여기 둔다
- **대안**: `GET /api/v1/users/{id}/profile`을 지금 만들기 — 쓰는 화면이 없고 AC도 없다

## R10. 아바타 (FR-009)

- **결정**: 기본 아바타는 코드의 enum `DefaultAvatar`(키 목록)와 프론트 정적 이미지 `frontend/public/avatars/<키>.svg`로 둔다. 처음 목록(잠정, 그림은 구현 전에 정함): `default`, `cat`, `dog`, `fox`, `bear`, `rabbit`, `penguin`, `robot`. `GET /api/v1/profile/avatars`가 키 목록을 준다. 저장할 때 목록 밖이면 V001. 저장된 키가 목록에서 빠졌으면 응답에서 `default`로 바꿔 준다(Edge Cases)
- **이유**: spec이 "moa가 정해 둔 묶음, 회원이 더하거나 올릴 수 없다"고 정했다. 업로드가 없으므로 파일 저장소 헬퍼가 필요 없다
- **대안**: 공통코드로 아바타 목록 — 관리자가 키를 더해도 이미지가 없으면 깨진다

## R11. 010과 함께 고치는 곳 (경계)

| 겹치는 곳 | 010 계획 | 009 계획 | 맞추는 방법 |
|---|---|---|---|
| `UserGetResponse` / 프론트 `Me` | `hasPassword`, `socialAccounts`, `socialLinkAllowed` | `avatar`, `mypageAllowed` | 칸만 더하므로 나중에 병합하는 쪽이 앞쪽 칸을 그대로 두고 더한다 |
| 관리자·운영자 판단 | `SocialAccountService.isManagerAccount` | `MemberActivityPolicy.isManagerAccount` | 먼저 구현하는 쪽이 `MemberActivityPolicy`로 만들고 다른 쪽이 쓴다. `socialLinkAllowed`와 `mypageAllowed`는 같은 값이다 |
| 닉네임 규칙·태그 | `NicknamePolicy`, `NicknameTags`, B201(태그 다 참)을 만든다 | 그대로 쓴다 | R4. 010이 먼저다 |
| `user_profiles` 테이블과 가입 때 프로필 행 | 마이그레이션이 최소형(`user_id`, `nickname_key`, `nickname_tag`, 시각)을 만들고 기존 회원을 채운다. 가입 트랜잭션에서 행을 만든다 | 나머지 칸을 더한다(ALTER) | R12·R14. 010이 먼저다 |
| 관리자 이름 바꾸기(`UserAdminService.rename`) | `nickname_key`·태그를 함께 맞춘다 | (없음) | R14 |
| `account-menu.tsx` | `hasPassword` 거짓이면 "비밀번호 변경" 숨김 | 닉네임·아바타, `loginId` 숨김, "마이페이지" 항목 | 같은 파일. 나중 쪽이 앞의 변경을 지키고 더한다. 항목 순서는 마이페이지, 설정, 비밀번호 변경, 로그아웃 |
| 마이그레이션 번호 | 구현 때 dev 마지막 다음 | 구현 때 dev 마지막 다음 (010·004 Phase 6·008 시드 다음) | R12 |

## R12. 마이그레이션 번호

- **결정**: 번호를 정해 두지 않는다. `V<번호>__user_profiles.sql`(`user_profiles` 칸 더하기와 새 테이블 2개), `V<번호>_1__play_time_codes.sql`(공통코드 `PLAY_TIME` 시드)로 적고, 번호는 구현 때 dev 마지막 다음 번호로 한다. moa는 `V2` 대역에서 차례 번호를 쓰고(008 R8), 데이터 시드는 `_1`로 나눈다
- **순서 조건**: 009 시드는 004 Phase 6(`code_groups` 재생성)과 008 공통코드 시드 다음이어야 한다. 시드가 `code_groups`의 `system` 열에 넣고(R13), 테이블 마이그레이션과 시드는 같은 번호를 나눠 쓰므로 009 두 파일 모두 그 뒤다. **009는 010 다음이어야 한다**: `user_profiles`를 010 마이그레이션(`*__social_login.sql`)이 만들고 009는 칸을 더한다(R4·R14, 2026-11-19)
- **이유**
  - 계획 때(2026-11-17)는 008 `V2_7`, 010 `V2_8`, 009 `V2_9`로 정했다. 그 뒤 dev에서 `V2_7`을 004(`V2_7__code_groups_from_code.sql`, 2026-11-17)가 썼고, 004 Phase 6이 다음 번호를 계획에 두어 정해 둔 번호가 모두 어긋났다(2026-11-19). 기능마다 번호를 미리 정하면 앞 기능이 옮길 때마다 따라 옮겨야 한다
  - Flyway는 `outOfOrder` 설정이 없어 기본값(거짓)이다(`application.yaml`). dev에 들어간 번호보다 작은 번호는 나중에 넣을 수 없다(검증 실패). 그래서 009 번호는 004 Phase 6·008 시드 번호보다 커야 하고, 구현 PR을 병합하기 직전에 dev 마지막 번호를 다시 보고 그보다 크게 맞춘다
- **대안**
  - 지금 번호를 다시 정하기: 같은 이유로 다시 어긋난다
  - `V2_7_2` 같은 사이 번호: 앞 기능이 옮길 때 다시 꼬이고, 이미 들어간 번호보다 작으면 `outOfOrder`에 걸린다

## R13. 공통코드 `PLAY_TIME` (FR-012)과 게임 코드 의존

- **결정**
  - 새 그룹 `PLAY_TIME`(이름 "플레이 가능 시간대", 설명 "마이페이지 플레이 가능 시간대 선택지", 쓰는 곳 `009-mypage`). 시드 코드는 data-model "공통코드 시드" 표
  - 그룹 넣는 방식은 `004-admin-codes` FR-002·FR-008(2026-11-18)과 `008-party` R7을 따른다: 기능이 코드에서 읽는 그룹은 시스템 그룹이다. 009 시드 마이그레이션이 `code_groups`에 `PLAY_TIME`을 `system` = true로 넣고, `codes`에 코드 4개를 넣는다
  - `code_groups` 표와 `system` 열은 004 Phase 6(004 tasks T013)이 다시 만든다. 지금 dev는 `V2_7__code_groups_from_code.sql`이 `code_groups`를 지운 상태라 **004 Phase 6이 먼저 dev에 있어야 한다** (R12 순서 조건)
  - `GAME`, `<게임>_TIER`, `<게임>_POSITION`은 **008의 공통코드 시드 마이그레이션을 쓴다**(008도 시스템 그룹으로 넣는다). 009는 같은 그룹·코드를 다시 넣지 않는다. 008이 구현되기 전에 009를 구현하면 그 시드를 009가 가져올지 tasks T002에서 정한다
  - 테스트에서 `DatabaseCleaner`가 `codes`와 `code_groups`의 시드를 지우므로, 008이 둘 `CodeSeed` 지원 코드에 `PLAY_TIME` 그룹(`system` = true)과 코드를 더한다. 004가 `TestCodeGroups`를 지우므로(004 research R10) 그것에 기대지 않는다
- **이유**: 같은 코드를 두 기능이 넣으면 유일 제약에 걸리거나 순서가 어긋난다. 004 FR-002가 "새 그룹을 쓰는 기능이 스펙에 그룹과 초기 코드를 적고 004 FR-001 표에 더한다"고 정한다 → 004 표 갱신은 spec "나중에 고칠 곳"

## R14. 프로필 행을 만드는 때와 010 경계 (FR-003, R4)

- **결정**: 프로필 행은 닉네임을 정하는 때 만든다. 처음 마이페이지에 들어올 때(GET)는 만들지 않는다 (2026-11-19 태그 방식에 맞춰 고침)
  - `010-social-login` 가입: 가입 트랜잭션에서 `users` 행과 함께 `user_profiles` 행(`nickname_key`, `NicknameTags.assign`으로 정한 `nickname_tag`)을 만든다. 010 구현이 한다
  - 기존 회원: 010 마이그레이션(`*__social_login.sql`)이 삭제되지 않은 모든 `users`에 행을 만들고 `nickname_key` = `LOWER(TRIM(name))`, `nickname_tag` = `LPAD(ROW_NUMBER() OVER (PARTITION BY LOWER(TRIM(name)) ORDER BY id), 4, '0')`로 채운다(MariaDB 11.4). 규칙 이전 이름이 겹쳐도 태그가 달라 모두 값을 갖는다
  - 키트가 계정을 만드는 경로(첫 관리자 `UserService.bootstrapAdmin`, 관리자의 계정 만들기 `UserAdminService.create`)는 고치지 않는다. 이 계정은 행 없이(태그 없이) 남는다. 대부분 관리자·운영자라 마이페이지를 쓰지 않고(FR-016), 업무 역할이 없는 회원이면 마이페이지에서 처음 저장할 때 행을 만들고 태그를 정한다(R8의 "없으면 만든 뒤"와 같은 자리)
  - 관리자 이름 바꾸기(`002-admin-users` FR-007, `UserAdminService.rename`): 프로필 행이 있으면 `nickname_key`를 바꾸고 태그를 `keepOrAssign`으로 맞춘다(R4). 규칙(2~12자 등)과 변경 제한(R5)은 받지 않는다. **010 구현이 한다** — 010이 테이블을 만들고 같은 파일(소셜 회원 역할 막기)을 이미 고치므로, 010 병합 뒤 이름 바꾸기로 `nickname_key`가 어긋나는 틈을 두지 않는다. 002 스펙 갱신은 spec "나중에 고칠 곳" 002에 더한다
  - 탈퇴(soft delete)한 회원의 행을 지우거나 태그를 풀어 주는 일은 회원 탈퇴 스펙이 정한다(spec 가정). 그 전까지 탈퇴 회원의 태그는 그 닉네임에서 쓰인 채로 남는다
- **010과의 경계**: 010이 먼저 병합된다(R12). 009 구현은 010의 `user_profiles`·`NicknamePolicy`·`NicknameTags`·가입 때 행 만들기를 그대로 쓰고, 칸만 더한다
- **이유**: 닉네임이 처음 정해지는 때는 가입이고, 태그도 그때 정해야 바로 `닉네임#태그`로 구별된다. 첫 마이페이지 진입 때 만들면 GET이 쓰기가 된다. 기존 회원은 마이그레이션이 한 번에 채운다
- **대안**: 처음 마이페이지 진입 때 만들기 — 위 이유. 키트가 계정을 만드는 경로 전부에서 만들기 — 키트 코드 여러 곳을 고치는데, 그 경로로 생기는 계정은 대부분 마이페이지를 쓰지 않는다

## 열린 질문의 처리 (spec [NEEDS CLARIFICATION] 8곳, 모두 팀 확인 대기. 2026-11-19 태그 질문 2곳 더함)

| 열린 질문 | 잠정으로 설계한 것 | 확정 뒤 바꿀 자리 |
|---|---|---|
| FR-003 닉네임 규칙 | R4: 2~12자, 한글·영문·숫자·`_`, 예약어. 중복 허용 + 고유 태그 | `NicknamePolicy` 한 곳과 프론트 zod 규칙(010 가입도 씀) |
| FR-003 태그 직접 고르기·바꾸기 | R4: 두지 않음 | 두기로 하면 태그 바꾸기 API와 변경 제한 |
| FR-003 표시·검색·멘션 형식 (팀 기획안 `@my_handle`과 다름) | R4: `닉네임#0421`, 응답은 `nickname`·`nicknameTag` 따로 | 핸들 방식으로 가면 R4·`user_profiles` 칸·010 가입 화면을 다시 정한다 |
| FR-004 변경 제한 | R5: 첫 변경 바로, 그 뒤 30일 | `ProfileProperties.nicknameCooldown`과 첫 변경 판단 |
| FR-007 만 14세 미만 | R6: 막지 않음, 출생 연도 1900~올해 | 010 가입 흐름과 함께 정함. 막기로 하면 출생 연도 검증과 가입 화면 |
| FR-007 나이·성별 공개 개별 | R6: 칸 하나 `age_gender_public` | 칸 둘로 나누는 마이그레이션 하나, 응답·폼 |
| FR-013 친구·팔로워 정의 | R7·R9: `FRIENDS` 값만 저장, 지금은 아무에게도 안 보임 | `ProfileVisibility`의 `FRIENDS` 판단 (친구 기능이 생길 때) |
| FR-013 보기 범위 | R9: 규칙만 `ProfileVisibility`에, 보는 API 없음 | `ProfileVisibility`와 그 규칙을 쓰는 화면 |
