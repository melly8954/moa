package com.moa.support.external;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.moa.common.oauth.OAuthClient;
import com.moa.common.oauth.OAuthException;
import com.moa.common.oauth.OAuthProvider;
import com.moa.common.oauth.OAuthUser;

/**
 * 구글·카카오 OAuth 포트의 테스트 대역. 인가 코드마다 제공자가 돌려줄 사용자를 미리 정해 둔다.
 * 정하지 않은 코드는 코드 교환 실패(OAuthException)로 다룬다.
 */
public class StubOAuthClient implements OAuthClient {

	public static final String AUTHORIZATION_URL = "https://oauth.provider.test/authorize";

	private final Map<String, OAuthUser> usersByCode = new ConcurrentHashMap<>();

	/** 인가 코드 code로 콜백이 오면 제공자가 user를 알려 준 것으로 한다 */
	public void willReturn(String code, OAuthUser user) {
		usersByCode.put(code, user);
	}

	@Override
	public String authorizationUrl(OAuthProvider provider, String state) {
		return AUTHORIZATION_URL + "?provider=" + provider.pathValue() + "&state=" + state;
	}

	@Override
	public OAuthUser fetchUser(OAuthProvider provider, String code) {
		OAuthUser user = usersByCode.get(code);
		if (user == null || user.provider() != provider) {
			throw new OAuthException("테스트 대역: 정하지 않은 인가 코드", null);
		}
		return user;
	}

	public void clear() {
		usersByCode.clear();
	}
}
