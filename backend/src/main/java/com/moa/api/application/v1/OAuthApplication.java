package com.moa.api.application.v1;

import org.springframework.stereotype.Component;

import com.moa.api.application.v1.command.HandleOAuthCallbackCommand;
import com.moa.api.dto.OAuthAuthorizationDto;
import com.moa.api.dto.OAuthCallbackDto;

/**
 * 구글·카카오 인증 API의 진입점. 가입과 로그인이 같은 콜백을 쓴다.
 */
@Component
public class OAuthApplication {

	private static final String NOT_IMPLEMENTED = "구현 전입니다";

	/**
	 * @param provider 경로의 제공자 값 (google, kakao)
	 * @param intent 시작한 화면. 실패했을 때 돌아갈 곳을 정한다 (SIGN_UP, SIGN_IN)
	 * @throws com.moa.common.exception.ServiceException 모르는 제공자면 R001
	 */
	public OAuthAuthorizationDto authorize(String provider, String intent) {
		throw new UnsupportedOperationException(NOT_IMPLEMENTED);
	}

	/**
	 * 콜백을 처리한다. 오류는 예외로 던지지 않고 실패 주소(error=오류 코드)로 돌려준다.
	 * <ul>
	 *   <li>state 불일치, 취소, 코드 교환 실패: A007</li>
	 *   <li>같은 이메일 계정이 있으나 제공자가 인증된 이메일로 알려 주지 않음: B003</li>
	 * </ul>
	 */
	public OAuthCallbackDto handleCallback(HandleOAuthCallbackCommand command) {
		throw new UnsupportedOperationException(NOT_IMPLEMENTED);
	}
}
