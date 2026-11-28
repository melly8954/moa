package com.moa.api.application.v1.support;

import java.util.List;

import org.springframework.stereotype.Component;

import com.moa.api.dto.code.CodeGroupDto;
import com.moa.api.dto.support.SupportCategories;
import com.moa.api.dto.support.SupportCategories.SupportCategory;
import com.moa.api.service.code.CodeService;

import lombok.RequiredArgsConstructor;

/**
 * 고객센터 분류를 공통코드에서 읽는다 (specs/017-customer-center research R8). FAQ·문의 Application이 같이 쓰고,
 * 서비스에는 읽은 값을 넘긴다 (Service는 다른 Service를 부르지 않는다). 부르는 쪽의 트랜잭션 안에서 돈다.
 * 그룹이 없으면(지우지 못하지만 데이터를 비운 경우) 분류가 하나도 없는 것으로 본다. 목록은 그대로 열리고 쓰기만 V001이다.
 */
@Component
@RequiredArgsConstructor
public class SupportCategoryLoader {

	private final CodeService codeService;

	public SupportCategories load() {
		var group = codeService.getGroups().stream()
			.filter(g -> SupportCategories.GROUP.equals(g.groupCode()))
			.findFirst();
		if (group.isEmpty()) {
			return new SupportCategories(List.of());
		}
		boolean groupEnabled = group.map(CodeGroupDto::enabled).orElse(false);
		return new SupportCategories(codeService.getCodes(SupportCategories.GROUP, true).stream()
			.map(code -> new SupportCategory(code.code(), code.name(), groupEnabled && code.enabled()))
			.toList());
	}
}
