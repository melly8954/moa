package com.moa.common.exception;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.moa.common.response.ErrorResponse;
import com.moa.common.response.ErrorResponse.FieldError;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * 모든 예외를 ErrorResponse 형식으로 바꾼다. 스프링 기본 오류 형식(ProblemDetail 등)은 내보내지 않는다.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(ServiceException.class)
	public ResponseEntity<ErrorResponse> handleService(ServiceException ex, HttpServletRequest request) {
		ErrorCode errorCode = ex.getErrorCode();
		if (errorCode.getHttpStatus().is5xxServerError()) {
			log.error("ServiceException code={}", errorCode.getCode(), ex);
		} else {
			log.info("ServiceException code={} message={}", errorCode.getCode(), ex.getMessage());
		}
		return respond(errorCode, ex.getMessage(), request, ex.getErrors());
	}

	/** @Valid 본문·폼 바인딩 실패 (MethodArgumentNotValidException 포함) */
	@ExceptionHandler(BindException.class)
	public ResponseEntity<ErrorResponse> handleBind(BindException ex, HttpServletRequest request) {
		List<FieldError> errors = ex.getFieldErrors().stream()
			.map(error -> new FieldError(error.getField(), error.getDefaultMessage()))
			.toList();
		return respond(ErrorCode.INVALID_INPUT, ErrorCode.INVALID_INPUT.getMessage(), request, errors);
	}

	/** 메서드 파라미터(@RequestParam, @PathVariable 등) 검증 실패 */
	@ExceptionHandler(HandlerMethodValidationException.class)
	public ResponseEntity<ErrorResponse> handleMethodValidation(HandlerMethodValidationException ex,
		HttpServletRequest request) {
		List<FieldError> errors = ex.getParameterValidationResults().stream()
			.flatMap(result -> result.getResolvableErrors().stream()
				.map(error -> new FieldError(result.getMethodParameter().getParameterName(),
					error.getDefaultMessage())))
			.toList();
		return respond(ErrorCode.INVALID_INPUT, ErrorCode.INVALID_INPUT.getMessage(), request, errors);
	}

	@ExceptionHandler({HttpMessageNotReadableException.class, MissingServletRequestParameterException.class,
		MethodArgumentTypeMismatchException.class})
	public ResponseEntity<ErrorResponse> handleBadRequest(Exception ex, HttpServletRequest request) {
		log.info("잘못된 요청: {}", ex.getMessage());
		return respond(ErrorCode.INVALID_INPUT, ErrorCode.INVALID_INPUT.getMessage(), request, List.of());
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ResponseEntity<ErrorResponse> handleMethodNotAllowed(HttpServletRequest request) {
		return respond(ErrorCode.METHOD_NOT_ALLOWED, ErrorCode.METHOD_NOT_ALLOWED.getMessage(), request, List.of());
	}

	@ExceptionHandler(HttpMediaTypeNotSupportedException.class)
	public ResponseEntity<ErrorResponse> handleMediaType(HttpServletRequest request) {
		return respond(ErrorCode.UNSUPPORTED_MEDIA_TYPE, ErrorCode.UNSUPPORTED_MEDIA_TYPE.getMessage(), request,
			List.of());
	}

	@ExceptionHandler(NoResourceFoundException.class)
	public ResponseEntity<ErrorResponse> handleNoResource(HttpServletRequest request) {
		return respond(ErrorCode.NOT_FOUND, ErrorCode.NOT_FOUND.getMessage(), request, List.of());
	}

	/** 메서드 보안(@PreAuthorize 등)에서 난 예외. 필터에서 난 것은 SecurityConfig의 핸들러가 처리한다 */
	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<ErrorResponse> handleAccessDenied(HttpServletRequest request) {
		return respond(ErrorCode.FORBIDDEN, ErrorCode.FORBIDDEN.getMessage(), request, List.of());
	}

	@ExceptionHandler(AuthenticationException.class)
	public ResponseEntity<ErrorResponse> handleAuthentication(HttpServletRequest request) {
		return respond(ErrorCode.UNAUTHORIZED, ErrorCode.UNAUTHORIZED.getMessage(), request, List.of());
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleUnknown(Exception ex, HttpServletRequest request) {
		log.error("처리하지 못한 예외", ex);
		return respond(ErrorCode.INTERNAL_ERROR, ErrorCode.INTERNAL_ERROR.getMessage(), request, List.of());
	}

	private ResponseEntity<ErrorResponse> respond(ErrorCode errorCode, String message, HttpServletRequest request,
		List<FieldError> errors) {
		return ResponseEntity.status(errorCode.getHttpStatus())
			.body(ErrorResponse.of(errorCode, message, request.getRequestURI(), errors));
	}
}
