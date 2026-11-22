package com.moa.api.application.v1.user;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.moa.api.dto.code.CodeDto;
import com.moa.api.dto.user.DefaultAvatar;
import com.moa.api.dto.user.NicknameChangeDto;
import com.moa.api.dto.user.ProfileDto;
import com.moa.api.dto.user.ProfileSaveCommand;
import com.moa.api.service.code.CodeService;
import com.moa.api.service.user.MemberActivityPolicy;
import com.moa.api.service.user.NicknameTags;
import com.moa.api.service.user.ProfileCreationConflictException;
import com.moa.api.service.user.ProfileService;

import lombok.RequiredArgsConstructor;

/**
 * 마이페이지 흐름 (specs/009-mypage). 모든 마이페이지 API는 먼저 관리자 계정을 막는다 (FR-016, A201).
 * 기본 아바타 목록(GET /api/v1/profile/avatars)은 막지 않는다.
 * <p>
 * 고치는 흐름은 TransactionTemplate으로 한 트랜잭션에 묶고, 태그 유일 제약이나 프로필 행 동시 생성에 걸리면 새 트랜잭션으로 처음부터 다시 한다
 * (최대 {@link NicknameTags#MAX_ATTEMPTS}번, 마지막에도 걸리면 서비스가 B201·R002로 바꾼다. 태그는 010 가입과 같은 규칙).
 */
@Component
@RequiredArgsConstructor
public class ProfileApplication {

	static final String PLAY_TIME_GROUP = "PLAY_TIME";

	private final MemberActivityPolicy memberActivityPolicy;
	private final ProfileService profileService;
	private final CodeService codeService;
	private final TransactionTemplate transactionTemplate;

	/** 내 프로필 (US1-AC1·AC4). 꺼진 시간대 코드도 이름과 함께 보인다 (Edge Cases) */
	@Transactional(readOnly = true)
	public ProfileDto getMyProfile(Long userId) {
		memberActivityPolicy.requireMypageAllowed(userId);
		ProfileDto profile = profileService.getMyProfile(userId);
		return profile.playTimes().isEmpty() ? profile : withPlayTimeNames(profile, playTimeCodes());
	}

	/** 닉네임 바꾸기 (US2) */
	public NicknameChangeDto changeNickname(Long userId, String nickname) {
		return withRetry(lastAttempt -> {
			memberActivityPolicy.requireMypageAllowed(userId);
			return profileService.changeNickname(userId, nickname, lastAttempt);
		});
	}

	/** 프로필 저장 (US3·US4·US6). 켜진 PLAY_TIME 코드만 받는다 */
	public ProfileDto saveProfile(Long userId, ProfileSaveCommand command) {
		return withRetry(lastAttempt -> {
			memberActivityPolicy.requireMypageAllowed(userId);
			List<CodeDto> codes = playTimeCodes();
			Set<String> enabled = codes.stream()
				.filter(CodeDto::enabled)
				.map(CodeDto::code)
				.collect(Collectors.toSet());
			return withPlayTimeNames(profileService.save(userId, command, enabled, lastAttempt), codes);
		});
	}

	/** 기본 아바타 키 (US4, research R10). 목록일 뿐이라 관리자 계정도 막지 않는다 */
	public List<String> getAvatars() {
		return DefaultAvatar.keys();
	}

	/**
	 * 태그 충돌과 프로필 행 동시 생성(PK 충돌·교착)은 새 트랜잭션으로 다시 한다. 마지막 시도에서는 서비스가 B201·R002로 바꾼다.
	 */
	private <T> T withRetry(Function<Boolean, T> work) {
		for (int attempt = 1;; attempt++) {
			boolean lastAttempt = attempt >= NicknameTags.MAX_ATTEMPTS;
			try {
				return transactionTemplate.execute(status -> work.apply(lastAttempt));
			} catch (DataIntegrityViolationException ex) {
				if (lastAttempt || !NicknameTags.isTagConflict(ex)) {
					throw ex;
				}
			} catch (ProfileCreationConflictException ex) {
				if (lastAttempt) {
					throw ex;
				}
			}
		}
	}

	/** 공통코드 순서, 꺼진 항목 포함 */
	private List<CodeDto> playTimeCodes() {
		return codeService.getCodes(PLAY_TIME_GROUP, true);
	}

	private static ProfileDto withPlayTimeNames(ProfileDto profile, List<CodeDto> codes) {
		if (profile.playTimes().isEmpty()) {
			return profile;
		}
		Map<String, String> names = new LinkedHashMap<>();
		for (CodeDto code : codes) {
			names.put(code.code(), code.name());
		}
		return profile.withPlayTimeNames(names);
	}
}
