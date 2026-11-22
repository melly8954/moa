package com.moa.api.service.user;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Service;

import com.moa.api.dto.user.DefaultAvatar;
import com.moa.api.dto.user.DiscordVisibility;
import com.moa.api.dto.user.Gender;
import com.moa.api.dto.user.NicknameChangeDto;
import com.moa.api.dto.user.ProfileDto;
import com.moa.api.dto.user.ProfileDto.PlayTimeDto;
import com.moa.api.dto.user.ProfileSaveCommand;
import com.moa.api.entity.user.User;
import com.moa.api.entity.user.UserProfile;
import com.moa.api.repository.reader.user.UserProfileReader;
import com.moa.api.repository.reader.user.UserReader;
import com.moa.api.repository.writer.user.UserProfileWriter;
import com.moa.common.exception.ErrorCode;
import com.moa.common.exception.ServiceException;
import com.moa.common.response.ErrorResponse.FieldError;

import lombok.RequiredArgsConstructor;

/**
 * 마이페이지 프로필 (specs/009-mypage FR-001·FR-003~FR-009·FR-012, research R3~R7·R14).
 * 닉네임은 tb_user.name, 나머지는 tb_user_profile. 프로필 행은 처음 진입(조회)에서 만들지 않고 처음 고칠 때 만든다.
 * <p>
 * 고치는 메서드는 태그 유일 제약 위반(같은 닉네임 동시 저장)을 그대로 던져 부르는 쪽(Application)이 새 트랜잭션으로 다시 하게 한다.
 * 마지막 시도(lastAttempt)에서도 걸리면 B201로 바꾼다 (010 가입과 같은 규칙, {@link NicknameTags}).
 */
@Service
@RequiredArgsConstructor
public class ProfileService {

	static final int MIN_BIRTH_YEAR = 1900;
	static final int MAX_BIO_LENGTH = 500;
	/** 디스코드 사용자 이름: 소문자·숫자·_·. 2~32자, 마침표 연속 불가 (research R6) */
	static final Pattern DISCORD_TAG = Pattern.compile("^(?!.*\\.\\.)[a-z0-9_.]{2,32}$");

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

	/** 계정 바 아바타 키. 프로필이 없거나 목록에서 빠진 키면 default (FR-014, US4-AC1) */
	public String getAvatar(Long userId) {
		return userProfileReader.find(userId)
			.map(profile -> DefaultAvatar.displayKey(profile.getAvatarKey()))
			.orElse(DefaultAvatar.DEFAULT.key());
	}

	/**
	 * 닉네임 바꾸기 (FR-003·FR-004, US2, research R4·R5). 앞뒤 공백을 지운 값이 지금 이름과 같으면 아무것도 바꾸지 않는다.
	 * 첫 변경은 바로 되고, 그 뒤는 마지막 변경 + 쿨다운이 지나야 한다.
	 *
	 * @param lastAttempt 태그 충돌 때 다시 하지 않을 마지막 시도인지
	 * @throws ServiceException 규칙 V001(칸 nickname), 변경 제한 B202, 태그가 다 참 B201, 회원이 없으면 R001
	 * @throws DataIntegrityViolationException 마지막 시도가 아닌데 태그 유일 제약에 걸림
	 */
	public NicknameChangeDto changeNickname(Long userId, String rawNickname, boolean lastAttempt) {
		User user = requireUser(userId);
		if (NicknamePolicy.normalize(rawNickname).equals(user.getName())) {
			Optional<UserProfile> profile = userProfileReader.find(userId);
			return new NicknameChangeDto(user.getName(), profile.map(UserProfile::getNicknameTag).orElse(null),
				profile.map(found -> nicknameChangeableAt(found, LocalDateTime.now())).orElse(null));
		}
		String nickname = NicknamePolicy.validate(rawNickname);
		UserProfile profile = requireProfileForUpdate(userId, lastAttempt);
		LocalDateTime now = LocalDateTime.now();
		if (nicknameChangeableAt(profile, now) != null) {
			throw new ServiceException(ErrorCode.NICKNAME_CHANGE_TOO_SOON);
		}
		String key = NicknamePolicy.key(nickname);
		profile.changeNickname(key, nicknameTags.keepOrAssign(key, profile.getNicknameKey(), profile.getNicknameTag()));
		profile.markNicknameChanged(now);
		user.rename(nickname);
		saveFlushed(profile, lastAttempt);
		return new NicknameChangeDto(nickname, profile.getNicknameTag(), nicknameChangeableAt(profile, now));
	}

	/**
	 * 프로필 저장 (닉네임·게임 정보 제외, FR-006~FR-009·FR-012, US3·US4·US6). 모든 칸을 보낸 값으로 바꾼다.
	 * 맞지 않는 칸은 모아 한 번에 V001 칸 오류로 돌려준다.
	 *
	 * @param enabledPlayTimes 켜진 PLAY_TIME 코드 (Application이 공통코드에서 읽는다)
	 * @param lastAttempt 태그 충돌 때 다시 하지 않을 마지막 시도인지 (프로필 행을 새로 만들 때만 걸린다)
	 * @return 저장 뒤 내 프로필 (시간대 이름은 비어 있다)
	 * @throws ServiceException 칸 오류 V001, 태그가 다 참 B201, 회원이 없으면 R001
	 * @throws DataIntegrityViolationException 마지막 시도가 아닌데 태그 유일 제약에 걸림
	 */
	public ProfileDto save(Long userId, ProfileSaveCommand command, Set<String> enabledPlayTimes,
		boolean lastAttempt) {
		requireUser(userId);
		List<FieldError> errors = new ArrayList<>();
		String avatar = avatar(command.avatar(), errors);
		String bio = blankToNull(command.bio());
		if (bio != null && bio.length() > MAX_BIO_LENGTH) {
			errors.add(new FieldError("bio", "자기소개는 " + MAX_BIO_LENGTH + "자까지 쓸 수 있습니다."));
		}
		Short birthYear = birthYear(command.birthYear(), errors);
		Gender gender = parse(Gender.class, command.gender(), "gender", "성별", errors);
		String discordTag = discordTag(command.discordTag(), errors);
		DiscordVisibility discordVisibility = command.discordVisibility() == null ? DiscordVisibility.PRIVATE
			: parse(DiscordVisibility.class, command.discordVisibility(), "discordVisibility", "디스코드 공개 범위", errors);
		Set<String> playTimes = playTimes(command.playTimes(), enabledPlayTimes, errors);
		if (!errors.isEmpty()) {
			throw new ServiceException(ErrorCode.INVALID_INPUT, ErrorCode.INVALID_INPUT.getMessage(), errors);
		}
		UserProfile profile = requireProfileForUpdate(userId, lastAttempt);
		profile.update(avatar, bio, birthYear, gender, Boolean.TRUE.equals(command.ageGenderPublic()), discordTag,
			discordVisibility, playTimes);
		return getMyProfile(userId);
	}

	/**
	 * 고치기 전에 프로필 행을 잠가 가져온다. 없으면(키트 경로로 만든 회원) 지금 이름으로 키·태그를 정해 만든다 (research R14).
	 *
	 * @param lastAttempt 태그 충돌 때 다시 하지 않을 마지막 시도인지
	 * @throws ServiceException 회원이 없으면 R001, 그 닉네임의 태그가 다 찼거나 마지막 시도에서도 태그가 겹치면 B201,
	 *     마지막 시도에서도 같은 회원의 행이 동시에 만들어지면 R002
	 * @throws DataIntegrityViolationException 마지막 시도가 아닌데 새로 만들다 태그 유일 제약에 걸림
	 * @throws ProfileCreationConflictException 마지막 시도가 아닌데 같은 회원의 행이 동시에 만들어짐
	 */
	UserProfile requireProfileForUpdate(Long userId, boolean lastAttempt) {
		return userProfileReader.findForUpdate(userId).orElseGet(() -> create(userId, lastAttempt));
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

	/**
	 * 같은 회원이 동시에 처음 저장하면 PK(user_id) 충돌이나 교착이 난다. 다시 하면 행이 있어 고치기 경로로 가므로
	 * {@link ProfileCreationConflictException}으로 알리고, 마지막 시도면 R002 (research R14).
	 */
	private UserProfile create(Long userId, boolean lastAttempt) {
		String key = NicknamePolicy.key(requireUser(userId).getName());
		try {
			return saveFlushed(new UserProfile(userId, key, nicknameTags.assign(key)), lastAttempt);
		} catch (DataIntegrityViolationException | PessimisticLockingFailureException ex) {
			if (NicknameTags.isTagConflict(ex)) {
				throw ex;
			}
			if (lastAttempt) {
				throw new ServiceException(ErrorCode.DUPLICATE_RESOURCE, "프로필을 동시에 저장하는 요청이 있었습니다. 다시 시도해 주세요.");
			}
			throw new ProfileCreationConflictException(ex);
		}
	}

	private UserProfile saveFlushed(UserProfile profile, boolean lastAttempt) {
		try {
			return userProfileWriter.save(profile);
		} catch (DataIntegrityViolationException ex) {
			if (lastAttempt && NicknameTags.isTagConflict(ex)) {
				throw new ServiceException(ErrorCode.NICKNAME_TAGS_EXHAUSTED);
			}
			throw ex;
		}
	}

	/** 목록 밖의 키는 V001 (FR-009, research R10) */
	private static String avatar(String raw, List<FieldError> errors) {
		String key = blankToNull(raw);
		if (key == null) {
			return DefaultAvatar.DEFAULT.key();
		}
		if (DefaultAvatar.find(key).isEmpty()) {
			errors.add(new FieldError("avatar", "고를 수 있는 아바타가 아닙니다."));
		}
		return key;
	}

	/** 1900~올해. 만 14세 미만은 막지 않는다 (잠정, research R7) */
	private static Short birthYear(Integer year, List<FieldError> errors) {
		if (year == null) {
			return null;
		}
		int thisYear = LocalDate.now().getYear();
		if (year < MIN_BIRTH_YEAR || year > thisYear) {
			errors.add(new FieldError("birthYear", "출생 연도는 " + MIN_BIRTH_YEAR + "~" + thisYear + " 사이여야 합니다."));
			return null;
		}
		return year.shortValue();
	}

	/** 앞뒤 공백만 지운다. 대문자는 소문자로 바꾸지 않고 형식 오류다 (research R6) */
	private static String discordTag(String raw, List<FieldError> errors) {
		String tag = blankToNull(raw);
		if (tag != null && !DISCORD_TAG.matcher(tag).matches()) {
			errors.add(new FieldError("discordTag", "디스코드 사용자 이름은 2~32자의 영문 소문자, 숫자, _, .만 쓸 수 있습니다."));
		}
		return tag;
	}

	/** 켜진 코드만, 겹치면 하나로 (FR-012, US6) */
	private static Set<String> playTimes(List<String> codes, Set<String> enabled, List<FieldError> errors) {
		Set<String> result = new LinkedHashSet<>();
		if (codes == null) {
			return result;
		}
		for (String code : codes) {
			if (code == null || !enabled.contains(code)) {
				errors.add(new FieldError("playTimes", "고를 수 없는 시간대입니다: " + code));
				continue;
			}
			result.add(code);
		}
		return result;
	}

	private static <E extends Enum<E>> E parse(Class<E> type, String raw, String field, String label,
		List<FieldError> errors) {
		if (raw == null) {
			return null;
		}
		try {
			return Enum.valueOf(type, raw);
		} catch (IllegalArgumentException ex) {
			errors.add(new FieldError(field, label + " 값이 올바르지 않습니다."));
			return null;
		}
	}

	private static String blankToNull(String value) {
		if (value == null) {
			return null;
		}
		String stripped = value.strip();
		return stripped.isEmpty() ? null : stripped;
	}

	private User requireUser(Long userId) {
		return userReader.find(userId).orElseThrow(() -> new ServiceException(ErrorCode.NOT_FOUND));
	}
}
