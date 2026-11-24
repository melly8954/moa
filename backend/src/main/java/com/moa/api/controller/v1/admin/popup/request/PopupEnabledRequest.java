package com.moa.api.controller.v1.admin.popup.request;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "여러 팝업 켜기·끄기")
public record PopupEnabledRequest(
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "팝업 ID들",
		example = "[1, 2]") @NotEmpty @Size(max = 200) List<@NotNull Long> ids,
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "켜짐") @NotNull Boolean enabled) {
}
