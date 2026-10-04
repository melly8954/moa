package com.moa.api.controller.v1;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.moa.api.application.v1.MemberApplication;
import com.moa.api.application.v1.command.CreateMemberCommand;
import com.moa.api.controller.v1.request.MemberCreateRequest;
import com.moa.api.controller.v1.response.AuthRefreshResponse;
import com.moa.common.response.ErrorResponse;
import com.moa.common.security.token.IssuedTokens;
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
 * 회원 API.
 */
@Tag(name = "회원 API", description = "가입 완료")
@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class ApiMemberController {

	private final MemberApplication memberApplication;
	private final SignUpTokenTransport signUpTokenTransport;
	private final RefreshTokenTransport refreshTokenTransport;

	@Operation(summary = "가입 완료",
		description = "쿠키의 가입 진행으로 ACTIVE 회원을 만들고 로그인시킨다. 리프레시 토큰은 쿠키로 주고 가입 진행 쿠키는 지운다",
		responses = {
			@ApiResponse(responseCode = "201", description = "가입·로그인", useReturnTypeSchema = true),
			@ApiResponse(responseCode = "400", description = "V001 입력 오류(닉네임 규칙, 필수 동의), B010 만 14세 미만",
				content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode = "401", description = "A006 가입 진행 없음·만료",
				content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode = "409",
				description = "B015 인증 미완료, B011~B014 휴대폰 번호 겹침·재가입 제한, B002 이메일 이미 가입(이메일 가입), "
					+ "B003 이메일 이미 가입·제공자 계정 이미 연결(구글·카카오 가입), R003 닉네임 중복",
				content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
		})
	@PermitAll
	@SecurityRequirements
	@PostMapping
	public ResponseEntity<AuthRefreshResponse> createMember(@Valid @RequestBody MemberCreateRequest request,
		HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
		IssuedTokens tokens = memberApplication.createMember(new CreateMemberCommand(
			signUpTokenTransport.read(httpRequest), request.nickname(), request.termsAgreed(),
			request.privacyAgreed()));
		String bodyRefreshToken = refreshTokenTransport.write(httpRequest, httpResponse, tokens);
		signUpTokenTransport.clear(httpResponse);
		return ResponseEntity.status(HttpStatus.CREATED).body(AuthRefreshResponse.from(tokens, bodyRefreshToken));
	}
}
