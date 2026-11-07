package com.moa.common.guard;

/**
 * 골격 기본 규칙 (dev·prod).
 */
public class CoreGuardRule implements GuardRule {

	@Override
	public void check(GuardContext context) {
		String secret = context.property("app.jwt.secret");
		if (secret.isEmpty() || secret.contains("local-only") || secret.contains("test-only")) {
			context.fail("JWT_SECRET이 비었거나 로컬·테스트 기본값입니다. 32바이트 이상의 새 값을 줍니다");
		}
		if (context.property("spring.flyway.locations").contains("localdata")) {
			context.fail("Flyway가 로컬 개발 계정 SQL(db/localdata)을 읽습니다. 서버 DB에 admin/user 계정이 들어갑니다");
		}
		if (context.isTrue("spring.docker.compose.enabled")) {
			context.fail("docker compose 자동 실행이 켜져 있습니다 (local 전용)");
		}
		if (context.isTrue("springdoc.swagger-ui.enabled")) {
			context.failInProd("Swagger가 켜져 있습니다 (SWAGGER_ENABLED=false)");
		}
		if (!context.isTrue("app.auth.refresh-cookie.secure")) {
			context.failInProd("리프레시 쿠키 secure가 꺼져 있습니다 (AUTH_COOKIE_SECURE=true, HTTPS 필요)");
		}
		String origins = context.property("app.cors.allowed-origins");
		if (origins.contains("localhost") || origins.contains("127.0.0.1") || origins.contains("*")) {
			context.failInProd("CORS 허용 출처에 localhost나 *가 있습니다: " + origins);
		}
	}
}
