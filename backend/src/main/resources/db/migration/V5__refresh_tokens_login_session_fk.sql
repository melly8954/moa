-- 리프레시 토큰이 속한 로그인 세션 FK. 세션을 지우면(예약 작업) 그 세션의 토큰도 함께 지운다
ALTER TABLE refresh_tokens
    ADD CONSTRAINT fk_refresh_tokens_login_sessions FOREIGN KEY (login_session_id) REFERENCES login_sessions (id)
        ON DELETE CASCADE;
