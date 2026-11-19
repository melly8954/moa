package com.moa.api.service.user;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.moa.api.dto.user.CreatedUserDto;
import com.moa.api.dto.user.UserAdminDto;
import com.moa.api.entity.user.User;
import com.moa.api.entity.user.UserProfile;
import com.moa.api.repository.reader.user.UserAdminReader;
import com.moa.api.repository.reader.user.UserProfileReader;
import com.moa.api.repository.reader.user.UserReader;
import com.moa.api.repository.reader.user.UserSocialAccountReader;
import com.moa.api.repository.writer.user.UserAdminWriter;
import com.moa.api.repository.writer.user.UserProfileWriter;
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
 *   <li>ADMIN 역할을 주거나 뺄 수 있는 것은 ADMIN뿐이다. ADMIN이 아니면 ADMIN 계정을 건드릴 수 없다 (A005)</li>
 *   <li>자기 자신의 역할 바꾸기·잠그기·지우기는 B001. 그래서 ADMIN이 하나도 남지 않게 되는 일은 없다
 *       (ADMIN을 건드리는 사람은 언제나 다른 ADMIN이다)</li>
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
	// moa: 소셜 회원 역할 막기, 닉네임 키·태그 맞추기 (specs/010-social-login FR-007, research R14)
	private final UserSocialAccountReader userSocialAccountReader;
	private final UserProfileReader userProfileReader;
	private final UserProfileWriter userProfileWriter;
	private final NicknameTags nicknameTags;

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
	 * @throws ServiceException ADMIN이 아닌데 ADMIN으로 만들면 A005, 아이디가 겹치면 R002
	 */
	public CreatedUserDto create(AuthPrincipal actor, String loginId, String name, Role role) {
		if (role == Role.ADMIN && !isAdmin(actor)) {
			throw new ServiceException(ErrorCode.FORBIDDEN, "ADMIN 역할은 ADMIN만 줄 수 있습니다.");
		}
		if (userAdminReader.existsLoginId(loginId)) {
			throw new ServiceException(ErrorCode.DUPLICATE_RESOURCE, "이미 쓰는 아이디입니다: " + loginId);
		}
		String temporary = temporaryPassword();
		User user = new User(loginId, null, name, role);
		user.resetPassword(passwordEncoder.encode(temporary));
		return new CreatedUserDto(UserAdminDto.from(userAdminWriter.save(user)), temporary);
	}

	/**
	 * moa: 프로필(user_profiles)이 있는 회원은 닉네임 규칙(NicknamePolicy)을 지키고 닉네임 키·태그를 함께 맞춘다
	 * (specs/010-social-login research R14, specs/009-mypage FR-003). 태그는 새 키에서 비어 있으면 그대로, 아니면 새로.
	 * 프로필이 없는 회원(관리자 등)은 키트 규칙만 따른다.
	 *
	 * @throws ServiceException 없으면 R001, ADMIN이 아닌데 ADMIN 계정이면 A005, 프로필이 있는데 닉네임 규칙에 안 맞으면 V001(칸 name),
	 *     새 닉네임의 태그가 다 차면 B201, 같은 닉네임으로 동시에 바뀌어 태그가 겹치면 R002
	 */
	public UserAdminDto rename(AuthPrincipal actor, Long id, String name) {
		User user = requireManageable(actor, id);
		Optional<UserProfile> profile = userProfileReader.find(id);
		if (profile.isEmpty()) {
			user.rename(name);
			return UserAdminDto.from(user);
		}
		String nickname = NicknamePolicy.validate(name, "name");
		String key = NicknamePolicy.key(nickname);
		UserProfile found = profile.get();
		found.changeNickname(key, nicknameTags.keepOrAssign(key, found.getNicknameKey(), found.getNicknameTag()));
		user.rename(nickname);
		try {
			userProfileWriter.save(found);
		} catch (DataIntegrityViolationException ex) {
			if (NicknameTags.isTagConflict(ex)) {
				throw new ServiceException(ErrorCode.DUPLICATE_RESOURCE,
					"같은 닉네임으로 바꾸는 요청이 동시에 있었습니다. 다시 시도해 주세요.");
			}
			throw ex;
		}
		return UserAdminDto.from(user);
	}

	/**
	 * 다음 토큰 재발급부터 반영된다 (액세스 토큰이 끝날 때까지는 이전 역할).
	 *
	 * @throws ServiceException 없으면 R001, ADMIN이 아니면 A005, 자기 자신이면 B001, moa: 소셜 회원에게 ADMIN이면 A011
	 */
	public UserAdminDto changeRole(AuthPrincipal actor, Long id, Role role) {
		if (!isAdmin(actor)) {
			throw new ServiceException(ErrorCode.FORBIDDEN, "시스템 역할은 ADMIN만 바꿀 수 있습니다.");
		}
		User user = requireOther(actor, id);
		// moa: 소셜 회원에게 ADMIN을 주지 않는다 (specs/010-social-login FR-007, US1-AC6)
		if (role == Role.ADMIN && userSocialAccountReader.existsByUserId(id)) {
			throw new ServiceException(ErrorCode.SOCIAL_MEMBER_ROLE_NOT_ALLOWED);
		}
		user.changeRole(role);
		return UserAdminDto.from(user);
	}

	/**
	 * 이미 잠겼으면 그대로 둔다. 로그아웃(토큰 폐기)은 부르는 쪽이 한다.
	 *
	 * @throws ServiceException 없으면 R001, ADMIN이 아닌데 ADMIN 계정이면 A005, 자기 자신이면 B001
	 */
	public void lock(AuthPrincipal actor, Long id) {
		User user = requireManageable(actor, id);
		requireNotSelf(actor, id);
		user.lock(LocalDateTime.now());
	}

	/**
	 * @throws ServiceException 없으면 R001, ADMIN이 아닌데 ADMIN 계정이면 A005
	 */
	public void unlock(AuthPrincipal actor, Long id) {
		requireManageable(actor, id).unlock();
	}

	/**
	 * 임시 비밀번호로 바꾼다. 다음 로그인 때 바꿔야 한다. 로그아웃(토큰 폐기)은 부르는 쪽이 한다.
	 *
	 * @return 임시 비밀번호 (이 응답에서만 보인다)
	 * @throws ServiceException 없으면 R001, ADMIN이 아닌데 ADMIN 계정이면 A005, 비밀번호가 없는 계정(소셜)이면 B001
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
	 * @throws ServiceException 없으면 R001, ADMIN이 아닌데 ADMIN 계정이면 A005, 자기 자신이면 B001
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

	private User requireManageable(AuthPrincipal actor, Long id) {
		User user = require(id);
		if (user.getRole() == Role.ADMIN && !isAdmin(actor)) {
			throw new ServiceException(ErrorCode.FORBIDDEN, "ADMIN 계정은 ADMIN만 다룰 수 있습니다.");
		}
		return user;
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

	private static boolean isAdmin(AuthPrincipal actor) {
		return actor.roles().contains(Role.ADMIN.name());
	}

	private static String temporaryPassword() {
		StringBuilder builder = new StringBuilder(TEMPORARY_LENGTH);
		for (int i = 0; i < TEMPORARY_LENGTH; i++) {
			builder.append(TEMPORARY_CHARS.charAt(RANDOM.nextInt(TEMPORARY_CHARS.length())));
		}
		return builder.toString();
	}
}
