package com.moa.api.service.user;

import org.springframework.stereotype.Component;

import com.moa.api.repository.reader.user.UserReader;
import com.moa.common.security.access.AccountAccessChecker;

import lombok.RequiredArgsConstructor;

/** 탈퇴한 회원의 요청을 막는다 (specs/018-member-withdrawal FR-006) */
@Component
@RequiredArgsConstructor
public class WithdrawnAccountChecker implements AccountAccessChecker {

	private final UserReader userReader;

	@Override
	public boolean isBlocked(Long userId) {
		return userReader.existsWithdrawn(userId);
	}
}
