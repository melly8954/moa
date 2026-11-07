package com.moa.api.controller.v1.admin.user.request;

import org.jspecify.annotations.Nullable;

import com.moa.common.security.Role;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "사용자 검색 조건. 모두 선택")
public record UserSearchRequest(
	@Schema(description = "아이디나 이름에 들어 있는 글자", example = "hong") @Size(max = 50) @Nullable String keyword,
	@Schema(description = "시스템 역할", example = "USER") @Nullable Role role,
	@Schema(description = "잠김 여부") @Nullable Boolean locked) {
}
