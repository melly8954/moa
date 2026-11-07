package com.moa.common.guard;

import java.util.ArrayList;
import java.util.List;

import org.springframework.core.env.Environment;

/**
 * 가드 규칙이 설정값을 읽고 위반을 알리는 창구.
 *
 * <ul>
 *   <li>{@link #fail}: dev·prod 모두 시작을 막는다 (비밀키, 로컬 계정 SQL처럼 어디서든 위험한 것)</li>
 *   <li>{@link #failInProd}: prod는 막고 dev는 경고만 남긴다 (Swagger, 쿠키 secure, CORS처럼 운영 기준)</li>
 * </ul>
 */
public class GuardContext {

	private final String profile;
	private final Environment environment;
	private final List<String> errors = new ArrayList<>();
	private final List<String> warnings = new ArrayList<>();

	GuardContext(String profile, Environment environment) {
		this.profile = profile;
		this.environment = environment;
	}

	public String profile() {
		return profile;
	}

	public boolean isProd() {
		return StartupGuard.PROD.equals(profile);
	}

	/** 값이 없으면 빈 문자열 */
	public String property(String key) {
		return environment.getProperty(key, "").trim();
	}

	public boolean isTrue(String key) {
		return Boolean.parseBoolean(property(key));
	}

	public void fail(String message) {
		errors.add(message);
	}

	public void failInProd(String message) {
		if (isProd()) {
			errors.add(message);
		} else {
			warnings.add(message);
		}
	}

	List<String> errors() {
		return errors;
	}

	List<String> warnings() {
		return warnings;
	}
}
