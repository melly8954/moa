-- moa: 키트 시드(V1_7, V1_7_1)가 "관리" 그룹(id 1) 아래에 둔 관리 메뉴를 최상위로 옮기고 그룹을 지운다
-- 순서와 권한은 키트 시드 그대로다. 스펙 specs/001-admin-menus FR-001
UPDATE menus SET parent_id = NULL, updated_at = NOW(6), updated_by = 1 WHERE parent_id = 1;
UPDATE menus SET deleted_at = NOW(6), deleted_by = 1, updated_at = NOW(6), updated_by = 1 WHERE id = 1;
