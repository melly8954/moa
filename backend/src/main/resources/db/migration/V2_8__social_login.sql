-- moa: 소셜 로그인. 스펙 specs/010-social-login (data-model.md, research.md R14)
-- 번호는 병합 직전에 dev 마지막 다음으로 다시 정함 (004 Phase 6도 V2_8 계획). 나중에 병합하는 쪽이 옮긴다
-- 재가입 제한 기록(social_rejoin_blocks, FR-015)은 이번에 만들지 않는다
-- 009-mypage는 이 파일 다음이다 (user_profiles에 칸을 더한다)

-- 외부 계정 연결. 회원 하나에 제공자마다 하나. 같은 소셜 계정은 한 회원에만
CREATE TABLE user_social_accounts (
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    user_id       BIGINT        NOT NULL,
    provider      VARCHAR(20)   NOT NULL COMMENT 'SocialProvider (GOOGLE, DISCORD)',
    subject       VARCHAR(255)  NOT NULL COMMENT '제공자의 바뀌지 않는 계정 식별자 (구글 sub)',
    email         VARCHAR(255)  NOT NULL COMMENT '연결 때 제공자가 인증한 이메일. 공백 제거·소문자. 나중에 고치지 않는다',
    display_name  VARCHAR(100)  NULL     COMMENT '연결 때 제공자 이름. 표시 이름으로 쓰지 않는다',
    created_at    DATETIME(6)   NOT NULL COMMENT '연결 시각',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_social_accounts_provider_subject (provider, subject),
    UNIQUE KEY uk_user_social_accounts_user_provider (user_id, provider),
    KEY idx_user_social_accounts_email (email)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- 약관 동의 기록. 가입 트랜잭션에서 종류마다 한 행
CREATE TABLE user_terms_agreements (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    user_id        BIGINT       NOT NULL,
    terms_type     VARCHAR(20)  NOT NULL COMMENT 'TermsType (SERVICE, PRIVACY)',
    terms_version  VARCHAR(20)  NOT NULL,
    agreed_at      DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_terms_agreements_user_type_version (user_id, terms_type, terms_version)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- 프로필(최소형): 닉네임 구별용 키·태그. 닉네임은 겹쳐도 되고 닉네임#태그가 고유하다
-- 009-mypage가 나머지 프로필 칸을 더한다. 규칙 정본은 specs/009-mypage research R4
CREATE TABLE user_profiles (
    user_id       BIGINT       NOT NULL,
    nickname_key  VARCHAR(50)  NOT NULL COLLATE utf8mb4_bin COMMENT 'users.name 앞뒤 공백 제거 + 소문자 (앱이 정규화)',
    nickname_tag  CHAR(4)      NOT NULL COMMENT '0001~9999. 서버가 정한다',
    created_at    DATETIME(6)  NOT NULL,
    updated_at    DATETIME(6)  NOT NULL,
    PRIMARY KEY (user_id),
    UNIQUE KEY uk_user_profiles_nickname_tag (nickname_key, nickname_tag)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- 기존 회원 채우기: 삭제되지 않은 모든 회원. 태그는 같은 키 안에서 id 순서대로 0001부터
INSERT INTO user_profiles (user_id, nickname_key, nickname_tag, created_at, updated_at)
SELECT id,
       LOWER(TRIM(name)) COLLATE utf8mb4_bin,
       LPAD(ROW_NUMBER() OVER (PARTITION BY LOWER(TRIM(name)) COLLATE utf8mb4_bin ORDER BY id), 4, '0'),
       NOW(6), NOW(6)
FROM users
WHERE deleted_at IS NULL;
