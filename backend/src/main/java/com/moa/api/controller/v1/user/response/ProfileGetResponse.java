package com.moa.api.controller.v1.user.response;

import java.time.OffsetDateTime;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.moa.api.dto.user.DiscordVisibility;
import com.moa.api.dto.user.Gender;
import com.moa.api.dto.user.ProfileDto;
import com.moa.common.response.ApiTimes;

import io.swagger.v3.oas.annotations.media.Schema;

/** specs/009-mypage contracts/profile-api.md GET. 게임 정보(games)는 US5 때 더한다 */
@Schema(description = "내 프로필")
public record ProfileGetResponse(
	@Schema(description = "닉네임 (tb_user.name)", example = "모아") String nickname,
	@Schema(description = "닉네임 태그. 프로필이 아직 없으면 null", example = "0421") @Nullable String nicknameTag,
	@Schema(description = "아바타 키. 목록에서 빠진 키면 default", example = "default") String avatar,
	@Schema(description = "자기소개") @Nullable String bio,
	@Schema(description = "출생 연도", example = "2000") @Nullable Integer birthYear,
	@Schema(description = "나이 (올해 − 출생 연도). 출생 연도가 없으면 null", example = "26") @Nullable Integer age,
	@Schema(description = "성별") @Nullable Gender gender,
	@Schema(description = "나이·성별 공개") boolean ageGenderPublic,
	@Schema(description = "디스코드 사용자 이름") @Nullable String discordTag,
	@Schema(description = "디스코드 공개 범위") DiscordVisibility discordVisibility,
	@Schema(description = "플레이 가능 시간대 (공통코드 순서, 꺼진 값도 이름과 함께)") List<PlayTime> playTimes,
	@Schema(description = "다음에 닉네임을 바꿀 수 있는 시각. 지금 바꿀 수 있으면 null") @Nullable OffsetDateTime nicknameChangeableAt) {

	@Schema(description = "플레이 가능 시간대")
	public record PlayTime(
		@Schema(description = "코드", example = "WEEKDAY_EVENING") String code,
		@Schema(description = "이름", example = "평일 저녁") String name) {
	}

	public static ProfileGetResponse from(ProfileDto profile) {
		return new ProfileGetResponse(profile.nickname(), profile.nicknameTag(), profile.avatar(), profile.bio(),
			profile.birthYear(), profile.age(), profile.gender(), profile.ageGenderPublic(), profile.discordTag(),
			profile.discordVisibility(),
			profile.playTimes().stream().map(playTime -> new PlayTime(playTime.code(), playTime.name())).toList(),
			ApiTimes.of(profile.nicknameChangeableAt()));
	}
}
