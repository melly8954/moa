# Research: 파티 (party)

Technical Context의 미결은 실시간 통신 하나였다. 2026-11-15에 moa가 직접 구성하기로 정했다 (R1). 나머지는 코드를 확인해 정했다.

## R1. 실시간 통신 (FR-018, FR-028)

- **Decision**: Spring WebSocket + STOMP를 moa 코드로 구성한다. 브로커는 서버 안의 단순 브로커(simple broker)를 쓴다. 키트의 확장 지점(`PublicPaths`로 핸드셰이크 경로 열기, `HttpSecurityCustomizer`)만 빌린다
- **Rationale**: 저장소와 로컬 어디에도 psw-dev-kit 원본이 없어 websocket 헬퍼가 있는지 확인할 수 없었다. 사용자가 "웬만한 것은 직접 만들어 자급자족한다"로 정했다 (2026-11-15). STOMP는 구독 주소(topic) 단위로 권한을 걸기 쉽고, 개인에게만 보내는 주소(`/user/queue/...`)를 기본으로 준다. 프론트는 `@stomp/stomp.js`로 붙고 끊기면 스스로 다시 잇는다
- **Alternatives considered**
  - 키트 헬퍼를 기다림: 있는지조차 모르고, 기다리는 동안 P1 스토리(US9)를 못 만든다
  - 원시 WebSocket(STOMP 없이): 구독 주소, 개인 메시지, 권한 검사를 직접 만들어야 한다
  - SSE(서버→브라우저 단방향): 보내기는 REST로 되지만, Spring MVC에서 연결마다 스레드를 잡고 개인·파티 단위 전달을 직접 만들어야 한다
  - 짧은 주기 폴링: SC-007(1초 안에 보임)을 지키려면 요청이 너무 많다
- **제약**: 단순 브로커는 서버 한 대 안에서만 전달한다. 서버를 여러 대로 늘리면 외부 브로커(RabbitMQ STOMP 릴레이 등)로 바꿔야 한다. 지금 배포는 한 대로 본다 (plan.md Complexity Tracking)

## R2. 보내기는 REST, 받기는 STOMP

- **Decision**: 메시지 보내기와 모든 파티 동작(참가, 나가기 등)은 REST API로 한다. 서버는 트랜잭션이 커밋된 뒤에 STOMP로 알린다 (`@TransactionalEventListener(phase = AFTER_COMMIT)`)
- **Rationale**: 검증 오류, 권한 오류, 오류 응답 형식(`ErrorResponse`)을 기존 REST 그대로 쓴다. AC 대부분을 기존 `AcceptanceTest`(MockMvc)로 확인할 수 있다. 커밋 뒤에 알리므로 롤백된 일이 화면에 나타나지 않는다
- **Alternatives considered**: STOMP `SEND`로 메시지 보내기. 오류를 STOMP ERROR 프레임으로 따로 다뤄야 하고 테스트가 어려워진다

## R3. STOMP 인증과 구독 권한

- **Decision**
  - 핸드셰이크 경로 `/ws`는 `PublicPaths`로 연다
  - 인증은 STOMP `CONNECT` 프레임의 `Authorization: Bearer <액세스 토큰>` 헤더로 한다. 기존 JWT 검증을 그대로 쓰는 `ChannelInterceptor`가 맡는다
  - `SUBSCRIBE /topic/parties/{id}`는 그 파티의 지금 멤버만 할 수 있다. 개인 주소 `/user/queue/parties`는 본인만 받는다
- **Rationale**: 웹 토큰은 브라우저 메모리에만 있고 쿠키가 아니다(`stores/auth.ts`). 그래서 핸드셰이크 쿠키로는 인증할 수 없다. 토큰이 15분마다 바뀌어도 연결은 그대로 둔다. 다시 연결할 때만 새 토큰을 쓴다
- **주의**: 멤버에서 빠진 회원이 이미 구독해 둔 것은 서버가 끊어야 한다. 내보내기·나가기 이벤트를 보낼 때 그 회원의 구독을 끊고, 개인 주소로 "내보내짐"을 알린다 (US9-AC8)
- **Alternatives considered**: 쿼리 문자열로 토큰 넘기기. 접속 로그에 토큰이 남는다

## R4. 프론트의 연결 주소

- **Decision**: 브라우저는 백엔드의 `/ws`에 바로 붙는다. 주소는 환경 변수 `NEXT_PUBLIC_WS_URL`로 받는다. 백엔드는 핸드셰이크에 `CorsProperties.allowedOrigins()`와 같은 출처 목록을 쓴다
- **Rationale**: 지금 `/api/*`는 `src/proxy.ts`의 `NextResponse.rewrite`로 넘긴다. 이 경로로 WebSocket 업그레이드가 지나가는지는 확인하지 못했다. 바로 붙으면 이 불확실성이 없다
- **implement에서 확인할 것**: proxy로 업그레이드가 통하면 같은 출처(`/ws`)로 바꿔 환경 변수를 없앤다

## R5. 끊김 복구와 이전 대화 (FR-018, FR-019)

- **Decision**
  - 메시지 id는 파티 안에서 늘어나는 값으로 쓴다
  - 이전 대화는 커서 방식으로 불러온다: `before={id}`(위로 올리기), `after={id}`(다시 연결된 뒤 빠진 것 채우기), 한 번에 50개
  - 다시 연결되면 클라이언트가 마지막으로 받은 id로 `after` 조회를 한 번 한다
- **Rationale**: 대화는 계속 늘어나므로 쪽 번호 방식(`PageResponse`)을 쓰면 새 메시지가 올 때마다 쪽이 밀린다. moa에 커서 방식은 처음이라 Complexity Tracking에 적는다
- **Alternatives considered**: 서버가 끊긴 동안의 메시지를 쌓아 두었다가 보내기. 단순 브로커는 그런 기능이 없다

## R6. 동시성 (FR-009, FR-026)

- **Decision**
  - 참가, 승인, 초대 입장, 나가기, 내보내기, 정보 고치기, 해산은 파티 행을 비관적 락으로 잡고 처리한다 (`@Lock(PESSIMISTIC_WRITE)`, 선례: `RefreshTokenRepository`)
  - 인원과 포지션 자리는 락 안에서 센다
  - "한 번에 한 파티"(FR-026)는 `party_members.user_id` 유니크 제약으로 마지막에 한 번 더 막는다. 두 파티에 동시에 들어가는 경우는 파티 락으로 막히지 않기 때문이다
  - 유니크 충돌(`DataIntegrityViolationException`)은 Service가 받아 B103(이미 다른 파티에 속함)으로 바꾼다. 지금 전역 처리기는 이 예외를 500으로 보내기 때문이다
- **Rationale**: 한 파티의 쓰기는 몇 명 사이에서만 일어나 락 경합이 작다. 낙관적 락(`@Version`)은 moa에 선례가 없고, 충돌하면 다시 시도하는 로직이 필요하다
- **Alternatives considered**: 낙관적 락, 인원 수를 원자적 UPDATE로 늘리기. 포지션별 자리까지 함께 보기 어렵다

## R7. 게임·모드·포지션·티어 코드 (FR-002, FR-021, FR-022)

- **Decision**
  - 공통코드 그룹 `GAME` 하나와 게임마다 `<게임 코드>_MODE`, `<게임 코드>_POSITION`, `<게임 코드>_TIER` 그룹을 둔다. 배그는 포지션 그룹이 없다
  - 초기 코드는 spec.md FR-002 표다
  - 파티에는 코드 값(문자열)만 저장한다
  - 티어의 높낮이는 코드의 정렬 순서(`sortOrder`)로 본다
- **Rationale**: 그룹 이름을 게임 코드로 만들면 게임을 고른 뒤 어느 그룹을 읽을지 규칙으로 정해진다. 게임 관리 기능으로 옮길 때 코드 값을 그대로 쓰면 기존 파티를 고치지 않아도 된다 (SC-006)
- **Alternatives considered**
  - 모드·포지션·티어를 그룹 하나에 두고 코드 앞에 게임을 붙이기(`LOL_TOP`): 게임별로 거르는 일이 화면에 생긴다
  - 파티에 이름을 저장하기: 이름이 바뀌면 어긋난다
- **영향**: 004의 FR-001 표에 그룹을 더한다. `AdminCodesAcceptanceTest`의 기대 목록도 고친다. `DatabaseCleaner`가 시드를 지우므로 `MenuSeed`처럼 시드를 되돌리는 `CodeSeed` 지원 코드를 둔다 (`004-admin-codes` research.md R3)

## R8. 마이그레이션 번호

- **Decision**: moa 기능은 `V2_<스펙 번호>` 대역을 쓴다. 이 기능은 `V2_6__parties.sql`(테이블), `V2_6_1__party_codes.sql`(공통코드 시드)이다
- **Rationale**: `V1_0_x`는 골격, `V1_1` 이후는 키트 헬퍼가 쓴다(`V1_0_1__users.sql:1`). 비어 있는 `V1_1`, `V1_2`, `V1_5`, `V1_6`이 어느 헬퍼의 대역인지 몰라 겹치지 않게 `V2`로 올린다. Flyway는 번호 순서로만 돌므로 나중에 키트 헬퍼가 `V1_x`를 더해도 `outOfOrder` 없이는 이미 적용된 DB에서 막힌다. 그때는 그 헬퍼의 스펙에서 따로 정한다
- **Alternatives considered**: 빈 `V1_5` 같은 번호를 쓰기. 키트 헬퍼와 겹칠 수 있다

## R9. 오류 코드

- **Decision**: `ErrorCode`에 파티 코드를 더한다. 업무 오류는 `B1xx`, 파티 권한 오류는 `A1xx`이다. 목록은 [contracts/party-api.md](./contracts/party-api.md) "오류 코드"에 있다. 화면 문구는 `messages/ko.json`의 `errors`에 더한다
- **Rationale**: 오류 코드는 enum 하나이고, 접두 글자가 종류를 나타낸다(`ErrorCode.java:10`). 프론트 주석이 프로젝트 코드의 예로 `B101`을 든다(`lib/error-messages.ts:6-8`). 방장만 할 수 있는 일은 권한 키(`@RequirePermission`)가 아니라 파티마다의 관계라서 A005 대신 파티 전용 코드로 이유를 알린다

## R10. 계층 배치

- **Decision**
  - 기존 계층(Controller → Application → Service → Reader/Writer → Repository)을 그대로 따르고, 도메인 이름은 `party`이다
  - 실시간 알림 리스너(`PartyRealtimeNotifier`)는 Application 계층(`api/application/v1/party`)에 둔다. 트랜잭션 이벤트를 받고 `SimpMessagingTemplate`으로 보낸다
  - STOMP 설정과 CONNECT 인증은 `common/websocket`에 둔다
  - 구독 권한 검사는 Controller 계층(`api/controller/ws/party`)에서 `PartyApplication`을 불러 한다
- **Rationale**: ArchitectureTest는 `api.*` 계층 사이의 의존만 본다(`consideringOnlyDependenciesInLayers`). 구독 검사는 바깥 요청을 받는 입구라 Controller에 두면 "Application은 Controller·Scheduler만 부른다" 규칙을 지킨다
- **implement에서 확인할 것**: `./gradlew build`의 ArchitectureTest가 통과하는지

## R11. 메뉴와 화면 경로

- **Decision**
  - "모집"과 "내 파티"는 프론트 코드 메뉴(`components/shell/menu.ts`의 `MENU`)에 권한 없이 더한다
  - 화면 경로는 `/recruit`(모집), `/parties/{id}`(파티 화면), `/invite/{code}`(초대 입장), `/my-party`(내 파티)이다. 파티 만들기는 모집 페이지 위에 여는 창이다
- **Rationale**: DB 메뉴 시드로 넣으면 `MenuSeed`와 001의 AC 기대값을 함께 고쳐야 한다. 관리자가 바꿀 일이 없는 사용자 메뉴라 코드 메뉴가 맞다. 권한이 없는 코드 메뉴는 로그인한 모두에게 보인다
- **초대 링크**: 로그인하지 않은 채 열면 `AuthGate`가 `?next=`를 붙여 로그인 화면으로 보내고, 로그인 뒤 돌아온다(`providers/auth-gate.tsx:17`). 새로 만들 것이 없다

## R12. 해산된 파티의 데이터

- **Decision**
  - 파티는 소프트 삭제(`deleted_at`)로 해산한다
  - 멤버, 포지션 자리, 대기 신청은 해산할 때 정리한다. 멤버 행을 지워야 FR-026 유니크가 풀린다
  - 메시지와 참가 제한 기록은 남기되 API로는 보이지 않는다
- **Rationale**: 나중에 신고 처리 기능이 해산된 파티의 대화를 봐야 할 수 있다. 얼마나 남길지는 신고 처리 스펙에서 정한다
- **Alternatives considered**: 해산할 때 메시지를 모두 지우기. 신고 근거가 사라진다
