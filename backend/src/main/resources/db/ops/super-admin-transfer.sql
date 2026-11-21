-- moa 개발자 절차: 최고 관리자 바꾸기. 스펙 specs/002-admin-users (FR-009, research R14), quickstart "운영: 최고 관리자 되돌리기"
-- 최고 관리자인 사람이 바뀔 때만 쓴다. 서버가 실행하는 파일이 아니다(Flyway 위치 밖). 서버는 다시 띄우지 않는다
-- 새 사람은 사용자 관리에서 관리자 계정으로 먼저 만들어 둔다. 아래 한 줄만 고쳐 DB에서 실행한다
SET @to := 'new-admin';

START TRANSACTION;

SET @super := (SELECT id FROM tb_access_role WHERE code = 'SUPER_ADMIN' AND deleted_at IS NULL);
-- 변수는 연결의 정렬 규칙을 따르므로 표의 정렬 규칙(utf8mb4_unicode_ci, docs/db.md)으로 바꿔 비교한다
SET @to_id := (SELECT id FROM tb_user WHERE login_id = CONVERT(@to USING utf8mb4) COLLATE utf8mb4_unicode_ci AND role = 'ADMIN' AND deleted_at IS NULL);
SET @from_id := (SELECT user_id FROM tb_user_access_role WHERE access_role_id = @super);

-- 확인: 새 사람이 없는 아이디·회원·지운 계정이면 여기서 멈춘다 ("Column 'ok' cannot be null"). 아무것도 바뀌지 않는다
DROP TEMPORARY TABLE IF EXISTS moa_ops_check;
CREATE TEMPORARY TABLE moa_ops_check (ok BIGINT NOT NULL);
INSERT INTO moa_ops_check VALUES (@to_id);
INSERT INTO moa_ops_check VALUES (@super);
DROP TEMPORARY TABLE moa_ops_check;

-- 오류가 나도 다음 문을 계속 실행하는 도구에서도 안전하도록, 새 사람이 없으면 아래 문은 아무 줄도 고르지 않는다

-- 새 사람의 다른 역할을 뺀다 (최고 관리자는 다른 역할을 받지 않는다)
DELETE FROM tb_user_access_role WHERE user_id = @to_id;

-- 예전 최고 관리자 줄을 먼저 지우고 새 줄을 넣는다. 최고 관리자 줄은 DB 유일 제약으로 하나만 들어가므로 순서를 바꾸면 DB가 거부한다 (V8)
DELETE FROM tb_user_access_role WHERE access_role_id = @super AND @to_id IS NOT NULL;
INSERT INTO tb_user_access_role (user_id, access_role_id, created_at, updated_at, created_by, updated_by)
SELECT @to_id, @super, NOW(6), NOW(6), 1, 1
FROM DUAL
WHERE @to_id IS NOT NULL;

-- 예전 최고 관리자는 역할 없는 관리자 계정이 된다. 모든 기기에서 로그아웃시킨다
UPDATE tb_refresh_token
SET revoked_at = NOW(6)
WHERE user_id = @from_id
  AND @to_id IS NOT NULL
  AND revoked_at IS NULL;

COMMIT;

-- 결과: 최고 관리자가 새 사람 한 명인지 본다
SELECT u.login_id AS super_admin
FROM tb_user_access_role r
         JOIN tb_user u ON u.id = r.user_id
WHERE r.access_role_id = @super;
