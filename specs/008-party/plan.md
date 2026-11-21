# Implementation Plan: 파티 (party)

**Branch**: `docs/008-party` (설계), `feat/008-party` (구현) | **Date**: 2026-11-15 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/008-party/spec.md`

## Summary

회원이 임시 모임인 파티를 만든다. 공개·승인 파티는 모집 페이지에 올라간다. 참가는 바로 참가, 승인, 초대 코드·링크 세 가지 길이 있다. 멤버는 파티 채팅으로 이야기하고, 파티 화면은 새로고침 없이 바뀐다.

키트에 없는 기능이라 moa가 처음부터 만든다. 게임·모드·포지션·티어 목록만 키트 code 헬퍼의 공통코드를 빌려 쓴다 (research.md R7).

실시간 통신은 알림(`007-notifications`)이 얹은 키트 websocket 헬퍼(STOMP)와 프론트 실시간 연결을 그대로 쓰고, 파티는 구독 경로와 구독 권한 검사만 더한다 (research.md R1, 2026-11-16에 직접 구성에서 바꿈). 동작은 모두 REST로 하고, 커밋한 뒤 STOMP로 알린다 (R2). 인원과 포지션 자리는 파티 행 락으로, "한 번에 한 파티"는 DB 유니크로 지킨다 (R6).

남이 한 일(신청, 승인, 거절, 방장 이어받음, 내보내짐, 해산)이 회원에게 닿으면 007의 `NotificationApplication.notify`로 개인 알림을 보낸다 (spec.md FR-029, research.md R13, 2026-11-22). 테이블·마이그레이션은 `docs/db.md`를 따른다 (R8, data-model.md).

## Technical Context

**Language/Version**: Java 21 (Spring Boot 4.1.2), TypeScript 5 (Next 16.3, React 19.2)

**Primary Dependencies**
- 백엔드: 기존 골격(Spring MVC, JPA + QueryDSL, Flyway, JWT)과 키트 websocket 헬퍼(007이 얹음). 더하는 의존성은 없다
- 프론트: 기존(TanStack Query, axios, zustand, react-hook-form + zod, next-intl, shadcn, `@stomp/stompjs`). 더하는 의존성은 없다

**Storage**
- MariaDB 11.4. 새 테이블 6개: `tb_party`, `tb_party_position`, `tb_party_member`, `tb_party_join_request`, `tb_party_ban`, `tb_party_message`
- 마이그레이션은 파일 하나 `V<다음 번호>__party.sql`에 테이블, 공통코드 시드, `tb_notification.type` COMMENT 고치기를 함께 둔다. 번호는 PR 직전에 정한다 (research.md R8). 자세한 것은 [data-model.md](./data-model.md)

**Testing**
- 테스트는 기능 코드(백엔드·프론트)를 모두 만든 뒤 PR 직전에 쓴다 (`CLAUDE.md`). tasks.md도 테스트 작업을 맨 끝 단계에 모은다
- 로컬에서는 빠른 검사(`./gradlew build`, `npm run check`)만 돌린다. 인수 테스트(`dbTest`)와 e2e는 PR CI로 본다
- API 통합: 기존 `AcceptanceTest`(MockMvc + Testcontainers, `dbTest`). AC마다 하나
- 실시간 통합: 007이 만든 `RealtimeAcceptanceTest`(실제 포트로 서버를 띄우고 STOMP 클라이언트로 붙는다)를 상속한다
  - 대상: US9-AC1, AC3, AC7~AC9와 구독 권한
  - 같은 AC를 API 쪽에서는 "저장됨", 실시간 쪽에서는 "전달됨"으로 나눠 확인한다
- 동시성: US3-AC5는 두 스레드로 동시에 참가해 한 명만 성공하는지 본다
- 프론트: Vitest 단위 테스트(폼 스키마, 이벤트 반영), Playwright e2e 3개(spec.md "e2e로 확인할 흐름"). WebSocket은 `page.routeWebSocket`으로 가로챈다 (007 e2e와 같다)

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
- 사용자 스토리 10개, AC 75개, 화면 5개(모집, 만들기 창, 파티, 초대 입장, 내 파티)
- REST API 21개, STOMP 구독 주소 2개

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| 원칙 | 판정 | 근거 |
|---|---|---|
| I. 스펙이 먼저다 | 통과 | spec.md에 `[NEEDS CLARIFICATION]`이 없다. plan에서 정한 값(공통코드 초기값)은 spec.md FR-002 표에 적었다 |
| II. AC마다 테스트 하나 | 통과 | AC 75개마다 테스트를 하나씩 둔다. 이름에 AC 번호를 적는다. 실시간 AC는 "저장"과 "전달"을 나눠 확인한다(Testing). e2e는 핵심 흐름 3개만이다 |
| III. 키트를 따른다 | 통과 | 계층·엔터티·오류·페이지 규칙은 키트 골격을 따른다. 실시간 통신은 007이 얹은 키트 websocket 헬퍼를 고치지 않고 쓴다. 구독 권한 검사는 키트 파일 밖(파티 쪽 설정)에서 더한다 (research.md R3) |
| IV. 기존 기능은 제자리에서 고친다 | 통과 | 004의 spec.md FR-001 표와 `AdminCodesAcceptanceTest` 기대값을 그 자리에서 고친다. 새 기능 폴더는 008 하나다 |
| V. 테스트를 고쳐서 통과시키지 않는다 | 통과 | `AdminCodesAcceptanceTest`는 스펙(004 FR-001 표)을 먼저 고친 뒤 그에 맞춰 기대값을 바꾼다. 실패를 덮으려는 수정이 아니다 |
| VI. 시크릿을 다루지 않는다 | 통과 | STOMP 인증은 기존 액세스 토큰을 쓴다. 새 비밀값과 새 환경 변수가 없다 |

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
- `specs/001-admin-menus/spec.md`: FR-007 메뉴 표와 FR-008 연결 화면 표에 모집·내 파티를 더한다 (2026-11-22)
- `specs/README.md`: 경계 메모를 고친다

### Source Code (repository root)

```text
backend/
├── src/main/java/com/moa/
│   ├── common/websocket/                               # 키트 그대로 (007이 얹음). 고치지 않는다
│   ├── common/exception/ErrorCode.java                 # A101~A103, B101~B113
│   └── api/
│       ├── controller/v1/party/                        # ApiPartyController, ApiPartyMemberController,
│       │   ├── request/                                #   ApiPartyJoinRequestController, ApiPartyMessageController
│       │   └── response/
│       ├── controller/ws/party/                        # 구독 권한 검사 인터셉터와 그것을 붙이는 설정 (research.md R3, R10)
│       ├── application/v1/party/                       # PartyApplication, PartyMessageApplication,
│       │   ├── command/                                #   PartyRealtimeNotifier (커밋 뒤 알림)
│       │   └── query/
│       ├── service/party/                              # PartyService, PartyMemberService,
│       │                                               #   PartyJoinRequestService, PartyMessageService
│       ├── repository/{reader,writer,jpa}/party/
│       ├── entity/party/                               # Party, PartyPosition, PartyMember,
│       │                                               #   PartyJoinRequest(승인 신청), PartyBan, PartyMessage
│       ├── dto/party/
│       ├── screen/PartyScreens.java                    # 연결 화면 RECRUIT, MY_PARTY (spec.md FR-030, research.md R11)
│       └── dto/notification/NotificationType.java      # 기존(007). PARTY_ 종류 6개를 더한다 (research.md R13)
├── src/main/resources/db/migration/
│   └── V<다음 번호>__party.sql                     # 테이블 6개 + 공통코드 시드 + 알림 종류 COMMENT (번호는 PR 직전)
└── src/test/java/com/moa/
    ├── support/CodeSeed.java                           # 공통코드 시드 되돌리기 (MenuSeed와 같은 방식)
    ├── support/PartyFixtures.java                      # 파티·멤버를 바로 넣는 도우미
    ├── api/AdminCodesAcceptanceTest.java               # 기대 그룹 목록 고침
    └── api/party/                                      # Party<스토리>AcceptanceTest, PartyRealtimeAcceptanceTest(기존 RealtimeAcceptanceTest 상속),
                                                        #   PartyNotificationAcceptanceTest(FR-029)

frontend/
├── messages/ko.json                                    # party 키(하위에 recruit 등), errors의 A101~B113, menu의 recruit·myParty
├── src/
│   ├── api/party.ts, api/index.ts                      # api.v1.party
│   ├── providers/realtime-provider.tsx                 # 기존(007). RESYNC_KEYS에 파티 쿼리, 연결 상태 훅을 더한다 (research.md R5)
│   ├── components/notification/realtime-toasts.tsx    # 기존(007). 파티 알림의 관련 화면을 더한다 (spec.md FR-029, research.md R13)
│   ├── lib/party-realtime.ts                           # usePartyChannel, useMyPartyQueue (기존 useRealtimeSubscription 위)
│   ├── lib/schemas/party.ts                            # 만들기·고치기 폼 (zod)
│   └── app/(user)/                                     # 사용자 셸. 로그인하지 않아도 열린다
│       ├── recruit/                                    # 모집 페이지 + 파티 만들기 창
│       ├── parties/[id]/                               # 파티 화면 (정보, 멤버, 채팅, 방장 동작, 참가 창)
│       └── (signed-in)/                                # 로그인이 필요한 화면 (AuthGate)
│           ├── invite/[code]/                          # 초대 입장 화면
│           └── my-party/                               # 내 파티
├── tests/unit/party-*.test.ts
└── e2e/party.spec.ts
```

**Structure Decision**: 백엔드는 기존 `com.moa.api` 계층을 그대로 쓰고 도메인 이름은 `party`이다. 실시간 기반은 007이 얹은 키트 `common/websocket`과 프론트 `RealtimeProvider`를 함께 쓴다. 프론트는 사용자 셸 `(user)` 아래에 화면을 두고(로그인이 필요한 화면은 `(user)/(signed-in)`), 메뉴는 연결 화면(`PartyScreens`)과 서버 사용자 메뉴로 더한다 (research.md R11).

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| STOMP 구독 권한 검사를 처음 둔다 (키트 헬퍼는 CONNECT 인증만 한다) | 파티 주소는 그 파티 멤버만 받아야 한다 (research.md R3) | 검사 없이 두면 아무 회원이나 남의 파티 채팅을 구독할 수 있다. 키트 `WebSocketConfig`를 고치는 대신 파티 쪽 설정으로 붙인다 |
| 커서 페이지(`before`·`after`)를 처음 쓴다 | 대화는 계속 늘어나 쪽 번호가 밀린다. 다시 연결한 뒤 빠진 메시지를 채워야 한다 (FR-018, FR-019) | `PageResponse`의 쪽 번호로는 새 메시지가 올 때마다 겹치거나 빠진다 |
