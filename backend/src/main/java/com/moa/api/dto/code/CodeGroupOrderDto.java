package com.moa.api.dto.code;

/** 그룹 순서 바꾸기 한 줄 (moa, specs/004-admin-codes FR-005) */
public record CodeGroupOrderDto(String groupCode, int sortOrder) {
}
