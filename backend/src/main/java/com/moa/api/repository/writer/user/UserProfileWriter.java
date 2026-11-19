package com.moa.api.repository.writer.user;

import org.springframework.stereotype.Component;

import com.moa.api.entity.user.UserProfile;
import com.moa.api.repository.jpa.user.UserProfileRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UserProfileWriter {

	private final UserProfileRepository repository;

	/** (nickname_key, nickname_tag) 유일 제약 위반을 바로 알도록 flush한다 (NicknameTags 다시 하기) */
	public UserProfile save(UserProfile profile) {
		return repository.saveAndFlush(profile);
	}
}
