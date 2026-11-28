package com.moa.api.controller.v1.user.request;

import java.util.List;

import org.jspecify.annotations.Nullable;

import com.moa.api.dto.user.ProfileSaveCommand;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

/** 목록·연도 범위·디스코드 형식은 서비스가 칸 오류(V001)로 확인한다 (specs/009-mypage contracts "프로필 저장") */
@Schema(description = "프로필 저장 요청. 모든 칸을 보낸다. null이나 빈 배열은 비움")
public record ProfileSaveRequest(
	@Schema(description = "기본 아바타 키 (GET /api/v1/profile/avatars). null이면 default", example = "cat") @Size(
		max = 30) @Nullable String avatar,
	@Schema(description = "자기소개. 500자까지") @Size(max = 500) @Nullable String bio,
	@Schema(description = "출생 연도. 1900~올해", example = "2000") @Nullable Integer birthYear,
	@Schema(description = "성별: MALE, FEMALE, UNDISCLOSED", example = "FEMALE") @Nullable String gender,
	@Schema(description = "나이·성별 공개. null이면 거짓") @Nullable Boolean ageGenderPublic,
	@Schema(description = "디스코드 사용자 이름. 2~32자 영문 소문자·숫자·_·.", example = "moa_user.01") @Size(
		max = 100) @Nullable String discordTag,
	@Schema(description = "디스코드 공개 범위: PRIVATE, FRIENDS, MEMBERS. null이면 PRIVATE",
		example = "MEMBERS") @Nullable String discordVisibility,
	@Schema(description = "플레이 시간대 (PLAY_TIME 코드)") @Size(max = 50) @Nullable List<String> playTimes) {

	public ProfileSaveCommand toCommand() {
		return new ProfileSaveCommand(avatar, bio, birthYear, gender, ageGenderPublic, discordTag, discordVisibility,
			playTimes);
	}
}
