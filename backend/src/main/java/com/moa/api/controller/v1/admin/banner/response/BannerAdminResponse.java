package com.moa.api.controller.v1.admin.banner.response;

import java.time.OffsetDateTime;

import org.jspecify.annotations.Nullable;

import com.moa.api.dto.banner.BannerDto;
import com.moa.api.dto.marketing.ExposureStatus;
import com.moa.common.response.ApiTimes;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "배너 (관리)")
public record BannerAdminResponse(
	@Schema(description = "배너 ID") Long id,
	@Schema(description = "이름") String name,
	@Schema(description = "넓은 화면 이미지 주소") String wideImageUrl,
	@Schema(description = "좁은 화면 이미지 주소. 없으면 넓은 화면 이미지를 쓴다") @Nullable String narrowImageUrl,
	@Schema(description = "대체 문구") String altText,
	@Schema(description = "연결 주소") @Nullable String linkUrl,
	@Schema(description = "노출 시작 일시") OffsetDateTime startsAt,
	@Schema(description = "노출 끝 일시. 없으면 끌 때까지") @Nullable OffsetDateTime endsAt,
	@Schema(description = "순서") int sortOrder,
	@Schema(description = "켜짐") boolean enabled,
	@Schema(description = "상태") ExposureStatus status,
	@Schema(description = "등록한 시각") OffsetDateTime createdAt,
	@Schema(description = "고친 시각") OffsetDateTime updatedAt) {

	public static BannerAdminResponse from(BannerDto dto) {
		return new BannerAdminResponse(dto.id(), dto.name(), dto.wideImageUrl(), dto.narrowImageUrl(), dto.altText(),
			dto.linkUrl(), ApiTimes.of(dto.startsAt()), ApiTimes.of(dto.endsAt()), dto.sortOrder(), dto.enabled(),
			dto.status(), ApiTimes.of(dto.createdAt()), ApiTimes.of(dto.updatedAt()));
	}
}
