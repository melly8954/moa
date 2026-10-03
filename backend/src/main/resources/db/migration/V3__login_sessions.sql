-- 로그인 세션 (docs/design/security.md 로그인 세션으로 즉시 끊기)
-- 로그인할 때마다 하나 만든다. 액세스 토큰의 sid 클레임이 이 행의 id다.
-- 리프레시 토큰은 세션에 속하고, 재발급해도 세션은 같다.
CREATE TABLE login_sessions (
    id            BIGINT      NOT NULL AUTO_INCREMENT,
    member_id     BIGINT      NOT NULL,
    revoked_at    DATETIME(6) NULL,
    revoke_reason VARCHAR(30) NULL,
    created_at    DATETIME(6) NOT NULL,
    updated_at    DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_login_sessions_member_id (member_id),
    CONSTRAINT fk_login_sessions_members FOREIGN KEY (member_id) REFERENCES members (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- 골격 토큰(세션 없이 발급한 토큰)과 함께 쓰도록 NULL을 허용한다
ALTER TABLE refresh_tokens
    ADD COLUMN login_session_id BIGINT NULL AFTER family_id,
    ADD KEY idx_refresh_tokens_login_session_id (login_session_id);
