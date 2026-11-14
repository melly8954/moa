package com.moa.common.helper.richtext;

import com.moa.common.entity.BaseEntity;
import com.moa.common.entity.HardDelete;

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
 * 본문 이미지 (선택 헬퍼 rich-text). 올리면 TEMP, 글에 쓰이면 USED(owner_type·owner_id).
 */
@Getter
@Entity
@Table(name = "rich_text_images")
@HardDelete(reason = "글에 쓰이지 않은 이미지는 보관 시간이 지나면 파일과 함께 지운다")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RichTextImage extends BaseEntity {

	public enum Status {
		TEMP, USED
	}

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "public_id", nullable = false, length = 36, updatable = false)
	private String publicId;

	@Column(nullable = false, length = 300, updatable = false)
	private String path;

	@Column(name = "content_type", nullable = false, length = 50, updatable = false)
	private String contentType;

	@Column(nullable = false, updatable = false)
	private long size;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private Status status;

	@Column(name = "owner_type", length = 50)
	private String ownerType;

	@Column(name = "owner_id")
	private Long ownerId;

	public RichTextImage(String publicId, String path, String contentType, long size) {
		this.publicId = publicId;
		this.path = path;
		this.contentType = contentType;
		this.size = size;
		this.status = Status.TEMP;
	}

	public boolean usableBy(String ownerType, Long ownerId) {
		return status == Status.TEMP || ownerType.equals(this.ownerType) && ownerId.equals(this.ownerId);
	}

	public void use(String ownerType, Long ownerId) {
		this.status = Status.USED;
		this.ownerType = ownerType;
		this.ownerId = ownerId;
	}

	/** 글에서 빠졌다. 다시 임시가 되어 보관 시간이 지나면 지워진다 */
	public void release() {
		this.status = Status.TEMP;
		this.ownerType = null;
		this.ownerId = null;
	}
}
