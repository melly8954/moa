-- 메뉴 권한 (선택 헬퍼 menu). 메뉴(화면) 트리와 메뉴를 볼 때 필요한 권한. 깊이는 2단계(그룹 → 메뉴)까지
CREATE TABLE menus (
    id          BIGINT        NOT NULL AUTO_INCREMENT,
    parent_id   BIGINT        NULL COMMENT 'NULL이면 1단계',
    name        VARCHAR(50)   NOT NULL,
    path        VARCHAR(200)  NULL COMMENT '화면 경로 (/로 시작). 그룹은 비울 수 있다',
    icon        VARCHAR(50)   NULL COMMENT '아이콘 이름. 해석은 프론트가 한다',
    sort_order  INT           NOT NULL,
    enabled     BOOLEAN       NOT NULL,
    permission  VARCHAR(100)  NULL COMMENT '볼 때 필요한 권한 키 (예: MENU_MANAGE). NULL이면 로그인한 사용자 모두',
    created_at  DATETIME(6)   NOT NULL,
    updated_at  DATETIME(6)   NOT NULL,
    created_by  BIGINT        NOT NULL,
    updated_by  BIGINT        NOT NULL,
    deleted_at  DATETIME(6)   NULL,
    deleted_by  BIGINT        NULL,
    PRIMARY KEY (id),
    KEY idx_menus_parent_id (parent_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- 시드: 관리 > 메뉴 관리. admin 프리셋은 같은 그룹(id 1)에 코드 관리·접속 이력·역할 관리를 더한다 (V1_7_1)
-- 그룹은 권한을 두지 않는다. 볼 수 있는 하위 메뉴가 없으면 내 메뉴에서 빠진다
INSERT INTO menus (id, parent_id, name, path, icon, sort_order, enabled, permission,
                   created_at, updated_at, created_by, updated_by)
VALUES (1, NULL, '관리', NULL, 'settings', 900, TRUE, NULL, NOW(6), NOW(6), 1, 1),
       (2, 1, '메뉴 관리', '/admin/menus', 'menu', 10, TRUE, 'MENU_MANAGE', NOW(6), NOW(6), 1, 1);
