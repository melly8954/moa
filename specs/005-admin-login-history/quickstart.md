# Quickstart: 접속 이력 (admin-login-history)

기대 결과의 기준은 [contracts/admin-login-history.md](./contracts/admin-login-history.md)다.

## 자동 검증

Docker가 떠 있어야 한다. `backend/`에서 실행한다 (Windows는 `.\gradlew.bat`).

```bash
./gradlew dbTest --tests "*AdminLoginHistoryAcceptanceTest"
```

기대: `US1-AC1`과 `FR-002` 테스트가 통과한다.

## 화면 확인

1. `backend/`에서 `./gradlew bootRun`, `frontend/`에서 `npm run dev`
2. 로그인 화면에서 시험 계정 `user`로 비밀번호를 한 번 틀리고, 맞게 다시 로그인한 뒤 로그아웃한다
3. 시험 계정 `admin`으로 로그인해 접속 이력을 열고 아이디 `user`로 찾는다: 성공과 실패가 최신순으로 접속 IP, 기기와 함께 보인다

## 운영 배포 때 확인

- 운영 환경의 `.env`에 `LOGIN_HISTORY_RETENTION_DAYS`가 365로 남아 있지 않은지 본다. 있으면 지우거나 90으로 바꾼다 ([research.md](./research.md) R2)
