package com.moa.api.service.user;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.moa.api.dto.user.DiscordVisibility;
import com.moa.api.dto.user.Gender;
import com.moa.api.dto.user.ProfileDto;
import com.moa.api.dto.user.ProfileDto.PlayTimeDto;

/**
 * 다른 회원에게 보이는 프로필 규칙 (잠정, specs/009-mypage FR-013, research R9). 쓰는 API는 아직 없다.
 */
class ProfileVisibilityTest {

	private static final Long VIEWER = 20L;

	@Test
	@DisplayName("FR-013 로그인하지 않은 사람에게는 아무것도 보이지 않는다")
	void anonymousSeesNothing() {
		assertThat(ProfileVisibility.view(profile(true, DiscordVisibility.MEMBERS), null)).isEmpty();
	}

	@Test
	@DisplayName("FR-013 닉네임·태그·아바타·소개·시간대는 로그인한 회원 모두에게 보인다")
	void membersSeePublicFields() {
		assertThat(ProfileVisibility.view(profile(false, DiscordVisibility.PRIVATE), VIEWER)).hasValueSatisfying(
			visible -> {
				assertThat(visible.nickname()).isEqualTo("모아킴");
				assertThat(visible.nicknameTag()).isEqualTo("0421");
				assertThat(visible.avatar()).isEqualTo("cat");
				assertThat(visible.bio()).isEqualTo("소개");
				assertThat(visible.playTimes()).containsExactly(new PlayTimeDto("DAWN", "새벽"));
			});
	}

	@Test
	@DisplayName("FR-013 나이와 성별은 공개로 켠 경우에만 함께 보인다")
	void ageAndGenderFollowOneSwitch() {
		assertThat(ProfileVisibility.view(profile(true, DiscordVisibility.PRIVATE), VIEWER)).hasValueSatisfying(
			visible -> {
				assertThat(visible.age()).isEqualTo(26);
				assertThat(visible.gender()).isEqualTo(Gender.FEMALE);
			});
		assertThat(ProfileVisibility.view(profile(false, DiscordVisibility.PRIVATE), VIEWER)).hasValueSatisfying(
			visible -> {
				assertThat(visible.age()).isNull();
				assertThat(visible.gender()).isNull();
			});
	}

	@ParameterizedTest(name = "FR-013 디스코드 공개 범위 {0}")
	@EnumSource(DiscordVisibility.class)
	@DisplayName("FR-013 디스코드 태그는 전체 회원일 때만 보인다 (친구만은 친구 기능 전까지 아무에게도 보이지 않는다)")
	void discordTag(DiscordVisibility visibility) {
		String expected = visibility == DiscordVisibility.MEMBERS ? "moa_user" : null;

		assertThat(ProfileVisibility.view(profile(true, visibility), VIEWER)).hasValueSatisfying(
			visible -> assertThat(visible.discordTag()).isEqualTo(expected));
	}

	private static ProfileDto profile(boolean ageGenderPublic, DiscordVisibility visibility) {
		return new ProfileDto("모아킴", "0421", "cat", "소개", 2000, 26, Gender.FEMALE, ageGenderPublic, "moa_user",
			visibility, List.of(new PlayTimeDto("DAWN", "새벽")), null);
	}
}
