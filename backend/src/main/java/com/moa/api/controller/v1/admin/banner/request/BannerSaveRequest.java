package com.moa.api.controller.v1.admin.banner.request;

import java.time.OffsetDateTime;

import org.jspecify.annotations.Nullable;

import com.moa.api.controller.v1.admin.marketing.MarketingRequests;
import com.moa.api.dto.banner.BannerContent;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "배너 등록·고치기 (specs/016-banner-popup FR-003)")
public record BannerSaveRequest(
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "이름 (관리 목록에서 알아보는 이름)",
		example = "윈터컵 참가 신청") @NotBlank @Size(max = 100) String name,
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED,
		description = "넓은 화면 이미지 주소 (rich-text 이미지 올리기의 url). JPG·PNG·WebP") @NotBlank @Size(
			max = 100) String wideImageUrl,
	@Schema(description = "좁은 화면 이미지 주소. 비면 넓은 화면 이미지를 쓴다") @Size(max = 100) @Nullable String narrowImageUrl,
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "대체 문구",
		example = "moa 윈터컵 5인 팀 대회 참가 신청") @NotBlank @Size(max = 200) String altText,
	@Schema(description = "연결 주소. /로 시작하는 내부 주소나 http·https 주소",
		example = "/notices/42") @Size(max = 500) @Pattern(regexp = MarketingRequests.LINK_URL_PATTERN,
			message = MarketingRequests.LINK_URL_MESSAGE) @Nullable String linkUrl,
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "노출 시작 일시",
		example = "2026-12-01T10:00:00+09:00") @NotNull OffsetDateTime startsAt,
	@Schema(description = "노출 끝 일시. 비면 끌 때까지. 있으면 시작보다 뒤",
		example = "2026-12-10T23:59:00+09:00") @Nullable OffsetDateTime endsAt,
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "순서 (작을수록 먼저)",
		example = "0") @NotNull @Min(0) @Max(9999) Integer sortOrder,
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "켜짐") @NotNull Boolean enabled) {

	public BannerContent toContent() {
		return new BannerContent(name, wideImageUrl, narrowImageUrl, altText, linkUrl,
			MarketingRequests.toServerTime(startsAt), MarketingRequests.toServerTime(endsAt), sortOrder, enabled);
	}
}
