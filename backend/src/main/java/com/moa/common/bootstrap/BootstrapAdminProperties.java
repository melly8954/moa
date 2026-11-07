package com.moa.common.bootstrap;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 첫 관리자 설정. 아이디와 비밀번호를 둘 다 주면 AdminBootstrapRunner가 쓴다.
 *
 * @param loginId BOOTSTRAP_ADMIN_LOGIN_ID
 * @param password BOOTSTRAP_ADMIN_PASSWORD (임시 비밀번호. 8자 이상)
 * @param name BOOTSTRAP_ADMIN_NAME (기본 "관리자")
 */
@ConfigurationProperties("app.bootstrap-admin")
public record BootstrapAdminProperties(String loginId, String password, String name) {

	public boolean configured() {
		return loginId != null && !loginId.isBlank() && password != null && !password.isBlank();
	}
}
