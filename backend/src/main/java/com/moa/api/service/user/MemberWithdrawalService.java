package com.moa.api.service.user;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.moa.api.dto.user.UserDto;
import com.moa.api.entity.user.User;
import com.moa.api.repository.reader.user.UserReader;
import com.moa.common.exception.ErrorCode;
import com.moa.common.exception.ServiceException;
import com.moa.common.response.ErrorResponse.FieldError;
import com.moa.common.security.recovery.RecoveryTicket;

import lombok.RequiredArgsConstructor;

/**
 * 회원 탈퇴와 복구 (specs/018-member-withdrawal research R4·R5). 트랜잭션은 Application이 연다.
 */
@Service
@RequiredArgsConstructor
public class MemberWithdrawalService {

	private final UserReader userReader;
	private final WithdrawalProperties withdrawalProperties;

	/**
	 * 닉네임을 확인하고 탈퇴시킨다. 회원 행을 잠가 읽으므로 트랜잭션 안에서 부른다.
	 *
	 * @param nickname 회원이 입력한 닉네임. 앞뒤 공백·유니코드 정규화를 맞춘 뒤 대소문자를 구별해 비교한다
	 * @return 탈퇴한 시각. 이미 탈퇴했으면(동시에 두 번) 그때의 시각을 그대로 돌려준다
	 * @throws ServiceException 없는 회원이면 R001, 닉네임이 다르면 V001(칸 {@code nickname})
	 */
	public LocalDateTime withdraw(Long userId, String nickname, LocalDateTime now) {
		User user = userReader.findForUpdateIncludingWithdrawn(userId)
			.orElseThrow(() -> new ServiceException(ErrorCode.NOT_FOUND));
		if (user.isWithdrawn()) {
			return user.getWithdrawnAt();
		}
		if (!NicknamePolicy.normalize(nickname).equals(NicknamePolicy.normalize(user.getName()))) {
			throw new ServiceException(ErrorCode.INVALID_INPUT, ErrorCode.INVALID_INPUT.getMessage(),
				List.of(new FieldError(NicknamePolicy.FIELD, "닉네임이 일치하지 않습니다.")));
		}
		user.withdraw(now);
		return user.getWithdrawnAt();
	}

	/**
	 * @throws ServiceException 복구 쿠키가 없거나 서명·만료·용도가 맞지 않으면 A403
	 */
	public RecoveryTicket requireTicket(Optional<RecoveryTicket> ticket) {
		return ticket.orElseThrow(() -> new ServiceException(ErrorCode.RECOVERY_NOT_AVAILABLE));
	}

	/**
	 * 복구 안내 (GET). 쿠키의 회원이 지금도 그 탈퇴 상태인지 본다.
	 *
	 * @return 완전 삭제 예정 시각
	 * @throws ServiceException 이미 복구했거나 다시 탈퇴했거나 지워졌으면 A403, 유예가 지났으면 A402
	 */
	public LocalDateTime requireRecoverable(RecoveryTicket ticket, LocalDateTime now) {
		User user = userReader.findIncludingWithdrawn(ticket.userId())
			.filter(found -> sameWithdrawal(found, ticket))
			.orElseThrow(() -> new ServiceException(ErrorCode.RECOVERY_NOT_AVAILABLE));
		if (!withdrawalProperties.recoverable(user.getWithdrawnAt(), now)) {
			throw new ServiceException(ErrorCode.ACCOUNT_PURGE_PENDING);
		}
		return withdrawalProperties.purgeAt(user.getWithdrawnAt());
	}

	/**
	 * 복구한다 (POST). 회원 행을 잠가 읽으므로 트랜잭션 안에서 부른다. 탈퇴 전 그대로 돌아온다(탈퇴 시각만 비운다).
	 *
	 * @throws ServiceException 이미 복구했거나 다시 탈퇴했거나 지워졌으면 A403, 잠겼으면 A007, 유예가 지났으면 A402
	 */
	public UserDto restore(RecoveryTicket ticket, LocalDateTime now) {
		User user = userReader.findForUpdateIncludingWithdrawn(ticket.userId())
			.filter(found -> sameWithdrawal(found, ticket))
			.orElseThrow(() -> new ServiceException(ErrorCode.RECOVERY_NOT_AVAILABLE));
		if (user.isLocked()) {
			throw new ServiceException(ErrorCode.ACCOUNT_LOCKED);
		}
		if (!withdrawalProperties.recoverable(user.getWithdrawnAt(), now)) {
			throw new ServiceException(ErrorCode.ACCOUNT_PURGE_PENDING);
		}
		user.restore();
		return UserDto.from(user);
	}

	/** 본인 확인 때와 같은 탈퇴인지. 다시 탈퇴했으면 시각이 달라 옛 쿠키를 쓸 수 없다 (DB는 마이크로초까지 둔다) */
	private static boolean sameWithdrawal(User user, RecoveryTicket ticket) {
		return user.isWithdrawn() && user.getWithdrawnAt()
			.truncatedTo(ChronoUnit.MICROS)
			.equals(ticket.withdrawnAt().truncatedTo(ChronoUnit.MICROS));
	}
}
