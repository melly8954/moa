package com.moa.api.service.user;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.moa.api.dto.user.SocialLoginUserDto;
import com.moa.api.dto.user.SocialSignUpCheck;
import com.moa.api.dto.user.TermsAgreementDto;
import com.moa.api.dto.user.UserDto;
import com.moa.api.entity.user.User;
import com.moa.api.entity.user.UserProfile;
import com.moa.api.entity.user.UserSocialAccount;
import com.moa.api.entity.user.UserTermsAgreement;
import com.moa.api.repository.reader.user.UserReader;
import com.moa.api.repository.reader.user.UserSocialAccountReader;
import com.moa.api.repository.writer.user.UserProfileWriter;
import com.moa.api.repository.writer.user.UserSocialAccountWriter;
import com.moa.api.repository.writer.user.UserTermsAgreementWriter;
import com.moa.api.repository.writer.user.UserWriter;
import com.moa.common.exception.ErrorCode;
import com.moa.common.exception.ServiceException;
import com.moa.common.security.Role;
import com.moa.common.security.oauth.OAuthIdentity;
import com.moa.common.security.oauth.OAuthSignup;
import com.moa.common.security.oauth.SocialProvider;
import com.moa.common.terms.TermsProperties;
import com.moa.common.terms.TermsType;

import lombok.RequiredArgsConstructor;

/**
 * 외부(소셜) 계정 연결: 찾기, 가입 확인, 가입 (specs/010-social-login research R5·R12·R14).
 * 연결·해제와 관리자·운영자 판단은 US5에서 더한다. 재가입 제한(FR-015)은 다음 단계에서 더한다.
 */
@Service
@RequiredArgsConstructor
public class SocialAccountService {

	/** 소셜 가입 회원의 로그인 아이디 접두사. 화면에 보이지 않는다 (잠정, FR-004 (c)) */
	static final String LOGIN_ID_PREFIX = "s_";
	private static final int LOGIN_ID_RANDOM_LENGTH = 12;
	private static final String LOGIN_ID_CHARS = "abcdefghijklmnopqrstuvwxyz0123456789";
	private static final String SOCIAL_ACCOUNT_CONSTRAINT = "uk_user_social_account_provider_subject";
	private static final SecureRandom RANDOM = new SecureRandom();

	private final UserReader userReader;
	private final UserWriter userWriter;
	private final UserSocialAccountReader userSocialAccountReader;
	private final UserSocialAccountWriter userSocialAccountWriter;
	private final UserProfileWriter userProfileWriter;
	private final UserTermsAgreementWriter userTermsAgreementWriter;
	private final NicknameTags nicknameTags;
	private final TermsProperties termsProperties;

	/** 이 소셜 계정이 연결된 회원 (FR-003). moa 018: 탈퇴(유예 중·파기 대기)한 회원도 나온다 (관리자가 지운 회원은 아니다) */
	public Optional<SocialLoginUserDto> findLinkedUser(OAuthIdentity identity) {
		return userSocialAccountReader.findByProviderAndSubject(identity.provider(), identity.subject())
			.flatMap(account -> userReader.findIncludingWithdrawn(account.getUserId()))
			.map(user -> new SocialLoginUserDto(UserDto.from(user), user.isLocked(), user.getWithdrawnAt()));
	}

	/** 소셜 계정이 하나라도 연결된 회원인지 (FR-007) */
	public boolean isSocialMember(Long userId) {
		return userSocialAccountReader.existsByUserId(userId);
	}

	/** 회원마다 연결된 제공자. 연결이 없는 회원은 빠진다 (specs/002-admin-users FR-004, research R17) */
	public Map<Long, List<SocialProvider>> providersOf(Collection<Long> userIds) {
		return userSocialAccountReader.providersOf(userIds);
	}

	/**
	 * 연결된 회원이 없는 소셜 계정으로 가입해도 되는지 (FR-014). 콜백이 결과대로 화면을 고른다.
	 */
	public SocialSignUpCheck checkSignUp(OAuthIdentity identity) {
		String email = normalizeEmail(identity.email());
		if (email == null || !identity.emailVerified()) {
			return new SocialSignUpCheck(SocialSignUpCheck.Result.EMAIL_UNVERIFIED, email);
		}
		if (userSocialAccountReader.existsByEmail(email)) {
			return new SocialSignUpCheck(SocialSignUpCheck.Result.EMAIL_IN_USE, email);
		}
		return new SocialSignUpCheck(SocialSignUpCheck.Result.ALLOWED, email);
	}

	/**
	 * @throws ServiceException 가입 대기 쿠키가 없거나 만료·위조면 A009
	 */
	public OAuthSignup requireSignup(Optional<OAuthSignup> signup) {
		return signup.orElseThrow(() -> new ServiceException(ErrorCode.OAUTH_FAILED));
	}

	/**
	 * 닉네임·약관 동의를 받아 회원을 만든다 (FR-002·FR-005·FR-013, research R5·R14). 회원·프로필(태그)·연결·동의 2행을
	 * 부르는 쪽의 한 트랜잭션에서 만든다.
	 * <p>
	 * 태그 유일 제약 위반(같은 닉네임 동시 가입)은 그대로 던져 부르는 쪽이 새 트랜잭션으로 다시 하게 한다.
	 * 마지막 시도에서도 걸리면 B201로 바꾼다.
	 *
	 * @param lastAttempt 태그 충돌 때 다시 하지 않을 마지막 시도인지
	 * @throws ServiceException 닉네임 규칙 V001, 약관 A017, 이메일 중복 A015, 같은 소셜 계정이 이미 가입 A012,
	 *     태그가 다 참 B201
	 * @throws DataIntegrityViolationException 마지막 시도가 아닌데 태그 유일 제약에 걸림
	 */
	public UserDto signUp(OAuthSignup signup, String rawNickname, List<TermsAgreementDto> agreements,
		boolean lastAttempt) {
		String nickname = NicknamePolicy.validate(rawNickname);
		Map<TermsType, String> agreed = requireCurrentTerms(agreements);
		if (userSocialAccountReader.existsByEmail(signup.email())) {
			throw new ServiceException(ErrorCode.SOCIAL_EMAIL_IN_USE);
		}
		LocalDateTime now = LocalDateTime.now();

		User user = userWriter.save(new User(newLoginId(), null, nickname, Role.USER));
		saveProfile(user.getId(), nickname, lastAttempt);
		saveSocialAccount(new UserSocialAccount(user.getId(), signup.provider(), signup.subject(), signup.email(),
			signup.name(), now));
		userTermsAgreementWriter.saveAll(agreed.entrySet().stream()
			.map(entry -> new UserTermsAgreement(user.getId(), entry.getKey(), entry.getValue(), now))
			.toList());
		return UserDto.from(user);
	}

	/** 필수 약관 두 종류가 모두 있고 현재 버전과 같아야 한다 (FR-013, research R11) */
	private Map<TermsType, String> requireCurrentTerms(List<TermsAgreementDto> agreements) {
		Map<TermsType, String> agreed = new EnumMap<>(TermsType.class);
		if (agreements != null) {
			for (TermsAgreementDto agreement : agreements) {
				parseType(agreement.type()).ifPresent(type -> agreed.put(type, agreement.version()));
			}
		}
		for (TermsType type : TermsType.values()) {
			if (!termsProperties.versionOf(type).equals(agreed.get(type))) {
				throw new ServiceException(ErrorCode.TERMS_NOT_AGREED);
			}
		}
		return agreed;
	}

	private void saveProfile(Long userId, String nickname, boolean lastAttempt) {
		String key = NicknamePolicy.key(nickname);
		try {
			userProfileWriter.save(new UserProfile(userId, key, nicknameTags.assign(key)));
		} catch (DataIntegrityViolationException ex) {
			if (lastAttempt && NicknameTags.isTagConflict(ex)) {
				throw new ServiceException(ErrorCode.NICKNAME_TAGS_EXHAUSTED);
			}
			throw ex;
		}
	}

	private void saveSocialAccount(UserSocialAccount account) {
		try {
			userSocialAccountWriter.save(account);
		} catch (DataIntegrityViolationException ex) {
			if (mentions(ex, SOCIAL_ACCOUNT_CONSTRAINT)) {
				throw new ServiceException(ErrorCode.SOCIAL_ACCOUNT_IN_USE);
			}
			throw ex;
		}
	}

	/** s_ + 영소문자·숫자 12자. 지우지 않은 회원과 겹치면 다시 */
	private String newLoginId() {
		while (true) {
			StringBuilder id = new StringBuilder(LOGIN_ID_PREFIX);
			for (int i = 0; i < LOGIN_ID_RANDOM_LENGTH; i++) {
				id.append(LOGIN_ID_CHARS.charAt(RANDOM.nextInt(LOGIN_ID_CHARS.length())));
			}
			if (userReader.findByLoginId(id.toString()).isEmpty()) {
				return id.toString();
			}
		}
	}

	private static Optional<TermsType> parseType(String value) {
		for (TermsType type : TermsType.values()) {
			if (type.name().equals(value)) {
				return Optional.of(type);
			}
		}
		return Optional.empty();
	}

	private static boolean mentions(Throwable ex, String constraint) {
		for (Throwable cause = ex; cause != null && cause.getCause() != cause; cause = cause.getCause()) {
			if (cause.getMessage() != null && cause.getMessage().contains(constraint)) {
				return true;
			}
		}
		return false;
	}

	/** 앞뒤 공백 제거, 소문자 (research R12). 비면 null */
	static String normalizeEmail(String email) {
		if (email == null) {
			return null;
		}
		String normalized = email.strip().toLowerCase(Locale.ROOT);
		return normalized.isEmpty() ? null : normalized;
	}
}
