-- admin 프리셋: menu 헬퍼의 "관리" 그룹(id 1) 아래에 사용자 관리·코드 관리·접속 이력·역할 관리 메뉴를 더한다
-- 화면 경로와 권한 키는 헬퍼 계약으로 고정이다
INSERT INTO menus (id, parent_id, name, path, icon, sort_order, enabled, permission,
                   created_at, updated_at, created_by, updated_by)
VALUES (6, 1, '사용자 관리', '/admin/users', 'users', 5, TRUE, 'USER_MANAGE', NOW(6), NOW(6), 1, 1),
       (3, 1, '코드 관리', '/admin/codes', 'list', 20, TRUE, 'CODE_MANAGE', NOW(6), NOW(6), 1, 1),
       (4, 1, '접속 이력', '/admin/login-histories', 'history', 30, TRUE, 'LOGIN_HISTORY_READ', NOW(6), NOW(6), 1, 1),
       (5, 1, '역할 관리', '/admin/roles', 'shield', 40, TRUE, 'ROLE_MANAGE', NOW(6), NOW(6), 1, 1);
