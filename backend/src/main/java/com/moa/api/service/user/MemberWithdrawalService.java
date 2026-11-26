package com.moa.api.service.user;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.moa.api.entity.user.User;
import com.moa.api.repository.reader.user.UserReader;
import com.moa.common.exception.ErrorCode;
import com.moa.common.exception.ServiceException;
import com.moa.common.response.ErrorResponse.FieldError;

import lombok.RequiredArgsConstructor;

/**
 * 회원 탈퇴와 복구 (specs/018-member-withdrawal research R4·R5). 트랜잭션은 Application이 연다.
 */
@Service
@RequiredArgsConstructor
public class MemberWithdrawalService {

	private final UserReader userReader;

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
}
