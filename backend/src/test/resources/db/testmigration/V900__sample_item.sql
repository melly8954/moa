-- 골격 검증용 테이블. 테스트에서만 쓴다 (docs/db.md "마이그레이션": 테스트 전용은 V900부터)
CREATE TABLE tb_sample_item (
    id          BIGINT        NOT NULL AUTO_INCREMENT,
    name        VARCHAR(100)  NOT NULL COMMENT '시험 항목 이름',
    created_at  DATETIME(6)   NOT NULL,
    updated_at  DATETIME(6)   NOT NULL,
    created_by  BIGINT        NOT NULL,
    updated_by  BIGINT        NOT NULL,
    deleted_at  DATETIME(6)   NULL,
    deleted_by  BIGINT        NULL,
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '감사 검증용 시험 항목';
