# Research: 마이페이지 (009)

스펙: [spec.md](./spec.md). 각 항목은 결정 / 이유 / 검토한 대안 순서다.
**"잠정"** 표시는 spec의 열린 질문(팀 확인 대기)에 기댄 부분이다. 맨 아래 "열린 질문의 처리"에 확정 뒤 바꿀 자리를 모았다.
`010-social-login`(PR #54, 병합 전)은 `origin/docs/010-google-login`의 문서를 읽기만 해서 경계를 맞췄다. 2026-11-22: 010 구현이 dev에 들어갔고(PR #67), dev의 DB 규칙(`docs/db.md`)에 맞춰 테이블·마이그레이션·공통코드 시드와 010이 이미 만든 것을 이 문서들에서 다시 맞췄다 (R11~R14).

## R1. 화면 경로와 묶음 (FR-001, FR-015)

- **결정**: 마이페이지는 `/account/profile`이다. 키트 계정 화면 묶음 `frontend/src/app/(account)/`에 둔다(`/account/password`와 같은 묶음). 이 묶음의 `layout.tsx`가 `AuthGate`(로그인 확인, 로그인 뒤 돌아오기)와 `AccountShell`(사용자에 맞는 셸)을 이미 하므로 US1-AC2를 따로 만들지 않는다. 계정 바 톱니 메뉴로 여는 계정 화면이라 프로그램으로 등록하지 않는다(`001-admin-menus` FR-008)
- **이유**: 기존 계정 화면과 같은 틀·같은 보호를 그대로 쓴다. 경로 상수는 `lib/next-path.ts`의 `PASSWORD_PATH`·`SETTINGS_PATH` 옆에 `PROFILE_PATH`로 둔다
- **대안**: `/mypage` — 계정 화면 묶음 밖이 되어 `AuthGate`·셸 처리를 다시 해야 한다. `/settings` 안에 넣기 — spec FR-002가 프로필과 로그인 방식(010의 계정 설정)을 나눈다

## R2. 관리자·운영자 막기 (FR-016)

- **결정**
  - "관리자·운영자 계정" = 시스템 역할 `ADMIN`인 계정. 2026-11-21부터 회원(`USER`)은 업무 역할을 받지 않으므로(`003-admin-roles` FR-002) "`ADMIN`이거나 업무 역할을 가진 계정"(spec FR-016의 문구)과 같다 (`specs/README.md` "관리자·운영자 계정의 회원 활동 금지" 줄, `010-social-login` FR-007)
  - 판단은 서비스 한 곳 `MemberActivityPolicy.isManagerAccount(userId)`에 둔다. 010은 이 판단을 따로 만들지 않았다(2026-11-22 dev: 소셜 회원 막기는 `UserAdminService.changeRole`의 A011뿐). 009가 이 클래스를 새로 만든다 (R11)
  - 서버: 마이페이지 API 전부(프로필·닉네임·게임 정보)가 관리자·운영자면 403 `MYPAGE_NOT_ALLOWED`(A201)
  - 프론트: `GET /api/v1/users/me`에 `mypageAllowed`(관리자·운영자면 거짓)를 더한다. 계정 바는 거짓이면 톱니 메뉴의 "마이페이지"를 숨기고(US1-AC6), `/account/profile`은 거짓이면 기존 권한 없음 화면을 보인다(US1-AC7, `001-admin-menus` FR-003)
- **이유**: 서버에서 막아야 주소·API 직접 호출도 막힌다(SC-006). 화면은 서버가 준 값으로 보이기만 정한다(010 R9과 같은 방식)
- **대안**: 프론트가 `role`과 `/users/me/permissions`로 판단 — 업무 역할 판단 규칙이 프론트에 생겨 서버와 어긋날 수 있다

## R3. 프로필 저장 위치 (Key Entities)

- **결정**: 키트 `tb_user`는 구조를 바꾸지 않는다. 닉네임은 `tb_user.name` 그대로(`User.rename` 사용). 나머지 프로필은 1:1 새 테이블 `tb_user_profile`, 플레이 가능 시간대는 `tb_user_profile_play_time`, 게임 정보는 `tb_user_game_profile`
  - `tb_user_profile` 행을 만드는 때는 R14. 행이 없으면 기본값(기본 아바타, 빈 값, 비공개)으로 응답한다
  - 닉네임 변경 시각(`nickname_changed_at`)과 닉네임 정규화 값(`nickname_key`)·태그(`nickname_tag`, R4)도 `tb_user_profile`에 둔다. `tb_user_profile`는 010 마이그레이션이 `user_id`·`nickname_key`·`nickname_tag`·시각 칸만으로 먼저 만들고, 009가 나머지 칸을 더한다 (R12, R14)
- **이유**: 키트 골격 테이블(`tb_user`)을 넓히면 키트를 다시 적용할 때 충돌하고 공용 파일이 늘어난다. 010도 `tb_user`를 바꾸지 않고 별도 테이블을 둔다(010 data-model). 닉네임은 이미 여러 기능이 `name`으로 보이고 있어(`008-party` FR-027) 칸을 옮기지 않는다
- **대안**: `tb_user`에 칸 추가 — 위 이유. 프로필을 JSON 한 칸에 — 공통코드 검증·조회가 어렵다

## R4. 닉네임 규칙과 고유 태그 (FR-003, 잠정) — 2026-11-19 "중복 불가"에서 바뀜

- **결정**
  - 닉네임은 다른 회원과 겹쳐도 된다. 회원은 `닉네임#태그`로 구별한다. 태그는 숫자 4자리 문자열 `0001`~`9999`
  - 규칙은 한 곳 `NicknamePolicy`에 둔다: 앞뒤 공백 제거 → 2~12자 → 한글(완성형)·영문·숫자·`_`만(`^[가-힣A-Za-z0-9_]{2,12}$`) → 예약어 아님. `key(nickname)` = `toLowerCase(Locale.ROOT)`. **010 구현이 만들고 009가 그대로 쓴다**
  - 예약어(대소문자 무시, 같으면 막음): `admin`, `administrator`, `root`, `system`, `moa`, `운영자`, `관리자`, `운영팀`, `관리팀`. 목록은 `NicknamePolicy` 안의 상수다 (잠정, 팀 확인 때 함께 본다)
  - 저장: `tb_user_profile.nickname_key`(VARCHAR(50), 위 `key`)와 `nickname_tag`(CHAR(4)), 유일 제약 `uk_user_profile_nickname_key_nickname_tag (nickname_key, nickname_tag)`. 표시 닉네임은 그대로 `tb_user.name`이다. 키트 `tb_user`는 고치지 않는다. 정규화는 앱이 해서 DB 정렬 규칙에 기대지 않는다
  - 태그 정하기: `NicknameTags.assign(key)` — 그 `key`에서 쓰이지 않은 번호 중 무작위. 회원이 고르지 않는다. 가입(010), 프로필 행이 처음 생길 때(R14), 닉네임을 바꿀 때 부른다. **010 구현이 만들고 009가 그대로 쓴다**
  - 닉네임 바꾸기: `NicknameTags.keepOrAssign(newKey, currentTag)` — 새 `key`에서 지금 태그가 비어 있으면 그대로, 아니면 `assign`. 대소문자만 바꾸면 `key`가 같으므로 그대로. `tb_user.name`(`User.rename`)과 `nickname_key`·`nickname_tag`를 같은 트랜잭션에서 함께 바꾼다
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
- **이유**: 닉네임 겹침을 허용하면 원하는 닉네임을 쓸 수 있고 "이미 쓰는 닉네임" 막힘이 거의 없어진다(가입 화면에서 막히지 않음). 태그를 1:1 프로필 쪽에 두면 키트 `tb_user`를 고치지 않고 DB에서 고유함을 지킨다. 4자리 무작위는 순차보다 같은 닉네임의 가입 순서가 덜 드러난다. 규칙(`NicknamePolicy`)과 태그(`NicknameTags`)를 한 곳에 두어 010 가입·009 변경·002 이름 바꾸기가 같은 판단을 쓴다
- **대안**
  - 닉네임 유일(2026-11-17 잠정안, 이전 R4): 인기 닉네임 경쟁, 대소문자만 다른 이름도 막힘
  - 서버가 주는 공개 번호(무작위 영숫자 8자): 사람이 외우거나 말로 전하기 어렵다
  - 회원이 정하는 핸들 `@my_handle`(팀 기획안 화면): 가입 칸이 하나 늘고 핸들 중복 막힘이 다시 생긴다. 팀 확인 결과에 따라 바뀔 수 있다
  - 키트 `tb_user`에 태그 칸: 키트 테이블을 바꾼다(원칙 III)
  - 순차 번호(`0001`, `0002` …): 닉네임마다 가입 순서와 인기가 드러난다. 다만 기존 회원 채우기(010 마이그레이션)는 SQL로 무작위를 고르기 어려워 id 순서대로 `0001`부터 준다

## R5. 닉네임 변경 제한 (FR-004, 잠정)

- **결정**: `tb_user_profile.nickname_changed_at`이 비어 있으면 가입 뒤 첫 변경이라 바로 바꾼다(US2-AC6). 값이 있으면 그 시각 + 30일 전에는 409 `NICKNAME_CHANGE_TOO_SOON`(B202)과 다시 바꿀 수 있는 시각(`availableAt`)을 돌려준다(US2-AC4). 바꾸면 지금 시각을 적는다. 같은 값으로 저장하면 아무것도 바꾸지 않고 시각도 그대로다(Edge Cases)
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
  - `tb_user_game_profile`: 회원·게임마다 한 행(유일 `uk_user_game_profile_user_id_game_code`), 랭크 코드·포지션 코드·게임 아이디(50자) 선택
  - 검증: 게임은 `GAME` 그룹의 켜진 코드, 랭크는 `<게임>_TIER`, 포지션은 `<게임>_POSITION`의 켜진 코드. 포지션 그룹이 없는 게임(`PUBG`)에 포지션을 보내면 V001. 공통코드 조회는 키트 code 헬퍼의 서비스(`CodeService`)를 쓴다
  - 쓰지 않게 된 코드(Edge Cases): 이미 저장된 값은 그대로 응답한다(이름은 `includeDisabled`로 찾음). 새로 저장하거나 고칠 때는 켜진 코드만 받는다
  - 최대 3개: 추가 때 같은 트랜잭션에서 회원의 `tb_user_profile` 행을 잠그고(`SELECT … FOR UPDATE`, 없으면 만든 뒤) 개수를 센다. 3개면 409 `GAME_PROFILE_LIMIT`(B203). 같은 게임이 있으면 409 `GAME_PROFILE_DUPLICATED`(B204)
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

2026-11-22: 010이 dev에 먼저 들어갔다(PR #67). 아래 "010이 한 것"은 dev에 있는 코드다.

| 겹치는 곳 | 010이 한 것 (dev) | 009가 할 것 |
|---|---|---|
| `UserGetResponse` / 프론트 `Me` | `hasPassword`만 더했다. `socialAccounts`·`socialLinkAllowed`는 계정 설정(US5) 때 더한다 | `hasPassword`를 지키고 `avatar`, `mypageAllowed`를 더한다 |
| 관리자·운영자 판단 | 따로 만들지 않았다. 소셜 회원에게 `ADMIN`을 주는 것만 `UserAdminService.changeRole`이 A011로 막는다 | `MemberActivityPolicy.isManagerAccount`를 새로 만든다 (`role == ADMIN`, R2). 010 US5가 `socialLinkAllowed`를 더할 때 이것을 쓴다 |
| 닉네임 규칙·태그 | `NicknamePolicy`, `NicknameTags`, B201(`NICKNAME_TAGS_EXHAUSTED`)을 만들었다 | 그대로 쓴다 (R4) |
| `tb_user_profile`과 가입 때 프로필 행 | 010 마이그레이션(`*__social_login.sql`)이 최소형(`user_id`, `nickname_key`, `nickname_tag`, 시각)을 만들고 기존 회원(`USER`, 시스템 계정 제외)을 채웠다. 가입 트랜잭션에서 행을 만든다. 엔터티 `UserProfile`(`BaseTimeEntity`) | `ALTER TABLE tb_user_profile ADD COLUMN`으로 칸을 더하고 엔터티에 필드를 더한다 (R12·R14) |
| 관리자 이름 바꾸기(`UserAdminService.rename`) | `nickname_key`·태그를 함께 맞춘다 | 없음 (R14) |
| 계정 바(`components/shell/account-bar.tsx`, dev가 `account-menu.tsx`를 바꾼 것) | `hasPassword`가 참이 아니면 아이디 줄(툴팁 포함)과 톱니 메뉴의 "비밀번호 변경"을 숨긴다 | 사람 아이콘 자리에 아바타, 회원(`mypageAllowed`)에게는 아이디 줄을 숨기고, 톱니 메뉴 맨 앞에 "마이페이지"를 둔다. 010의 숨김을 지킨다 (contracts/users-me-and-screens.md) |
| 마이그레이션 | 010 마이그레이션(`*__social_login.sql`) | `V<다음 번호>__mypage.sql` 한 파일. 010의 `tb_user_profile` 다음 (R12) |

## R12. 마이그레이션 번호

- **결정**: `docs/db.md` "마이그레이션"을 따른다 (2026-11-22)
  - 한 파일 `V<다음 번호>__mypage.sql`에 `tb_user_profile` 칸 더하기, 새 테이블 `tb_user_profile_play_time`·`tb_user_game_profile`, 공통코드 `PLAY_TIME` 시드를 둔다. 기능 하나의 스키마 변경과 그 변경에 필요한 데이터는 파일 하나다. `_1` 같은 점·밑줄 번호는 쓰지 않는다
  - 스펙에는 번호를 적지 않는다. PR 직전에 dev를 병합하고 마지막 번호 + 1로 정한다. CI `migration-numbers`가 겹친 번호·빈 번호를 막는다
  - 머리 주석 `-- moa: 마이페이지 프로필 칸, 플레이 가능 시간대, 게임 정보, 공통코드 PLAY_TIME. 스펙 specs/009-mypage (FR-006~FR-012, data-model.md, research.md R12·R13)`
- **순서 조건**: 010의 `tb_user_profile` 다음이다(010 마이그레이션 `*__social_login.sql`, dev에 있다). 공통코드 표(`tb_code_group`, `tb_code`)는 처음 스키마에 있어 기다릴 것이 없다. 008 공통코드 시드와는 번호 순서 조건이 없다(009 마이그레이션은 `GAME` 계열 행을 읽거나 쓰지 않는다). 다만 게임 정보 기능은 그 시드가 있어야 돈다 (R13)
- **이유**: 기능마다 번호를 미리 정하면 앞 기능이 옮길 때마다 따라 옮겨야 했다(2026-11-17~19, 008·010·009에 미리 정한 번호가 모두 어긋났다). Flyway `outOfOrder`가 꺼져 있어 dev에 들어간 번호보다 작은 번호는 넣을 수 없으므로, 번호는 늘 PR 직전에 정한다
- **대안**: 테이블과 시드를 두 파일로 — 적용 시점이 같아 나눌 이유가 없다(`docs/db.md`: 나누는 것은 적용 시점이 달라야 할 때뿐)

## R13. 공통코드 `PLAY_TIME` (FR-012)과 게임 코드 의존

- **결정**
  - 새 그룹 `PLAY_TIME`(이름 "플레이 가능 시간대", 설명 "마이페이지 플레이 가능 시간대 선택지 (009-mypage)"). 항목은 data-model "공통코드 시드" 표
  - 지금 dev의 공통코드를 따른다 (2026-11-22 확인): 그룹은 `tb_code_group`(`group_code`, `name`, `description`, `sort_order`, `enabled`, 소프트 삭제), 항목은 `tb_code`(`group_code`, `code`, `name`, `sort_order`, `enabled`, 소프트 삭제). 관리자가 코드 관리 화면에서 그룹을 더하고 이름·설명·사용 여부·순서를 고친다(`004-admin-codes` FR-004). 그룹은 지우지 않고 끈다(FR-008). 시스템·일반 그룹 구분(`system` 열)과 쓰는 곳 칸(`used_by`)은 2026-11-20에 없어졌고, 쓰는 곳은 설명에 적는다
  - 넣는 방법: 009 마이그레이션이 `PLAY_TIME` 그룹(활성 행이 없을 때만, `sort_order` = 가장 큰 값 + 1)과 항목 4개(각각 활성 행이 없을 때만)를 넣는다. 있는 행은 고치지 않는다(`docs/db.md` "관리자가 화면에서 바꾸는 데이터", `004-admin-codes` FR-002: 관리자가 이미 만든 그룹이면 그대로 읽는다)
  - 읽기: `CodeService.getCodes(groupCode, includeDisabled)`. 꺼진 그룹이면 화면용 목록이 빈 목록이고, 그룹이 없으면 R001이다(004 FR-008·Edge Cases). 저장 검증은 켜진 항목만 받는다
  - `GAME`, `<게임>_TIER`, `<게임>_POSITION`은 **008의 공통코드 시드를 쓴다**. 009는 같은 그룹·항목을 다시 넣지 않는다. **2026-11-22 dev에 없다**(008 "스펙 작성 중"). 그래서 게임 정보(US5)는 008 시드가 dev에 들어간 뒤에 구현한다. US1~US4·US6은 기다리지 않는다. 그 전에 US5까지 하려면 008 담당과 정한다(tasks T002)
  - 테스트에서 `DatabaseCleaner`가 모든 테이블을 비우므로(`TRUNCATE`) 마이그레이션 시드가 남지 않는다. 009 테스트 지원 코드가 `tb_code_group`·`tb_code`에 `PLAY_TIME`(와 US5면 게임 계열)을 넣는다. 008이 공통코드 시드 지원 코드를 먼저 두면 거기에 더한다 (tasks T048)
- **이유**: 같은 코드를 두 기능이 넣으면 유일 제약에 걸리거나 순서가 어긋난다. 004 FR-002가 "새 그룹을 쓰는 기능이 스펙에 그룹과 초기 코드를 적고 004 FR-001 표에 더한다"고 정한다 → 004 표 갱신은 spec "나중에 고칠 곳"

## R14. 프로필 행을 만드는 때와 010 경계 (FR-003, R4)

- **결정**: 프로필 행은 닉네임을 정하는 때 만든다. 처음 마이페이지에 들어올 때(GET)는 만들지 않는다 (2026-11-19 태그 방식에 맞춰 고침)
  - `010-social-login` 가입: 가입 트랜잭션에서 `tb_user` 행과 함께 `tb_user_profile` 행(`nickname_key`, `NicknameTags.assign`으로 정한 `nickname_tag`)을 만든다. 010 구현이 한다
  - 기존 회원: 010 마이그레이션(`*__social_login.sql`)이 삭제되지 않은 회원(`role = 'USER'`, 시스템 계정 제외)에 행을 만들고 `nickname_key` = `LOWER(TRIM(name))`, `nickname_tag` = `LPAD(ROW_NUMBER() OVER (PARTITION BY LOWER(TRIM(name)) ORDER BY id), 4, '0')`로 채운다(MariaDB 11.4). 규칙 이전 이름이 겹쳐도 태그가 달라 모두 값을 갖는다
  - 키트가 계정을 만드는 경로(첫 관리자 `UserService.bootstrapAdmin`, 관리자의 계정 만들기 `UserAdminService.create`)는 고치지 않는다. 이 계정은 행 없이(태그 없이) 남는다. 관리자 계정(`ADMIN`)은 마이페이지를 쓰지 않고(FR-016), 관리자가 만든 회원(`USER`)이면 마이페이지에서 처음 저장할 때 행을 만들고 태그를 정한다(R8의 "없으면 만든 뒤"와 같은 자리)
  - 관리자 이름 바꾸기(`002-admin-users` FR-007, `UserAdminService.rename`): 프로필 행이 있으면 `nickname_key`를 바꾸고 태그를 `keepOrAssign`으로 맞춘다(R4). 변경 제한(R5)은 받지 않는다. **010이 dev에 구현했다**(프로필 행이 있으면 `NicknamePolicy` 규칙도 검사한다, 010 research R14). 002 스펙 갱신은 spec "나중에 고칠 곳" 002에 더한다
  - 탈퇴(soft delete)한 회원의 행을 지우거나 태그를 풀어 주는 일은 회원 탈퇴 스펙이 정한다(spec 가정). 그 전까지 탈퇴 회원의 태그는 그 닉네임에서 쓰인 채로 남는다
- **010과의 경계**: 010이 먼저 병합됐다(2026-11-22, PR #67). 009 구현은 010의 `tb_user_profile`·`NicknamePolicy`·`NicknameTags`·가입 때 행 만들기를 그대로 쓰고, 칸만 더한다
- **이유**: 닉네임이 처음 정해지는 때는 가입이고, 태그도 그때 정해야 바로 `닉네임#태그`로 구별된다. 첫 마이페이지 진입 때 만들면 GET이 쓰기가 된다. 기존 회원은 마이그레이션이 한 번에 채운다
- **대안**: 처음 마이페이지 진입 때 만들기 — 위 이유. 키트가 계정을 만드는 경로 전부에서 만들기 — 키트 코드 여러 곳을 고치는데, 그 경로로 생기는 계정은 대부분 마이페이지를 쓰지 않는다

## 열린 질문의 처리 (spec [NEEDS CLARIFICATION] 8곳, 모두 팀 확인 대기. 2026-11-19 태그 질문 2곳 더함)

| 열린 질문 | 잠정으로 설계한 것 | 확정 뒤 바꿀 자리 |
|---|---|---|
| FR-003 닉네임 규칙 | R4: 2~12자, 한글·영문·숫자·`_`, 예약어. 중복 허용 + 고유 태그 | `NicknamePolicy` 한 곳과 프론트 zod 규칙(010 가입도 씀) |
| FR-003 태그 직접 고르기·바꾸기 | R4: 두지 않음 | 두기로 하면 태그 바꾸기 API와 변경 제한 |
| FR-003 표시·검색·멘션 형식 (팀 기획안 `@my_handle`과 다름) | R4: `닉네임#0421`, 응답은 `nickname`·`nicknameTag` 따로 | 핸들 방식으로 가면 R4·`tb_user_profile` 칸·010 가입 화면을 다시 정한다 |
| FR-004 변경 제한 | R5: 첫 변경 바로, 그 뒤 30일 | `ProfileProperties.nicknameCooldown`과 첫 변경 판단 |
| FR-007 만 14세 미만 | R6: 막지 않음, 출생 연도 1900~올해 | 010 가입 흐름과 함께 정함. 막기로 하면 출생 연도 검증과 가입 화면 |
| FR-007 나이·성별 공개 개별 | R6: 칸 하나 `age_gender_public` | 칸 둘로 나누는 마이그레이션 하나, 응답·폼 |
| FR-013 친구·팔로워 정의 | R7·R9: `FRIENDS` 값만 저장, 지금은 아무에게도 안 보임 | `ProfileVisibility`의 `FRIENDS` 판단 (친구 기능이 생길 때) |
| FR-013 보기 범위 | R9: 규칙만 `ProfileVisibility`에, 보는 API 없음 | `ProfileVisibility`와 그 규칙을 쓰는 화면 |
