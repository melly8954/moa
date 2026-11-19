# Contract: 마이페이지 API (009)

모든 경로는 로그인 필요다(기본 보안 규칙, `SecurityConfig` 수정 없음). 로그인하지 않으면 기존 401 A001.
관리자·운영자 계정(`ADMIN` 또는 업무 역할 보유)이 부르면 아래 모든 API가 403 A201이다 (spec FR-016, [research.md](../research.md) R2).
검증 실패는 기존 400 V001과 칸별 오류(`errors[].field`)로 돌려준다.

## 오류 코드 (`ErrorCode`에 더함, 번호는 구현 때 다시 확인)

| 코드 | 이름 | HTTP | 언제 |
|---|---|---|---|
| A201 | `MYPAGE_NOT_ALLOWED` | 403 | 관리자·운영자 계정이 마이페이지 API를 부름 (FR-016, US1-AC7) |
| B201 | `NICKNAME_IN_USE` | 409 | 다른 회원이 쓰는 닉네임(대소문자 무시) (FR-003, US2-AC5). `010-social-login` 가입도 같은 코드를 쓴다 |
| B202 | `NICKNAME_CHANGE_TOO_SOON` | 409 | 마지막 변경 + 30일 전 (FR-004, US2-AC4). 응답 `data.availableAt` |
| B203 | `GAME_PROFILE_LIMIT` | 409 | 게임 정보가 이미 3개 (FR-010, US5-AC2) |
| B204 | `GAME_PROFILE_DUPLICATED` | 409 | 같은 게임이 이미 있음 (FR-010, US5-AC3) |

- 번호 대역: 008이 `A1xx`·`B1xx`, 010이 `A008`~`A017`을 계획했다. 009는 `A2xx`·`B2xx`를 쓴다 (008 R9의 "접두 글자 + 기능별 백 단위" 방식)
- 닉네임 길이·글자·예약어, 출생 연도, 성별, 디스코드 형식, 아바타 키, 목록 밖 공통코드는 V001 칸 오류다

## GET `/api/v1/users/me/profile` — 내 프로필

- 200
  ```json
  {
    "nickname": "string",
    "avatar": "default",
    "bio": "string | null",
    "birthYear": 2000,
    "age": 26,
    "gender": "MALE | FEMALE | UNDISCLOSED | null",
    "ageGenderPublic": false,
    "discordTag": "string | null",
    "discordVisibility": "PRIVATE | FRIENDS | MEMBERS",
    "playTimes": [ { "code": "WEEKDAY_EVENING", "name": "평일 저녁" } ],
    "nicknameChangeAvailableAt": "2026-12-17T10:00:00 | null",
    "games": {
      "count": 1,
      "max": 3,
      "items": [
        {
          "gameCode": "LOL", "gameName": "리그 오브 레전드",
          "tierCode": "GOLD", "tierName": "골드",
          "positionCode": "MID", "positionName": "미드",
          "gameAccountId": "이름#KR1",
          "statsUrl": "https://… | null"
        }
      ]
    }
  }
  ```
- 프로필 행이 없으면 기본값(아바타 `default`, 빈 값, `ageGenderPublic` 거짓, `discordVisibility` `PRIVATE`) (US1-AC1·AC4)
- `age`는 `birthYear`가 있을 때만. `nicknameChangeAvailableAt`은 지금 바꿀 수 없을 때만 값이 있다 (FR-004)
- 꺼진 공통코드의 값도 이름과 함께 그대로 보인다 (spec Edge Cases)

## PUT `/api/v1/users/me/profile` — 프로필 저장 (닉네임·게임 정보 제외)

- 요청 (모든 칸을 보낸다. `null`이나 빈 배열은 "비움")
  ```json
  {
    "avatar": "cat",
    "bio": "string | null",
    "birthYear": 2000,
    "gender": "FEMALE",
    "ageGenderPublic": true,
    "discordTag": "moa_user.01",
    "discordVisibility": "MEMBERS",
    "playTimes": ["WEEKDAY_EVENING", "WEEKEND_ALL_DAY"]
  }
  ```
- 검증 (V001): `avatar`는 아바타 목록 키, `bio` 500자, `birthYear` 1900~올해, `gender`·`discordVisibility`는 목록 값, `discordTag`는 `^(?!.*\.\.)[a-z0-9_.]{2,32}$`, `playTimes`는 켜진 `PLAY_TIME` 코드(겹치면 하나로)
- `discordVisibility`를 보내지 않으면 `PRIVATE` (US3-AC3)
- 200 위 GET과 같은 모양 (US3-AC1~AC5, US4-AC1·AC2, US6-AC1·AC2)

## PUT `/api/v1/users/me/nickname` — 닉네임 바꾸기

- 요청 `{ "nickname": "string" }`
- 처리: 앞뒤 공백 제거 → 같은 값이면 그대로 200(시각 안 바뀜) → 규칙(V001) → 중복(B201) → 변경 제한(B202) → `users.name` 바꾸기, `nickname_changed_at` = 지금
- 200 `{ "nickname": "string", "nicknameChangeAvailableAt": "…" }` (US2-AC1·AC6)
- 409 B202 응답의 `data.availableAt`으로 화면이 "언제부터"를 보인다 (US2-AC4)

## POST `/api/v1/users/me/games` — 게임 정보 더하기 (게임정보 추가 모달)

- 요청 `{ "gameCode": "LOL", "tierCode": "GOLD | null", "positionCode": "MID | null", "gameAccountId": "string | null" }`
- 검증 (V001): `gameCode`는 켜진 `GAME`, `tierCode`는 켜진 `<gameCode>_TIER`, `positionCode`는 켜진 `<gameCode>_POSITION`(그룹이 없으면 반드시 null), `gameAccountId` 50자
- 409 B203(3개), B204(같은 게임)
- 201 GET의 `games`와 같은 모양 (US5-AC1·AC2·AC3·AC4·AC5·AC9)

## PUT `/api/v1/users/me/games/{gameCode}` — 게임 정보 고치기

- 요청 `{ "tierCode": …, "positionCode": …, "gameAccountId": … }` (게임은 바꾸지 않는다. 바꾸려면 지우고 더한다)
- 검증은 POST와 같다. 꺼진 코드로 남아 있던 값을 그대로 보내면 V001 (spec Edge Cases "고쳐 저장하려면 쓰는 값으로")
- 404 R001 그 게임 정보 없음
- 200 `games`

## DELETE `/api/v1/users/me/games/{gameCode}` — 게임 정보 지우기

- 204. 없으면 204 그대로 (US5-AC7)

## GET `/api/v1/profile/avatars` — 기본 아바타 목록

- 200 `{ "avatars": ["default", "cat", "dog", …] }` (순서대로, research R10). 이미지는 프론트 `/avatars/<키>.svg`
- 로그인 필요. 관리자·운영자도 부를 수 있다(목록일 뿐이라 막지 않음)

## 공통코드 조회 (키트, 변경 없음)

- 화면의 선택지는 기존 `GET /api/v1/codes/{groupCode}`로 읽는다: `GAME`, `<게임>_TIER`, `<게임>_POSITION`, `PLAY_TIME`

## 만들지 않는 것

- 다른 회원 프로필 조회 API: spec FR-013이 화면을 범위 밖으로 둔다. 보이는 규칙은 `ProfileVisibility`에만 둔다 (research R9)
- 이미지 올리기 API (FR-009)
