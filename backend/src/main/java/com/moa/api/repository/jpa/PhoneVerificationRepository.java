package com.moa.api.repository.jpa;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.moa.api.entity.PhoneVerification;

import jakarta.persistence.LockModeType;

public interface PhoneVerificationRepository extends JpaRepository<PhoneVerification, Long> {

	Optional<PhoneVerification> findTopByPhoneHmacOrderByIdDesc(String phoneHmac);

	long countByPhoneHmacAndCreatedAtAfter(String phoneHmac, LocalDateTime after);

	/** 입력 횟수를 정확히 세도록 행을 잠근다 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select v from PhoneVerification v where v.signUpId = :signUpId order by v.id desc limit 1")
	Optional<PhoneVerification> findLatestForUpdateBySignUpId(@Param("signUpId") Long signUpId);

	@Modifying
	@Query("delete from PhoneVerification v where v.createdAt < :before")
	int deleteCreatedBefore(@Param("before") LocalDateTime before);
}
