package com.moa.api.service;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moa.api.dto.MemberPolicy;
import com.moa.api.dto.OAuthAuthorizationDto;
import com.moa.api.dto.OAuthCallbackDto;
import com.moa.api.entity.Member;
import com.moa.api.entity.MemberSocialAccount;
import com.moa.api.entity.Notification;
import com.moa.api.entity.NotificationTargetType;
import com.moa.api.entity.NotificationType;
import com.moa.api.entity.SignUp;
import com.moa.api.repository.reader.MemberReader;
import com.moa.api.repository.reader.MemberSocialAccountReader;
import com.moa.api.repository.writer.MemberSocialAccountWriter;
import com.moa.api.repository.writer.NotificationWriter;
import com.moa.api.repository.writer.SignUpWriter;
import com.moa.common.exception.ErrorCode;
import com.moa.common.exception.ServiceException;
import com.moa.common.logging.AuthAuditLogger;
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
	private final NotificationWriter notificationWriter;
	private final AuthAuditLogger auditLogger;
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
	 * 제공자가 알려 준 사용자로 로그인하거나 가입을 시작한다.
	 * <ul>
	 *   <li>이미 연결된 제공자 계정: 그 회원으로 로그인</li>
	 *   <li>제공자가 인증된 이메일로 알려 줬고 같은 이메일의 기존 계정이 있음: 계정 합치기 조건에 맞으면 합치고 로그인</li>
	 *   <li>제공자가 인증된 이메일로 알려 줌: 그 이메일로 가입 시작 (다음은 휴대폰 인증)</li>
	 *   <li>이메일이 없거나 인증되지 않음: 이메일 없이 가입 시작 (다음은 이메일 가입 단계, 회원 _policy.md 제공자 이메일 없음)</li>
	 * </ul>
	 *
	 * @throws ServiceException 연결된 계정이 ACTIVE가 아니면 A001, 같은 이메일 계정이 있으나 합치기 조건(ACTIVE,
	 *     같은 제공자의 다른 계정이 연결되지 않음)에 맞지 않으면 B003
	 */
	@Transactional
	public OAuthCallbackDto signInOrStartSignUp(OAuthUser user) {
		Optional<MemberSocialAccount> linked = memberSocialAccountReader.find(user.provider(),
			user.providerUserId());
		if (linked.isPresent()) {
			Member member = memberReader.findById(linked.get().getMemberId())
				.filter(Member::isActive)
				.orElseThrow(() -> new ServiceException(ErrorCode.UNAUTHORIZED));
			return signedIn(member, user, "LINKED_ACCOUNT");
		}
		String verifiedEmail = user.email() != null && user.emailVerified()
			? SignUpRules.normalizeEmail(user.email())
			: null;
		if (verifiedEmail != null) {
			Optional<Member> sameEmail = memberReader.findByEmail(verifiedEmail);
			if (sameEmail.isPresent()) {
				return merge(sameEmail.get(), user);
			}
		}
		String rawToken = SecureTokens.newToken();
		signUpWriter.create(SignUp.startWithProvider(SecureTokens.sha256(rawToken), user.provider(),
			user.providerUserId(), verifiedEmail, LocalDateTime.now().plus(MemberPolicy.SIGN_UP_TTL)));
		String next = verifiedEmail == null ? "/signup/email" : "/signup/phone";
		return new OAuthCallbackDto(webProperties.url(next), rawToken, null);
	}

	/** 실패 주소. 시작 화면에 오류 코드를 붙인다 */
	public OAuthCallbackDto failure(String intent, ErrorCode errorCode) {
		String path = SIGN_UP.equals(intent) ? "/signup" : "/login";
		return new OAuthCallbackDto(webProperties.url(path + "?error=" + errorCode.getCode()), null, null);
	}

	/** 계정 합치기 (회원 _policy.md): 제공자가 인증된 이메일로 알려 줬을 때, ACTIVE 계정이고 같은 제공자의 다른 계정이 없으면 */
	private OAuthCallbackDto merge(Member member, OAuthUser user) {
		boolean mergeable = member.isActive()
			&& !memberSocialAccountReader.existsByMemberAndProvider(member.getId(), user.provider());
		if (!mergeable) {
			throw new ServiceException(ErrorCode.PROVIDER_EMAIL_CONFLICT);
		}
		MemberSocialAccount account;
		try {
			account = memberSocialAccountWriter.create(
				new MemberSocialAccount(member.getId(), user.provider(), user.providerUserId()));
		} catch (DataIntegrityViolationException ex) {
			throw SignUpRules.uniqueViolation(ex, ErrorCode.PROVIDER_EMAIL_CONFLICT);
		}
		notificationWriter.create(new Notification(member.getId(), NotificationType.LOGIN_METHOD_LINKED, null,
			NotificationTargetType.MEMBER_SOCIAL_ACCOUNT, account.getId()));
		return signedIn(member, user, "ACCOUNT_MERGE");
	}

	private OAuthCallbackDto signedIn(Member member, OAuthUser user, String via) {
		IssuedTokens tokens = authTokenService.issueWithNewSession(SignUpRules.principalOf(member));
		auditLogger.signInSucceeded(member.getId(), user.provider().name(), via);
		return new OAuthCallbackDto(webProperties.url("/"), null, tokens);
	}

	private static String normalizeIntent(String intent) {
		return SIGN_UP.equals(intent) ? SIGN_UP : SIGN_IN;
	}
}
