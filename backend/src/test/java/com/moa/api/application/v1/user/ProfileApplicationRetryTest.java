package com.moa.api.application.v1.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.moa.api.dto.user.NicknameChangeDto;
import com.moa.api.service.code.CodeService;
import com.moa.api.service.user.MemberActivityPolicy;
import com.moa.api.service.user.ProfileCreationConflictException;
import com.moa.api.service.user.ProfileService;
import com.moa.common.exception.ErrorCode;
import com.moa.common.exception.ServiceException;

/**
 * 마이페이지 저장을 새 트랜잭션으로 다시 하는 규칙 (specs/009-mypage research R4·R14). 최대 3번, 세 번째가 마지막 시도다.
 */
class ProfileApplicationRetryTest {

	private static final Long USER_ID = 10L;
	private static final String NICKNAME = "새이름";
	private static final DataIntegrityViolationException TAG_CONFLICT = new DataIntegrityViolationException(
		"Duplicate entry for key 'uk_user_profile_nickname_key_nickname_tag'");
	private static final NicknameChangeDto CHANGED = new NicknameChangeDto(NICKNAME, "0002", null);

	private final ProfileService profileService = mock(ProfileService.class);
	private final PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
	private final ProfileApplication application = new ProfileApplication(mock(MemberActivityPolicy.class),
		profileService, mock(CodeService.class), new TransactionTemplate(transactionManager));

	@Test
	@DisplayName("Edge 태그 충돌이 두 번이면 새 트랜잭션으로 다시 하고, 세 번째를 마지막 시도로 부른다")
	void retriesTagConflict() {
		when(profileService.changeNickname(USER_ID, NICKNAME, false)).thenThrow(TAG_CONFLICT);
		when(profileService.changeNickname(USER_ID, NICKNAME, true)).thenReturn(CHANGED);

		assertThat(application.changeNickname(USER_ID, NICKNAME)).isEqualTo(CHANGED);

		InOrder order = inOrder(profileService);
		order.verify(profileService, times(2)).changeNickname(USER_ID, NICKNAME, false);
		order.verify(profileService).changeNickname(USER_ID, NICKNAME, true);
		verify(transactionManager, times(3)).getTransaction(any());
		verify(transactionManager, times(2)).rollback(any());
		verify(transactionManager, times(1)).commit(any());
	}

	@Test
	@DisplayName("Edge 프로필 행 동시 생성도 새 트랜잭션으로 다시 하고, 다음 시도에 되면 거기서 끝난다")
	void retriesCreationConflict() {
		when(profileService.changeNickname(USER_ID, NICKNAME, false))
			.thenThrow(new ProfileCreationConflictException(new IllegalStateException("PK")))
			.thenReturn(CHANGED);

		assertThat(application.changeNickname(USER_ID, NICKNAME)).isEqualTo(CHANGED);

		verify(transactionManager, times(2)).getTransaction(any());
		verify(profileService, never()).changeNickname(USER_ID, NICKNAME, true);
	}

	@Test
	@DisplayName("Edge 마지막 시도의 B201·R002는 그대로 나오고 더 하지 않는다")
	void lastAttemptErrorIsFinal() {
		when(profileService.changeNickname(USER_ID, NICKNAME, false)).thenThrow(TAG_CONFLICT);
		when(profileService.changeNickname(USER_ID, NICKNAME, true))
			.thenThrow(new ServiceException(ErrorCode.NICKNAME_TAGS_EXHAUSTED))
			.thenThrow(new ServiceException(ErrorCode.DUPLICATE_RESOURCE));

		assertThatThrownBy(() -> application.changeNickname(USER_ID, NICKNAME))
			.isInstanceOfSatisfying(ServiceException.class,
				ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.NICKNAME_TAGS_EXHAUSTED));
		assertThatThrownBy(() -> application.changeNickname(USER_ID, NICKNAME))
			.isInstanceOfSatisfying(ServiceException.class,
				ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_RESOURCE));
		verify(transactionManager, times(6)).getTransaction(any());
	}

	@Test
	@DisplayName("Edge 태그 충돌이 아닌 저장 오류와 규칙 오류(B202)는 다시 하지 않는다")
	void doesNotRetryOtherErrors() {
		DataIntegrityViolationException other = new DataIntegrityViolationException("Data too long for column 'bio'");
		when(profileService.changeNickname(USER_ID, NICKNAME, false))
			.thenThrow(other)
			.thenThrow(new ServiceException(ErrorCode.NICKNAME_CHANGE_TOO_SOON));

		assertThatThrownBy(() -> application.changeNickname(USER_ID, NICKNAME)).isSameAs(other);
		assertThatThrownBy(() -> application.changeNickname(USER_ID, NICKNAME)).isInstanceOf(ServiceException.class);

		verify(transactionManager, times(2)).getTransaction(any());
	}
}
