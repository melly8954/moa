package com.moa.api.entity.notice;

import java.time.LocalDateTime;

import org.hibernate.annotations.SQLRestriction;

import com.moa.api.dto.notice.NoticeStatus;
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
 * 공지 (specs/006-notices). 본문은 저장 때 걸러진 HTML이다.
 * 게시 시각은 처음 게시할 때만 정한다. 고치거나 고정해도 바뀌지 않아 새 공지 표시가 다시 생기지 않는다.
 */
@Getter
@Entity
@Table(name = "notices")
@SQLRestriction(SoftDeleteEntity.NOT_DELETED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notice extends SoftDeleteEntity {

	/** 본문 이미지의 글 종류 (rich-text 헬퍼 attach·detach) */
	public static final String OWNER_TYPE = "NOTICE";

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 200)
	private String title;

	@Column(nullable = false, columnDefinition = "MEDIUMTEXT")
	private String content;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private NoticeStatus status;

	@Column(nullable = false)
	private boolean pinned;

	@Column(name = "published_at")
	private LocalDateTime publishedAt;

	public Notice(String title, String content, boolean pinned) {
		this.title = title;
		this.content = content;
		this.pinned = pinned;
		this.status = NoticeStatus.DRAFT;
	}

	public boolean isPublished() {
		return status == NoticeStatus.PUBLISHED;
	}

	/**
	 * 임시 저장 공지를 게시한다. 이미 게시했으면 false를 돌려주고 아무것도 바꾸지 않는다.
	 */
	public boolean publish(LocalDateTime now) {
		if (isPublished()) {
			return false;
		}
		this.status = NoticeStatus.PUBLISHED;
		this.publishedAt = now;
		return true;
	}

	/** 제목과 본문만 바꾼다. 상태와 게시 시각은 그대로다 */
	public void modify(String title, String content) {
		this.title = title;
		this.content = content;
	}

	public void pin(boolean pinned) {
		this.pinned = pinned;
	}
}
