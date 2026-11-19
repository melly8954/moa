# Research: 파티 (party)

Technical Context의 미결은 실시간 통신 하나였다. 처음(2026-11-15)에는 moa가 직접 구성하기로 했으나, 그 사이 알림(`007-notifications`)이 키트 websocket 헬퍼를 얹어 2026-11-16에 그것을 쓰는 것으로 바꿨다 (R1). 나머지는 코드를 확인해 정했다.

## R1. 실시간 통신 (FR-018, FR-028)

- **Decision**: `007-notifications`가 얹은 키트 websocket 헬퍼(STOMP, 서버 안 단순 브로커)와 프론트 실시간 연결을 그대로 쓴다. 파티는 그 위에 구독 경로와 구독 권한 검사만 더한다
  - 백엔드: `common/websocket/`의 `WebSocketConfig`(`/ws`, `/topic`·`/queue`, `/app`, `/user`), `StompAuthInterceptor`(CONNECT 토큰 인증), `StompPrincipal`(이름이 사용자 ID). 키트 파일이라 고치지 않는다
  - 프론트: `providers/realtime-provider.tsx`의 `RealtimeProvider`(로그인하면 연결, 끊기면 5초 뒤 다시 잇기)와 `useRealtimeSubscription(destination, handler)`. 라이브러리는 이미 있는 `@stomp/stompjs`다
  - 구독 경로 이름은 007 규칙을 따른다: 여럿 `/topic/<기능>`, 한 사용자 `/user/queue/<기능>` (`007-notifications` contracts/realtime.md)
- **Rationale**: 같은 연결을 알림과 함께 쓴다. 기반을 둘 두면 연결이 둘이 되고, 인증·다시 잇기를 두 번 만든다. 처음 결정의 이유(키트에 헬퍼가 있는지 확인할 수 없음)는 007이 키트 헬퍼(`springboot/v0.5.0`)를 얹으면서 없어졌다
- **Alternatives considered**
  - 파티용 연결을 따로 직접 구성(처음 결정): 같은 이름의 `WebSocketConfig`·`StompAuthInterceptor`가 이미 있어 겹친다
  - SSE, 짧은 주기 폴링: 처음 검토 때와 같은 이유로 버린다 (SC-007 1초 안에 보임)
- **제약**: 단순 브로커는 서버 한 대 안에서만 전달한다 (키트 HELPER 주의). 서버를 여러 대로 늘리면 외부 브로커로 바꾸는 일은 키트·007과 함께 정한다

## R2. 보내기는 REST, 받기는 STOMP

- **Decision**: 메시지 보내기와 모든 파티 동작(참가, 나가기 등)은 REST API로 한다. 서버는 트랜잭션이 커밋된 뒤에 STOMP로 알린다 (`@TransactionalEventListener(phase = AFTER_COMMIT)`)
- **Rationale**: 검증 오류, 권한 오류, 오류 응답 형식(`ErrorResponse`)을 기존 REST 그대로 쓴다. AC 대부분을 기존 `AcceptanceTest`(MockMvc)로 확인할 수 있다. 커밋 뒤에 알리므로 롤백된 일이 화면에 나타나지 않는다
- **Alternatives considered**: STOMP `SEND`로 메시지 보내기. 오류를 STOMP ERROR 프레임으로 따로 다뤄야 하고 테스트가 어려워진다

## R3. STOMP 인증과 구독 권한

- **Decision**
  - 핸드셰이크(`/ws`의 `PublicPaths`)와 CONNECT 토큰 인증은 키트 헬퍼가 이미 한다 (R1). 파티는 고치지 않는다
  - 기존 인터셉터는 CONNECT만 본다. 그래서 `SUBSCRIBE /topic/parties/{id}`를 그 파티의 지금 멤버만 할 수 있게 하는 검사를 파티가 더한다
    - 키트 `WebSocketConfig`를 고치지 않고, 파티 쪽에 `WebSocketMessageBrokerConfigurer`를 하나 더 두어 `configureClientInboundChannel`로 검사 인터셉터를 붙인다. Spring은 이 설정을 여럿 모아 쓴다
    - SUBSCRIBE 프레임의 사용자는 CONNECT 때 세운 `StompPrincipal`이라, 인터셉터 순서와 상관없이 사용자 ID를 읽을 수 있다
  - 개인 주소 `/user/queue/parties`는 `StompPrincipal` 이름(사용자 ID)으로 보내므로 본인만 받는다. 따로 검사하지 않는다
- **Rationale**: 웹 토큰은 브라우저 메모리에만 있고 쿠키가 아니다(`stores/auth.ts`). 그래서 핸드셰이크 쿠키로는 인증할 수 없다. 토큰이 바뀌면 프론트 `RealtimeProvider`가 다시 붙는다
- **주의**
  - 멤버에서 빠진 회원이 이미 구독해 둔 것은 서버가 끊어야 한다. 내보내기·나가기 이벤트를 보낼 때 그 회원의 구독을 끊고, 개인 주소로 "내보내짐"을 알린다 (US9-AC8)
  - 검사에서 막은 SUBSCRIBE가 ERROR 프레임이 되면, 알림과 함께 쓰는 연결 전체가 끊길 수 있다. implement에서 실제 동작을 확인한다. 화면은 멤버일 때(`me.relation`이 MEMBER·HOST)만 파티 주소를 구독한다
- **Alternatives considered**: 쿼리 문자열로 토큰 넘기기. 접속 로그에 토큰이 남는다

## R4. 프론트의 연결 주소

- **Decision**: 007이 정한 것을 그대로 쓴다. 브라우저는 백엔드의 `/ws`에 바로 붙고, 주소는 서버 환경 변수 `WS_URL`을 `app/layout.tsx`가 요청 때 읽어 `RealtimeProvider`에 넘긴다 (`007-notifications` research R2). 파티가 더할 설정은 없다
- **Rationale**: 프론트 프록시(`src/proxy.ts`)는 WebSocket을 넘기지 못한다는 것을 007에서 확인했다. 처음에 적은 `NEXT_PUBLIC_WS_URL`과 "proxy로 통하는지 확인"은 필요 없어졌다

## R5. 끊김 복구와 이전 대화 (FR-018, FR-019)

- **Decision**
  - 메시지 id는 파티 안에서 늘어나는 값으로 쓴다
  - 이전 대화는 커서 방식으로 불러온다: `before={id}`(위로 올리기), `after={id}`(다시 연결된 뒤 빠진 것 채우기), 한 번에 50개
  - 다시 연결되면 클라이언트가 마지막으로 받은 id로 `after` 조회를 한 번 한다
  - `RealtimeProvider`는 다시 이어질 때 정해진 쿼리(`RESYNC_KEYS`)만 다시 읽고, 연결 상태를 밖으로 내보내지 않는다. 파티는 `RESYNC_KEYS`에 파티 쿼리를 더하고, 연결 상태를 읽는 훅을 하나 내보내 "끊김" 표시와 `after` 채우기에 쓴다. 이 파일은 키트가 아니라 moa가 먼저 만든 것이라 고칠 수 있다
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
- **004와 맞출 것**: 004는 그룹을 `code_groups`(그룹 코드, 이름, 설명, 사용 여부)에 두고 관리자가 화면에서 다루며, 그룹과 항목을 지우지 않고 사용 여부로만 끈다 (2026-11-20, `004-admin-codes` FR-002·FR-008). 파티의 그룹 12개와 코드는 시드 SQL에 사용 중(`enabled` = true)으로 넣는다. 꺼진 그룹은 화면용 목록이 빈 목록이므로 파티 화면은 빈 선택 상자를 그대로 보인다. 004의 바뀐 열(`V2_9`)이 먼저 들어가야 한다

## R8. 마이그레이션 번호

- **Decision**: moa 기능은 `V2` 대역에서 차례 번호를 쓴다. dev의 마지막이 `V2_6__admin_menu_groups.sql`(2026-11-16)이므로 이 기능은 `V2_7__parties.sql`(테이블), `V2_7_1__party_codes.sql`(공통코드 시드)이다. implement를 시작할 때 dev의 마지막 번호를 다시 보고, 그사이 누가 썼으면 다음 번호로 옮긴다
- **Rationale**: `V1_0_x`는 골격, `V1_1` 이후는 키트 헬퍼가 쓴다(`V1_0_1__users.sql:1`). moa의 마이그레이션은 `V2`부터 기능 구분 없이 차례로 번호를 받아 왔다(`V2_1__notices.sql`, `V2_3__notifications.sql` 등). 처음에 적은 "`V2_<스펙 번호>`" 규칙은 dev가 따르지 않아 `V2_6`이 이미 다른 파일에 쓰였다
- **Alternatives considered**: 빈 `V1_5` 같은 번호를 쓰기. 키트 헬퍼와 겹칠 수 있다

## R9. 오류 코드

- **Decision**: `ErrorCode`에 파티 코드를 더한다. 업무 오류는 `B1xx`, 파티 권한 오류는 `A1xx`이다. 목록은 [contracts/party-api.md](./contracts/party-api.md) "오류 코드"에 있다. 화면 문구는 `messages/ko.json`의 `errors`에 더한다
- **Rationale**: 오류 코드는 enum 하나이고, 접두 글자가 종류를 나타낸다(`ErrorCode.java:10`). 프론트 주석이 프로젝트 코드의 예로 `B101`을 든다(`lib/error-messages.ts:6-8`). 방장만 할 수 있는 일은 권한 키(`@RequirePermission`)가 아니라 파티마다의 관계라서 A005 대신 파티 전용 코드로 이유를 알린다

## R10. 계층 배치

- **Decision**
  - 기존 계층(Controller → Application → Service → Reader/Writer → Repository)을 그대로 따르고, 도메인 이름은 `party`이다
  - 실시간 알림 리스너(`PartyRealtimeNotifier`)는 Application 계층(`api/application/v1/party`)에 둔다. 트랜잭션 이벤트를 받고 `SimpMessagingTemplate`으로 보낸다
  - STOMP 설정과 CONNECT 인증은 키트의 `common/websocket`에 이미 있다 (R1)
  - 구독 권한 검사(인터셉터와 그것을 붙이는 설정)는 Controller 계층(`api/controller/ws/party`)에서 `PartyApplication`을 불러 한다
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
