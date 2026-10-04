package com.moa.common.web;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

/**
 * 프론트 주소. 인증 메일 링크와 구글·카카오 콜백 뒤 이동에 쓴다.
 *
 * @param baseUrl 프론트 기본 주소 (WEB_BASE_URL, 예: https://www.example.com). 끝에 /를 붙이지 않는다
 */
@Validated
@ConfigurationProperties("psw.web")
public record WebProperties(@NotBlank String baseUrl) {

	/** @param path /로 시작하는 프론트 경로 */
	public String url(String path) {
		String base = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
		return base + path;
	}
}
