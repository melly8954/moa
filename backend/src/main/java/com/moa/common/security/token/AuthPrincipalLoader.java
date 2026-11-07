package com.moa.common.security.token;

import com.moa.common.security.AuthPrincipal;

/**
 * 토큰 재발급 때 사용자의 현재 역할을 다시 읽는다. 사용자 도메인이 구현해 빈으로 등록한다.
 * 구현이 없으면 재발급은 S001로 실패한다.
 */
public interface AuthPrincipalLoader {

	/**
	 * @throws com.moa.common.exception.ServiceException 사용자가 없거나 로그인할 수 없는 상태면 A003
	 */
	AuthPrincipal load(Long userId);
}
