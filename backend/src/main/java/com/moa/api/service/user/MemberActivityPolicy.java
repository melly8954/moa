package com.moa.api.service.user;

import org.springframework.stereotype.Service;

import com.moa.api.repository.reader.user.UserReader;
import com.moa.common.exception.ErrorCode;
import com.moa.common.exception.ServiceException;
import com.moa.common.security.Role;

import lombok.RequiredArgsConstructor;

/**
 * 관리자·운영자 계정은 회원 활동을 하지 않는다 (specs/README.md 맞춰야 할 경계, specs/009-mypage research R2).
 * 판단은 시스템 역할이 ADMIN인지다. 회원(USER)은 업무 역할을 받지 않으므로 "ADMIN이거나 업무 역할을 가진 계정"과 같다.
 * 009·008보다 017(specs/017-customer-center research R4)이 먼저 구현되어 009의 설계대로 여기 만든다.
 */
@Service
@RequiredArgsConstructor
public class MemberActivityPolicy {

	private final UserReader userReader;

	/** 관리자·운영자 계정이면 참. 없는 사용자는 거짓 */
	public boolean isManagerAccount(Long userId) {
		return userReader.find(userId).map(user -> user.getRole() == Role.ADMIN).orElse(false);
	}

	/**
	 * 회원 활동을 하려는 계정이 관리자·운영자 계정이면 막는다.
	 *
	 * @throws ServiceException 관리자·운영자 계정이면 A005
	 */
	public void requireMember(Long userId) {
		if (isManagerAccount(userId)) {
			throw new ServiceException(ErrorCode.FORBIDDEN);
		}
	}
}
