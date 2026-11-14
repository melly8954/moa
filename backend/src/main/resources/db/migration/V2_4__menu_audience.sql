-- moa: 메뉴에 대상(관리자·사용자)을 둔다. 사용자 셸 메뉴(홈·공지)도 메뉴 관리에서 다룬다
-- 스펙 specs/001-admin-menus FR-007, research R9. 기존 메뉴는 모두 관리 메뉴(ADMIN)다
ALTER TABLE menus
    ADD COLUMN audience VARCHAR(10) NOT NULL DEFAULT 'ADMIN' COMMENT '메뉴 대상. ADMIN은 관리 셸, USER는 사용자 셸. 하위 메뉴는 부모와 같다' AFTER permission;

-- 사용자 메뉴 처음 구성. 권한이 없으면 로그인하지 않은 사람까지 모두 본다
INSERT INTO menus (parent_id, name, path, icon, sort_order, enabled, permission, audience,
                   created_at, updated_at, created_by, updated_by)
VALUES (NULL, '홈', '/', 'home', 10, TRUE, NULL, 'USER', NOW(6), NOW(6), 1, 1),
       (NULL, '공지', '/notices', 'megaphone', 20, TRUE, NULL, 'USER', NOW(6), NOW(6), 1, 1);
