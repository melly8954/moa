package com.moa.api.repository.writer.user;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import com.moa.api.repository.jpa.user.MemberPurgeRepository;

import lombok.RequiredArgsConstructor;

/** 회원 도메인의 개인정보 파기 (specs/018-member-withdrawal data-model "파기할 때") */
@Component
@RequiredArgsConstructor
public class MemberPurgeWriter {

	private final MemberPurgeRepository memberPurgeRepository;

	/** 회원만 쓰는 행: 프로필 시간대·게임 정보·프로필·소셜 연결·약관 동의 (자식부터) */
	public void deleteOwnedRows(Long userId) {
		memberPurgeRepository.deletePlayTimes(userId);
		memberPurgeRepository.deleteGameProfiles(userId);
		memberPurgeRepository.deleteProfile(userId);
		memberPurgeRepository.deleteSocialAccounts(userId);
		memberPurgeRepository.deleteTermsAgreements(userId);
	}

	/** 회원 행의 개인정보 열을 덮는다 */
	public void erase(Long userId, String name, LocalDateTime now, Long systemId) {
		memberPurgeRepository.erase(userId, name, now, systemId);
	}
}
