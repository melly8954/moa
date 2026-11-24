package com.moa.api.entity.popup;

import java.time.LocalDateTime;

import org.hibernate.annotations.SQLRestriction;

import com.moa.api.dto.popup.PopupContent;
import com.moa.common.entity.SoftDeleteEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 팝업 (specs/016-banner-popup). 내용은 저장 때 걸러진 HTML이다 (rich-text).
 * 상태는 칸에 두지 않고 켜짐과 노출 기간으로 정한다 (ExposureStatus).
 */
@Getter
@Entity
@Table(name = "tb_popup")
@SQLRestriction(SoftDeleteEntity.NOT_DELETED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Popup extends SoftDeleteEntity {

	/** 본문 이미지의 글 종류 (rich-text 헬퍼 attach·detach) */
	public static final String OWNER_TYPE = "POPUP";

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 200)
	private String title;

	@Column(nullable = false, columnDefinition = "MEDIUMTEXT")
	private String content;

	@Column(name = "link_url", length = 500)
	private String linkUrl;

	@Column(name = "starts_at", nullable = false)
	private LocalDateTime startsAt;

	@Column(name = "ends_at")
	private LocalDateTime endsAt;

	@Column(name = "sort_order", nullable = false)
	private int sortOrder;

	@Column(nullable = false)
	private boolean enabled;

	public Popup(PopupContent content) {
		apply(content);
	}

	/** 모든 칸을 바꾼다 */
	public void modify(PopupContent content) {
		apply(content);
	}

	public void changeEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	private void apply(PopupContent content) {
		this.title = content.title();
		this.content = content.content();
		this.linkUrl = content.linkUrl();
		this.startsAt = content.startsAt();
		this.endsAt = content.endsAt();
		this.sortOrder = content.sortOrder();
		this.enabled = content.enabled();
	}
}
