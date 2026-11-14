# Implementation Plan: 알림 (notifications)

**Branch**: `feat/007-notifications` | **Date**: 2026-11-15 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/007-notifications/spec.md`

## Summary

키트 websocket 헬퍼(`springboot/v0.5.0`)를 얹어 실시간 연결을 열고, 그 위에 두 갈래 알림을 만든다.
전체 알림은 공지 게시 때 `/topic/notices`로 보내고(`006-notices` US4), 개인 알림은 `notifications` 테이블에 쌓은 뒤 `/user/queue/notifications`와 브라우저 알림(웹 푸시)으로 보낸다.
다른 기능은 `NotificationApplication.notify(...)` 하나로 개인 알림을 보낸다.
프론트는 로그인하면 실시간 연결을 열고, 토스트와 표시(공지 점, 계정 줄 벨)를 바꾸며, 설정 화면에서 브라우저 알림을 켜고 끈다(2026-11-15에 알림 화면에서 옮김).
프론트 실시간 연결, 웹 푸시, 셸 벨은 키트에 없어 moa에서 먼저 만든다 ([research.md](./research.md) R1, R2, R6, R10).

## Technical Context

**Language/Version**: Java 21 (Spring Boot 4.1.2), TypeScript 5 (Next 16.3)

**Primary Dependencies**: 키트 websocket 헬퍼(`spring-boot-starter-websocket`, STOMP), 웹 푸시 라이브러리(백엔드, R6), `@stomp/stompjs`(프론트, R3)

**Storage**: MariaDB. `notifications`, `push_subscriptions` (moa `V2_3`). 전체 알림은 저장하지 않는다

**Testing**: 인수 테스트 `NotificationsAcceptanceTest` (실제 포트로 띄워 STOMP 클라이언트와 MockMvc를 함께 쓴다, 웹 푸시는 가짜 푸시 서버로 받는다, `./gradlew dbTest`). 키트 `WebSocketAcceptanceTest`를 함께 얹는다. 프론트는 단위 테스트와 핵심 흐름 e2e(Playwright `routeWebSocket`으로 STOMP를 흉내 낸다)

**Target Platform**: 웹 (같은 Next 앱, 서비스 워커)

**Project Type**: 웹 서비스 (모노레포 `backend/`, `frontend/`)

**Performance Goals**: 실시간 알림은 이벤트 뒤 5초 안 (SC-001, SC-002). 벨의 수는 인덱스 한 번 조회

**Constraints**: 키트 코드는 고치지 않는다 (셸 파일은 006 R12처럼 moa에서 먼저). 서버 한 대 전제(키트 simple broker). VAPID 키는 환경 변수로만 받는다 (원칙 VI)

**Scale/Scope**: 백엔드 API 7개, STOMP 구독 경로 2개, 프론트 화면 1개(`/notifications`) + 셸 벨 + 서비스 워커, AC 18개

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| 원칙 | 판정 | 근거 |
|---|---|---|
| I. 스펙이 먼저다 | 통과 | spec.md에 `[NEEDS CLARIFICATION]`이 없다. 미결 3개와 검토 4개는 2026-11-15 사용자 결정 |
| II. AC마다 테스트 하나 | 통과 | US1~US4의 AC 18개에 테스트를 하나씩 둔다. 브라우저·화면에서만 보이는 AC(토스트, 권한 안내, 서비스 워커)는 e2e로, 운영체제 알림 표시는 화면 직접 확인으로 남긴다 (R11, SC-006) |
| III. 키트를 따른다 | 통과 | websocket 헬퍼를 키트 그대로 얹는다. 프론트 실시간 연결·웹 푸시·셸 벨은 키트에 없어 moa에서 먼저 만들고 키트로 옮길 목록에 올린다 (사용자 결정) |
| IV. 기존 기능은 제자리에서 고친다 | 통과 | 공지 게시 알림은 `006-notices` US4로 이루고, 006 tasks T041~T043을 이 구현에서 체크한다. 006 R9의 "게시 이벤트는 007에서 정한다"를 R4로 정한다 |
| V. 테스트를 고쳐서 통과시키지 않는다 | 통과 | 기존 테스트를 바꾸지 않는다. 키트 `use-sign-out.ts`·셸은 동작을 더할 뿐이다 |
| VI. 시크릿을 다루지 않는다 | 통과 | VAPID 개인 키는 환경 변수로만 받고 읽거나 커밋하지 않는다. 테스트는 테스트 안에서 만든 키를 쓴다. 운영 키는 사람이 만든다 (quickstart) |

Phase 1 설계 뒤 다시 확인했다. 위반이 없다.

## Project Structure

### Documentation (this feature)

```text
specs/007-notifications/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── notifications-api.md   # HTTP API (알림함, 브라우저 알림 구독)
│   ├── realtime.md            # STOMP 연결·구독 경로·메시지 모양, 웹 푸시 페이로드
│   └── notifications-screens.md
└── tasks.md                   # /speckit-tasks가 만든다
```

### Source Code (repository root)

```text
backend/
├── gradle/helpers/websocket.gradle                  # 키트 그대로
├── build.gradle                                     # 웹 푸시 라이브러리 (R6)
├── src/main/resources/application.yaml              # app.notification.*, app.web-push.* (VAPID는 환경 변수)
├── src/main/resources/db/migration/V2_3__notifications.sql
├── src/main/java/com/moa/
│   ├── common/websocket/                            # 키트 그대로 (패키지만 com.moa)
│   ├── common/helper/webpush/                       # 웹 푸시 보내기 (moa 먼저, 키트로 옮길 것)
│   └── api/
│       ├── application/v1/notification/             # NotificationApplication (notify, 알림함, 구독), 커밋 뒤 보내기
│       ├── application/v1/notice/NoticeApplication  # publish 뒤 NoticePublishedEvent (R4)
│       ├── controller/v1/notification/              # ApiNotificationController
│       ├── service/notification/  repository/{jpa,reader,writer}/notification/
│       ├── entity/notification/                     # Notification, PushSubscription
│       ├── dto/notification/
│       └── realtime/                                # 커밋 뒤 STOMP·웹 푸시로 보내는 리스너, NotificationCleaner, AsyncConfig(@EnableAsync)
└── src/test/java/com/moa/
    ├── common/WebSocketAcceptanceTest.java          # 키트 그대로
    ├── api/NotificationsAcceptanceTest.java         # US1~US4 AC
    └── support/FakePushServer.java                  # 웹 푸시 요청을 받아 두는 가짜 서버

frontend/
├── public/sw.js                                     # 서비스 워커 (push, notificationclick)
├── src/lib/realtime.ts  src/providers/realtime-provider.tsx   # STOMP 연결 (moa 먼저)
├── src/lib/web-push.ts                              # 구독·해제
├── src/api/notification.ts  src/types/notification.ts
├── src/components/notification/                     # 벨, 펼침 목록, 토스트, 브라우저 알림 켜기
├── src/app/(main)/notifications/page.tsx
├── src/app/layout.tsx  src/providers/app-providers.tsx  # WebSocket 주소를 실행할 때 읽어 넘긴다 (R2)
├── src/components/shell/sidebar-shell.tsx           # 계정 줄 벨, 좁은 폭 머리 벨 (R10)
├── src/components/shell/use-sign-out.ts             # 로그아웃 전 브라우저 알림 구독 해제 (R8)
├── messages/ko.json                                 # notification, menu
├── tests/unit/notification.test.ts
└── e2e/notifications.spec.ts
```

**Structure Decision**: 백엔드는 006과 같은 계층이다. 커밋 뒤 보내기(STOMP, 웹 푸시)는 `@TransactionalEventListener(AFTER_COMMIT)` 리스너에 모은다(키트 websocket HELPER 권장).
프론트 실시간 연결은 루트 공급자에 두어 `(main)`과 `(site)` 화면 모두에서 로그인 상태면 연결한다.
2026-11-15 셸 나누기로 경로 묶음이 `(admin)`·`(user)`·`(account)`가 된다. 연결은 루트 공급자라 그대로다. 벨은 두 셸에, 브라우저 알림 스위치는 `(account)/settings`로 간다. 이 변경의 작업은 `001-admin-menus` tasks Phase 7에 있다 (R10).

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|---|---|---|
| 키트 골격 파일(`sidebar-shell.tsx`, `use-sign-out.ts`, `app/layout.tsx`, `providers/app-providers.tsx`)을 moa에서 고친다 | 벨 자리, 로그아웃 때 구독 해제, WebSocket 주소 넘기기와 실시간 공급자 (FR-001, FR-005, FR-008) | 키트에 먼저 넣기: 사용자가 moa 먼저로 정했다 (006 R12와 같은 길). 키트 옮길 목록에 있다 |
