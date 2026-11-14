package com.moa.common.helper.richtext;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 본문 이미지 설정 (선택 헬퍼 rich-text).
 *
 * @param imageDir 저장 폴더
 * @param imageMaxSize 한 장의 최대 크기
 * @param tempRetention 임시 이미지를 지우기까지의 시간
 */
@Validated
@ConfigurationProperties("app.rich-text")
public record RichTextProperties(@NotBlank String imageDir, @NotNull DataSize imageMaxSize,
	@NotNull Duration tempRetention) {
}
