-- moa: 메뉴의 연결 화면 키 열 이름을 program_key에서 screen_key로 바꾼다. 스펙 specs/001-admin-menus (FR-008, 2026-11-21 결정)
-- 코드의 Program(프로그램)을 Screen(화면)으로 바꾸면서 열 이름도 맞춘다. 값과 타입은 그대로다
ALTER TABLE tb_menu
    CHANGE COLUMN program_key screen_key VARCHAR(50) NULL COMMENT '연결 화면 키 (NULL: 상위 메뉴)';
