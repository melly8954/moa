package com.moa.api.service.notice;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moa.api.dto.notice.NoticeDto;
import com.moa.api.dto.notice.NoticeStatus;
import com.moa.api.entity.notice.Notice;
import com.moa.api.entity.notice.NoticeRead;
import com.moa.api.repository.reader.notice.NoticeReader;
import com.moa.api.repository.writer.notice.NoticeWriter;
import com.moa.common.exception.ErrorCode;
import com.moa.common.exception.ServiceException;

import lombok.RequiredArgsConstructor;

/**
 * 공지 (specs/006-notices). 게시 시각과 새 공지 판정은 서버 시각으로 한다.
 */
@Service
@RequiredArgsConstructor
public class NoticeService {

	private final NoticeReader noticeReader;
	private final NoticeWriter noticeWriter;

	public Page<NoticeDto> getAdminPage(String keyword, NoticeStatus status, Pageable pageable) {
		return noticeReader.findAdminPage(keyword, status, pageable).map(NoticeDto::from);
	}

	/**
	 * @throws ServiceException 없으면 R001
	 */
	public NoticeDto get(Long id) {
		return NoticeDto.from(require(id));
	}

	public Page<NoticeDto> getPublishedPage(Pageable pageable) {
		return noticeReader.findPublishedPage(pageable).map(NoticeDto::from);
	}

	/**
	 * @throws ServiceException 게시하지 않았거나 없으면 R001
	 */
	public NoticeDto getPublished(Long id) {
		Notice notice = require(id);
		if (!notice.isPublished()) {
			throw new ServiceException(ErrorCode.NOT_FOUND);
		}
		return NoticeDto.from(notice);
	}

	/** 만든다. publish면 바로 게시한다 */
	public NoticeDto create(String title, String content, boolean pinned, boolean publish) {
		Notice notice = new Notice(title, content, pinned);
		if (publish) {
			notice.publish(LocalDateTime.now());
		}
		return NoticeDto.from(noticeWriter.save(notice));
	}

	/**
	 * 제목과 본문만 바꾼다. 상태와 게시 시각은 그대로다.
	 *
	 * @throws ServiceException 없으면 R001
	 */
	public NoticeDto modify(Long id, String title, String content) {
		Notice notice = require(id);
		notice.modify(title, content);
		return NoticeDto.from(notice);
	}

	/**
	 * @throws ServiceException 없으면 R001, 이미 게시했으면 B001
	 */
	public NoticeDto publish(Long id) {
		Notice notice = require(id);
		if (!notice.publish(LocalDateTime.now())) {
			throw new ServiceException(ErrorCode.INVALID_STATE, "이미 게시한 공지입니다: " + id);
		}
		return NoticeDto.from(notice);
	}

	/**
	 * @throws ServiceException 없으면 R001
	 */
	public NoticeDto pin(Long id, boolean pinned) {
		Notice notice = require(id);
		notice.pin(pinned);
		return NoticeDto.from(notice);
	}

	/**
	 * @throws ServiceException 없으면 R001
	 */
	public void delete(Long id, Long actorId) {
		require(id).delete(actorId);
	}

	/** 지금을 이 사용자가 공지 목록을 마지막으로 연 시각으로 기록한다 */
	public void markSeen(Long userId) {
		LocalDateTime now = LocalDateTime.now();
		noticeReader.findRead(userId).ifPresentOrElse(read -> read.seen(now),
			() -> noticeWriter.saveRead(new NoticeRead(userId, now)));
	}

	/** 마지막으로 연 뒤 게시된 공지가 있나. 연 적이 없으면 게시한 공지가 하나라도 있나 */
	public boolean hasNew(Long userId) {
		LocalDateTime lastSeen = noticeReader.findRead(userId).map(NoticeRead::getLastSeenAt).orElse(null);
		return noticeReader.existsPublishedAfter(lastSeen);
	}

	private Notice require(Long id) {
		return noticeReader.findById(id).orElseThrow(() -> new ServiceException(ErrorCode.NOT_FOUND));
	}
}
