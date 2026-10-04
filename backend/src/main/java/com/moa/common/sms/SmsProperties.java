package com.moa.common.sms;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * 문자 발송 설정.
 *
 * @param provider 어댑터: solapi 또는 log (SMS_PROVIDER). 운영에서는 log를 쓸 수 없다 (SmsAdapterGuard)
 * @param solapi 솔라피 접속 정보. provider가 solapi면 모두 있어야 한다
 */
@Validated
@ConfigurationProperties("psw.sms")
public record SmsProperties(@NotBlank @Pattern(regexp = "solapi|log") String provider,
	@Valid @NotNull Solapi solapi) {

	@AssertTrue(message = "psw.sms.provider가 solapi면 SOLAPI_API_KEY, SOLAPI_API_SECRET, SOLAPI_SENDER가 있어야 합니다.")
	public boolean isSolapiConfigured() {
		return !"solapi".equals(provider) || solapi != null && solapi.isComplete();
	}

	/**
	 * @param apiKey API 키 (SOLAPI_API_KEY)
	 * @param apiSecret API 시크릿 (SOLAPI_API_SECRET)
	 * @param sender 등록한 발신번호 (SOLAPI_SENDER)
	 * @param baseUrl API 주소
	 */
	public record Solapi(String apiKey, String apiSecret, String sender, @NotBlank String baseUrl) {

		boolean isComplete() {
			return notBlank(apiKey) && notBlank(apiSecret) && notBlank(sender);
		}

		private static boolean notBlank(String value) {
			return value != null && !value.isBlank();
		}
	}
}
