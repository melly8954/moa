-- 공통코드 (선택 헬퍼 code). 관리 화면에서 바꾸는 값(분류, 상태명 등)을 코드 그룹과 코드로 둔다
CREATE TABLE code_groups (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    group_code   VARCHAR(50)   NOT NULL,
    name         VARCHAR(100)  NOT NULL,
    description  VARCHAR(500)  NULL,
    created_at   DATETIME(6)   NOT NULL,
    updated_at   DATETIME(6)   NOT NULL,
    created_by   BIGINT        NOT NULL,
    updated_by   BIGINT        NOT NULL,
    deleted_at   DATETIME(6)   NULL,
    deleted_by   BIGINT        NULL,
    active_key   TINYINT AS (IF(deleted_at IS NULL, 1, NULL)) VIRTUAL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_code_groups_group_code_active (group_code, active_key)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE codes (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    group_code   VARCHAR(50)   NOT NULL,
    code         VARCHAR(50)   NOT NULL,
    name         VARCHAR(100)  NOT NULL,
    sort_order   INT           NOT NULL,
    enabled      BOOLEAN       NOT NULL,
    created_at   DATETIME(6)   NOT NULL,
    updated_at   DATETIME(6)   NOT NULL,
    created_by   BIGINT        NOT NULL,
    updated_by   BIGINT        NOT NULL,
    deleted_at   DATETIME(6)   NULL,
    deleted_by   BIGINT        NULL,
    active_key   TINYINT AS (IF(deleted_at IS NULL, 1, NULL)) VIRTUAL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_codes_group_code_code_active (group_code, code, active_key),
    KEY idx_codes_group_code_sort (group_code, sort_order)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
