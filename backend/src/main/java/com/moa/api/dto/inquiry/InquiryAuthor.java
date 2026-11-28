package com.moa.api.dto.inquiry;

import org.jspecify.annotations.Nullable;

/**
 * 문의를 쓴 회원 (관리 화면, specs/017-customer-center research R6). 회원 행이 없으면 아이디·이름이 비었다.
 */
public record InquiryAuthor(Long userId, @Nullable String loginId, @Nullable String name) {
}
