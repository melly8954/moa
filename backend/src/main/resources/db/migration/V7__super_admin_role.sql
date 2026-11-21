-- moa: 시스템 역할 ADMIN을 "모든 권한"에서 "관리자 계정"으로 바꾸고, 모든 권한은 최고 관리자 역할이 가진다.
-- 스펙 specs/003-admin-roles (FR-003·FR-010·FR-011, research R20), specs/002-admin-users (FR-005)
-- 최고 관리자 역할은 권한 행을 두지 않는다. 모든 권한은 코드(PermissionGuard)가 준다

-- 1. 최고 관리자 역할
INSERT INTO tb_access_role (code, name, description, created_at, updated_at, created_by, updated_by)
SELECT 'SUPER_ADMIN', '최고 관리자', '모든 관리 메뉴와 기능을 써요', NOW(6), NOW(6), 1, 1
WHERE NOT EXISTS (SELECT 1 FROM tb_access_role WHERE code = 'SUPER_ADMIN' AND deleted_at IS NULL);

-- 2. 지금 ADMIN인 계정(그때까지 모든 권한)은 최고 관리자가 된다. 3보다 먼저 해야 운영자가 섞이지 않는다
INSERT IGNORE INTO tb_user_access_role (user_id, access_role_id, created_at, updated_at, created_by, updated_by)
SELECT u.id, r.id, NOW(6), NOW(6), 1, 1
FROM tb_user u
         JOIN tb_access_role r ON r.code = 'SUPER_ADMIN' AND r.deleted_at IS NULL
WHERE u.role = 'ADMIN'
  AND u.deleted_at IS NULL;

-- 3. 업무 역할을 가진 USER(운영자)는 관리자 계정이 된다. 회원(USER)은 역할을 받지 않는다
UPDATE tb_user
SET role       = 'ADMIN',
    updated_at = NOW(6),
    updated_by = 1
WHERE role = 'USER'
  AND id IN (SELECT user_id FROM tb_user_access_role);

-- 4. 시스템 역할의 뜻이 바뀌었으므로 COMMENT를 고친다 (docs/db.md "마이그레이션")
ALTER TABLE tb_user
    MODIFY COLUMN role VARCHAR(20) NOT NULL COMMENT '계정 종류 (USER: 회원, ADMIN: 관리자 계정)';

-- 5. 관리 메뉴 "역할 관리"를 "권한 관리"로. 관리자가 이미 다른 이름으로 바꾼 메뉴는 건드리지 않는다
UPDATE tb_menu
SET name       = '권한 관리',
    updated_at = NOW(6),
    updated_by = 1
WHERE screen_key = 'ADMIN_ROLES'
  AND name = '역할 관리'
  AND deleted_at IS NULL;
