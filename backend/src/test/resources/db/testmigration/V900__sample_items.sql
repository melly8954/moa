-- 골격 검증용 테이블. 테스트에서만 쓴다
CREATE TABLE sample_items (
    id          BIGINT        NOT NULL AUTO_INCREMENT,
    name        VARCHAR(100)  NOT NULL,
    created_at  DATETIME(6)   NOT NULL,
    updated_at  DATETIME(6)   NOT NULL,
    created_by  BIGINT        NOT NULL,
    updated_by  BIGINT        NOT NULL,
    deleted_at  DATETIME(6)   NULL,
    deleted_by  BIGINT        NULL,
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
