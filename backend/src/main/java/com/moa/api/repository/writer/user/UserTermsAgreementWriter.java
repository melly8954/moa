package com.moa.api.repository.writer.user;

import java.util.List;

import org.springframework.stereotype.Component;

import com.moa.api.entity.user.UserTermsAgreement;
import com.moa.api.repository.jpa.user.UserTermsAgreementRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UserTermsAgreementWriter {

	private final UserTermsAgreementRepository repository;

	public List<UserTermsAgreement> saveAll(List<UserTermsAgreement> agreements) {
		return repository.saveAll(agreements);
	}
}
