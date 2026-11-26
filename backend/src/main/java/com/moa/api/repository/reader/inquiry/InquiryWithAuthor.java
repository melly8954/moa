package com.moa.api.repository.reader.inquiry;

import com.moa.api.dto.inquiry.InquiryAuthor;
import com.moa.api.entity.inquiry.Inquiry;

/** 관리 목록 한 줄: 문의와 쓴 회원 (specs/017-customer-center research R6) */
public record InquiryWithAuthor(Inquiry inquiry, InquiryAuthor author) {
}
