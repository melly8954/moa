package com.moa.api.entity.notice;

import java.time.LocalDateTime;

import com.moa.common.entity.HardDelete;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 공지 확인 기록 (specs/006-notices FR-008). 사용자가 공지 목록을 마지막으로 연 시각. 사용자마다 한 행이다.
 */
@Getter
@Entity
@Table(name = "notice_reads")
@HardDelete(reason = "사용자마다 마지막 확인 시각 한 행만 덮어쓴다. 지울 일이 없고 이력이 아니다")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NoticeRead {

	@Id
	@Column(name = "user_id")
	private Long userId;

	@Column(name = "last_seen_at", nullable = false)
	private LocalDateTime lastSeenAt;

	public NoticeRead(Long userId, LocalDateTime lastSeenAt) {
		this.userId = userId;
		this.lastSeenAt = lastSeenAt;
	}

	public void seen(LocalDateTime now) {
		this.lastSeenAt = now;
	}
}
