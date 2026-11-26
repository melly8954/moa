package com.moa.api.repository.reader.user;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Component;

import com.moa.api.repository.jpa.user.MemberPurgeRepository;

import lombok.RequiredArgsConstructor;

/** 파기 대상 찾기 (specs/018-member-withdrawal research R6) */
@Component
@RequiredArgsConstructor
public class MemberPurgeReader {

	private final MemberPurgeRepository memberPurgeRepository;

	/** @param cutoff 이 시각이나 그 전에 탈퇴했거나 지워진 회원 */
	public List<Long> findDueIds(LocalDateTime cutoff, Long systemId) {
		return memberPurgeRepository.findDueIds(cutoff, systemId);
	}

	/** 행을 잠그고 아직 파기 대상인지. 트랜잭션 안에서 부른다 */
	public boolean lockIfDue(Long userId, LocalDateTime cutoff, Long systemId) {
		return memberPurgeRepository.lockIfDue(userId, cutoff, systemId).isPresent();
	}
}
