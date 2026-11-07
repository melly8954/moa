package com.moa.common.config;

import java.util.List;
import java.util.stream.Stream;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.annotation.AnnotationTemplateExpressionDefaults;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import com.moa.common.logging.TraceIdFilter;
import com.moa.common.security.HttpSecurityCustomizer;
import com.moa.common.security.PermitAllRequestMatcher;
import com.moa.common.security.PublicPaths;
import com.moa.common.security.RestAccessDeniedHandler;
import com.moa.common.security.RestAuthenticationEntryPoint;
import com.moa.common.security.Role;
import com.moa.common.security.jwt.JwtAuthenticationFilter;
import com.moa.common.security.jwt.JwtTokenProvider;

/**
 * 기본은 모든 API가 인증을 요구한다. 로그인 없이 부를 API는 컨트롤러에 {@code @PermitAll}을 붙인다.
 * 권한이 필요한 API는 {@code @RequirePermission(키)}. ADMIN은 모든 권한을 갖는다. 역할 계층은 ADMIN > USER다.
 * 세션을 쓰지 않는다(stateless). 리프레시 쿠키는 SameSite=Strict로 교차 사이트 요청을 막는다.
 */
@Configuration
@EnableMethodSecurity(jsr250Enabled = true)
public class SecurityConfig {

	private static final String[] PUBLIC_PATHS = {
		"/error", "/actuator/health", "/swagger-ui/**", "/v3/api-docs/**"
	};

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtTokenProvider tokenProvider,
		RestAuthenticationEntryPoint authenticationEntryPoint, RestAccessDeniedHandler accessDeniedHandler,
		@Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping handlerMapping,
		ObjectProvider<PublicPaths> extraPublicPaths, ObjectProvider<HttpSecurityCustomizer> customizers)
		throws Exception {
		// 헬퍼 등이 PublicPaths 빈으로 더한 경로 (WebSocket 핸드셰이크 등). 없으면 PUBLIC_PATHS만 연다
		String[] publicPaths = Stream.concat(Stream.of(PUBLIC_PATHS),
			extraPublicPaths.orderedStream().flatMap(paths -> paths.paths().stream())).toArray(String[]::new);
		http
			.csrf(AbstractHttpConfigurer::disable)
			.cors(Customizer.withDefaults())
			.formLogin(AbstractHttpConfigurer::disable)
			.httpBasic(AbstractHttpConfigurer::disable)
			.logout(AbstractHttpConfigurer::disable)
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.authorizeHttpRequests(authorize -> authorize
				.requestMatchers(publicPaths).permitAll()
				.requestMatchers(new PermitAllRequestMatcher(handlerMapping)).permitAll()
				.anyRequest().authenticated())
			.exceptionHandling(exceptions -> exceptions
				.authenticationEntryPoint(authenticationEntryPoint)
				.accessDeniedHandler(accessDeniedHandler))
			.addFilterBefore(new JwtAuthenticationFilter(tokenProvider), UsernamePasswordAuthenticationFilter.class);
		// 헬퍼가 더하는 설정 (OAuth 로그인 등)
		for (HttpSecurityCustomizer customizer : customizers.orderedStream().toList()) {
			customizer.customize(http);
		}
		return http.build();
	}

	/** ADMIN은 USER 역할을 포함한다. URL 규칙과 @PreAuthorize("hasRole(...)")에 적용된다 */
	@Bean
	public static RoleHierarchy roleHierarchy() {
		return RoleHierarchyImpl.withDefaultRolePrefix()
			.role(Role.ADMIN.name()).implies(Role.USER.name())
			.build();
	}

	/** @RequirePermission의 '{value}'를 어노테이션 값으로 채운다 */
	@Bean
	public static AnnotationTemplateExpressionDefaults annotationTemplateExpressionDefaults() {
		return new AnnotationTemplateExpressionDefaults();
	}

	/** 저장 형식은 {bcrypt}$2a$... 다. 알고리즘을 바꿔도 기존 해시를 읽는다 */
	@Bean
	public PasswordEncoder passwordEncoder() {
		return PasswordEncoderFactories.createDelegatingPasswordEncoder();
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource(CorsProperties corsProperties) {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(corsProperties.allowedOrigins());
		configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		configuration.setAllowedHeaders(List.of("*"));
		configuration.setExposedHeaders(List.of(HttpHeaders.LOCATION, TraceIdFilter.HEADER));
		configuration.setAllowCredentials(true);
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}
}
