-- moa: 고객센터(FAQ·1:1 문의) 테이블, 분류 코드, 메뉴. 스펙 specs/017-customer-center (FR-001~FR-004, FR-007, FR-008, FR-014)
-- 문의 상태는 칸에 두지 않고 answered_at으로 읽는다 (research R3). 문의는 개인정보가 든 행이라 하드 삭제다 (docs/privacy.md)

CREATE TABLE tb_faq (
    id             BIGINT        NOT NULL AUTO_INCREMENT,
    category_code  VARCHAR(50)   NOT NULL COMMENT '분류 (SUPPORT_CATEGORY 항목 코드)',
    question       VARCHAR(200)  NOT NULL COMMENT '질문',
    answer         MEDIUMTEXT    NOT NULL COMMENT '답 (정제된 HTML)',
    sort_order     INT           NOT NULL COMMENT '순서 (작을수록 먼저)',
    enabled        BOOLEAN       NOT NULL COMMENT '켜짐 여부 (사용자에게 보임)',
    created_at     DATETIME(6)   NOT NULL,
    updated_at     DATETIME(6)   NOT NULL,
    created_by     BIGINT        NOT NULL,
    updated_by     BIGINT        NOT NULL,
    deleted_at     DATETIME(6)   NULL,
    deleted_by     BIGINT        NULL,
    PRIMARY KEY (id),
    KEY idx_faq_deleted_at_sort_order_created_at (deleted_at, sort_order, created_at),
    KEY idx_faq_category_code (category_code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '자주 묻는 질문';

CREATE TABLE tb_inquiry (
    id             BIGINT         NOT NULL AUTO_INCREMENT,
    user_id        BIGINT         NOT NULL COMMENT '쓴 회원',
    category_code  VARCHAR(50)    NOT NULL COMMENT '분류 (SUPPORT_CATEGORY 항목 코드)',
    title          VARCHAR(100)   NOT NULL COMMENT '제목',
    content        VARCHAR(2000)  NOT NULL COMMENT '내용 (서식 없는 글)',
    answer         MEDIUMTEXT     NULL     COMMENT '답변 (정제된 HTML)',
    answered_at    DATETIME(6)    NULL     COMMENT '처음 답변한 시각 (NULL: 답변 대기)',
    answered_by    BIGINT         NULL     COMMENT '처음 답변한 관리자',
    created_at     DATETIME(6)    NOT NULL,
    updated_at     DATETIME(6)    NOT NULL,
    created_by     BIGINT         NOT NULL,
    updated_by     BIGINT         NOT NULL,
    PRIMARY KEY (id),
    KEY idx_inquiry_user_id_created_at (user_id, created_at),
    KEY idx_inquiry_answered_at_created_at (answered_at, created_at),
    KEY idx_inquiry_category_code (category_code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci
    COMMENT = '1:1 문의 (개인정보: 탈퇴한 날부터 1년 보관, docs/privacy.md; 하드 삭제)';

CREATE TABLE tb_inquiry_image (
    inquiry_id  BIGINT        NOT NULL COMMENT '주인 문의',
    sort_order  INT           NOT NULL COMMENT '붙인 순서 (0~4)',
    image_url   VARCHAR(100)  NOT NULL COMMENT '이미지 주소',
    PRIMARY KEY (inquiry_id, sort_order)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci
    COMMENT = '문의 첨부 이미지 (PK: 문의별 순서; 감사 열 없음: 컬렉션 테이블)';

-- 고객센터 분류 (specs/017-customer-center FR-003, specs/004-admin-codes FR-001). 관리자가 바꾸는 데이터라 넣기만 한다
INSERT INTO tb_code_group (group_code, name, description, sort_order, enabled, created_at, updated_at, created_by, updated_by)
SELECT 'SUPPORT_CATEGORY', '고객센터 분류', 'FAQ와 1:1 문의의 분류',
       (SELECT COALESCE(MAX(g.sort_order), 0) + 1 FROM tb_code_group g), TRUE, NOW(6), NOW(6), 1, 1
WHERE NOT EXISTS (SELECT 1 FROM tb_code_group g WHERE g.group_code = 'SUPPORT_CATEGORY' AND g.deleted_at IS NULL);

INSERT INTO tb_code (group_code, code, name, sort_order, enabled, created_at, updated_at, created_by, updated_by)
SELECT 'SUPPORT_CATEGORY', v.code, v.name, v.sort_order, TRUE, NOW(6), NOW(6), 1, 1
FROM (SELECT 'ACCOUNT' AS code, '계정' AS name, 1 AS sort_order
      UNION ALL SELECT 'USAGE', '이용 방법', 2
      UNION ALL SELECT 'ERROR', '오류', 3
      UNION ALL SELECT 'SUGGESTION', '제안', 4
      UNION ALL SELECT 'ETC', '기타', 5) v
WHERE NOT EXISTS (SELECT 1 FROM tb_code c
                  WHERE c.group_code = 'SUPPORT_CATEGORY' AND c.code = v.code AND c.deleted_at IS NULL);

-- 관리 메뉴: 운영(id 10) 아래 공지 관리(20)와 접속 이력(30) 사이 (specs/001-admin-menus FR-001)
-- 사용자 메뉴: 고객센터 (specs/001-admin-menus FR-007). 자리는 사용자 쪽 담당이 정하면 메뉴 관리에서 옮긴다
-- 기존 메뉴 행은 고치지 않는다 (docs/db.md)
INSERT INTO tb_menu (parent_id, name, screen_key, icon, sort_order, enabled, audience, visibility,
                     created_at, updated_at, created_by, updated_by)
VALUES (10, 'FAQ 관리', 'ADMIN_FAQS', 'list', 23, TRUE, 'ADMIN', NULL, NOW(6), NOW(6), 1, 1),
       (10, '문의 관리', 'ADMIN_INQUIRIES', 'mail', 26, TRUE, 'ADMIN', NULL, NOW(6), NOW(6), 1, 1),
       (NULL, '고객센터', 'SUPPORT', 'mail', 90, TRUE, 'USER', 'PUBLIC', NOW(6), NOW(6), 1, 1);
