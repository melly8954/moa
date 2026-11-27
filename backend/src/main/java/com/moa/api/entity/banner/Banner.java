package com.moa.api.entity.banner;

import java.time.LocalDateTime;

import org.hibernate.annotations.SQLRestriction;

import com.moa.api.dto.banner.BannerContent;
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
 * 홍보 배너 (specs/016-banner-popup). 이미지는 rich-text 헬퍼의 이미지 주소로 둔다 (research R1).
 * 상태는 칸에 두지 않고 켜짐과 노출 기간으로 정한다 (ExposureStatus).
 */
@Getter
@Entity
@Table(name = "tb_banner")
@SQLRestriction(SoftDeleteEntity.NOT_DELETED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Banner extends SoftDeleteEntity {

	/** 이미지의 글 종류 (rich-text 헬퍼 attach·detach) */
	public static final String OWNER_TYPE = "BANNER";

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 100)
	private String name;

	@Column(name = "wide_image_url", nullable = false, length = 100)
	private String wideImageUrl;

	@Column(name = "narrow_image_url", length = 100)
	private String narrowImageUrl;

	@Column(name = "alt_text", nullable = false, length = 200)
	private String altText;

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

	public Banner(BannerContent content) {
		apply(content);
	}

	/** 모든 칸을 바꾼다 */
	public void modify(BannerContent content) {
		apply(content);
	}

	public void changeEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	private void apply(BannerContent content) {
		this.name = content.name();
		this.wideImageUrl = content.wideImageUrl();
		this.narrowImageUrl = content.narrowImageUrl();
		this.altText = content.altText();
		this.linkUrl = content.linkUrl();
		this.startsAt = content.startsAt();
		this.endsAt = content.endsAt();
		this.sortOrder = content.sortOrder();
		this.enabled = content.enabled();
	}
}
