# Quickstart: 공통코드 (admin-codes)

기대 결과의 기준은 [contracts/admin-codes.md](./contracts/admin-codes.md)다.

## 자동 검증

Docker가 떠 있어야 한다. `backend/`에서 실행한다 (Windows는 `.\gradlew.bat`).

```bash
./gradlew dbTest --tests "*Code*AcceptanceTest"
```

기대: `US1-AC1`~`US1-AC5`가 통과한다. `US1-AC6`은 `./gradlew build`의 `CodeGroupRegistryTest`가 본다.

## 화면 확인

local 프로필은 시험 그룹 둘(`LOCAL_GAME_GENRE` 게임 장르, `LOCAL_REPORT_REASON` 신고 사유)을 등록한다 (`LocalCodeGroups`).

1. `backend/`에서 `./gradlew bootRun`, `frontend/`에서 `npm run dev`
2. 시험 계정 `admin`으로 로그인해 공통코드를 연다: 그룹 행 둘(이름·그룹 코드·쓰는 곳)이 보이고, 그룹을 더하는 단추는 없다. 머리 설명이 FR-007 문구다
3. 게임 장르의 "항목 추가"를 눌러 이름만 적고 저장한다: 그룹 맨 뒤에 코드 `001`로 생긴다. 하나 더 만들면 `002`다
4. 항목 이름을 누른다: 드로어에서 이름과 사용을 고치고, 코드는 바꿀 수 없다
5. 항목 둘을 체크해 "사용 안 함"을 누른다: 둘 다 사용 안 함이 된다. 다른 그룹의 항목을 함께 고르면 "한 그룹씩 처리해요"가 보인다
6. 손잡이를 끌거나 키보드(스페이스, 화살표, 스페이스)로 같은 그룹 안 순서를 바꾼다: 새로 고쳐도 그 순서다. 다른 그룹으로는 옮겨지지 않는다
7. 그룹 행을 눌러 접고 편다
