package com.moa.api.repository.writer;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import com.moa.api.entity.PhoneVerification;
import com.moa.api.repository.jpa.PhoneVerificationRepository;

import lombok.RequiredArgsConstructor;

/**
 * 휴대폰 인증 변경. 하드 삭제한다.
 */
@Component
@RequiredArgsConstructor
public class PhoneVerificationWriter {

	private final PhoneVerificationRepository phoneVerificationRepository;

	public PhoneVerification create(PhoneVerification verification) {
		return phoneVerificationRepository.save(verification);
	}

	public int deleteCreatedBefore(LocalDateTime before) {
		return phoneVerificationRepository.deleteCreatedBefore(before);
	}

	public void delete(Long id) {
		phoneVerificationRepository.deleteById(id);
	}
}
