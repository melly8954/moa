package com.moa.common.response;

import java.util.List;
import java.util.function.Function;

import org.springframework.data.domain.Page;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 페이징 응답. Spring의 Page를 그대로 직렬화하지 않는다 (JSON 구조가 보장되지 않는다).
 */
@Schema(description = "페이징 응답")
public record PageResponse<T>(
	@Schema(description = "항목") List<T> content,
	@Schema(description = "페이지 번호 (0부터)") int page,
	@Schema(description = "페이지 크기") int size,
	@Schema(description = "전체 항목 수") long totalElements,
	@Schema(description = "전체 페이지 수") int totalPages) {

	public static <T> PageResponse<T> from(Page<T> page) {
		return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(),
			page.getTotalPages());
	}

	public static <S, T> PageResponse<T> from(Page<S> page, Function<S, T> mapper) {
		return from(page.map(mapper));
	}
}
