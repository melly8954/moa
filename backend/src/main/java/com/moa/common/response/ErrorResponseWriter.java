package com.moa.common.response;

import java.io.IOException;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moa.common.exception.ErrorCode;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * 컨트롤러 밖(필터, 보안 핸들러)에서 오류 응답을 쓴다. 형식은 GlobalExceptionHandler와 같다.
 */
@Component
@RequiredArgsConstructor
public class ErrorResponseWriter {

	private final ObjectMapper objectMapper;

	public void write(HttpServletRequest request, HttpServletResponse response, ErrorCode errorCode)
		throws IOException {
		response.setStatus(errorCode.getHttpStatus().value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");
		objectMapper.writeValue(response.getWriter(), ErrorResponse.of(errorCode, request.getRequestURI()));
	}
}
