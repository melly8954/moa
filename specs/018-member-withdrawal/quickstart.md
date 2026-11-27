# Quickstart: 회원 탈퇴 (018) 확인 안내

API는 [contracts/withdrawal-api.md](./contracts/withdrawal-api.md), 화면은 [contracts/screens.md](./contracts/screens.md), 데이터는 [data-model.md](./data-model.md).

## 준비

1. `backend/`에서 `.\gradlew.bat bootRun` (local, 시험 계정 `admin`·`user`), `frontend/`에서 `npm run dev`
2. 소셜 확인은 `backend/.env.local`에 구글 값이 있어야 한다. 없으면 3·4·8은 건너뛴다
3. 시간을 앞당길 때는 로컬 DB에서 `tb_user.withdrawn_at`을 고친다

## 수동 확인

| 순서 | 할 일 | 기대 결과 | AC |
|---|---|---|---|
| 1 | `user`로 마이페이지 → 계정 관리 탭 | 맨 아래 오른쪽 빨간 "탈퇴하기" | US1-AC1 |
| 2 | 확인 창에서 다른 닉네임 → 취소 → 다시 열기 | 단추 꺼짐, 안내 세 가지, 다시 열면 비어 있음 | US1-AC2·AC3·AC9 |
| 3 | 두 브라우저로 로그인한 뒤 한쪽에서 탈퇴 | 로그인 화면에 복구 기한. 다른 쪽은 다음 동작에서 로그인 화면 | US1-AC5·AC6 |
| 4 | 다시 로그인 (비밀번호, 구글) | 복구 안내와 완전 삭제 예정 시각 | US2-AC1·AC2 |
| 5 | 틀린 비밀번호 | 보통의 실패 안내 | US2-AC3 |
| 6 | 복구 | 로그인되고 프로필 그대로 | US2-AC4 |
| 7 | 탈퇴 → 관리자로 같은 아이디 계정 만들기 | "이미 쓰는 아이디" | US2-AC12 |
| 8 | `withdrawn_at`을 31일 전으로 → 로그인 | 복구할 수 없다는 안내 | US2-AC9 |
| 9 | 8 뒤 파기 정리를 돌림(cron을 잠시 앞당김) → DB 확인 | 회원 행만 남고 값이 덮임, 딸린 행 없음, 파기 이력 "회원" 1건 | US3-AC1~AC3·AC5 |
| 10 | 9 뒤 같은 구글 계정으로 로그인 | 새 가입 화면 | US3-AC12 |
| 11 | 관리자로 회원을 지우고 `deleted_at`을 31일 전으로 → 정리 | 회원 행이 덮이고 파기 이력 "회원" 1건 | US3-AC11 |
| 12 | 유예 중에 관리자 사용자 관리 목록 | 나오지 않음. 복구하면 나옴 | US1-AC13 |

## AC와 테스트

| AC | 테스트 |
|---|---|
| US1-AC1·AC2·AC3·AC9 | 프론트 단위 |
| US1-AC4·AC5·AC6·AC8·AC10~AC13 | 인수 (`MemberWithdrawalAcceptanceTest`). AC5는 e2e도 |
| US1-AC7 | 인수(기본 조회에 나오지 않음)와 백엔드 단위(`MemberDisplay`). 다른 회원을 보이는 화면이 아직 없다 (research R8) |
| US2-AC1~AC4·AC6~AC13 | 인수. AC4는 e2e도 |
| US2-AC5 | 프론트 단위 |
| US3-AC1~AC16 | 인수 (`MemberPurgeAcceptanceTest`) |
| US4-AC1·AC2 | 프론트 단위 |
| US4-AC3~AC6 | 인수 (시험용 리스너) |

- Edge Case: 동시 탈퇴, 탈퇴와 겹친 재발급, 복구와 파기 동시, 파기 두 번 동시, 유예 중 잠김, 관리자가 지운 회원의 아이디를 새 계정이 쓰는 중
- 실시간 연결: `RealtimeAcceptanceTest`를 상속해 탈퇴 뒤 열린 연결이 닫히고 다시 연결할 수 없는지 본다
- 로컬에서는 `.\gradlew.bat build`와 `npm run check`. [공용] 프론트 파일을 바꾸므로 push 전에 e2e 전체를 돌린다. `User` 조건을 바꾼 뒤와 마지막에 `dbTest` 전체도 로컬에서 돌린다 (사용자 지시)
