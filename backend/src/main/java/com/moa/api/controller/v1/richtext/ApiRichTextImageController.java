package com.moa.api.controller.v1.richtext;

import java.time.Duration;

import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.moa.api.application.v1.richtext.RichTextImageApplication;
import com.moa.api.controller.v1.richtext.response.RichTextImageUploadResponse;
import com.moa.common.helper.richtext.StoredImage;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.PermitAll;
import lombok.RequiredArgsConstructor;

/**
 * 본문 이미지 (선택 헬퍼 rich-text). 올리기는 로그인한 사용자, 보여 주기는 로그인 없이 (img 태그는 토큰을 싣지 못한다).
 * 주소는 HtmlSanitizer.IMAGE_PATH_PREFIX와 같다.
 */
@Tag(name = "본문 이미지 API")
@RestController
@RequestMapping("/api/v1/rich-text/images")
@RequiredArgsConstructor
public class ApiRichTextImageController {

	private static final Duration CACHE = Duration.ofDays(365);

	private final RichTextImageApplication richTextImageApplication;

	@Operation(summary = "본문 이미지 올리기",
		description = "png, jpg, gif, webp만 (파일 내용으로 판별, SVG 안 됨). 크기 초과·형식 오류·빈 파일 V001. "
			+ "올린 이미지는 임시이고, 글을 저장할 때 본문에 쓰이면 남는다")
	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<RichTextImageUploadResponse> upload(@RequestParam("file") MultipartFile file) {
		return ResponseEntity.status(HttpStatus.CREATED)
			.body(RichTextImageUploadResponse.from(richTextImageApplication.upload(file)));
	}

	@Operation(summary = "본문 이미지 보여 주기", description = "로그인 없이 연다. 없으면 R001")
	@PermitAll
	@GetMapping("/{id}")
	public ResponseEntity<Resource> show(@PathVariable String id) {
		StoredImage image = richTextImageApplication.load(id);
		return ResponseEntity.ok()
			.contentType(MediaType.parseMediaType(image.contentType()))
			.cacheControl(CacheControl.maxAge(CACHE).cachePublic().immutable())
			.header("X-Content-Type-Options", "nosniff")
			.header("Content-Security-Policy", "default-src 'none'")
			.header(HttpHeaders.CONTENT_DISPOSITION, "inline")
			.body(image.resource());
	}
}
