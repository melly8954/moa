package com.moa.api.dto.code;

import java.util.List;

/**
 * 공통코드 트리 표의 그룹 하나 (moa, specs/004-admin-codes FR-005, research R7)
 *
 * @param codes 그룹의 항목 (사용 안 함 포함, 정렬 순서대로)
 */
public record CodeGroupTreeDto(CodeGroupDto group, List<CodeDto> codes) {
}
