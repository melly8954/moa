package com.moa.api.controller.v1.auth.response;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "설정된 소셜 로그인 제공자")
public record OAuthProvidersResponse(
	@Schema(description = "제공자 경로 값. 설정이 없으면 빈 배열", example = "[\"google\"]") List<String> providers) {
}
