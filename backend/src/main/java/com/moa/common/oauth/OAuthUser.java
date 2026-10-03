package com.moa.common.oauth;

/**
 * 제공자가 알려 준 사용자.
 *
 * @param provider 제공자
 * @param providerUserId 제공자 안의 사용자 ID (구글 sub, 카카오 id)
 * @param email 제공자가 준 이메일. 카카오에서 이메일 제공에 동의하지 않으면 null
 * @param emailVerified 제공자가 인증된 이메일로 알려 줬는지 (구글 email_verified, 카카오 is_email_verified).
 *     참일 때만 같은 이메일의 기존 계정에 합친다
 */
public record OAuthUser(OAuthProvider provider, String providerUserId, String email, boolean emailVerified) {
}
