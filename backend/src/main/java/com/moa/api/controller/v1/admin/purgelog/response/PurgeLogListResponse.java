package com.moa.api.controller.v1.admin.purgelog.response;

import java.time.OffsetDateTime;

import com.moa.api.dto.purgelog.PurgeLogDto;
import com.moa.common.purgelog.PurgeActor;
import com.moa.common.purgelog.PurgeMethod;
import com.moa.common.purgelog.PurgeTarget;
import com.moa.common.response.ApiTimes;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "파기 기록")
public record PurgeLogListResponse(
	@Schema(description = "파기 기록 ID") Long id,
	@Schema(description = "파기 일시 (시간대 포함)") OffsetDateTime createdAt,
	@Schema(description = "대상 종류") PurgeTarget target,
	@Schema(description = "방법") PurgeMethod method,
	@Schema(description = "건수", example = "3") int count,
	@Schema(description = "처리자") PurgeActor actor) {

	public static PurgeLogListResponse from(PurgeLogDto dto) {
		return new PurgeLogListResponse(dto.id(), ApiTimes.of(dto.createdAt()), dto.target(), dto.method(),
			dto.count(), dto.actor());
	}
}
