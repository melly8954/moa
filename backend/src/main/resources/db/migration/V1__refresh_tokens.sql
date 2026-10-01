-- 리프레시 토큰 (문서 조각: docs-fragments/auth-token)
CREATE TABLE refresh_tokens (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    user_id     BIGINT       NOT NULL,
    family_id   VARCHAR(36)  NOT NULL,
    token_hash  VARCHAR(64)  NOT NULL,
    expires_at  DATETIME(6)  NOT NULL,
    used_at     DATETIME(6)  NULL,
    revoked_at  DATETIME(6)  NULL,
    created_at  DATETIME(6)  NOT NULL,
    updated_at  DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_refresh_tokens_token_hash (token_hash),
    KEY idx_refresh_tokens_user_id (user_id),
    KEY idx_refresh_tokens_family_id (family_id),
    KEY idx_refresh_tokens_expires_at (expires_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
