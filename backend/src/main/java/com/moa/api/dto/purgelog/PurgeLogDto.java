package com.moa.api.dto.purgelog;

import java.time.LocalDateTime;

import com.moa.common.purgelog.PurgeActor;
import com.moa.common.purgelog.PurgeLog;
import com.moa.common.purgelog.PurgeMethod;
import com.moa.common.purgelog.PurgeTarget;

public record PurgeLogDto(Long id, PurgeTarget target, PurgeMethod method, int count, PurgeActor actor,
	LocalDateTime createdAt) {

	public static PurgeLogDto from(PurgeLog log) {
		return new PurgeLogDto(log.getId(), log.getTarget(), log.getPurgeMethod(), log.getPurgedCount(),
			log.getActorType(), log.getCreatedAt());
	}
}
