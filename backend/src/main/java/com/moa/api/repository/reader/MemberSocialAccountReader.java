package com.moa.api.repository.reader;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.moa.api.entity.MemberSocialAccount;
import com.moa.api.repository.jpa.MemberSocialAccountRepository;
import com.moa.common.oauth.OAuthProvider;

import lombok.RequiredArgsConstructor;

/**
 * 연결된 소셜 계정 조회.
 */
@Component
@RequiredArgsConstructor
public class MemberSocialAccountReader {

	private final MemberSocialAccountRepository memberSocialAccountRepository;

	public Optional<MemberSocialAccount> find(OAuthProvider provider, String providerUserId) {
		return memberSocialAccountRepository.findByProviderAndProviderUserId(provider, providerUserId);
	}

	public boolean existsByMemberAndProvider(Long memberId, OAuthProvider provider) {
		return memberSocialAccountRepository.existsByMemberIdAndProvider(memberId, provider);
	}
}
