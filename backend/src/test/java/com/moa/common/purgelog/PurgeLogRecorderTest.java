package com.moa.common.purgelog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 파기 기록기 (specs/013-purge-log FR-003, FR-004). 저장소는 save만 흉내 낸다
 */
class PurgeLogRecorderTest {

	@Test
	@DisplayName("FR-003 지운 건수가 0이면 파기 기록을 저장하지 않는다")
	void skipsZeroCount() {
		List<PurgeLog> saved = new ArrayList<>();
		PurgeLogRecorder recorder = new PurgeLogRecorder(repository(saved, null));

		recorder.record(PersonalDataPurgedEvent.byScheduler(PurgeTarget.LOGIN_HISTORY, 0));

		assertThat(saved).isEmpty();
	}

	@Test
	@DisplayName("FR-003 지운 건수가 1 이상이면 종류·건수·방법·처리자를 저장한다")
	void savesPositiveCount() {
		List<PurgeLog> saved = new ArrayList<>();
		PurgeLogRecorder recorder = new PurgeLogRecorder(repository(saved, null));

		recorder.record(PersonalDataPurgedEvent.byScheduler(PurgeTarget.NOTIFICATION, 4));

		assertThat(saved).singleElement().satisfies(log -> {
			assertThat(log.getTarget()).isEqualTo(PurgeTarget.NOTIFICATION);
			assertThat(log.getPurgedCount()).isEqualTo(4);
			assertThat(log.getPurgeMethod()).isEqualTo(PurgeMethod.ROW_DELETE);
			assertThat(log.getActorType()).isEqualTo(PurgeActor.SCHEDULER);
			assertThat(log.getActorUserId()).isNull();
			assertThat(log.getTargetUserId()).isNull();
		});
	}

	@Test
	@DisplayName("FR-004 저장이 실패하면 기록기도 예외를 던져 정리의 트랜잭션이 되돌려진다")
	void propagatesSaveFailure() {
		RuntimeException failure = new IllegalStateException("저장 실패");
		PurgeLogRecorder recorder = new PurgeLogRecorder(repository(new ArrayList<>(), failure));

		assertThatThrownBy(() -> recorder.record(PersonalDataPurgedEvent.byScheduler(PurgeTarget.REFRESH_TOKEN, 1)))
			.isSameAs(failure);
	}

	private static PurgeLogRepository repository(List<PurgeLog> saved, RuntimeException failure) {
		return (PurgeLogRepository)Proxy.newProxyInstance(PurgeLogRepository.class.getClassLoader(),
			new Class<?>[] {PurgeLogRepository.class}, (proxy, method, args) -> {
				if (!method.getName().equals("save")) {
					throw new UnsupportedOperationException(method.getName());
				}
				if (failure != null) {
					throw failure;
				}
				saved.add((PurgeLog)args[0]);
				return args[0];
			});
	}
}
