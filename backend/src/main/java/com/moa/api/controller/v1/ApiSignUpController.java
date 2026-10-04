package com.moa.api.controller.v1;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.moa.api.application.v1.SignUpApplication;
import com.moa.api.application.v1.command.ConfirmEmailVerificationCommand;
import com.moa.api.application.v1.command.ConfirmPhoneVerificationCommand;
import com.moa.api.application.v1.command.CreateEmailVerificationCommand;
import com.moa.api.application.v1.command.CreatePhoneVerificationCommand;
import com.moa.api.controller.v1.request.EmailConfirmationCreateRequest;
import com.moa.api.controller.v1.request.EmailVerificationCreateRequest;
import com.moa.api.controller.v1.request.PhoneConfirmationCreateRequest;
import com.moa.api.controller.v1.request.PhoneVerificationCreateRequest;
import com.moa.api.controller.v1.response.EmailConfirmationGetResponse;
import com.moa.api.controller.v1.response.EmailVerificationGetResponse;
import com.moa.api.controller.v1.response.PhoneVerificationGetResponse;
import com.moa.api.controller.v1.response.SignUpGetResponse;
import com.moa.api.dto.EmailConfirmationDto;
import com.moa.api.dto.EmailConfirmationResult;
import com.moa.common.response.ErrorResponse;
import com.moa.common.security.token.RefreshTokenTransport;
import com.moa.common.security.token.SignUpTokenTransport;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.PermitAll;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 가입 진행 API. 비회원이 부른다. 가입 진행은 쿠키(sign_up_token)로 이어진다.
 */
@Tag(name = "가입 API", description = "이메일 인증, 휴대폰 인증, 가입 진행 상태")
@PermitAll
@SecurityRequirements
@RestController
@RequestMapping("/api/v1/sign-ups")
@RequiredArgsConstructor
public class ApiSignUpController {

	private final SignUpApplication signUpApplication;
	private final SignUpTokenTransport signUpTokenTransport;
	private final RefreshTokenTransport refreshTokenTransport;

	@Operation(summary = "인증 메일 받기",
		description = "이메일·비밀번호를 받아 인증 메일을 보낸다. 카카오가 이메일을 주지 않아 이어 온 가입이면 쿠키의 가입 진행에 붙는다",
		responses = {
			@ApiResponse(responseCode = "201", description = "보냄", useReturnTypeSchema = true),
			@ApiResponse(responseCode = "400", description = "V001 입력 오류",
				content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode = "401", description = "A006 가입 진행 무효 (쿠키가 있을 때)",
				content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode = "409", description = "B002 비밀번호가 있는 기존 계정의 이메일",
				content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode = "429", description = "B008 60초 안에 다시 보냄",
				content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode = "503", description = "S002 발송 실패",
				content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
		})
	@PostMapping("/email-verifications")
	public ResponseEntity<EmailVerificationGetResponse> createEmailVerification(
		@Valid @RequestBody EmailVerificationCreateRequest request, HttpServletRequest httpRequest) {
		CreateEmailVerificationCommand command = new CreateEmailVerificationCommand(request.email(),
			request.password(), signUpTokenTransport.read(httpRequest));
		return ResponseEntity.status(HttpStatus.CREATED)
			.body(EmailVerificationGetResponse.from(signUpApplication.createEmailVerification(command)));
	}

	@Operation(summary = "이메일 인증 링크 확인",
		description = "CONTINUE_SIGN_UP이면 가입 진행 쿠키를 쓴다. SIGNED_IN이면 기존 계정에 연결하고 리프레시 쿠키와 토큰을 준다",
		responses = {
			@ApiResponse(responseCode = "200", description = "확인", useReturnTypeSchema = true),
			@ApiResponse(responseCode = "400", description = "B004 만료·이미 쓴 링크",
				content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode = "401", description = "A006 이어 온 구글·카카오 가입 진행이 만료됨",
				content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode = "409",
				description = "B002 비밀번호가 있는 기존 계정의 이메일, B016 정지 계정의 이메일, B017 탈퇴 유예 계정의 이메일, "
					+ "B003 기존 계정에 같은 제공자의 다른 계정이 연결됨",
				content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
		})
	@PostMapping("/email-confirmations")
	public ResponseEntity<EmailConfirmationGetResponse> confirmEmailVerification(
		@Valid @RequestBody EmailConfirmationCreateRequest request, HttpServletRequest httpRequest,
		HttpServletResponse httpResponse) {
		EmailConfirmationDto result = signUpApplication.confirmEmailVerification(
			new ConfirmEmailVerificationCommand(request.token()));
		String bodyRefreshToken = null;
		if (result.result() == EmailConfirmationResult.SIGNED_IN) {
			bodyRefreshToken = refreshTokenTransport.write(httpRequest, httpResponse, result.tokens());
			signUpTokenTransport.clear(httpResponse);
		} else {
			signUpTokenTransport.write(httpResponse, result.signUpToken());
		}
		return ResponseEntity.ok(EmailConfirmationGetResponse.from(result, bodyRefreshToken));
	}

	@Operation(summary = "문자 인증 번호 받기",
		responses = {
			@ApiResponse(responseCode = "201", description = "보냄", useReturnTypeSchema = true),
			@ApiResponse(responseCode = "400", description = "V001 입력 오류",
				content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode = "401", description = "A006 가입 진행 없음·만료",
				content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode = "409", description = "B015 계정 이메일이 아직 없음",
				content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode = "429", description = "B008 60초 안에 다시 받음, B009 하루 10회 넘음",
				content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode = "503", description = "S003 발송 실패",
				content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
		})
	@PostMapping("/phone-verifications")
	public ResponseEntity<PhoneVerificationGetResponse> createPhoneVerification(
		@Valid @RequestBody PhoneVerificationCreateRequest request, HttpServletRequest httpRequest) {
		CreatePhoneVerificationCommand command = new CreatePhoneVerificationCommand(
			signUpTokenTransport.read(httpRequest), request.phoneNumber());
		return ResponseEntity.status(HttpStatus.CREATED)
			.body(PhoneVerificationGetResponse.from(signUpApplication.createPhoneVerification(command)));
	}

	@Operation(summary = "문자 인증 번호 확인",
		description = "인증 번호와 생년월일을 확인한다. 번호가 기존 계정과 겹치면 409이고, 안내 정보는 가입 진행 상태 조회로 받는다",
		responses = {
			@ApiResponse(responseCode = "204", description = "확인"),
			@ApiResponse(responseCode = "400",
				description = "V001 입력 오류, B005 번호 틀림, B006 만료, B007 입력 횟수 넘음, B010 만 14세 미만",
				content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode = "401", description = "A006 가입 진행 없음·만료",
				content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode = "409",
				description = "B011 활동 중 계정과 겹침, B012 정지 계정과 겹침, B013 탈퇴 유예 계정과 겹침, B014 재가입 제한",
				content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
		})
	@PostMapping("/phone-confirmations")
	public ResponseEntity<Void> confirmPhoneVerification(@Valid @RequestBody PhoneConfirmationCreateRequest request,
		HttpServletRequest httpRequest) {
		signUpApplication.confirmPhoneVerification(new ConfirmPhoneVerificationCommand(
			signUpTokenTransport.read(httpRequest), request.verificationCode(), request.birthDate()));
		return ResponseEntity.noContent().build();
	}

	@Operation(summary = "가입 진행 상태",
		responses = {
			@ApiResponse(responseCode = "200", description = "조회", useReturnTypeSchema = true),
			@ApiResponse(responseCode = "401", description = "A006 가입 진행 없음·만료",
				content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
		})
	@GetMapping("/current")
	public ResponseEntity<SignUpGetResponse> getSignUp(HttpServletRequest httpRequest) {
		return ResponseEntity.ok(
			SignUpGetResponse.from(signUpApplication.getSignUp(signUpTokenTransport.read(httpRequest))));
	}

	@Operation(summary = "가입 취소", description = "가입 진행을 지운다. 회원은 생기지 않는다. 가입 진행이 없어도 성공한다",
		responses = @ApiResponse(responseCode = "204", description = "취소"))
	@DeleteMapping("/current")
	public ResponseEntity<Void> cancelSignUp(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
		signUpApplication.cancelSignUp(signUpTokenTransport.read(httpRequest));
		signUpTokenTransport.clear(httpResponse);
		return ResponseEntity.noContent().build();
	}
}
