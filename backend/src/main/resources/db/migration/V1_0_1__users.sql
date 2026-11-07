-- 사용자 (골격 회원·로그인). 버전 V1_0_x는 골격, V1_1부터는 헬퍼가 쓴다
CREATE TABLE users (
    id          BIGINT        NOT NULL AUTO_INCREMENT,
    login_id    VARCHAR(50)   NOT NULL,
    password    VARCHAR(100)  NULL COMMENT '{bcrypt}... 형식. NULL이면 비밀번호로 로그인할 수 없다 (시스템 계정)',
    name        VARCHAR(50)   NOT NULL,
    role        VARCHAR(20)   NOT NULL COMMENT 'USER, ADMIN',
    created_at  DATETIME(6)   NOT NULL,
    updated_at  DATETIME(6)   NOT NULL,
    created_by  BIGINT        NOT NULL,
    updated_by  BIGINT        NOT NULL,
    deleted_at  DATETIME(6)   NULL,
    deleted_by  BIGINT        NULL,
    -- 활성 행에만 값이 있어 삭제된 계정과 같은 아이디로 다시 가입할 수 있다
    active_key  TINYINT AS (IF(deleted_at IS NULL, 1, NULL)) VIRTUAL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_login_id_active (login_id, active_key)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- 시스템 계정. 로그인 전 요청과 배치 작업의 감사 행위자다 (app.audit.system-actor-id 기본값 1)
INSERT INTO users (id, login_id, password, name, role, created_at, updated_at, created_by, updated_by)
VALUES (1, 'system', NULL, '시스템', 'USER', NOW(6), NOW(6), 1, 1);
