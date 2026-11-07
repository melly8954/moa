package com.moa.api.application.v1.loginhistory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.moa.api.application.v1.loginhistory.query.LoginHistoryQuery;
import com.moa.api.dto.loginhistory.LoginHistoryDto;
import com.moa.api.service.loginhistory.LoginHistoryService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class LoginHistoryApplication {

	private final LoginHistoryService loginHistoryService;

	@Transactional(readOnly = true)
	public Page<LoginHistoryDto> getPage(LoginHistoryQuery query, Pageable pageable) {
		return loginHistoryService.getPage(query.userId(), query.loginId(), query.success(), query.from(), query.to(),
			pageable);
	}
}
