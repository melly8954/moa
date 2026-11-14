package com.moa.api.application.v1.richtext;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.moa.common.helper.richtext.RichTextImageService;
import com.moa.common.helper.richtext.StoredImage;
import com.moa.common.helper.richtext.UploadedImage;

import lombok.RequiredArgsConstructor;

/**
 * 본문 이미지 API의 진입점 (선택 헬퍼 rich-text).
 */
@Component
@RequiredArgsConstructor
public class RichTextImageApplication {

	private final RichTextImageService richTextImageService;

	@Transactional
	public UploadedImage upload(MultipartFile file) {
		return richTextImageService.upload(file);
	}

	@Transactional(readOnly = true)
	public StoredImage load(String publicId) {
		return richTextImageService.load(publicId);
	}
}
