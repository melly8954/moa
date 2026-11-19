package com.moa.api.repository.jpa.user;

import java.util.Collection;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.moa.api.entity.user.UserSocialAccount;
import com.moa.common.security.oauth.SocialProvider;

public interface UserSocialAccountRepository extends JpaRepository<UserSocialAccount, Long> {

	Optional<UserSocialAccount> findByProviderAndSubject(SocialProvider provider, String subject);

	boolean existsByUserId(Long userId);

	boolean existsByUserIdIn(Collection<Long> userIds);

	boolean existsByEmail(String email);
}
