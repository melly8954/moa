-- moa: 알림. 스펙 specs/007-notifications (data-model.md)
-- 개인 알림. 전체 알림(공지 게시)은 저장하지 않는다. 90일 지나면 정리기가 지운다
CREATE TABLE notifications (
    id          BIGINT        NOT NULL AUTO_INCREMENT,
    user_id     BIGINT        NOT NULL COMMENT '받는 사람',
    type        VARCHAR(30)   NOT NULL COMMENT 'NotificationType (SYSTEM 등)',
    title       VARCHAR(200)  NOT NULL,
    link        VARCHAR(500)  NOT NULL COMMENT '관련 화면 경로 (/로 시작)',
    created_at  DATETIME(6)   NOT NULL,
    read_at     DATETIME(6)   NULL COMMENT '비어 있으면 안 읽음',
    PRIMARY KEY (id),
    KEY idx_notifications_user_created (user_id, created_at),
    KEY idx_notifications_user_read (user_id, read_at),
    KEY idx_notifications_created (created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- 브라우저 알림(웹 푸시) 구독. 한 사용자가 브라우저마다 하나. 같은 endpoint는 하나만 둔다
CREATE TABLE push_subscriptions (
    id          BIGINT         NOT NULL AUTO_INCREMENT,
    user_id     BIGINT         NOT NULL,
    endpoint    VARCHAR(1000)  NOT NULL COMMENT '브라우저 푸시 서비스 주소',
    p256dh      VARCHAR(200)   NOT NULL COMMENT '브라우저 공개 키 (base64url)',
    auth        VARCHAR(100)   NOT NULL COMMENT '인증 비밀 (base64url)',
    created_at  DATETIME(6)    NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_push_subscriptions_endpoint (endpoint(700)),
    KEY idx_push_subscriptions_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
