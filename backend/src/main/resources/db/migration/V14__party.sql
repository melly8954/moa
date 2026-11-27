-- moa: 파티 테이블, 게임 공통코드, 파티 알림 종류. 스펙 specs/008-party (data-model.md, FR-002, FR-029)
-- 공통코드와 메뉴는 관리자가 화면에서 고치는 데이터라 없는 행만 넣고 있는 행은 고치지 않는다 (docs/db.md "마이그레이션")

-- 1. 파티 (소프트 삭제: 해산하면 deleted_at이 찬다)
CREATE TABLE tb_party (
    id                BIGINT         NOT NULL AUTO_INCREMENT,
    game_code         VARCHAR(50)    NOT NULL COMMENT '게임 코드 (GAME 그룹)',
    mode_code         VARCHAR(50)    NULL     COMMENT '게임 모드 코드 (<게임>_MODE 그룹; NULL: 조건 없음)',
    tier_min_code     VARCHAR(50)    NULL     COMMENT '최소 티어 코드 (<게임>_TIER 그룹; NULL: 조건 없음)',
    tier_max_code     VARCHAR(50)    NULL     COMMENT '최대 티어 코드 (<게임>_TIER 그룹; NULL: 조건 없음)',
    title             VARCHAR(50)    NOT NULL COMMENT '제목 (1~50자)',
    description       VARCHAR(1000)  NOT NULL COMMENT '소개 (0~1000자)',
    capacity          TINYINT        NOT NULL COMMENT '모집 인원 (방장 포함 2~10; 포지션 조건이 있으면 자리 합)',
    join_policy       VARCHAR(20)    NOT NULL COMMENT '참가 방식 (OPEN, APPROVAL, PRIVATE)',
    status            VARCHAR(20)    NOT NULL COMMENT '모집 상태 (RECRUITING, CLOSED)',
    closed_by_host    BOOLEAN        NOT NULL COMMENT '방장이 직접 마감했는지',
    host_user_id      BIGINT         NOT NULL COMMENT '지금 방장 회원 ID',
    position_enabled  BOOLEAN        NOT NULL COMMENT '포지션 조건을 걸었는지',
    invite_code       CHAR(8)        NOT NULL COMMENT '초대 코드 (대문자·숫자 8자, 0 O 1 I 제외)',
    created_at        DATETIME(6)    NOT NULL,
    updated_at        DATETIME(6)    NOT NULL,
    created_by        BIGINT         NOT NULL,
    updated_by        BIGINT         NOT NULL,
    deleted_at        DATETIME(6)    NULL,
    deleted_by        BIGINT         NULL,
    active_key        TINYINT AS (IF(deleted_at IS NULL, 1, NULL)) VIRTUAL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_party_invite_code_active_key (invite_code, active_key),
    KEY idx_party_recruit_list (status, join_policy, game_code, mode_code, id)
        COMMENT '모집 페이지 목록 (status, join_policy, game_code, mode_code, id)',
    KEY idx_party_host_user_id (host_user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '파티';

-- 2. 포지션 자리 (하드 삭제)
CREATE TABLE tb_party_position (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    party_id       BIGINT       NOT NULL COMMENT '파티 ID',
    position_code  VARCHAR(50)  NOT NULL COMMENT '포지션 코드 (<게임>_POSITION 그룹)',
    slots          TINYINT      NOT NULL COMMENT '자리 수 (1 이상; 롤은 1)',
    created_at     DATETIME(6)  NOT NULL,
    updated_at     DATETIME(6)  NOT NULL,
    created_by     BIGINT       NOT NULL,
    updated_by     BIGINT       NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_party_position_party_id_position_code (party_id, position_code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci
    COMMENT = '파티 포지션 자리 (하드 삭제: 파티 설정의 일부라 이력이 필요 없음)';

-- 3. 파티 멤버 (하드 삭제)
CREATE TABLE tb_party_member (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    party_id       BIGINT       NOT NULL COMMENT '파티 ID',
    user_id        BIGINT       NOT NULL COMMENT '회원 ID',
    role           VARCHAR(20)  NOT NULL COMMENT '역할 (HOST, MEMBER)',
    position_code  VARCHAR(50)  NULL     COMMENT '포지션 코드 (NULL: 포지션 조건 없음)',
    tier_code      VARCHAR(50)  NULL     COMMENT '본인이 적은 티어 코드 (NULL: 안 적음)',
    joined_at      DATETIME(6)  NOT NULL COMMENT '들어온 시각 (방장을 잇는 순서)',
    created_at     DATETIME(6)  NOT NULL,
    updated_at     DATETIME(6)  NOT NULL,
    created_by     BIGINT       NOT NULL,
    updated_by     BIGINT       NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_party_member_user_id (user_id),
    KEY idx_party_member_party_id_joined_at (party_id, joined_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci
    COMMENT = '파티 멤버 (하드 삭제: 나가면 지우고 이력은 채팅 안내로 남음)';

-- 4. 참가 신청 (하드 삭제)
CREATE TABLE tb_party_join_request (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    party_id       BIGINT       NOT NULL COMMENT '파티 ID',
    user_id        BIGINT       NOT NULL COMMENT '신청한 회원 ID',
    position_code  VARCHAR(50)  NULL     COMMENT '신청할 때 고른 포지션 코드',
    tier_code      VARCHAR(50)  NULL     COMMENT '신청할 때 적은 티어 코드',
    status         VARCHAR(20)  NOT NULL COMMENT '신청 상태 (PENDING, APPROVED, REJECTED, CANCELLED)',
    decided_at     DATETIME(6)  NULL     COMMENT '승인·거절·취소한 시각 (NULL: 대기 중)',
    created_at     DATETIME(6)  NOT NULL,
    updated_at     DATETIME(6)  NOT NULL,
    created_by     BIGINT       NOT NULL,
    updated_by     BIGINT       NOT NULL,
    pending_key    BIGINT AS (IF(status = 'PENDING', 0, id)) VIRTUAL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_party_join_request_party_id_user_id_pending_key (party_id, user_id, pending_key),
    KEY idx_party_join_request_user_id (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci
    COMMENT = '파티 참가 신청 (하드 삭제: 처리된 신청은 상태로 남기고 해산하면 지움)';

-- 5. 참가 제한 (하드 삭제)
CREATE TABLE tb_party_ban (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    party_id    BIGINT       NOT NULL COMMENT '파티 ID',
    user_id     BIGINT       NOT NULL COMMENT '제한된 회원 ID',
    reason      VARCHAR(20)  NOT NULL COMMENT '제한 이유 (KICKED, REJECTED)',
    created_at  DATETIME(6)  NOT NULL,
    updated_at  DATETIME(6)  NOT NULL,
    created_by  BIGINT       NOT NULL,
    updated_by  BIGINT       NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_party_ban_party_id_user_id (party_id, user_id),
    KEY idx_party_ban_user_id (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci
    COMMENT = '파티 참가 제한 (하드 삭제: 해산돼도 남김)';

-- 6. 파티 메시지 (하드 삭제)
CREATE TABLE tb_party_message (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    party_id         BIGINT       NOT NULL COMMENT '파티 ID',
    type             VARCHAR(20)  NOT NULL COMMENT '메시지 종류 (CHAT, SYSTEM)',
    sender_user_id   BIGINT       NULL     COMMENT '보낸 회원 ID (NULL: SYSTEM)',
    content          VARCHAR(500) NULL     COMMENT '내용 (CHAT이면 1~500자; NULL: SYSTEM)',
    system_event     VARCHAR(30)  NULL     COMMENT '안내 종류 (JOINED, LEFT, KICKED, HOST_CHANGED; NULL: CHAT)',
    subject_user_id  BIGINT       NULL     COMMENT '안내 대상 회원 ID (NULL: CHAT)',
    created_at       DATETIME(6)  NOT NULL,
    updated_at       DATETIME(6)  NOT NULL,
    created_by       BIGINT       NOT NULL,
    updated_by       BIGINT       NOT NULL,
    PRIMARY KEY (id),
    KEY idx_party_message_party_id_id (party_id, id),
    KEY idx_party_message_sender_user_id (sender_user_id),
    KEY idx_party_message_subject_user_id (subject_user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci
    COMMENT = '파티 메시지 (하드 삭제: 지우는 기능 없음, 해산돼도 남김)';

-- 7. 공통코드 그룹 12개 (spec.md FR-002). 활성 그룹이 없을 때만, 순서는 지금 가장 큰 값 뒤로. 감사 열은 시스템 계정(1)
INSERT INTO tb_code_group (group_code, name, description, sort_order, enabled, created_at, updated_at, created_by, updated_by)
SELECT v.group_code, v.name, v.description, g.next_order + v.idx, TRUE, NOW(6), NOW(6), 1, 1
FROM (SELECT 'GAME' AS group_code, '게임' AS name, '파티·게임 정보의 게임 (008-party)' AS description, 0 AS idx
      UNION ALL SELECT 'LOL_MODE', '리그 오브 레전드 모드', '파티 게임 모드 (008-party)', 1
      UNION ALL SELECT 'LOL_POSITION', '리그 오브 레전드 포지션', '파티 포지션 (008-party)', 2
      UNION ALL SELECT 'LOL_TIER', '리그 오브 레전드 티어', '파티·게임 정보의 티어, 낮은 것부터 (008-party)', 3
      UNION ALL SELECT 'OVERWATCH_MODE', '오버워치 모드', '파티 게임 모드 (008-party)', 4
      UNION ALL SELECT 'OVERWATCH_POSITION', '오버워치 포지션', '파티 포지션 (008-party)', 5
      UNION ALL SELECT 'OVERWATCH_TIER', '오버워치 티어', '파티·게임 정보의 티어, 낮은 것부터 (008-party)', 6
      UNION ALL SELECT 'VALORANT_MODE', '발로란트 모드', '파티 게임 모드 (008-party)', 7
      UNION ALL SELECT 'VALORANT_POSITION', '발로란트 포지션', '파티 포지션 (008-party)', 8
      UNION ALL SELECT 'VALORANT_TIER', '발로란트 티어', '파티·게임 정보의 티어, 낮은 것부터 (008-party)', 9
      UNION ALL SELECT 'PUBG_MODE', '배틀그라운드 모드', '파티 게임 모드 (008-party)', 10
      UNION ALL SELECT 'PUBG_TIER', '배틀그라운드 티어', '파티·게임 정보의 티어, 낮은 것부터 (008-party)', 11) v
CROSS JOIN (SELECT COALESCE(MAX(sort_order), 0) + 1 AS next_order FROM tb_code_group WHERE deleted_at IS NULL) g
WHERE NOT EXISTS (SELECT 1 FROM tb_code_group cg WHERE cg.group_code = v.group_code AND cg.deleted_at IS NULL);

-- 8. 공통코드 항목. (group_code, code) 활성 행이 없는 것만. 순서는 표의 순서(티어는 낮은 것부터) 10, 20, 30…
INSERT INTO tb_code (group_code, code, name, sort_order, enabled, created_at, updated_at, created_by, updated_by)
SELECT v.group_code, v.code, v.name, v.sort_order, TRUE, NOW(6), NOW(6), 1, 1
FROM (SELECT 'GAME' AS group_code, 'LOL' AS code, '리그 오브 레전드' AS name, 10 AS sort_order
      UNION ALL SELECT 'GAME', 'OVERWATCH', '오버워치', 20
      UNION ALL SELECT 'GAME', 'VALORANT', '발로란트', 30
      UNION ALL SELECT 'GAME', 'PUBG', '배틀그라운드', 40
      UNION ALL SELECT 'LOL_MODE', 'SOLO_RANK', '솔로랭크', 10
      UNION ALL SELECT 'LOL_MODE', 'FLEX_RANK', '자유랭크', 20
      UNION ALL SELECT 'LOL_MODE', 'NORMAL', '일반', 30
      UNION ALL SELECT 'LOL_MODE', 'ARAM', '칼바람 나락', 40
      UNION ALL SELECT 'LOL_POSITION', 'TOP', '탑', 10
      UNION ALL SELECT 'LOL_POSITION', 'JUNGLE', '정글', 20
      UNION ALL SELECT 'LOL_POSITION', 'MID', '미드', 30
      UNION ALL SELECT 'LOL_POSITION', 'ADC', '원딜', 40
      UNION ALL SELECT 'LOL_POSITION', 'SUPPORT', '서포터', 50
      UNION ALL SELECT 'LOL_TIER', 'IRON', '아이언', 10
      UNION ALL SELECT 'LOL_TIER', 'BRONZE', '브론즈', 20
      UNION ALL SELECT 'LOL_TIER', 'SILVER', '실버', 30
      UNION ALL SELECT 'LOL_TIER', 'GOLD', '골드', 40
      UNION ALL SELECT 'LOL_TIER', 'PLATINUM', '플래티넘', 50
      UNION ALL SELECT 'LOL_TIER', 'EMERALD', '에메랄드', 60
      UNION ALL SELECT 'LOL_TIER', 'DIAMOND', '다이아몬드', 70
      UNION ALL SELECT 'LOL_TIER', 'MASTER', '마스터', 80
      UNION ALL SELECT 'LOL_TIER', 'GRANDMASTER', '그랜드마스터', 90
      UNION ALL SELECT 'LOL_TIER', 'CHALLENGER', '챌린저', 100
      UNION ALL SELECT 'OVERWATCH_MODE', 'COMPETITIVE', '경쟁전', 10
      UNION ALL SELECT 'OVERWATCH_MODE', 'QUICK_PLAY', '빠른 대전', 20
      UNION ALL SELECT 'OVERWATCH_MODE', 'ARCADE', '아케이드', 30
      UNION ALL SELECT 'OVERWATCH_POSITION', 'TANK', '돌격', 10
      UNION ALL SELECT 'OVERWATCH_POSITION', 'DAMAGE', '공격', 20
      UNION ALL SELECT 'OVERWATCH_POSITION', 'SUPPORT', '지원', 30
      UNION ALL SELECT 'OVERWATCH_TIER', 'BRONZE', '브론즈', 10
      UNION ALL SELECT 'OVERWATCH_TIER', 'SILVER', '실버', 20
      UNION ALL SELECT 'OVERWATCH_TIER', 'GOLD', '골드', 30
      UNION ALL SELECT 'OVERWATCH_TIER', 'PLATINUM', '플래티넘', 40
      UNION ALL SELECT 'OVERWATCH_TIER', 'DIAMOND', '다이아몬드', 50
      UNION ALL SELECT 'OVERWATCH_TIER', 'MASTER', '마스터', 60
      UNION ALL SELECT 'OVERWATCH_TIER', 'GRANDMASTER', '그랜드마스터', 70
      UNION ALL SELECT 'OVERWATCH_TIER', 'CHAMPION', '챔피언', 80
      UNION ALL SELECT 'OVERWATCH_TIER', 'TOP500', '500위', 90
      UNION ALL SELECT 'VALORANT_MODE', 'COMPETITIVE', '경쟁전', 10
      UNION ALL SELECT 'VALORANT_MODE', 'UNRATED', '일반전', 20
      UNION ALL SELECT 'VALORANT_MODE', 'SWIFTPLAY', '신속플레이', 30
      UNION ALL SELECT 'VALORANT_MODE', 'SPIKE_RUSH', '스파이크 돌격', 40
      UNION ALL SELECT 'VALORANT_POSITION', 'DUELIST', '타격대', 10
      UNION ALL SELECT 'VALORANT_POSITION', 'INITIATOR', '척후대', 20
      UNION ALL SELECT 'VALORANT_POSITION', 'CONTROLLER', '전략가', 30
      UNION ALL SELECT 'VALORANT_POSITION', 'SENTINEL', '감시자', 40
      UNION ALL SELECT 'VALORANT_TIER', 'IRON', '아이언', 10
      UNION ALL SELECT 'VALORANT_TIER', 'BRONZE', '브론즈', 20
      UNION ALL SELECT 'VALORANT_TIER', 'SILVER', '실버', 30
      UNION ALL SELECT 'VALORANT_TIER', 'GOLD', '골드', 40
      UNION ALL SELECT 'VALORANT_TIER', 'PLATINUM', '플래티넘', 50
      UNION ALL SELECT 'VALORANT_TIER', 'DIAMOND', '다이아몬드', 60
      UNION ALL SELECT 'VALORANT_TIER', 'ASCENDANT', '초월자', 70
      UNION ALL SELECT 'VALORANT_TIER', 'IMMORTAL', '불멸', 80
      UNION ALL SELECT 'VALORANT_TIER', 'RADIANT', '레디언트', 90
      UNION ALL SELECT 'PUBG_MODE', 'RANKED', '경쟁전', 10
      UNION ALL SELECT 'PUBG_MODE', 'NORMAL', '일반전', 20
      UNION ALL SELECT 'PUBG_TIER', 'BRONZE', '브론즈', 10
      UNION ALL SELECT 'PUBG_TIER', 'SILVER', '실버', 20
      UNION ALL SELECT 'PUBG_TIER', 'GOLD', '골드', 30
      UNION ALL SELECT 'PUBG_TIER', 'PLATINUM', '플래티넘', 40
      UNION ALL SELECT 'PUBG_TIER', 'DIAMOND', '다이아몬드', 50
      UNION ALL SELECT 'PUBG_TIER', 'MASTER', '마스터', 60) v
WHERE NOT EXISTS (SELECT 1 FROM tb_code c
                  WHERE c.group_code = v.group_code AND c.code = v.code AND c.deleted_at IS NULL);

-- 9. 사용자 메뉴 "모집"(누구나)과 "내 파티"(회원만) (spec.md FR-030, 001 FR-007). 지금 사용자 메뉴 뒤에 screen_key로 없을 때만
INSERT INTO tb_menu (parent_id, name, screen_key, icon, sort_order, enabled, audience, visibility,
                     created_at, updated_at, created_by, updated_by)
SELECT NULL, v.name, v.screen_key, v.icon, m.max_order + v.step, TRUE, 'USER', v.visibility, NOW(6), NOW(6), 1, 1
FROM (SELECT '모집' AS name, 'RECRUIT' AS screen_key, 'gamepad' AS icon, 'PUBLIC' AS visibility, 10 AS step
      UNION ALL SELECT '내 파티', 'MY_PARTY', 'users', 'MEMBER', 20) v
CROSS JOIN (SELECT COALESCE(MAX(sort_order), 0) AS max_order
            FROM tb_menu WHERE audience = 'USER' AND parent_id IS NULL AND deleted_at IS NULL) m
WHERE NOT EXISTS (SELECT 1 FROM tb_menu t WHERE t.screen_key = v.screen_key AND t.deleted_at IS NULL);

-- 10. 알림 종류 COMMENT에 파티 알림 6개 (spec.md FR-029). 타입은 그대로 옮겨 적는다
ALTER TABLE tb_notification
    MODIFY COLUMN type VARCHAR(30) NOT NULL COMMENT '알림 종류 (SYSTEM, PARTY_APPLIED, PARTY_APPROVED, PARTY_REJECTED, PARTY_HOST_CHANGED, PARTY_KICKED, PARTY_DISSOLVED)';
