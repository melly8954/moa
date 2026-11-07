-- 사용자 계정 상태 (골격). 잠금과 비밀번호 변경 요구. 관리 API는 user-admin 헬퍼가 준다
ALTER TABLE users
    ADD COLUMN locked_at                DATETIME(6)  NULL     COMMENT '잠근 시각. 값이 있으면 로그인·토큰 재발급이 막힌다' AFTER role,
    ADD COLUMN password_change_required BOOLEAN      NOT NULL DEFAULT FALSE
        COMMENT '임시 비밀번호를 받았다. 화면이 비밀번호 변경으로 보낸다' AFTER locked_at;
