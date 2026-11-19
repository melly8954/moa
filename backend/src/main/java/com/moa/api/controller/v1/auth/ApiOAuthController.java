package com.moa.api.controller.v1.auth;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.moa.api.controller.v1.auth.response.OAuthProvidersResponse;
import com.moa.common.security.oauth.OAuthProviderRegistry;
import com.moa.common.security.oauth.SocialProvider;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.PermitAll;
import lombok.RequiredArgsConstructor;

/**
 * 소셜 로그인 API (specs/010-social-login contracts/auth-oauth.md). 모두 로그인 없이 부른다.
 * 인가 시작·콜백·가입은 US1에서 더한다.
 */
@Tag(name = "소셜 로그인 API", description = "제공자 목록, 인가 시작, 콜백, 가입")
@PermitAll
@SecurityRequirements
@RestController
@RequestMapping("/api/v1/auth/oauth")
@RequiredArgsConstructor
public class ApiOAuthController {

	private final OAuthProviderRegistry providerRegistry;

	@Operation(summary = "제공자 목록", description = "설정이 있는 제공자만 준다. 로그인 화면은 이 목록의 단추만 보인다 (US3-AC3)",
		responses = @ApiResponse(responseCode = "200", description = "제공자 목록", useReturnTypeSchema = true))
	@GetMapping("/providers")
	public ResponseEntity<OAuthProvidersResponse> providers() {
		return ResponseEntity.ok(new OAuthProvidersResponse(
			providerRegistry.enabledProviders().stream().map(SocialProvider::pathValue).toList()));
	}
}
