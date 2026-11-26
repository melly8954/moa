package com.moa.api.controller.v1.auth;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.moa.api.application.v1.user.AccountRecoveryApplication;
import com.moa.api.controller.v1.auth.response.AuthRefreshResponse;
import com.moa.api.controller.v1.auth.response.RecoveryResponse;
import com.moa.common.security.recovery.RecoveryCookies;
import com.moa.common.security.token.IssuedTokens;
import com.moa.common.security.token.RefreshTokenTransport;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.PermitAll;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * 탈퇴한 계정 복구 (specs/018-member-withdrawal contracts/withdrawal-api.md). 로그인 없이 부르고, 본인 확인에 성공한 로그인이 준
 * 복구 쿠키(withdrawal_recovery)로 확인한다.
 */
@Tag(name = "인증 API")
@PermitAll
@SecurityRequirements
@RestController
@RequestMapping("/api/v1/auth/recovery")
@RequiredArgsConstructor
public class ApiAccountRecoveryController {

	private final AccountRecoveryApplication accountRecoveryApplication;
	private final RecoveryCookies recoveryCookies;
	private final RefreshTokenTransport refreshTokenTransport;

	@Operation(summary = "복구 안내", description = "완전 삭제 예정 시각. 복구 쿠키가 없거나 맞지 않으면 A403, 유예가 지났으면 A402")
	@GetMapping
	public ResponseEntity<RecoveryResponse> getRecovery(HttpServletRequest request) {
		return ResponseEntity
			.ok(RecoveryResponse.from(accountRecoveryApplication.getRecovery(recoveryCookies.read(request))));
	}

	@Operation(summary = "복구",
		description = "탈퇴 전 그대로 돌아오고 로그인과 같은 토큰을 준다. 복구 쿠키를 지운다. "
			+ "쿠키가 없거나 맞지 않으면(이미 복구함·다시 탈퇴함) A403, 잠겼으면 A007, 유예가 지났으면 A402")
	@PostMapping
	public ResponseEntity<AuthRefreshResponse> recover(HttpServletRequest request, HttpServletResponse response) {
		IssuedTokens tokens = accountRecoveryApplication.recover(recoveryCookies.read(request));
		String bodyRefreshToken = refreshTokenTransport.write(request, response, tokens);
		recoveryCookies.clear(response);
		return ResponseEntity.ok(AuthRefreshResponse.from(tokens, bodyRefreshToken));
	}
}
