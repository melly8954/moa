package com.moa.api.application.v1.purgelog;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.moa.api.application.v1.purgelog.query.PurgeLogQuery;
import com.moa.api.dto.purgelog.PurgeLogDto;
import com.moa.api.service.purgelog.PurgeLogService;

import lombok.RequiredArgsConstructor;

/** 개인정보 파기 이력 조회 (specs/013-purge-log US2) */
@Component
@RequiredArgsConstructor
public class PurgeLogApplication {

	private final PurgeLogService purgeLogService;

	@Transactional(readOnly = true)
	public Page<PurgeLogDto> getPage(PurgeLogQuery query, Pageable pageable) {
		return purgeLogService.getPage(query.target(), query.from(), query.to(), pageable);
	}
}
