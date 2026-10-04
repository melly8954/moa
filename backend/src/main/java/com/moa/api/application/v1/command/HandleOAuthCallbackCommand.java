package com.moa.api.application.v1.command;

/**
 * 구글·카카오 인증 콜백.
 *
 * @param provider 경로의 제공자 값 (google, kakao)
 * @param code 인가 코드. 실패·취소면 null
 * @param state 제공자가 돌려준 state
 * @param expectedState 브라우저 쿠키에 둔 state
 * @param error 제공자가 준 오류 (취소 등). 없으면 null
 */
public record HandleOAuthCallbackCommand(String provider, String code, String state, String expectedState,
	String error) {
}
