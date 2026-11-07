package com.moa.common.helper.loginhistory;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class DeviceInfoTest {

	@ParameterizedTest(name = "{1} {2} {3}")
	@CsvSource(delimiter = '|', value = {
		"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0 Safari/537.36"
			+ "|PC|Windows|Chrome",
		"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/130.0 Safari/537.36 Edg/130.0"
			+ "|PC|Windows|Edge",
		"Mozilla/5.0 (Macintosh; Intel Mac OS X 14_5) AppleWebKit/605.1.15 Version/17.5 Safari/605.1.15"
			+ "|PC|macOS|Safari",
		"Mozilla/5.0 (iPhone; CPU iPhone OS 17_5 like Mac OS X) AppleWebKit/605.1.15 Mobile/15E148 Safari/604.1"
			+ "|MOBILE|iOS|Safari",
		"Mozilla/5.0 (Linux; Android 14; SM-S921N) AppleWebKit/537.36 SamsungBrowser/25.0 Chrome/121.0 Mobile"
			+ " Safari/537.36|MOBILE|Android|Samsung Internet",
		"Mozilla/5.0 (Linux; Android 14; SM-X710) AppleWebKit/537.36 Chrome/130.0 Safari/537.36"
			+ "|TABLET|Android|Chrome",
		"curl/8.5.0|OTHER|Unknown|Unknown"
	})
	void parse(String userAgent, String deviceType, String os, String browser) {
		assertThat(DeviceInfo.parse(userAgent)).isEqualTo(new DeviceInfo(deviceType, os, browser));
	}
}
