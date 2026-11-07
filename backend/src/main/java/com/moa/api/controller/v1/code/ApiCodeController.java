package com.moa.api.controller.v1.code;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.moa.api.application.v1.code.CodeApplication;
import com.moa.api.controller.v1.code.response.CodeListResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * 화면용 공통코드 (선택 헬퍼 code). 로그인한 사용자 누구나. 정렬 순서대로, 기본은 사용하는 코드만.
 */
@Tag(name = "공통코드 API")
@RestController
@RequestMapping("/api/v1/codes")
@RequiredArgsConstructor
public class ApiCodeController {

	private final CodeApplication codeApplication;

	@Operation(summary = "그룹의 코드 목록",
		description = "선택 상자 등에 쓴다. includeDisabled=true면 사용 안 함 코드도 enabled=false로 준다 "
			+ "(예전 데이터의 코드 이름을 보일 때). 지운 코드는 주지 않는다. 그룹이 없으면 R001")
	@GetMapping("/{groupCode}")
	public ResponseEntity<List<CodeListResponse>> getCodes(@PathVariable String groupCode,
		@RequestParam(defaultValue = "false") boolean includeDisabled) {
		return ResponseEntity.ok(codeApplication.getScreenCodes(groupCode).stream()
			.filter(code -> includeDisabled || code.enabled())
			.map(CodeListResponse::from)
			.toList());
	}
}
