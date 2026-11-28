package com.moa.api.controller.v1.user.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 규칙(2~12자, 글자, 예약어)은 서비스가 칸 오류(V001, 칸 nickname)로 확인한다 */
@Schema(description = "닉네임 바꾸기 요청")
public record NicknameChangeRequest(
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "새 닉네임. 2~12자 한글·영문·숫자·_",
		example = "모아") @NotNull @Size(max = 100) String nickname) {
}
