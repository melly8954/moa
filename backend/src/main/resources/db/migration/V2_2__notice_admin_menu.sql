-- moa: 관리 메뉴에 "공지 관리"를 최상위 6번째(역할 관리 다음)로 더한다
-- 스펙 specs/006-notices FR-002, specs/001-admin-menus FR-001
INSERT INTO menus (id, parent_id, name, path, icon, sort_order, enabled, permission,
                   created_at, updated_at, created_by, updated_by)
VALUES (7, NULL, '공지 관리', '/admin/notices', 'clipboard', 50, TRUE, 'NOTICE_MANAGE', NOW(6), NOW(6), 1, 1);
