-- moa 개발자 절차: 최고 관리자 비밀번호 되돌리기. 스펙 specs/002-admin-users (FR-009, research R14), quickstart "운영: 최고 관리자 되돌리기"
-- 최고 관리자가 비밀번호를 잊었거나 잠겨 로그인을 못 할 때만 쓴다. 서버가 실행하는 파일이 아니다(Flyway 위치 밖). 서버는 다시 띄우지 않는다
-- 아래 두 줄만 고쳐 DB에서 실행한다. 임시 비밀번호의 bcrypt 값은 quickstart의 명령으로 만들고 앞에 {bcrypt}를 붙인다
SET @login := 'admin';
SET @hash := '{bcrypt}$2y$10$replace.with.bcrypt.hash.of.temporary.password.xxxxxxxxxxxx';

START TRANSACTION;

SET @super := (SELECT id FROM tb_access_role WHERE code = 'SUPER_ADMIN' AND deleted_at IS NULL);
SET @user_id := (SELECT u.id
                 FROM tb_user u
                          JOIN tb_user_access_role r ON r.user_id = u.id AND r.access_role_id = @super
                 WHERE u.login_id = @login
                   AND u.deleted_at IS NULL);

-- 확인: 아이디가 지금 최고 관리자가 아니거나 값이 bcrypt가 아니면 여기서 멈춘다 ("Column 'ok' cannot be null"). 아무것도 바뀌지 않는다
DROP TEMPORARY TABLE IF EXISTS moa_ops_check;
CREATE TEMPORARY TABLE moa_ops_check (ok BIGINT NOT NULL);
INSERT INTO moa_ops_check VALUES (@user_id);
INSERT INTO moa_ops_check VALUES (IF(@hash LIKE '{bcrypt}$2%', 1, NULL));
DROP TEMPORARY TABLE moa_ops_check;

-- 오류가 나도 다음 문을 계속 실행하는 도구에서도 안전하도록, 확인이 틀리면 아래 문은 아무 줄도 고르지 않는다

-- 임시 비밀번호로 바꾸고 다음 로그인 때 변경을 요구한다. 잠겨 있었으면 푼다
UPDATE tb_user
SET password                 = @hash,
    password_change_required = TRUE,
    locked_at                = NULL,
    updated_at               = NOW(6),
    updated_by               = 1
WHERE id = @user_id
  AND @hash LIKE '{bcrypt}$2%';

-- 모든 기기에서 로그아웃 (리프레시 토큰 폐기)
UPDATE tb_refresh_token
SET revoked_at = NOW(6)
WHERE user_id = @user_id
  AND @hash LIKE '{bcrypt}$2%'
  AND revoked_at IS NULL;

COMMIT;

-- 결과: 그 계정이 변경 요구·잠금 없음 상태인지, 최고 관리자가 한 명인지 본다
SELECT login_id, password_change_required, locked_at FROM tb_user WHERE id = @user_id;
SELECT COUNT(*) AS super_admins FROM tb_user_access_role WHERE access_role_id = @super;
