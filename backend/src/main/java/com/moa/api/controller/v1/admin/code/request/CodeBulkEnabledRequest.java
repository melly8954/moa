package com.moa.api.controller.v1.admin.code.request;

import java.util.List;

import com.moa.api.dto.code.CodeKeyDto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "moa: 그룹과 항목의 사용 여부 한꺼번에 바꾸기 요청. 여러 그룹에 걸쳐도 된다 (specs/004-admin-codes FR-005)")
public record CodeBulkEnabledRequest(
	@Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "그룹 코드들 (그룹 자체를 켜고 끈다)",
		example = "[\"GAME_GENRE\"]") @Size(max = 200) List<@NotBlank String> groupCodes,
	@Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED,
		description = "항목들") @Size(max = 1000) List<@Valid @NotNull CodeKey> codes,
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "사용 여부") @NotNull Boolean enabled) {

	public List<String> groupCodesOrEmpty() {
		return groupCodes == null ? List.of() : groupCodes;
	}

	public List<CodeKeyDto> codeKeys() {
		return codes == null ? List.of() : codes.stream().map(CodeKey::toDto).toList();
	}

	@Schema(hidden = true)
	@AssertTrue(message = "그룹이나 항목을 하나 이상 골라야 합니다")
	public boolean isAnySelected() {
		return !groupCodesOrEmpty().isEmpty() || (codes != null && !codes.isEmpty());
	}

	@Schema(description = "항목 하나")
	public record CodeKey(
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "그룹 코드",
			example = "GAME_GENRE") @NotBlank String groupCode,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "코드",
			example = "001") @NotBlank String code) {

		CodeKeyDto toDto() {
			return new CodeKeyDto(groupCode, code);
		}
	}
}
