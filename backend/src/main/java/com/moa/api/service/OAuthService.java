package com.moa.api.service;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moa.api.dto.MemberPolicy;
import com.moa.api.dto.OAuthAuthorizationDto;
import com.moa.api.dto.OAuthCallbackDto;
import com.moa.api.entity.Member;
import com.moa.api.entity.MemberSocialAccount;
import com.moa.api.entity.NotificationTargetType;
import com.moa.api.entity.NotificationType;
import com.moa.api.entity.SignUp;
import com.moa.api.repository.reader.MemberReader;
import com.moa.api.repository.reader.MemberSocialAccountReader;
import com.moa.api.repository.writer.MemberSocialAccountWriter;
import com.moa.api.repository.writer.SignUpWriter;
import com.moa.common.exception.ErrorCode;
import com.moa.common.exception.ServiceException;
import com.moa.common.oauth.OAuthClient;
import com.moa.common.oauth.OAuthProvider;
import com.moa.common.oauth.OAuthUser;
import com.moa.common.security.SecureTokens;
import com.moa.common.security.token.AuthTokenService;
import com.moa.common.security.token.IssuedTokens;
import com.moa.common.web.WebProperties;

import lombok.RequiredArgsConstructor;

/**
 * 구글·카카오 인증 뒤 처리: 이미 연결된 계정 로그인, 같은 이메일 계정에 합치기, 새 가입 시작.
 * state는 {@code <시작 화면>.<무작위 값>}이다. 시작 화면(SIGN_UP, SIGN_IN)으로 실패 때 돌아갈 곳을 정한다.
 */
@Service
@RequiredArgsConstructor
public class OAuthService {

	public static final String SIGN_UP = "SIGN_UP";
	public static final String SIGN_IN = "SIGN_IN";
	private static final String STATE_SEPARATOR = ".";

	private final OAuthClient oauthClient;
	private final MemberReader memberReader;
	private final MemberSocialAccountReader memberSocialAccountReader;
	private final MemberSocialAccountWriter memberSocialAccountWriter;
	private final SignUpWriter signUpWriter;
	private final NotificationService notificationService;
	private final AuthTokenService authTokenService;
	private final WebProperties webProperties;

	/**
	 * @throws ServiceException 모르는 제공자면 R001
	 */
	public OAuthProvider provider(String pathValue) {
		return OAuthProvider.fromPathValue(pathValue).orElseThrow(() -> new ServiceException(ErrorCode.NOT_FOUND));
	}

	public OAuthAuthorizationDto authorize(OAuthProvider provider, String intent) {
		String state = normalizeIntent(intent) + STATE_SEPARATOR + SecureTokens.newToken();
		return new OAuthAuthorizationDto(oauthClient.authorizationUrl(provider, state), state);
	}

	/** state에서 시작 화면을 읽는다. 모르면 로그인 화면 */
	public String intentOf(String state) {
		if (state != null && state.startsWith(SIGN_UP + STATE_SEPARATOR)) {
			return SIGN_UP;
		}
		return SIGN_IN;
	}

	/**
	 * 제공자에서 사용자를 읽는다. 트랜잭션 밖에서 부른다.
	 *
	 * @throws com.moa.common.oauth.OAuthException 코드 교환·사용자 정보 실패
	 */
	public OAuthUser fetchUser(OAuthProvider provider, String code) {
		return oauthClient.fetchUser(provider, code);
	}

	/**
	 * @throws ServiceException 연결된 계정이 ACTIVE가 아니면 A001, 같은 이메일 계정이 있으나 합치기 조건에 맞지 않으면 B003
	 */
	@Transactional
	public OAuthCallbackDto signInOrStartSignUp(OAuthUser user) {
		Optional<MemberSocialAccount> linked = memberSocialAccountReader.find(user.provider(),
			user.providerUserId());
		if (linked.isPresent()) {
			Member member = memberReader.findById(linked.get().getMemberId())
				.filter(Member::isActive)
				.orElseThrow(() -> new ServiceException(ErrorCode.UNAUTHORIZED));
			return signedIn(member);
		}
		if (user.email() != null) {
			Optional<Member> sameEmail = memberReader.findByEmail(SignUpRules.normalizeEmail(user.email()));
			if (sameEmail.isPresent()) {
				return merge(sameEmail.get(), user);
			}
		}
		String rawToken = SecureTokens.newToken();
		String email = user.email() == null ? null : SignUpRules.normalizeEmail(user.email());
		signUpWriter.create(SignUp.startWithProvider(SecureTokens.sha256(rawToken), user.provider(),
			user.providerUserId(), email, MemberPolicy.SIGN_UP_TTL));
		String next = email == null ? "/signup/email" : "/signup/phone";
		return new OAuthCallbackDto(webProperties.url(next), rawToken, null);
	}

	/** 실패 주소. 시작 화면에 오류 코드를 붙인다 */
	public OAuthCallbackDto failure(String intent, ErrorCode errorCode) {
		String path = SIGN_UP.equals(intent) ? "/signup" : "/login";
		return new OAuthCallbackDto(webProperties.url(path + "?error=" + errorCode.getCode()), null, null);
	}

	/** 계정 합치기 (회원 _policy.md): 인증된 이메일, ACTIVE 계정, 같은 제공자의 다른 계정이 연결되지 않은 계정에만 */
	private OAuthCallbackDto merge(Member member, OAuthUser user) {
		boolean mergeable = user.emailVerified() && member.isActive()
			&& !memberSocialAccountReader.existsByMemberAndProvider(member.getId(), user.provider());
		if (!mergeable) {
			throw new ServiceException(ErrorCode.SOCIAL_EMAIL_CONFLICT);
		}
		MemberSocialAccount account = memberSocialAccountWriter.create(
			new MemberSocialAccount(member.getId(), user.provider(), user.providerUserId()));
		notificationService.create(member.getId(), NotificationType.LOGIN_METHOD_LINKED, null,
			NotificationTargetType.MEMBER_SOCIAL_ACCOUNT, account.getId());
		return signedIn(member);
	}

	private OAuthCallbackDto signedIn(Member member) {
		IssuedTokens tokens = authTokenService.issueWithNewSession(SignUpRules.principalOf(member));
		return new OAuthCallbackDto(webProperties.url("/"), null, tokens);
	}

	private static String normalizeIntent(String intent) {
		return SIGN_UP.equals(intent) ? SIGN_UP : SIGN_IN;
	}
}
