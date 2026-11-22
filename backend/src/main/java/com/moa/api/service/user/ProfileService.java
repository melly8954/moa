package com.moa.api.service.user;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.moa.api.dto.user.DefaultAvatar;
import com.moa.api.dto.user.DiscordVisibility;
import com.moa.api.dto.user.ProfileDto;
import com.moa.api.dto.user.ProfileDto.PlayTimeDto;
import com.moa.api.entity.user.User;
import com.moa.api.entity.user.UserProfile;
import com.moa.api.repository.reader.user.UserProfileReader;
import com.moa.api.repository.reader.user.UserReader;
import com.moa.api.repository.writer.user.UserProfileWriter;
import com.moa.common.exception.ErrorCode;
import com.moa.common.exception.ServiceException;

import lombok.RequiredArgsConstructor;

/**
 * 마이페이지 프로필 (specs/009-mypage FR-001·FR-006~FR-009·FR-012, research R3·R14).
 * 닉네임은 tb_user.name, 나머지는 tb_user_profile. 프로필 행은 처음 진입(조회)에서 만들지 않고 처음 고칠 때 만든다.
 */
@Service
@RequiredArgsConstructor
public class ProfileService {

	private final UserReader userReader;
	private final UserProfileReader userProfileReader;
	private final UserProfileWriter userProfileWriter;
	private final NicknameTags nicknameTags;
	private final ProfileProperties profileProperties;

	/**
	 * 내 프로필. 행이 없으면 기본값(태그 없음, 아바타 default, 빈 값, 비공개). 시간대 이름은 비어 있다(Application이 붙인다).
	 *
	 * @throws ServiceException 회원이 없으면 R001
	 */
	public ProfileDto getMyProfile(Long userId) {
		User user = requireUser(userId);
		Optional<UserProfile> found = userProfileReader.find(userId);
		if (found.isEmpty()) {
			return new ProfileDto(user.getName(), null, DefaultAvatar.DEFAULT.key(), null, null, null, null, false,
				null, DiscordVisibility.PRIVATE, List.of(), null);
		}
		UserProfile profile = found.get();
		Integer birthYear = profile.getBirthYear() == null ? null : profile.getBirthYear().intValue();
		// 생일을 받지 않으므로 연 나이 (spec 가정)
		Integer age = birthYear == null ? null : LocalDate.now().getYear() - birthYear;
		List<PlayTimeDto> playTimes = profile.getPlayTimes().stream()
			.sorted()
			.map(code -> new PlayTimeDto(code, null))
			.toList();
		return new ProfileDto(user.getName(), profile.getNicknameTag(),
			DefaultAvatar.displayKey(profile.getAvatarKey()),
			profile.getBio(), birthYear, age, profile.getGender(), profile.isAgeGenderPublic(), profile.getDiscordTag(),
			profile.getDiscordVisibility(), playTimes, nicknameChangeableAt(profile, LocalDateTime.now()));
	}

	/**
	 * 고치기 전에 프로필 행을 잠가 가져온다. 없으면(키트 경로로 만든 회원) 지금 이름으로 키·태그를 정해 만든다 (research R14).
	 * 새로 만들 때 태그 유일 제약에 걸리면 DataIntegrityViolationException이 난다. 부르는 쪽(Application)이
	 * {@link NicknameTags#isTagConflict}로 알아보고 새 트랜잭션으로 다시 한다.
	 *
	 * @throws ServiceException 회원이 없으면 R001, 그 닉네임의 태그가 다 찼으면 B201
	 */
	public UserProfile requireProfileForUpdate(Long userId) {
		return userProfileReader.findForUpdate(userId).orElseGet(() -> create(userId));
	}

	/** 다음에 닉네임을 바꿀 수 있는 시각. 지금 바꿀 수 있으면(첫 변경 포함) null (FR-004) */
	LocalDateTime nicknameChangeableAt(UserProfile profile, LocalDateTime now) {
		LocalDateTime changedAt = profile.getNicknameChangedAt();
		if (changedAt == null) {
			return null;
		}
		LocalDateTime changeableAt = changedAt.plus(profileProperties.nicknameCooldown());
		return changeableAt.isAfter(now) ? changeableAt : null;
	}

	private UserProfile create(Long userId) {
		String key = NicknamePolicy.key(requireUser(userId).getName());
		return userProfileWriter.save(new UserProfile(userId, key, nicknameTags.assign(key)));
	}

	private User requireUser(Long userId) {
		return userReader.find(userId).orElseThrow(() -> new ServiceException(ErrorCode.NOT_FOUND));
	}
}
