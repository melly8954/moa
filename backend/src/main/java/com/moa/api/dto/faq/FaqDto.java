package com.moa.api.dto.faq;

import java.time.LocalDateTime;

import com.moa.api.dto.support.SupportCategories;
import com.moa.api.entity.faq.Faq;

public record FaqDto(Long id, String categoryCode, String categoryName, String question, String answer, int sortOrder,
	boolean enabled, LocalDateTime createdAt, LocalDateTime updatedAt) {

	public static FaqDto from(Faq faq, SupportCategories categories) {
		return new FaqDto(faq.getId(), faq.getCategoryCode(), categories.nameOf(faq.getCategoryCode()),
			faq.getQuestion(), faq.getAnswer(), faq.getSortOrder(), faq.isEnabled(), faq.getCreatedAt(),
			faq.getUpdatedAt());
	}
}
