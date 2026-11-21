-- moa: 소셜 로그인의 외부 계정 연결·약관 동의 기록·프로필(닉네임 태그). 스펙 specs/010-social-login (FR-002·FR-005·FR-013, data-model.md, research.md R14)
-- 재가입 제한 기록(FR-015)은 이번에 만들지 않는다. 009-mypage는 이 파일 다음이다 (tb_user_profile에 칸을 더한다)

CREATE TABLE tb_user_social_account (
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    user_id       BIGINT        NOT NULL COMMENT '회원 ID',
    provider      VARCHAR(20)   NOT NULL COMMENT '소셜 제공자 (GOOGLE, DISCORD)',
    subject       VARCHAR(255)  NOT NULL COMMENT '제공자 계정 식별자 (구글 sub)',
    email         VARCHAR(255)  NOT NULL COMMENT '연결 때 인증된 이메일 (공백 제거·소문자; 바꾸지 않음)',
    display_name  VARCHAR(100)  NULL     COMMENT '연결 때 제공자 이름 (표시에 안 씀)',
    created_at    DATETIME(6)   NOT NULL COMMENT '연결 시각',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_social_account_provider_subject (provider, subject),
    UNIQUE KEY uk_user_social_account_user_id_provider (user_id, provider),
    KEY idx_user_social_account_email (email)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci
    COMMENT = '소셜 계정 연결 (감사 열 없음: 연결 해제는 행 삭제)';

CREATE TABLE tb_user_terms_agreement (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    user_id        BIGINT       NOT NULL COMMENT '회원 ID',
    terms_type     VARCHAR(20)  NOT NULL COMMENT '약관 종류 (SERVICE, PRIVACY)',
    terms_version  VARCHAR(20)  NOT NULL COMMENT '동의한 약관 버전',
    agreed_at      DATETIME(6)  NOT NULL COMMENT '동의 시각',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_terms_agreement_user_id_terms_type_terms_version (user_id, terms_type, terms_version)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci
    COMMENT = '약관 동의 기록 (감사 열 없음: 고치지 않는 기록)';

CREATE TABLE tb_user_profile (
    user_id       BIGINT       NOT NULL COMMENT '회원 ID',
    nickname_key  VARCHAR(50)  NOT NULL COLLATE utf8mb4_bin COMMENT '닉네임 비교 키 (표시 이름의 공백 제거·소문자; utf8mb4_bin: 글자 그대로 비교)',
    nickname_tag  CHAR(4)      NOT NULL COMMENT '닉네임 태그 (0001, ... 9999)',
    created_at    DATETIME(6)  NOT NULL,
    updated_at    DATETIME(6)  NOT NULL,
    PRIMARY KEY (user_id),
    UNIQUE KEY uk_user_profile_nickname_key_nickname_tag (nickname_key, nickname_tag)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci
    COMMENT = '회원 프로필 (PK: 1:1 테이블)';

-- 기존 회원 채우기: 삭제되지 않은 회원(USER)만. 관리자 계정(ADMIN)과 시스템 계정은 프로필을 두지 않는다
-- 태그는 같은 키 안에서 id 순서대로 0001부터
INSERT INTO tb_user_profile (user_id, nickname_key, nickname_tag, created_at, updated_at)
SELECT id,
       LOWER(TRIM(name)) COLLATE utf8mb4_bin,
       LPAD(ROW_NUMBER() OVER (PARTITION BY LOWER(TRIM(name)) COLLATE utf8mb4_bin ORDER BY id), 4, '0'),
       NOW(6), NOW(6)
FROM tb_user
WHERE role = 'USER'
  AND deleted_at IS NULL
  AND login_id <> 'system';
