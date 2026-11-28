package com.moa.api.repository.writer.faq;

import org.springframework.stereotype.Component;

import com.moa.api.entity.faq.Faq;
import com.moa.api.repository.jpa.faq.FaqRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class FaqWriter {

	private final FaqRepository faqRepository;

	public Faq save(Faq faq) {
		return faqRepository.save(faq);
	}
}
