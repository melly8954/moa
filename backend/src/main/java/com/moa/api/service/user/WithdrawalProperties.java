package com.moa.api.service.user;

import java.time.LocalDateTime;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;

/**
 * 회원 탈퇴 설정 (specs/018-member-withdrawal FR-012, docs/privacy.md). 정리 시각(cleanup-cron·cleanup-zone)은 정리기가 읽는다.
 *
 * @param graceDays 탈퇴한 뒤 복구할 수 있는 날수. 지나면 파기한다. docs/privacy.md의 30일과 맞춘다
 */
@Validated
@ConfigurationProperties("app.withdrawal")
public record WithdrawalProperties(@DefaultValue("30") @Min(1) int graceDays) {

	/** 완전 삭제 예정 시각. 이때부터 복구할 수 없고 파기 대상이다 */
	public LocalDateTime purgeAt(LocalDateTime withdrawnAt) {
		return withdrawnAt.plusDays(graceDays);
	}

	/** 유예 기간이 남아 복구할 수 있는지 */
	public boolean recoverable(LocalDateTime withdrawnAt, LocalDateTime now) {
		return purgeAt(withdrawnAt).isAfter(now);
	}

	/** 이 시각이나 그 전에 탈퇴했거나 지워진 회원이 파기 대상이다 */
	public LocalDateTime purgeCutoff(LocalDateTime now) {
		return now.minusDays(graceDays);
	}
}
