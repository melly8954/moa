package com.moa.api.controller.v1.admin.code.request;

import static com.moa.api.controller.v1.admin.code.request.CodeGroupCreateRequest.blankToNull;

import org.jspecify.annotations.Nullable;

import com.moa.api.application.v1.code.command.ModifyCodeGroupCommand;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "moa: 코드 그룹 바꾸기 요청. 그룹 코드와 시스템 여부는 바꾸지 않는다")
public record CodeGroupModifyRequest(
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "이름",
		example = "이벤트 분류") @NotBlank @Size(max = 100) String name,
	@Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED,
		description = "설명") @Size(max = 500) @Nullable String description,
	@Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "쓰는 곳",
		example = "이벤트 등록 화면의 분류") @Size(max = 200) @Nullable String usedBy) {

	public ModifyCodeGroupCommand toCommand(String groupCode) {
		return new ModifyCodeGroupCommand(groupCode, name, blankToNull(description), blankToNull(usedBy));
	}
}
