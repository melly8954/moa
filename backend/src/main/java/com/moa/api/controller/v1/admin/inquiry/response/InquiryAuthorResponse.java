package com.moa.api.controller.v1.admin.inquiry.response;

import org.jspecify.annotations.Nullable;

import com.moa.api.dto.inquiry.InquiryAuthor;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "문의를 쓴 회원")
public record InquiryAuthorResponse(
	@Schema(description = "회원 ID") Long userId,
	@Schema(description = "아이디. 회원 행이 없으면 없다") @Nullable String loginId,
	@Schema(description = "이름. 회원 행이 없으면 없다") @Nullable String name) {

	public static @Nullable InquiryAuthorResponse from(@Nullable InquiryAuthor author) {
		return author == null ? null : new InquiryAuthorResponse(author.userId(), author.loginId(), author.name());
	}
}
