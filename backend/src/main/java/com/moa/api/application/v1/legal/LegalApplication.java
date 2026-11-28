package com.moa.api.application.v1.legal;

import java.time.LocalDate;
import java.time.ZoneId;

import org.springframework.stereotype.Component;

import com.moa.api.dto.legal.LegalDocumentDto;
import com.moa.api.dto.legal.LegalVersionDto;
import com.moa.api.service.legal.LegalService;
import com.moa.common.legal.LegalProperties;

import lombok.RequiredArgsConstructor;

/**
 * 공개 문서 (specs/012-terms-privacy research R4·R5). DB를 쓰지 않아 트랜잭션이 없다.
 * 오늘은 요청마다 app.legal.zone(기본 Asia/Seoul)의 날짜로 구한다. 시행일이 되면 배포 없이 지금 버전이 바뀐다.
 */
@Component
@RequiredArgsConstructor
public class LegalApplication {

	private final LegalService legalService;
	private final LegalProperties legalProperties;

	public LegalDocumentDto getDocument(String document) {
		return legalService.document(document, today());
	}

	public LegalVersionDto getVersion(String document, String version) {
		return legalService.version(document, version, today());
	}

	private LocalDate today() {
		return LocalDate.now(ZoneId.of(legalProperties.zone()));
	}
}
