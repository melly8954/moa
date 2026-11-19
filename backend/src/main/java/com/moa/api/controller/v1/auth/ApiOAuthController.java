package com.moa.api.controller.v1.auth;

import java.net.URI;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.moa.api.application.v1.auth.SocialLoginApplication;
import com.moa.api.controller.v1.auth.request.OAuthSignUpRequest;
import com.moa.api.controller.v1.auth.response.AuthRefreshResponse;
import com.moa.api.controller.v1.auth.response.OAuthProvidersResponse;
import com.moa.common.response.ErrorResponse;
import com.moa.common.security.oauth.OAuthCookies;
import com.moa.common.security.oauth.OAuthProviderRegistry;
import com.moa.common.security.oauth.SocialProvider;
import com.moa.common.security.token.IssuedTokens;
import com.moa.common.security.token.RefreshTokenTransport;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
 * 소셜 로그인 API (specs/010-social-login contracts/auth-oauth.md). 모두 로그인 없이 부른다 (@PermitAll, research R1).
 * 인가 시작과 콜백은 브라우저 이동(302)이고, 가입은 기존 로그인과 같은 토큰 응답이다.
 */
@Tag(name = "소셜 로그인 API", description = "제공자 목록, 인가 시작, 콜백, 가입")
@PermitAll
@SecurityRequirements
@RestController
@RequestMapping("/api/v1/auth/oauth")
@RequiredArgsConstructor
public class ApiOAuthController {

	private final OAuthProviderRegistry providerRegistry;
	private final SocialLoginApplication socialLoginApplication;
	private final OAuthCookies oauthCookies;
	private final RefreshTokenTransport refreshTokenTransport;

	@Operation(summary = "제공자 목록", description = "설정이 있는 제공자만 준다. 로그인 화면은 이 목록의 단추만 보인다 (US3-AC3)",
		responses = @ApiResponse(responseCode = "200", description = "제공자 목록", useReturnTypeSchema = true))
	@GetMapping("/providers")
	public ResponseEntity<OAuthProvidersResponse> providers() {
		return ResponseEntity.ok(new OAuthProvidersResponse(
			providerRegistry.enabledProviders().stream().map(SocialProvider::pathValue).toList()));
	}

	@Operation(summary = "인가 시작", description = "브라우저가 이 주소로 이동한다. oauth_state 쿠키를 심고 제공자 인가 화면으로 보낸다. "
		+ "제공자 설정이 없으면 /sign-in?oauthError=disabled로 보낸다",
		responses = @ApiResponse(responseCode = "302", description = "제공자 인가 화면 또는 로그인 화면"))
	@GetMapping("/{provider}/authorize")
	public ResponseEntity<Void> authorize(
		@Parameter(description = "제공자", example = "google") @PathVariable String provider,
		@Parameter(description = "login (연결 link는 나중)") @RequestParam(required = false) String mode,
		@Parameter(description = "로그인 뒤 돌아갈 같은 사이트 경로") @RequestParam(required = false) String next,
		HttpServletResponse httpResponse) {
		return redirect(socialLoginApplication.authorize(provider, mode, next, httpResponse));
	}

	@Operation(summary = "콜백", description = "제공자가 돌려보내는 주소. 결과에 따라 /auth/oauth/done, /sign-up/nickname, "
		+ "/sign-in?oauthError=… 로 보낸다",
		responses = @ApiResponse(responseCode = "302", description = "결과 화면"))
	@GetMapping("/{provider}/callback")
	public ResponseEntity<Void> callback(@PathVariable String provider, @RequestParam(required = false) String code,
		@RequestParam(required = false) String state, @RequestParam(required = false) String error,
		HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
		return redirect(socialLoginApplication.callback(provider, code, state, error, httpRequest, httpResponse));
	}

	@Operation(summary = "가입", description = "oauth_signup 쿠키의 소셜 계정으로 닉네임·약관 동의를 받아 회원을 만들고 로그인시킨다. "
		+ "응답은 로그인과 같다. 성공하면 oauth_signup 쿠키를 지운다",
		responses = {
			@ApiResponse(responseCode = "200", description = "가입·로그인 성공", useReturnTypeSchema = true),
			@ApiResponse(responseCode = "400",
				description = "V001 닉네임 규칙, A017 약관 미동의·버전 다름, A009 가입 대기 쿠키 없음·만료",
				content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode = "409", description = "A012 이미 가입된 소셜 계정, A015 이메일 중복, B201 닉네임 태그가 다 참",
				content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
		})
	@PostMapping("/sign-up")
	public ResponseEntity<AuthRefreshResponse> signUp(@Valid @RequestBody OAuthSignUpRequest request,
		HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
		IssuedTokens tokens = socialLoginApplication.signUp(oauthCookies.readSignup(httpRequest), request.toCommand());
		oauthCookies.clearSignup(httpResponse);
		String bodyRefreshToken = refreshTokenTransport.write(httpRequest, httpResponse, tokens);
		return ResponseEntity.ok(AuthRefreshResponse.from(tokens, bodyRefreshToken));
	}

	private static ResponseEntity<Void> redirect(String location) {
		return ResponseEntity.status(HttpStatus.FOUND)
			.location(URI.create(location))
			.cacheControl(CacheControl.noStore())
			.build();
	}
}
