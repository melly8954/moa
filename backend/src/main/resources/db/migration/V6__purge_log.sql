-- moa: 개인정보 파기 기록 테이블과 관리 메뉴. 스펙 specs/013-purge-log (FR-001·FR-007)
-- 정리기가 개인정보를 지운 사실(종류·건수·방법·처리자)을 로그 파일과 따로 1년 남긴다. 개인정보는 넣지 않는다
CREATE TABLE tb_purge_log (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    target          VARCHAR(30)  NOT NULL COMMENT '파기 대상 종류 (LOGIN_HISTORY, NOTIFICATION, REFRESH_TOKEN, RICH_TEXT_IMAGE)',
    purge_method    VARCHAR(20)  NOT NULL COMMENT '파기 방법 (ROW_DELETE, COLUMN_ERASE)',
    purged_count    INT          NOT NULL COMMENT '파기 건수 (1 이상)',
    actor_type      VARCHAR(20)  NOT NULL COMMENT '처리자 종류 (SCHEDULER, PERSON)',
    actor_user_id   BIGINT       NULL     COMMENT '처리한 관리자 ID (NULL: 정리기)',
    target_user_id  BIGINT       NULL     COMMENT '파기 대상 회원 ID (NULL: 개별 파기 아님)',
    created_at      DATETIME(6)  NOT NULL,
    updated_at      DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    KEY idx_purge_log_created_at (created_at),
    KEY idx_purge_log_target_created_at (target, created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '개인정보 파기 기록 (개인정보 없음)';

-- 관리 메뉴: 운영 아래 접속 이력 다음 (specs/001-admin-menus FR-001)
INSERT INTO tb_menu (parent_id, name, screen_key, icon, sort_order, enabled, audience, visibility,
                     created_at, updated_at, created_by, updated_by)
VALUES (10, '파기 기록', 'ADMIN_PURGE_LOGS', 'file', 40, TRUE, 'ADMIN', NULL, NOW(6), NOW(6), 1, 1);
