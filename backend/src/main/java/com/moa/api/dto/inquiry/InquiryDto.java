package com.moa.api.dto.inquiry;

import java.time.LocalDateTime;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.moa.api.dto.support.SupportCategories;
import com.moa.api.entity.inquiry.Inquiry;

/**
 * 1:1 문의 (specs/017-customer-center). 작성자와 답변한 관리자 이름은 관리 화면에서만 채운다.
 */
public record InquiryDto(Long id, Long userId, String categoryCode, String categoryName, String title, String content,
	List<String> imageUrls, InquiryStatus status, @Nullable String answer, @Nullable LocalDateTime answeredAt,
	@Nullable InquiryAuthor author, @Nullable String answeredByName, LocalDateTime createdAt) {

	public static InquiryDto from(Inquiry inquiry, SupportCategories categories) {
		return of(inquiry, categories, null, null);
	}

	public static InquiryDto of(Inquiry inquiry, SupportCategories categories, @Nullable InquiryAuthor author,
		@Nullable String answeredByName) {
		return new InquiryDto(inquiry.getId(), inquiry.getUserId(), inquiry.getCategoryCode(),
			categories.nameOf(inquiry.getCategoryCode()), inquiry.getTitle(), inquiry.getContent(),
			List.copyOf(inquiry.getImageUrls()), inquiry.status(), inquiry.getAnswer(), inquiry.getAnsweredAt(), author,
			answeredByName, inquiry.getCreatedAt());
	}

	public boolean deletable() {
		return status == InquiryStatus.WAITING;
	}
}
