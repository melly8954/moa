package com.moa.api.repository.reader;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.moa.api.entity.PhoneVerification;
import com.moa.api.repository.jpa.PhoneVerificationRepository;

import lombok.RequiredArgsConstructor;

/**
 * 휴대폰 인증 조회.
 */
@Component
@RequiredArgsConstructor
public class PhoneVerificationReader {

	private final PhoneVerificationRepository phoneVerificationRepository;

	public Optional<PhoneVerification> findById(Long id) {
		return phoneVerificationRepository.findById(id);
	}

	/** 그 번호로 가장 최근에 보낸 인증 */
	public Optional<PhoneVerification> findLatestByPhoneHmac(String phoneHmac) {
		return phoneVerificationRepository.findTopByPhoneHmacOrderByIdDesc(phoneHmac);
	}

	public long countByPhoneHmacSince(String phoneHmac, LocalDateTime since) {
		return phoneVerificationRepository.countByPhoneHmacAndCreatedAtAfter(phoneHmac, since);
	}

	/** 가입 진행의 가장 최근 인증 번호 */
	public Optional<PhoneVerification> findLatestForUpdateBySignUpId(Long signUpId) {
		return phoneVerificationRepository.findLatestForUpdateBySignUpId(signUpId);
	}
}
