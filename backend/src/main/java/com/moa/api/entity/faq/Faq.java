package com.moa.api.entity.faq;

import org.hibernate.annotations.SQLRestriction;

import com.moa.api.dto.faq.FaqContent;
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
 * 자주 묻는 질문 (specs/017-customer-center FR-004). 답은 저장 때 걸러진 HTML이다 (rich-text).
 */
@Getter
@Entity
@Table(name = "tb_faq")
@SQLRestriction(SoftDeleteEntity.NOT_DELETED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Faq extends SoftDeleteEntity {

	/** 답 본문 이미지의 글 종류 (rich-text 헬퍼 attach·detach) */
	public static final String OWNER_TYPE = "FAQ";

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "category_code", nullable = false, length = 50)
	private String categoryCode;

	@Column(nullable = false, length = 200)
	private String question;

	@Column(nullable = false, columnDefinition = "MEDIUMTEXT")
	private String answer;

	@Column(name = "sort_order", nullable = false)
	private int sortOrder;

	@Column(nullable = false)
	private boolean enabled;

	public Faq(FaqContent content) {
		apply(content);
	}

	/** 모든 칸을 바꾼다 */
	public void modify(FaqContent content) {
		apply(content);
	}

	public void changeEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	private void apply(FaqContent content) {
		this.categoryCode = content.categoryCode();
		this.question = content.question();
		this.answer = content.answer();
		this.sortOrder = content.sortOrder();
		this.enabled = content.enabled();
	}
}
