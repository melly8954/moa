# Quickstart: 파티 (party)

기대 결과의 기준은 [contracts/party-api.md](./contracts/party-api.md)와 [contracts/party-realtime.md](./contracts/party-realtime.md)다.

## 자동 검증

로컬에서는 빠른 검사만 돌린다. `backend/`에서 `./gradlew build`(포맷, lint, ArchitectureTest, 단위 테스트), `frontend/`에서 `npm run check`(포맷, lint, 타입, 문구 검사, 단위 테스트)다 (Windows는 `.\gradlew.bat`).

인수 테스트와 e2e는 PR CI(`backend-ci`의 `db-test`, `frontend-ci`의 `e2e`)가 돌린다 (`CLAUDE.md`). 사용자가 요청할 때만 로컬에서 돌리고, 그때는 Docker가 떠 있어야 한다.

```bash
./gradlew dbTest --tests "*Party*"                    # US1~US10의 API 통합 테스트와 실시간 통합 테스트
./gradlew dbTest --tests "*AdminCodesAcceptanceTest"  # 004 FR-001 표에 더한 코드 그룹
npm run test:e2e -- e2e/party.spec.ts                 # spec.md "e2e로 확인할 흐름" 3개
```

기대: 이름이 `US<n>-AC<m>`인 테스트가 spec.md의 AC 73개와 하나씩 맞고 모두 통과한다.

## 화면 확인

계정이 둘 필요하다. 시험 계정 `admin`과 `user`를 서로 다른 브라우저(또는 한쪽은 시크릿 창)로 쓴다. 관리자도 파티를 쓸 수 있다 (FR-012).

1. `backend/`에서 `./gradlew bootRun`, `frontend/`에서 `npm run dev`
2. `user`로 로그인한 뒤 사이드바 "모집"을 열어 파티를 만든다 (롤, 솔로랭크, 미드, 정글 모집, 공개)
   - 만든 뒤 파티 화면으로 옮겨 간다
   - 모집 페이지에 카드가 보인다
3. `admin`으로 모집 페이지를 열어 그 파티에 정글로 참가한다
   - 두 브라우저의 멤버 목록이 새로고침 없이 바뀐다
   - 채팅에 참가 안내가 뜬다 (US9-AC3, AC7)
4. 서로 메시지를 보낸다: 상대 화면에 1초 안에 나타난다 (SC-007)
5. `admin` 브라우저의 네트워크를 잠깐 끊었다가 다시 켠다: 끊긴 동안 `user`가 보낸 메시지가 채워진다 (FR-018)
6. `user`(방장)가 참가 방식을 비공개로 바꾼다
   - 모집 페이지에서 사라진다
   - 초대 링크를 복사해 둔다
7. `admin`이 나갔다가 초대 링크로 다시 들어온다: "들어가기" 한 번에 들어온다 (SC-003. 포지션 조건이 있으면 참가 창이 열린다)
8. `user`가 `admin`을 내보낸다
   - `admin` 화면에 알림이 뜨고 모집 페이지로 옮겨 간다
   - 초대 링크로도 다시 들어올 수 없다 (US7-AC5, US9-AC8)
9. `user`가 나간다: 마지막 멤버라 해산되고, 모집 페이지와 내 파티 어디에도 없다 (US6-AC4)

## 확인한 뒤

로컬 DB에 남은 시험 파티는 해산되어 보이지 않는다. 지우지 않아도 된다.
