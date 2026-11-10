# Quickstart: 운영자 역할 (admin-roles)

기대 결과의 기준은 [contracts/admin-roles.md](./contracts/admin-roles.md)다.

## 자동 검증

Docker가 떠 있어야 한다. `backend/`에서 실행한다 (Windows는 `.\gradlew.bat`).

```bash
./gradlew dbTest --tests "*AdminRolesAcceptanceTest" --tests "*AdminMenusAcceptanceTest"
```

기대: 003의 `US1-AC1`~`US1-AC3`과, 시드 복원을 옮긴 뒤의 001 AC 5개가 통과한다.

## 화면 확인

1. `backend/`에서 `./gradlew bootRun`, `frontend/`에서 `npm run dev`
2. 시험 계정 `admin`으로 로그인해 역할 관리에서 "보안 담당" 역할을 만들고 접속 이력 권한만 묶는다
3. 사용자 관리에서 시험 계정 `user`의 "업무 역할"에 그 역할을 준다
4. `user`로 로그인한다: "관리" 아래 접속 이력만 보이고 목록이 열린다. `/admin/users`로 직접 들어가면 목록 자리에 "이 화면을 볼 권한이 없어요."가 보인다
5. `admin`으로 그 역할을 지우면, `user`는 다음 요청부터 접속 이력도 쓰지 못한다
