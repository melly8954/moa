package com.moa.api.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import com.moa.api.entity.EmailVerification;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {
}
