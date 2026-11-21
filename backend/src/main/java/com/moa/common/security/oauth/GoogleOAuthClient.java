package com.moa.common.security.oauth;

import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * 구글 OIDC 인가 코드 + PKCE (specs/010-social-login research R1·R2).
 * ID 토큰은 TLS로 토큰 엔드포인트에서 직접 받으므로 서명 대신 TLS 서버 확인에 기댄다 (OIDC Core 3.1.3.7 6).
 * iss·aud·exp·nonce는 확인한다. 실패하면 빈 값을 돌려주고 이유를 로그에 남긴다 (토큰·비밀 값은 남기지 않는다).
 */
@Component
public class GoogleOAuthClient implements OAuthProviderClient {

	static final String AUTHORIZATION_ENDPOINT = "https://accounts.google.com/o/oauth2/v2/auth";
	static final String TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token";
	private static final Set<String> ISSUERS = Set.of("https://accounts.google.com", "accounts.google.com");
	private static final Duration TIMEOUT = Duration.ofSeconds(5);
	private static final Logger log = LoggerFactory.getLogger(GoogleOAuthClient.class);

	private final OAuthProperties.Client settings;
	private final ObjectMapper objectMapper;
	private final RestClient restClient;

	public GoogleOAuthClient(OAuthProperties properties, ObjectMapper objectMapper) {
		this.settings = properties.google();
		this.objectMapper = objectMapper;
		JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(
			HttpClient.newBuilder().connectTimeout(TIMEOUT).build());
		requestFactory.setReadTimeout(TIMEOUT);
		this.restClient = RestClient.builder().requestFactory(requestFactory).build();
	}

	@Override
	public SocialProvider provider() {
		return SocialProvider.GOOGLE;
	}

	@Override
	public boolean enabled() {
		return settings.enabled();
	}

	@Override
	public String authorizationUrl(String state, String nonce, String codeChallenge) {
		return UriComponentsBuilder.fromUriString(AUTHORIZATION_ENDPOINT)
			.queryParam("response_type", "code")
			.queryParam("client_id", settings.clientId())
			.queryParam("redirect_uri", settings.redirectUri())
			.queryParam("scope", "openid email profile")
			.queryParam("state", state)
			.queryParam("nonce", nonce)
			.queryParam("code_challenge", codeChallenge)
			.queryParam("code_challenge_method", "S256")
			.encode()
			.toUriString();
	}

	@Override
	public Optional<OAuthIdentity> exchange(String code, String codeVerifier, String nonce) {
		try {
			String idToken = requestIdToken(code, codeVerifier);
			IdTokenClaims claims = decode(idToken);
			verify(claims, nonce);
			return Optional.of(new OAuthIdentity(SocialProvider.GOOGLE, claims.sub(), claims.email(),
				claims.emailVerified(), claims.name()));
		} catch (VerificationFailed ex) {
			// 예외 메시지에는 제공자 응답 본문이 섞일 수 있어 남기지 않는다 (docs/privacy.md "파기 방법": 로그)
			log.warn("구글 로그인 확인 실패: 이유={}, 예외={}, HTTP 상태={}", ex.reason, ex.exceptionType, ex.httpStatus);
			return Optional.empty();
		}
	}

	private String requestIdToken(String code, String codeVerifier) {
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("grant_type", "authorization_code");
		form.add("code", code);
		form.add("client_id", settings.clientId());
		form.add("client_secret", settings.clientSecret());
		form.add("redirect_uri", settings.redirectUri());
		form.add("code_verifier", codeVerifier);
		try {
			TokenResponse response = restClient.post()
				.uri(TOKEN_ENDPOINT)
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.body(form)
				.retrieve()
				.body(TokenResponse.class);
			if (response == null || response.idToken() == null) {
				throw failed("토큰 응답에 id_token이 없습니다");
			}
			return response.idToken();
		} catch (RestClientException ex) {
			throw failed("토큰 교환 실패", ex);
		}
	}

	private IdTokenClaims decode(String idToken) {
		String[] parts = idToken.split("\\.");
		if (parts.length != 3) {
			throw failed("ID 토큰 형식이 아닙니다");
		}
		try {
			byte[] payload = Base64.getUrlDecoder().decode(parts[1]);
			return objectMapper.readValue(new String(payload, StandardCharsets.UTF_8), IdTokenClaims.class);
		} catch (IllegalArgumentException | JacksonException ex) {
			throw failed("ID 토큰을 읽을 수 없습니다", ex);
		}
	}

	private void verify(IdTokenClaims claims, String nonce) {
		if (!ISSUERS.contains(claims.iss())) {
			throw failed("iss가 다릅니다");
		}
		if (!claims.hasAudience(settings.clientId())) {
			throw failed("aud가 다릅니다");
		}
		if (claims.exp() == null || Instant.ofEpochSecond(claims.exp()).isBefore(Instant.now())) {
			throw failed("ID 토큰이 만료됐습니다");
		}
		if (nonce == null || !nonce.equals(claims.nonce())) {
			throw failed("nonce가 다릅니다");
		}
		if (claims.sub() == null || claims.sub().isBlank()) {
			throw failed("sub가 없습니다");
		}
	}

	private VerificationFailed failed(String reason) {
		return new VerificationFailed(reason, null, null);
	}

	/** 원인 예외는 종류와 (있으면) HTTP 상태만 남긴다. 메시지·응답 본문은 버린다 */
	private VerificationFailed failed(String reason, Exception cause) {
		Integer status = cause instanceof RestClientResponseException response
			? response.getStatusCode().value() : null;
		return new VerificationFailed(reason, cause.getClass().getSimpleName(), status);
	}

	/**
	 * 교환·확인 실패. exchange 안에서만 쓰고 밖으로 내보내지 않는다.
	 * reason은 이 클래스가 정한 고정 문구라 제공자 값이 섞이지 않는다
	 */
	private static final class VerificationFailed extends RuntimeException {

		private final String reason;
		private final String exceptionType;
		private final Integer httpStatus;

		VerificationFailed(String reason, String exceptionType, Integer httpStatus) {
			super(null, null, false, false);
			this.reason = reason;
			this.exceptionType = exceptionType;
			this.httpStatus = httpStatus;
		}
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record TokenResponse(@JsonProperty("id_token") String idToken) {
	}

	/** aud는 문자열이나 배열, email_verified는 불리언이나 문자열로 올 수 있다 */
	@JsonIgnoreProperties(ignoreUnknown = true)
	record IdTokenClaims(String iss, Object aud, Long exp, String nonce, String sub, String email,
		@JsonProperty("email_verified") Object emailVerifiedRaw, String name) {

		boolean hasAudience(String clientId) {
			if (aud instanceof String value) {
				return value.equals(clientId);
			}
			return aud instanceof Collection<?> values && values.contains(clientId);
		}

		boolean emailVerified() {
			return Boolean.TRUE.equals(emailVerifiedRaw) || "true".equals(emailVerifiedRaw);
		}
	}
}
