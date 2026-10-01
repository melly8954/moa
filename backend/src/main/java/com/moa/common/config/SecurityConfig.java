package com.moa.common.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import com.moa.common.logging.TraceIdFilter;
import com.moa.common.security.PermitAllRequestMatcher;
import com.moa.common.security.RestAccessDeniedHandler;
import com.moa.common.security.RestAuthenticationEntryPoint;
import com.moa.common.security.jwt.JwtAuthenticationFilter;
import com.moa.common.security.jwt.JwtTokenProvider;

/**
 * 기본은 모든 API가 인증을 요구한다. 로그인 없이 부를 API는 컨트롤러에 {@code @PermitAll}을 붙인다.
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
		@Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping handlerMapping) throws Exception {
		return http
			.csrf(AbstractHttpConfigurer::disable)
			.cors(Customizer.withDefaults())
			.formLogin(AbstractHttpConfigurer::disable)
			.httpBasic(AbstractHttpConfigurer::disable)
			.logout(AbstractHttpConfigurer::disable)
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.authorizeHttpRequests(authorize -> authorize
				.requestMatchers(PUBLIC_PATHS).permitAll()
				.requestMatchers(new PermitAllRequestMatcher(handlerMapping)).permitAll()
				.anyRequest().authenticated())
			.exceptionHandling(exceptions -> exceptions
				.authenticationEntryPoint(authenticationEntryPoint)
				.accessDeniedHandler(accessDeniedHandler))
			.addFilterBefore(new JwtAuthenticationFilter(tokenProvider), UsernamePasswordAuthenticationFilter.class)
			.build();
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
