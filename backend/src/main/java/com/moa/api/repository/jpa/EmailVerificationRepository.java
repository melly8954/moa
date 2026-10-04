package com.moa.api.repository.jpa;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.moa.api.entity.EmailVerification;

import jakarta.persistence.LockModeType;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {

	/** 같은 링크를 동시에 열어도 한 번만 쓰이도록 행을 잠근다 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select v from EmailVerification v where v.tokenHash = :tokenHash")
	Optional<EmailVerification> findForUpdateByTokenHash(@Param("tokenHash") String tokenHash);

	Optional<EmailVerification> findTopByEmailOrderByIdDesc(String email);

	@Modifying
	@Query("delete from EmailVerification v where v.expiresAt < :before")
	int deleteExpiredBefore(@Param("before") LocalDateTime before);
}
