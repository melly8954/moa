package com.moa.api.service.user;

import org.springframework.stereotype.Component;

import com.moa.api.entity.user.User;
import com.moa.api.repository.reader.user.UserReader;
import com.moa.common.exception.ErrorCode;
import com.moa.common.exception.ServiceException;
import com.moa.common.security.Role;

/**
 * 관리자·운영자 계정은 회원 활동을 하지 않는다 (specs/009-mypage FR-016·research R2, specs/010-social-login FR-007).
 * 관리자·운영자 계정 = 계정 종류 ADMIN. 회원(USER)은 업무 역할을 받지 않으므로(003-admin-roles FR-002)
 * "ADMIN이거나 업무 역할을 가진 계정"과 같다. 010 계정 설정(socialLinkAllowed)도 이 판단을 쓴다.
 */
@Component
public class MemberActivityPolicy {

	private final UserReader userReader;

	public MemberActivityPolicy(UserReader userReader) {
		this.userReader = userReader;
	}

	/**
	 * @throws ServiceException 회원이 없으면 R001
	 */
	public boolean isManagerAccount(Long userId) {
		return requireUser(userId).getRole() == Role.ADMIN;
	}

	/**
	 * 마이페이지 API 앞에서 부른다.
	 *
	 * @throws ServiceException 관리자 계정이면 A201, 회원이 없으면 R001
	 */
	public void requireMypageAllowed(Long userId) {
		if (isManagerAccount(userId)) {
			throw new ServiceException(ErrorCode.MYPAGE_NOT_ALLOWED);
		}
	}

	/**
	 * 파티 만들기·참가·신청·초대 입장·내 파티 앞에서 부른다 (specs/008-party FR-012, research R14).
	 *
	 * @throws ServiceException 관리자 계정이면 A103, 회원이 없으면 R001
	 */
	public void requirePartyAllowed(Long userId) {
		if (isManagerAccount(userId)) {
			throw new ServiceException(ErrorCode.PARTY_NOT_ALLOWED);
		}
	}

	/**
	 * 회원 문의 API 앞에서 부른다 (specs/017-customer-center FR-011·research R4). 관리자 계정은 사용자 셸을 열 수 없어
	 * 이 오류를 화면에서 볼 일이 없으므로 따로 코드를 두지 않고 A005로 막는다.
	 *
	 * @throws ServiceException 관리자 계정이면 A005, 회원이 없으면 R001
	 */
	public void requireMember(Long userId) {
		if (isManagerAccount(userId)) {
			throw new ServiceException(ErrorCode.FORBIDDEN);
		}
	}

	private User requireUser(Long userId) {
		return userReader.find(userId).orElseThrow(() -> new ServiceException(ErrorCode.NOT_FOUND));
	}
}
