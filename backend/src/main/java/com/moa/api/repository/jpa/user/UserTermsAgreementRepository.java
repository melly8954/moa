package com.moa.api.repository.jpa.user;

import org.springframework.data.jpa.repository.JpaRepository;

import com.moa.api.entity.user.UserTermsAgreement;

public interface UserTermsAgreementRepository extends JpaRepository<UserTermsAgreement, Long> {
}
