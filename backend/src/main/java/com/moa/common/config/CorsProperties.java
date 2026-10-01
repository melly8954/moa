package com.moa.common.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * @param allowedOrigins 브라우저에서 API를 부를 수 있는 출처. 비어 있으면 교차 출처 요청을 막는다
 */
@Validated
@ConfigurationProperties("psw.cors")
public record CorsProperties(List<String> allowedOrigins) {

	public CorsProperties {
		allowedOrigins = allowedOrigins == null
			? List.of()
			: allowedOrigins.stream().filter(origin -> !origin.isBlank()).toList();
	}
}
