package com.moa.api.dto.inquiry;

import java.util.List;

/**
 * 회원이 문의를 쓸 때 넣는 칸 (specs/017-customer-center FR-007). 내용은 서식 없는 글, 첨부는 rich-text 이미지 주소다.
 */
public record InquiryContent(String categoryCode, String title, String content, List<String> imageUrls) {

	public InquiryContent {
		imageUrls = List.copyOf(imageUrls);
	}
}
