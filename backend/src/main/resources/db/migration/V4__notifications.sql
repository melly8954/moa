-- 알림 (docs/req/functional/notification/_policy.md)
-- 받는 회원(member_id)의 서비스 안 알림 목록. 보관 기간(30일)이 지나면 지운다(하드 삭제).
-- type은 알림 사건, target_type·target_id는 알림을 누르면 갈 대상이다.
-- actor_id는 알림을 일으킨 회원이다. 시스템이나 관리자 조치처럼 행위자가 없으면 NULL.
CREATE TABLE notifications (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    member_id   BIGINT      NOT NULL,
    type        VARCHAR(50) NOT NULL,
    actor_id    BIGINT      NULL,
    target_type VARCHAR(30) NULL,
    target_id   BIGINT      NULL,
    read_at     DATETIME(6) NULL,
    created_at  DATETIME(6) NOT NULL,
    updated_at  DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_notifications_member_id_created_at (member_id, created_at DESC, id DESC),
    KEY idx_notifications_member_id_read_at (member_id, read_at),
    KEY idx_notifications_actor_id (actor_id),
    KEY idx_notifications_created_at (created_at),
    CONSTRAINT fk_notifications_members FOREIGN KEY (member_id) REFERENCES members (id),
    CONSTRAINT fk_notifications_members_actor FOREIGN KEY (actor_id) REFERENCES members (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
