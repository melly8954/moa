-- 로컬 개발 계정. local 프로필에서만 읽는다 (application-local.yaml flyway.locations). 운영 DB에는 들어가지 않는다
-- admin / admin1234!, user / user1234!
INSERT IGNORE INTO users (id, login_id, password, name, role, created_at, updated_at, created_by, updated_by)
VALUES
    (2, 'admin', '{bcrypt}$2a$10$haqEv4o4uhzCJ92rDUeuGObSlq1SLU0fVvi/oX6xEm.F7W.fL7A3y', '관리자', 'ADMIN',
        NOW(6), NOW(6), 1, 1),
    (3, 'user', '{bcrypt}$2a$10$Q6j5vCfAfvKEAvADya60o.e0YeVnlmy.w1oEXksje43SQt56/PWli', '사용자', 'USER',
        NOW(6), NOW(6), 1, 1);
