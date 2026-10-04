package com.moa.common.oauth;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Set;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 구글(OpenID Connect)·카카오 OAuth 어댑터 (docs/design/architecture.md 구글 로그인, 카카오 로그인).
 *
 * <ul>
 *   <li>구글: 코드를 토큰 엔드포인트에서 바로(TLS) 교환해 받은 ID 토큰의 iss·aud·exp를 확인하고 sub, email,
 *       email_verified를 읽는다</li>
 *   <li>카카오: 코드를 교환한 액세스 토큰으로 사용자 정보를 읽는다(실패하면 한 번 더). 이메일은 is_email_verified와
 *       is_email_valid가 모두 참일 때만 인증된 것으로 본다 (INT-KAKAO-002)</li>
 * </ul>
 */
@Component
public class HttpOAuthClient implements OAuthClient {

	private static final Duration TIMEOUT = Duration.ofSeconds(5);
	private static final String GOOGLE_AUTHORIZE = "https://accounts.google.com/o/oauth2/v2/auth";
	private static final String GOOGLE_TOKEN = "https://oauth2.googleapis.com/token";
	private static final Set<String> GOOGLE_ISSUERS = Set.of("https://accounts.google.com", "accounts.google.com");
	private static final String KAKAO_AUTHORIZE = "https://kauth.kakao.com/oauth/authorize";
	private static final String KAKAO_TOKEN = "https://kauth.kakao.com/oauth/token";
	private static final String KAKAO_USER = "https://kapi.kakao.com/v2/user/me";

	private final OAuthProperties properties;
	private final ObjectMapper objectMapper;
	private final RestClient restClient;

	public HttpOAuthClient(OAuthProperties properties, ObjectMapper objectMapper) {
		this.properties = properties;
		this.objectMapper = objectMapper;
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout(TIMEOUT);
		requestFactory.setReadTimeout(TIMEOUT);
		this.restClient = RestClient.builder().requestFactory(requestFactory).build();
	}

	@Override
	public String authorizationUrl(OAuthProvider provider, String state) {
		UriComponentsBuilder builder = UriComponentsBuilder
			.fromUriString(provider == OAuthProvider.GOOGLE ? GOOGLE_AUTHORIZE : KAKAO_AUTHORIZE)
			.queryParam("response_type", "code")
			.queryParam("client_id", properties.client(provider).clientId())
			.queryParam("redirect_uri", properties.redirectUri(provider))
			.queryParam("state", state);
		if (provider == OAuthProvider.GOOGLE) {
			builder.queryParam("scope", "openid email");
		}
		return builder.encode().build().toUriString();
	}

	@Override
	public OAuthUser fetchUser(OAuthProvider provider, String code) {
		try {
			JsonNode token = exchangeCode(provider, code);
			return provider == OAuthProvider.GOOGLE ? googleUser(token) : kakaoUser(token);
		} catch (RestClientException | JsonProcessingException | IllegalArgumentException ex) {
			throw new OAuthException(provider + " 인증 실패", ex);
		}
	}

	private JsonNode exchangeCode(OAuthProvider provider, String code) throws JsonProcessingException {
		OAuthProperties.Client client = properties.client(provider);
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("grant_type", "authorization_code");
		form.add("code", code);
		form.add("client_id", client.clientId());
		form.add("client_secret", client.clientSecret());
		form.add("redirect_uri", properties.redirectUri(provider));
		String body = restClient.post()
			.uri(provider == OAuthProvider.GOOGLE ? GOOGLE_TOKEN : KAKAO_TOKEN)
			.contentType(MediaType.APPLICATION_FORM_URLENCODED)
			.body(form)
			.retrieve()
			.body(String.class);
		return objectMapper.readTree(body);
	}

	private OAuthUser googleUser(JsonNode token) throws JsonProcessingException {
		String[] parts = token.path("id_token").asText("").split("\\.");
		if (parts.length < 2) {
			throw new IllegalArgumentException("ID 토큰이 없습니다");
		}
		JsonNode claims = objectMapper.readTree(
			new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8));
		boolean validIssuer = GOOGLE_ISSUERS.contains(claims.path("iss").asText());
		boolean validAudience = properties.google().clientId().equals(claims.path("aud").asText());
		boolean notExpired = claims.path("exp").asLong(0) > Instant.now().getEpochSecond();
		if (!validIssuer || !validAudience || !notExpired) {
			throw new IllegalArgumentException("ID 토큰 검증 실패");
		}
		String email = claims.hasNonNull("email") ? claims.get("email").asText() : null;
		boolean emailVerified = claims.path("email_verified").asBoolean(false);
		return new OAuthUser(OAuthProvider.GOOGLE, requireUserId(claims.path("sub")), email, emailVerified);
	}

	private OAuthUser kakaoUser(JsonNode token) throws JsonProcessingException {
		String accessToken = token.path("access_token").asText("");
		JsonNode user;
		try {
			user = readKakaoUser(accessToken);
		} catch (RestClientException ex) {
			user = readKakaoUser(accessToken);
		}
		JsonNode account = user.path("kakao_account");
		String email = account.hasNonNull("email") ? account.get("email").asText() : null;
		boolean emailVerified = account.path("is_email_verified").asBoolean(false)
			&& account.path("is_email_valid").asBoolean(false);
		return new OAuthUser(OAuthProvider.KAKAO, requireUserId(user.path("id")), email,
			email != null && emailVerified);
	}

	/** 제공자 사용자 ID가 없으면 사용자를 정할 수 없으므로 인증 실패로 다룬다 */
	private static String requireUserId(JsonNode node) {
		String id = node.isMissingNode() || node.isNull() ? "" : node.asText("");
		if (id.isBlank()) {
			throw new IllegalArgumentException("제공자 사용자 ID가 없습니다");
		}
		return id;
	}

	private JsonNode readKakaoUser(String accessToken) throws JsonProcessingException {
		String body = restClient.get()
			.uri(KAKAO_USER)
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
			.retrieve()
			.body(String.class);
		return objectMapper.readTree(body);
	}
}
