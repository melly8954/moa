-- moa: 마이페이지 프로필 칸, 플레이 가능 시간대, 게임 정보, 공통코드 PLAY_TIME. 스펙 specs/009-mypage (FR-006~FR-012, data-model.md, research.md R12·R13)
-- 010 마이그레이션(V10__social_login.sql)이 만든 tb_user_profile에 칸을 더한다. 기존 회원 채우기는 010이 했으므로 행을 넣지 않는다 (research R14)

-- 1. 프로필 칸. DEFAULT는 기존 행을 채우려고 NOT NULL 칸 셋에만 둔다 (docs/db.md "열")
ALTER TABLE tb_user_profile
    ADD COLUMN avatar_key          VARCHAR(30)  NOT NULL DEFAULT 'default' COMMENT '기본 아바타 키 (DefaultAvatar)' AFTER nickname_tag,
    ADD COLUMN bio                 VARCHAR(500) NULL     COMMENT '자기소개' AFTER avatar_key,
    ADD COLUMN birth_year          SMALLINT     NULL     COMMENT '출생 연도' AFTER bio,
    ADD COLUMN gender              VARCHAR(20)  NULL     COMMENT '성별 (MALE, FEMALE, UNDISCLOSED)' AFTER birth_year,
    ADD COLUMN age_gender_public   BOOLEAN      NOT NULL DEFAULT FALSE COMMENT '나이·성별 공개 여부' AFTER gender,
    ADD COLUMN discord_tag         VARCHAR(32)  NULL     COMMENT '디스코드 사용자명' AFTER age_gender_public,
    ADD COLUMN discord_visibility  VARCHAR(20)  NOT NULL DEFAULT 'PRIVATE' COMMENT '디스코드 공개 범위 (PRIVATE, FRIENDS, MEMBERS)' AFTER discord_tag,
    ADD COLUMN nickname_changed_at DATETIME(6)  NULL     COMMENT '닉네임 바꾼 시각 (NULL: 가입 뒤 안 바꿈)' AFTER discord_visibility;

-- 2. 플레이 가능 시간대 (UserProfile의 @ElementCollection)
CREATE TABLE tb_user_profile_play_time (
    user_id         BIGINT       NOT NULL COMMENT '회원 ID',
    play_time_code  VARCHAR(50)  NOT NULL COMMENT '플레이 가능 시간대 코드 (PLAY_TIME 그룹)',
    PRIMARY KEY (user_id, play_time_code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci
    COMMENT = '플레이 가능 시간대 (PK: 컬렉션 테이블; 감사 열 없음: 프로필과 함께 저장)';

-- 3. 게임 정보 (UserGameProfile, BaseEntity + @HardDelete). 엔터티는 US5 때 더한다
CREATE TABLE tb_user_game_profile (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    user_id          BIGINT       NOT NULL COMMENT '회원 ID',
    game_code        VARCHAR(50)  NOT NULL COMMENT '게임 코드 (GAME 그룹)',
    tier_code        VARCHAR(50)  NULL     COMMENT '랭크 코드 (<게임>_TIER 그룹; 회원 입력, 확인 안 함)',
    position_code    VARCHAR(50)  NULL     COMMENT '포지션 코드 (<게임>_POSITION 그룹; NULL: 없음)',
    game_account_id  VARCHAR(50)  NULL     COMMENT '게임 아이디',
    created_at       DATETIME(6)  NOT NULL,
    updated_at       DATETIME(6)  NOT NULL,
    created_by       BIGINT       NOT NULL,
    updated_by       BIGINT       NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_game_profile_user_id_game_code (user_id, game_code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci
    COMMENT = '회원 게임 정보';

-- 4. 공통코드 PLAY_TIME. 관리자가 화면에서 고치는 데이터라 없는 행만 넣고 있는 행은 고치지 않는다 (docs/db.md "마이그레이션", 004-admin-codes FR-002)
-- 그룹: 활성 PLAY_TIME 그룹이 없을 때만, 순서는 지금 가장 큰 값 + 1. 감사 열은 시스템 계정(1)
INSERT INTO tb_code_group (group_code, name, description, sort_order, enabled, created_at, updated_at, created_by, updated_by)
SELECT 'PLAY_TIME', '플레이 가능 시간대', '마이페이지 플레이 가능 시간대 선택지 (009-mypage)',
       g.next_order, TRUE, NOW(6), NOW(6), 1, 1
FROM (SELECT COALESCE(MAX(sort_order), 0) + 1 AS next_order FROM tb_code_group WHERE deleted_at IS NULL) g
WHERE NOT EXISTS (SELECT 1 FROM tb_code_group WHERE group_code = 'PLAY_TIME' AND deleted_at IS NULL);

-- 항목: (group_code, code) 활성 행이 없는 것만
INSERT INTO tb_code (group_code, code, name, sort_order, enabled, created_at, updated_at, created_by, updated_by)
SELECT 'PLAY_TIME', v.code, v.name, v.sort_order, TRUE, NOW(6), NOW(6), 1, 1
FROM (SELECT 'WEEKDAY_MORNING' AS code, '평일 오전' AS name, 0 AS sort_order
      UNION ALL SELECT 'WEEKDAY_EVENING', '평일 저녁', 1
      UNION ALL SELECT 'WEEKEND_ALL_DAY', '주말 종일', 2
      UNION ALL SELECT 'DAWN', '새벽', 3) v
WHERE NOT EXISTS (SELECT 1 FROM tb_code c
                  WHERE c.group_code = 'PLAY_TIME' AND c.code = v.code AND c.deleted_at IS NULL);
