package com.moa.common.terms;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

/**
 * 현재 약관 버전 (specs/010-social-login research R11). 비밀 값이 아니다. 문안은 프론트가 버전마다 둔다.
 */
@Validated
@ConfigurationProperties("app.terms")
public record TermsProperties(@NotBlank String serviceVersion, @NotBlank String privacyVersion) {

	public String versionOf(TermsType type) {
		return switch (type) {
			case SERVICE -> serviceVersion;
			case PRIVACY -> privacyVersion;
		};
	}
}
