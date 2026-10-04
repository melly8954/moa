package com.moa.api.repository.reader;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.moa.api.entity.EmailVerification;
import com.moa.api.repository.jpa.EmailVerificationRepository;

import lombok.RequiredArgsConstructor;

/**
 * 이메일 인증 조회.
 */
@Component
@RequiredArgsConstructor
public class EmailVerificationReader {

	private final EmailVerificationRepository emailVerificationRepository;

	public Optional<EmailVerification> findById(Long id) {
		return emailVerificationRepository.findById(id);
	}

	public Optional<EmailVerification> findForUpdateByTokenHash(String tokenHash) {
		return emailVerificationRepository.findForUpdateByTokenHash(tokenHash);
	}

	/** 그 이메일로 가장 최근에 보낸 인증 */
	public Optional<EmailVerification> findLatestByEmail(String email) {
		return emailVerificationRepository.findTopByEmailOrderByIdDesc(email);
	}
}
