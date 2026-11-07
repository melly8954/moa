package com.moa.common.security;

/**
 * 로그인 시도. 성공·실패 모두 낸다. 골격에는 받는 쪽이 없고, 선택 헬퍼 login-history가 받아 접속 이력을 남긴다.
 * 요청 스레드에서 동기로 전달되므로 받는 쪽은 RequestContextHolder로 IP·User-Agent를 읽을 수 있다.
 *
 * @param loginId 입력한 아이디
 * @param userId 성공하면 사용자 ID, 실패하면 null
 * @param success 성공 여부
 * @param failureCode 실패한 에러 코드 (예: A006). 성공하면 null
 */
public record LoginAttemptedEvent(String loginId, Long userId, boolean success, String failureCode) {

	public static LoginAttemptedEvent succeeded(String loginId, Long userId) {
		return new LoginAttemptedEvent(loginId, userId, true, null);
	}

	public static LoginAttemptedEvent failed(String loginId, String failureCode) {
		return new LoginAttemptedEvent(loginId, null, false, failureCode);
	}
}
