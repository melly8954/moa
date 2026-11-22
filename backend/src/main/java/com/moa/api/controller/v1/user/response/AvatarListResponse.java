package com.moa.api.controller.v1.user.response;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "기본 아바타 목록")
public record AvatarListResponse(
	@Schema(description = "아바타 키 (순서대로). 그림은 프론트 /avatars/<키>.svg") List<String> avatars) {
}
