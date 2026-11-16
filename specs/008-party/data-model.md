# Data Model: 파티 (party)

새 테이블 6개를 `V2_6__parties.sql`에 만든다. 공통코드 시드는 `V2_6_1__party_codes.sql`에 넣는다 (research.md R7, R8).
모든 테이블에 `created_at`, `updated_at`, `created_by`, `updated_by`가 있다 (`BaseEntity`). 아래 표에서는 생략한다.

## parties (파티)

소프트 삭제. 해산하면 `deleted_at`이 찬다 (`SoftDeleteEntity`).

| 컬럼 | 형 | 규칙 |
|---|---|---|
| id | BIGINT PK | |
| game_code | VARCHAR(50) NOT NULL | `GAME` 그룹의 사용 중인 코드 (FR-002) |
| mode_code | VARCHAR(50) NULL | `<game>_MODE` 그룹의 코드 |
| tier_min_code, tier_max_code | VARCHAR(50) NULL | `<game>_TIER` 그룹의 코드. 둘 다 있으면 min의 정렬 순서가 max 이하 |
| title | VARCHAR(50) NOT NULL | 1~50자 (FR-003) |
| description | VARCHAR(1000) NOT NULL | 0~1000자 |
| capacity | TINYINT NOT NULL | 2~10. 포지션 조건이 있으면 `party_positions.slots`의 합 (FR-022) |
| join_policy | VARCHAR(20) NOT NULL | `OPEN`(공개), `APPROVAL`(승인), `PRIVATE`(비공개) |
| status | VARCHAR(20) NOT NULL | `RECRUITING`, `CLOSED` |
| closed_by_host | BOOLEAN NOT NULL | 방장이 직접 마감했는지 (FR-008) |
| host_user_id | BIGINT NOT NULL | 지금 방장. `party_members`의 HOST와 같다 |
| position_enabled | BOOLEAN NOT NULL | 포지션 조건을 걸었는지 |
| invite_code | CHAR(8) NOT NULL | 대문자·숫자 8자. 헷갈리는 글자(0 O 1 I)는 빼고 만든다 |
| deleted_at, deleted_by | | 해산 |
| active_key | 가상 컬럼 | `UNIQUE(invite_code, active_key)`: 살아 있는 파티끼리 초대 코드가 겹치지 않는다 |

색인: `(status, join_policy, game_code, mode_code, id DESC)`로 모집 페이지 목록을 찾는다.

### 상태 전이

```text
           만들기
             │
             ▼
   ┌──── RECRUITING ◀────────────────┐
   │         │                        │
   │  정원 참 │ 방장 마감               │ 자리 남 && !closed_by_host
   │         ▼                        │ 또는 방장 재개
   │      CLOSED ─────────────────────┘
   │         │
   ▼         ▼
 해산 (deleted_at) ◀── 방장 해산, 마지막 멤버 나감
```

- 정원이 차서 마감되면 `closed_by_host = false`, 방장이 마감하면 `true`이다
- 재개는 방장만 하고, 자리가 남았을 때만 된다. 재개하면 `closed_by_host = false`
- `PRIVATE`는 상태와 따로 논다. 모집 페이지에는 `RECRUITING`이면서 `OPEN`·`APPROVAL`인 것만 보인다 (FR-004)

## party_positions (포지션 자리)

하드 삭제(`@HardDelete`). 파티 설정의 일부이고 이력이 필요 없다.

| 컬럼 | 형 | 규칙 |
|---|---|---|
| id | BIGINT PK | |
| party_id | BIGINT NOT NULL | |
| position_code | VARCHAR(50) NOT NULL | `<game>_POSITION` 그룹의 코드 |
| slots | TINYINT NOT NULL | 1 이상. 롤은 항상 1 (FR-022) |

`UNIQUE(party_id, position_code)`. 방장의 자리도 여기에 센다.

## party_members (파티 멤버)

하드 삭제. 나가거나 내보내지면 행을 지운다. 이력은 채팅의 안내 메시지로 남는다.

| 컬럼 | 형 | 규칙 |
|---|---|---|
| id | BIGINT PK | |
| party_id | BIGINT NOT NULL | |
| user_id | BIGINT NOT NULL | |
| role | VARCHAR(20) NOT NULL | `HOST`, `MEMBER` |
| position_code | VARCHAR(50) NULL | 포지션 조건이 있으면 NOT NULL (Service가 확인) |
| tier_code | VARCHAR(50) NULL | 본인이 적은 티어 (FR-024) |
| joined_at | DATETIME(6) NOT NULL | 방장을 잇는 순서. 같으면 id가 작은 쪽 (FR-010) |

- `UNIQUE(user_id)`: 한 번에 한 파티 (FR-026, research.md R6)
- 파티 안의 인원 = 이 파티의 행 수. 포지션별 인원 = 이 파티에서 그 position_code인 행 수

## party_join_requests (참가 신청)

엔터티 이름은 `PartyJoinRequest`다. `PartyApplication`은 Application 계층 클래스 이름과 겹친다.

하드 삭제. 처리된 신청은 상태로 남기고, 파티가 해산되면 지운다.

| 컬럼 | 형 | 규칙 |
|---|---|---|
| id | BIGINT PK | |
| party_id | BIGINT NOT NULL | |
| user_id | BIGINT NOT NULL | |
| position_code, tier_code | VARCHAR(50) NULL | 신청할 때 고른 값 |
| status | VARCHAR(20) NOT NULL | `PENDING`, `APPROVED`, `REJECTED`, `CANCELLED` |
| decided_at | DATETIME(6) NULL | 승인·거절·취소한 시각 |
| pending_key | 가상 컬럼 | status가 PENDING이면 0, 아니면 id. `UNIQUE(party_id, user_id, pending_key)`: 한 파티에 대기 신청은 하나 (US5-AC7) |

신청 시각은 `created_at`이다.

```text
PENDING ──승인──▶ APPROVED
   │ ──거절──▶ REJECTED  (party_bans에 REJECTED 추가)
   └──취소──▶ CANCELLED  (본인 취소, 다른 파티에 들어감, 승인 방식에서 바뀜)
```

## party_bans (참가 제한)

하드 삭제. 파티가 해산돼도 남긴다 (research.md R12).

| 컬럼 | 형 | 규칙 |
|---|---|---|
| id | BIGINT PK | |
| party_id | BIGINT NOT NULL | |
| user_id | BIGINT NOT NULL | |
| reason | VARCHAR(20) NOT NULL | `KICKED`, `REJECTED` |

`UNIQUE(party_id, user_id)`. 참가, 신청, 초대 입장 앞에 확인한다 (FR-013).

## party_messages (파티 메시지)

하드 삭제. 지우는 기능은 없고, 해산돼도 남긴다 (research.md R12).

| 컬럼 | 형 | 규칙 |
|---|---|---|
| id | BIGINT PK | 커서로 쓴다 (research.md R5) |
| party_id | BIGINT NOT NULL | |
| type | VARCHAR(20) NOT NULL | `CHAT`, `SYSTEM` |
| sender_user_id | BIGINT NULL | CHAT이면 보낸 회원, SYSTEM이면 NULL |
| content | VARCHAR(500) NULL | CHAT이면 1~500자 (FR-017) |
| system_event | VARCHAR(30) NULL | SYSTEM이면 `JOINED`, `LEFT`, `KICKED`, `HOST_CHANGED` (FR-020) |
| subject_user_id | BIGINT NULL | SYSTEM의 대상 회원 |

색인: `(party_id, id)`.
안내 문구는 저장하지 않고 화면이 `system_event`와 대상 회원의 표시 이름으로 만든다. 문구는 ko.json에 둔다.

## 공통코드 시드

그룹과 초기 코드는 spec.md FR-002 표가 기준이다. 시드는 `code_groups`와 `codes`에 id를 고정해 넣는다. `created_by`와 `updated_by`는 1이다 (`V1_7__menus.sql` 선례).

## 표시 이름

회원의 표시 이름은 `users.name`이다(FR-027). 응답에는 `userId`와 `displayName`만 싣고 `login_id`는 싣지 않는다.
