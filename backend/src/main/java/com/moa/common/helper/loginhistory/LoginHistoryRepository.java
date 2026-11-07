package com.moa.common.helper.loginhistory;

import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 접속 이력 저장·정리. 조건 조회는 LoginHistoryReader가 QueryDSL로 한다.
 */
public interface LoginHistoryRepository extends JpaRepository<LoginHistory, Long> {

	@Modifying
	@Query("delete from LoginHistory h where h.createdAt < :before")
	int deleteCreatedBefore(@Param("before") LocalDateTime before);
}
