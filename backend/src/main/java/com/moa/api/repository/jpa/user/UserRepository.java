package com.moa.api.repository.jpa.user;

import java.util.Optional;

import com.moa.api.entity.user.User;
import com.moa.common.repository.SoftDeleteRepository;
import com.moa.common.security.Role;

public interface UserRepository extends SoftDeleteRepository<User, Long> {

	Optional<User> findByLoginId(String loginId);

	boolean existsByRole(Role role);
}
