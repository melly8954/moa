package com.moa.common.purgelog;

/**
 * 개인정보를 지웠다는 사실 (specs/013-purge-log research R1). 정리기가 지운 뒤 같은 트랜잭션에서 낸다.
 * 지운 데이터의 값(개인정보)은 담지 않는다 (FR-002).
 *
 * @param target 대상 종류
 * @param method 방법
 * @param count 지운 건수. 0 이하면 기록하지 않는다 (FR-003)
 * @param actorUserId 처리한 관리자 ID (정리기는 null)
 * @param targetUserId 대상 회원 ID (개별 파기가 아니면 null, FR-005)
 */
public record PersonalDataPurgedEvent(PurgeTarget target, PurgeMethod method, int count, Long actorUserId,
	Long targetUserId) {

	/** 정리기가 행을 지웠다 */
	public static PersonalDataPurgedEvent byScheduler(PurgeTarget target, int count) {
		return new PersonalDataPurgedEvent(target, PurgeMethod.ROW_DELETE, count, null, null);
	}

	public PurgeActor actor() {
		return actorUserId == null ? PurgeActor.SCHEDULER : PurgeActor.PERSON;
	}
}
