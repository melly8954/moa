package com.moa.api.dto.user;

import java.time.LocalDateTime;

import com.moa.api.entity.user.User;

/**
 * 회원 상태 (specs/018-member-withdrawal FR-018, data-model "상태 변화").
 *
 * @param withdrawnAt 스스로 탈퇴한 시각. 파기한 뒤에도 남는다. 탈퇴하지 않았거나 관리자가 지웠으면 null
 * @param purgedAt 개인정보를 파기한 시각. 파기하지 않았으면 null
 */
public record MemberStatusDto(State state, LocalDateTime withdrawnAt, LocalDateTime purgedAt) {

	public enum State {
		/** 활동 중 */
		ACTIVE,
		/** 스스로 탈퇴함. 유예 중이거나 파기를 기다린다 */
		WITHDRAWN,
		/** 관리자가 지움 */
		DELETED,
		/** 개인정보를 파기함 */
		PURGED
	}

	public static MemberStatusDto from(User user) {
		return new MemberStatusDto(stateOf(user), user.getWithdrawnAt(), user.getPurgedAt());
	}

	private static State stateOf(User user) {
		if (user.getPurgedAt() != null) {
			return State.PURGED;
		}
		if (user.isDeleted()) {
			return State.DELETED;
		}
		return user.isWithdrawn() ? State.WITHDRAWN : State.ACTIVE;
	}
}
