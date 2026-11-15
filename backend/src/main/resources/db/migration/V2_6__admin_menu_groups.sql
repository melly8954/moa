-- moa: 관리 메뉴를 "운영"·"설정" 묶음으로 나눈다 (specs/001-admin-menus FR-001, research R18)
-- 처음 구성의 최상위 관리 메뉴만 program_key로 찾아 옮긴다. 관리자가 이미 옮긴 메뉴(하위 메뉴)는 건드리지 않는다
INSERT INTO menus (parent_id, name, program_key, icon, sort_order, enabled, audience, visibility,
                   created_at, updated_at, created_by, updated_by)
VALUES (NULL, '운영', NULL, 'briefcase', 10, TRUE, 'ADMIN', NULL, NOW(6), NOW(6), 1, 1),
       (NULL, '설정', NULL, 'settings', 20, TRUE, 'ADMIN', NULL, NOW(6), NOW(6), 1, 1);

UPDATE menus m
    JOIN (SELECT id FROM menus WHERE name = '운영' AND audience = 'ADMIN' AND parent_id IS NULL
                                 AND program_key IS NULL AND deleted_at IS NULL ORDER BY id DESC LIMIT 1) g
SET m.parent_id  = g.id,
    m.sort_order = CASE m.program_key
                       WHEN 'ADMIN_USERS' THEN 10
                       WHEN 'ADMIN_NOTICES' THEN 20
                       WHEN 'ADMIN_LOGIN_HISTORIES' THEN 30 END,
    m.updated_at = NOW(6),
    m.updated_by = 1
WHERE m.parent_id IS NULL
  AND m.audience = 'ADMIN'
  AND m.deleted_at IS NULL
  AND m.program_key IN ('ADMIN_USERS', 'ADMIN_NOTICES', 'ADMIN_LOGIN_HISTORIES');

UPDATE menus m
    JOIN (SELECT id FROM menus WHERE name = '설정' AND audience = 'ADMIN' AND parent_id IS NULL
                                 AND program_key IS NULL AND deleted_at IS NULL ORDER BY id DESC LIMIT 1) g
SET m.parent_id  = g.id,
    m.sort_order = CASE m.program_key
                       WHEN 'ADMIN_ROLES' THEN 10
                       WHEN 'ADMIN_MENUS' THEN 20
                       WHEN 'ADMIN_CODES' THEN 30 END,
    m.updated_at = NOW(6),
    m.updated_by = 1
WHERE m.parent_id IS NULL
  AND m.audience = 'ADMIN'
  AND m.deleted_at IS NULL
  AND m.program_key IN ('ADMIN_ROLES', 'ADMIN_MENUS', 'ADMIN_CODES');

UPDATE menus
SET name = '공통코드', updated_at = NOW(6), updated_by = 1
WHERE program_key = 'ADMIN_CODES' AND name = '코드 관리' AND deleted_at IS NULL;
