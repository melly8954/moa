# Quickstart: 이용약관·개인정보처리방침 공개 페이지 (terms-privacy)

기대 결과의 기준은 [contracts/api.md](./contracts/api.md), [contracts/screens.md](./contracts/screens.md), [data-model.md](./data-model.md)다.

## 자동 검증

로컬에서는 빠른 검사만 돌린다. 인수 테스트(`dbTest`)와 화면 검사(e2e)는 PR CI가 돌린다 (`CLAUDE.md` 로컬 검사).

`backend/`에서 `./gradlew build`(Windows는 `.\gradlew.bat`), `frontend/`에서 `npm run check`.

기대:

- `build`: `LegalServiceTest`(버전 고르기, 시행일 당일 전환), `LegalCatalogTest`(실제 문안이 배포 전 검사 통과)
- PR CI `dbTest`: `LegalAcceptanceTest`(contracts/api.md "AC와 확인 방법")
- PR CI e2e `legal`(3폭, 가로 넘침·접근성)과 고친 `social-login`

## 화면 확인

1. `backend/`에서 `./gradlew bootRun`, `frontend/`에서 `npm run dev`
2. 로그인하지 않은 채 `/sign-in`: 맨 아래 푸터 줄 "이용약관 · 개인정보처리방침"(처리방침 굵게). 누르면 각 문서가 같은 탭에서 열린다
3. `/privacy`: 머리 줄(moa, 이용약관, 개인정보처리방침), 제목, 시행일·버전, 목차, 보관 기간 표. 휴대폰 폭으로 줄이면 표만 가로로 스크롤된다. 인쇄 미리 보기에서 머리 줄과 안내 띠가 없다
4. 시험 계정 `user`로 로그인: 사용자 셸 본문 맨 아래에 푸터 줄. 본문이 짧은 화면에서도 화면 맨 아래에 붙고, 사이드바를 접거나 휴대폰 폭으로 줄여도 같은 자리다
5. 시험 계정 `admin`으로 로그인: 관리 셸에는 푸터 줄이 없다. 주소창에 `/privacy`를 넣으면 열린다. 머리 줄의 moa를 누르면 관리 셸로 간다
6. 처음 소셜 가입(010) 닉네임 화면: 약관 칸마다 전문이 상자 안에 바로 보이고 상자 안에서 스크롤된다. "더보기"·"개인정보처리방침 보기"와 맨 아래 푸터 줄은 새 탭으로 열리고 입력하던 화면은 그대로다

## 문안 대조 (FR-009, SC-003)

처리방침 문안이나 `docs/privacy.md`를 고치는 PR에서 사람이 한다.

- 처리방침의 보관 기간 표와 `docs/privacy.md` "보관 기간 표"의 항목·기간이 같은지
- 다르면 `docs/privacy.md`를 먼저 고치고, 처리방침은 새 버전(새 시행일)으로 더한다. 예전 버전은 지우지 않는다
