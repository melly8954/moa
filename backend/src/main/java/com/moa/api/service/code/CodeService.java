package com.moa.api.service.code;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

import com.moa.api.dto.code.CodeDto;
import com.moa.api.dto.code.CodeGroupDto;
import com.moa.api.dto.code.CodeGroupTreeDto;
import com.moa.api.dto.code.CodeOrderDto;
import com.moa.api.entity.code.Code;
import com.moa.api.entity.code.CodeGroup;
import com.moa.api.repository.reader.code.CodeReader;
import com.moa.api.repository.writer.code.CodeWriter;
import com.moa.common.exception.ErrorCode;
import com.moa.common.exception.ServiceException;

import lombok.RequiredArgsConstructor;

/**
 * 공통코드. moa: 그룹은 DB(code_groups)에 두고 관리자가 다룬다. 그룹과 항목은 지우지 않고 사용 여부로 끈다
 * (specs/004-admin-codes FR-004·FR-008). 없는 그룹은 R001
 */
@Service
@RequiredArgsConstructor
public class CodeService {

	/** 자동 순번의 최소 자릿수 (FR-006). 999를 넘으면 자릿수가 늘어난다 */
	private static final String AUTO_CODE_FORMAT = "%03d";

	private final CodeReader codeReader;
	private final CodeWriter codeWriter;

	/** moa: 만든 순서 */
	public List<CodeGroupDto> getGroups() {
		return codeReader.findGroups().stream().map(CodeGroupDto::from).toList();
	}

	/** moa: 그룹마다 항목(사용 안 함 포함). 트리 표 (FR-005) */
	public List<CodeGroupTreeDto> getTree() {
		List<CodeGroup> groups = codeReader.findGroups();
		Map<String, List<CodeDto>> codes = codeReader.findCodes(groups.stream().map(CodeGroup::getGroupCode)
			.toList()).stream()
			.map(CodeDto::from)
			.collect(Collectors.groupingBy(CodeDto::groupCode));
		return groups.stream()
			.map(group -> new CodeGroupTreeDto(CodeGroupDto.from(group),
				codes.getOrDefault(group.getGroupCode(), List.of())))
			.toList();
	}

	/**
	 * moa: 관리자가 그룹을 만든다. 사용 중으로 만든다 (FR-004)
	 *
	 * @throws ServiceException 그룹 코드가 겹치면 R002
	 */
	public CodeGroupDto createGroup(String groupCode, String name, @Nullable String description) {
		if (codeReader.existsGroup(groupCode)) {
			throw new ServiceException(ErrorCode.DUPLICATE_RESOURCE, "이미 있는 그룹 코드입니다: " + groupCode);
		}
		return CodeGroupDto.from(codeWriter.save(new CodeGroup(groupCode, name, description)));
	}

	/**
	 * moa: 그룹 이름·설명·사용 여부 (FR-004·FR-008)
	 *
	 * @throws ServiceException 그룹이 없으면 R001
	 */
	public CodeGroupDto modifyGroup(String groupCode, String name, @Nullable String description, boolean enabled) {
		CodeGroup group = requireGroup(groupCode);
		group.modify(name, description, enabled);
		return CodeGroupDto.from(group);
	}

	/**
	 * moa: 그룹이 사용 중인가. 꺼진 그룹의 화면용 목록은 빈 목록이다 (FR-008)
	 *
	 * @throws ServiceException 그룹이 없으면 R001
	 */
	public boolean isGroupEnabled(String groupCode) {
		return requireGroup(groupCode).isEnabled();
	}

	/**
	 * @param includeDisabled 사용 안 함 코드도 넣을지 (관리 화면)
	 * @throws ServiceException 그룹이 없으면 R001
	 */
	public List<CodeDto> getCodes(String groupCode, boolean includeDisabled) {
		requireGroup(groupCode);
		List<Code> codes = includeDisabled ? codeReader.findCodes(groupCode) : codeReader.findEnabledCodes(groupCode);
		return codes.stream().map(CodeDto::from).toList();
	}

	/**
	 * moa: 코드를 비우면 그룹 안의 자동 순번(지운 것까지 센 숫자 코드 + 1, 세 자리 이상), 순서를 비우면 그룹 맨 뒤 (FR-006, R6)
	 *
	 * @throws ServiceException 그룹이 없으면 R001, 그룹 안에 같은 코드가 있으면 R002
	 */
	public CodeDto createCode(String groupCode, @Nullable String code, String name, @Nullable Integer sortOrder) {
		requireGroup(groupCode);
		String target = code == null || code.isBlank()
			? AUTO_CODE_FORMAT.formatted(codeReader.findMaxNumericCode(groupCode) + 1)
			: code;
		if (codeReader.existsCode(groupCode, target)) {
			throw new ServiceException(ErrorCode.DUPLICATE_RESOURCE, "이미 있는 코드입니다: " + groupCode + "." + target);
		}
		int order = sortOrder == null ? codeReader.findMaxSortOrder(groupCode) + 1 : sortOrder;
		return CodeDto.from(codeWriter.save(new Code(groupCode, target, name, order)));
	}

	/**
	 * @param sortOrder 비우면 그대로 둔다 (moa: 드로어는 이름·사용만 바꾼다)
	 * @throws ServiceException 코드가 없으면 R001
	 */
	public CodeDto modifyCode(String groupCode, String code, String name, @Nullable Integer sortOrder,
		boolean enabled) {
		Code target = requireCode(groupCode, code);
		target.modify(name, sortOrder == null ? target.getSortOrder() : sortOrder, enabled);
		return CodeDto.from(target);
	}

	/**
	 * moa: 한 그룹 안의 여러 항목을 한 번에 켜고 끈다 (FR-005). 하나라도 없으면 아무것도 바꾸지 않는다
	 * (호출하는 Application의 트랜잭션이 롤백한다).
	 *
	 * @throws ServiceException 그룹이 없거나 그룹에 없는 코드 R001
	 */
	public void setEnabled(String groupCode, Collection<String> codes, boolean enabled) {
		requireAll(groupCode, codes).forEach(code -> code.changeEnabled(enabled));
	}

	/**
	 * moa: 한 그룹 안의 순서를 한 번에 바꾼다 (FR-005). 다른 그룹의 코드가 섞이면 아무것도 바꾸지 않는다
	 *
	 * @throws ServiceException 그룹이 없거나 그룹에 없는 코드 R001
	 */
	public void reorder(String groupCode, List<CodeOrderDto> orders) {
		Map<String, Code> codes = requireAll(groupCode, orders.stream().map(CodeOrderDto::code).toList()).stream()
			.collect(Collectors.toMap(Code::getCode, Function.identity()));
		orders.forEach(order -> codes.get(order.code()).changeSortOrder(order.sortOrder()));
	}

	private CodeGroup requireGroup(String groupCode) {
		return codeReader.findGroup(groupCode).orElseThrow(() -> new ServiceException(ErrorCode.NOT_FOUND));
	}

	private Code requireCode(String groupCode, String code) {
		requireGroup(groupCode);
		return codeReader.findCode(groupCode, code).orElseThrow(() -> new ServiceException(ErrorCode.NOT_FOUND));
	}

	/** 보낸 코드가 모두 그 그룹에 있어야 한다 (겹친 코드는 하나로 본다) */
	private List<Code> requireAll(String groupCode, Collection<String> codes) {
		requireGroup(groupCode);
		Map<String, Code> found = codeReader.findCodes(groupCode).stream()
			.collect(Collectors.toMap(Code::getCode, Function.identity()));
		List<Code> targets = new ArrayList<>();
		for (String code : new LinkedHashSet<>(codes)) {
			Code target = found.get(code);
			if (target == null) {
				throw new ServiceException(ErrorCode.NOT_FOUND, "그룹에 없는 코드입니다: " + groupCode + "." + code);
			}
			targets.add(target);
		}
		return targets;
	}
}
