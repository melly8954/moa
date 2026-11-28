package com.moa.api.entity.inquiry;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.moa.api.dto.inquiry.InquiryContent;
import com.moa.api.dto.inquiry.InquiryStatus;
import com.moa.common.entity.BaseEntity;
import com.moa.common.entity.HardDelete;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 1:1 문의 (specs/017-customer-center FR-007, FR-008). 문의 하나에 답변 하나다.
 * 상태는 칸에 두지 않고 answered_at으로 읽는다 (research R3). 회원은 문의를 고치지 않으므로 updated_*는 답변을 고친 관리자다.
 */
@Getter
@Entity
@Table(name = "tb_inquiry")
@HardDelete(reason = "개인정보가 든 행이다. 회원이 답변 대기 문의를 지우거나 탈퇴 뒤 1년이 지나면 지운다 (docs/privacy.md)")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Inquiry extends BaseEntity {

	/** 첨부 이미지의 글 종류 (rich-text 헬퍼 attach·detach) */
	public static final String OWNER_TYPE = "INQUIRY";

	/** 답변 본문 이미지의 글 종류. 첨부와 같은 문의 ID에 붙으므로 종류를 나눈다 (research R2) */
	public static final String ANSWER_OWNER_TYPE = "INQUIRY_ANSWER";

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false, updatable = false)
	private Long userId;

	@Column(name = "category_code", nullable = false, length = 50, updatable = false)
	private String categoryCode;

	@Column(nullable = false, length = 100, updatable = false)
	private String title;

	@Column(nullable = false, length = 2000, updatable = false)
	private String content;

	@ElementCollection
	@CollectionTable(name = "tb_inquiry_image", joinColumns = @JoinColumn(name = "inquiry_id"))
	@OrderColumn(name = "sort_order")
	@Column(name = "image_url", length = 100, nullable = false)
	private List<String> imageUrls = new ArrayList<>();

	@Column(columnDefinition = "MEDIUMTEXT")
	private String answer;

	@Column(name = "answered_at")
	private LocalDateTime answeredAt;

	@Column(name = "answered_by")
	private Long answeredBy;

	public Inquiry(Long userId, InquiryContent content) {
		this.userId = userId;
		this.categoryCode = content.categoryCode();
		this.title = content.title();
		this.content = content.content();
		this.imageUrls = new ArrayList<>(content.imageUrls());
	}

	public InquiryStatus status() {
		return answeredAt == null ? InquiryStatus.WAITING : InquiryStatus.ANSWERED;
	}

	public boolean isOwnedBy(Long userId) {
		return this.userId.equals(userId);
	}

	/**
	 * 답변을 쓴다. 처음이면 답변 시각과 답변한 관리자를 남긴다. 이미 답변했으면 내용만 바꾼다 (FR-008).
	 *
	 * @return 처음 답변이면 참 (알림을 보낸다, FR-012)
	 */
	public boolean answer(String answer, Long adminUserId, LocalDateTime now) {
		this.answer = answer;
		if (answeredAt != null) {
			return false;
		}
		this.answeredAt = now;
		this.answeredBy = adminUserId;
		return true;
	}
}
