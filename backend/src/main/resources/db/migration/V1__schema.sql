-- moa: 처음 스키마 전체. 근거 docs/db.md "마이그레이션"
-- 키트(springboot 골격·헬퍼·admin 프리셋)와 moa의 이전 마이그레이션(V1~V2_10)을 하나로 합쳤다 (2026-11-21, 배포 DB 없음).
-- 이 파일은 고치지 않는다. 바꿀 것은 V3부터 새 파일로 더한다

CREATE TABLE tb_user (
    id                        BIGINT        NOT NULL AUTO_INCREMENT,
    login_id                  VARCHAR(50)   NOT NULL COMMENT '로그인 아이디',
    password                  VARCHAR(100)  NULL     COMMENT '비밀번호 해시 ({bcrypt} 형식; NULL: 비밀번호 로그인 불가)',
    name                      VARCHAR(50)   NOT NULL COMMENT '표시 이름',
    role                      VARCHAR(20)   NOT NULL COMMENT '시스템 역할 (USER, ADMIN)',
    locked_at                 DATETIME(6)   NULL     COMMENT '잠근 시각 (NULL: 안 잠김)',
    password_change_required  BOOLEAN       NOT NULL COMMENT '비밀번호 변경 필요 여부 (임시 비밀번호 발급 뒤)',
    created_at                DATETIME(6)   NOT NULL,
    updated_at                DATETIME(6)   NOT NULL,
    created_by                BIGINT        NOT NULL,
    updated_by                BIGINT        NOT NULL,
    deleted_at                DATETIME(6)   NULL,
    deleted_by                BIGINT        NULL,
    active_key                TINYINT AS (IF(deleted_at IS NULL, 1, NULL)) VIRTUAL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_login_id_active_key (login_id, active_key)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '사용자';

CREATE TABLE tb_refresh_token (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    user_id     BIGINT       NOT NULL COMMENT '사용자 ID',
    family_id   VARCHAR(36)  NOT NULL COMMENT '토큰 계열 ID (UUID; 재발급해도 유지)',
    token_hash  VARCHAR(64)  NOT NULL COMMENT '토큰 해시 (SHA-256 hex)',
    expires_at  DATETIME(6)  NOT NULL COMMENT '만료 시각',
    used_at     DATETIME(6)  NULL     COMMENT '재발급에 쓴 시각 (NULL: 안 씀)',
    revoked_at  DATETIME(6)  NULL     COMMENT '폐기 시각 (NULL: 유효)',
    created_at  DATETIME(6)  NOT NULL,
    updated_at  DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_refresh_token_token_hash (token_hash),
    KEY idx_refresh_token_user_id (user_id),
    KEY idx_refresh_token_family_id (family_id),
    KEY idx_refresh_token_expires_at (expires_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '리프레시 토큰';

CREATE TABLE tb_login_history (
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    user_id       BIGINT        NULL     COMMENT '사용자 ID (NULL: 로그인 실패)',
    login_id      VARCHAR(50)   NOT NULL COMMENT '입력한 로그인 아이디',
    success       BOOLEAN       NOT NULL COMMENT '로그인 성공 여부',
    failure_code  VARCHAR(10)   NULL     COMMENT '실패 오류 코드 (NULL: 성공)',
    ip            VARCHAR(45)   NULL     COMMENT '접속 IP',
    user_agent    VARCHAR(500)  NULL     COMMENT '브라우저 User-Agent',
    device_type   VARCHAR(10)   NOT NULL COMMENT '기기 종류 (PC, MOBILE, TABLET, OTHER)',
    os            VARCHAR(30)   NOT NULL COMMENT '운영체제 이름',
    browser       VARCHAR(30)   NOT NULL COMMENT '브라우저 이름',
    created_at    DATETIME(6)   NOT NULL,
    updated_at    DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    KEY idx_login_history_user_id_created_at (user_id, created_at),
    KEY idx_login_history_login_id_created_at (login_id, created_at),
    KEY idx_login_history_created_at (created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '접속 이력 (로그인 성공·실패)';

CREATE TABLE tb_code_group (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    group_code   VARCHAR(50)   NOT NULL COMMENT '그룹 코드 (만든 뒤 고정)',
    name         VARCHAR(100)  NOT NULL COMMENT '그룹 이름',
    description  VARCHAR(500)  NULL     COMMENT '그룹 설명',
    enabled      BOOLEAN       NOT NULL COMMENT '사용 여부',
    created_at   DATETIME(6)   NOT NULL,
    updated_at   DATETIME(6)   NOT NULL,
    created_by   BIGINT        NOT NULL,
    updated_by   BIGINT        NOT NULL,
    deleted_at   DATETIME(6)   NULL,
    deleted_by   BIGINT        NULL,
    active_key   TINYINT AS (IF(deleted_at IS NULL, 1, NULL)) VIRTUAL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_code_group_group_code_active_key (group_code, active_key)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '공통코드 그룹';

CREATE TABLE tb_code (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    group_code   VARCHAR(50)   NOT NULL COMMENT '소속 그룹 코드',
    code         VARCHAR(50)   NOT NULL COMMENT '항목 코드 (그룹 안 유일)',
    name         VARCHAR(100)  NOT NULL COMMENT '항목 이름',
    sort_order   INT           NOT NULL COMMENT '그룹 안 표시 순서',
    enabled      BOOLEAN       NOT NULL COMMENT '사용 여부',
    created_at   DATETIME(6)   NOT NULL,
    updated_at   DATETIME(6)   NOT NULL,
    created_by   BIGINT        NOT NULL,
    updated_by   BIGINT        NOT NULL,
    deleted_at   DATETIME(6)   NULL,
    deleted_by   BIGINT        NULL,
    active_key   TINYINT AS (IF(deleted_at IS NULL, 1, NULL)) VIRTUAL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_code_group_code_code_active_key (group_code, code, active_key),
    KEY idx_code_group_code_sort_order (group_code, sort_order)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '공통코드 항목';

CREATE TABLE tb_menu (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    parent_id    BIGINT       NULL     COMMENT '상위 메뉴 ID (NULL: 1단계)',
    name         VARCHAR(50)  NOT NULL COMMENT '메뉴 이름',
    program_key  VARCHAR(50)  NULL     COMMENT '연결 화면 프로그램 키 (NULL: 상위 메뉴)',
    icon         VARCHAR(50)  NULL     COMMENT '아이콘 이름',
    sort_order   INT          NOT NULL COMMENT '같은 단계 안 표시 순서',
    enabled      BOOLEAN      NOT NULL COMMENT '사용 여부',
    audience     VARCHAR(10)  NOT NULL COMMENT '메뉴 대상 (ADMIN, USER)',
    visibility   VARCHAR(10)  NULL     COMMENT '공개 범위 (PUBLIC, MEMBER; NULL: 관리 메뉴·상위 메뉴)',
    created_at   DATETIME(6)  NOT NULL,
    updated_at   DATETIME(6)  NOT NULL,
    created_by   BIGINT       NOT NULL,
    updated_by   BIGINT       NOT NULL,
    deleted_at   DATETIME(6)  NULL,
    deleted_by   BIGINT       NULL,
    PRIMARY KEY (id),
    KEY idx_menu_parent_id (parent_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '메뉴 (2단계 트리)';

CREATE TABLE tb_access_role (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    code         VARCHAR(50)   NOT NULL COMMENT '역할 코드 (영문 대문자·숫자·밑줄; 만든 뒤 고정)',
    name         VARCHAR(50)   NOT NULL COMMENT '역할 이름',
    description  VARCHAR(200)  NULL     COMMENT '역할 설명',
    created_at   DATETIME(6)   NOT NULL,
    updated_at   DATETIME(6)   NOT NULL,
    created_by   BIGINT        NOT NULL,
    updated_by   BIGINT        NOT NULL,
    deleted_at   DATETIME(6)   NULL,
    deleted_by   BIGINT        NULL,
    active_key   TINYINT AS (IF(deleted_at IS NULL, 1, NULL)) VIRTUAL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_access_role_code_active_key (code, active_key)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '업무 역할';

CREATE TABLE tb_access_role_permission (
    access_role_id  BIGINT        NOT NULL COMMENT '역할 ID',
    permission      VARCHAR(100)  NOT NULL COMMENT '권한 키',
    PRIMARY KEY (access_role_id, permission)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci
    COMMENT = '역할별 권한 키 (PK: 컬렉션 테이블; 감사 열 없음: 역할과 함께 저장)';

CREATE TABLE tb_user_access_role (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    user_id         BIGINT       NOT NULL COMMENT '사용자 ID',
    access_role_id  BIGINT       NOT NULL COMMENT '역할 ID',
    created_at      DATETIME(6)  NOT NULL,
    updated_at      DATETIME(6)  NOT NULL,
    created_by      BIGINT       NOT NULL,
    updated_by      BIGINT       NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_access_role_user_id_access_role_id (user_id, access_role_id),
    KEY idx_user_access_role_access_role_id (access_role_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '사용자별 업무 역할';

CREATE TABLE tb_rich_text_image (
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    public_id     CHAR(36)      NOT NULL COMMENT '공개 ID (UUID)',
    path          VARCHAR(300)  NOT NULL COMMENT '저장 경로 (image-dir 기준 상대 경로)',
    content_type  VARCHAR(50)   NOT NULL COMMENT '파일 형식 (MIME)',
    size          BIGINT        NOT NULL COMMENT '파일 크기 (바이트)',
    status        VARCHAR(10)   NOT NULL COMMENT '이미지 상태 (TEMP, USED)',
    owner_type    VARCHAR(50)   NULL     COMMENT '사용 글 종류 (NULL: TEMP)',
    owner_id      BIGINT        NULL     COMMENT '사용 글 ID (NULL: TEMP)',
    created_at    DATETIME(6)   NOT NULL,
    updated_at    DATETIME(6)   NOT NULL,
    created_by    BIGINT        NOT NULL,
    updated_by    BIGINT        NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_rich_text_image_public_id (public_id),
    KEY idx_rich_text_image_owner_type_owner_id (owner_type, owner_id),
    KEY idx_rich_text_image_status_updated_at (status, updated_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '본문 이미지';

CREATE TABLE tb_notice (
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    title         VARCHAR(200)  NOT NULL COMMENT '공지 제목',
    content       MEDIUMTEXT    NOT NULL COMMENT '공지 본문 (정제된 HTML)',
    status        VARCHAR(20)   NOT NULL COMMENT '공지 상태 (DRAFT, PUBLISHED)',
    pinned        BOOLEAN       NOT NULL COMMENT '상단 고정 여부',
    published_at  DATETIME(6)   NULL     COMMENT '처음 게시 시각 (NULL: 게시 전)',
    created_at    DATETIME(6)   NOT NULL,
    updated_at    DATETIME(6)   NOT NULL,
    created_by    BIGINT        NOT NULL,
    updated_by    BIGINT        NOT NULL,
    deleted_at    DATETIME(6)   NULL,
    deleted_by    BIGINT        NULL,
    PRIMARY KEY (id),
    KEY idx_notice_status_deleted_at_pinned_published_at (status, deleted_at, pinned, published_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '공지사항';

CREATE TABLE tb_notice_read (
    user_id       BIGINT       NOT NULL COMMENT '사용자 ID',
    last_seen_at  DATETIME(6)  NOT NULL COMMENT '공지 목록 마지막 확인 시각',
    PRIMARY KEY (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci
    COMMENT = '공지 확인 기록 (PK: 1:1 테이블; 감사 열 없음: 사용자당 1행 덮어쓰기)';

CREATE TABLE tb_notification (
    id          BIGINT        NOT NULL AUTO_INCREMENT,
    user_id     BIGINT        NOT NULL COMMENT '받는 사용자 ID',
    type        VARCHAR(30)   NOT NULL COMMENT '알림 종류 (SYSTEM)',
    title       VARCHAR(200)  NOT NULL COMMENT '알림 제목',
    link        VARCHAR(500)  NOT NULL COMMENT '이동 화면 경로 (/로 시작)',
    created_at  DATETIME(6)   NOT NULL,
    read_at     DATETIME(6)   NULL     COMMENT '읽은 시각 (NULL: 안 읽음)',
    PRIMARY KEY (id),
    KEY idx_notification_user_id_created_at (user_id, created_at),
    KEY idx_notification_user_id_read_at (user_id, read_at),
    KEY idx_notification_created_at (created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci
    COMMENT = '개인 알림 (감사 열 없음: 90일 뒤 삭제)';

CREATE TABLE tb_push_subscription (
    id          BIGINT         NOT NULL AUTO_INCREMENT,
    user_id     BIGINT         NOT NULL COMMENT '사용자 ID',
    endpoint    VARCHAR(1000)  NOT NULL COMMENT '푸시 서비스 주소',
    p256dh      VARCHAR(200)   NOT NULL COMMENT '브라우저 공개 키 (base64url)',
    auth        VARCHAR(100)   NOT NULL COMMENT '인증 비밀 (base64url)',
    created_at  DATETIME(6)    NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_push_subscription_endpoint (endpoint(700)),
    KEY idx_push_subscription_user_id (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci
    COMMENT = '웹 푸시 구독 (감사 열 없음: 해제 때 삭제)';
