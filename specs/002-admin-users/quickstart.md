# Quickstart: 관리자·운영자 계정 (admin-users)

기대 결과의 기준은 [contracts/admin-accounts.md](./contracts/admin-accounts.md)다.

## 자동 검증

Docker가 떠 있어야 한다. `backend/`에서 실행한다 (Windows는 `.\gradlew.bat`).

```bash
./gradlew dbTest --tests "*AdminUsersAcceptanceTest"
```

기대: `US1-AC1`, `US2-AC1`이 통과한다.

## 화면 확인

자동 테스트는 API까지 확인한다. 화면의 안내와 창은 아래로 직접 본다.

1. `backend/`에서 `./gradlew bootRun`, `frontend/`에서 `npm run dev`
2. 시험 계정 `admin`으로 로그인해 사용자 관리에서 사용자를 만든다: 만들기 드로어 안에 임시 비밀번호가 보이고, 닫으면 다시 볼 수 없다
3. 로그아웃하고 새 계정과 임시 비밀번호로 로그인한다: 비밀번호 변경 화면에 안내가 보이고, 다른 화면으로 갈 수 없다
4. 비밀번호를 바꾸면 화면으로 돌아간다. 역할을 받지 않았으므로 관리 메뉴는 보이지 않는다
5. 다시 `admin`으로 로그인해 사용자 관리를 연다: 업무 역할 열이 있고 관리자 줄은 "모든 권한", 역할이 없는 줄은 비어 있다. 줄에 "관리" 메뉴 단추는 없다
6. 아이디를 누르면 드로어가 열린다: 업무 역할은 보기만 하고 역할 관리 링크가 있다. 내 줄은 시스템 역할·잠그기·삭제가 꺼져 있다
7. 역할 관리에서 그 사용자에게 역할을 준 뒤 사용자 관리로 돌아오면 업무 역할 열에 보인다

첫 관리자는 빈 DB에 환경 변수 `BOOTSTRAP_ADMIN_LOGIN_ID`, `BOOTSTRAP_ADMIN_PASSWORD`를 주고 띄우면 생긴다. 만든 뒤에는 그 환경 변수를 지운다.
