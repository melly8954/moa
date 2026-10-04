package com.moa.api.repository.reader;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.moa.api.entity.Member;
import com.moa.api.repository.jpa.MemberRepository;

import lombok.RequiredArgsConstructor;

/**
 * 회원 조회.
 */
@Component
@RequiredArgsConstructor
public class MemberReader {

	private final MemberRepository memberRepository;

	public Optional<Member> findById(Long id) {
		return memberRepository.findById(id);
	}

	public Optional<Member> findByEmail(String email) {
		return memberRepository.findByEmail(email);
	}

	/** 휴대폰 번호 HMAC으로 찾는다. 파기된(PURGED) 회원도 재가입 제한 기간에는 HMAC이 남아 찾아진다 */
	public Optional<Member> findByPhoneHmac(String phoneHmac) {
		return memberRepository.findByPhoneHmac(phoneHmac);
	}

	public boolean existsByNickname(String nickname) {
		return memberRepository.existsByNickname(nickname);
	}
}
