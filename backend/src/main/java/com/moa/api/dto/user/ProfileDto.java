package com.moa.api.dto.user;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * 내 프로필 (specs/009-mypage contracts/profile-api.md GET). 게임 정보는 US5 때 더한다.
 *
 * @param nicknameTag 프로필 행이 없으면 null
 * @param avatar 보일 아바타 키 (목록에서 빠진 키는 default)
 * @param age 올해 − 출생 연도. 출생 연도가 없으면 null
 * @param nicknameChangeableAt 다음에 닉네임을 바꿀 수 있는 시각. 지금 바꿀 수 있으면 null
 */
public record ProfileDto(String nickname, String nicknameTag, String avatar, String bio, Integer birthYear,
	Integer age, Gender gender, boolean ageGenderPublic, String discordTag, DiscordVisibility discordVisibility,
	List<PlayTimeDto> playTimes, LocalDateTime nicknameChangeableAt) {

	/**
	 * @param name 공통코드 PLAY_TIME 항목 이름. 서비스는 코드만 채우고 Application이 이름을 붙인다
	 */
	public record PlayTimeDto(String code, String name) {
	}

	/**
	 * 시간대에 이름을 붙이고 공통코드 순서로 늘어놓는다. 이름을 못 찾은 코드(지운 항목)는 코드를 이름으로 맨 뒤에 둔다.
	 *
	 * @param names 공통코드 순서대로 코드 → 이름 (꺼진 항목 포함)
	 */
	public ProfileDto withPlayTimeNames(Map<String, String> names) {
		List<String> order = List.copyOf(names.keySet());
		List<PlayTimeDto> named = playTimes.stream()
			.map(playTime -> new PlayTimeDto(playTime.code(), names.getOrDefault(playTime.code(), playTime.code())))
			.sorted(Comparator.comparingInt((PlayTimeDto playTime) -> rank(order, playTime.code()))
				.thenComparing(PlayTimeDto::code))
			.toList();
		return new ProfileDto(nickname, nicknameTag, avatar, bio, birthYear, age, gender, ageGenderPublic, discordTag,
			discordVisibility, named, nicknameChangeableAt);
	}

	private static int rank(List<String> order, String code) {
		int index = order.indexOf(code);
		return index < 0 ? Integer.MAX_VALUE : index;
	}
}
