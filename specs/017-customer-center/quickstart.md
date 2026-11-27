# Quickstart: 고객센터 (customer-center)

기대 결과의 기준은 [contracts/api.md](./contracts/api.md), [contracts/screens.md](./contracts/screens.md), [data-model.md](./data-model.md)다.

## 자동 검증

로컬에서는 빠른 검사만 돌린다. 인수 테스트(`dbTest`)와 화면 검사(e2e)는 작업 브랜치 → `dev` PR의 CI가 돌린다 (`CLAUDE.md` 로컬 검사).

`backend/`에서 실행한다 (Windows는 `.\gradlew.bat`).

```bash
./gradlew build
```

`frontend/`에서 실행한다.

```bash
npm run check
```

PR CI에서 기대하는 것:

- `CustomerCenterAcceptanceTest`의 US1~US4 AC (화면 쪽 동작은 e2e, research R13). US5는 회원 탈퇴 스펙 뒤
- 운영 하위 메뉴·사용자 메뉴·연결 화면을 더한 `AdminMenusAcceptanceTest`, 권한 트리를 더한 `AdminRolesAcceptanceTest`, 분류 그룹을 더한 `AdminCodesAcceptanceTest`
- e2e `support`: 회원이 이미지를 붙여 문의 → 관리자가 답변 → 회원이 알림으로 상세를 연다. 관리자가 FAQ를 등록하고 사용자 FAQ에서 분류·검색으로 찾는다 (3폭, 가로 넘침·접근성)

## 화면 확인

1. `backend/`에서 `./gradlew bootRun`, `frontend/`에서 `npm run dev`
2. 시험 계정 `admin`으로 로그인: 운영을 펴면 사용자 관리, 공지 관리, FAQ 관리, 문의 관리, 접속 이력, 파기 이력 순서다
3. FAQ 관리에서 분류 "계정"으로 이미지가 든 FAQ를 등록하고, 하나 더 등록해 순서 숫자를 바꿔 본다. 둘을 체크해 끄기·켜기·삭제를 해 본다
4. 로그아웃한 채 `/support`: 켜진 FAQ가 보이고, 분류 단추와 검색어로 거르고, 질문을 눌러 답을 펼친다. "1:1 문의하기"를 누르면 로그인으로 가고, `user`로 로그인하면 쓰기 화면으로 돌아온다
5. 이미지 두 장을 붙여 문의를 보낸다 (SC-002, 2분 안). 여섯 장째는 붙일 수 없다. 내 문의에 "답변 대기"로 보인다
6. `admin`으로 문의 관리: 기본 거르기 "답변 대기"에 그 문의가 있다. 드로어에서 이미지를 크게 보고 답변을 저장한다
7. `user` 화면의 벨에 "1:1 문의에 답변이 등록되었습니다"가 오고(SC-003, 5초 안), 누르면 상세에 "moa 운영팀"의 답변이 보인다. 지우기 단추는 없다
8. 권한 관리에서 FAQ 관리 권한만 준 운영자로 로그인: FAQ 관리는 열리고 문의 관리는 흐리게 자물쇠가 붙는다
