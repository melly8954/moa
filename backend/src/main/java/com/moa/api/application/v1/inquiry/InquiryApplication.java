package com.moa.api.application.v1.inquiry;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;
import org.springframework.context.ApplicationEventPublisher;
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
import com.moa.api.service.user.MemberStatusService;
import com.moa.common.audit.CurrentActor;
import com.moa.common.helper.richtext.RichTextImageService;
import com.moa.common.purgelog.PersonalDataPurgedEvent;
import com.moa.common.purgelog.PurgeTarget;

import lombok.RequiredArgsConstructor;

/**
 * 1:1 문의 (specs/017-customer-center US3·US4). 첨부와 답변 본문 이미지는 같은 트랜잭션에서 rich-text 헬퍼에 잇는다
 * (research R1·R2). 관리자·운영자 계정은 회원 문의 기능을 쓰지 않는다 (FR-011, research R4).
 * 탈퇴한 회원의 문의에는 답변하지 않고, 탈퇴한 날부터 보관 기간이 지나면 지운다 (US5, FR-017, research R10).
 */
@Component
@RequiredArgsConstructor
public class InquiryApplication {

	/** 답변 알림 제목 (FR-012). 문의 제목은 다른 회원에게 보이지 않는 정보라 넣지 않는다 */
	static final String ANSWERED_TITLE = "1:1 문의에 답변이 등록되었습니다";

	private final InquiryService inquiryService;
	private final MemberActivityPolicy memberActivityPolicy;
	private final MemberStatusService memberStatusService;
	private final SupportCategoryLoader supportCategoryLoader;
	private final NotificationApplication notificationApplication;
	private final RichTextImageService richTextImageService;
	private final CurrentActor currentActor;
	private final ApplicationEventPublisher eventPublisher;

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
		boolean withdrawn = memberStatusService.isWithdrawn(inquiryService.authorOf(id));
		return inquiryService.getForAdmin(id, supportCategoryLoader.load(), !withdrawn);
	}

	/** 답변을 저장하고, 처음 답변이면 문의를 쓴 회원에게 개인 알림을 보낸다 (FR-008, FR-012) */
	@Transactional
	public void answer(Long id, String content) {
		boolean withdrawn = memberStatusService.isWithdrawn(inquiryService.authorOf(id));
		AnswerResult result = inquiryService.answer(id, content, currentActor.id(), LocalDateTime.now(), withdrawn);
		richTextImageService.attach(Inquiry.ANSWER_OWNER_TYPE, id, content);
		if (result.first()) {
			notificationApplication.notify(result.userId(), NotificationType.INQUIRY_ANSWERED, ANSWERED_TITLE,
				"/support/inquiries/" + result.inquiryId());
		}
	}

	/**
	 * 탈퇴한 날부터 보관 기간이 지난 회원의 문의를 첨부·답변 이미지와 함께 지우고 파기 이력에 건수를 남긴다 (FR-017).
	 * 복구한 회원은 탈퇴 시각이 없어 빠진다. 여러 서버에서 같이 돌아도 이미 지운 행은 다시 찾지 않는다.
	 *
	 * @return 지운 문의 수
	 */
	@Transactional
	public int deleteOfWithdrawnMembers(int retentionDays) {
		var userIds = memberStatusService.findIdsWithdrawnBefore(LocalDateTime.now().minusDays(retentionDays));
		var deleted = inquiryService.deleteAllOfUsers(userIds);
		deleted.forEach(inquiryId -> {
			richTextImageService.detach(Inquiry.OWNER_TYPE, inquiryId);
			richTextImageService.detach(Inquiry.ANSWER_OWNER_TYPE, inquiryId);
		});
		eventPublisher.publishEvent(PersonalDataPurgedEvent.byScheduler(PurgeTarget.INQUIRY, deleted.size()));
		return deleted.size();
	}

	/** attach가 HTML만 받으므로 첨부 주소로 짧은 img 조각을 만든다 (배너와 같은 방식) */
	private static String imagesHtml(InquiryDto inquiry) {
		return inquiry.imageUrls().stream().map(url -> "<img src=\"" + url + "\">").collect(Collectors.joining());
	}
}
