package com.moa.api.application.v1.user;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.moa.api.dto.code.CodeDto;
import com.moa.api.dto.user.ProfileDto;
import com.moa.api.service.code.CodeService;
import com.moa.api.service.user.MemberActivityPolicy;
import com.moa.api.service.user.ProfileService;

import lombok.RequiredArgsConstructor;

/**
 * 마이페이지 흐름 (specs/009-mypage). 모든 마이페이지 API는 먼저 관리자 계정을 막는다 (FR-016, A201).
 * 기본 아바타 목록(GET /api/v1/profile/avatars)은 막지 않는다.
 */
@Component
@RequiredArgsConstructor
public class ProfileApplication {

	static final String PLAY_TIME_GROUP = "PLAY_TIME";

	private final MemberActivityPolicy memberActivityPolicy;
	private final ProfileService profileService;
	private final CodeService codeService;

	/** 내 프로필 (US1-AC1·AC4). 꺼진 시간대 코드도 이름과 함께 보인다 (Edge Cases) */
	@Transactional(readOnly = true)
	public ProfileDto getMyProfile(Long userId) {
		memberActivityPolicy.requireMypageAllowed(userId);
		return withPlayTimeNames(profileService.getMyProfile(userId));
	}

	private ProfileDto withPlayTimeNames(ProfileDto profile) {
		if (profile.playTimes().isEmpty()) {
			return profile;
		}
		Map<String, String> names = new LinkedHashMap<>();
		for (CodeDto code : codeService.getCodes(PLAY_TIME_GROUP, true)) {
			names.put(code.code(), code.name());
		}
		return profile.withPlayTimeNames(names);
	}
}
