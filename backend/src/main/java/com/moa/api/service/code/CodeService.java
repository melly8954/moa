package com.moa.api.service.code;

import java.util.List;

import org.springframework.stereotype.Service;

import com.moa.api.dto.code.CodeDto;
import com.moa.api.dto.code.CodeGroupDto;
import com.moa.api.entity.code.Code;
import com.moa.api.entity.code.CodeGroup;
import com.moa.api.repository.reader.code.CodeReader;
import com.moa.api.repository.writer.code.CodeWriter;
import com.moa.common.exception.ErrorCode;
import com.moa.common.exception.ServiceException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CodeService {

	private final CodeReader codeReader;
	private final CodeWriter codeWriter;

	public List<CodeGroupDto> getGroups() {
		return codeReader.findGroups().stream().map(CodeGroupDto::from).toList();
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
	 * @throws ServiceException 같은 그룹 코드가 있으면 R002
	 */
	public CodeGroupDto createGroup(String groupCode, String name, String description) {
		if (codeReader.existsGroup(groupCode)) {
			throw new ServiceException(ErrorCode.DUPLICATE_RESOURCE, "이미 있는 코드 그룹입니다: " + groupCode);
		}
		return CodeGroupDto.from(codeWriter.saveGroup(new CodeGroup(groupCode, name, description)));
	}

	/**
	 * @throws ServiceException 그룹이 없으면 R001, 그룹 안에 같은 코드가 있으면 R002
	 */
	public CodeDto createCode(String groupCode, String code, String name, int sortOrder) {
		requireGroup(groupCode);
		if (codeReader.existsCode(groupCode, code)) {
			throw new ServiceException(ErrorCode.DUPLICATE_RESOURCE, "이미 있는 코드입니다: " + groupCode + "." + code);
		}
		return CodeDto.from(codeWriter.save(new Code(groupCode, code, name, sortOrder)));
	}

	/**
	 * @throws ServiceException 코드가 없으면 R001
	 */
	public CodeDto modifyCode(String groupCode, String code, String name, int sortOrder, boolean enabled) {
		Code target = requireCode(groupCode, code);
		target.modify(name, sortOrder, enabled);
		return CodeDto.from(target);
	}

	/**
	 * @throws ServiceException 코드가 없으면 R001
	 */
	public void deleteCode(String groupCode, String code, Long actorId) {
		requireCode(groupCode, code).delete(actorId);
	}

	private void requireGroup(String groupCode) {
		if (!codeReader.existsGroup(groupCode)) {
			throw new ServiceException(ErrorCode.NOT_FOUND);
		}
	}

	private Code requireCode(String groupCode, String code) {
		return codeReader.findCode(groupCode, code).orElseThrow(() -> new ServiceException(ErrorCode.NOT_FOUND));
	}
}
