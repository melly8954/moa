-- moa: 최고 관리자를 첫 관리자 한 명으로 고정한다. 스펙 specs/003-admin-roles (FR-010, Clarifications 2026-11-22, research R22)
-- 화면·API는 앱이 막고, DB는 유일 제약으로 최고 관리자 줄이 하나만 들어가게 한다 (active_key와 같은 방식)

-- 0. 이 DB의 최고 관리자 역할 ID (DB마다 다를 수 있다. V7이 넣은 행이고 지우거나 다시 만들 수 없다)
SET @super := (SELECT id FROM tb_access_role WHERE code = 'SUPER_ADMIN' AND deleted_at IS NULL);

-- 1. V7이 그때 ADMIN이던 계정을 모두 최고 관리자로 넣었으므로, 여럿이면 ID가 가장 작은 한 명(부트스트랩으로 만든 첫 관리자)만 남긴다.
--    나머지는 계정은 그대로이고 최고 관리자 줄만 빠져 역할 없는 관리자 계정이 된다
DELETE FROM tb_user_access_role
WHERE access_role_id = @super
  AND user_id > (SELECT m FROM (SELECT MIN(user_id) AS m FROM tb_user_access_role WHERE access_role_id = @super) AS t);

-- 2. 최고 관리자 줄이면 1, 그 밖은 NULL인 생성 열과 유일 제약. 생성 열 식은 다른 표를 볼 수 없어 역할 ID를 숫자로 박는다.
--    @super가 NULL이면(최고 관리자 역할이 없는 DB) 문장이 NULL이 되어 PREPARE가 실패하고 마이그레이션이 멈춘다
SET @ddl := CONCAT(
    'ALTER TABLE tb_user_access_role ',
    'ADD COLUMN super_slot TINYINT AS (IF(access_role_id = ', @super, ', 1, NULL)) VIRTUAL ',
    'COMMENT ''최고 관리자 자리 (1: 최고 관리자 줄, NULL: 그 밖)'', ',
    'ADD UNIQUE KEY uk_user_access_role_super_slot (super_slot)');
PREPARE add_super_slot FROM @ddl;
EXECUTE add_super_slot;
DEALLOCATE PREPARE add_super_slot;
