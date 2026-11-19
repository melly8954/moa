package com.moa.api.application.v1.auth;

import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.moa.api.application.v1.auth.command.SocialSignUpCommand;
import com.moa.api.dto.user.SocialLoginUserDto;
import com.moa.api.dto.user.SocialSignUpCheck;
import com.moa.api.dto.user.UserDto;
import com.moa.api.service.user.NicknameTags;
import com.moa.api.service.user.SocialAccountService;
import com.moa.common.security.oauth.OAuthCookies;
import com.moa.common.security.oauth.OAuthIdentity;
import com.moa.common.security.oauth.OAuthMode;
import com.moa.common.security.oauth.OAuthNextPath;
import com.moa.common.security.oauth.OAuthProviderClient;
import com.moa.common.security.oauth.OAuthProviderRegistry;
import com.moa.common.security.oauth.OAuthSecrets;
import com.moa.common.security.oauth.OAuthSignup;
import com.moa.common.security.oauth.OAuthState;
import com.moa.common.security.token.AuthTokenService;
import com.moa.common.security.token.IssuedTokens;
import com.moa.common.security.token.RefreshTokenTransport;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * 소셜 로그인 흐름 (specs/010-social-login research R1·R4·R5, contracts/auth-oauth.md).
 * 인가 시작과 콜백은 브라우저 이동이므로 결과를 오류 응답이 아니라 이동할 주소로 돌려준다.
 * <p>
 * 클래스에 @Transactional을 두지 않는다 (AuthTokenApplication과 같은 이유. 토큰 발급은 AuthTokenService가 트랜잭션을 가진다).
 * 가입만 TransactionTemplate으로 한 트랜잭션에 묶고, 태그 충돌이면 새 트랜잭션으로 다시 한다 (research R14).
 */
@Component
@RequiredArgsConstructor
public class SocialLoginApplication {

	static final String SIGN_IN_PATH = "/sign-in";
	static final String SIGN_UP_PATH = "/sign-up/nickname";
	static final String DONE_PATH = "/auth/oauth/done";
	private static final String LOGIN_MODE = "login";
	private static final String ACCESS_DENIED = "access_denied";

	private final OAuthProviderRegistry providerRegistry;
	private final OAuthCookies oauthCookies;
	private final SocialAccountService socialAccountService;
	private final AuthTokenService authTokenService;
	private final RefreshTokenTransport refreshTokenTransport;
	private final TransactionTemplate transactionTemplate;

	/**
	 * 인가 시작 (contracts "authorize", US1). state·nonce·PKCE를 만들어 oauth_state 쿠키에 담고 제공자 인가 화면 주소를 준다.
	 *
	 * @param mode login만 받는다. 연결(link)은 US5에서 더한다
	 * @return 이동할 주소. 제공자가 없거나 꺼졌으면 /sign-in?oauthError=disabled
	 */
	public String authorize(String provider, String mode, String next, HttpServletResponse response) {
		Optional<OAuthProviderClient> client = providerRegistry.find(provider);
		if (client.isEmpty()) {
			return signInWithError("disabled");
		}
		if (mode != null && !LOGIN_MODE.equals(mode)) {
			return signInWithError("failed");
		}
		String state = OAuthSecrets.random();
		String nonce = OAuthSecrets.random();
		String verifier = OAuthSecrets.random();
		oauthCookies.writeState(response, new OAuthState(client.get().provider(), state, nonce, verifier,
			OAuthMode.LOGIN, OAuthNextPath.sanitize(next)));
		return client.get().authorizationUrl(state, nonce, OAuthSecrets.codeChallenge(verifier));
	}

	/**
	 * 제공자 콜백 (contracts 콜백 표, US1·US2·US3).
	 * <ul>
	 *   <li>취소(access_denied): 안내 없이 로그인 화면</li>
	 *   <li>state·제공자 불일치, 교환·확인 실패: oauthError=failed</li>
	 *   <li>연결된 회원: 잠겼으면 oauthError=locked, 아니면 토큰 발급 뒤 /auth/oauth/done</li>
	 *   <li>연결 없음: 가입 확인에 막히면 oauthError=emailUnverified|emailInUse, 통과하면 회원을 만들지 않고
	 *       oauth_signup 쿠키를 심어 닉네임 화면</li>
	 * </ul>
	 *
	 * @return 이동할 주소
	 */
	public String callback(String provider, String code, String state, String error, HttpServletRequest request,
		HttpServletResponse response) {
		Optional<OAuthState> saved = oauthCookies.readState(request);
		oauthCookies.clearState(response);
		if (error != null) {
			return ACCESS_DENIED.equals(error) ? SIGN_IN_PATH : signInWithError("failed");
		}
		Optional<OAuthProviderClient> client = providerRegistry.find(provider);
		if (client.isEmpty()) {
			return signInWithError("disabled");
		}
		if (saved.isEmpty() || saved.get().provider() != client.get().provider() || code == null
			|| !saved.get().state().equals(state)) {
			return signInWithError("failed");
		}
		Optional<OAuthIdentity> identity = client.get()
			.exchange(code, saved.get().codeVerifier(), saved.get().nonce());
		if (identity.isEmpty()) {
			return signInWithError("failed");
		}
		String next = saved.get().next();

		Optional<SocialLoginUserDto> linked = socialAccountService.findLinkedUser(identity.get());
		if (linked.isPresent()) {
			return logIn(linked.get(), next, request, response);
		}
		SocialSignUpCheck check = socialAccountService.checkSignUp(identity.get());
		return switch (check.result()) {
			case EMAIL_UNVERIFIED -> signInWithError("emailUnverified");
			case EMAIL_IN_USE -> signInWithError("emailInUse");
			case ALLOWED -> {
				OAuthIdentity verified = identity.get();
				oauthCookies.writeSignup(response, new OAuthSignup(verified.provider(), verified.subject(),
					check.email(), verified.name(), next));
				yield withNext(SIGN_UP_PATH, next);
			}
		};
	}

	/**
	 * 닉네임·약관을 받아 가입시키고 토큰을 발급한다 (contracts "sign-up", research R5·R14).
	 *
	 * @param signup oauth_signup 쿠키에서 읽은 값. 없으면 A009
	 */
	public IssuedTokens signUp(Optional<OAuthSignup> signup, SocialSignUpCommand command) {
		OAuthSignup pending = socialAccountService.requireSignup(signup);
		UserDto user = null;
		for (int attempt = 1; user == null; attempt++) {
			boolean lastAttempt = attempt >= NicknameTags.MAX_ATTEMPTS;
			try {
				user = transactionTemplate.execute(status -> socialAccountService.signUp(pending, command.nickname(),
					command.agreements(), lastAttempt));
			} catch (DataIntegrityViolationException ex) {
				if (lastAttempt || !NicknameTags.isTagConflict(ex)) {
					throw ex;
				}
			}
		}
		return authTokenService.issue(user.toPrincipal());
	}

	/** 연결된 회원 로그인 (FR-003, US2). 잠긴 회원은 아이디·비밀번호 로그인과 똑같이 막는다 */
	private String logIn(SocialLoginUserDto linked, String next, HttpServletRequest request,
		HttpServletResponse response) {
		if (linked.locked()) {
			return signInWithError("locked");
		}
		IssuedTokens tokens = authTokenService.issue(linked.user().toPrincipal());
		refreshTokenTransport.write(request, response, tokens);
		return withNext(DONE_PATH, next);
	}

	private static String signInWithError(String code) {
		return SIGN_IN_PATH + "?oauthError=" + code;
	}

	private static String withNext(String path, String next) {
		if (next == null) {
			return path;
		}
		return UriComponentsBuilder.fromPath(path).queryParam("next", next).encode().build().toUriString();
	}
}
