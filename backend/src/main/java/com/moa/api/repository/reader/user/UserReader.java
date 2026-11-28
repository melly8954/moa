package com.moa.api.repository.reader.user;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
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

	/** 활동 중인 회원만. 탈퇴(유예 중)·파기된 회원은 빠진다 */
	public List<User> findAll(Collection<Long> ids) {
		return ids.isEmpty() ? List.of() : userRepository.findByIdIn(ids);
	}

	/** moa 018: 유예 중인 회원도 나온다 (아이디는 파기할 때까지 붙잡는다) */
	public Optional<User> findByLoginId(String loginId) {
		return userRepository.findByLoginId(loginId);
	}

	/** moa 018: 유예 중인 회원도 나온다 (복구). 관리자가 지운 회원은 나오지 않는다 */
	public Optional<User> findIncludingWithdrawn(Long id) {
		return userRepository.findIncludingWithdrawn(id);
	}

	/** moa 018: 탈퇴·복구 전에 행을 잠가 읽는다. 트랜잭션 안에서 부른다 */
	public Optional<User> findForUpdateIncludingWithdrawn(Long id) {
		return userRepository.findForUpdateIncludingWithdrawn(id);
	}

	/** moa 018: 지운 회원과 파기된 회원까지 */
	public Optional<User> findInAnyState(Long id) {
		return userRepository.findInAnyState(id);
	}

	/** moa 018: 스스로 탈퇴한 회원인지 (유예 중, 파기됨) */
	public boolean existsWithdrawn(Long id) {
		return userRepository.countWithdrawn(id) > 0;
	}

	/** moa 017: 이 시각 전에 스스로 탈퇴한 회원 ID (유예 중, 파기됨) */
	public List<Long> findIdsWithdrawnBefore(LocalDateTime cutoff) {
		return userRepository.findIdsWithdrawnBefore(cutoff);
	}

	/** 지우지 않은 ADMIN이 하나라도 있는지 */
	public boolean existsAdmin() {
		return userRepository.existsByRole(Role.ADMIN);
	}
}
