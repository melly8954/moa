package com.moa.api.controller.v1.admin.popup.request;

import java.time.OffsetDateTime;

import org.jspecify.annotations.Nullable;

import com.moa.api.controller.v1.admin.marketing.MarketingRequests;
import com.moa.api.dto.popup.PopupContent;
import com.moa.common.helper.richtext.SafeHtml;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "팝업 등록·고치기 (specs/016-banner-popup FR-004)")
public record PopupSaveRequest(
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "제목",
		example = "12월 시즌 대회 열려요") @NotBlank @Size(max = 200) String title,
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED,
		description = "내용 HTML. 허용한 서식만 남기고 걸러 저장한다 (rich-text)") @SafeHtml @NotBlank @Size(
			max = 20000) String content,
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

	public PopupContent toContent() {
		return new PopupContent(title, content, linkUrl, MarketingRequests.toServerTime(startsAt),
			MarketingRequests.toServerTime(endsAt), sortOrder, enabled);
	}
}
