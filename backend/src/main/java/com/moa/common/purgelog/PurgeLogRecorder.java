package com.moa.common.purgelog;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * 정리기가 낸 PersonalDataPurgedEvent를 파기 이력으로 남긴다 (specs/013-purge-log FR-001, research R1).
 * 동기 리스너라 정리의 트랜잭션에 함께 들어간다.
 *
 * <p>LoginHistoryRecorder와 반대로 저장 실패를 잡지 않는다. 예외가 정리의 트랜잭션을 되돌려 기록 없이 지워진 개인정보가
 * 생기지 않게 한다 (FR-004). 지운 건수가 0이면 남기지 않는다 (FR-003).
 */
@Component
@RequiredArgsConstructor
public class PurgeLogRecorder {

	private final PurgeLogRepository purgeLogRepository;

	@EventListener
	public void record(PersonalDataPurgedEvent event) {
		if (event.count() < 1) {
			return;
		}
		purgeLogRepository.save(new PurgeLog(event));
	}
}
