-- 역할·권한 (선택 헬퍼 rbac). 업무 역할을 DB에서 만들고 권한 키를 묶어 사용자에게 준다
-- 시스템 역할(users.role: USER·ADMIN)과 별개다. ADMIN은 이 표와 관계없이 모든 권한을 갖는다
CREATE TABLE access_roles (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    code         VARCHAR(50)   NOT NULL COMMENT '영문 대문자·숫자·밑줄 (예: MANAGER). 만든 뒤 바꾸지 않는다',
    name         VARCHAR(50)   NOT NULL,
    description  VARCHAR(200)  NULL,
    created_at   DATETIME(6)   NOT NULL,
    updated_at   DATETIME(6)   NOT NULL,
    created_by   BIGINT        NOT NULL,
    updated_by   BIGINT        NOT NULL,
    deleted_at   DATETIME(6)   NULL,
    deleted_by   BIGINT        NULL,
    -- 활성 행에만 값이 있어 지운 역할과 같은 코드로 다시 만들 수 있다
    active_key   TINYINT AS (IF(deleted_at IS NULL, 1, NULL)) VIRTUAL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_access_roles_code_active (code, active_key)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- 역할에 묶은 권한 키 (코드의 PermissionSource로 등록한 키)
CREATE TABLE access_role_permissions (
    access_role_id  BIGINT        NOT NULL,
    permission      VARCHAR(100)  NOT NULL,
    PRIMARY KEY (access_role_id, permission)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- 사용자에게 준 역할. 한 사용자에 여러 역할을 줄 수 있고, 권한은 합집합이다
CREATE TABLE user_access_roles (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    user_id         BIGINT       NOT NULL,
    access_role_id  BIGINT       NOT NULL,
    created_at      DATETIME(6)  NOT NULL,
    updated_at      DATETIME(6)  NOT NULL,
    created_by      BIGINT       NOT NULL,
    updated_by      BIGINT       NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_access_roles (user_id, access_role_id),
    KEY idx_user_access_roles_role (access_role_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
