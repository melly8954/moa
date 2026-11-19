package com.moa.api.codegroup;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 등록된 코드 그룹 전부 (moa, specs/004-admin-codes FR-004, research R5). 그룹의 기준은 이 등록이다 (DB에 그룹 표는 없다).
 * 시작할 때 검사하고 틀리면 시작이 실패한다: 그룹 코드 형식, 겹침, 이름·쓰는 곳 필수.
 * 등록에서 빠진 그룹의 항목은 DB에 남지만 어디에도 보이지 않는다.
 */
@Component
public class CodeGroupRegistry {

	private static final Pattern GROUP_CODE = Pattern.compile("[A-Z][A-Z0-9_]{0,49}");

	private final Map<String, CodeGroupDefinition> definitions = new LinkedHashMap<>();

	@Autowired
	public CodeGroupRegistry(ObjectProvider<CodeGroupSource> sources) {
		this(sources.orderedStream().flatMap(source -> source.codeGroups().stream()).toList());
	}

	public CodeGroupRegistry(List<CodeGroupDefinition> groups) {
		groups.forEach(this::register);
	}

	/** 등록 순서 */
	public List<CodeGroupDefinition> all() {
		return List.copyOf(definitions.values());
	}

	public Optional<CodeGroupDefinition> find(String groupCode) {
		return Optional.ofNullable(groupCode == null ? null : definitions.get(groupCode));
	}

	public boolean contains(String groupCode) {
		return find(groupCode).isPresent();
	}

	private void register(CodeGroupDefinition group) {
		String groupCode = group.groupCode();
		if (groupCode == null || !GROUP_CODE.matcher(groupCode).matches()) {
			throw new IllegalStateException("코드 그룹은 대문자·숫자·밑줄 50자 이하여야 합니다: " + groupCode);
		}
		if (group.name() == null || group.name().isBlank() || group.usedBy() == null || group.usedBy().isBlank()) {
			throw new IllegalStateException("코드 그룹은 이름과 쓰는 곳이 필요합니다: " + groupCode);
		}
		if (definitions.putIfAbsent(groupCode, group) != null) {
			throw new IllegalStateException("코드 그룹이 겹칩니다: " + groupCode);
		}
	}
}
