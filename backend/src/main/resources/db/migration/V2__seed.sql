-- moa: 모든 환경에 들어가는 처음 데이터. 근거 docs/db.md "마이그레이션"
-- 이전 마이그레이션(V1~V2_10)이 남긴 데이터를 합쳤다 (2026-11-21). 지운 메뉴(옛 "관리" 묶음, id 1)는 옮기지 않았다.
-- 이 파일은 고치지 않는다. 로컬 시험 계정은 db/localdata에 있다

-- 시스템 계정. 로그인 전 요청과 배치 작업의 감사 행위자다 (app.audit.system-actor-id 기본값 1)
INSERT INTO tb_user (id, login_id, password, name, role, password_change_required,
                     created_at, updated_at, created_by, updated_by)
VALUES (1, 'system', NULL, '시스템', 'USER', FALSE, NOW(6), NOW(6), 1, 1);

-- 메뉴 처음 구성 (specs/001-admin-menus FR-001·FR-007)
-- 관리 메뉴: 운영(사용자 관리, 공지 관리, 접속 이력), 설정(역할 관리, 메뉴 관리, 공통코드)
-- 사용자 메뉴: 홈, 공지 (모두 공개)
INSERT INTO tb_menu (id, parent_id, name, program_key, icon, sort_order, enabled, audience, visibility,
                     created_at, updated_at, created_by, updated_by)
VALUES (10, NULL, '운영', NULL, 'briefcase', 10, TRUE, 'ADMIN', NULL, NOW(6), NOW(6), 1, 1),
       (11, NULL, '설정', NULL, 'settings', 20, TRUE, 'ADMIN', NULL, NOW(6), NOW(6), 1, 1),
       (6, 10, '사용자 관리', 'ADMIN_USERS', 'users', 10, TRUE, 'ADMIN', NULL, NOW(6), NOW(6), 1, 1),
       (7, 10, '공지 관리', 'ADMIN_NOTICES', 'clipboard', 20, TRUE, 'ADMIN', NULL, NOW(6), NOW(6), 1, 1),
       (4, 10, '접속 이력', 'ADMIN_LOGIN_HISTORIES', 'history', 30, TRUE, 'ADMIN', NULL, NOW(6), NOW(6), 1, 1),
       (5, 11, '역할 관리', 'ADMIN_ROLES', 'shield', 10, TRUE, 'ADMIN', NULL, NOW(6), NOW(6), 1, 1),
       (2, 11, '메뉴 관리', 'ADMIN_MENUS', 'menu', 20, TRUE, 'ADMIN', NULL, NOW(6), NOW(6), 1, 1),
       (3, 11, '공통코드', 'ADMIN_CODES', 'list', 30, TRUE, 'ADMIN', NULL, NOW(6), NOW(6), 1, 1),
       (8, NULL, '홈', 'HOME', 'home', 10, TRUE, 'USER', 'PUBLIC', NOW(6), NOW(6), 1, 1),
       (9, NULL, '공지', 'NOTICES', 'megaphone', 20, TRUE, 'USER', 'PUBLIC', NOW(6), NOW(6), 1, 1);
