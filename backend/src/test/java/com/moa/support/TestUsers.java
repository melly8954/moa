package com.moa.support;

import java.util.List;

import org.springframework.boot.flyway.autoconfigure.FlywayMigrationInitializer;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.moa.api.entity.role.AccessRoles;
import com.moa.common.security.Role;

/**
 * AC 테스트용 사용자를 만든다. DatabaseCleaner가 테스트마다 테이블을 비우므로 필요한 테스트 안에서 만든다.
 *
 * moa (specs/003-admin-roles research R21): 같은 ID가 이미 있으면 그대로 둔다. AcceptanceTest.superAdmin()이 먼저 만든
 * 계정(2, admin)을 테스트가 다시 만들어도 실패하지 않게 하려는 것이다. 역할 행과 구성원도 여기서 만든다
 * moa (research R25): 최고 관리자 역할은 마이그레이션 직후의 ID로 다시 만든다. V8의 생성 열 super_slot이 그 ID를 박았기
 * 때문이다. 다른 ID로 만들면 엉뚱한 역할이 한 명만 받게 된다. FlywayMigrationInitializer를 받아 마이그레이션 뒤에 읽는다 (MenuSeed와 같다)
 */
@Component
public class TestUsers {

	private final JdbcTemplate jdbcTemplate;
	private final PasswordEncoder passwordEncoder;
	private final long superAdminRoleId;

	public TestUsers(JdbcTemplate jdbcTemplate, PasswordEncoder passwordEncoder, FlywayMigrationInitializer migrated) {
		this.jdbcTemplate = jdbcTemplate;
		this.passwordEncoder = passwordEncoder;
		this.superAdminRoleId = jdbcTemplate.queryForObject(
			"select id from tb_access_role where code = ? and deleted_at is null", Long.class,
			AccessRoles.SUPER_ADMIN_CODE);
	}

	public void create(Long id, String loginId, String rawPassword, Role role) {
		jdbcTemplate.update("insert ignore into tb_user (id, login_id, password, name, role, password_change_required, "
			+ "created_at, updated_at, created_by, updated_by) values (?, ?, ?, ?, ?, false, now(6), now(6), 1, 1)",
			id, loginId, rawPassword == null ? null : passwordEncoder.encode(rawPassword), loginId, role.name());
	}

	/** moa: 최고 관리자 역할 행 (V7이 넣지만 테스트마다 표를 비운다). 역할 ID */
	public long superAdminRole() {
		List<Long> found = jdbcTemplate.queryForList(
			"select id from tb_access_role where code = ? and deleted_at is null", Long.class,
			AccessRoles.SUPER_ADMIN_CODE);
		if (!found.isEmpty()) {
			return found.getFirst();
		}
		jdbcTemplate.update(
			"insert into tb_access_role (id, code, name, description, created_at, updated_at, created_by, "
				+ "updated_by) values (?, ?, ?, ?, now(6), now(6), 1, 1)",
			superAdminRoleId, AccessRoles.SUPER_ADMIN_CODE,
			AccessRoles.SUPER_ADMIN_NAME, AccessRoles.SUPER_ADMIN_DESCRIPTION);
		return superAdminRole();
	}

	/** moa: 이 계정을 최고 관리자 역할에 넣는다 */
	public void makeSuperAdmin(Long userId) {
		addRole(userId, superAdminRole());
	}

	/** moa: 이 권한들을 묶은 역할을 만들어 이 계정에 준다. 역할 ID */
	public long grant(Long userId, String code, String... permissions) {
		jdbcTemplate.update("insert into tb_access_role (code, name, description, created_at, updated_at, created_by, "
			+ "updated_by) values (?, ?, null, now(6), now(6), 1, 1)", code, code);
		Long roleId = jdbcTemplate.queryForObject(
			"select id from tb_access_role where code = ? and deleted_at is null", Long.class, code);
		for (String permission : permissions) {
			jdbcTemplate.update("insert into tb_access_role_permission (access_role_id, permission) values (?, ?)",
				roleId, permission);
		}
		addRole(userId, roleId);
		return roleId;
	}

	private void addRole(Long userId, long roleId) {
		jdbcTemplate.update("insert ignore into tb_user_access_role (user_id, access_role_id, created_at, updated_at, "
			+ "created_by, updated_by) values (?, ?, now(6), now(6), 1, 1)", userId, roleId);
	}
}
