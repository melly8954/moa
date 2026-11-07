package com.moa.common.helper.loginhistory;

import java.util.Locale;

/**
 * User-Agent에서 기기 종류·OS·브라우저를 뽑는다. 화면에 보여 주고 이상 접속을 찾는 정도의 정확도다.
 * 더 자세한 분석이 필요하면 uap-java 같은 라이브러리로 바꾼다.
 */
public record DeviceInfo(String deviceType, String os, String browser) {

	public static final String UNKNOWN = "Unknown";

	public static DeviceInfo parse(String userAgent) {
		if (userAgent == null || userAgent.isBlank()) {
			return new DeviceInfo("OTHER", UNKNOWN, UNKNOWN);
		}
		String ua = userAgent.toLowerCase(Locale.ROOT);
		return new DeviceInfo(deviceType(ua), os(ua), browser(ua));
	}

	private static String deviceType(String ua) {
		if (ua.contains("ipad") || ua.contains("tablet") || ua.contains("android") && !ua.contains("mobile")) {
			return "TABLET";
		}
		if (ua.contains("mobile") || ua.contains("iphone")) {
			return "MOBILE";
		}
		if (ua.contains("windows") || ua.contains("macintosh") || ua.contains("linux") || ua.contains("cros")) {
			return "PC";
		}
		return "OTHER";
	}

	private static String os(String ua) {
		if (ua.contains("windows")) {
			return "Windows";
		}
		if (ua.contains("iphone") || ua.contains("ipad")) {
			return "iOS";
		}
		if (ua.contains("android")) {
			return "Android";
		}
		if (ua.contains("mac os")) {
			return "macOS";
		}
		if (ua.contains("cros")) {
			return "ChromeOS";
		}
		if (ua.contains("linux")) {
			return "Linux";
		}
		return UNKNOWN;
	}

	/** 순서가 중요하다. Edge, Whale, 삼성 인터넷 UA에도 chrome과 safari가 들어 있다 */
	private static String browser(String ua) {
		if (ua.contains("edg/")) {
			return "Edge";
		}
		if (ua.contains("whale/")) {
			return "Whale";
		}
		if (ua.contains("samsungbrowser/")) {
			return "Samsung Internet";
		}
		if (ua.contains("firefox/") || ua.contains("fxios/")) {
			return "Firefox";
		}
		if (ua.contains("chrome/") || ua.contains("crios/")) {
			return "Chrome";
		}
		if (ua.contains("safari/")) {
			return "Safari";
		}
		return UNKNOWN;
	}
}
