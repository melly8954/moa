package com.moa.api.application.v1.notice;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.moa.api.dto.notice.NoticeDto;
import com.moa.api.dto.notice.NoticeStatus;
import com.moa.api.dto.notice.PublishedNoticeDto;
import com.moa.api.entity.notice.Notice;
import com.moa.api.service.notice.NoticeService;
import com.moa.common.audit.CurrentActor;
import com.moa.common.helper.richtext.RichTextImageService;

import lombok.RequiredArgsConstructor;

/**
 * 공지 (specs/006-notices). 본문 이미지는 같은 트랜잭션에서 rich-text 헬퍼에 잇는다.
 * 임시 저장 공지도 잇는다 (게시 전에 이미지가 정리되지 않게).
 * 게시하면 NoticePublishedEvent를 내고, 커밋 뒤 NoticeRealtimeListener가 전체 알림으로 보낸다 (US4, 007 R4).
 */
@Component
@RequiredArgsConstructor
public class NoticeApplication {

	private final NoticeService noticeService;
	private final RichTextImageService richTextImageService;
	private final CurrentActor currentActor;
	private final ApplicationEventPublisher eventPublisher;

	@Transactional(readOnly = true)
	public Page<NoticeDto> getAdminPage(String keyword, NoticeStatus status, Pageable pageable) {
		return noticeService.getAdminPage(keyword, status, pageable);
	}

	@Transactional(readOnly = true)
	public NoticeDto get(Long id) {
		return noticeService.get(id);
	}

	@Transactional(readOnly = true)
	public Page<PublishedNoticeDto> getPublishedPage(Pageable pageable) {
		return noticeService.getPublishedPage(pageable);
	}

	@Transactional(readOnly = true)
	public NoticeDto getPublished(Long id) {
		return noticeService.getPublished(id);
	}

	@Transactional
	public NoticeDto create(String title, String content, boolean pinned, boolean publish) {
		NoticeDto notice = noticeService.create(title, content, pinned, publish);
		richTextImageService.attach(Notice.OWNER_TYPE, notice.id(), notice.content());
		if (publish) {
			eventPublisher.publishEvent(new NoticePublishedEvent(notice.id(), notice.title()));
		}
		return notice;
	}

	@Transactional
	public NoticeDto modify(Long id, String title, String content) {
		NoticeDto notice = noticeService.modify(id, title, content);
		richTextImageService.attach(Notice.OWNER_TYPE, notice.id(), notice.content());
		return notice;
	}

	@Transactional
	public NoticeDto publish(Long id) {
		NoticeDto notice = noticeService.publish(id);
		eventPublisher.publishEvent(new NoticePublishedEvent(notice.id(), notice.title()));
		return notice;
	}

	@Transactional
	public NoticeDto pin(Long id, boolean pinned) {
		return noticeService.pin(id, pinned);
	}

	@Transactional
	public void delete(Long id) {
		noticeService.delete(id, currentActor.id());
		richTextImageService.detach(Notice.OWNER_TYPE, id);
	}

	@Transactional
	public void markSeen(Long userId) {
		noticeService.markSeen(userId);
	}

	@Transactional(readOnly = true)
	public boolean hasNew(Long userId) {
		return noticeService.hasNew(userId);
	}
}
