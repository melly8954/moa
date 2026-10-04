package com.moa.api.controller.v1;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.moa.api.application.v1.OAuthApplication;
import com.moa.api.application.v1.command.HandleOAuthCallbackCommand;
import com.moa.api.dto.OAuthAuthorizationDto;
import com.moa.api.dto.OAuthCallbackDto;
import com.moa.common.oauth.OAuthStateTransport;
import com.moa.common.security.token.RefreshTokenTransport;
import com.moa.common.security.token.SignUpTokenTransport;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.PermitAll;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * 구글·카카오 인증 API. 브라우저가 주소로 이동해 부른다(fetch가 아니다). 가입과 로그인이 같은 콜백을 쓴다.
 */
@Tag(name = "구글·카카오 인증 API", description = "구글·카카오 동의 화면 이동과 콜백")
@PermitAll
@SecurityRequirements
@RestController
@RequestMapping("/api/v1/auth/oauth")
@RequiredArgsConstructor
public class ApiOAuthController {

	private final OAuthApplication oauthApplication;
	private final OAuthStateTransport oauthStateTransport;
	private final SignUpTokenTransport signUpTokenTransport;
	private final RefreshTokenTransport refreshTokenTransport;

	@Operation(summary = "동의 화면으로 이동", description = "state를 쿠키(oauth_state)에 두고 제공자 동의 화면으로 보낸다",
		responses = {
			@ApiResponse(responseCode = "302", description = "제공자로 이동"),
			@ApiResponse(responseCode = "404", description = "R001 모르는 제공자")
		})
	@GetMapping("/{provider}")
	public ResponseEntity<Void> authorize(
		@Parameter(description = "google 또는 kakao") @PathVariable String provider,
		@Parameter(description = "시작한 화면. SIGN_UP이면 실패 때 가입 화면, SIGN_IN이면 로그인 화면으로 돌아간다") @RequestParam(
			defaultValue = "SIGN_IN") String intent,
		HttpServletResponse httpResponse) {
		OAuthAuthorizationDto authorization = oauthApplication.authorize(provider, intent);
		oauthStateTransport.write(httpResponse, authorization.state());
		return redirect(authorization.authorizationUrl());
	}

	@Operation(summary = "제공자 콜백",
		description = "로그인이면 리프레시 쿠키를 쓰고 홈으로, 새 가입이면 가입 진행 쿠키를 쓰고 /signup/phone(카카오가 이메일을 "
			+ "주지 않으면 /signup/email)으로, 실패면 시작 화면에 error=오류 코드(A007, B003)를 붙여 보낸다",
		responses = @ApiResponse(responseCode = "302", description = "프론트로 이동"))
	@GetMapping("/{provider}/callback")
	public ResponseEntity<Void> callback(@PathVariable String provider,
		@RequestParam(required = false) String code, @RequestParam(required = false) String state,
		@RequestParam(required = false) String error, HttpServletRequest httpRequest,
		HttpServletResponse httpResponse) {
		OAuthCallbackDto result = oauthApplication.handleCallback(new HandleOAuthCallbackCommand(provider, code,
			state, oauthStateTransport.read(httpRequest), error));
		oauthStateTransport.clear(httpResponse);
		if (result.tokens() != null) {
			refreshTokenTransport.write(httpRequest, httpResponse, result.tokens());
		}
		if (result.signUpToken() != null) {
			signUpTokenTransport.write(httpResponse, result.signUpToken());
		}
		return redirect(result.redirectUrl());
	}

	private ResponseEntity<Void> redirect(String url) {
		return ResponseEntity.status(HttpStatus.FOUND).header(HttpHeaders.LOCATION, url).build();
	}
}
