package com.moa.api.service.user;

import java.util.Locale;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.moa.api.entity.user.UserSocialAccount;
import com.moa.api.repository.reader.user.UserSocialAccountReader;
import com.moa.common.exception.ErrorCode;
import com.moa.common.exception.ServiceException;
import com.moa.common.security.oauth.OAuthIdentity;

import lombok.RequiredArgsConstructor;

/**
 * 외부(소셜) 계정 연결: 찾기, 가입 확인 (specs/010-social-login research R12).
 * 가입·연결·해제와 관리자·운영자 판단은 그 스토리를 만들 때 더한다.
 */
@Service
@RequiredArgsConstructor
public class SocialAccountService {

	private final UserSocialAccountReader userSocialAccountReader;

	/** 이 소셜 계정이 연결된 회원 */
	public Optional<Long> findLinkedUserId(OAuthIdentity identity) {
		return userSocialAccountReader.findByProviderAndSubject(identity.provider(), identity.subject())
			.map(UserSocialAccount::getUserId);
	}

	/** 소셜 계정이 하나라도 연결된 회원인지 (FR-007) */
	public boolean isSocialMember(Long userId) {
		return userSocialAccountReader.existsByUserId(userId);
	}

	/**
	 * 연결된 회원이 없는 소셜 계정으로 가입해도 되는지 (FR-014). 재가입 제한(FR-015)은 다음 단계에서 더한다.
	 *
	 * @return 정규화한 이메일
	 * @throws ServiceException 이메일이 없거나 제공자가 인증하지 않았으면 A014, 다른 회원이 쓰는 이메일이면 A015
	 */
	public String checkSignUp(OAuthIdentity identity) {
		String email = normalizeEmail(identity.email());
		if (email == null || !identity.emailVerified()) {
			throw new ServiceException(ErrorCode.SOCIAL_EMAIL_UNVERIFIED);
		}
		if (userSocialAccountReader.existsByEmail(email)) {
			throw new ServiceException(ErrorCode.SOCIAL_EMAIL_IN_USE);
		}
		return email;
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
