package com.moa.api.controller.v1.admin.banner.response;

import java.time.OffsetDateTime;

import org.jspecify.annotations.Nullable;

import com.moa.api.dto.banner.BannerDto;
import com.moa.api.dto.marketing.ExposureStatus;
import com.moa.common.response.ApiTimes;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "배너 관리 목록 항목")
public record BannerAdminListResponse(
	@Schema(description = "배너 ID") Long id,
	@Schema(description = "이름") String name,
	@Schema(description = "넓은 화면 이미지 주소") String wideImageUrl,
	@Schema(description = "노출 시작 일시") OffsetDateTime startsAt,
	@Schema(description = "노출 끝 일시. 없으면 끌 때까지") @Nullable OffsetDateTime endsAt,
	@Schema(description = "순서") int sortOrder,
	@Schema(description = "켜짐") boolean enabled,
	@Schema(description = "상태") ExposureStatus status) {

	public static BannerAdminListResponse from(BannerDto dto) {
		return new BannerAdminListResponse(dto.id(), dto.name(), dto.wideImageUrl(), ApiTimes.of(dto.startsAt()),
			ApiTimes.of(dto.endsAt()), dto.sortOrder(), dto.enabled(), dto.status());
	}
}
