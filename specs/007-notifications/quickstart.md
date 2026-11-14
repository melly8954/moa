# Quickstart: 알림 (notifications)

기대 결과의 기준은 [contracts/](./contracts/)와 [data-model.md](./data-model.md)다.

## 자동 검증

무거운 검사(DB 테스트 전체, 화면 검사 전체)는 PR CI가 돈다 (`docs/git.md` "빌드·테스트"). 로컬에서는 바꾼 기능만 본다.

```bash
# backend/ (Windows는 .\gradlew.bat, Docker 필요)
./gradlew build
./gradlew dbTest --tests "*NotificationsAcceptanceTest" --tests "*WebSocketAcceptanceTest"

# frontend/
npm run check
```

기대: `NotificationsAcceptanceTest`의 백엔드 AC가 통과한다. 화면 AC(`e2e/notifications.spec.ts`)는 PR CI에서 확인한다.

## 웹 푸시 키 만들기 (사람이 한다)

VAPID 키는 시크릿이라 에이전트가 만들거나 읽지 않는다 (원칙 VI).

1. 키 한 쌍을 만든다 (예: `npx web-push generate-vapid-keys`)
2. `backend/.env`에 `WEB_PUSH_PUBLIC_KEY`, `WEB_PUSH_PRIVATE_KEY`, `WEB_PUSH_SUBJECT=mailto:<운영 메일>`을 넣는다. 비우면 웹 푸시가 꺼진다
3. 운영은 같은 이름의 환경 변수로 준다. 키를 바꾸면 기존 구독은 모두 다시 켜야 한다

## 화면 확인

1. `backend/`에서 `./gradlew bootRun`, `frontend/`에서 `npm run dev`
2. 브라우저 두 개(또는 일반 창 + 시크릿 창)에서 `admin`, `user`로 로그인한다
3. `admin`이 공지를 게시한다: `user` 화면에 새로고침 없이 "새 공지" 토스트와 "공지" 점이 생기고, 토스트를 누르면 공지 상세로 간다 (US1)
4. `user` 화면 사이드바 계정 줄에 벨이 보인다. 개발용으로 개인 알림을 만들 방법은 없으므로 벨·알림 화면의 빈 결과 안내를 본다 (개인 알림 실시간은 자동 테스트로 확인)
5. `user`가 `/notifications`에서 브라우저 알림을 켠다: 권한 요청이 뜨고 허락하면 스위치가 켜진다. 권한을 거부한 브라우저는 안내가 보인다 (US4-AC4)
6. 좁은 폭(모바일)에서 화면 머리에 벨이 보인다
7. 브라우저 알림 표시(US4-AC1·AC2)는 개인 알림을 만드는 기능이 생긴 뒤 그 기능으로 확인한다. 탭을 닫거나 다른 탭을 본 상태에서 알림이 오고, 누르면 관련 화면이 열린다
