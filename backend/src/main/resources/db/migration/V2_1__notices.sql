-- moa: 공지사항. 스펙 specs/006-notices (data-model.md)
-- 본문은 저장 때 rich-text 헬퍼가 거른 HTML이다. 본문 이미지는 rich_text_images(owner_type 'NOTICE')에 있다
CREATE TABLE notices (
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    title         VARCHAR(200)  NOT NULL,
    content       MEDIUMTEXT    NOT NULL,
    status        VARCHAR(20)   NOT NULL COMMENT 'DRAFT(임시 저장), PUBLISHED(게시)',
    pinned        BOOLEAN       NOT NULL DEFAULT FALSE,
    published_at  DATETIME(6)   NULL COMMENT '처음 게시한 시각. 다시 바꾸지 않는다',
    created_at    DATETIME(6)   NOT NULL,
    updated_at    DATETIME(6)   NOT NULL,
    created_by    BIGINT        NOT NULL,
    updated_by    BIGINT        NOT NULL,
    deleted_at    DATETIME(6)   NULL,
    deleted_by    BIGINT        NULL,
    PRIMARY KEY (id),
    KEY idx_notices_public (status, deleted_at, pinned, published_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- 사용자가 공지 목록을 마지막으로 연 시각. 사용자마다 한 행이다 (새 공지 표시)
CREATE TABLE notice_reads (
    user_id       BIGINT        NOT NULL,
    last_seen_at  DATETIME(6)   NOT NULL,
    PRIMARY KEY (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
