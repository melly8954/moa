-- 로컬 화면 확인용 시험 코드 그룹 (specs/004-admin-codes quickstart "화면 확인", research R11).
-- local 프로필에서만 읽는다 (application-local.yaml flyway.locations). 둘 다 사용 중으로 넣는다
INSERT IGNORE INTO code_groups (id, group_code, name, description, enabled,
    created_at, updated_at, created_by, updated_by)
VALUES
    (1, 'LOCAL_GAME_GENRE', '게임 장르 (시험)', '화면 확인용 시험 그룹', TRUE, NOW(6), NOW(6), 1, 1),
    (2, 'LOCAL_REPORT_REASON', '신고 사유 (시험)', '화면 확인용 시험 그룹', TRUE, NOW(6), NOW(6), 1, 1);
