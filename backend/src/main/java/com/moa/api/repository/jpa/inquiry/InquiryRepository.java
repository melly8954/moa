package com.moa.api.repository.jpa.inquiry;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.moa.api.entity.inquiry.Inquiry;

import jakarta.persistence.LockModeType;

public interface InquiryRepository extends JpaRepository<Inquiry, Long> {

	/** 답변 저장용. 두 관리자가 함께 처음 답변해도 알림이 한 번만 가게 잠근다 (research R3) */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select i from Inquiry i where i.id = :id")
	Optional<Inquiry> findByIdForUpdate(@Param("id") Long id);
}
