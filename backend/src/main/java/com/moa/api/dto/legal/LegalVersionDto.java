package com.moa.api.dto.legal;

import com.moa.common.legal.LegalVersion;

/**
 * 버전 하나와 오늘 기준 상태.
 *
 * @param currentVersion 지금 시행 중인 버전 이름 (예전·예정 버전 화면의 "지금 버전 보기")
 */
public record LegalVersionDto(LegalVersion version, VersionState state, String currentVersion) {
}
