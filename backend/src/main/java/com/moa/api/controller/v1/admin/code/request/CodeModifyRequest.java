package com.moa.api.controller.v1.admin.code.request;

import com.moa.api.application.v1.code.command.ModifyCodeCommand;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "코드 바꾸기 요청. 코드 값 자체는 바꾸지 않는다")
public record CodeModifyRequest(
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "이름",
		example = "결제 완료") @NotBlank @Size(max = 100) String name,
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "정렬 순서",
		example = "10") @NotNull @Min(0) Integer sortOrder,
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED,
		description = "사용 여부. false면 화면 목록에서 빠진다") @NotNull Boolean enabled) {

	public ModifyCodeCommand toCommand(String groupCode, String code) {
		return new ModifyCodeCommand(groupCode, code, name, sortOrder, enabled);
	}
}
