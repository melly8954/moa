package com.moa.api.service.legal;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

import com.moa.api.dto.legal.LegalDocumentDto;
import com.moa.api.dto.legal.LegalVersionDto;
import com.moa.api.dto.legal.VersionState;
import com.moa.api.repository.reader.legal.LegalReader;
import com.moa.common.exception.ErrorCode;
import com.moa.common.exception.ServiceException;
import com.moa.common.legal.LegalDocument;
import com.moa.common.legal.LegalVersion;

import lombok.RequiredArgsConstructor;

/**
 * 공개 문서의 지금·예정·예전 버전 고르기 (specs/012-terms-privacy research R4·R5). 오늘은 Application이 넘긴다.
 *
 * <pre>
 * 시행일 > 오늘                       → UPCOMING
 * 시행일 ≤ 오늘 중 시행일이 가장 늦은 것 → CURRENT
 * 그 밖                              → PAST
 * </pre>
 */
@Service
@RequiredArgsConstructor
public class LegalService {

	private final LegalReader legalReader;

	/**
	 * @param key 문서 키 (terms, privacy)
	 * @return 지금 버전, 시행일이 가장 이른 예정 버전, 버전 목록(시행일 최근순)
	 * @throws ServiceException 없는 문서면 R001
	 */
	public LegalDocumentDto document(String key, LocalDate today) {
		List<LegalVersionDto> versions = withState(requireDocument(key), today);
		LegalVersionDto current = versions.stream().filter(v -> v.state() == VersionState.CURRENT).findFirst()
			.orElseThrow(() -> new ServiceException(ErrorCode.NOT_FOUND));
		// 시행일 최근순이라 예정 버전 중 마지막이 시행일이 가장 이르다
		LegalVersionDto upcoming = versions.stream().filter(v -> v.state() == VersionState.UPCOMING)
			.reduce((later, earlier) -> earlier).orElse(null);
		return new LegalDocumentDto(current, upcoming, versions);
	}

	/**
	 * @throws ServiceException 없는 문서나 버전이면 R001
	 */
	public LegalVersionDto version(String key, String version, LocalDate today) {
		return withState(requireDocument(key), today).stream()
			.filter(v -> v.version().version().equals(version))
			.findFirst()
			.orElseThrow(() -> new ServiceException(ErrorCode.NOT_FOUND));
	}

	private LegalDocument requireDocument(String key) {
		return LegalDocument.fromKey(key).orElseThrow(() -> new ServiceException(ErrorCode.NOT_FOUND));
	}

	/** 시행일 최근순 목록에 상태를 붙인다. 오늘 이전 중 첫 번째가 지금 버전이다 */
	private List<LegalVersionDto> withState(LegalDocument document, LocalDate today) {
		List<LegalVersion> versions = legalReader.findVersions(document);
		LegalVersion current = versions.stream().filter(v -> !v.effectiveDate().isAfter(today)).findFirst()
			.orElseThrow(() -> new ServiceException(ErrorCode.NOT_FOUND));
		return versions.stream()
			.map(v -> new LegalVersionDto(v, stateOf(v, current, today), current.version()))
			.toList();
	}

	private static VersionState stateOf(LegalVersion version, LegalVersion current, LocalDate today) {
		if (version.effectiveDate().isAfter(today)) {
			return VersionState.UPCOMING;
		}
		return version.equals(current) ? VersionState.CURRENT : VersionState.PAST;
	}
}
