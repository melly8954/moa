package com.moa.api.repository.reader;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.moa.api.entity.SignUp;
import com.moa.api.repository.jpa.SignUpRepository;

import lombok.RequiredArgsConstructor;

/**
 * 가입 진행 조회.
 */
@Component
@RequiredArgsConstructor
public class SignUpReader {

	private final SignUpRepository signUpRepository;

	public Optional<SignUp> findById(Long id) {
		return signUpRepository.findById(id);
	}

	public Optional<SignUp> findByTokenHash(String tokenHash) {
		return signUpRepository.findByTokenHash(tokenHash);
	}
}
