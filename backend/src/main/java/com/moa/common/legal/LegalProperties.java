package com.moa.common.legal;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

/**
 * 공개 문서 설정 (specs/012-terms-privacy data-model "설정").
 *
 * @param location 문안 위치. 그 아래 문서 폴더마다 버전 파일(json + html)을 둔다
 * @param zone "오늘"을 정하는 시간대. 시행일 0시(이 시간대)부터 그 버전이 지금 시행 중이다 (research R5)
 */
@Validated
@ConfigurationProperties("app.legal")
public record LegalProperties(@DefaultValue("classpath:legal/") @NotBlank String location,
	@DefaultValue("Asia/Seoul") @NotBlank String zone) {
}
