-- moa: 공통코드 그룹에 순서를 둔다. 스펙 specs/004-admin-codes (FR-004·FR-005, 2026-11-21 결정)
-- 그룹 행도 끌어서 순서를 바꾼다. 기존 그룹은 만든 순서(id)로 채운다
ALTER TABLE tb_code_group
    ADD COLUMN sort_order INT NOT NULL DEFAULT 0 COMMENT '그룹 표시 순서' AFTER description;

UPDATE tb_code_group SET sort_order = id;
