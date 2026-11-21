# Contract: 내 정보 응답 추가 칸과 화면 (009)

## GET `/api/v1/users/me` — 칸 추가 (`UserGetResponse`, 프론트 `Me`) [공용]

| 칸 | 형식 | 쓰는 곳 |
|---|---|---|
| `avatar` | string | 계정 바 아바타 (FR-014, US4-AC1). 프로필이 없거나 목록에서 빠진 키면 `default` |
| `mypageAllowed` | boolean | 관리자·운영자(`ADMIN` 계정)면 거짓 → 계정 바 톱니 메뉴에 "마이페이지"를 숨기고 `/account/profile`에서 권한 없음 (FR-016, US1-AC6·AC7) |

- 기존 칸(`id, loginId, name, role, passwordChangeRequired, hasPassword`)은 그대로다. `name`이 닉네임이다. `hasPassword`는 010이 더했다(dev, 2026-11-22)
- `010-social-login` US5(계정 설정의 소셜 연결)가 나중에 `socialAccounts`, `socialLinkAllowed`를 더한다. `mypageAllowed`와 `socialLinkAllowed`는 같은 판단(`MemberActivityPolicy`, 009가 만든다)의 값이다 ([research.md](../research.md) R11)
- 추가 뒤 `npm run gen:api`로 타입을 다시 만든다

## 화면 (프론트)

모두 메뉴로 열지 않는 계정 화면이라 프로그램으로 등록하지 않는다 (`001-admin-menus` FR-008, spec FR-015).

| 경로·부분 | 셸 | 내용 |
|---|---|---|
| `/account/profile` (새) | account (사용자 셸) | `mypageAllowed` 거짓이면 기존 권한 없음 화면. 아니면 프로필 편집 화면: 아바타(고르기 단추 → 목록에서 하나), 닉네임(필수 표시, 따로 "닉네임 바꾸기" 저장, 바꿀 수 없으면 "{날짜}부터 바꿀 수 있어요"), 자기소개·출생 연도(옆에 계산한 나이)·성별·"나이·성별 공개" 토글 하나·디스코드 태그와 공개 범위(비공개, 친구·팔로워만, 전체 회원)·플레이 가능 시간대(여러 개 선택)는 모두 선택 표시. 게임 정보 영역: 제목 옆 "1/3", 게임마다 게임·랭크·포지션·게임 아이디와 전적 사이트 링크(새 창), 고치기·지우기, "게임정보 추가" 단추(3/3이면 꺼짐) (FR-001, FR-017, US1, US3~US6) |
| 게임정보 추가 모달 (새) | — | 게임 선택(이미 있는 게임은 뺌) → 그 게임의 랭크(`<게임>_TIER`)·포지션(`<게임>_POSITION`, 그룹이 없으면 칸 없음) 선택, 게임 아이디. 안내 문구 "랭크는 직접 입력한 값이라 확인되지 않은 값이에요". 고치기도 같은 모달(게임은 고정) (FR-010, US5-AC1·AC4·AC8) |
| 계정 바 (`components/shell/account-bar.tsx`, 사이드바 바닥) [공용] | 두 셸 | 사람 아이콘 자리에 아바타(`avatar`), 이름 자리에 닉네임(`name`). 아이디 줄(툴팁 포함)은 회원(`mypageAllowed`)에게 보이지 않는다. 010의 "`hasPassword`가 참이 아니면 아이디 줄·비밀번호 변경 숨김"은 그대로 둔다. 톱니 메뉴: 화면 모드, (언어), 마이페이지(`mypageAllowed`일 때만, 설정 앞), 설정, 비밀번호 변경(`hasPassword`일 때만). 로그아웃은 따로 단추다 (FR-014, US1-AC3·AC6, `001-admin-menus` FR-005) |

- 저장이 끝나면 `me`와 `profile` 쿼리를 무효화해 계정 바가 새로고침 없이 바뀐다 (FR-005, SC-002)
- 칸 오류는 서버 V001의 `errors[].field`를 그 칸에 보인다. 프론트 zod는 길이·형식만 먼저 막고, 예약어·중복·변경 제한·목록 확인은 서버 응답을 따른다
- 문구는 `frontend/messages/ko.json`의 새 `profile` 묶음과 `shell.mypage`에 둔다 [공용]
- 아바타 이미지는 `frontend/public/avatars/<키>.svg`, 키 → 경로는 `lib/avatars.ts`. 모르는 키는 `default`
