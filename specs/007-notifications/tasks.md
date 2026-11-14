---

description: "알림 (notifications) 작업 목록"
---

# Tasks: 알림 (notifications)

**Input**: `specs/007-notifications/`의 설계 문서

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/notifications-api.md, contracts/realtime.md, contracts/notifications-screens.md

**Tests**: 원칙 II에 따라 AC마다 테스트를 하나 둔다. 백엔드 AC는 `NotificationsAcceptanceTest`(실제 포트, STOMP 클라이언트, MockMvc, 가짜 푸시 서버, research R11), 화면에서만 보이는 AC는 `e2e/notifications.spec.ts`(Playwright `routeWebSocket`). 운영체제 알림 표시(US4-AC1·AC2의 화면 부분)는 화면 직접 확인으로 남긴다.
로컬에서는 빠른 검사와 바꾼 기능의 DB 테스트만 돌린다. DB 테스트 전체와 화면 검사 전체는 PR CI로 확인한다 (`docs/git.md` "빌드·테스트").

## Format: `[ID] [P?] [Story] Description`

- **[P]**: 병렬 가능 (다른 파일, 의존 없음)
- **[Story]**: 작업이 속한 사용자 스토리
- 경로는 저장소 루트 기준이다. 키트 파일은 `C:\psw\github\psw-dev-kit`의 `springboot/v0.5.0` 태그에서 `git show <태그>:<경로>`로 꺼낸다

## Phase 1: Setup (헬퍼 얹기, 라이브러리 확인)

**Purpose**: 키트 websocket 헬퍼를 얹고, 웹 푸시 라이브러리가 쓸 수 있는지 먼저 확인한다 (research R1, R6)

- [ ] T001 웹 푸시 라이브러리를 확인한다: 후보 `nl.martijndwars:web-push`(+ BouncyCastle)의 최신 판을 backend/build.gradle에 더하고, 테스트 안에서 만든 VAPID 키로 테스트 안 가짜 푸시 서버(`com.sun.net.httpserver`)에 한 번 보내 요청이 오는지 확인한다(임시 테스트, 확인 뒤 지운다). Spring Boot 4·Java 21에서 안 되면 다른 라이브러리를 고르고 research.md R6을 고친다
- [ ] T002 [P] 키트 `backend/springboot/helpers/websocket/files/` 아래 파일을 backend/에 같은 경로로 옮기고 `com.example.psw` → `com.moa`로 바꾼다 (`common/websocket/` 3개, `WebSocketAcceptanceTest`). 키트 `helper.gradle`을 backend/gradle/helpers/websocket.gradle로 옮긴다
- [ ] T003 [P] frontend/에서 `@stomp/stompjs`를 `npm install`로 더한다
- [ ] T004 backend/.psw-dev-kit `helpers`에 `websocket`을 더하고, .specify/memory/constitution.md "기술 제약"의 백엔드 줄에 websocket을 적고 버전을 1.1.4(PATCH)로 올린다
- [ ] T005 backend/에서 `./gradlew spotlessApply`, `./gradlew build`, `./gradlew dbTest --tests "*WebSocketAcceptanceTest"`로 헬퍼가 키트 테스트대로 도는지 확인한다

**Checkpoint**: `/ws` 연결과 사용자별 메시지가 moa에서 돌고, 웹 푸시 라이브러리가 정해졌다

---

## Phase 2: Foundational (데이터, 설정, 연결, 테스트 틀)

**Purpose**: 모든 스토리가 쓰는 테이블, 엔터티, 설정, 프론트 실시간 연결, 테스트 틀

**⚠️ CRITICAL**: US1~US4 모두 이 단계에 기댄다

- [ ] T006 backend/src/main/resources/db/migration/V2_3__notifications.sql을 만든다. `notifications`: `id` BIGINT 자동 증가, `user_id` BIGINT NOT NULL, `type` VARCHAR(30) NOT NULL, `title` VARCHAR(200) NOT NULL, `link` VARCHAR(500) NOT NULL, `created_at` DATETIME(6) NOT NULL, `read_at` DATETIME(6) NULL, 인덱스 `(user_id, created_at)`·`(user_id, read_at)`·`(created_at)`. `push_subscriptions`: `id`, `user_id` BIGINT NOT NULL, `endpoint` VARCHAR(1000) NOT NULL 유일, `p256dh` VARCHAR(200) NOT NULL, `auth` VARCHAR(100) NOT NULL, `created_at` DATETIME(6) NOT NULL (data-model.md)
- [ ] T007 [P] backend/src/main/java/com/moa/api/dto/notification/NotificationType.java(`SYSTEM`)와 backend/src/main/java/com/moa/api/entity/notification/에 `Notification`(`@HardDelete`, `read(now)`: 이미 읽었으면 그대로), `PushSubscription`(`@HardDelete`, 같은 endpoint면 사용자·키를 바꾸는 `rebind`)을 만든다. enum은 컨트롤러가 쓰므로 dto 패키지에 둔다 (006 `NoticeStatus`와 같은 이유)
- [ ] T008 backend/src/main/java/com/moa/api/repository/{jpa,reader,writer}/notification/에 리포지토리·Reader·Writer를 만든다: 내 목록(최신순 페이지), 안 읽은 수, 내 알림 하나, 모두 읽음(벌크 update), 만든 지 N일 지난 알림 지우기, 구독 endpoint로 찾기·사용자 구독 목록·지우기
- [ ] T009 [P] backend/src/main/java/com/moa/api/realtime/AsyncConfig.java에 `@EnableAsync`를 둔다 (키트 `Application.java`는 고치지 않는다, research R4)
- [ ] T010 [P] backend/src/main/resources/application.yaml에 moa 설정 문서를 덧붙인다: `app.notification.retention-days: ${NOTIFICATION_RETENTION_DAYS:90}`, `cleanup-cron: ${NOTIFICATION_CLEANUP_CRON:0 0 5 * * *}`, `app.web-push.public-key: ${WEB_PUSH_PUBLIC_KEY:}`, `private-key: ${WEB_PUSH_PRIVATE_KEY:}`, `subject: ${WEB_PUSH_SUBJECT:}`. backend/.env.example에 같은 이름을 빈 값으로 더한다. 키 값은 넣지 않는다 (원칙 VI)
- [ ] T011 backend/src/test/java/com/moa/support/에 실시간 테스트 틀을 만든다: `RealtimeAcceptanceTest`(`@Tag("db")`, `@SpringBootTest(RANDOM_PORT)`, `@AutoConfigureMockMvc`, Testcontainers, DatabaseCleaner, 토큰 도우미), STOMP 도우미(연결·구독·받을 때까지 기다리기·일정 시간 안 온다 확인), `FakePushServer`(받은 요청을 쌓아 두는 `com.sun.net.httpserver`). 키트 `WebSocketAcceptanceTest` 방식을 따른다
- [ ] T012 [P] frontend/src/app/layout.tsx가 서버 환경 변수 `WS_URL`(비우면 `ws://localhost:8080/ws`)을 요청 때 읽어 frontend/src/providers/app-providers.tsx에 넘기게 한다. frontend/.env.example에 `WS_URL=`을 더한다 (research R2)
- [ ] T013 frontend/src/lib/realtime.ts와 frontend/src/providers/realtime-provider.tsx를 만든다: 로그인 상태면 액세스 토큰으로 STOMP 연결, 로그아웃이면 끊기, 토큰이 바뀌면 다시 연결, 끊긴 뒤 다시 이어지면 벨의 수·새 공지 여부 쿼리를 무효화, 구독을 등록하는 hook(`useRealtimeSubscription(destination, handler)`). app-providers.tsx에 공급자를 둔다 (research R3)
- [ ] T014 [P] frontend/src/types/notification.ts, frontend/src/api/notification.ts(contracts/notifications-api.md 7개)를 만들고 frontend/src/api/index.ts에 `notification`을 더한다. frontend/messages/ko.json에 `notification` 문구 묶음을 더한다

**Checkpoint**: 테이블·엔터티·설정이 있고, 로그인한 화면이 실시간 연결을 연다

---

## Phase 3: User Story 1 - 공지 게시를 화면을 보고 있는 사람에게 알린다 (Priority: P1) 🎯 MVP

**Goal**: 공지를 게시하면 연결된 로그인 사용자에게 토스트와 "공지" 점 (`006-notices` US4)

**Independent Test**: `./gradlew dbTest --tests "*NotificationsAcceptanceTest"`에서 US1-AC1~AC3가 통과한다. 화면 AC는 PR CI e2e

- [ ] T015 [US1] backend/src/main/java/com/moa/api/application/v1/notice/NoticeApplication.java의 `publish`와 만들기(바로 게시)에서 `NoticePublishedEvent(id, title)`를 낸다. 임시 저장·고치기·고정·지우기에서는 내지 않는다 (FR-003, research R4)
- [ ] T016 [US1] backend/src/main/java/com/moa/api/realtime/NoticeRealtimeListener.java를 만든다: `@TransactionalEventListener(AFTER_COMMIT)`로 `/topic/notices`에 {type: "NOTICE_PUBLISHED", id, title, link: "/notices/{id}"}를 보낸다 (contracts/realtime.md)
- [ ] T017 [US1] `@DisplayName("US1-AC1 ...")`~`("US1-AC3 ...")` 테스트 3개를 backend/src/test/java/com/moa/api/NotificationsAcceptanceTest.java(`RealtimeAcceptanceTest` 상속)에 더한다 (contracts/realtime.md 표)
- [ ] T018 [US1] frontend에서 `/topic/notices`를 구독해 "새 공지: {제목}" 토스트(누르면 `link`)를 띄우고 새 공지 쿼리(`NEW_NOTICE_QUERY_KEY`)를 무효화한다. 위치: frontend/src/components/notification/realtime-toasts.tsx (공급자 안에 둔다)
- [ ] T019 [US1] frontend/e2e/notifications.spec.ts를 만들고 US1-AC1(토스트 + "공지" 점)·US1-AC4(토스트 누르면 공지 상세)를 `page.routeWebSocket`으로 STOMP CONNECTED·MESSAGE 프레임을 흉내 내 확인한다. 로그아웃 화면은 `/ws`에 붙지 않는 것도 본다(US1-AC3 화면 부분)
- [ ] T020 [US1] specs/006-notices/tasks.md의 T041~T043을 이 단계 결과로 체크하고, specs/006-notices/research.md R9에 "게시 이벤트는 007 R4로 정했다"를 적는다 (research R12)

**Checkpoint**: 공지 실시간 알림이 된다 (006 US4 끝)

---

## Phase 4: User Story 2 - 개인 알림을 알림함에서 보고 읽는다 (Priority: P1)

**Goal**: `notify`로 쌓인 개인 알림을 벨·알림 화면에서 보고 읽는다

**Independent Test**: `./gradlew dbTest --tests "*NotificationsAcceptanceTest"`에서 US2-AC1~AC6이 통과한다

- [ ] T021 [US2] backend/src/main/java/com/moa/api/service/notification/NotificationService.java와 backend/src/main/java/com/moa/api/application/v1/notification/NotificationApplication.java를 만든다: `notify(userId, type, title, link)`(link는 `/`로 시작, 저장 뒤 `NotificationCreatedEvent`), 내 목록, 안 읽은 수, 읽음(내 것이 아니거나 없으면 R001), 모두 읽음 (research R5)
- [ ] T022 [US2] backend/src/main/java/com/moa/api/controller/v1/notification/ApiNotificationController.java에 알림함 API 4개를 둔다 (contracts/notifications-api.md "알림함", 로그인 필요, `@CurrentUser`)
- [ ] T023 [US2] `@DisplayName("US2-AC1 ...")`~`("US2-AC6 ...")` 테스트 6개를 NotificationsAcceptanceTest.java에 더한다. 알림은 `NotificationApplication.notify`를 테스트에서 불러 만든다 (contracts/notifications-api.md 표)
- [ ] T024 [US2] frontend/src/components/notification/notification-bell.tsx를 만든다: 안 읽은 수(99+), 팝오버(최근 10개, 안 읽음 표시, 누르면 읽음 뒤 `link`, "모두 읽음", "모두 보기"), 빈 결과 안내, 화면 낭독기 이름 "알림, 안 읽은 알림 N개" (contracts/notifications-screens.md)
- [ ] T025 [US2] frontend/src/components/shell/sidebar-shell.tsx의 계정 줄(`SidebarFooter`)에 벨을 두고, 좁은 폭(서랍)일 때 화면 머리의 서랍 단추 옆에도 둔다. 접으면 아이콘 위 숫자 점 (research R10, 키트 셸을 moa에서 먼저)
- [ ] T026 [US2] frontend/src/app/(main)/notifications/page.tsx 알림 화면을 만든다: 머리("알림", "모두 읽음"), 내 알림 목록(최신순, 페이지, 누르면 읽음 뒤 `link`). 브라우저 알림 스위치 자리는 US4에서 채운다
- [ ] T027 [P] [US2] frontend/tests/unit/notification.test.ts에 안 읽은 수 표시(0, 3, 99, 100 → "99+")와 알림 링크 검사(`/`로 시작)를 둔다

**Checkpoint**: 알림함이 단독으로 된다

---

## Phase 5: User Story 3 - 새 개인 알림을 바로 알아챈다 (Priority: P2)

**Goal**: 새 개인 알림이 연결된 그 사용자의 모든 화면에 토스트와 벨 숫자로

**Independent Test**: `./gradlew dbTest --tests "*NotificationsAcceptanceTest"`에서 US3-AC1~AC3이 통과한다

- [ ] T028 [US3] backend/src/main/java/com/moa/api/realtime/NotificationRealtimeListener.java를 만든다: `NotificationCreatedEvent`를 커밋 뒤 `convertAndSendToUser(userId, "/queue/notifications", {type: "NOTIFICATION_CREATED", id, notificationType, title, link, createdAt})`로 보낸다
- [ ] T029 [US3] `@DisplayName("US3-AC1 ...")`~`("US3-AC3 ...")` 테스트 3개를 NotificationsAcceptanceTest.java에 더한다 (contracts/realtime.md 표)
- [ ] T030 [US3] frontend/src/components/notification/realtime-toasts.tsx에서 `/user/queue/notifications`를 구독해 "{제목}" 토스트(누르면 읽음 뒤 `link`)를 띄우고 벨 숫자·목록 쿼리를 무효화한다
- [ ] T031 [US3] frontend/e2e/notifications.spec.ts에 US3-AC1(메시지 → 토스트, 벨 숫자 +1)을 더한다

**Checkpoint**: 개인 알림 실시간이 된다

---

## Phase 6: User Story 4 - 브라우저 알림으로 받는다 (Priority: P3)

**Goal**: 켜 둔 브라우저에 화면이 보이지 않을 때 개인 알림이 브라우저 알림으로

**Independent Test**: `./gradlew dbTest --tests "*NotificationsAcceptanceTest"`에서 US4-AC1·AC3·AC5가 통과하고, PR CI e2e에서 US4-AC4가 통과한다

- [ ] T032 [US4] backend/src/main/java/com/moa/common/helper/webpush/에 웹 푸시 보내기를 만든다: 설정(`app.web-push.*`, 키가 비면 꺼짐), 구독 하나에 페이로드 보내기, 404·410이면 "만료"를 알림 (research R6, T001의 라이브러리). moa에서 먼저 만들고 키트 헬퍼로 옮길 것이라고 클래스 주석에 적는다
- [ ] T033 [US4] NotificationApplication과 ApiNotificationController에 구독 API 3개를 더한다(공개 키, 저장: 같은 endpoint면 내 것으로, 지우기) (contracts/notifications-api.md "브라우저 알림 구독")
- [ ] T034 [US4] backend/src/main/java/com/moa/api/realtime/NotificationPushListener.java를 만든다: `NotificationCreatedEvent`를 커밋 뒤 `@Async`로 그 사용자의 구독마다 {id, title, link}를 보내고, 만료된 구독은 지운다
- [ ] T035 [US4] `@DisplayName("US4-AC1 ...")`, `("US4-AC3 ...")`, `("US4-AC5 ...")` 테스트 3개를 NotificationsAcceptanceTest.java에 더한다. 테스트 안에서 VAPID 키를 만들어 설정에 넣고, 구독 endpoint를 `FakePushServer` 주소로 저장한다. US4-AC2·AC4는 화면 쪽이라 각각 화면 직접 확인과 e2e로 둔다 (contracts/notifications-api.md 표)
- [ ] T036 [US4] frontend/public/sw.js를 만든다: `push` 때 보이는 moa 창이 있으면 띄우지 않고, 없으면 `showNotification(title, { tag: id, data: { link } })`, `notificationclick` 때 열린 창이 있으면 앞으로 가져와 `link`로, 없으면 새 창 (research R7)
- [ ] T037 [US4] frontend/src/lib/web-push.ts를 만든다: 지원 여부, 권한 상태, 켜기(권한 요청 → 서비스 워커 등록 → `pushManager.subscribe` → 서버 저장), 끄기(서버 지우기 → `unsubscribe`), 브라우저 쪽만 해제
- [ ] T038 [US4] frontend/src/app/(main)/notifications/page.tsx에 브라우저 알림 스위치를 둔다: 지원 안 함, 권한 거부(꺼짐 + 브라우저 설정 안내), 서버 웹 푸시 꺼짐(공개 키 없음) 안내 (contracts/notifications-screens.md)
- [ ] T039 [US4] 로그아웃 때 구독 해제: frontend/src/components/shell/use-sign-out.ts가 서버 로그아웃 전에 구독을 서버에서 지우고, frontend/src/providers/realtime-provider.tsx(또는 같은 자리)가 로그인 상태가 `signedOut`·`expired`로 바뀌면 이유와 관계없이 브라우저 쪽 구독을 해제한다 (research R8)
- [ ] T040 [US4] frontend/e2e/notifications.spec.ts에 US4-AC4(`Notification.permission` = "denied" 흉내 → 꺼짐 + 안내)를 더한다

**Checkpoint**: US1~US4가 모두 된다

---

## Phase 7: Polish & Cross-Cutting

**Purpose**: 보관 기간 정리, 문서 정합성, PR CI 확인

- [ ] T041 backend/src/main/java/com/moa/api/realtime/NotificationCleaner.java를 만든다: `@Scheduled(cron = "${app.notification.cleanup-cron}")`로 `retention-days`(90)가 지난 알림을 지운다 (FR-009, research R9). NotificationsAcceptanceTest.java에 89일·91일 전 알림으로 정리를 확인하는 테스트를 더한다 (FR-009 확인, AC 아님)
- [ ] T042 backend/에서 `./gradlew spotlessApply`, `./gradlew build`, `./gradlew dbTest --tests "*NotificationsAcceptanceTest" --tests "*WebSocketAcceptanceTest"`, frontend/에서 `npm run format`, `npm run check`를 돌린다. DB 테스트 전체와 화면 검사 전체는 PR CI 결과로 확인한다 (`docs/git.md` "빌드·테스트")
- [ ] T043 specs/README.md 기능 지도에서 `007-notifications`의 상태를 `구현됨`으로, `006-notices`의 상태에서 "(실시간 알림 US4는 007 뒤)"를 지운다
- [ ] T044 specs/007-notifications/quickstart.md의 "화면 확인"을 사람이 직접 수행한다. VAPID 키 만들기도 사람이 한다 (원칙 VI)
- [ ] T045 메신저 담당에게 알린다: 같은 연결(`/ws`), 구독 경로 규칙(spec FR-010), 개인 알림 보내기(`NotificationApplication.notify`). 사람이 한다

---

## Dependencies & Execution Order

- Setup: T001(라이브러리 확인)은 T032보다 먼저. T002 → T004 → T005. T003은 아무 때나
- Foundational: T006 → T007 → T008. T009·T010은 아무 때나. T011은 T005 뒤. T012 → T013. T014는 T003 뒤
- US1(T015~T020): Foundational 뒤. US2(T021~T027): Foundational 뒤. US1과 US2는 서로 기대지 않는다
- US3(T028~T031): US2의 `notify`(T021) 뒤
- US4(T032~T040): T001, US2의 `notify`(T021) 뒤. T036 → T037 → T038·T039
- Polish: US1~US4 뒤
- `NotificationsAcceptanceTest.java`는 한 파일이라 T017·T023·T029·T035·T041은 차례로 한다. `realtime-toasts.tsx`(T018·T030), `notifications/page.tsx`(T026·T038), `e2e/notifications.spec.ts`(T019·T031·T040)도 차례로

## Parallel Example

```text
Setup:        T002 (백엔드 헬퍼)  ∥  T003 (프론트 의존성)
Foundational: T007 · T009 · T010 (백엔드)  ∥  T012 · T014 (프론트)
US1 ∥ US2:    T015~T017 (공지 이벤트·테스트)  ∥  T021~T022 (알림함 API)  ∥  T024 · T027 (벨·단위)
```

## Implementation Strategy

1. Setup(T001~T005): 웹 푸시 라이브러리를 먼저 확인하고 websocket 헬퍼를 얹는다
2. Foundational(T006~T014): 테이블·설정·연결·테스트 틀
3. US1(T015~T020): 공지 실시간 (MVP, 006 US4 끝)
4. US2(T021~T027): 알림함과 벨
5. US3(T028~T031): 개인 알림 실시간
6. US4(T032~T040): 브라우저 알림
7. Polish(T041~T045) 뒤 구현 PR을 연다. PR CI(DB 테스트 전체, 화면 검사 전체)가 통과하면 병합한다
