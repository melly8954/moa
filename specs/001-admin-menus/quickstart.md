# Quickstart: 관리 메뉴와 접근 (admin-menus)

기대 결과의 기준은 [contracts/admin-access.md](./contracts/admin-access.md)와 [data-model.md](./data-model.md)다.

## 자동 검증

Docker가 떠 있어야 한다. `backend/`에서 실행한다 (Windows는 `.\gradlew.bat`).

```bash
./gradlew dbTest --tests "*AdminMenusAcceptanceTest"
```

기대: `US1-AC1`~`US2-AC3` 5개가 통과한다.

셸 나누기(US1-AC1의 첫 화면·사이드바, US2-AC1, US2-AC4)는 `frontend/`의 e2e로 본다: `npx playwright test e2e/menu.spec.ts e2e/auth.spec.ts`

## 화면 확인

자동 테스트는 API까지 확인한다. 화면의 안내 문구는 아래로 직접 본다.

1. `backend/`에서 `./gradlew bootRun`, `frontend/`에서 `npm run dev`
2. 시험 계정 `admin`으로 로그인: 사용자 관리 화면이 열리고, 사이드바에 홈 없이 메뉴 6개가 묶음 없이 사용자 관리, 메뉴 관리, 코드 관리, 접속 이력, 역할 관리, 공지 관리 순서로 보이며, 각 화면에 목록이 나온다. 계정 메뉴의 설정·비밀번호 변경·알림 화면도 관리 셸 안에서 열린다
3. 시험 계정 `user`로 로그인: 사용자 셸 첫 화면이 열리고 사이드바에 홈, 공지만 있다. `/admin/users`로 직접 들어가면 목록 자리에 "이 화면을 볼 권한이 없어요."가 보인다
4. 로그아웃 뒤 `/admin/users`로 들어가면 로그인 화면으로 간다. 로그인하면 그 주소로 돌아온다
