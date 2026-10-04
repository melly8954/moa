package com.moa.api.repository.writer;

import org.springframework.stereotype.Component;

import com.moa.api.entity.Member;
import com.moa.api.repository.jpa.MemberRepository;

import lombok.RequiredArgsConstructor;

/**
 * 회원 변경.
 */
@Component
@RequiredArgsConstructor
public class MemberWriter {

	private final MemberRepository memberRepository;

	public Member create(Member member) {
		return memberRepository.save(member);
	}
}
