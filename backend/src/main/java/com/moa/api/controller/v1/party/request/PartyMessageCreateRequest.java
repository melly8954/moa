package com.moa.api.controller.v1.party.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "채팅 보내기 (specs/008-party US9)")
public record PartyMessageCreateRequest(
	@Schema(description = "내용 1~500자. 글자만") @NotBlank @Size(min = 1, max = 500) String content) {
}
