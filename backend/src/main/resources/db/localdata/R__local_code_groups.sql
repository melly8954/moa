-- 로컬 화면 확인용 시험 코드 그룹과 항목 (specs/004-admin-codes quickstart "화면 확인", research R11).
-- local 프로필에서만 읽는다 (application-local.yaml flyway.locations). 모두 사용 중으로 넣는다
-- 항목 코드는 뜻이 보이게 적는다 (자동 순번 없음, FR-006 2026-11-21)
INSERT IGNORE INTO tb_code_group (id, group_code, name, description, sort_order, enabled,
    created_at, updated_at, created_by, updated_by)
VALUES
    (1, 'LOCAL_GAME_GENRE', '게임 장르 (시험)', '화면 확인용 시험 그룹', 1, TRUE, NOW(6), NOW(6), 1, 1),
    (2, 'LOCAL_REPORT_REASON', '신고 사유 (시험)', '화면 확인용 시험 그룹', 2, TRUE, NOW(6), NOW(6), 1, 1);

INSERT IGNORE INTO tb_code (group_code, code, name, sort_order, enabled,
    created_at, updated_at, created_by, updated_by)
VALUES
    ('LOCAL_GAME_GENRE', 'FPS', 'FPS', 0, TRUE, NOW(6), NOW(6), 1, 1),
    ('LOCAL_GAME_GENRE', 'RPG', '롤플레잉', 1, TRUE, NOW(6), NOW(6), 1, 1),
    ('LOCAL_GAME_GENRE', 'MOBA', 'MOBA', 2, TRUE, NOW(6), NOW(6), 1, 1),
    ('LOCAL_REPORT_REASON', 'ABUSE', '욕설·비방', 0, TRUE, NOW(6), NOW(6), 1, 1),
    ('LOCAL_REPORT_REASON', 'SPAM', '도배·광고', 1, TRUE, NOW(6), NOW(6), 1, 1);
