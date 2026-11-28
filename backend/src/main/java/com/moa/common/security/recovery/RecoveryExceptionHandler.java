package com.moa.common.security.recovery;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.moa.common.exception.ErrorCode;
import com.moa.common.response.ErrorResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * A401에 복구 쿠키를 붙인다 (specs/018-member-withdrawal research R5). 응답 본문은 키트 GlobalExceptionHandler와 같다.
 * 키트 GlobalExceptionHandler·ApiAuthTokenController를 고치지 않으려고 더 높은 순서의 advice로 둔다.
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
@RequiredArgsConstructor
public class RecoveryExceptionHandler {

	private final RecoveryCookies recoveryCookies;

	@ExceptionHandler(WithdrawnAccountException.class)
	public ResponseEntity<ErrorResponse> handleWithdrawn(WithdrawnAccountException ex, HttpServletRequest request,
		HttpServletResponse response) {
		ErrorCode errorCode = ex.getErrorCode();
		log.info("ServiceException code={} userId={}", errorCode.getCode(), ex.getTicket().userId());
		recoveryCookies.write(response, ex.getTicket());
		return ResponseEntity.status(errorCode.getHttpStatus())
			.body(ErrorResponse.of(errorCode, ex.getMessage(), request.getRequestURI(), ex.getErrors(),
				ex.getDetails()));
	}
}
