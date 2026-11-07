package com.moa.api.repository.writer.user;

import org.springframework.stereotype.Component;

import com.moa.api.entity.user.User;
import com.moa.api.repository.jpa.user.UserRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UserWriter {

	private final UserRepository userRepository;

	public User save(User user) {
		return userRepository.save(user);
	}
}
