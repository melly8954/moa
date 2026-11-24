-- moa: 배너·팝업 관리 테이블과 마케팅 메뉴. 스펙 specs/016-banner-popup (FR-001~FR-004)
-- 이번에는 관리 화면만 만든다. 상태(예정·노출 중·끝남·꺼짐)는 칸에 두지 않고 켜짐과 노출 기간으로 읽을 때 정한다 (research R3)
CREATE TABLE tb_banner (
    id                BIGINT        NOT NULL AUTO_INCREMENT,
    name              VARCHAR(100)  NOT NULL COMMENT '배너 이름 (관리용)',
    wide_image_url    VARCHAR(100)  NOT NULL COMMENT '넓은 화면 이미지 주소',
    narrow_image_url  VARCHAR(100)  NULL     COMMENT '좁은 화면 이미지 주소 (NULL: 넓은 화면 이미지)',
    alt_text          VARCHAR(200)  NOT NULL COMMENT '대체 문구',
    link_url          VARCHAR(500)  NULL     COMMENT '연결 주소',
    starts_at         DATETIME(6)   NOT NULL COMMENT '노출 시작 일시',
    ends_at           DATETIME(6)   NULL     COMMENT '노출 끝 일시 (NULL: 끌 때까지)',
    sort_order        INT           NOT NULL COMMENT '순서 (작을수록 먼저)',
    enabled           BOOLEAN       NOT NULL COMMENT '켜짐 여부',
    created_at        DATETIME(6)   NOT NULL,
    updated_at        DATETIME(6)   NOT NULL,
    created_by        BIGINT        NOT NULL,
    updated_by        BIGINT        NOT NULL,
    deleted_at        DATETIME(6)   NULL,
    deleted_by        BIGINT        NULL,
    PRIMARY KEY (id),
    KEY idx_banner_deleted_at_sort_order_starts_at (deleted_at, sort_order, starts_at),
    KEY idx_banner_enabled_starts_at_ends_at (enabled, starts_at, ends_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '홍보 배너';

CREATE TABLE tb_popup (
    id          BIGINT        NOT NULL AUTO_INCREMENT,
    title       VARCHAR(200)  NOT NULL COMMENT '팝업 제목',
    content     MEDIUMTEXT    NOT NULL COMMENT '팝업 내용 (정제된 HTML)',
    link_url    VARCHAR(500)  NULL     COMMENT '연결 주소',
    starts_at   DATETIME(6)   NOT NULL COMMENT '노출 시작 일시',
    ends_at     DATETIME(6)   NULL     COMMENT '노출 끝 일시 (NULL: 끌 때까지)',
    sort_order  INT           NOT NULL COMMENT '순서 (작을수록 먼저)',
    enabled     BOOLEAN       NOT NULL COMMENT '켜짐 여부',
    created_at  DATETIME(6)   NOT NULL,
    updated_at  DATETIME(6)   NOT NULL,
    created_by  BIGINT        NOT NULL,
    updated_by  BIGINT        NOT NULL,
    deleted_at  DATETIME(6)   NULL,
    deleted_by  BIGINT        NULL,
    PRIMARY KEY (id),
    KEY idx_popup_deleted_at_sort_order_starts_at (deleted_at, sort_order, starts_at),
    KEY idx_popup_enabled_starts_at_ends_at (enabled, starts_at, ends_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '팝업';

-- 관리 메뉴: 운영(10)과 설정(20) 사이에 마케팅 상위 메뉴, 그 아래 배너 관리·팝업 관리 (specs/001-admin-menus FR-001)
-- 기존 메뉴 행은 고치지 않는다 (docs/db.md)
INSERT INTO tb_menu (parent_id, name, screen_key, icon, sort_order, enabled, audience, visibility,
                     created_at, updated_at, created_by, updated_by)
VALUES (NULL, '마케팅', NULL, 'megaphone', 15, TRUE, 'ADMIN', NULL, NOW(6), NOW(6), 1, 1);

SET @marketing_id = LAST_INSERT_ID();

INSERT INTO tb_menu (parent_id, name, screen_key, icon, sort_order, enabled, audience, visibility,
                     created_at, updated_at, created_by, updated_by)
VALUES (@marketing_id, '배너 관리', 'ADMIN_BANNERS', 'folder', 10, TRUE, 'ADMIN', NULL, NOW(6), NOW(6), 1, 1),
       (@marketing_id, '팝업 관리', 'ADMIN_POPUPS', 'file', 20, TRUE, 'ADMIN', NULL, NOW(6), NOW(6), 1, 1);
