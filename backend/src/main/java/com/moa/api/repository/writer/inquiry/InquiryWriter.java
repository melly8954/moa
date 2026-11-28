package com.moa.api.repository.writer.inquiry;

import org.springframework.stereotype.Component;

import com.moa.api.entity.inquiry.Inquiry;
import com.moa.api.repository.jpa.inquiry.InquiryRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class InquiryWriter {

	private final InquiryRepository inquiryRepository;

	public Inquiry save(Inquiry inquiry) {
		return inquiryRepository.save(inquiry);
	}

	/** 하드 삭제. 첨부 행도 함께 지워진다 (컬렉션 테이블) */
	public void delete(Inquiry inquiry) {
		inquiryRepository.delete(inquiry);
	}
}
