-- moa: 코드 그룹을 다시 DB에 두고 관리자가 화면에서 다룬다 (specs/004-admin-codes FR-004·FR-008, research R9).
-- V2_7에서 지운 키트 code_groups(V1_4__codes.sql)에 used_by(쓰는 곳)와 system(기능이 코드에서 읽는 그룹)을 더한다.
-- system은 마이그레이션으로만 켠다. 켜져 있으면 지울 수 없다. moa에는 아직 그룹이 없어 옮길 행이 없다
CREATE TABLE code_groups (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    group_code   VARCHAR(50)   NOT NULL,
    name         VARCHAR(100)  NOT NULL,
    description  VARCHAR(500)  NULL,
    used_by      VARCHAR(200)  NULL,
    `system`     BOOLEAN       NOT NULL DEFAULT FALSE,
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
