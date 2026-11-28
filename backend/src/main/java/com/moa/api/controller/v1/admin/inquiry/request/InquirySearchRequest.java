package com.moa.api.controller.v1.admin.inquiry.request;

import java.time.LocalDate;

import org.jspecify.annotations.Nullable;
import org.springframework.format.annotation.DateTimeFormat;

import com.moa.api.dto.inquiry.InquiryStatusFilter;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "문의 관리 검색 조건. 모두 선택이고 함께 주면 AND")
public record InquirySearchRequest(
	@Schema(description = "상태. 비면 답변 대기, ALL이면 모두") @Nullable InquiryStatusFilter status,
	@Schema(description = "분류 코드", example = "ACCOUNT") @Size(max = 50) @Nullable String categoryCode,
	@Schema(description = "쓴 날짜 시작 (포함)", example = "2026-11-01") @DateTimeFormat(
		iso = DateTimeFormat.ISO.DATE) @Nullable LocalDate from,
	@Schema(description = "쓴 날짜 끝 (포함, 그날 끝까지). from보다 이르면 V001", example = "2026-11-30") @DateTimeFormat(
		iso = DateTimeFormat.ISO.DATE) @Nullable LocalDate to,
	@Schema(description = "제목, 작성자 아이디·이름에 들어 있는 글자", example = "로그인") @Size(
		max = 200) @Nullable String keyword) {
}
