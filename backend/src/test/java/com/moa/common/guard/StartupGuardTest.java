package com.moa.common.guard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

/**
 * 시작 가드. 스프링을 띄우지 않고 설정값만으로 확인한다.
 */
class StartupGuardTest {

	private final StartupGuard guard = new StartupGuard(destination -> destination.get());

	@Test
	@DisplayName("가드: 프로필이 없거나 둘 이상이면 막는다 (기본 프로필이 없다)")
	void requiresOneProfile() {
		assertThatThrownBy(() -> guard.postProcessEnvironment(environment(Map.of()), new SpringApplication()))
			.hasMessageContaining("SPRING_PROFILES_ACTIVE");
		assertThatThrownBy(() -> guard.postProcessEnvironment(environment(Map.of(), "dev", "prod"),
			new SpringApplication())).hasMessageContaining("SPRING_PROFILES_ACTIVE");
	}

	@Test
	@DisplayName("가드: local은 검사하지 않는다")
	void localSkips() {
		assertThatCode(() -> guard.postProcessEnvironment(environment(Map.of("app.jwt.secret", ""), "local"),
			new SpringApplication())).doesNotThrowAnyException();
	}

	@Test
	@DisplayName("가드: prod에서 위반을 모두 모아 한 번에 알리고 시작을 막는다")
	void prodCollectsAll() {
		Map<String, Object> bad = Map.of("app.jwt.secret", "local-only-secret-change-me-0123456789abcdef",
			"springdoc.swagger-ui.enabled", "true", "app.cors.allowed-origins", "http://localhost:3000");

		assertThatThrownBy(() -> guard.postProcessEnvironment(environment(bad, "prod"), new SpringApplication()))
			.hasMessageContaining("시작할 수 없습니다 (prod)")
			.hasMessageContaining("JWT_SECRET")
			.hasMessageContaining("Swagger")
			.hasMessageContaining("secure")
			.hasMessageContaining("CORS");
	}

	@Test
	@DisplayName("가드: dev는 비밀키·로컬 계정 SQL만 막고, 운영 기준(Swagger, secure, CORS)은 경고로 둔다")
	void devWarnsOnly() {
		GuardContext context = check("dev", Map.of("app.jwt.secret", "a-real-secret-0123456789abcdef0123456789",
			"springdoc.swagger-ui.enabled", "true", "app.cors.allowed-origins", "http://localhost:3000"));
		GuardContext localData = check("dev", Map.of("app.jwt.secret", "a-real-secret-0123456789abcdef0123456789",
			"spring.flyway.locations", "classpath:db/migration,classpath:db/localdata"));

		assertThat(context.errors()).isEmpty();
		assertThat(context.warnings()).hasSize(3);
		assertThat(localData.errors()).singleElement().asString().contains("localdata");
	}

	@Test
	@DisplayName("가드: 운영 기준을 지킨 prod 설정은 골격 규칙을 통과한다")
	void prodOk() {
		GuardContext context = check("prod", Map.of("app.jwt.secret", "a-real-secret-0123456789abcdef0123456789",
			"app.auth.refresh-cookie.secure", "true", "app.cors.allowed-origins", "https://shop.example.com"));

		assertThat(context.errors()).isEmpty();
		assertThat(context.warnings()).isEmpty();
	}

	private GuardContext check(String profile, Map<String, Object> properties) {
		GuardContext context = new GuardContext(profile, environment(properties, profile));
		new CoreGuardRule().check(context);
		return context;
	}

	private StandardEnvironment environment(Map<String, Object> properties, String... profiles) {
		StandardEnvironment environment = new StandardEnvironment();
		environment.getPropertySources().addFirst(new MapPropertySource("test", new HashMap<>(properties)));
		environment.setActiveProfiles(profiles);
		return environment;
	}
}
