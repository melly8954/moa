package com.moa.common.security.access;

/**
 * 토큰이 유효해도 막아야 하는 계정인지 (moa 018, specs/018-member-withdrawal research R2). 구현은 회원 도메인이 한다.
 */
@FunctionalInterface
public interface AccountAccessChecker {

	boolean isBlocked(Long userId);
}
