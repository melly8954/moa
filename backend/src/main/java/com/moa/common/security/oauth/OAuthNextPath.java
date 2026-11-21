package com.moa.common.security.oauth;

/**
 * 로그인 뒤 돌아갈 주소(next) 검사. 프론트 {@code lib/next-path.ts}의 safeNextPath와 같은 규칙이다:
 * 같은 사이트 경로("/"로 시작, "//"·"/\" 아님)만 받고 로그인 화면은 받지 않는다.
 */
public final class OAuthNextPath {

	private static final String SIGN_IN_PATH = "/sign-in";

	private OAuthNextPath() {
	}

	/**
	 * @return 받을 수 있으면 그 값, 아니면 null
	 */
	public static String sanitize(String value) {
		if (value == null || !value.startsWith("/") || value.startsWith("//") || value.startsWith("/\\")) {
			return null;
		}
		if (value.equals(SIGN_IN_PATH) || value.startsWith(SIGN_IN_PATH + "?")) {
			return null;
		}
		return value;
	}
}
