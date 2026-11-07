package com.moa.api.controller.v1.user;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.moa.api.application.v1.user.UserApplication;
import com.moa.api.controller.v1.user.request.PasswordChangeRequest;
import com.moa.api.controller.v1.user.response.MyPermissionResponse;
import com.moa.api.controller.v1.user.response.UserGetResponse;
import com.moa.common.security.AuthPrincipal;
import com.moa.common.security.CurrentUser;
import com.moa.common.security.permission.PermissionGuard;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "사용자 API")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class ApiUserController {

	private final UserApplication userApplication;
	private final PermissionGuard permissionGuard;

	@Operation(summary = "내 정보", description = "로그인한 사용자의 정보. 화면이 로그인 상태와 역할을 확인할 때 쓴다")
	@GetMapping("/me")
	public ResponseEntity<UserGetResponse> me(@CurrentUser AuthPrincipal user) {
		return ResponseEntity.ok(UserGetResponse.from(userApplication.getMe(user.userId())));
	}

	@Operation(summary = "내 권한", description = "로그인한 사용자의 권한 키. 화면이 버튼·메뉴를 숨길 때 쓴다 (API 보호는 서버가 따로 한다)")
	@GetMapping("/me/permissions")
	public ResponseEntity<MyPermissionResponse> myPermissions(@CurrentUser AuthPrincipal user) {
		return ResponseEntity.ok(new MyPermissionResponse(List.copyOf(permissionGuard.permissions(user))));
	}

	@Operation(summary = "내 비밀번호 바꾸기",
		description = "바꾸면 비밀번호 변경 요구(passwordChangeRequired)가 풀린다. 현재 비밀번호가 틀리거나 새 비밀번호가 같으면 V001, "
			+ "비밀번호가 없는 계정(소셜)은 B001")
	@PutMapping("/me/password")
	public ResponseEntity<Void> changePassword(@CurrentUser AuthPrincipal user,
		@Valid @RequestBody PasswordChangeRequest request) {
		userApplication.changePassword(user.userId(), request.currentPassword(), request.newPassword());
		return ResponseEntity.noContent().build();
	}
}
