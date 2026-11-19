package com.moa.api.controller.v1.terms.response;

import java.util.Arrays;
import java.util.List;

import com.moa.common.terms.TermsProperties;
import com.moa.common.terms.TermsType;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "가입 때 동의받을 현재 약관")
public record TermsResponse(List<Item> terms) {

	public static TermsResponse from(TermsProperties properties) {
		return new TermsResponse(Arrays.stream(TermsType.values())
			.map(type -> new Item(type.name(), properties.versionOf(type), true))
			.toList());
	}

	@Schema(description = "약관 하나")
	public record Item(
		@Schema(description = "종류 (SERVICE 이용약관, PRIVACY 개인정보 수집·이용)", example = "SERVICE") String type,
		@Schema(description = "현재 버전. 프론트가 이 버전의 문안을 고른다", example = "draft") String version,
		@Schema(description = "필수 동의인지") boolean required) {
	}
}
