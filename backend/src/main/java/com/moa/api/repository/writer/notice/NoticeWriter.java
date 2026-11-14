package com.moa.api.repository.writer.notice;

import org.springframework.stereotype.Component;

import com.moa.api.entity.notice.Notice;
import com.moa.api.entity.notice.NoticeRead;
import com.moa.api.repository.jpa.notice.NoticeReadRepository;
import com.moa.api.repository.jpa.notice.NoticeRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class NoticeWriter {

	private final NoticeRepository noticeRepository;
	private final NoticeReadRepository noticeReadRepository;

	public Notice save(Notice notice) {
		return noticeRepository.save(notice);
	}

	public NoticeRead saveRead(NoticeRead read) {
		return noticeReadRepository.save(read);
	}
}
