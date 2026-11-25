package com.moa.api.service.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Optional;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataIntegrityViolationException;

import com.moa.api.entity.user.User;
import com.moa.api.repository.reader.user.UserProfileReader;
import com.moa.api.repository.reader.user.UserReader;
import com.moa.api.repository.writer.user.UserProfileWriter;
import com.moa.common.exception.ErrorCode;
import com.moa.common.exception.ServiceException;

/**
 * 프로필 행을 처음 만들 때의 충돌 (specs/009-mypage research R14). 마지막 시도가 아니면 다시 하게 알리고, 마지막이면 오류 코드로 바꾼다.
 */
class ProfileServiceConflictTest {

	private static final Long USER_ID = 10L;
	private static final DataIntegrityViolationException TAG_CONFLICT = new DataIntegrityViolationException(
		"Duplicate entry 'moa_kim-0001' for key '" + NicknameTags.UNIQUE_CONSTRAINT + "'");
	private static final DataIntegrityViolationException PRIMARY_KEY_CONFLICT = new DataIntegrityViolationException(
		"Duplicate entry '10' for key 'PRIMARY'");
	private static final CannotAcquireLockException DEADLOCK = new CannotAcquireLockException("Deadlock found");

	private final UserProfileWriter writer = mock(UserProfileWriter.class);
	private ProfileService service;

	@BeforeEach
	void setUp() {
		UserReader userReader = mock(UserReader.class);
		UserProfileReader profileReader = mock(UserProfileReader.class);
		NicknameTags nicknameTags = mock(NicknameTags.class);
		User user = mock(User.class);
		when(user.getName()).thenReturn("moa_kim");
		when(userReader.find(USER_ID)).thenReturn(Optional.of(user));
		when(profileReader.findForUpdate(USER_ID)).thenReturn(Optional.empty());
		when(nicknameTags.assign(any())).thenReturn("0001");
		service = new ProfileService(userReader, profileReader, writer, nicknameTags,
			new ProfileProperties(Duration.ofDays(30)));
	}

	@Test
	@DisplayName("Edge 태그 충돌은 마지막 시도가 아니면 그대로 던지고, 마지막이면 B201이다")
	void tagConflict() {
		when(writer.save(any())).thenThrow(TAG_CONFLICT);

		assertThatThrownBy(() -> service.requireProfileForUpdate(USER_ID, false)).isSameAs(TAG_CONFLICT);
		assertServiceError(() -> service.requireProfileForUpdate(USER_ID, true), ErrorCode.NICKNAME_TAGS_EXHAUSTED);
	}

	@Test
	@DisplayName("Edge 같은 회원의 행이 동시에 만들어지면(PK 충돌) 다시 하게 알리고, 마지막이면 R002다")
	void primaryKeyConflict() {
		when(writer.save(any())).thenThrow(PRIMARY_KEY_CONFLICT);

		assertThatThrownBy(() -> service.requireProfileForUpdate(USER_ID, false))
			.isInstanceOf(ProfileCreationConflictException.class);
		assertServiceError(() -> service.requireProfileForUpdate(USER_ID, true), ErrorCode.DUPLICATE_RESOURCE);
	}

	@Test
	@DisplayName("Edge 같은 회원의 행을 동시에 만들다 교착이 나도 다시 하게 알리고, 마지막이면 R002다")
	void deadlock() {
		when(writer.save(any())).thenThrow(DEADLOCK);

		assertThatThrownBy(() -> service.requireProfileForUpdate(USER_ID, false))
			.isInstanceOf(ProfileCreationConflictException.class);
		assertServiceError(() -> service.requireProfileForUpdate(USER_ID, true), ErrorCode.DUPLICATE_RESOURCE);
	}

	private static void assertServiceError(ThrowingCallable call, ErrorCode errorCode) {
		assertThatThrownBy(call).isInstanceOfSatisfying(ServiceException.class,
			ex -> assertThat(ex.getErrorCode()).isEqualTo(errorCode));
	}
}
