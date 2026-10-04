package com.moa.api.application.v1;

import org.springframework.stereotype.Component;

import com.moa.api.application.v1.command.HandleOAuthCallbackCommand;
import com.moa.api.dto.OAuthAuthorizationDto;
import com.moa.api.dto.OAuthCallbackDto;
import com.moa.api.service.OAuthService;
import com.moa.common.exception.ErrorCode;
import com.moa.common.exception.ServiceException;
import com.moa.common.logging.AuthAuditLogger;
import com.moa.common.oauth.OAuthException;
import com.moa.common.oauth.OAuthProvider;
import com.moa.common.oauth.OAuthUser;

import lombok.RequiredArgsConstructor;

/**
 * 구글·카카오 인증 API의 진입점. 가입과 로그인이 같은 콜백을 쓴다.
 */
@Component
@RequiredArgsConstructor
public class OAuthApplication {

	private static final String UNKNOWN_USER = "-";

	private final OAuthService oauthService;
	private final AuthAuditLogger auditLogger;

	/**
	 * @param provider 경로의 제공자 값 (google, kakao)
	 * @param intent 시작한 화면. 실패했을 때 돌아갈 곳을 정한다 (SIGN_UP, SIGN_IN)
	 * @throws com.moa.common.exception.ServiceException 모르는 제공자면 R001
	 */
	public OAuthAuthorizationDto authorize(String provider, String intent) {
		return oauthService.authorize(oauthService.provider(provider), intent);
	}

	/**
	 * 콜백을 처리한다. 오류는 예외로 던지지 않고 실패 주소(error=오류 코드)로 돌려주고 로그인 실패 감사 로그를 남긴다.
	 * <ul>
	 *   <li>이미 연결된 제공자 계정: 그 회원으로 로그인 세션을 발급한다</li>
	 *   <li>제공자가 인증된 이메일로 알려 줬고 같은 이메일의 기존 계정이 합치기 조건에 맞음: 연결하고, 같은 트랜잭션에서
	 *       로그인 수단 연결 알림(LOGIN_METHOD_LINKED, 대상 MEMBER_SOCIAL_ACCOUNT)을 그 계정에 남기고, 로그인시킨다</li>
	 *   <li>제공자가 이메일을 주지 않았거나 인증되지 않은 이메일을 줌: 이메일 가입 단계(/signup/email)로 이어 간다</li>
	 *   <li>state 불일치, 취소, 코드 교환 실패: A007</li>
	 *   <li>인증된 이메일의 기존 계정이 있으나 합치기 조건에 맞지 않음(기존 계정이 ACTIVE가 아님, 같은 제공자의 다른
	 *       계정이 이미 연결됨): B003. 새 회원도 만들지 않는다</li>
	 *   <li>연결된 계정이 ACTIVE가 아님: A001</li>
	 * </ul>
	 */
	public OAuthCallbackDto handleCallback(HandleOAuthCallbackCommand command) {
		OAuthProvider provider = oauthService.provider(command.provider());
		String intent = oauthService.intentOf(command.expectedState());
		boolean stateMatches = command.state() != null && command.state().equals(command.expectedState());
		if (!stateMatches || command.error() != null || command.code() == null) {
			return fail(intent, UNKNOWN_USER, provider, ErrorCode.OAUTH_FAILED);
		}
		OAuthUser user;
		try {
			user = oauthService.fetchUser(provider, command.code());
		} catch (OAuthException ex) {
			return fail(intent, UNKNOWN_USER, provider, ErrorCode.OAUTH_FAILED);
		}
		try {
			return oauthService.signInOrStartSignUp(user);
		} catch (ServiceException ex) {
			return fail(intent, user.providerUserId(), provider, ex.getErrorCode());
		}
	}

	private OAuthCallbackDto fail(String intent, String identifier, OAuthProvider provider, ErrorCode errorCode) {
		auditLogger.signInFailed(identifier, provider.name(), errorCode.getCode());
		return oauthService.failure(intent, errorCode);
	}
}
