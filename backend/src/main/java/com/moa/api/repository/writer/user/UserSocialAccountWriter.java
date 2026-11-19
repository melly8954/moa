package com.moa.api.repository.writer.user;

import org.springframework.stereotype.Component;

import com.moa.api.entity.user.UserSocialAccount;
import com.moa.api.repository.jpa.user.UserSocialAccountRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UserSocialAccountWriter {

	private final UserSocialAccountRepository repository;

	/** 유일 제약((provider, subject), (user_id, provider)) 위반을 바로 알도록 flush한다 */
	public UserSocialAccount save(UserSocialAccount account) {
		return repository.saveAndFlush(account);
	}
}
