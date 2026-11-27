package com.moa.api.controller.v1.user;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.moa.api.application.v1.user.MemberWithdrawalApplication;
import com.moa.api.controller.v1.user.request.WithdrawalRequest;
import com.moa.api.controller.v1.user.response.WithdrawalResponse;
import com.moa.common.security.AuthPrincipal;
import com.moa.common.security.CurrentUser;
import com.moa.common.security.token.RefreshTokenTransport;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** 회원 탈퇴 (specs/018-member-withdrawal contracts/withdrawal-api.md) */
@Tag(name = "마이페이지 API")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ApiMyWithdrawalController {

	private final MemberWithdrawalApplication memberWithdrawalApplication;
	private final RefreshTokenTransport refreshTokenTransport;

	@Operation(summary = "회원 탈퇴",
		description = "닉네임이 다르면 V001(칸 nickname), 관리자 계정은 A201. 성공하면 리프레시 쿠키를 지우고 완전 삭제 예정 시각을 준다. "
			+ "이미 탈퇴한 요청이 겹치면 처음 시각으로 답한다")
	@PostMapping("/users/me/withdrawal")
	public ResponseEntity<WithdrawalResponse> withdraw(@CurrentUser AuthPrincipal user,
		@Valid @RequestBody WithdrawalRequest request, HttpServletRequest httpRequest,
		HttpServletResponse httpResponse) {
		WithdrawalResponse response = WithdrawalResponse
			.from(memberWithdrawalApplication.withdraw(user.userId(), request.nickname()));
		refreshTokenTransport.clear(httpRequest, httpResponse);
		return ResponseEntity.ok(response);
	}
}
