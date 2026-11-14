# Quickstart: 공지사항 (notices)

기대 결과의 기준은 [contracts/notices-api.md](./contracts/notices-api.md), [contracts/notices-screens.md](./contracts/notices-screens.md), [data-model.md](./data-model.md)다.

## 자동 검증

Docker가 떠 있어야 한다. `backend/`에서 실행한다 (Windows는 `.\gradlew.bat`).

```bash
./gradlew dbTest --tests "*NoticesAcceptanceTest" --tests "*AdminMenusAcceptanceTest" --tests "*RichTextImageAcceptanceTest"
./gradlew build
```

기대: `NoticesAcceptanceTest`의 `US1-AC1`~`US3-AC3` 14개, 메뉴 6개로 고친 `AdminMenusAcceptanceTest`, 키트 rich-text 테스트가 통과한다. `build`는 `HtmlSanitizerTest`를 포함한다.

`frontend/`에서 실행한다.

```bash
npm run check
npm run test:e2e -- notices rich-text
```

기대: 단위 테스트와 공지·rich-text 화면 검사(3폭, 가로 넘침·접근성)가 통과한다.

## 화면 확인

자동 테스트는 API와 가로챈 화면까지 확인한다. 실제 서버로 아래를 직접 본다.

1. `backend/`에서 `./gradlew bootRun`, `frontend/`에서 `npm run dev`
2. 시험 계정 `admin`으로 로그인: 사이드바 최상위 6번째에 "공지 관리"가 보인다
3. 공지 관리에서 이미지 하나가 든 공지를 써서 게시한다 (SC-001, 2분 안). 하나는 임시 저장한다
4. 로그아웃한 채 `/notices`: 표에 게시한 공지만 번호·제목·게시일로 보이고(고정 공지는 번호 대신 고정 표시), 상세에 서식과 이미지가 보인다. 임시 저장 공지는 없다
5. 시험 계정 `user`로 로그인: 사이드바 "공지" 메뉴 옆에 점이 보인다. "공지" 메뉴로 목록을 연 뒤 돌아오면 점이 없다. 사이드바를 접어도 아이콘 위에 점이 보인다
6. `admin`으로 공지를 고정하고, 고치고, 지운다: 사용자 목록 순서와 내용이 따라 바뀌고, 지운 공지 상세는 없는 공지 안내가 보인다
