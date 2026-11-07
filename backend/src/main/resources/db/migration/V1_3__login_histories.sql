-- 접속 이력 (선택 헬퍼 login-history). 로그인 성공·실패를 모두 남긴다
CREATE TABLE login_histories (
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    user_id       BIGINT        NULL COMMENT '성공하면 사용자 ID. 실패하면 NULL',
    login_id      VARCHAR(50)   NOT NULL COMMENT '입력한 아이디',
    success       BOOLEAN       NOT NULL,
    failure_code  VARCHAR(10)   NULL,
    ip            VARCHAR(45)   NULL,
    user_agent    VARCHAR(500)  NULL,
    device_type   VARCHAR(10)   NOT NULL COMMENT 'PC, MOBILE, TABLET, OTHER',
    os            VARCHAR(30)   NOT NULL,
    browser       VARCHAR(30)   NOT NULL,
    created_at    DATETIME(6)   NOT NULL,
    updated_at    DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    KEY idx_login_histories_user_id_created_at (user_id, created_at),
    KEY idx_login_histories_login_id_created_at (login_id, created_at),
    KEY idx_login_histories_created_at (created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
