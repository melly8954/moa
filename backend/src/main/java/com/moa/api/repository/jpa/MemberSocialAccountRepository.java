package com.moa.api.repository.jpa;

import java.util.Optional;

import com.moa.api.entity.MemberSocialAccount;
import com.moa.common.oauth.OAuthProvider;
import com.moa.common.repository.SoftDeleteRepository;

public interface MemberSocialAccountRepository extends SoftDeleteRepository<MemberSocialAccount, Long> {

	Optional<MemberSocialAccount> findByProviderAndProviderUserId(OAuthProvider provider, String providerUserId);

	boolean existsByMemberIdAndProvider(Long memberId, OAuthProvider provider);
}
