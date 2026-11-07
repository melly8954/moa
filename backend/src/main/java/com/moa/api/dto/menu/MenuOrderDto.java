package com.moa.api.dto.menu;

/**
 * 순서 일괄 변경의 한 줄.
 *
 * @param parentId 없으면 1단계
 */
public record MenuOrderDto(Long id, Long parentId, int sortOrder) {
}
