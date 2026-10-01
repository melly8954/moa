package com.moa.common.exception;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.moa.common.response.ErrorResponse;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 컨트롤러에 닿기 전(필터 등)에 난 오류도 ErrorResponse 형식으로 답한다. 스프링 기본 /error 응답을 대신한다.
 */
@Hidden
@RestController
public class ErrorPageController implements ErrorController {

	@RequestMapping("/error")
	public ResponseEntity<ErrorResponse> error(HttpServletRequest request) {
		Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
		Object uri = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
		ErrorCode errorCode = toErrorCode(status instanceof Integer code ? code : 500);
		String path = uri != null ? uri.toString() : request.getRequestURI();
		return ResponseEntity.status(errorCode.getHttpStatus()).body(ErrorResponse.of(errorCode, path));
	}

	private ErrorCode toErrorCode(int status) {
		return switch (status) {
			case 400 -> ErrorCode.INVALID_INPUT;
			case 401 -> ErrorCode.UNAUTHORIZED;
			case 403 -> ErrorCode.FORBIDDEN;
			case 404 -> ErrorCode.NOT_FOUND;
			case 405 -> ErrorCode.METHOD_NOT_ALLOWED;
			case 415 -> ErrorCode.UNSUPPORTED_MEDIA_TYPE;
			default -> ErrorCode.INTERNAL_ERROR;
		};
	}
}
