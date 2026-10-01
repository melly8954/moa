package com.moa.common.security.jwt;

import java.io.IOException;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.moa.common.exception.ServiceException;
import com.moa.common.security.AuthPrincipal;
import com.moa.common.security.RestAuthenticationEntryPoint;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * Authorization: Bearer 토큰을 읽어 인증 주체를 세운다.
 * 토큰이 무효하면 인증 없이 진행하고, 인증이 필요한 API라면 EntryPoint가 이 오류 코드로 응답한다.
 */
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private static final String BEARER = "Bearer ";

	private final JwtTokenProvider tokenProvider;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
		throws ServletException, IOException {
		String header = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (header != null && header.startsWith(BEARER)) {
			authenticate(header.substring(BEARER.length()), request);
		}
		chain.doFilter(request, response);
	}

	private void authenticate(String token, HttpServletRequest request) {
		try {
			AuthPrincipal principal = tokenProvider.parse(token);
			List<SimpleGrantedAuthority> authorities = principal.roles().stream()
				.map(role -> new SimpleGrantedAuthority("ROLE_" + role))
				.toList();
			SecurityContextHolder.getContext()
				.setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, authorities));
		} catch (ServiceException ex) {
			request.setAttribute(RestAuthenticationEntryPoint.ERROR_CODE_ATTRIBUTE, ex.getErrorCode());
		}
	}
}
