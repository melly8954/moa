package com.moa.common.security.oauth;

/**
 * 제공자가 확인해 준 신원. 제공자 토큰은 담지 않는다.
 *
 * @param subject 제공자의 바뀌지 않는 계정 식별자 (구글 sub)
 * @param email 제공자가 준 이메일. 없을 수 있다
 * @param emailVerified 제공자가 이메일을 인증했는지
 * @param name 제공자 이름. 표시 이름으로 쓰지 않는다 (FR-005)
 */
public record OAuthIdentity(SocialProvider provider, String subject, String email, boolean emailVerified,
	String name) {
}
