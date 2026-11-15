# Quickstart: 운영자 역할 (admin-roles)

기대 결과의 기준은 [contracts/admin-roles.md](./contracts/admin-roles.md)다.

## 자동 검증

Docker가 떠 있어야 한다. `backend/`에서 실행한다 (Windows는 `.\gradlew.bat`).

```bash
./gradlew dbTest --tests "*AdminRolesAcceptanceTest" --tests "*AdminMenusAcceptanceTest"
```

기대: 003의 `US1-AC1`~`US4-AC3`과 001 AC가 통과한다.

## 화면 확인

1. `backend/`에서 `./gradlew bootRun`, `frontend/`에서 `npm run dev`
2. 시험 계정 `admin`으로 역할 관리를 연다: 탭이 없고, 검색 칸과 역할 표(체크박스·이름·코드·권한 수·구성원 수)만 있다. 행에 단추가 없다
3. "역할 추가"로 드로어를 열어 "보안 담당"을 만든다: 구성원 탭이 없고, 권한 칸이 관리 메뉴 트리(운영·설정 묶음)로 보인다. "접속 이력"만 체크해 저장하면 같은 드로어에 구성원 탭이 열린다
4. 구성원 탭에서 `user`를 검색해 체크하고 "더하기": 검색하지 않으면 구성원만 보이고, 검색하면 구성원 여부가 보인다. 관리자 행은 "모든 권한"이고 체크할 수 없다
5. `user`로 로그인한다: 관리 메뉴는 "운영" 아래 접속 이력만 보이고 목록이 열린다. `/admin/users`로 직접 들어가면 목록 자리에 "이 화면을 볼 권한이 없어요."가 보인다
6. `admin`으로 역할 표에서 "보안 담당"을 체크해 삭제하면 사용자 수를 알리고 되묻는다. 지우면 `user`는 다음 요청부터 접속 이력도 쓰지 못한다
