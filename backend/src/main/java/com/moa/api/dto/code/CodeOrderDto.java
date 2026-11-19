package com.moa.api.dto.code;

/** 그룹 안 순서 바꾸기 한 줄 (moa, specs/004-admin-codes FR-005) */
public record CodeOrderDto(String code, int sortOrder) {
}
