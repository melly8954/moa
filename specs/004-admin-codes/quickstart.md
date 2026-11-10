# Quickstart: 공통코드 (admin-codes)

기대 결과의 기준은 [contracts/admin-codes.md](./contracts/admin-codes.md)다.

## 자동 검증

Docker가 떠 있어야 한다. `backend/`에서 실행한다 (Windows는 `.\gradlew.bat`).

```bash
./gradlew dbTest --tests "*AdminCodesAcceptanceTest"
```

기대: `US1-AC1`이 통과한다.

## 화면 확인

1. `backend/`에서 `./gradlew bootRun`, `frontend/`에서 `npm run dev`
2. 시험 계정 `admin`으로 로그인해 코드 관리를 연다: 코드 그룹이 하나도 없고, 빈 목록 안내가 보인다
3. (선택) 시험용 그룹과 코드를 하나 만들고 사용 안 함으로 바꿔 본다: 배포 없이 바로 반영된다 (SC-001). 확인한 뒤 지운다
