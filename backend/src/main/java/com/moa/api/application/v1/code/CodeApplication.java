package com.moa.api.application.v1.code;

import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.moa.api.application.v1.code.command.CreateCodeCommand;
import com.moa.api.application.v1.code.command.ModifyCodeCommand;
import com.moa.api.dto.code.CodeDto;
import com.moa.api.dto.code.CodeGroupDto;
import com.moa.api.service.code.CodeService;
import com.moa.common.audit.CurrentActor;

import lombok.RequiredArgsConstructor;

/**
 * 공통코드 (선택 헬퍼 code). 화면용 코드 목록은 캐시한다 ("codes", 그룹 코드별, 사용 안 함 포함).
 * 사용하는 코드만 걸러 주는 일은 부르는 쪽이 한다. 캐시 키가 그룹 하나라 쓰기 때 비우기가 단순하다.
 * redis 헬퍼(@EnableCaching)가 없으면 캐시 어노테이션은 아무 일도 하지 않는다.
 */
@Component
@RequiredArgsConstructor
public class CodeApplication {

	public static final String CACHE = "codes";

	private final CodeService codeService;
	private final CurrentActor currentActor;

	@Transactional(readOnly = true)
	@Cacheable(value = CACHE, key = "#groupCode")
	public List<CodeDto> getScreenCodes(String groupCode) {
		return codeService.getCodes(groupCode, true);
	}

	@Transactional(readOnly = true)
	public List<CodeDto> getAllCodes(String groupCode) {
		return codeService.getCodes(groupCode, true);
	}

	@Transactional(readOnly = true)
	public List<CodeGroupDto> getGroups() {
		return codeService.getGroups();
	}

	@Transactional
	public CodeGroupDto createGroup(String groupCode, String name, String description) {
		return codeService.createGroup(groupCode, name, description);
	}

	@Transactional
	@CacheEvict(value = CACHE, key = "#command.groupCode()")
	public CodeDto createCode(CreateCodeCommand command) {
		return codeService.createCode(command.groupCode(), command.code(), command.name(), command.sortOrder());
	}

	@Transactional
	@CacheEvict(value = CACHE, key = "#command.groupCode()")
	public CodeDto modifyCode(ModifyCodeCommand command) {
		return codeService.modifyCode(command.groupCode(), command.code(), command.name(), command.sortOrder(),
			command.enabled());
	}

	@Transactional
	@CacheEvict(value = CACHE, key = "#groupCode")
	public void deleteCode(String groupCode, String code) {
		codeService.deleteCode(groupCode, code, currentActor.id());
	}
}
