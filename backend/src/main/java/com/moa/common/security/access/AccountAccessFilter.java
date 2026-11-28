package com.moa.common.security.access;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.moa.common.exception.ErrorCode;
import com.moa.common.security.AuthPrincipal;
import com.moa.common.security.RestAuthenticationEntryPoint;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * 막힌 계정의 요청을 로그인하지 않은 것으로 본다 (moa 018, specs/018-member-withdrawal FR-006, research R2).
 * 로그인이 필요한 API는 A002로 답한다. 프론트가 재발급(A003으로 실패)을 거쳐 로그인 화면으로 간다.
 */
@RequiredArgsConstructor
public class AccountAccessFilter extends OncePerRequestFilter {

	private final AccountAccessChecker accountAccessChecker;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
		throws ServletException, IOException {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication != null && authentication.getPrincipal() instanceof AuthPrincipal principal
			&& accountAccessChecker.isBlocked(principal.userId())) {
			SecurityContextHolder.clearContext();
			request.setAttribute(RestAuthenticationEntryPoint.ERROR_CODE_ATTRIBUTE, ErrorCode.ACCESS_TOKEN_EXPIRED);
		}
		chain.doFilter(request, response);
	}
}
