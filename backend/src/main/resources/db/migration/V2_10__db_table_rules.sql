-- moa: 테이블·인덱스를 DB 테이블 규칙(docs/db.md)에 맞춘다. 동작은 바뀌지 않는다.
-- 테이블은 tb_ + 단수, 인덱스는 idx_·uk_ + tb_를 뺀 테이블 + 줄이지 않은 열, 테이블마다 COMMENT.
-- 키트 V1_x 파일은 고치지 않는다. 같은 이름은 키트로도 옮긴다
RENAME TABLE users TO tb_user,
    refresh_tokens TO tb_refresh_token,
    login_histories TO tb_login_history,
    code_groups TO tb_code_group,
    codes TO tb_code,
    menus TO tb_menu,
    access_roles TO tb_access_role,
    access_role_permissions TO tb_access_role_permission,
    user_access_roles TO tb_user_access_role,
    rich_text_images TO tb_rich_text_image,
    notices TO tb_notice,
    notice_reads TO tb_notice_read,
    notifications TO tb_notification,
    push_subscriptions TO tb_push_subscription;

ALTER TABLE tb_user
    RENAME INDEX uk_users_login_id_active TO uk_user_login_id_active_key,
    COMMENT = '사용자 (회원·로그인 계정)';

ALTER TABLE tb_refresh_token
    RENAME INDEX uk_refresh_tokens_token_hash TO uk_refresh_token_token_hash,
    RENAME INDEX idx_refresh_tokens_user_id TO idx_refresh_token_user_id,
    RENAME INDEX idx_refresh_tokens_family_id TO idx_refresh_token_family_id,
    RENAME INDEX idx_refresh_tokens_expires_at TO idx_refresh_token_expires_at,
    COMMENT = '리프레시 토큰 (해시 저장, 재발급·로그아웃)';

ALTER TABLE tb_login_history
    RENAME INDEX idx_login_histories_user_id_created_at TO idx_login_history_user_id_created_at,
    RENAME INDEX idx_login_histories_login_id_created_at TO idx_login_history_login_id_created_at,
    RENAME INDEX idx_login_histories_created_at TO idx_login_history_created_at,
    COMMENT = '접속 이력 (로그인 성공·실패)';

ALTER TABLE tb_code_group
    RENAME INDEX uk_code_groups_group_code_active TO uk_code_group_group_code_active_key,
    COMMENT = '공통코드 그룹';

ALTER TABLE tb_code
    RENAME INDEX uk_codes_group_code_code_active TO uk_code_group_code_code_active_key,
    RENAME INDEX idx_codes_group_code_sort TO idx_code_group_code_sort_order,
    COMMENT = '공통코드 항목';

ALTER TABLE tb_menu
    RENAME INDEX idx_menus_parent_id TO idx_menu_parent_id,
    COMMENT = '메뉴 (관리·사용자 셸, 2단계 트리)';

ALTER TABLE tb_access_role
    RENAME INDEX uk_access_roles_code_active TO uk_access_role_code_active_key,
    COMMENT = '업무 역할';

ALTER TABLE tb_access_role_permission
    COMMENT = '역할에 묶은 권한 키';

ALTER TABLE tb_user_access_role
    RENAME INDEX uk_user_access_roles TO uk_user_access_role_user_id_access_role_id,
    RENAME INDEX idx_user_access_roles_role TO idx_user_access_role_access_role_id,
    COMMENT = '사용자에게 준 업무 역할';

ALTER TABLE tb_rich_text_image
    RENAME INDEX uk_rich_text_images_public_id TO uk_rich_text_image_public_id,
    RENAME INDEX idx_rich_text_images_owner TO idx_rich_text_image_owner_type_owner_id,
    RENAME INDEX idx_rich_text_images_status_updated TO idx_rich_text_image_status_updated_at,
    COMMENT = '본문 이미지 (임시·사용)';

-- 공지 고정 여부는 엔터티가 늘 값을 준다. CREATE TABLE에 둔 DEFAULT를 뺀다
ALTER TABLE tb_notice
    RENAME INDEX idx_notices_public TO idx_notice_status_deleted_at_pinned_published_at,
    ALTER COLUMN pinned DROP DEFAULT,
    COMMENT = '공지사항';

ALTER TABLE tb_notice_read
    COMMENT = '사용자가 공지 목록을 마지막으로 연 시각 (새 공지 표시)';

ALTER TABLE tb_notification
    RENAME INDEX idx_notifications_user_created TO idx_notification_user_id_created_at,
    RENAME INDEX idx_notifications_user_read TO idx_notification_user_id_read_at,
    RENAME INDEX idx_notifications_created TO idx_notification_created_at,
    COMMENT = '개인 알림. 쓰고 버리는 행이라 감사 열이 없다. 90일 지나면 지운다';

ALTER TABLE tb_push_subscription
    RENAME INDEX uk_push_subscriptions_endpoint TO uk_push_subscription_endpoint,
    RENAME INDEX idx_push_subscriptions_user TO idx_push_subscription_user_id,
    COMMENT = '브라우저 알림(웹 푸시) 구독. 쓰고 버리는 행이라 감사 열이 없다';
