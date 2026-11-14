-- 본문 이미지 (선택 헬퍼 rich-text). 올리면 임시(TEMP), 글에 쓰이면 사용(USED). 임시는 보관 시간이 지나면 지운다
CREATE TABLE rich_text_images (
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    public_id     CHAR(36)      NOT NULL COMMENT '주소에 쓰는 추측하기 어려운 ID (UUID)',
    path          VARCHAR(300)  NOT NULL COMMENT 'image-dir 아래 상대 경로',
    content_type  VARCHAR(50)   NOT NULL COMMENT '파일 내용으로 판별한 형식 (image/png 등)',
    size          BIGINT        NOT NULL,
    status        VARCHAR(10)   NOT NULL COMMENT 'TEMP, USED',
    owner_type    VARCHAR(50)   NULL COMMENT '쓰는 글의 종류 (예: NOTICE). USED일 때만',
    owner_id      BIGINT        NULL COMMENT '쓰는 글의 ID. USED일 때만',
    created_at    DATETIME(6)   NOT NULL,
    updated_at    DATETIME(6)   NOT NULL COMMENT 'TEMP가 된 시각 기준으로 정리한다',
    created_by    BIGINT        NOT NULL,
    updated_by    BIGINT        NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_rich_text_images_public_id (public_id),
    KEY idx_rich_text_images_owner (owner_type, owner_id),
    KEY idx_rich_text_images_status_updated (status, updated_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
