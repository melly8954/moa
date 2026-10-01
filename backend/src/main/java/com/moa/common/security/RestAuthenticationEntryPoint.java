package com.moa.common.security;

import java.io.IOException;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.moa.common.exception.ErrorCode;
import com.moa.common.response.ErrorResponseWriter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * 인증이 필요한 API를 인증 없이 부르면 401. 토큰 필터가 남긴 코드(A002 만료 등)가 있으면 그 코드로 답한다.
 */
@Component
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

	public static final String ERROR_CODE_ATTRIBUTE = RestAuthenticationEntryPoint.class.getName() + ".errorCode";

	private final ErrorResponseWriter errorResponseWriter;

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
		AuthenticationException authException) throws IOException {
		ErrorCode errorCode = request.getAttribute(ERROR_CODE_ATTRIBUTE) instanceof ErrorCode code
			? code
			: ErrorCode.UNAUTHORIZED;
		errorResponseWriter.write(request, response, errorCode);
	}
}
