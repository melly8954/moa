# Implementation Plan: 파티 (party)

**Branch**: `docs/008-party` (설계), `feat/008-party` (구현) | **Date**: 2026-11-15 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/008-party/spec.md`

## Summary

회원이 임시 모임인 파티를 만든다. 공개·승인 파티는 모집 페이지에 올라간다. 참가는 바로 참가, 승인, 초대 코드·링크 세 가지 길이 있다. 멤버는 파티 채팅으로 이야기하고, 파티 화면은 새로고침 없이 바뀐다.

키트에 없는 기능이라 moa가 처음부터 만든다. 게임·모드·포지션·티어 목록만 키트 code 헬퍼의 공통코드를 빌려 쓴다 (research.md R7).

실시간 통신은 moa가 Spring WebSocket + STOMP로 직접 구성한다 (research.md R1). 동작은 모두 REST로 하고, 커밋한 뒤 STOMP로 알린다 (R2). 인원과 포지션 자리는 파티 행 락으로, "한 번에 한 파티"는 DB 유니크로 지킨다 (R6).

## Technical Context

**Language/Version**: Java 21 (Spring Boot 4.1.2), TypeScript 5 (Next 16.3, React 19.2)

**Primary Dependencies**
- 백엔드: 기존 골격(Spring MVC, JPA + QueryDSL, Flyway, JWT)
- 백엔드에 더함: `spring-boot-starter-websocket`
- 프론트: 기존(TanStack Query, axios, zustand, react-hook-form + zod, next-intl, shadcn)
- 프론트에 더함: `@stomp/stomp.js`. 버전은 implement 때 고정한다 (exact version)

**Storage**
- MariaDB 11.4. 새 테이블 6개: `parties`, `party_positions`, `party_members`, `party_join_requests`, `party_bans`, `party_messages` (`V2_6__parties.sql`)
- 공통코드 시드 (`V2_6_1__party_codes.sql`). 자세한 것은 [data-model.md](./data-model.md)

**Testing**
- API 통합: 기존 `AcceptanceTest`(MockMvc + Testcontainers, `./gradlew dbTest`). AC마다 하나
- 실시간 통합: 새 지원 클래스 `RealtimeTest`. `@SpringBootTest(webEnvironment = RANDOM_PORT)`로 서버를 띄우고 STOMP 클라이언트(`WebSocketStompClient`)로 확인한다
  - 대상: US9-AC1, AC3, AC7~AC9와 구독 권한
  - 같은 AC를 API 쪽에서는 "저장됨", 실시간 쪽에서는 "전달됨"으로 나눠 확인한다
- 동시성: US3-AC5는 두 스레드로 동시에 참가해 한 명만 성공하는지 본다
- 프론트: Vitest 단위 테스트(폼 스키마, 이벤트 반영), Playwright e2e 3개(spec.md "e2e로 확인할 흐름"). WebSocket은 `page.routeWebSocket`으로 가로챈다

**Target Platform**: 웹 (데스크톱·휴대폰 브라우저)

**Project Type**: 웹 서비스 (모노레포 `backend/`, `frontend/`)

**Performance Goals**
- 메시지가 1초 안에 다른 멤버에게 보인다 (SC-007)
- 모집 페이지 한 쪽(20개)을 1초 안에 보인다

**Constraints**
- 백엔드 서버 한 대 (단순 브로커, research.md R1)
- 로그인 아이디를 응답에 싣지 않는다 (FR-027)
- 계층 규칙은 ArchitectureTest를 따른다 (research.md R10)

**Scale/Scope**
- 사용자 스토리 10개, AC 67개, 화면 5개(모집, 만들기 창, 파티, 초대 입장, 내 파티)
- REST API 21개, STOMP 구독 주소 2개

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| 원칙 | 판정 | 근거 |
|---|---|---|
| I. 스펙이 먼저다 | 통과 | spec.md에 `[NEEDS CLARIFICATION]`이 없다. plan에서 정한 값(공통코드 초기값)은 spec.md FR-002 표에 적었다 |
| II. AC마다 테스트 하나 | 통과 | AC 67개마다 테스트를 하나씩 둔다. 이름에 AC 번호를 적는다. 실시간 AC는 "저장"과 "전달"을 나눠 확인한다(Testing). e2e는 핵심 흐름 3개만이다 |
| III. 키트를 따른다 | 통과 | 계층·엔터티·오류·페이지 규칙은 키트 골격을 따른다. 실시간 통신은 키트에 헬퍼가 없어 moa가 구성한다. 키트 규칙을 어기는 것이 아니라 키트 밖의 새 기반이다. 위험과 대안은 Complexity Tracking에 적었다 |
| IV. 기존 기능은 제자리에서 고친다 | 통과 | 004의 spec.md FR-001 표와 `AdminCodesAcceptanceTest` 기대값을 그 자리에서 고친다. 새 기능 폴더는 008 하나다 |
| V. 테스트를 고쳐서 통과시키지 않는다 | 통과 | `AdminCodesAcceptanceTest`는 스펙(004 FR-001 표)을 먼저 고친 뒤 그에 맞춰 기대값을 바꾼다. 실패를 덮으려는 수정이 아니다 |
| VI. 시크릿을 다루지 않는다 | 통과 | STOMP 인증은 기존 액세스 토큰을 쓴다. 새 비밀값은 없다. `NEXT_PUBLIC_WS_URL`은 공개 주소다 |

Phase 1 설계 뒤 다시 확인했다. 위반이 없다.

## Project Structure

### Documentation (this feature)

```text
specs/008-party/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── party-api.md
│   └── party-realtime.md
├── checklists/requirements.md
└── tasks.md             # /speckit-tasks가 만든다
```

함께 고치는 문서:
- `specs/004-admin-codes/spec.md`: FR-001 표에 파티 코드 그룹을 더한다
- `specs/README.md`: 경계 메모를 고친다

### Source Code (repository root)

```text
backend/
├── build.gradle                                        # spring-boot-starter-websocket
├── src/main/java/com/moa/
│   ├── common/websocket/                               # STOMP 설정, CONNECT 인증 인터셉터, 핸드셰이크 PublicPaths
│   ├── common/exception/ErrorCode.java                 # A101~A102, B101~B113
│   └── api/
│       ├── controller/v1/party/                        # ApiPartyController, ApiPartyMemberController,
│       │   ├── request/                                #   ApiPartyJoinRequestController, ApiPartyMessageController
│       │   └── response/
│       ├── controller/ws/party/                        # 구독 권한 검사 (research.md R10)
│       ├── application/v1/party/                       # PartyApplication, PartyMessageApplication,
│       │   ├── command/                                #   PartyRealtimeNotifier (커밋 뒤 알림)
│       │   └── query/
│       ├── service/party/                              # PartyService, PartyMemberService,
│       │                                               #   PartyJoinRequestService, PartyMessageService
│       ├── repository/{reader,writer,jpa}/party/
│       ├── entity/party/                               # Party, PartyPosition, PartyMember,
│       │                                               #   PartyJoinRequest(승인 신청), PartyBan, PartyMessage
│       └── dto/party/
├── src/main/resources/db/migration/
│   ├── V2_6__parties.sql
│   └── V2_6_1__party_codes.sql
└── src/test/java/com/moa/
    ├── support/CodeSeed.java                           # 공통코드 시드 되돌리기 (MenuSeed와 같은 방식)
    ├── support/RealtimeTest.java                       # 실서버 + STOMP 클라이언트 기반
    ├── api/AdminCodesAcceptanceTest.java               # 기대 그룹 목록 고침
    └── api/party/                                      # Party<스토리>AcceptanceTest, PartyRealtimeTest

frontend/
├── package.json                                        # @stomp/stomp.js
├── messages/ko.json                                    # party 키(하위에 recruit 등), errors의 A101~B113, menu의 recruit·myParty
├── src/
│   ├── api/party.ts, api/index.ts                      # api.v1.party
│   ├── lib/realtime/                                   # STOMP 연결, 다시 잇기, 빠진 메시지 채우기
│   ├── lib/schemas/party.ts                            # 만들기·고치기 폼 (zod)
│   ├── components/shell/menu.ts                        # "모집", "내 파티" 코드 메뉴
│   └── app/(main)/
│       ├── recruit/                                    # 모집 페이지 + 파티 만들기 창
│       ├── parties/[id]/                               # 파티 화면 (정보, 멤버, 채팅, 방장 동작, 참가 창)
│       ├── invite/[code]/                              # 초대 입장 화면
│       └── my-party/                                   # 내 파티
├── tests/unit/party-*.test.ts
└── e2e/party.spec.ts
```

**Structure Decision**: 백엔드는 기존 `com.moa.api` 계층을 그대로 쓰고 도메인 이름은 `party`이다. 실시간 기반은 다른 기능(크루, 메신저)도 쓸 수 있게 `common/websocket`에 둔다. 프론트는 `(main)` 아래에 사용자 화면을 두고, 메뉴는 코드 메뉴로 더한다 (research.md R11).

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| 키트 websocket 헬퍼 없이 moa가 STOMP를 구성한다 (원칙 위반은 아니고, 키트 밖 기반을 새로 두는 위험을 적는다) | 파티 채팅과 실시간 파티 화면(US9, FR-018, FR-028)이 P1이다. 키트에 헬퍼가 있는지 확인할 수 없었고, 사용자가 직접 구성으로 정했다 (2026-11-15) | 키트 헬퍼를 기다리면 P1을 만들 수 없다. 나중에 키트 헬퍼가 생기면 `common/websocket`만 바꾸도록 실시간 기반을 한곳에 모은다 |
| 서버 안 단순 브로커 (서버 한 대 전제) | 외부 브로커 없이 바로 시작할 수 있다 | 외부 브로커(RabbitMQ 등)는 지금 인프라에 없다. 서버를 늘릴 때 브로커 릴레이로 바꾼다 |
| 커서 페이지(`before`·`after`)를 처음 쓴다 | 대화는 계속 늘어나 쪽 번호가 밀린다. 다시 연결한 뒤 빠진 메시지를 채워야 한다 (FR-018, FR-019) | `PageResponse`의 쪽 번호로는 새 메시지가 올 때마다 겹치거나 빠진다 |
| 실시간 통합 테스트 지원(`RealtimeTest`)을 새로 둔다 | MockMvc는 실제 소켓을 열지 않아 STOMP 전달을 확인할 수 없다 | 이벤트 발행만 단위 테스트하면 구독 권한과 커밋 뒤 전달을 확인하지 못한다 |
