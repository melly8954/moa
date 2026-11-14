package com.moa.api.controller.v1.richtext.response;

import com.moa.common.helper.richtext.UploadedImage;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "올린 본문 이미지")
public record RichTextImageUploadResponse(
	@Schema(description = "이미지 ID", example = "3f2b8c1e-9a4d-4e7b-8c2a-1d5e6f7a8b9c") String id,
	@Schema(description = "본문 img src에 그대로 넣을 주소",
		example = "/api/v1/rich-text/images/3f2b8c1e-9a4d-4e7b-8c2a-1d5e6f7a8b9c") String url) {

	public static RichTextImageUploadResponse from(UploadedImage image) {
		return new RichTextImageUploadResponse(image.id(), image.url());
	}
}
