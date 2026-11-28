-- moa: 회원 탈퇴 시각·파기 시각. 스펙 specs/018-member-withdrawal (FR-009·FR-012·FR-013·FR-019, data-model.md)
-- 유예 중에는 deleted_at을 쓰지 않는다. active_key가 1로 남아 로그인 아이디가 유니크에 붙잡힌다 (research R1)
ALTER TABLE tb_user
    ADD COLUMN withdrawn_at DATETIME(6) NULL COMMENT '탈퇴한 시각 (NULL: 탈퇴 안 함)' AFTER password_change_required,
    ADD COLUMN purged_at    DATETIME(6) NULL COMMENT '개인정보 파기 시각 (NULL: 파기 안 함)' AFTER withdrawn_at,
    ADD KEY idx_user_purged_at_withdrawn_at (purged_at, withdrawn_at),
    ADD KEY idx_user_purged_at_deleted_at (purged_at, deleted_at);

-- 파기 이력의 대상 종류에 회원(MEMBER)을 더한다. 타입은 그대로다 (docs/db.md "COMMENT", specs/013-purge-log FR-002)
ALTER TABLE tb_purge_log
    MODIFY COLUMN target VARCHAR(30) NOT NULL COMMENT '파기 대상 종류 (LOGIN_HISTORY, NOTIFICATION, REFRESH_TOKEN, RICH_TEXT_IMAGE, MEMBER)';
