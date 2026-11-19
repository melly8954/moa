package com.moa.api.repository.reader.user;

import java.util.Collection;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.moa.api.entity.user.UserSocialAccount;
import com.moa.api.repository.jpa.user.UserSocialAccountRepository;
import com.moa.common.security.oauth.SocialProvider;

import lombok.RequiredArgsConstructor;

/**
 * 외부 계정 연결 조회 (specs/010-social-login).
 */
@Component
@RequiredArgsConstructor
public class UserSocialAccountReader {

	private final UserSocialAccountRepository repository;

	public Optional<UserSocialAccount> findByProviderAndSubject(SocialProvider provider, String subject) {
		return repository.findByProviderAndSubject(provider, subject);
	}

	/** 소셜 계정이 하나라도 연결된 회원인지 (FR-007) */
	public boolean existsByUserId(Long userId) {
		return repository.existsByUserId(userId);
	}

	/** 이 회원들 중 소셜 회원이 하나라도 있는지 (FR-007) */
	public boolean existsByUserIdIn(Collection<Long> userIds) {
		return !userIds.isEmpty() && repository.existsByUserIdIn(userIds);
	}

	/** 이 이메일(정규화한 값)을 쓰는 연결이 있는지 (FR-014) */
	public boolean existsByEmail(String email) {
		return repository.existsByEmail(email);
	}
}
