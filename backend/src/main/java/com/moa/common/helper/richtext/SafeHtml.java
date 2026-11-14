package com.moa.common.helper.richtext;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;

import tools.jackson.databind.annotation.JsonDeserialize;

/**
 * 요청 DTO의 서식 있는 글 칸에 붙인다 (선택 헬퍼 rich-text). JSON을 읽을 때 HtmlSanitizer로 거른다.
 * 검증(@Size 등)은 거른 뒤의 값으로 한다.
 *
 * <pre>{@code
 * public record NoticeCreateRequest(@NotBlank String title, @SafeHtml @NotBlank @Size(max = 20000) String content) {}
 * }</pre>
 */
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@JacksonAnnotationsInside
@JsonDeserialize(using = SafeHtmlDeserializer.class)
public @interface SafeHtml {
	// 속성 없음. JSON 문자열을 HtmlSanitizer.clean으로 거른다
}
