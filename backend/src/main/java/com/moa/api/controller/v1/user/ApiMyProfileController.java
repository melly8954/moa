package com.moa.api.controller.v1.user;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.moa.api.application.v1.user.ProfileApplication;
import com.moa.api.controller.v1.user.request.NicknameChangeRequest;
import com.moa.api.controller.v1.user.request.ProfileSaveRequest;
import com.moa.api.controller.v1.user.response.AvatarListResponse;
import com.moa.api.controller.v1.user.response.NicknameChangeResponse;
import com.moa.api.controller.v1.user.response.ProfileGetResponse;
import com.moa.common.security.AuthPrincipal;
import com.moa.common.security.CurrentUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** 마이페이지 (specs/009-mypage contracts/profile-api.md). 관리자 계정(ADMIN)은 아바타 목록 말고 모두 A201 */
@Tag(name = "마이페이지 API")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ApiMyProfileController {

	private final ProfileApplication profileApplication;

	@Operation(summary = "내 프로필", description = "프로필을 아직 만들지 않았으면 기본값. 관리자 계정은 A201")
	@GetMapping("/users/me/profile")
	public ResponseEntity<ProfileGetResponse> getMyProfile(@CurrentUser AuthPrincipal user) {
		return ResponseEntity.ok(ProfileGetResponse.from(profileApplication.getMyProfile(user.userId())));
	}

	@Operation(summary = "프로필 저장",
		description = "닉네임·게임 정보를 뺀 모든 칸을 보낸 값으로 바꾼다. 칸 오류는 V001, 관리자 계정은 A201")
	@PutMapping("/users/me/profile")
	public ResponseEntity<ProfileGetResponse> saveProfile(@CurrentUser AuthPrincipal user,
		@Valid @RequestBody ProfileSaveRequest request) {
		return ResponseEntity.ok(
			ProfileGetResponse.from(profileApplication.saveProfile(user.userId(), request.toCommand())));
	}

	@Operation(summary = "닉네임 바꾸기",
		description = "지금과 같으면 그대로. 규칙 V001, 변경 제한 기간 안 B202(다시 바꿀 수 있는 시각은 프로필 조회의 "
			+ "nicknameChangeableAt), 태그가 다 차면 B201, 관리자 계정은 A201")
	@PutMapping("/users/me/nickname")
	public ResponseEntity<NicknameChangeResponse> changeNickname(@CurrentUser AuthPrincipal user,
		@Valid @RequestBody NicknameChangeRequest request) {
		return ResponseEntity.ok(
			NicknameChangeResponse.from(profileApplication.changeNickname(user.userId(), request.nickname())));
	}

	@Operation(summary = "기본 아바타 목록", description = "로그인한 누구나 부를 수 있다")
	@GetMapping("/profile/avatars")
	public ResponseEntity<AvatarListResponse> getAvatars() {
		return ResponseEntity.ok(new AvatarListResponse(profileApplication.getAvatars()));
	}
}
