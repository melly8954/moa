package com.moa.common.oauth;

/**
 * 제공자가 알려 준 사용자.
 *
 * @param provider 제공자
 * @param providerUserId 제공자 안의 사용자 ID (구글 sub, 카카오 id)
 * @param email 제공자가 준 이메일. 카카오에서 이메일 제공에 동의하지 않으면 null
 * @param emailVerified 제공자가 인증된 이메일로 알려 줬는지. 어댑터가 제공자별 규칙으로 계산한다.
 *     구글은 email_verified, 카카오는 is_email_verified와 is_email_valid가 모두 참일 때 참이다.
 *     참일 때만 같은 이메일의 기존 계정에 합칠 수 있다
 */
public record OAuthUser(OAuthProvider provider, String providerUserId, String email, boolean emailVerified) {
}
