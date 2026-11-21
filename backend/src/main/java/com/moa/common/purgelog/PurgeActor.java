package com.moa.common.purgelog;

/**
 * 파기 처리자 (specs/013-purge-log FR-002).
 */
public enum PurgeActor {
	/** 매일 도는 정리기 */
	SCHEDULER,
	/** 사람이 한 개별 파기 (FR-005) */
	PERSON
}
