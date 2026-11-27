package com.moa.api.controller.v1.admin.banner.request;

import org.jspecify.annotations.Nullable;

import com.moa.api.dto.marketing.ExposureStatusFilter;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "배너 관리 검색 조건. 모두 선택")
public record BannerSearchRequest(
	@Schema(description = "이름에 들어 있는 글자", example = "윈터") @Size(max = 200) @Nullable String keyword,
	@Schema(description = "상태. 비면 끝난 것만 빼고, ALL이면 모두") @Nullable ExposureStatusFilter status) {
}
