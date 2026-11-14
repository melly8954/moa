# Quickstart: 관리 메뉴와 접근 (admin-menus)

기대 결과의 기준은 [contracts/admin-access.md](./contracts/admin-access.md)와 [data-model.md](./data-model.md)다.

## 자동 검증

Docker가 떠 있어야 한다. `backend/`에서 실행한다 (Windows는 `.\gradlew.bat`).

```bash
./gradlew dbTest --tests "*AdminMenusAcceptanceTest"
```

기대: `US1-AC1`~`US2-AC3` 5개가 통과한다.

## 화면 확인

자동 테스트는 API까지 확인한다. 화면의 안내 문구는 아래로 직접 본다.

1. `backend/`에서 `./gradlew bootRun`, `frontend/`에서 `npm run dev`
2. 시험 계정 `admin`으로 로그인: 사이드바에 홈 다음으로 메뉴 5개가 묶음 없이 사용자 관리, 메뉴 관리, 코드 관리, 접속 이력, 역할 관리 순서로 보이고, 각 화면에 목록이 나온다
3. 시험 계정 `user`로 로그인: 관리 메뉴가 하나도 없다. `/admin/users`로 직접 들어가면 목록 자리에 "이 화면을 볼 권한이 없어요."가 보인다
4. 로그아웃 뒤 `/admin/users`로 들어가면 로그인 화면으로 간다. 로그인하면 그 주소로 돌아온다
