package com.moa.support;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.moa.common.security.oauth.OAuthIdentity;
import com.moa.common.security.oauth.OAuthProviderClient;
import com.moa.common.security.oauth.SocialProvider;

/**
 * 테스트용 구글 제공자 (specs/010-social-login tasks T058). 실제 구글을 부르지 않는다.
 * 정해 둔 인가 코드 → 정해 둔 신원. 등록하지 않은 코드는 교환 실패(빈 값)다.
 * 테스트 설정에는 app.oauth.google.*이 없어 실제 GoogleOAuthClient는 꺼져 있고, 레지스트리는 켜진 이 가짜를 쓴다.
 */
@Component
public class FakeOAuthProviderClient implements OAuthProviderClient {

	public static final String AUTHORIZATION_ENDPOINT = "https://provider.test/authorize";

	private final Map<String, OAuthIdentity> identities = new ConcurrentHashMap<>();
	private volatile boolean enabled = true;

	/** 이 인가 코드로 교환하면 이 신원을 준다 */
	public void register(String code, String subject, String email, boolean emailVerified, String name) {
		identities.put(code, new OAuthIdentity(SocialProvider.GOOGLE, subject, email, emailVerified, name));
	}

	/** 설정이 없는 환경 흉내 (US3-AC3) */
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public void reset() {
		identities.clear();
		enabled = true;
	}

	@Override
	public SocialProvider provider() {
		return SocialProvider.GOOGLE;
	}

	@Override
	public boolean enabled() {
		return enabled;
	}

	@Override
	public String authorizationUrl(String state, String nonce, String codeChallenge) {
		return AUTHORIZATION_ENDPOINT + "?state=" + state + "&nonce=" + nonce + "&code_challenge=" + codeChallenge;
	}

	@Override
	public Optional<OAuthIdentity> exchange(String code, String codeVerifier, String nonce) {
		return Optional.ofNullable(identities.get(code));
	}
}
