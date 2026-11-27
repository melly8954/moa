package com.moa.api.service.user;

import java.util.List;
import java.util.Optional;

import com.moa.api.dto.user.DiscordVisibility;
import com.moa.api.dto.user.Gender;
import com.moa.api.dto.user.ProfileDto;
import com.moa.api.dto.user.ProfileDto.PlayTimeDto;

/**
 * 다른 회원이 내 프로필을 보는 규칙 한 곳 (잠정, specs/009-mypage FR-013·research R9).
 * 이번에는 이 규칙을 쓰는 API·화면을 만들지 않는다. 다른 회원 프로필을 보이는 기능이 이 규칙을 따른다.
 */
public final class ProfileVisibility {

	private ProfileVisibility() {
	}

	/** 다른 회원에게 보이는 프로필. 가려진 칸은 null */
	public record VisibleProfile(String nickname, String nicknameTag, String avatar, String bio, Integer age,
		Gender gender, String discordTag, List<PlayTimeDto> playTimes) {
	}

	/**
	 * @param viewerUserId 보는 사람. 로그인하지 않았으면 null
	 * @return 로그인하지 않았으면 비어 있다
	 */
	public static Optional<VisibleProfile> view(ProfileDto profile, Long viewerUserId) {
		if (viewerUserId == null) {
			return Optional.empty();
		}
		boolean ageGender = profile.ageGenderPublic();
		// FRIENDS(친구만)는 친구 기능(specs/014-friends-follow)이 생기기 전까지 아무에게도 보이지 않는다 (Edge Cases)
		boolean discord = profile.discordVisibility() == DiscordVisibility.MEMBERS;
		return Optional.of(new VisibleProfile(profile.nickname(), profile.nicknameTag(), profile.avatar(),
			profile.bio(), ageGender ? profile.age() : null, ageGender ? profile.gender() : null,
			discord ? profile.discordTag() : null, profile.playTimes()));
	}
}
