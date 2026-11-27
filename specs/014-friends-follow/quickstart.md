# Quickstart: 회원 프로필 보기, 친구, 팔로우 (014)

기대 결과의 기준은 [contracts/friends-api.md](./contracts/friends-api.md)와 [data-model.md](./data-model.md)다.

## 자동 검증

로컬에서는 빠른 검사만 돌린다. `backend/`에서 `./gradlew build`(포맷, lint, ArchitectureTest, 단위 테스트), `frontend/`에서 `npm run check`다 (Windows는 `.\gradlew.bat`).

인수 테스트와 e2e는 PR CI(`backend-ci`의 `db-test`, `frontend-ci`의 `e2e`)가 돌린다 (`CLAUDE.md` "로컬 검사"). 사용자가 요청할 때만 로컬에서 돌리고, 그때는 Docker가 떠 있어야 한다.

```bash
./gradlew dbTest --tests "*Friend*" --tests "*Follow*" --tests "*MemberProfile*" --tests "*PrivateAccount*"
npm run test:e2e -- e2e/friends.spec.ts
```

기대: 이름이 `US<n>-AC<m>`인 테스트가 spec.md의 AC 41개와 하나씩 맞고 모두 통과한다.

## 화면 확인

준비: 회원 계정이 둘 필요하다. 시험 계정 `user`와, `008-party` quickstart "준비"의 `user2`(비밀번호는 사람이 정해 넣는다, 원칙 VI)를 서로 다른 브라우저로 쓴다. 두 계정의 마이페이지에서 자기소개와 디스코드 태그(공개 범위 "친구만")를 적어 둔다.

1. `backend/`에서 `./gradlew bootRun`, `frontend/`에서 `npm run dev`
2. `user`로 로그인해 사이드바 "친구"를 연다: 네 묶음(친구, 받은 요청, 보낸 요청, 팔로우 요청)이 비어 있다
3. `user`가 주소로 `user2`의 프로필을 연다
   - 닉네임·태그와 공개 항목, 친구 0·팔로워 0·팔로잉 0이 보인다. 디스코드 태그("친구만")는 보이지 않는다
   - "친구 추가"를 누른다: "요청 보냄"이 된다 (US2-AC1)
4. `user2` 화면에 친구 요청 토스트가 뜨고, 벨에 알림이 쌓인다 (US6-AC1)
5. `user2`가 친구 화면의 받은 요청에서 수락한다
   - 두 사람의 친구 목록에 서로가 보인다
   - 두 사람의 팔로워·팔로잉 수가 1이다 (US2-AC3)
   - `user`가 `user2`의 프로필을 다시 열면 디스코드 태그가 보인다 (FR-015)
   - `user` 화면에 "친구가 되었습니다" 알림이 뜬다 (US6-AC2)
6. `user`가 `user2`의 팔로우만 취소한다: 친구는 그대로이고 `user2`의 팔로워 수가 0이 된다 (US3-AC3)
7. `user`가 친구를 끊는다. `user2`가 설정에서 계정을 "비공개"로 바꾼다
   - 두 사람의 친구·팔로우가 모두 사라진다 (US5-AC1)
   - `user`가 `user2`의 프로필을 열면 자물쇠 표시와 "비공개 계정입니다"가 보이고, 팔로워 수를 눌러도 목록이 열리지 않는다 (US4-AC6)
   - `user`가 "팔로우 요청"을 누르면 "요청됨"이 되고, `user2`에게 팔로우 요청 알림이 온다 (US4-AC2, US6-AC5)
8. `user2`가 친구 화면의 팔로우 요청에서 수락한다: `user`에게 수락 알림이 오고, `user`가 `user2`의 팔로워 목록을 볼 수 있다 (US4-AC3, US6-AC6)
9. `user`가 다시 친구 요청을 보내고 `user2`가 거절한다. `user`가 곧바로 다시 요청하면 "지금은 보낼 수 없다"가 뜬다 (US2-AC9)
10. `admin`으로 로그인하면 관리 화면으로 가고, 사용자 셸의 친구 화면을 열 수 없다 (FR-013)

## 확인한 뒤

로컬 DB에 남은 관계는 지워도 되고 그대로 두어도 된다. 거절 기록은 30일 뒤 정리기가 지운다. 탈퇴 흐름(US5-AC5·AC6)은 018 탈퇴를 거쳐야 해서 자동 테스트로만 본다.
