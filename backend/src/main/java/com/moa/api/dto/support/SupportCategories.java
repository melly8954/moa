package com.moa.api.dto.support;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 고객센터 분류 (공통코드 SUPPORT_CATEGORY, specs/017-customer-center FR-003·research R8). FAQ와 문의가 함께 쓴다.
 *
 * @param all 모든 항목 (꺼진 항목 포함, 코드 관리의 순서). 저장된 분류의 이름을 보일 때 쓴다
 */
public record SupportCategories(List<SupportCategory> all) {

	public static final String GROUP = "SUPPORT_CATEGORY";

	/**
	 * @param active 지금 고를 수 있는지 (항목이 사용 중이고 그룹도 켜져 있다)
	 */
	public record SupportCategory(String code, String name, boolean active) {
	}

	public SupportCategories {
		all = List.copyOf(all);
	}

	/** 항목 이름. 없는 항목이면 코드를 그대로 */
	public String nameOf(String code) {
		return all.stream().filter(c -> c.code().equals(code)).map(SupportCategory::name).findFirst().orElse(code);
	}

	public boolean isActive(String code) {
		return all.stream().anyMatch(c -> c.active() && c.code().equals(code));
	}

	/** 지금 고를 수 있는 항목 (순서대로) */
	public List<SupportCategory> activeList() {
		return all.stream().filter(SupportCategory::active).toList();
	}

	/** 지금 고를 수 있는 항목 코드 */
	public Set<String> active() {
		return all.stream().filter(SupportCategory::active).map(SupportCategory::code).collect(Collectors.toSet());
	}
}
