# Quickstart: 마이페이지 (009) 확인 안내

구현 뒤 기능이 스펙대로 도는지 확인하는 순서다. API 모양은 [contracts/profile-api.md](./contracts/profile-api.md), 화면은 [contracts/users-me-and-screens.md](./contracts/users-me-and-screens.md), 데이터는 [data-model.md](./data-model.md).

## 준비

1. 공통코드 시드가 있어야 한다: `GAME`·`<게임>_TIER`·`<게임>_POSITION`(`008-party`의 공통코드 시드 마이그레이션), `PLAY_TIME`(이 기능의 `V<번호>_1__play_time_codes.sql`). 둘 다 004 Phase 6이 다시 만든 `code_groups`에 시스템 그룹으로 들어간다. 004 Phase 6이나 008이 아직 병합되지 않았으면 tasks T002에서 정한 방법을 따른다 (research R13)
2. 백엔드 `backend/`에서 `.\gradlew.bat bootRun` (local 프로필, 시험 계정 `admin`·`user`), 프론트 `frontend/`에서 `npm run dev`
3. 칸 추가 뒤 프론트 타입: `npm run gen:api`

## 수동 확인 (스펙 AC와 대응)

| 순서 | 할 일 | 기대 결과 | AC |
|---|---|---|---|
| 1 | 로그아웃 상태로 `/account/profile` | 로그인 화면 → 로그인하면 마이페이지 | US1-AC2 |
| 2 | `user`로 계정 메뉴 | 닉네임(이름)·기본 아바타, 아이디 없음, "마이페이지" 항목 | US1-AC3·AC4 |
| 3 | 마이페이지 | 모든 항목이 비어 있음으로 보이고, 닉네임만 필수 표시, 로그인 방식·소셜 연결 없음 | US1-AC1·AC5·AC8 |
| 4 | 닉네임을 `새닉네임`으로 바꿈 | 저장 안내, 계정 메뉴 즉시 바뀜 | US2-AC1·AC6 |
| 5 | 바로 다시 바꿈 | "{30일 뒤}부터 바꿀 수 있어요" | US2-AC4 |
| 6 | (DB에서 `nickname_changed_at`을 비운 뒤) `a`, 13자, `닉네임!`, `admin`으로 저장 | 각각 칸 오류 | US2-AC3 |
| 7 | `admin` 계정의 이름과 대소문자만 다른 닉네임 | 이미 쓰는 닉네임 | US2-AC5 |
| 8 | 4의 회원이 들어간 파티 멤버 목록 (008 구현 뒤) | 새 닉네임 | US2-AC2 |
| 9 | 자기소개 501자 | 칸 오류. 500자는 저장 | US3-AC1 |
| 10 | 출생 연도 2000, 성별 여성, 공개 토글 켬 | 저장, 나이 표시, 둘 다 공개. 내년 연도는 칸 오류 | US3-AC2 |
| 11 | 디스코드 `moa_user.01`, 공개 범위 전체 회원 / `Moa#1234` / `a..b` | 첫째 저장, 나머지 형식 안내 | US3-AC3·AC4 |
| 12 | 3·9~11의 값을 모두 지우고 저장 | 비어 있음 | US3-AC5 |
| 13 | 아바타를 `cat`으로 | 마이페이지·계정 메뉴에 새 아바타 | US4-AC1 |
| 14 | API로 아바타 `unknown` 저장 | V001 | US4-AC2 |
| 15 | 게임정보 추가 → 리그 오브 레전드, 골드, 미드, 아이디 | 저장, "1/3", op.gg 링크(새 창), 모달에 랭크 미확인 안내 | US5-AC1·AC6·AC8·AC9 |
| 16 | 배그 추가 | 포지션 칸 없음 | US5-AC4 |
| 17 | 리그 오브 레전드를 다시 추가 | 목록에 없음 / API는 B204 | US5-AC3 |
| 18 | 셋째 게임 추가 뒤 넷째 | "3/3", 추가 단추 꺼짐 / API는 B203 | US5-AC2·AC9 |
| 19 | API로 `LOL`에 `VALORANT`의 랭크 코드 | V001 | US5-AC5 |
| 20 | 한 게임 지움 | 목록에서 빠지고 추가 단추 켜짐 | US5-AC7 |
| 21 | 플레이 가능 시간대 둘 고름 / 모두 풂 | 저장 / 비어 있음 | US6-AC1·AC2 |
| 22 | `admin`으로 계정 메뉴, `/account/profile`, API | 항목 없음, 권한 없음 화면, 403 A201 | US1-AC6·AC7, SC-006 |

## 자동 검사

- 빠른 검사: 백엔드 `.\gradlew.bat build`, 프론트 `npm run check`
- 인수 테스트 `*MyPageAcceptanceTest`(원칙 II: AC마다 하나, 이름에 AC 번호)는 로컬에서 돌리지 않고 PR CI로 본다 (CLAUDE.md). 공통코드 시드는 테스트 지원 코드가 되돌린다 (research R13)
- 화면만의 AC(US1-AC8, US5-AC8·AC9의 표시)는 프론트 단위 테스트, e2e는 핵심 흐름(닉네임 바꾸기 → 계정 메뉴 반영)만
