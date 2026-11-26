package com.moa.api.application.v1.inquiry;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.moa.api.application.v1.notification.NotificationApplication;
import com.moa.api.application.v1.support.SupportCategoryLoader;
import com.moa.api.dto.inquiry.InquiryContent;
import com.moa.api.dto.inquiry.InquiryDto;
import com.moa.api.dto.inquiry.InquiryStatusFilter;
import com.moa.api.dto.notification.NotificationType;
import com.moa.api.entity.inquiry.Inquiry;
import com.moa.api.service.inquiry.InquiryService;
import com.moa.api.service.inquiry.InquiryService.AnswerResult;
import com.moa.api.service.user.MemberActivityPolicy;
import com.moa.common.audit.CurrentActor;
import com.moa.common.helper.richtext.RichTextImageService;

import lombok.RequiredArgsConstructor;

/**
 * 1:1 문의 (specs/017-customer-center US3·US4). 첨부와 답변 본문 이미지는 같은 트랜잭션에서 rich-text 헬퍼에 잇는다
 * (research R1·R2). 관리자·운영자 계정은 회원 문의 기능을 쓰지 않는다 (FR-011, research R4).
 */
@Component
@RequiredArgsConstructor
public class InquiryApplication {

	/** 답변 알림 제목 (FR-012). 문의 제목은 다른 회원에게 보이지 않는 정보라 넣지 않는다 */
	static final String ANSWERED_TITLE = "1:1 문의에 답변이 등록되었습니다";

	private final InquiryService inquiryService;
	private final MemberActivityPolicy memberActivityPolicy;
	private final SupportCategoryLoader supportCategoryLoader;
	private final NotificationApplication notificationApplication;
	private final RichTextImageService richTextImageService;
	private final CurrentActor currentActor;

	@Transactional(readOnly = true)
	public Page<InquiryDto> getMyPage(Long userId, Pageable pageable) {
		memberActivityPolicy.requireMember(userId);
		return inquiryService.getMyPage(userId, pageable, supportCategoryLoader.load());
	}

	@Transactional(readOnly = true)
	public InquiryDto getMine(Long userId, Long id) {
		memberActivityPolicy.requireMember(userId);
		return inquiryService.getMine(userId, id, supportCategoryLoader.load());
	}

	@Transactional
	public InquiryDto create(Long userId, InquiryContent content) {
		memberActivityPolicy.requireMember(userId);
		InquiryDto inquiry = inquiryService.create(userId, content, supportCategoryLoader.load());
		richTextImageService.attach(Inquiry.OWNER_TYPE, inquiry.id(), imagesHtml(inquiry));
		return inquiry;
	}

	@Transactional
	public void deleteMine(Long userId, Long id) {
		memberActivityPolicy.requireMember(userId);
		inquiryService.deleteMine(userId, id);
		richTextImageService.detach(Inquiry.OWNER_TYPE, id);
		richTextImageService.detach(Inquiry.ANSWER_OWNER_TYPE, id);
	}

	@Transactional(readOnly = true)
	public Page<InquiryDto> getAdminPage(@Nullable InquiryStatusFilter status, @Nullable String categoryCode,
		@Nullable LocalDate from, @Nullable LocalDate to, @Nullable String keyword, Pageable pageable) {
		return inquiryService.getAdminPage(status, categoryCode, from, to, keyword, pageable,
			supportCategoryLoader.load());
	}

	@Transactional(readOnly = true)
	public InquiryDto getForAdmin(Long id) {
		return inquiryService.getForAdmin(id, supportCategoryLoader.load());
	}

	/** 답변을 저장하고, 처음 답변이면 문의를 쓴 회원에게 개인 알림을 보낸다 (FR-008, FR-012) */
	@Transactional
	public void answer(Long id, String content) {
		AnswerResult result = inquiryService.answer(id, content, currentActor.id(), LocalDateTime.now());
		richTextImageService.attach(Inquiry.ANSWER_OWNER_TYPE, id, content);
		if (result.first()) {
			notificationApplication.notify(result.userId(), NotificationType.INQUIRY_ANSWERED, ANSWERED_TITLE,
				"/support/inquiries/" + result.inquiryId());
		}
	}

	/** attach가 HTML만 받으므로 첨부 주소로 짧은 img 조각을 만든다 (배너와 같은 방식) */
	private static String imagesHtml(InquiryDto inquiry) {
		return inquiry.imageUrls().stream().map(url -> "<img src=\"" + url + "\">").collect(Collectors.joining());
	}
}
