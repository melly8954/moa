package com.moa.api.service.user;

import java.time.LocalDateTime;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.moa.api.dto.user.UserDto;
import com.moa.api.entity.user.User;
import com.moa.api.repository.reader.user.UserReader;
import com.moa.api.repository.writer.user.UserWriter;
import com.moa.common.exception.ErrorCode;
import com.moa.common.exception.ServiceException;
import com.moa.common.security.LoginAttemptedEvent;
import com.moa.common.security.Role;
import com.moa.common.security.recovery.WithdrawnAccountException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

	/** 없는 아이디도 비밀번호 비교 시간을 같게 해, 응답 시간으로 아이디가 있는지 알 수 없게 한다 */
	private static final String DUMMY_HASH = "{bcrypt}$2a$10$Q6j5vCfAfvKEAvADya60o.e0YeVnlmy.w1oEXksje43SQt56/PWli";

	private final UserReader userReader;
	private final UserWriter userWriter;
	private final PasswordEncoder passwordEncoder;
	private final ApplicationEventPublisher eventPublisher;
	private final WithdrawalProperties withdrawalProperties;

	/**
	 * @throws ServiceException 없으면 R001
	 */
	public UserDto find(Long id) {
		return userReader.find(id)
			.map(UserDto::from)
			.orElseThrow(() -> new ServiceException(ErrorCode.NOT_FOUND));
	}

	/**
	 * 아이디·비밀번호를 확인한다. 아이디가 없는지 비밀번호가 틀렸는지 구분해 알리지 않는다.
	 * 성공·실패 모두 LoginAttemptedEvent를 낸다 (접속 이력 등이 받는다).
	 *
	 * 잠긴 계정은 비밀번호가 맞을 때만 A007로 알린다 (틀린 비밀번호로 잠김 여부를 알 수 없게).
	 * moa 018: 탈퇴도 같다. 비밀번호가 맞고 잠기지 않았을 때만 A401·A402로 알린다. 틀리면 탈퇴했어도 A006이다
	 * (specs/018-member-withdrawal research R5, US2-AC3·AC8)
	 *
	 * @throws ServiceException 아이디가 없거나, 비밀번호가 틀리거나, 비밀번호로 로그인할 수 없는 계정이면 A006. 잠긴 계정이면 A007.
	 *         탈퇴해 유예 중이면 A401({@link WithdrawnAccountException}, 복구 쿠키가 붙는다), 유예가 지났으면 A402
	 */
	public UserDto authenticate(String loginId, String rawPassword) {
		User user = userReader.findByLoginId(loginId).orElse(null);
		String hash = user != null && user.hasPassword() ? user.getPassword() : DUMMY_HASH;
		boolean matches = passwordEncoder.matches(rawPassword, hash);
		if (user == null || !user.hasPassword() || !matches) {
			eventPublisher.publishEvent(LoginAttemptedEvent.failed(loginId, ErrorCode.INVALID_CREDENTIALS.getCode()));
			throw new ServiceException(ErrorCode.INVALID_CREDENTIALS);
		}
		if (user.isLocked()) {
			eventPublisher.publishEvent(LoginAttemptedEvent.failed(loginId, ErrorCode.ACCOUNT_LOCKED.getCode()));
			throw new ServiceException(ErrorCode.ACCOUNT_LOCKED);
		}
		if (user.isWithdrawn()) {
			throw withdrawn(loginId, user);
		}
		eventPublisher.publishEvent(LoginAttemptedEvent.succeeded(loginId, user.getId()));
		return UserDto.from(user);
	}

	/** moa 018: 본인 확인에 성공한 탈퇴 회원. 실패로 남긴다 (spec 가정) */
	private ServiceException withdrawn(String loginId, User user) {
		LocalDateTime withdrawnAt = user.getWithdrawnAt();
		if (withdrawalProperties.recoverable(withdrawnAt, LocalDateTime.now())) {
			eventPublisher.publishEvent(LoginAttemptedEvent.failed(loginId, ErrorCode.ACCOUNT_WITHDRAWN.getCode()));
			return new WithdrawnAccountException(user.getId(), withdrawnAt);
		}
		eventPublisher.publishEvent(LoginAttemptedEvent.failed(loginId, ErrorCode.ACCOUNT_PURGE_PENDING.getCode()));
		return new ServiceException(ErrorCode.ACCOUNT_PURGE_PENDING);
	}

	/**
	 * 첫 관리자 계정(ADMIN)을 만든다. 만든 계정은 비밀번호 변경 요구 상태다.
	 * moa: 건너뛸지(최고 관리자가 이미 있는지)와 최고 관리자 역할에 넣는 것은 UserApplication이 한다
	 * (specs/002-admin-users FR-001, specs/003-admin-roles research R20)
	 *
	 * @return 만든 계정 ID
	 * @throws ServiceException 비밀번호가 8자보다 짧으면 V001, 같은 아이디의 계정이 있으면 R002
	 */
	public Long bootstrapAdmin(String loginId, String temporaryPassword, String name) {
		if (temporaryPassword.length() < 8) {
			throw new ServiceException(ErrorCode.INVALID_INPUT, "첫 관리자 비밀번호는 8자 이상이어야 합니다.");
		}
		if (userReader.findByLoginId(loginId).isPresent()) {
			throw new ServiceException(ErrorCode.DUPLICATE_RESOURCE, "같은 아이디의 계정이 이미 있습니다: " + loginId);
		}
		User admin = new User(loginId, null, name == null || name.isBlank() ? "관리자" : name, Role.ADMIN);
		admin.resetPassword(passwordEncoder.encode(temporaryPassword));
		return userWriter.save(admin).getId();
	}

	/**
	 * 내 비밀번호를 바꾼다. 바꾸면 비밀번호 변경 요구가 풀린다. 다른 기기의 로그인은 그대로 둔다.
	 *
	 * @throws ServiceException 없으면 R001, 비밀번호가 없는 계정(소셜)이면 B001, 현재 비밀번호가 틀리거나 새 비밀번호가 같으면 V001
	 */
	public void changePassword(Long userId, String currentPassword, String newPassword) {
		User user = userReader.find(userId).orElseThrow(() -> new ServiceException(ErrorCode.NOT_FOUND));
		if (!user.hasPassword()) {
			throw new ServiceException(ErrorCode.INVALID_STATE, "비밀번호로 로그인하지 않는 계정입니다.");
		}
		if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
			throw new ServiceException(ErrorCode.INVALID_INPUT, "현재 비밀번호가 맞지 않습니다.");
		}
		if (currentPassword.equals(newPassword)) {
			throw new ServiceException(ErrorCode.INVALID_INPUT, "새 비밀번호가 지금 비밀번호와 같습니다.");
		}
		user.changePassword(passwordEncoder.encode(newPassword));
	}
}
