package com.moa.api.entity.party;

import org.hibernate.annotations.SQLRestriction;

import com.moa.api.dto.party.JoinPolicy;
import com.moa.api.dto.party.PartyStatus;
import com.moa.common.entity.SoftDeleteEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 파티 (specs/008-party). 게임을 함께 할 회원을 모으는 임시 모임이다. 해산하면 소프트 삭제한다.
 * 상태 전이는 data-model.md "상태 전이"를 따른다.
 */
@Getter
@Entity
@Table(name = "tb_party")
@SQLRestriction(SoftDeleteEntity.NOT_DELETED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Party extends SoftDeleteEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "game_code", nullable = false, length = 50)
	private String gameCode;

	@Column(name = "mode_code", length = 50)
	private String modeCode;

	@Column(name = "tier_min_code", length = 50)
	private String tierMinCode;

	@Column(name = "tier_max_code", length = 50)
	private String tierMaxCode;

	@Column(nullable = false, length = 50)
	private String title;

	@Column(nullable = false, length = 1000)
	private String description;

	@Column(nullable = false, columnDefinition = "TINYINT")
	private int capacity;

	@Enumerated(EnumType.STRING)
	@Column(name = "join_policy", nullable = false, length = 20)
	private JoinPolicy joinPolicy;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private PartyStatus status;

	@Column(name = "closed_by_host", nullable = false)
	private boolean closedByHost;

	@Column(name = "host_user_id", nullable = false)
	private Long hostUserId;

	@Column(name = "position_enabled", nullable = false)
	private boolean positionEnabled;

	@Column(name = "invite_code", nullable = false, columnDefinition = "CHAR(8)")
	private String inviteCode;

	public Party(PartySettings settings, JoinPolicy joinPolicy, Long hostUserId, String inviteCode) {
		apply(settings);
		this.joinPolicy = joinPolicy;
		this.hostUserId = hostUserId;
		this.inviteCode = inviteCode;
		this.status = PartyStatus.RECRUITING;
		this.closedByHost = false;
	}

	/** 방장이 고칠 수 있는 칸을 바꾼다 (US7-AC1). 상태와 참가 방식은 그대로다 */
	public void modify(PartySettings settings) {
		apply(settings);
	}

	private void apply(PartySettings settings) {
		this.gameCode = settings.gameCode();
		this.modeCode = settings.modeCode();
		this.tierMinCode = settings.tierMinCode();
		this.tierMaxCode = settings.tierMaxCode();
		this.title = settings.title();
		this.description = settings.description();
		this.capacity = settings.capacity();
		this.positionEnabled = settings.positionEnabled();
	}

	public boolean isRecruiting() {
		return status == PartyStatus.RECRUITING;
	}

	/**
	 * 마감한다. 정원이 차서 마감하면 byHost = false, 방장이 마감하면 true다 (FR-008)
	 */
	public void close(boolean byHost) {
		this.status = PartyStatus.CLOSED;
		this.closedByHost = byHost;
	}

	/** 다시 모집한다. 자리가 남았는지는 부르는 쪽이 본다 */
	public void reopen() {
		this.status = PartyStatus.RECRUITING;
		this.closedByHost = false;
	}

	public void changeHost(Long userId) {
		this.hostUserId = userId;
	}

	public void changeJoinPolicy(JoinPolicy joinPolicy) {
		this.joinPolicy = joinPolicy;
	}

	/** 초대 코드를 새로 만든다. 이전 코드는 더 쓰지 못한다 (US7-AC7) */
	public void regenerateInviteCode(String inviteCode) {
		this.inviteCode = inviteCode;
	}

	public boolean isHost(Long userId) {
		return hostUserId.equals(userId);
	}
}
