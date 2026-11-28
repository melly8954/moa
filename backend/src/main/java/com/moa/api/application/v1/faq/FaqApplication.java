package com.moa.api.application.v1.faq;

import java.util.Collection;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.moa.api.application.v1.support.SupportCategoryLoader;
import com.moa.api.dto.faq.FaqContent;
import com.moa.api.dto.faq.FaqDto;
import com.moa.api.dto.support.SupportCategories.SupportCategory;
import com.moa.api.entity.faq.Faq;
import com.moa.api.service.faq.FaqService;
import com.moa.common.audit.CurrentActor;
import com.moa.common.helper.richtext.RichTextImageService;

import lombok.RequiredArgsConstructor;

/**
 * FAQ (specs/017-customer-center US1·US2). 답의 이미지는 같은 트랜잭션에서 rich-text 헬퍼에 잇는다 (research R2).
 */
@Component
@RequiredArgsConstructor
public class FaqApplication {

	private final FaqService faqService;
	private final SupportCategoryLoader supportCategoryLoader;
	private final RichTextImageService richTextImageService;
	private final CurrentActor currentActor;

	@Transactional(readOnly = true)
	public List<FaqDto> getAdminList(@Nullable String categoryCode, @Nullable Boolean enabled,
		@Nullable String keyword) {
		return faqService.getAdminList(categoryCode, enabled, keyword, supportCategoryLoader.load());
	}

	@Transactional(readOnly = true)
	public List<FaqDto> getPublicList(@Nullable String categoryCode, @Nullable String keyword) {
		return faqService.getPublicList(categoryCode, keyword, supportCategoryLoader.load());
	}

	/** 지금 고를 수 있는 고객센터 분류 (사용자 FAQ의 분류 단추, 문의·FAQ 쓰기의 분류 고르기) */
	@Transactional(readOnly = true)
	public List<SupportCategory> getActiveCategories() {
		return supportCategoryLoader.load().activeList();
	}

	@Transactional(readOnly = true)
	public FaqDto get(Long id) {
		return faqService.get(id, supportCategoryLoader.load());
	}

	@Transactional
	public FaqDto create(FaqContent content) {
		FaqDto faq = faqService.create(content, supportCategoryLoader.load());
		richTextImageService.attach(Faq.OWNER_TYPE, faq.id(), faq.answer());
		return faq;
	}

	@Transactional
	public FaqDto modify(Long id, FaqContent content) {
		FaqDto faq = faqService.modify(id, content, supportCategoryLoader.load());
		richTextImageService.attach(Faq.OWNER_TYPE, faq.id(), faq.answer());
		return faq;
	}

	@Transactional
	public void delete(Long id) {
		faqService.delete(id, currentActor.id());
		richTextImageService.detach(Faq.OWNER_TYPE, id);
	}

	@Transactional
	public void setEnabled(Collection<Long> ids, boolean enabled) {
		faqService.setEnabled(ids, enabled);
	}

	@Transactional
	public void deleteAll(Collection<Long> ids) {
		faqService.deleteAll(ids, currentActor.id()).forEach(id -> richTextImageService.detach(Faq.OWNER_TYPE, id));
	}
}
