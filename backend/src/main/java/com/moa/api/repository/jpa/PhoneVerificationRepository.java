package com.moa.api.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import com.moa.api.entity.PhoneVerification;

public interface PhoneVerificationRepository extends JpaRepository<PhoneVerification, Long> {
}
