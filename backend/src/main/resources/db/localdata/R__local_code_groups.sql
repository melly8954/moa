-- 로컬 화면 확인용 시험 코드 그룹 (specs/004-admin-codes quickstart "화면 확인", research R9).
-- local 프로필에서만 읽는다 (application-local.yaml flyway.locations). 시스템 그룹 하나, 일반 그룹 하나
INSERT IGNORE INTO code_groups (id, group_code, name, description, used_by, `system`,
    created_at, updated_at, created_by, updated_by)
VALUES
    (1, 'LOCAL_GAME_GENRE', '게임 장르 (시험)', '화면 확인용 시스템 그룹', 'local 프로필 화면 확인', TRUE,
        NOW(6), NOW(6), 1, 1),
    (2, 'LOCAL_REPORT_REASON', '신고 사유 (시험)', '화면 확인용 일반 그룹', NULL, FALSE,
        NOW(6), NOW(6), 1, 1);
