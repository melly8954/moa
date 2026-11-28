package com.moa.api.service.inquiry;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moa.api.dto.inquiry.InquiryContent;
import com.moa.api.dto.inquiry.InquiryDto;
import com.moa.api.dto.inquiry.InquiryStatusFilter;
import com.moa.api.dto.support.SupportCategories;
import com.moa.api.entity.inquiry.Inquiry;
import com.moa.api.repository.reader.inquiry.InquiryReader;
import com.moa.api.repository.writer.inquiry.InquiryWriter;
import com.moa.common.exception.ErrorCode;
import com.moa.common.exception.ServiceException;
import com.moa.common.helper.richtext.HtmlSanitizer;
import com.moa.common.helper.richtext.RichTextImageService;
import com.moa.common.response.ErrorResponse.FieldError;

import lombok.RequiredArgsConstructor;

/**
 * 1:1 문의 (specs/017-customer-center US3·US4). 첨부는 rich-text 이미지 주소다 (research R1).
 * 분류는 Application이 읽어 넘긴다 (research R8).
 */
@Service
@RequiredArgsConstructor
public class InquiryService {

	private final InquiryReader inquiryReader;
	private final InquiryWriter inquiryWriter;
	private final RichTextImageService richTextImageService;

	public Page<InquiryDto> getMyPage(Long userId, Pageable pageable, SupportCategories categories) {
		return inquiryReader.findMyPage(userId, pageable).map(inquiry -> InquiryDto.from(inquiry, categories));
	}

	/**
	 * 남의 문의도 없는 문의로 본다 (FR-010, research R5).
	 *
	 * @throws ServiceException 없거나 남의 것이면 R001
	 */
	public InquiryDto getMine(Long userId, Long id, SupportCategories categories) {
		return InquiryDto.from(requireMine(userId, id), categories);
	}

	/**
	 * @throws ServiceException 고를 수 없는 분류나 올리지 않은 이미지면 V001
	 */
	public InquiryDto create(Long userId, InquiryContent content, SupportCategories categories) {
		validate(content, categories);
		return InquiryDto.from(inquiryWriter.save(new Inquiry(userId, content)), categories);
	}

	/**
	 * 답변 대기인 자기 문의만 지운다 (FR-009). 하드 삭제다 (research R5).
	 *
	 * @throws ServiceException 없거나 남의 것이면 R001, 답변 완료면 B001
	 */
	public void deleteMine(Long userId, Long id) {
		Inquiry inquiry = requireMine(userId, id);
		if (inquiry.getAnsweredAt() != null) {
			throw new ServiceException(ErrorCode.INVALID_STATE, "답변한 문의는 지울 수 없습니다.");
		}
		inquiryWriter.delete(inquiry);
	}

	/**
	 * @throws ServiceException from이 to보다 늦으면 V001
	 */
	public Page<InquiryDto> getAdminPage(@Nullable InquiryStatusFilter status, @Nullable String categoryCode,
		@Nullable LocalDate from, @Nullable LocalDate to, @Nullable String keyword, Pageable pageable,
		SupportCategories categories) {
		if (from != null && to != null && from.isAfter(to)) {
			throw new ServiceException(ErrorCode.INVALID_INPUT, "시작일(from)이 종료일(to)보다 늦습니다.");
		}
		return inquiryReader.findAdminPage(status, categoryCode, from, to, keyword, pageable)
			.map(row -> InquiryDto.of(row.inquiry(), categories, row.author(), null, true));
	}

	/**
	 * 문의를 쓴 회원 ID. 답변 전에 작성자가 탈퇴했는지 볼 때 쓴다 (US5).
	 *
	 * @throws ServiceException 없으면 R001
	 */
	public Long authorOf(Long id) {
		return require(id).getUserId();
	}

	/**
	 * @param answerable 답변할 수 있는지 (작성자가 탈퇴하지 않았다)
	 * @throws ServiceException 없으면 R001
	 */
	public InquiryDto getForAdmin(Long id, SupportCategories categories, boolean answerable) {
		Inquiry inquiry = require(id);
		String answeredByName = inquiry.getAnsweredBy() == null
			? null
			: inquiryReader.findUserName(inquiry.getAnsweredBy()).orElse(null);
		return InquiryDto.of(inquiry, categories, inquiryReader.findAuthor(inquiry.getUserId()), answeredByName,
			answerable);
	}

	/**
	 * 답변을 저장한다. 행을 잠가 읽으므로 두 관리자가 함께 처음 답변해도 "처음"은 하나다 (research R3).
	 *
	 * @param authorWithdrawn 작성자가 탈퇴했는지. 탈퇴했으면 답변하지 않는다 (US5, FR-017)
	 * @return 처음 답변이면 알림을 보낼 회원과 함께 준다
	 * @throws ServiceException 없으면 R001, 작성자가 탈퇴했으면 B001
	 */
	public AnswerResult answer(Long id, String content, Long adminUserId, LocalDateTime now,
		boolean authorWithdrawn) {
		Inquiry inquiry = inquiryReader.findByIdForUpdate(id)
			.orElseThrow(() -> new ServiceException(ErrorCode.NOT_FOUND));
		if (authorWithdrawn) {
			throw new ServiceException(ErrorCode.INVALID_STATE, "탈퇴한 회원의 문의라 답변할 수 없습니다.");
		}
		boolean first = inquiry.answer(content, adminUserId, now);
		return new AnswerResult(inquiry.getId(), inquiry.getUserId(), first);
	}

	/**
	 * 이 회원들이 쓴 문의를 모두 지운다 (탈퇴한 날부터 1년이 지난 회원, FR-017). 하드 삭제이고 첨부 행도 함께 지워진다.
	 *
	 * @return 지운 문의 ID (첨부·답변 이미지를 풀 때 쓴다)
	 */
	public List<Long> deleteAllOfUsers(Collection<Long> userIds) {
		if (userIds.isEmpty()) {
			return List.of();
		}
		List<Inquiry> targets = inquiryReader.findAllByUserIdIn(userIds);
		targets.forEach(inquiryWriter::delete);
		return targets.stream().map(Inquiry::getId).toList();
	}

	/**
	 * @param inquiryId 답변한 문의
	 * @param userId 문의를 쓴 회원
	 * @param first 처음 답변이면 참
	 */
	public record AnswerResult(Long inquiryId, Long userId, boolean first) {
	}

	private Inquiry require(Long id) {
		return inquiryReader.findById(id).orElseThrow(() -> new ServiceException(ErrorCode.NOT_FOUND));
	}

	private Inquiry requireMine(Long userId, Long id) {
		return inquiryReader.findById(id)
			.filter(inquiry -> inquiry.isOwnedBy(userId))
			.orElseThrow(() -> new ServiceException(ErrorCode.NOT_FOUND));
	}

	private void validate(InquiryContent content, SupportCategories categories) {
		List<FieldError> errors = new ArrayList<>();
		if (!categories.isActive(content.categoryCode())) {
			errors.add(new FieldError("categoryCode", "고를 수 없는 분류입니다."));
		}
		for (String url : content.imageUrls()) {
			if (!isUploadedImage(url)) {
				errors.add(new FieldError("imageUrls", "이미지를 찾을 수 없습니다. 다시 올려 주세요."));
				break;
			}
		}
		if (!errors.isEmpty()) {
			throw new ServiceException(ErrorCode.INVALID_INPUT, ErrorCode.INVALID_INPUT.getMessage(), errors);
		}
	}

	/** rich-text로 올린 이미지 주소이고 저장된 이미지여야 한다 (research R1) */
	private boolean isUploadedImage(String url) {
		if (!url.startsWith(HtmlSanitizer.IMAGE_PATH_PREFIX)) {
			return false;
		}
		try {
			richTextImageService.load(url.substring(HtmlSanitizer.IMAGE_PATH_PREFIX.length()));
			return true;
		} catch (ServiceException e) {
			return false;
		}
	}
}
