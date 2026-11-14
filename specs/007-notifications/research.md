# Research: 알림 (notifications)

Technical Context에 미결은 없다. 아래는 키트와 moa 코드를 확인해 정한 것이다.

## R1. websocket 헬퍼 얹기

- **Decision**: 키트 `springboot/v0.5.0`의 `helpers/websocket`을 006 rich-text와 같은 방법으로 손으로 옮긴다: `files/`(패키지 `com.moa`), `helper.gradle` → `gradle/helpers/websocket.gradle`. 설정 조각은 없다. `backend/.psw-dev-kit` `helpers`에 `websocket`을 더하고 헌법 "기술 제약"에 적는다
- **Rationale**: 키트가 연결 `/ws`, CONNECT 프레임의 토큰 확인(`StompAuthInterceptor`), `/topic`·`/queue`·`/app`·`/user`를 준다. 핸드셰이크 경로는 `PublicPaths` 빈으로 HTTP 인증에서 뺀다
- **Alternatives considered**: SSE. 사용자 결정에서 버렸다(메신저가 WebSocket을 쓰고 경로 규칙을 이미 정함)

## R2. 브라우저가 붙는 WebSocket 주소

- **Decision**: 브라우저는 백엔드 `/ws`에 직접 붙는다. 주소는 서버 환경 변수 `WS_URL`(비우면 `ws://localhost:8080/ws`)을 루트 레이아웃(서버 컴포넌트)이 요청 때 읽어 공급자에 넘긴다. 백엔드 허용 출처는 키트대로 `CORS_ALLOWED_ORIGINS`
- **Rationale**: 프론트 백엔드 프록시(`src/proxy.ts`)는 미들웨어 rewrite라 WebSocket 업그레이드를 넘기지 못한다. `NEXT_PUBLIC_*`는 빌드 때 굳어, 실행할 때 주소를 읽는 키트 원칙(proxy.ts 주석)과 어긋난다
- **Alternatives considered**: Next 커스텀 서버로 업그레이드 프록시. 키트 골격에서 벗어나고 배포가 복잡해진다

## R3. 프론트 STOMP 클라이언트

- **Decision**: `@stomp/stompjs`의 `Client`를 쓴다. 로그인 상태(`useAuthStore` `signedIn`)가 되면 액세스 토큰을 CONNECT 헤더에 실어 연결하고, 로그아웃하면 끊는다. 토큰이 재발급되면 다시 연결한다(키트 HELPER: 연결 중에는 끊지 않는다). 끊기면 라이브러리 재연결(5초)을 쓰고, 다시 이어지면 벨의 수와 새 공지 여부를 다시 읽는다 (spec Edge Cases)
- **Rationale**: 키트 HELPER 예시와 같은 라이브러리다. 재연결과 heartbeat를 준다
- **Alternatives considered**: 직접 WebSocket + STOMP 프레임 작성. 재연결·heartbeat를 다시 만들어야 한다

## R4. 보내는 때: 커밋 뒤

- **Decision**: 보내기는 모두 DB 커밋 뒤에 한다. 공지 게시는 `NoticeApplication.publish`가 `NoticePublishedEvent(id, title)`를 내고, 개인 알림은 `NotificationApplication.notify`가 저장한 뒤 `NotificationCreatedEvent`를 낸다. `@TransactionalEventListener(phase = AFTER_COMMIT)` 리스너가 STOMP(`SimpMessagingTemplate`)와 웹 푸시로 보낸다. 웹 푸시는 느릴 수 있어 `@Async`로 보낸다. 골격에 `@EnableAsync`가 없으므로(키트 `Application.java`는 `@EnableScheduling`만) moa 설정 클래스 `AsyncConfig`에 `@EnableAsync`를 둔다 (키트 파일은 고치지 않는다)
- **Rationale**: 키트 websocket HELPER 권장. 롤백된 공지·알림을 알리지 않는다. `006-notices` R9가 미룬 게시 이벤트를 여기서 정한다
- **Alternatives considered**: Application 안에서 바로 보내기. 롤백되면 이미 보낸 알림을 되돌릴 수 없다

## R5. 개인 알림을 보내는 쪽 (다른 기능)

- **Decision**: `NotificationApplication.notify(Long userId, NotificationType type, String title, String link)`를 다른 기능의 Application이 같은 트랜잭션에서 부른다. `NotificationType`은 enum이고 지금은 `SYSTEM`(운영자가 한 사용자에게 알리는 시스템 알림 성격)만 둔다. 테스트도 `SYSTEM`을 쓴다. 테스트 전용 값을 운영 enum에 두지 않는다. 기능이 생길 때 그 기능의 스펙이 종류를 더한다(`COMMENT`, `INVITE`, `MESSAGE`, `SANCTION` 등). `link`는 moa 화면 경로(`/`로 시작)만 받는다
- **Rationale**: spec FR-004, Assumptions. 알림 목록 화면이 종류별 아이콘·문구를 고를 수 있게 종류를 enum으로 둔다
- **Alternatives considered**: 종류를 문자열로 열어 두기. 오타와 화면 문구 누락을 막지 못한다

## R6. 웹 푸시 보내기 (백엔드)

- **Decision**: 웹 푸시 프로토콜(VAPID, 페이로드 암호화 aes128gcm)을 구현한 Java 라이브러리를 쓴다. 후보 `nl.martijndwars:web-push`(BouncyCastle 필요). 오래 갱신되지 않았을 수 있어 **구현 첫 작업으로** 최신 판과 Spring Boot 4·Java 21 호환을 확인하고(가짜 푸시 서버로 한 번 보내 보기), 맞지 않으면 다른 라이브러리로 바꾸고 이 절을 고친다. 보내는 코드는 `common/helper/webpush/`에 모아 나중에 키트 헬퍼로 옮긴다
  - VAPID 키: 환경 변수 `WEB_PUSH_PUBLIC_KEY`, `WEB_PUSH_PRIVATE_KEY`, `WEB_PUSH_SUBJECT`(`mailto:` 주소). 비어 있으면 웹 푸시를 끈다(구독 API는 "지원 안 함"을 돌려준다). 테스트는 테스트 안에서 키를 만든다
  - 푸시 서버가 404·410을 돌려주면 그 구독을 지운다 (만료된 구독)
- **Rationale**: 암호화와 서명을 직접 짜면 틀리기 쉽다. 키는 원칙 VI에 따라 환경 변수로만
- **Alternatives considered**: Firebase Cloud Messaging. 외부 계정과 SDK가 필요하고 표준 웹 푸시로 충분하다

## R7. 서비스 워커와 "보이는 상태" 판정

- **Decision**: `public/sw.js`에서 `push` 이벤트 때 `clients.matchAll({ type: "window", includeUncontrolled: true })`로 moa 창 중 `visibilityState === "visible"`인 것이 있으면 브라우저 알림을 띄우지 않는다. 없으면 `showNotification(title, { tag: notificationId, data: { link } })`. `notificationclick`은 열린 moa 창이 있으면 그 창을 앞으로 가져와 `link`로 옮기고, 없으면 새 창을 연다
- **Rationale**: spec Edge Cases(보이는 상태면 토스트만). `tag`로 같은 알림이 겹치지 않는다
- **주의**: 화면이 보이는 상태라 브라우저 알림을 띄우지 않으면, 브라우저에 따라 "백그라운드에서 갱신됨" 같은 기본 알림이 뜰 수 있다. 화면이 보일 때는 대부분의 브라우저가 허용한다. 화면 직접 확인 항목으로 남긴다

## R8. 브라우저 알림 구독의 생애

- **Decision**: 켜기 = 권한 요청 → `pushManager.subscribe({ userVisibleOnly: true, applicationServerKey: 공개 키 })` → 구독(엔드포인트·키)을 서버에 저장. 끄기 = 서버에서 지우고 `subscription.unsubscribe()`. 로그아웃 = `use-sign-out.ts`가 서버 로그아웃 전에 그 브라우저의 구독을 지운다. 같은 엔드포인트로 다른 사용자가 저장하면 그 사용자 것으로 바꾼다
- 로그인 상태가 로그아웃(`signedOut`, `expired`)으로 바뀌면 이유와 관계없이(로그아웃 단추, 재발급 실패, 다른 탭의 로그아웃) 그 브라우저의 구독을 브라우저 쪽에서 해제한다(`subscription.unsubscribe()`, 서버 호출 없이). 서버에 남은 행은 푸시 서버가 404·410을 돌려줄 때 지운다(R6). 로그아웃 단추는 그 전에 서버 행도 지운다
- **Rationale**: FR-008(로그아웃하면 그 브라우저는 꺼짐), US4-AC5. 로그아웃 단추만 보면 재발급 실패로 끊긴 브라우저에 구독이 남는다. 공유 PC에서 앞 사용자의 알림이 뒤 사용자에게 가지 않게 엔드포인트를 유일하게 둔다
- **Alternatives considered**: 서버가 로그아웃 때 지우기. 로그아웃 API는 키트 것이고 어느 브라우저의 구독인지 모른다

## R9. 개인 알림 정리 (90일)

- **Decision**: `NotificationCleaner`가 매일 `created_at`이 90일 지난 알림을 지운다 (`@Scheduled(cron = "${app.notification.cleanup-cron:0 0 5 * * *}")`, 보관 기간 `app.notification.retention-days:90`). 키트 정리기(`LoginHistoryCleaner`, `RichTextImageCleaner`)와 같은 모양
- **Rationale**: FR-009. 정리 시각을 다른 정리기(4:30~4:50)와 겹치지 않게 5:00으로 둔다

## R10. 벨 자리 (FR-005)

- **Decision**: `sidebar-shell.tsx`의 계정 줄(`SidebarFooter`)에 `NotificationBell`을 둔다. 좁은 폭(서랍)에서는 화면 머리의 서랍 단추 옆에도 같은 벨을 둔다. 벨은 안 읽은 수(99+), 누르면 최근 10개를 펼치는 팝오버와 "모두 읽음"·"모두 보기"를 가진다. 사이드바를 접으면 아이콘과 숫자 점만 남는다
- **Rationale**: 2026-11-15 사용자 결정(계정 옆 벨), 검토 1번(좁은 폭 머리에도)
- **키트로 옮길 것**: 셸 계정 줄 벨 자리. 세 셸(sidebar·dual·top)과 함께

## R11. 테스트 방식

- **Decision**:
  - `NotificationsAcceptanceTest`는 `@SpringBootTest(webEnvironment = RANDOM_PORT)` + `@AutoConfigureMockMvc`로 띄워 HTTP는 MockMvc, 실시간은 STOMP 클라이언트(키트 `WebSocketAcceptanceTest` 방식)로 받는다. 구독 등록이 비동기라 받을 때까지 기다린다
  - 웹 푸시는 테스트 안 가짜 푸시 서버(`com.sun.net.httpserver`, 의존성 없음)를 구독 엔드포인트로 저장해, 서버가 보낸 요청이 오는지(또는 안 오는지) 본다. 암호화된 본문은 풀지 않는다
  - 화면 쪽 AC(토스트, 토스트 누르기, 벨 숫자 변화, 권한 거부 안내)는 e2e에서 Playwright `page.routeWebSocket`으로 STOMP 프레임을 흉내 내 확인한다
  - 서비스 워커의 표시·누르기(US4-AC1·AC2의 운영체제 알림)는 자동으로 확인할 수 없어 quickstart 화면 직접 확인으로 남긴다. 자동 테스트는 "켜면 구독이 저장되고 개인 알림 때 푸시 요청이 나간다"까지 본다
- **Rationale**: 원칙 II, SC-006. 키트 테스트와 같은 방식

## R12. 006 US4와의 관계

- **Decision**: `006-notices` US4-AC1은 이 스펙의 US1-AC1과 같은 테스트로 확인하고, 006 tasks T041~T043을 이 구현에서 체크한다. 006 `NoticesAcceptanceTest`에는 더하지 않는다 (실시간은 실제 포트가 필요해 다른 테스트 틀이다). 006 spec SC-006 "US1~US4 전부"는 이 테스트로 채워진다
- **Rationale**: 같은 동작을 두 곳에서 테스트하지 않는다
