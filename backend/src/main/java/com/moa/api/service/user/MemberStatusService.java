package com.moa.api.service.user;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.moa.api.dto.user.MemberStatusDto;
import com.moa.api.repository.reader.user.UserReader;

import lombok.RequiredArgsConstructor;

/**
 * 회원이 활동 중인지, 탈퇴했는지, 파기됐는지 (specs/018-member-withdrawal FR-018). 다른 기능이 탈퇴한 회원을 다룰 때 쓴다.
 * 유예 중·지움·파기된 회원은 User의 기본 조회에 나오지 않으므로 이 서비스만 안다 (research R8).
 */
@Service
@RequiredArgsConstructor
public class MemberStatusService {

	private final UserReader userReader;

	/** 한 번도 없던 회원이면 비어 있다 */
	public Optional<MemberStatusDto> find(Long userId) {
		return userReader.findInAnyState(userId).map(MemberStatusDto::from);
	}

	/** 스스로 탈퇴했는지. 유예 중과 파기된 뒤 모두 참이다 */
	public boolean isWithdrawn(Long userId) {
		return userReader.existsWithdrawn(userId);
	}
}
