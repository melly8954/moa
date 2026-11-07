package com.moa.support;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.moa.common.security.Role;

import lombok.RequiredArgsConstructor;

/**
 * AC 테스트용 사용자를 만든다. DatabaseCleaner가 테스트마다 테이블을 비우므로 필요한 테스트 안에서 만든다.
 */
@Component
@RequiredArgsConstructor
public class TestUsers {

	private final JdbcTemplate jdbcTemplate;
	private final PasswordEncoder passwordEncoder;

	public void create(Long id, String loginId, String rawPassword, Role role) {
		jdbcTemplate.update("insert into users (id, login_id, password, name, role, created_at, updated_at, "
			+ "created_by, updated_by) values (?, ?, ?, ?, ?, now(6), now(6), 1, 1)",
			id, loginId, rawPassword == null ? null : passwordEncoder.encode(rawPassword), loginId, role.name());
	}
}
