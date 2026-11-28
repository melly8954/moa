package com.moa.api.service.user;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.moa.api.repository.reader.user.MemberPurgeReader;
import com.moa.api.repository.writer.user.MemberPurgeWriter;
import com.moa.common.audit.AuditProperties;

import lombok.RequiredArgsConstructor;

/**
 * 유예가 끝난 회원의 회원 도메인 개인정보 파기 (specs/018-member-withdrawal research R6, data-model "파기할 때").
 * 대상: 스스로 탈퇴한 지, 또는 관리자가 지운 지 유예 기간(grace-days)이 지난 회원(USER). 시스템 계정은 빼고, 이미 파기한 회원도 뺀다.
 * 알림·공지 확인·리프레시 토큰은 각 도메인이 지우고 Application이 한 트랜잭션으로 묶는다.
 */
@Service
@RequiredArgsConstructor
public class MemberPurgeService {

	/** 유예가 끝난 뒤 이만큼 지나도 남아 있으면 정리기가 ERROR로 알린다 (매일 돌므로 다섯 번 다시 한 뒤) */
	static final int OVERDUE_DAYS = 4;

	private final MemberPurgeReader memberPurgeReader;
	private final MemberPurgeWriter memberPurgeWriter;
	private final WithdrawalProperties withdrawalProperties;
	private final AuditProperties auditProperties;

	public List<Long> findDueIds(LocalDateTime now) {
		return memberPurgeReader.findDueIds(withdrawalProperties.purgeCutoff(now), auditProperties.systemActorId());
	}

	/** 유예가 끝난 뒤 {@value #OVERDUE_DAYS}일이 지나도 파기되지 않은 회원 수 */
	public int countOverdue(LocalDateTime now) {
		return memberPurgeReader
			.findDueIds(withdrawalProperties.purgeCutoff(now).minusDays(OVERDUE_DAYS), auditProperties.systemActorId())
			.size();
	}

	/**
	 * 행을 잠그고 아직 대상이면(복구되지 않았고 파기되지 않음) 회원만 쓰는 행을 지우고 회원 행의 개인정보 열을 덮는다.
	 * 트랜잭션 안에서 부른다.
	 *
	 * @return 파기했으면 참. 그 사이 복구됐거나 다른 서버가 먼저 파기했으면 거짓
	 */
	public boolean eraseIfDue(Long userId, LocalDateTime now) {
		Long systemId = auditProperties.systemActorId();
		if (!memberPurgeReader.lockIfDue(userId, withdrawalProperties.purgeCutoff(now), systemId)) {
			return false;
		}
		memberPurgeWriter.deleteOwnedRows(userId);
		memberPurgeWriter.erase(userId, MemberDisplay.WITHDRAWN_NAME, now, systemId);
		return true;
	}
}
