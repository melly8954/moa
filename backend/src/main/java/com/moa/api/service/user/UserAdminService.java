package com.moa.api.service.user;

import java.security.SecureRandom;
import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.moa.api.dto.role.GrantBlock;
import com.moa.api.dto.user.CreatedUserDto;
import com.moa.api.dto.user.UserAdminDto;
import com.moa.api.entity.user.User;
import com.moa.api.repository.reader.user.UserAdminReader;
import com.moa.api.repository.reader.user.UserReader;
import com.moa.api.repository.writer.user.UserAdminWriter;
import com.moa.common.audit.AuditProperties;
import com.moa.common.exception.ErrorCode;
import com.moa.common.exception.ServiceException;
import com.moa.common.security.AuthPrincipal;
import com.moa.common.security.Role;

import lombok.RequiredArgsConstructor;

/**
 * 사용자 관리 (선택 헬퍼 user-admin).
 *
 * <ul>
 *   <li>moa: 계정 종류(USER 회원, ADMIN 관리자 계정)는 사용자 관리 권한으로 만들고 바꾼다. 키트의 "ADMIN은 ADMIN만"을 없앴다
 *       (specs/002-admin-users FR-005, research R9)</li>
 *   <li>moa: 어느 계정을 다룰 수 있는지(최고 관리자 계정은 최고 관리자만, 관리자 계정은 권한이 내 안쪽일 때만)는
 *       UserAdminApplication이 역할 쪽과 함께 정해 {@link #requireNotProtected}로 넘긴다 (FR-007)</li>
 *   <li>자기 자신의 역할 바꾸기·잠그기·지우기는 B001</li>
 *   <li>시스템 계정(app.audit.system-actor-id)은 목록에 없고 건드릴 수 없다 (R001)</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class UserAdminService {

	private static final String TEMPORARY_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789!@#$%";
	private static final int TEMPORARY_LENGTH = 12;
	private static final SecureRandom RANDOM = new SecureRandom();

	private final UserReader userReader;
	private final UserAdminReader userAdminReader;
	private final UserAdminWriter userAdminWriter;
	private final PasswordEncoder passwordEncoder;
	private final AuditProperties auditProperties;

	public Page<UserAdminDto> search(String keyword, Role role, Boolean locked, Pageable pageable) {
		return userAdminReader.findPage(auditProperties.systemActorId(), keyword, role, locked, pageable)
			.map(UserAdminDto::from);
	}

	/**
	 * @throws ServiceException 없으면 R001
	 */
	public UserAdminDto get(Long id) {
		return UserAdminDto.from(require(id));
	}

	/**
	 * 임시 비밀번호로 만든다. 다음 로그인 때 바꿔야 한다.
	 *
	 * @throws ServiceException 아이디가 겹치면 R002
	 */
	public CreatedUserDto create(AuthPrincipal actor, String loginId, String name, Role role) {
		if (userAdminReader.existsLoginId(loginId)) {
			throw new ServiceException(ErrorCode.DUPLICATE_RESOURCE, "이미 쓰는 아이디입니다: " + loginId);
		}
		String temporary = temporaryPassword();
		User user = new User(loginId, null, name, role);
		user.resetPassword(passwordEncoder.encode(temporary));
		return new CreatedUserDto(UserAdminDto.from(userAdminWriter.save(user)), temporary);
	}

	/**
	 * @throws ServiceException 없으면 R001
	 */
	public UserAdminDto rename(AuthPrincipal actor, Long id, String name) {
		User user = requireManageable(actor, id);
		user.rename(name);
		return UserAdminDto.from(user);
	}

	/**
	 * 다음 토큰 재발급부터 반영된다 (액세스 토큰이 끝날 때까지는 이전 역할).
	 *
	 * @throws ServiceException 없으면 R001, 자기 자신이면 B001
	 */
	public UserAdminDto changeRole(AuthPrincipal actor, Long id, Role role) {
		User user = requireOther(actor, id);
		user.changeRole(role);
		return UserAdminDto.from(user);
	}

	/**
	 * 이미 잠겼으면 그대로 둔다. 로그아웃(토큰 폐기)은 부르는 쪽이 한다.
	 *
	 * @throws ServiceException 없으면 R001, 자기 자신이면 B001
	 */
	public void lock(AuthPrincipal actor, Long id) {
		User user = requireManageable(actor, id);
		requireNotSelf(actor, id);
		user.lock(LocalDateTime.now());
	}

	/**
	 * @throws ServiceException 없으면 R001
	 */
	public void unlock(AuthPrincipal actor, Long id) {
		requireManageable(actor, id).unlock();
	}

	/**
	 * 임시 비밀번호로 바꾼다. 다음 로그인 때 바꿔야 한다. 로그아웃(토큰 폐기)은 부르는 쪽이 한다.
	 *
	 * @return 임시 비밀번호 (이 응답에서만 보인다)
	 * @throws ServiceException 없으면 R001, 비밀번호가 없는 계정(소셜)이면 B001
	 */
	public String resetPassword(AuthPrincipal actor, Long id) {
		User user = requireManageable(actor, id);
		if (!user.hasPassword()) {
			throw new ServiceException(ErrorCode.INVALID_STATE, "비밀번호로 로그인하지 않는 계정입니다.");
		}
		String temporary = temporaryPassword();
		user.resetPassword(passwordEncoder.encode(temporary));
		return temporary;
	}

	/**
	 * 소프트 삭제. 같은 아이디로 다시 만들 수 있다. 로그아웃(토큰 폐기)은 부르는 쪽이 한다.
	 *
	 * @throws ServiceException 없으면 R001, 자기 자신이면 B001
	 */
	public void delete(AuthPrincipal actor, Long id) {
		User user = requireManageable(actor, id);
		requireNotSelf(actor, id);
		user.delete(actor.userId());
	}

	private User require(Long id) {
		if (id.equals(auditProperties.systemActorId())) {
			throw new ServiceException(ErrorCode.NOT_FOUND);
		}
		return userReader.find(id).orElseThrow(() -> new ServiceException(ErrorCode.NOT_FOUND));
	}

	/**
	 * moa: 이 계정을 다룰 수 없는 이유가 있으면 막는다 (specs/002-admin-users FR-007, research R9).
	 *
	 * @throws ServiceException 이유가 있으면 A005
	 */
	public void requireNotProtected(GrantBlock protection) {
		if (protection == GrantBlock.SUPER_ADMIN_ONLY) {
			throw new ServiceException(ErrorCode.FORBIDDEN, "최고 관리자 계정은 최고 관리자만 다룰 수 있어요.");
		}
		if (protection == GrantBlock.WIDER_PERMISSIONS) {
			throw new ServiceException(ErrorCode.FORBIDDEN, "나보다 권한이 많은 계정은 다룰 수 없어요.");
		}
	}

	/** 다룰 수 있는지는 {@link #requireNotProtected}가 먼저 본다 (moa) */
	private User requireManageable(AuthPrincipal actor, Long id) {
		return require(id);
	}

	private User requireOther(AuthPrincipal actor, Long id) {
		User user = require(id);
		requireNotSelf(actor, id);
		return user;
	}

	private void requireNotSelf(AuthPrincipal actor, Long id) {
		if (id.equals(actor.userId())) {
			throw new ServiceException(ErrorCode.INVALID_STATE, "자기 자신에게는 할 수 없습니다.");
		}
	}

	private static String temporaryPassword() {
		StringBuilder builder = new StringBuilder(TEMPORARY_LENGTH);
		for (int i = 0; i < TEMPORARY_LENGTH; i++) {
			builder.append(TEMPORARY_CHARS.charAt(RANDOM.nextInt(TEMPORARY_CHARS.length())));
		}
		return builder.toString();
	}
}
