package com.moa.api.repository.writer;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import com.moa.api.entity.SignUp;
import com.moa.api.repository.jpa.SignUpRepository;

import lombok.RequiredArgsConstructor;

/**
 * 가입 진행 변경. 하드 삭제한다.
 */
@Component
@RequiredArgsConstructor
public class SignUpWriter {

	private final SignUpRepository signUpRepository;

	public SignUp create(SignUp signUp) {
		return signUpRepository.save(signUp);
	}

	public void delete(SignUp signUp) {
		signUpRepository.delete(signUp);
	}

	public int deleteExpiredBefore(LocalDateTime before) {
		return signUpRepository.deleteExpiredBefore(before);
	}
}
