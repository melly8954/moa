package com.moa.common.audit;

import java.util.function.Supplier;

/**
 * 로그인 사용자 없이 실행하는 작업(스케줄러, 배치, 이벤트 리스너, 웹훅)을 감싼다.
 * 이 안에서 저장하면 감사 행위자가 시스템 계정이 된다. 밖에서 행위자 없이 저장하면 예외가 난다.
 *
 * <pre>{@code
 * SystemActor.run(() -> orderWriter.expireOverdue());
 * }</pre>
 */
public final class SystemActor {

	private static final ThreadLocal<Boolean> ACTIVE = ThreadLocal.withInitial(() -> false);

	private SystemActor() {
	}

	public static void run(Runnable task) {
		call(() -> {
			task.run();
			return null;
		});
	}

	public static <T> T call(Supplier<T> task) {
		boolean outer = ACTIVE.get();
		ACTIVE.set(true);
		try {
			return task.get();
		} finally {
			if (!outer) {
				ACTIVE.remove();
			}
		}
	}

	public static boolean isActive() {
		return ACTIVE.get();
	}
}
