package com.moa.api.controller.v1.admin.popup.response;

import java.time.OffsetDateTime;

import org.jspecify.annotations.Nullable;

import com.moa.api.dto.marketing.ExposureStatus;
import com.moa.api.dto.popup.PopupDto;
import com.moa.common.response.ApiTimes;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "팝업 관리 목록 항목")
public record PopupAdminListResponse(
	@Schema(description = "팝업 ID") Long id,
	@Schema(description = "제목") String title,
	@Schema(description = "노출 시작 일시") OffsetDateTime startsAt,
	@Schema(description = "노출 끝 일시. 없으면 끌 때까지") @Nullable OffsetDateTime endsAt,
	@Schema(description = "순서") int sortOrder,
	@Schema(description = "켜짐") boolean enabled,
	@Schema(description = "상태") ExposureStatus status) {

	public static PopupAdminListResponse from(PopupDto dto) {
		return new PopupAdminListResponse(dto.id(), dto.title(), ApiTimes.of(dto.startsAt()),
			ApiTimes.of(dto.endsAt()), dto.sortOrder(), dto.enabled(), dto.status());
	}
}
