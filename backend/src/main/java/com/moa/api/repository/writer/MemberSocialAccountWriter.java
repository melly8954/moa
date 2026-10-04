package com.moa.api.repository.writer;

import org.springframework.stereotype.Component;

import com.moa.api.entity.MemberSocialAccount;
import com.moa.api.repository.jpa.MemberSocialAccountRepository;

import lombok.RequiredArgsConstructor;

/**
 * 소셜 계정 연결.
 */
@Component
@RequiredArgsConstructor
public class MemberSocialAccountWriter {

	private final MemberSocialAccountRepository memberSocialAccountRepository;

	public MemberSocialAccount create(MemberSocialAccount account) {
		return memberSocialAccountRepository.save(account);
	}
}
