# Quickstart: 배너·팝업 관리 (banner-popup)

기대 결과의 기준은 [contracts/admin-api.md](./contracts/admin-api.md), [contracts/admin-screens.md](./contracts/admin-screens.md), [data-model.md](./data-model.md)다.

## 자동 검증

로컬에서는 빠른 검사만 돌린다. 인수 테스트(`dbTest`)와 화면 검사(e2e)는 작업 브랜치 → `dev` PR의 CI가 돌린다 (`CLAUDE.md` 로컬 검사).

`backend/`에서 실행한다 (Windows는 `.\gradlew.bat`).

```bash
./gradlew build
```

`frontend/`에서 실행한다.

```bash
npm run check
```

PR CI에서 기대하는 것:

- `BannerPopupAcceptanceTest`의 US1·US2 AC (US1-AC10과 화면 쪽 동작은 e2e)
- 마케팅 메뉴를 더한 `AdminMenusAcceptanceTest`
- e2e `banners`·`popups`: 배너를 이미지와 함께 등록하고 미리보기를 본다, 목록에서 둘을 골라 끄고 지운다 (3폭, 가로 넘침·접근성)

## 화면 확인

1. `backend/`에서 `./gradlew bootRun`, `frontend/`에서 `npm run dev`
2. 시험 계정 `admin`으로 로그인: 사이드바가 운영, 마케팅, 설정 순서이고 마케팅을 펴면 배너 관리, 팝업 관리가 보인다
3. 배너 관리에서 넓은 화면 이미지만 올려 배너를 등록한다 (SC-001, 1분 안). 드로어 미리보기의 좁은 화면 틀에 넓은 화면 이미지가 보인다. 좁은 화면 이미지를 올리면 바뀐다
4. 시작 일시를 내일로 한 배너를 하나 더 등록한다: 목록 상태가 "예정"이다. 끝 일시를 어제로 한 배너는 기본 목록에 없고 상태 "끝남"·"전체"로 거르면 보인다
5. 배너 둘을 체크해 끄기 → 둘 다 "꺼짐", 다시 켜기 → 기간에 맞는 상태. 둘을 체크해 삭제 → 확인 뒤 목록에서 빠지고 고른 것이 풀린다
6. 팝업 관리에서 이미지가 든 팝업을 등록하고, 같은 일괄 처리를 해 본다
7. 권한 관리에서 배너 관리 권한만 준 운영자로 로그인: 배너 관리는 열리고 팝업 관리는 흐리게 자물쇠가 붙는다
