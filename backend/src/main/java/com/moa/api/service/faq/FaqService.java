package com.moa.api.service.faq;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

import com.moa.api.dto.faq.FaqContent;
import com.moa.api.dto.faq.FaqDto;
import com.moa.api.dto.support.SupportCategories;
import com.moa.api.entity.faq.Faq;
import com.moa.api.repository.reader.faq.FaqReader;
import com.moa.api.repository.writer.faq.FaqWriter;
import com.moa.common.exception.ErrorCode;
import com.moa.common.exception.ServiceException;
import com.moa.common.response.ErrorResponse.FieldError;

import lombok.RequiredArgsConstructor;

/**
 * FAQ (specs/017-customer-center). 답의 이미지는 rich-text 헬퍼가 맡는다 (research R2).
 * 분류는 Application이 읽어 넘긴다 (research R8).
 */
@Service
@RequiredArgsConstructor
public class FaqService {

	/** 지우지 않은 FAQ의 상한 (FR-015) */
	public static final int LIMIT = 200;

	private final FaqReader faqReader;
	private final FaqWriter faqWriter;

	public List<FaqDto> getAdminList(@Nullable String categoryCode, @Nullable Boolean enabled,
		@Nullable String keyword, SupportCategories categories) {
		return faqReader.findAdminList(categoryCode, enabled, keyword).stream()
			.map(faq -> FaqDto.from(faq, categories))
			.toList();
	}

	public List<FaqDto> getPublicList(@Nullable String categoryCode, @Nullable String keyword,
		SupportCategories categories) {
		return faqReader.findPublicList(categories.active(), categoryCode, keyword).stream()
			.map(faq -> FaqDto.from(faq, categories))
			.toList();
	}

	/**
	 * @throws ServiceException 없으면 R001
	 */
	public FaqDto get(Long id, SupportCategories categories) {
		return FaqDto.from(require(id), categories);
	}

	/**
	 * @throws ServiceException 200개면 B301, 고를 수 없는 분류면 V001
	 */
	public FaqDto create(FaqContent content, SupportCategories categories) {
		if (faqReader.count() >= LIMIT) {
			throw new ServiceException(ErrorCode.FAQ_LIMIT_EXCEEDED);
		}
		requireActiveCategory(content.categoryCode(), categories);
		return FaqDto.from(faqWriter.save(new Faq(content)), categories);
	}

	/**
	 * 분류를 바꿀 때만 고를 수 있는 분류인지 본다. 그대로 두면 꺼진 분류여도 고칠 수 있다 (research R8).
	 *
	 * @throws ServiceException 없으면 R001, 바꾼 분류를 고를 수 없으면 V001
	 */
	public FaqDto modify(Long id, FaqContent content, SupportCategories categories) {
		Faq faq = require(id);
		if (!faq.getCategoryCode().equals(content.categoryCode())) {
			requireActiveCategory(content.categoryCode(), categories);
		}
		faq.modify(content);
		return FaqDto.from(faq, categories);
	}

	/**
	 * @throws ServiceException 없으면 R001
	 */
	public void delete(Long id, Long actorId) {
		require(id).delete(actorId);
	}

	/**
	 * 여러 FAQ를 한 번에 켜거나 끈다 (FR-005). 하나라도 없으면 아무것도 바꾸지 않는다. 이미 그 값인 FAQ는 그대로 둔다.
	 *
	 * @throws ServiceException 없는 FAQ R001
	 */
	public void setEnabled(Collection<Long> ids, boolean enabled) {
		requireAll(ids).forEach(faq -> faq.changeEnabled(enabled));
	}

	/**
	 * 여러 FAQ를 한 번에 지운다 (FR-005). 하나라도 없으면 아무것도 지우지 않는다.
	 *
	 * @return 지운 FAQ ID (답 이미지를 풀 때 쓴다)
	 * @throws ServiceException 없는 FAQ R001
	 */
	public List<Long> deleteAll(Collection<Long> ids, Long actorId) {
		List<Faq> targets = requireAll(ids);
		targets.forEach(faq -> faq.delete(actorId));
		return targets.stream().map(Faq::getId).toList();
	}

	private Faq require(Long id) {
		return faqReader.findById(id).orElseThrow(() -> new ServiceException(ErrorCode.NOT_FOUND));
	}

	/** 보낸 ID가 모두 있어야 한다 (겹친 ID는 하나로 본다) */
	private List<Faq> requireAll(Collection<Long> ids) {
		Map<Long, Faq> faqs = faqReader.findAllByIdIn(ids).stream()
			.collect(Collectors.toMap(Faq::getId, Function.identity()));
		List<Faq> found = new ArrayList<>();
		for (Long id : new LinkedHashSet<>(ids)) {
			Faq faq = faqs.get(id);
			if (faq == null) {
				throw new ServiceException(ErrorCode.NOT_FOUND, "없는 FAQ입니다: " + id);
			}
			found.add(faq);
		}
		return found;
	}

	private static void requireActiveCategory(String categoryCode, SupportCategories categories) {
		if (!categories.isActive(categoryCode)) {
			throw new ServiceException(ErrorCode.INVALID_INPUT, ErrorCode.INVALID_INPUT.getMessage(),
				List.of(new FieldError("categoryCode", "고를 수 없는 분류입니다.")));
		}
	}
}
