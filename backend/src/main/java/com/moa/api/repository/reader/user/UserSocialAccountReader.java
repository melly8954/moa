package com.moa.api.repository.reader.user;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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

	/**
	 * moa: 회원마다 연결된 제공자 ({@link SocialProvider} 순서). 연결이 없는 회원은 빠진다. 한 번에 읽는다
	 * (specs/002-admin-users research R17)
	 */
	public Map<Long, List<SocialProvider>> providersOf(Collection<Long> userIds) {
		if (userIds.isEmpty()) {
			return Map.of();
		}
		return repository.findAllByUserIdIn(userIds).stream()
			.sorted(Comparator.comparing(UserSocialAccount::getProvider))
			.collect(Collectors.groupingBy(UserSocialAccount::getUserId,
				Collectors.mapping(UserSocialAccount::getProvider, Collectors.toList())));
	}

	/** 이 이메일(정규화한 값)을 쓰는 연결이 있는지 (FR-014) */
	public boolean existsByEmail(String email) {
		return repository.existsByEmail(email);
	}
}
