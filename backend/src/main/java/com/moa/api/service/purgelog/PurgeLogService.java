package com.moa.api.service.purgelog;

import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moa.api.dto.purgelog.PurgeLogDto;
import com.moa.api.repository.reader.purgelog.PurgeLogReader;
import com.moa.common.exception.ErrorCode;
import com.moa.common.exception.ServiceException;
import com.moa.common.purgelog.PurgeTarget;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PurgeLogService {

	private final PurgeLogReader purgeLogReader;

	/**
	 * 조건은 모두 선택이고 함께 주면 AND다.
	 *
	 * @throws ServiceException from이 to보다 늦으면 V001
	 */
	public Page<PurgeLogDto> getPage(PurgeTarget target, LocalDate from, LocalDate to, Pageable pageable) {
		if (from != null && to != null && from.isAfter(to)) {
			throw new ServiceException(ErrorCode.INVALID_INPUT, "시작일(from)이 종료일(to)보다 늦습니다.");
		}
		return purgeLogReader.findPage(target, from, to, pageable).map(PurgeLogDto::from);
	}
}
