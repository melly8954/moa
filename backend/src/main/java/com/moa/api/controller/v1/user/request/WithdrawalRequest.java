package com.moa.api.controller.v1.user.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 닉네임이 지금 닉네임과 같은지는 서비스가 칸 오류(V001, 칸 nickname)로 확인한다 */
@Schema(description = "회원 탈퇴 요청")
public record WithdrawalRequest(
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "확인용으로 입력한 지금 닉네임 (대소문자 구별)",
		example = "모아") @NotNull @Size(max = 100) String nickname) {
}
