-- 회원, 로그인 수단 연결, 가입 진행과 가입 인증 (회원가입)

-- 회원. 개인정보 컬럼은 파기(PURGED) 때 비우므로 NULL을 허용한다.
-- phone_hmac은 파기 뒤에도 재가입 제한 기간 동안 남는다.
CREATE TABLE members (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    email             VARCHAR(255) NULL,
    password_hash     VARCHAR(100) NULL,
    nickname          VARCHAR(12)  NULL,
    birth_date        DATE         NULL,
    phone_encrypted   VARCHAR(255) NULL,
    phone_hmac        VARCHAR(64)  NULL,
    role              VARCHAR(30)  NOT NULL,
    status            VARCHAR(30)  NOT NULL,
    terms_agreed_at   DATETIME(6)  NULL,
    privacy_agreed_at DATETIME(6)  NULL,
    created_at        DATETIME(6)  NOT NULL,
    created_by        BIGINT       NOT NULL,
    updated_at        DATETIME(6)  NOT NULL,
    updated_by        BIGINT       NOT NULL,
    deleted_at        DATETIME(6)  NULL,
    deleted_by        BIGINT       NULL,
    active_key        TINYINT AS (IF(deleted_at IS NULL, 1, NULL)) VIRTUAL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_members_email (email, active_key),
    UNIQUE KEY uk_members_nickname (nickname, active_key),
    UNIQUE KEY uk_members_phone_hmac (phone_hmac, active_key)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- 회원에 연결된 구글·카카오 계정. 한 제공자 계정은 회원 하나에만, 회원 하나에 제공자별로 하나만 연결된다.
CREATE TABLE member_social_accounts (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    member_id        BIGINT       NOT NULL,
    provider         VARCHAR(30)  NOT NULL,
    provider_user_id VARCHAR(255) NOT NULL,
    created_at       DATETIME(6)  NOT NULL,
    created_by       BIGINT       NOT NULL,
    updated_at       DATETIME(6)  NOT NULL,
    updated_by       BIGINT       NOT NULL,
    deleted_at       DATETIME(6)  NULL,
    deleted_by       BIGINT       NULL,
    active_key       TINYINT AS (IF(deleted_at IS NULL, 1, NULL)) VIRTUAL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_member_social_accounts_provider_user_id (provider, provider_user_id, active_key),
    UNIQUE KEY uk_member_social_accounts_member_id_provider (member_id, provider, active_key),
    CONSTRAINT fk_member_social_accounts_members FOREIGN KEY (member_id) REFERENCES members (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- 가입 진행. 회원이 생기기 전 단계의 확인된 값을 둔다. 브라우저는 토큰 원문을 쿠키로 가진다.
-- email이 있으면 계정 이메일이 정해진 것이다 (이메일 인증 완료 또는 제공자가 준 이메일).
CREATE TABLE sign_ups (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    token_hash        VARCHAR(64)  NOT NULL,
    email             VARCHAR(255) NULL,
    password_hash     VARCHAR(100) NULL,
    provider          VARCHAR(30)  NULL,
    provider_user_id  VARCHAR(255) NULL,
    birth_date        DATE         NULL,
    phone_encrypted   VARCHAR(255) NULL,
    phone_hmac        VARCHAR(64)  NULL,
    phone_verified_at DATETIME(6)  NULL,
    expires_at        DATETIME(6)  NOT NULL,
    created_at        DATETIME(6)  NOT NULL,
    updated_at        DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sign_ups_token_hash (token_hash),
    KEY idx_sign_ups_expires_at (expires_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- 이메일 인증 링크. 링크 토큰은 해시만 둔다. sign_up_id는 카카오가 이메일을 주지 않아 이어 온 가입일 때만 있다.
CREATE TABLE email_verifications (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    sign_up_id    BIGINT       NULL,
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    token_hash    VARCHAR(64)  NOT NULL,
    expires_at    DATETIME(6)  NOT NULL,
    used_at       DATETIME(6)  NULL,
    created_at    DATETIME(6)  NOT NULL,
    updated_at    DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_email_verifications_token_hash (token_hash),
    KEY idx_email_verifications_email_created_at (email, created_at),
    KEY idx_email_verifications_sign_up_id (sign_up_id),
    KEY idx_email_verifications_expires_at (expires_at),
    CONSTRAINT fk_email_verifications_sign_ups FOREIGN KEY (sign_up_id) REFERENCES sign_ups (id)
        ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- 휴대폰 인증 번호. 번호는 해시만 둔다. 하루 발송 횟수를 세야 하므로 가입 진행이 지워져도 남긴다.
CREATE TABLE phone_verifications (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    sign_up_id      BIGINT       NULL,
    phone_encrypted VARCHAR(255) NOT NULL,
    phone_hmac      VARCHAR(64)  NOT NULL,
    code_hash       VARCHAR(64)  NOT NULL,
    expires_at      DATETIME(6)  NOT NULL,
    failed_attempts INT          NOT NULL DEFAULT 0,
    verified_at     DATETIME(6)  NULL,
    created_at      DATETIME(6)  NOT NULL,
    updated_at      DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    KEY idx_phone_verifications_sign_up_id (sign_up_id),
    KEY idx_phone_verifications_phone_hmac_created_at (phone_hmac, created_at),
    KEY idx_phone_verifications_expires_at (expires_at),
    CONSTRAINT fk_phone_verifications_sign_ups FOREIGN KEY (sign_up_id) REFERENCES sign_ups (id)
        ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
