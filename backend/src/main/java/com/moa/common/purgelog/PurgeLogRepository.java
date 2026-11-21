package com.moa.common.purgelog;

import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 파기 기록 저장·정리. 조건 조회는 PurgeLogReader가 QueryDSL로 한다.
 */
public interface PurgeLogRepository extends JpaRepository<PurgeLog, Long> {

	@Modifying
	@Query("delete from PurgeLog l where l.createdAt < :before")
	int deleteCreatedBefore(@Param("before") LocalDateTime before);
}
