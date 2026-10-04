package com.moa.api.repository.jpa;

import java.util.Optional;

import com.moa.api.entity.Member;
import com.moa.common.repository.SoftDeleteRepository;

public interface MemberRepository extends SoftDeleteRepository<Member, Long> {

	/** 이메일 비교는 DB 정렬(utf8mb4_unicode_ci)로 대소문자를 무시한다 */
	Optional<Member> findByEmail(String email);

	Optional<Member> findByPhoneHmac(String phoneHmac);

	/** 닉네임 비교는 DB 정렬(utf8mb4_unicode_ci)로 대소문자를 무시한다 */
	boolean existsByNickname(String nickname);
}
