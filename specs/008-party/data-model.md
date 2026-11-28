# Data Model: 파티 (party)

테이블 이름, 열, 인덱스, COMMENT, 마이그레이션은 `docs/db.md`를 따른다 (2026-11-22에 맞춤).

- 마이그레이션은 파일 하나 `V<다음 번호>__party.sql`이다. 새 테이블 6개, 공통코드 시드, 메뉴 시드, 알림 종류 COMMENT 고치기를 함께 둔다. 번호는 PR 직전에 dev를 병합하고 마지막 번호 + 1로 정한다 (research.md R8)
- 파티는 `SoftDeleteEntity`, 나머지 5개는 `BaseEntity`(하드 삭제, `@HardDelete`)다. 감사 열(`created_at`, `updated_at`, `created_by`, `updated_by`)과 소프트 삭제 열(`deleted_at`, `deleted_by`)은 아래 표에서 생략한다
- 모든 테이블과 열(`id`, 감사 열, `active_key` 제외)에 COMMENT를 단다. 아래 표의 "규칙"이 COMMENT의 근거이고, 상태값 열은 값 이름을 괄호에 나열한다 (예: `'참가 방식 (OPEN, APPROVAL, PRIVATE)'`)
- `DEFAULT`와 외래 키 제약은 두지 않는다. 가리키는 행이 있는지는 Service가 본다

## tb_party (파티)

소프트 삭제. 해산하면 `deleted_at`이 찬다.

| 컬럼 | 형 | 규칙 |
|---|---|---|
| id | BIGINT PK | |
| game_code | VARCHAR(50) NOT NULL | `GAME` 그룹의 사용 중인 코드 (FR-002) |
| mode_code | VARCHAR(50) NULL | `<game>_MODE` 그룹의 코드 |
| tier_min_code, tier_max_code | VARCHAR(50) NULL | `<game>_TIER` 그룹의 코드. 둘 다 있으면 min의 정렬 순서가 max 이하 |
| title | VARCHAR(50) NOT NULL | 1~50자 (FR-003) |
| description | VARCHAR(1000) NOT NULL | 0~1000자 |
| capacity | TINYINT NOT NULL | 2~10. 포지션 조건이 있으면 `tb_party_position.slots`의 합 (FR-022) |
| join_policy | VARCHAR(20) NOT NULL | `OPEN`(공개), `APPROVAL`(승인), `PRIVATE`(비공개) |
| status | VARCHAR(20) NOT NULL | `RECRUITING`, `CLOSED` |
| closed_by_host | BOOLEAN NOT NULL | 방장이 직접 마감했는지 (FR-008) |
| host_user_id | BIGINT NOT NULL | 지금 방장. `tb_party_member`의 HOST와 같다 |
| position_enabled | BOOLEAN NOT NULL | 포지션 조건을 걸었는지 |
| invite_code | CHAR(8) NOT NULL | 대문자·숫자 8자. 헷갈리는 글자(0 O 1 I)는 빼고 만든다 |
| active_key | 가상 컬럼 | 살아 있는 행에만 값이 있다 (`tb_code_group`과 같은 정의) |

- `uk_party_invite_code_active_key (invite_code, active_key)`: 살아 있는 파티끼리 초대 코드가 겹치지 않는다
- `idx_party_recruit_list (status, join_policy, game_code, mode_code, id)`: 모집 페이지 목록. 열이 길어 쓰임새로 이름 짓고 인덱스 COMMENT에 열을 적는다
- `idx_party_host_user_id (host_user_id)`

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

## tb_party_position (포지션 자리)

하드 삭제. 파티 설정의 일부이고 이력이 필요 없다.

| 컬럼 | 형 | 규칙 |
|---|---|---|
| id | BIGINT PK | |
| party_id | BIGINT NOT NULL | |
| position_code | VARCHAR(50) NOT NULL | `<game>_POSITION` 그룹의 코드 |
| slots | TINYINT NOT NULL | 1 이상. 롤은 항상 1 (FR-022) |

- `uk_party_position_party_id_position_code (party_id, position_code)`. 방장의 자리도 여기에 센다

## tb_party_member (파티 멤버)

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

- `uk_party_member_user_id (user_id)`: 한 번에 한 파티 (FR-026, research.md R6)
- `idx_party_member_party_id_joined_at (party_id, joined_at)`: 파티의 멤버 목록과 다음 방장 찾기
- 파티 안의 인원 = 이 파티의 행 수. 포지션별 인원 = 이 파티에서 그 position_code인 행 수

## tb_party_join_request (참가 신청)

엔터티 이름은 `PartyJoinRequest`다. `PartyApplication`은 Application 계층 클래스 이름과 겹친다.

하드 삭제. 처리된 신청은 상태로 남기고, 파티가 해산되면 지운다.

| 컬럼 | 형 | 규칙 |
|---|---|---|
| id | BIGINT PK | |
| party_id | BIGINT NOT NULL | |
| user_id | BIGINT NOT NULL | |
| position_code, tier_code | VARCHAR(50) NULL | 신청할 때 고른 값 |
| status | VARCHAR(20) NOT NULL | `PENDING`, `APPROVED`, `REJECTED`, `CANCELLED` |
| decided_at | DATETIME(6) NULL | 승인·거절·취소한 시각 (NULL: 대기 중) |
| pending_key | 가상 컬럼 | status가 PENDING이면 1, 아니면 NULL (active_key와 같은 방식. 가상 열에는 AUTO_INCREMENT 열을 못 쓴다) |

- `uk_party_join_request_party_id_user_id_pending_key (party_id, user_id, pending_key)`: 한 파티에 대기 신청은 하나 (US5-AC7)
- `idx_party_join_request_user_id (user_id)`: 내 대기 신청 목록 (US8)
- 신청 시각은 `created_at`이다

```text
PENDING ──승인──▶ APPROVED
   │ ──거절──▶ REJECTED  (tb_party_ban에 REJECTED 추가)
   └──취소──▶ CANCELLED  (본인 취소, 다른 파티에 들어감, 승인 방식에서 바뀜)
```

## tb_party_ban (참가 제한)

하드 삭제. 파티가 해산돼도 남긴다 (research.md R12).

| 컬럼 | 형 | 규칙 |
|---|---|---|
| id | BIGINT PK | |
| party_id | BIGINT NOT NULL | |
| user_id | BIGINT NOT NULL | |
| reason | VARCHAR(20) NOT NULL | `KICKED`, `REJECTED` |

- `uk_party_ban_party_id_user_id (party_id, user_id)`. 참가, 신청, 초대 입장 앞에 확인한다 (FR-013)
- `idx_party_ban_user_id (user_id)`

## tb_party_message (파티 메시지)

하드 삭제. 지우는 기능은 없고, 해산돼도 남긴다 (research.md R12).

| 컬럼 | 형 | 규칙 |
|---|---|---|
| id | BIGINT PK | 커서로 쓴다 (research.md R5) |
| party_id | BIGINT NOT NULL | |
| type | VARCHAR(20) NOT NULL | `CHAT`, `SYSTEM` (`tb_notification.type`과 같은 이름) |
| sender_user_id | BIGINT NULL | CHAT이면 보낸 회원 (NULL: SYSTEM) |
| content | VARCHAR(500) NULL | CHAT이면 1~500자 (FR-017) |
| system_event | VARCHAR(30) NULL | SYSTEM이면 `JOINED`, `LEFT`, `KICKED`, `HOST_CHANGED` (FR-020) |
| subject_user_id | BIGINT NULL | SYSTEM의 대상 회원 |

- `idx_party_message_party_id_id (party_id, id)`: 커서 조회
- `idx_party_message_sender_user_id (sender_user_id)`, `idx_party_message_subject_user_id (subject_user_id)`
- 안내 문구는 저장하지 않고 화면이 `system_event`와 대상 회원의 표시 이름으로 만든다. 문구는 ko.json에 둔다

## 공통코드 시드

같은 마이그레이션 파일에 넣는다. 그룹과 초기 코드는 spec.md FR-002 표가 기준이다.

- `tb_code_group`에 12개 그룹, `tb_code`에 코드를 넣는다. 그룹은 `enabled` = true이고 `sort_order`를 준다 (`004-admin-codes` FR-004·FR-008)
- 고정 id를 쓰지 않는다. 코드는 `group_code`로 그룹에 잇는다 (`docs/db.md` "관리자가 화면에서 바꾸는 데이터")
- 관리자가 공통코드 화면에서 바꾸는 데이터라 넣기만 한다. 같은 `group_code`가 이미 있으면 넣지 않는다
- `created_by`와 `updated_by`는 시스템 계정(1)이다

## 메뉴 시드

같은 마이그레이션 파일에 넣는다 (spec.md FR-030, research.md R11).

- `tb_menu`에 사용자 메뉴 두 줄: "모집"(`screen_key` = `RECRUIT`, 공개 범위 `PUBLIC`), "내 파티"(`MY_PARTY`, `MEMBER`). `audience` = `USER`, `enabled` = true, 상위 메뉴 없음, 순서는 001 FR-007 표 순서대로 공지(20)와 고객센터(90) 사이인 30, 40이다
- `screen_key`로 찾고 이미 있으면 넣지 않는다. 고정 id를 쓰지 않는다. 열은 `V2__seed.sql`의 홈·공지 줄을 따른다

## 알림 종류

`tb_notification.type` 열의 COMMENT에 파티 알림 종류 6개(FR-029)를 더한다. 지금은 `'알림 종류 (SYSTEM)'`이다. 같은 마이그레이션에서 `MODIFY COLUMN`으로 고치고, 타입(`VARCHAR(30) NOT NULL`)은 그대로 옮겨 적는다 (`docs/db.md` "COMMENT"). 가장 긴 값 `PARTY_HOST_CHANGED`(18자)가 30자 안에 든다.

## 표시 이름

회원의 표시 이름은 `tb_user.name`이다(FR-027). 응답에는 `userId`와 `displayName`만 싣고 `login_id`는 싣지 않는다.
