-- 로컬 개발 계정. local 프로필에서만 읽는다 (application-local.yaml flyway.locations). 운영 DB에는 들어가지 않는다
-- admin / admin1234!, user / user1234!
INSERT IGNORE INTO tb_user (id, login_id, password, name, role, password_change_required,
                            created_at, updated_at, created_by, updated_by)
VALUES
    (2, 'admin', '{bcrypt}$2a$10$haqEv4o4uhzCJ92rDUeuGObSlq1SLU0fVvi/oX6xEm.F7W.fL7A3y', '관리자', 'ADMIN', FALSE,
        NOW(6), NOW(6), 1, 1),
    (3, 'user', '{bcrypt}$2a$10$Q6j5vCfAfvKEAvADya60o.e0YeVnlmy.w1oEXksje43SQt56/PWli', '사용자', 'USER', FALSE,
        NOW(6), NOW(6), 1, 1);

-- moa: admin은 최고 관리자 역할을 가진다 (specs/003-admin-roles FR-010, research R20).
-- 이 파일은 버전 마이그레이션(V7) 뒤에 돌아 V7이 admin을 보지 못하므로 여기서 넣는다
INSERT IGNORE INTO tb_user_access_role (user_id, access_role_id, created_at, updated_at, created_by, updated_by)
SELECT 2, r.id, NOW(6), NOW(6), 1, 1
FROM tb_access_role r
WHERE r.code = 'SUPER_ADMIN'
  AND r.deleted_at IS NULL;
