package com.moa.api.repository.reader.user;

import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.moa.api.entity.user.UserProfile;
import com.moa.api.repository.jpa.user.UserProfileRepository;

import lombok.RequiredArgsConstructor;

/**
 * 프로필 조회 (specs/009-mypage research R4, specs/010-social-login research R14).
 */
@Component
@RequiredArgsConstructor
public class UserProfileReader {

	private final UserProfileRepository repository;

	public Optional<UserProfile> find(Long userId) {
		return repository.findById(userId);
	}

	/** 이 닉네임 키에서 쓰이는 태그 */
	public Set<String> findUsedTags(String nicknameKey) {
		return Set.copyOf(repository.findTagsByNicknameKey(nicknameKey));
	}
}
