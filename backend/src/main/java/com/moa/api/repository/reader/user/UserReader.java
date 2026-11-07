package com.moa.api.repository.reader.user;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.moa.api.entity.user.User;
import com.moa.api.repository.jpa.user.UserRepository;
import com.moa.common.security.Role;

import lombok.RequiredArgsConstructor;

/**
 * 사용자 조회. 없으면 빈 Optional을 돌려주고, 오류 판단은 Service가 한다.
 */
@Component
@RequiredArgsConstructor
public class UserReader {

	private final UserRepository userRepository;

	public Optional<User> find(Long id) {
		return userRepository.findById(id);
	}

	public Optional<User> findByLoginId(String loginId) {
		return userRepository.findByLoginId(loginId);
	}

	/** 지우지 않은 ADMIN이 하나라도 있는지 */
	public boolean existsAdmin() {
		return userRepository.existsByRole(Role.ADMIN);
	}
}
