-- moa: 관리 메뉴 "파기 기록"을 "파기 이력"으로 바꾼다. 스펙 specs/013-purge-log (이름, 2026-11-22 결정), specs/001-admin-menus (FR-001)
-- 관리자가 이미 다른 이름으로 바꾼 메뉴는 건드리지 않는다 (docs/db.md "마이그레이션")
UPDATE tb_menu
SET name       = '파기 이력',
    updated_at = NOW(6),
    updated_by = 1
WHERE screen_key = 'ADMIN_PURGE_LOGS'
  AND name = '파기 기록'
  AND deleted_at IS NULL;
