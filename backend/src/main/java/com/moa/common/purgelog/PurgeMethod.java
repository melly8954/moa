package com.moa.common.purgelog;

/**
 * 파기 방법 (specs/013-purge-log FR-002, docs/privacy.md "파기 방법").
 */
public enum PurgeMethod {
	/** 행 삭제 */
	ROW_DELETE,
	/** 행은 남기고 개인정보 열만 삭제 (회원 탈퇴 등) */
	COLUMN_ERASE
}
