package com.moa.api.controller.v1;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.moa.api.application.v1.AuthTokenApplication;
import com.moa.api.controller.v1.request.AuthRefreshRequest;
import com.moa.api.controller.v1.response.AuthRefreshResponse;
import com.moa.common.response.ErrorResponse;
import com.moa.common.security.token.IssuedTokens;
import com.moa.common.security.token.RefreshTokenTransport;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.PermitAll;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * 토큰 재발급·로그아웃 API. 정본은 프로젝트 API 문서다 (키트 문서 조각 auth-token에서 옮김)
 */
@Tag(name = "인증 토큰 API", description = "액세스 토큰 재발급과 로그아웃")
@PermitAll
@SecurityRequirements
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class ApiAuthTokenController {

	private final AuthTokenApplication authTokenApplication;
	private final RefreshTokenTransport refreshTokenTransport;

	@Operation(summary = "토큰 재발급", description = "리프레시 토큰으로 액세스 토큰을 다시 받는다. 리프레시 토큰도 새 것으로 바뀐다",
		parameters = @Parameter(in = ParameterIn.HEADER, name = RefreshTokenTransport.CLIENT_TYPE_HEADER,
			description = "앱이면 app. 리프레시 토큰을 본문으로 주고받는다"),
		responses = {
			@ApiResponse(responseCode = "200", description = "재발급 성공", useReturnTypeSchema = true),
			@ApiResponse(responseCode = "401", description = "A003 무효·만료, A004 재사용 감지",
				content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
		})
	@PostMapping("/token/refresh")
	public ResponseEntity<AuthRefreshResponse> refresh(@RequestBody(required = false) AuthRefreshRequest request,
		HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
		String refreshToken = refreshTokenTransport.read(httpRequest, bodyValue(request));
		IssuedTokens tokens = authTokenApplication.refresh(refreshToken);
		String bodyRefreshToken = refreshTokenTransport.write(httpRequest, httpResponse, tokens);
		return ResponseEntity.ok(AuthRefreshResponse.from(tokens, bodyRefreshToken));
	}

	@Operation(summary = "로그아웃", description = "리프레시 토큰을 폐기한다. 토큰이 없거나 이미 폐기됐어도 성공한다",
		responses = @ApiResponse(responseCode = "204", description = "로그아웃 성공"))
	@PostMapping("/logout")
	public ResponseEntity<Void> logout(@RequestBody(required = false) AuthRefreshRequest request,
		HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
		authTokenApplication.logout(refreshTokenTransport.read(httpRequest, bodyValue(request)));
		refreshTokenTransport.clear(httpRequest, httpResponse);
		return ResponseEntity.noContent().build();
	}

	private String bodyValue(AuthRefreshRequest request) {
		return request == null ? null : request.refreshToken();
	}
}
