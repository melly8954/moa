# Data Model: 회원 프로필 보기, 친구, 팔로우 (014)

규칙은 `docs/db.md`다. 마이그레이션은 파일 하나 `V<다음 번호>__friends_follow.sql`(research.md R14).

- 모든 테이블과 열(`id`, 감사 열, `active_key` 제외)에 COMMENT를 단다. 아래 "규칙"이 COMMENT의 근거다
- `DEFAULT`와 외래 키는 두지 않는다. 가리키는 행이 있는지는 Service가 본다
- 관계 테이블은 이력이 필요 없어 하드 삭제다. 끊거나 처리하면 행을 지운다
- 상대 회원은 늘 `User` 기본 조회(삭제·탈퇴 유예 제외)와 조인해 읽는다. 탈퇴 유예 중인 회원의 행은 남지만 보이지 않는다 (research.md R15)

## tb_user_public_id (회원 공개 ID)

1:1 테이블. 감사 열 없음(만든 뒤 바뀌지 않는다). COMMENT: `'회원 공개 ID (PK: 1:1 테이블; 감사 열 없음: 만든 뒤 바뀌지 않음)'`

| 컬럼 | 형 | 규칙 |
|---|---|---|
| user_id | BIGINT NOT NULL PK | 회원 ID (`USER`만) |
| public_id | CHAR(36) NOT NULL | UUID. 프로필 주소 `/users/{publicId}` |
| created_at | DATETIME(6) NOT NULL | 만든 시각 |

- `uk_user_public_id_public_id (public_id)`
- 만드는 때: 소셜 가입, 관리자의 회원 만들기, 마이그레이션(기존 회원. 탈퇴 유예 중인 회원 포함) (research.md R1, R14)

## tb_friend (친구)

한 쌍을 두 행으로 둔다 (research.md R2). `BaseEntity`.

| 컬럼 | 형 | 규칙 |
|---|---|---|
| id | BIGINT PK | 커서 (research.md R5) |
| user_id | BIGINT NOT NULL | 이 행의 주인 |
| friend_user_id | BIGINT NOT NULL | 친구 |

- `uk_friend_user_id_friend_user_id (user_id, friend_user_id)`: 같은 친구는 한 번
- `idx_friend_user_id_id (user_id, id)`: 친구 목록 커서. 유니크와 앞 열이 같지만 정렬 열이 달라 둔다
- `idx_friend_friend_user_id (friend_user_id)`: 파기 때 상대 쪽 행 찾기
- 맺은 시각은 `created_at`이다. 한 회원의 행 수 ≤ 5,000 (FR-005)

## tb_friend_request (대기 친구 요청)

대기 중인 요청만 남는다. 수락·거절·취소하면 지운다 (research.md R3). `BaseEntity`.

| 컬럼 | 형 | 규칙 |
|---|---|---|
| id | BIGINT PK | 커서 |
| requester_id | BIGINT NOT NULL | 보낸 회원 |
| receiver_id | BIGINT NOT NULL | 받은 회원 |
| pair_low_id | BIGINT NOT NULL | 두 회원 ID 중 작은 값 |
| pair_high_id | BIGINT NOT NULL | 두 회원 ID 중 큰 값 |

- `uk_friend_request_pair_low_id_pair_high_id (pair_low_id, pair_high_id)`: 두 회원 사이에 대기 요청은 하나 (방향 무관)
- `idx_friend_request_receiver_id_id (receiver_id, id)`: 받은 요청 목록
- `idx_friend_request_requester_id_id (requester_id, id)`: 보낸 요청 목록
- 요청한 시각은 `created_at`이다

## tb_friend_rejection (친구 요청 거절 기록)

30일 동안 다시 요청을 막는 데만 쓴다 (research.md R4). 감사 열 없음. COMMENT: `'친구 요청 거절 기록 (감사 열 없음: 30일 뒤 정리기가 지움)'`

| 컬럼 | 형 | 규칙 |
|---|---|---|
| id | BIGINT PK | |
| requester_id | BIGINT NOT NULL | 거절당한 회원(요청을 보냈던 쪽) |
| receiver_id | BIGINT NOT NULL | 거절한 회원 |
| rejected_at | DATETIME(6) NOT NULL | 거절한 시각 |

- `uk_friend_rejection_requester_id_receiver_id (requester_id, receiver_id)`: 다시 거절하면 `rejected_at`만 새로 한다
- `idx_friend_rejection_receiver_id (receiver_id)`: 파기 때 거절한 쪽 행 찾기
- `idx_friend_rejection_rejected_at (rejected_at)`: 정리기

## tb_follow (팔로우)

승인된 팔로우만 있다. 한쪽 방향 한 행 (research.md R6). `BaseEntity`.

| 컬럼 | 형 | 규칙 |
|---|---|---|
| id | BIGINT PK | 커서 |
| follower_id | BIGINT NOT NULL | 팔로우하는 회원 |
| followee_id | BIGINT NOT NULL | 팔로우 받는 회원 |

- `uk_follow_follower_id_followee_id (follower_id, followee_id)`
- `idx_follow_follower_id_id (follower_id, id)`: 팔로잉 목록 커서
- `idx_follow_followee_id_id (followee_id, id)`: 팔로워 목록 커서
- 시작한 시각은 `created_at`이다

## tb_follow_request (대기 팔로우 요청)

비공개 계정에 보낸, 대기 중인 팔로우 요청만 남는다. 수락하면 `tb_follow`로 옮기고, 거절·취소하면 지운다 (research.md R6). `BaseEntity`.

| 컬럼 | 형 | 규칙 |
|---|---|---|
| id | BIGINT PK | 커서 |
| requester_id | BIGINT NOT NULL | 팔로우하려는 회원 |
| target_id | BIGINT NOT NULL | 비공개 계정 회원 |

- `uk_follow_request_requester_id_target_id (requester_id, target_id)`: 한 방향에 대기 요청은 하나
- `idx_follow_request_target_id_id (target_id, id)`: 받은 팔로우 요청 목록 커서
- 요청한 시각은 `created_at`이다

## tb_user_account_visibility (계정 공개 여부)

1:1 테이블. 행이 없으면 `PUBLIC`이다. `BaseTimeEntity`. COMMENT: `'계정 공개 여부 (PK: 1:1 테이블; 감사 열 없음: 회원 본인 설정)'`

| 컬럼 | 형 | 규칙 |
|---|---|---|
| user_id | BIGINT NOT NULL PK | 회원 ID |
| visibility | VARCHAR(20) NOT NULL | `PUBLIC`, `PRIVATE` |

## 상태 변화

친구:

```text
            요청 보내기 (상대가 이미 보냈으면 → 수락)
 관계 없음 ─────────────────────────────▶ 대기 친구 요청 (tb_friend_request)
     ▲                                      │      │       │
     │       취소(보낸 쪽) / 거절(받은 쪽)     │      │       │ 수락(받은 쪽)
     ├──────────────────────────────────────┘      │       ▼
     │        거절이면 tb_friend_rejection 남김       │    친구 (tb_friend 2행
     │                                              │         + 서로 tb_follow
     │                                              │         + 둘 사이 tb_follow_request 지움)
     └──────────────── 친구 끊기 (tb_friend 2행·서로 tb_follow 지움) ◀─┘
```

팔로우 (A가 B를):

```text
                   B가 PUBLIC이거나 친구
 팔로우 안 함 ──────────────────────────────▶ 팔로우 (tb_follow)
     │  ▲                                      ▲    │
     │  │ 거절(B) / 취소(A)                     │    │ 팔로우 취소(A), 친구 끊기
     │  │                                      │    ▼
     │  └──────── 대기 팔로우 요청 ──────────────┘  팔로우 안 함
     │               (tb_follow_request)   수락(B), B가 PUBLIC으로 바꿈, A와 B가 친구가 됨
     └──────────────────▶ (B가 PRIVATE이고 친구 아님)
```

- 팔로우(`tb_follow`)는 친구와 따로 넣고 지울 수 있다. 친구 사이에서 지워도 친구는 그대로다
- `PRIVATE`으로 바꿔도 `tb_follow`는 그대로다. `PUBLIC`으로 바꾸면 그 회원이 받은 `tb_follow_request`가 모두 `tb_follow`가 된다
- 탈퇴 유예: 행은 그대로이고 조인으로 보이지 않는다. 파기: 그 회원이 한쪽인 행을 위 7개 테이블에서 모두 지운다 (research.md R15)

## 다른 테이블과의 관계

- `tb_user`: 닉네임(`name`), 역할(`role`, `ADMIN`이면 쓰지 않음), 탈퇴(`withdrawn_at`)·파기(`purged_at`). 바꾸지 않는다
- `tb_user_profile`(010·009): 태그, 아바타, 공개 항목. 읽기만 한다 (research.md R8)
- `tb_notification.type`: COMMENT에 `FRIEND_REQUESTED`, `FRIEND_ACCEPTED`, `FOLLOW_STARTED`, `FOLLOW_REQUESTED`, `FOLLOW_REQUEST_ACCEPTED`를 더한다 (research.md R10)
- `tb_purge_log.target`: COMMENT에 `SOCIAL_RELATION`을 더한다. 지금 값은 `LOGIN_HISTORY, NOTIFICATION, REFRESH_TOKEN, RICH_TEXT_IMAGE, MEMBER`이고 타입은 `VARCHAR(30) NOT NULL`이다 (research.md R15)
- `tb_menu`: 사용자 메뉴 "친구"(`screen_key` = `FRIENDS`, `MEMBER`) (research.md R12)
