package com.moa.api.repository.writer;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import com.moa.api.entity.EmailVerification;
import com.moa.api.repository.jpa.EmailVerificationRepository;

import lombok.RequiredArgsConstructor;

/**
 * 이메일 인증 변경. 하드 삭제한다.
 */
@Component
@RequiredArgsConstructor
public class EmailVerificationWriter {

	private final EmailVerificationRepository emailVerificationRepository;

	public EmailVerification create(EmailVerification verification) {
		return emailVerificationRepository.save(verification);
	}

	public int deleteExpiredBefore(LocalDateTime before) {
		return emailVerificationRepository.deleteExpiredBefore(before);
	}

	public void delete(Long id) {
		emailVerificationRepository.deleteById(id);
	}
}
