-- moa: 공통코드는 지우지 않고 사용 여부로만 끈다 (specs/004-admin-codes FR-004·FR-008, research R11).
-- 시스템 그룹 구분(system)과 쓰는 곳(used_by)을 없애고 그룹에 사용 여부를 더한다. 소프트 삭제 열은 키트 표 그대로 두고 쓰지 않는다
ALTER TABLE code_groups
    DROP COLUMN used_by,
    DROP COLUMN `system`,
    ADD COLUMN enabled BOOLEAN NOT NULL DEFAULT TRUE AFTER description;
