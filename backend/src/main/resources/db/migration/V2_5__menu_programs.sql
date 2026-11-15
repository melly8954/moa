-- moa: 메뉴는 경로·권한 대신 프로그램을 가리키고, 사용자 메뉴는 공개 범위를 갖는다 (specs/001-admin-menus FR-007~FR-009)
-- 프로그램은 코드에 등록한다 (ProgramSource). 기존 메뉴는 경로로 프로그램을 찾는다. 맞는 것이 없으면 MISSING(없는 프로그램)
ALTER TABLE menus
    ADD COLUMN program_key VARCHAR(50) NULL COMMENT '가리키는 프로그램 키. NULL이면 묶음 메뉴(1단계만)' AFTER name,
    ADD COLUMN visibility  VARCHAR(10) NULL COMMENT '사용자 메뉴의 공개 범위 (PUBLIC 모두, MEMBER 로그인한 사용자). 관리 메뉴·묶음 메뉴는 NULL' AFTER audience;

UPDATE menus
SET program_key = CASE path
                      WHEN '/admin/users' THEN 'ADMIN_USERS'
                      WHEN '/admin/menus' THEN 'ADMIN_MENUS'
                      WHEN '/admin/codes' THEN 'ADMIN_CODES'
                      WHEN '/admin/login-histories' THEN 'ADMIN_LOGIN_HISTORIES'
                      WHEN '/admin/roles' THEN 'ADMIN_ROLES'
                      WHEN '/admin/notices' THEN 'ADMIN_NOTICES'
                      WHEN '/' THEN 'HOME'
                      WHEN '/notices' THEN 'NOTICES'
                      ELSE 'MISSING' END
WHERE path IS NOT NULL;

UPDATE menus SET visibility = 'PUBLIC' WHERE audience = 'USER' AND program_key IS NOT NULL;

ALTER TABLE menus
    DROP COLUMN path,
    DROP COLUMN permission;
