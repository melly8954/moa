package com.moa.api.repository.reader.legal;

import java.util.List;

import org.springframework.stereotype.Component;

import com.moa.common.legal.LegalCatalog;
import com.moa.common.legal.LegalDocument;
import com.moa.common.legal.LegalVersion;

import lombok.RequiredArgsConstructor;

/**
 * 공개 문서 문안 조회 (specs/012-terms-privacy research R4). 저장소가 DB가 아니라 시작 때 읽은 문안(LegalCatalog)이다.
 */
@Component
@RequiredArgsConstructor
public class LegalReader {

	private final LegalCatalog legalCatalog;

	/** 문서의 버전들. 시행일 최근순 */
	public List<LegalVersion> findVersions(LegalDocument document) {
		return legalCatalog.versions(document);
	}
}
