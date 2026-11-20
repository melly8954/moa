-- moa: 관리 메뉴 "공통코드"를 "코드 관리"로 되돌린다. 스펙 specs/001-admin-menus (FR-001, 2026-11-21 결정)
-- 관리자가 이미 다른 이름으로 바꾼 메뉴는 건드리지 않는다 (docs/db.md "마이그레이션")
UPDATE tb_menu
SET name       = '코드 관리',
    updated_at = NOW(6),
    updated_by = 1
WHERE program_key = 'ADMIN_CODES'
  AND name = '공통코드'
  AND deleted_at IS NULL;
